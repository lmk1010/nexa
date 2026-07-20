#!/usr/bin/env node
// Scan the ordersys Vue frontend (`order-ui/src/api/**/*.js`) and emit a raw
// endpoint list. Each Vue api module wraps a backend HTTP call; the preceding
// `/** ... */` or `//` comment is the natural seed for the endpoint's
// `purpose` sentence. Read-only endpoints only — anything that mutates state
// is filtered out by path / summary regex.

import { readdirSync, readFileSync, mkdirSync, writeFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const agentRoot = resolve(scriptDir, '..');
const ORDERSYS_UI_ROOT =
  process.env.ORDERSYS_UI_ROOT ||
  resolve(agentRoot, '..', '..', '..', 'ordersys', 'order-ui', 'src', 'api');
const outputPath = resolve(agentRoot, 'generated', 'ordersys-api-raw.json');

// Path-based blocklist for mutating verbs. Kept conservative — statistics
// endpoints often mention topics like "revoke" as query dimensions without
// being a revoke action, so verbs that show up in stat paths (revoke, cancel
// as a topic) are excluded here and filtered by summary regex instead.
const BLOCKED_PATH_RE = /(^|[-/])(create|update|delete|remove|submit|approve|reject|sync|import|export|upload|download|login|logout|auth|oauth|oauth2|authorize|token|password|captcha|callback|save|assign|transfer|start|stop|enable|disable|reset|send|bind|unbind|pay|refund|write-off|settle|confirm|edit|batch-set)([-/]|$)/i;
// Also block obvious action-style path suffixes.
const BLOCKED_ACTION_SUFFIX_RE = /\/(add|do[A-Z][a-z]+)$/;
const BLOCKED_SUMMARY_RE = /(创建|新增|添加|修改|更新|删除|移除|提交|审批|同意|拒绝|驳回|取消|同步|导入|导出|上传|下载|登录|登出|密码|验证码|发送|重置|保存|分配|转派|启用|停用|支付|退款|核销|结算|确认|绑定|解绑)/;

function walk(dir, acc = []) {
  let entries;
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return acc;
  }
  for (const entry of entries) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, acc);
    } else if (entry.isFile() && entry.name.endsWith('.js')) {
      acc.push(full);
    }
  }
  return acc;
}

// Grab the comment block that ends immediately before position `end`.
// Supports:
//   /** ... */
//   /* ... */
//   //  ...  (one or more consecutive lines)
function precedingComment(source, end) {
  let i = end - 1;
  // Skip whitespace back to previous non-whitespace char.
  while (i >= 0 && /\s/.test(source[i])) i -= 1;
  if (i < 0) return '';

  // Block comment /** ... */ or /* ... */
  if (source[i] === '/' && source[i - 1] === '*') {
    let j = i - 2;
    while (j >= 1 && !(source[j] === '/' && source[j + 1] === '*')) j -= 1;
    if (j >= 0) {
      return source.slice(j, i + 1);
    }
    return '';
  }

  // Line comments — walk backwards line by line.
  const lineStart = source.lastIndexOf('\n', i) + 1;
  const line = source.slice(lineStart, i + 1);
  if (line.trimStart().startsWith('//')) {
    // Collect all consecutive `//` lines above.
    const collected = [line.trim()];
    let cursor = lineStart - 1;
    while (cursor > 0) {
      const prevStart = source.lastIndexOf('\n', cursor - 1) + 1;
      const prevLine = source.slice(prevStart, cursor);
      if (!prevLine.trimStart().startsWith('//')) break;
      collected.unshift(prevLine.trim());
      cursor = prevStart - 1;
    }
    return collected.join('\n');
  }

  return '';
}

function cleanComment(raw) {
  if (!raw) return '';
  return raw
    .replace(/^\/\*+/, '')
    .replace(/\*+\/$/, '')
    .split('\n')
    .map((line) =>
      line
        .replace(/^\s*\/\/\s?/, '')
        .replace(/^\s*\*+\s?/, '')
        .trim(),
    )
    .filter(
      (line) =>
        line &&
        !line.startsWith('@param') &&
        !line.startsWith('@return') &&
        !line.startsWith('@returns') &&
        !line.startsWith('@description'),
    )
    .join(' ')
    .trim();
}

// Extract balanced-brace body of a request({ ... }) call. Returns null if not
// found or malformed. Handles nested braces and quoted strings.
function extractRequestBody(source, startAfterOpenBrace) {
  let depth = 1;
  let inString = false;
  let stringChar = '';
  for (let i = startAfterOpenBrace; i < source.length; i += 1) {
    const ch = source[i];
    const prev = source[i - 1];
    if (inString) {
      if (ch === stringChar && prev !== '\\') inString = false;
      continue;
    }
    if (ch === '"' || ch === "'" || ch === '`') {
      inString = true;
      stringChar = ch;
      continue;
    }
    if (ch === '{') depth += 1;
    else if (ch === '}') {
      depth -= 1;
      if (depth === 0) return { end: i, body: source.slice(startAfterOpenBrace, i) };
    }
  }
  return null;
}

