#!/usr/bin/env node

import { readdirSync, readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const agentRoot = resolve(scriptDir, '..');
const repoRoot = resolve(agentRoot, '..', '..');
const backendRoot = resolve(repoRoot, 'backend');
const outputPath = resolve(agentRoot, 'generated', 'kyx-api-index.json');

const BLOCKED_PATH_RE = /(^|[-/])(create|update|delete|remove|submit|approve|reject|cancel|sync|import|export|upload|download|login|logout|auth|oauth|oauth2|authorize|token|password|captcha|callback|open|public|file|api-key|secret|save|assign|transfer|start|stop|enable|disable|reset|send|bind|unbind|pay|refund|write-off|settle|confirm|clock-in)([-/]|$)/i;
const BLOCKED_SUMMARY_RE = /(创建|新增|修改|更新|删除|移除|提交|审批|同意|拒绝|驳回|取消|同步|导入|导出|上传|下载|登录|登出|密码|验证码|发送|重置|保存|分配|转派|启用|停用|支付|退款|核销|结算|确认)/i;
const BLOCKED_SOURCE_RE = /HttpServletResponse|OperateTypeEnum\.EXPORT|operateType\s*=\s*EXPORT/;

const TYPE_ALIASES = new Map([
  ['String', 'string'],
  ['Long', 'long'],
  ['Integer', 'int'],
  ['int', 'int'],
  ['Boolean', 'boolean'],
  ['boolean', 'boolean'],
  ['BigDecimal', 'decimal'],
  ['LocalDate', 'date'],
  ['LocalDateTime', 'datetime'],
  ['Date', 'datetime'],
]);

function walk(dir, predicate, acc = []) {
  let entries;
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return acc;
  }
  for (const entry of entries) {
    if (entry.name === 'target' || entry.name === 'build' || entry.name === '.git') {
      continue;
    }
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, predicate, acc);
    } else if (predicate(full)) {
      acc.push(full);
    }
  }
  return acc;
}

function firstStringLiteral(text) {
  if (!text) return '';
  const direct = text.match(/^\s*"([^"]+)"/);
  if (direct) return direct[1];
  const attr = text.match(/\b(?:value|path)\s*=\s*(?:\{\s*)?"([^"]+)"/);
  if (attr) return attr[1];
  const any = text.match(/"([^"]+)"/);
  return any ? any[1] : '';
}

function annotationArgs(block, name) {
  const marker = `@${name}`;
  const start = block.indexOf(marker);
  if (start < 0) return null;
  let i = start + marker.length;
  while (/\s/.test(block[i] || '')) i += 1;
  if (block[i] !== '(') return '';
  let depth = 0;
  let quote = null;
  for (let j = i; j < block.length; j += 1) {
    const ch = block[j];
    const prev = block[j - 1];
    if (quote) {
      if (ch === quote && prev !== '\\') quote = null;
      continue;
    }
    if (ch === '"' || ch === "'") {
      quote = ch;
      continue;
    }
    if (ch === '(') depth += 1;
    if (ch === ')') {
      depth -= 1;
      if (depth === 0) return block.slice(i + 1, j);
    }
  }
  return null;
}

function annotationArgsAll(block, name) {
  const out = [];
  let offset = 0;
  while (offset < block.length) {
    const idx = block.indexOf(`@${name}`, offset);
    if (idx < 0) break;
    const args = annotationArgs(block.slice(idx), name);
    if (args !== null) out.push(args);
    offset = idx + name.length + 1;
  }
  return out;
}

function stringAttr(args, attrName) {
  if (!args) return '';
  const attr = new RegExp(`\\b${attrName}\\s*=\\s*"([^"]*)"`).exec(args);
  if (attr) return attr[1];
  if (attrName === 'value' || attrName === 'name') {
    return firstStringLiteral(args);
  }
  return '';
}

function boolAttr(args, attrName, defaultValue = false) {
  if (!args) return defaultValue;
  const attr = new RegExp(`\\b${attrName}\\s*=\\s*(true|false)`).exec(args);
  return attr ? attr[1] === 'true' : defaultValue;
}

function normalizePath(...parts) {
  const joined = parts
    .filter((part) => part !== undefined && part !== null && String(part).trim() !== '')
    .map((part) => String(part).trim())
    .join('/');
  return joined.replace(/\/+/g, '/').replace(/\/$/, '') || '/';
}

function serviceName(file) {
  const rel = relative(backendRoot, file).split(sep);
  return rel[0] || 'backend';
}

function apiPrefixForFile(file) {
  const normalized = file.split(sep).join('/');
  if (normalized.includes('/controller/admin/')) return '/admin-api';
  if (normalized.includes('/controller/app/')) return '/app-api';
  return null;
}

function moduleNameFromService(service) {
  return service.replace(/^kyx-service-/, '').replace(/^kyx-/, '');
}

function categoryFor(path, service, tag) {
  const p = path.toLowerCase();
  const haystack = `${p} ${service} ${tag || ''}`.toLowerCase();
  if (p.includes('/business/work/requirement')) return 'requirement';
  if (p.includes('/business/todo')) return 'todo';
  if (p.includes('/business/executive-cockpit') || p.includes('/business/work/calendar')) return 'cockpit';
  if (p.includes('/hr/attendance') || p.includes('/hr/administrative/leave') || p.includes('/hr/leave/balance') || p.includes('/overtime') || p.includes('/correction')) return 'hr-attendance';
  if (p.includes('/hr/questionnaire')) return 'hr-questionnaire';
  if (p.includes('/hr/exam')) return 'hr-exam';
  if (p.includes('/hr/')) return 'hr-employee';
  if (p.includes('/system/') || haystack.includes('dept') || haystack.includes('dict')) return 'system';
  if (p.includes('/bpm/')) return 'bpm';
  if (p.includes('/erp/')) return 'erp';
  if (p.includes('/finance/')) return 'finance';
  if (p.includes('/ai/')) return 'ai';
  if (p.includes('/im/')) return 'im';
  return moduleNameFromService(service);
}

function normalizeType(raw, annotations = '') {
  let type = String(raw || '').replace(/\s+/g, ' ').trim();
  type = type.replace(/^final\s+/, '');
  const listMatch = type.match(/(?:List|Set|Collection)<\s*([\w.]+)\s*>/);
  if (listMatch) return `${normalizeType(listMatch[1])}[]`;
  if (type.endsWith('[]')) return `${normalizeType(type.slice(0, -2))}[]`;
  const short = type.split('.').pop();
  const mapped = TYPE_ALIASES.get(short) || short || 'string';
  if (annotations.includes('@DateTimeFormat') && mapped === 'string') return 'date';
  return mapped;
}

function splitTopLevel(text, delimiter = ',') {
  const items = [];
  let start = 0;
  let angle = 0;
  let paren = 0;
  let bracket = 0;
  let quote = null;
  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    const prev = text[i - 1];
    if (quote) {
      if (ch === quote && prev !== '\\') quote = null;
      continue;
    }
    if (ch === '"' || ch === "'") {
      quote = ch;
      continue;
    }
    if (ch === '<') angle += 1;
    else if (ch === '>') angle = Math.max(0, angle - 1);
    else if (ch === '(') paren += 1;
    else if (ch === ')') paren = Math.max(0, paren - 1);
    else if (ch === '[') bracket += 1;
    else if (ch === ']') bracket = Math.max(0, bracket - 1);
    else if (ch === delimiter && angle === 0 && paren === 0 && bracket === 0) {
      items.push(text.slice(start, i).trim());
      start = i + 1;
    }
  }
  const last = text.slice(start).trim();
  if (last) items.push(last);
  return items;
}

function stripAnnotations(param) {
  let out = '';
  for (let i = 0; i < param.length; i += 1) {
    if (param[i] !== '@') {
      out += param[i];
      continue;
    }
    while (i < param.length && !/\s/.test(param[i])) i += 1;
    while (/\s/.test(param[i] || '')) i += 1;
    if (param[i] === '(') {
      let depth = 0;
      let quote = null;
      for (; i < param.length; i += 1) {
        const ch = param[i];
        const prev = param[i - 1];
        if (quote) {
          if (ch === quote && prev !== '\\') quote = null;
          continue;
        }
        if (ch === '"' || ch === "'") {
          quote = ch;
          continue;
        }
        if (ch === '(') depth += 1;
        if (ch === ')') {
          depth -= 1;
          if (depth === 0) break;
        }
      }
    } else {
      i -= 1;
    }
  }
  return out.replace(/\s+/g, ' ').trim();
}