const URL_RE = /url\s*:\s*(['"`])([^'"`]+)\1/;
const METHOD_RE = /method\s*:\s*(['"`])([a-zA-Z]+)\1/;
const PARAMS_LIT_RE = /params\s*:\s*\{([^{}]*)\}/;

function extractParamNames(body) {
  const out = new Set();
  // params: { a, b, c: something }
  const litMatch = PARAMS_LIT_RE.exec(body);
  if (litMatch) {
    for (const chunk of litMatch[1].split(',')) {
      const name = chunk.trim().split(':')[0].trim();
      if (/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name)) out.add(name);
    }
  }
  return [...out];
}

// Look for the outer function signature just before request({...}).
// Handles:
//   export function foo(params) { return request({...}) }
//   export function foo({ a, b }) { return request({url,...,params:{a,b}}) }
const FUNC_HEADER_RE = /(?:export\s+)?(?:async\s+)?function\s+([A-Za-z_$][\w$]*)\s*\(([^)]*)\)/g;

function scanFile(file) {
  const source = readFileSync(file, 'utf8');
  const endpoints = [];

  FUNC_HEADER_RE.lastIndex = 0;
  const headers = [];
  let m;
  while ((m = FUNC_HEADER_RE.exec(source)) !== null) {
    headers.push({ name: m[1], signature: m[2], headerStart: m.index, bodyIdxStart: m.index + m[0].length });
  }

  for (let idx = 0; idx < headers.length; idx += 1) {
    const header = headers[idx];
    const nextHeaderStart = idx + 1 < headers.length ? headers[idx + 1].headerStart : source.length;
    const region = source.slice(header.bodyIdxStart, nextHeaderStart);

    // find request( ... {
    const reqMatch = /request\s*\(\s*\{/.exec(region);
    if (!reqMatch) continue;
    const openIdx = header.bodyIdxStart + reqMatch.index + reqMatch[0].length;
    const extracted = extractRequestBody(source, openIdx);
    if (!extracted) continue;
    const body = extracted.body;

    const urlM = URL_RE.exec(body);
    if (!urlM) continue;
    const url = urlM[2].trim();

    const methodM = METHOD_RE.exec(body);
    const method = (methodM ? methodM[2] : 'get').toLowerCase();

    if (method !== 'get') continue;

    const params = extractParamNames(body);
    const comment = cleanComment(precedingComment(source, header.headerStart));

    endpoints.push({
      functionName: header.name,
      path: url,
      method: 'GET',
      params,
      comment,
      sourceFile: relative(ORDERSYS_UI_ROOT, file).split('\\').join('/'),
    });
  }

  return endpoints;
}

function main() {
  try {
    statSync(ORDERSYS_UI_ROOT);
  } catch {
    console.error(`ordersys UI root not found: ${ORDERSYS_UI_ROOT}`);
    console.error('Set ORDERSYS_UI_ROOT env to override.');
    process.exit(1);
  }

  const files = walk(ORDERSYS_UI_ROOT);
  const rawEndpoints = [];
  for (const file of files) {
    try {
      rawEndpoints.push(...scanFile(file));
    } catch (err) {
      console.warn(`scan failed: ${file}: ${err.message}`);
    }
  }

  // Dedup by (path, method); prefer the entry with a comment.
  const byKey = new Map();
  for (const ep of rawEndpoints) {
    // Skip templated paths for now — they need typed segment inference.
    const key = `${ep.method} ${ep.path}`;
    const prev = byKey.get(key);
    if (!prev) {
      byKey.set(key, ep);
      continue;
    }
    if (!prev.comment && ep.comment) byKey.set(key, ep);
    else if (prev.params.length < ep.params.length) byKey.set(key, ep);
  }

  const deduped = [...byKey.values()];

  // Read-only filter.
  const kept = [];
  const skippedByBlocklist = [];
  for (const ep of deduped) {
    if (
      BLOCKED_PATH_RE.test(ep.path) ||
      BLOCKED_ACTION_SUFFIX_RE.test(ep.path) ||
      BLOCKED_SUMMARY_RE.test(ep.comment)
    ) {
      skippedByBlocklist.push(ep);
      continue;
    }
    kept.push(ep);
  }

  kept.sort((a, b) => a.path.localeCompare(b.path));

  const output = {
    meta: {
      system: 'ordersys',
      source: ORDERSYS_UI_ROOT,
      generatedAt: new Date().toISOString(),
      totalScanned: rawEndpoints.length,
      totalDeduped: deduped.length,
      totalReadOnly: kept.length,
      totalBlocked: skippedByBlocklist.length,
    },
    endpoints: kept,
    // Debug: what got dropped so we can spot-check filtering.
    _blockedSamples: skippedByBlocklist.slice(0, 20).map((ep) => ({
      path: ep.path,
      comment: ep.comment,
      sourceFile: ep.sourceFile,
    })),
  };

  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, JSON.stringify(output, null, 2));

  console.log(`scanned files: ${files.length}`);
  console.log(`raw endpoints: ${rawEndpoints.length}`);
  console.log(`deduped: ${deduped.length}`);
  console.log(`read-only kept: ${kept.length}`);
  console.log(`blocked (write/dangerous): ${skippedByBlocklist.length}`);
  console.log(`output: ${outputPath}`);
}

main();