function importedTypes(content, file) {
  const imports = new Map();
  for (const match of content.matchAll(/^\s*import\s+([\w.]+);/gm)) {
    const fqcn = match[1];
    imports.set(fqcn.split('.').pop(), fqcn);
  }
  const pkg = content.match(/^\s*package\s+([\w.]+);/m)?.[1];
  return { imports, pkg, file };
}

function schemaDescription(annotations) {
  const args = annotationArgs(annotations, 'Schema');
  if (!args) return '';
  return stringAttr(args, 'description') || stringAttr(args, 'value') || '';
}

function parseVoFields(file, javaByClass, cache) {
  if (!file || cache.has(file)) return cache.get(file) || [];
  let content;
  try {
    content = readFileSync(file, 'utf8');
  } catch {
    return [];
  }

  const fields = [];
  if (/extends\s+PageParam\b/.test(content)) {
    fields.push(
      { name: 'pageNo', type: 'int', notes: '分页页码，默认 1', source: 'PageParam' },
      { name: 'pageSize', type: 'int', notes: '分页大小，建议 <= 30；只取 total 时传 1', source: 'PageParam' },
    );
  }

  let pendingAnnotations = [];
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    if (trimmed.startsWith('@')) {
      pendingAnnotations.push(trimmed);
      continue;
    }
    const match = trimmed.match(/^private\s+(?!static\b)(?:final\s+)?([^;=\n]+?)\s+(\w+)\s*(?:=[^;]*)?;/);
    if (!match) {
      pendingAnnotations = [];
      continue;
    }
    const annotations = pendingAnnotations.join('\n');
    pendingAnnotations = [];
    const rawType = match[1];
    const name = match[2];
    if (!name || name === 'serialVersionUID') continue;
    const desc = schemaDescription(annotations);
    const type = normalizeType(rawType, annotations);
    const notes = desc || undefined;
    fields.push({ name, type, notes, source: file });
  }

  cache.set(file, fields);
  return fields;
}

function resolveTypeFile(typeName, context, javaByClass) {
  const short = typeName.replace(/<.*$/, '').replace(/\[\]$/, '').split('.').pop();
  if (!short || TYPE_ALIASES.has(short)) return null;
  const imported = context.imports.get(short);
  if (imported) return javaByClass.get(imported) || javaByClass.get(short) || null;
  if (context.pkg) {
    return javaByClass.get(`${context.pkg}.${short}`) || javaByClass.get(short) || null;
  }
  return javaByClass.get(short) || null;
}

function paramNameFromRequestParam(args, fallback) {
  const explicit = stringAttr(args, 'value') || stringAttr(args, 'name');
  return explicit || fallback;
}

function parseMethodParams(paramsText, context, javaByClass, voCache) {
  const params = [];
  const seen = new Set();
  for (const rawParam of splitTopLevel(paramsText)) {
    if (!rawParam || /HttpServlet(Response|Request)/.test(rawParam) || rawParam.includes('@RequestBody')) {
      continue;
    }
    const cleaned = stripAnnotations(rawParam);
    const tokens = cleaned.split(/\s+/).filter(Boolean);
    if (tokens.length < 2) continue;
    const name = tokens[tokens.length - 1].replace(/\[\]$/, '');
    const rawType = tokens.slice(0, -1).join(' ');
    const requestParamArgs = annotationArgs(rawParam, 'RequestParam');
    if (requestParamArgs !== null) {
      const paramName = paramNameFromRequestParam(requestParamArgs, name);
      const required = boolAttr(requestParamArgs, 'required', true);
      const type = normalizeType(rawType, rawParam);
      if (!seen.has(paramName)) {
        seen.add(paramName);
        params.push({
          name: paramName,
          type,
          notes: required ? '必填' : undefined,
          source: 'request-param',
        });
      }
      continue;
    }

    if (/(ReqVO|PageReqVO|QueryReqVO|ExportReqVO)$/.test(rawType.split(/[<\s]/).pop() || '')) {
      const voFile = resolveTypeFile(rawType, context, javaByClass);
      const fields = parseVoFields(voFile, javaByClass, voCache);
      for (const field of fields) {
        if (seen.has(field.name)) continue;
        seen.add(field.name);
        params.push({
          name: field.name,
          type: field.type,
          notes: field.notes,
          source: field.source === 'PageParam' ? 'PageParam' : `vo:${rawType.split(/[<\s]/).pop()}`,
        });
      }
    }
  }
  return params;
}

function parseInlineParameters(annotations) {
  return annotationArgsAll(annotations, 'Parameter').map((args) => ({
    name: stringAttr(args, 'name'),
    description: stringAttr(args, 'description') || stringAttr(args, 'value'),
    required: boolAttr(args, 'required', false),
  })).filter((item) => item.name);
}

function mergeParameterDocs(params, inlineDocs) {
  const docs = new Map(inlineDocs.map((item) => [item.name, item]));
  const merged = params.map((param) => {
    const doc = docs.get(param.name);
    const notes = [doc?.description || param.notes, doc?.required || param.notes === '必填' ? '必填' : '']
      .filter(Boolean)
      .join('；');
    return {
      ...param,
      notes: notes || undefined,
    };
  });
  for (const doc of inlineDocs) {
    if (merged.some((param) => param.name === doc.name)) continue;
    merged.push({
      name: doc.name,
      type: 'string',
      notes: [doc.description, doc.required ? '必填' : ''].filter(Boolean).join('；') || undefined,
      source: 'parameter-annotation',
    });
  }
  return merged;
}

function responseShape(returnType) {
  const compact = String(returnType || '').replace(/\s+/g, '');
  if (/PageResult<.+>/.test(compact)) return '{list, total}';
  if (/List<.+>/.test(compact)) return '[]';
  const common = compact.match(/CommonResult<(.+)>/);
  if (common) return common[1].replace(/>+$/, '>');
  return compact || 'object';
}

function methodBlocks(content) {
  const blocks = [];
  const re = /((?:\s*@[\w.]+(?:\([\s\S]*?\))?)+)\s*public\s+([\s\S]*?)\s+(\w+)\s*\(([\s\S]*?)\)\s*(?:throws\s+[^{]+)?\{/g;
  for (const match of content.matchAll(re)) {
    blocks.push({
      annotations: match[1] || '',
      returnType: match[2] || '',
      methodName: match[3],
      paramsText: match[4] || '',
    });
  }
  return blocks;
}

function extractClassRequestMapping(content) {
  const beforeClass = content.slice(0, content.search(/\bpublic\s+class\b/));
  const matches = annotationArgsAll(beforeClass, 'RequestMapping');
  return firstStringLiteral(matches[matches.length - 1] || '');
}

function extractTag(content) {
  const args = annotationArgs(content.slice(0, content.search(/\bpublic\s+class\b/)), 'Tag');
  return stringAttr(args, 'name') || '';
}

function methodMapping(annotations) {
  const getArgs = annotationArgs(annotations, 'GetMapping');
  if (getArgs !== null) {
    return { method: 'GET', path: firstStringLiteral(getArgs) };
  }
  const requestArgs = annotationArgs(annotations, 'RequestMapping');
  if (requestArgs !== null && /RequestMethod\.GET|\bmethod\s*=\s*GET\b/.test(requestArgs)) {
    return { method: 'GET', path: firstStringLiteral(requestArgs) };
  }
  return null;
}

function endpointKeywords(endpoint) {
  const pieces = [
    endpoint.path,
    endpoint.category,
    endpoint.tag,
    endpoint.purpose,
    endpoint.methodName,
    ...endpoint.query.flatMap((row) => [row[0], row[2] || '']),
  ];
  return Array.from(
    new Set(
      pieces
        .join(' ')
        .split(/[\s/_.:-]+/)
        .map((item) => item.trim())
        .filter((item) => item.length >= 2),
    ),
  ).slice(0, 24);
}

function isSafeReadEndpoint(endpoint, annotations) {
  if (endpoint.method !== 'GET') return false;
  if (!endpoint.path.startsWith('/admin-api/') && !endpoint.path.startsWith('/app-api/')) return false;
  if (BLOCKED_PATH_RE.test(endpoint.path)) return false;
  if (BLOCKED_SUMMARY_RE.test(endpoint.purpose || '')) return false;
  if (BLOCKED_SOURCE_RE.test(annotations)) return false;
  if (/void\b/.test(endpoint.returnType)) return false;
  return true;
}

function buildJavaClassIndex(javaFiles) {
  const index = new Map();
  for (const file of javaFiles) {
    let content;
    try {
      content = readFileSync(file, 'utf8');
    } catch {
      continue;
    }
    const pkg = content.match(/^\s*package\s+([\w.]+);/m)?.[1];
    const cls = content.match(/\b(?:class|interface|enum)\s+(\w+)/)?.[1];
    if (!cls) continue;
    index.set(cls, file);
    if (pkg) index.set(`${pkg}.${cls}`, file);
  }
  return index;
}

function scan() {
  const javaFiles = walk(backendRoot, (file) => file.endsWith('.java'));
  const javaByClass = buildJavaClassIndex(javaFiles);
  const controllerFiles = javaFiles.filter((file) =>
    file.endsWith('Controller.java') &&
    file.includes(`${sep}src${sep}main${sep}java${sep}`) &&
    file.includes(`${sep}controller${sep}`),
  );
  const voCache = new Map();
  const endpoints = [];
  const rejected = [];

  for (const file of controllerFiles) {
    const prefix = apiPrefixForFile(file);
    if (!prefix) continue;
    const content = readFileSync(file, 'utf8');
    const classMapping = extractClassRequestMapping(content);
    if (!classMapping) continue;
    const tag = extractTag(content);
    const service = serviceName(file);
    const context = importedTypes(content, file);

    for (const block of methodBlocks(content)) {
      const mapping = methodMapping(block.annotations);
      if (!mapping) continue;
      const path = normalizePath(prefix, classMapping, mapping.path);
      const summary = stringAttr(annotationArgs(block.annotations, 'Operation'), 'summary');
      const inlineParameters = parseInlineParameters(block.annotations);
      const params = mergeParameterDocs(
        parseMethodParams(block.paramsText, context, javaByClass, voCache),
        inlineParameters,
      );
      const endpoint = {
        path,
        method: mapping.method,
        category: categoryFor(path, service, tag),
        service,
        tag,
        purpose: summary || `${tag || service} ${block.methodName}`,
        query: params.map((param) => [param.name, param.type || 'string', param.notes].filter(Boolean)),
        response: responseShape(block.returnType),
        permission: stringAttr(annotationArgs(block.annotations, 'PreAuthorize'), 'value') || firstStringLiteral(annotationArgs(block.annotations, 'PreAuthorize')),
        methodName: block.methodName,
        returnType: block.returnType.replace(/\s+/g, ' ').trim(),
        generated: true,
        source: relative(repoRoot, file),
      };
      endpoint.tags = endpointKeywords(endpoint);
      if (isSafeReadEndpoint(endpoint, block.annotations)) {
        endpoints.push(endpoint);
      } else {
        rejected.push({
          path,
          method: mapping.method,
          purpose: endpoint.purpose,
          source: endpoint.source,
        });
      }
    }
  }

  endpoints.sort((a, b) => a.path.localeCompare(b.path));
  const unique = [];
  const seen = new Set();
  for (const endpoint of endpoints) {
    const key = `${endpoint.method} ${endpoint.path}`;
    if (seen.has(key)) continue;
    seen.add(key);
    unique.push(endpoint);
  }

  return {
    meta: {
      generatedAt: new Date().toISOString(),
      source: 'KYDev Java controller source scan',
      policy: {
        include: ['GET under /admin-api/** or /app-api/**'],
        exclude:
          'write/approval/auth/token/file/import/export/upload/download/open/public/callback style paths and summaries',
      },
      totalSafeReadEndpoints: unique.length,
      rejectedGetEndpoints: rejected.length,
    },
    endpoints: unique,
  };
}

const index = scan();
mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(outputPath, `${JSON.stringify(index, null, 2)}\n`, 'utf8');
console.log(`Generated ${index.endpoints.length} safe-read endpoints -> ${relative(repoRoot, outputPath)}`);
