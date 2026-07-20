#!/usr/bin/env node
// Merge OA (from Java controller scan, generated/kyx-api-index.json) +
// ordersys (from Vue api scan, generated/ordersys-api-raw.json) + hand-curated
// overlay files under curated/*.json → single generated/api-index.json in the
// unified schema described in docs/api-index-schema.md.
//
// This is the canonical index for `api_search` + `api_call`. No workflows,
// no aliases, no rules — one endpoint per record, purpose sentence is the
// primary retrieval signal, params/response describe the shape.

import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const agentRoot = resolve(scriptDir, '..');
const OA_RAW = resolve(agentRoot, 'generated', 'kyx-api-index.json');
const ORDERSYS_RAW = resolve(agentRoot, 'generated', 'ordersys-api-raw.json');
// 新增：Java 源码扫出来的 ordersys，参数和 purpose 更真实（覆盖 Vue-based 版本）
const ORDERSYS_JAVA = resolve(agentRoot, 'generated', 'ordersys-java-api.json');
const CURATED_DIR = resolve(agentRoot, 'curated');
const ALLOWLIST = resolve(agentRoot, 'curated', 'allowlist.json');
const OUTPUT = resolve(agentRoot, 'generated', 'api-index.json');

function readJson(path) {
  if (!existsSync(path)) return null;
  return JSON.parse(readFileSync(path, 'utf8'));
}

function normalizeQueryOA(query) {
  // OA scanner emits [name, type, notes] tuples.
  if (!Array.isArray(query)) return [];
  return query.map((row) => ({
    name: row[0],
    type: row[1] || 'string',
    notes: row[2] || undefined,
  }));
}

function normalizeQueryOrdersys(params) {
  if (!Array.isArray(params)) return [];
  return params.map((name) => ({ name, type: 'unknown' }));
}

// Purpose sentence for OA endpoints. Java scanner already fills
// `purpose` from @Operation.summary. Enrich by prepending the tag
// (module label) and appending the response-VO name so retrieval has
// more surface area.
function oaPurpose(ep) {
  const summary = (ep.purpose || '').trim();
  const tag = (ep.tag || '').replace(/^管理后台\s*-?\s*/, '').trim();
  const parts = [];
  if (summary) parts.push(summary);
  if (tag && !summary.includes(tag)) parts.push(`模块：${tag}`);
  if (ep.response && !/^object$/i.test(ep.response)) parts.push(`返回：${ep.response}`);
  return parts.join('。');
}

// Ordersys purpose starts from the extracted comment. When comment is
// missing or too short, we fall back to the URL segments as a poor-man's
// hint. Curated overlays will replace both.
function ordersysPurpose(ep) {
  const c = (ep.comment || '').trim();
  if (c) return c;
  const seg = ep.path.split('/').filter(Boolean).slice(-3).join(' / ');
  return `连图 ordersys 接口 ${seg}`;
}

function extractKeywords(purpose, path, extra = []) {
  const out = new Set(extra);
  // Path segments (skip common noise segments).
  for (const seg of path.split('/').filter(Boolean)) {
    if (seg.length >= 2 && !/^(admin|admin-api|app-api|api|v\d+|user|list|page)$/i.test(seg)) {
      out.add(seg);
    }
  }
  // Chinese-substring keywords: 2/3-char slices of purpose (bigrams/trigrams).
  const chineseRuns = (purpose || '').match(/[一-鿿]{2,}/g) || [];
  for (const run of chineseRuns) {
    for (let n = 2; n <= 4; n += 1) {
      for (let i = 0; i + n <= run.length; i += 1) {
        out.add(run.slice(i, i + n));
      }
    }
  }
  return [...out].filter((k) => k.length >= 2).slice(0, 40);
}

function buildOaRecord(ep) {
  const purpose = oaPurpose(ep);
  return {
    id: `oa:${ep.method || 'GET'}:${ep.path}`,
    domain: 'oa',
    path: ep.path,
    method: (ep.method || 'GET').toUpperCase(),
    purpose,
    params: normalizeQueryOA(ep.query),
    response: ep.response || 'object',
    keywords: extractKeywords(purpose, ep.path, ep.tags || []),
    source: ep.source || '',
    // OA extras — kept for debugging but not used by retrieval.
    _extras: {
      category: ep.category,
      service: ep.service,
      permission: ep.permission,
      methodName: ep.methodName,
    },
  };
}

function buildOrdersysRecord(ep) {
  const purpose = ordersysPurpose(ep);
  return {
    id: `ordersys:${ep.method || 'GET'}:${ep.path}`,
    domain: 'ordersys',
    path: ep.path,
    method: (ep.method || 'GET').toUpperCase(),
    purpose,
    params: normalizeQueryOrdersys(ep.params),
    response: 'object',
    keywords: extractKeywords(purpose, ep.path, [ep.functionName].filter(Boolean)),
    source: ep.sourceFile ? `order-ui/src/api/${ep.sourceFile}` : '',
    _extras: { functionName: ep.functionName },
  };
}

// 从 Java 源码扫的更真实描述（覆盖 Vue-based 记录，保留其 id/domain）
// scan-ordersys-java-api.mjs 出来的 endpoint 已经带真实 params + purpose
function buildOrdersysJavaRecord(ep) {
  const purpose = ep.purpose || '';
  return {
    id: `ordersys:${ep.method || 'GET'}:${ep.path}`,
    domain: 'ordersys',
    path: ep.path,
    method: (ep.method || 'GET').toUpperCase(),
    purpose,
    params: (ep.params || []).map((p) => ({
      name: p.name,
      type: p.type || 'unknown',
      notes: p.notes || undefined,
    })),
    response: 'object',
    keywords: extractKeywords(purpose, ep.path, [ep.voClass].filter(Boolean)),
    source: ep.source ? ep.source.replace(/^.*\/ordersys\//, 'ordersys/') : '',
    _extras: { voClass: ep.voClass },
  };
}

function applyCuratedOverlay(records, overlay) {
  const byId = new Map(records.map((r) => [r.id, r]));
  let applied = 0;
  let created = 0;
  for (const c of overlay.endpoints || []) {
    const method = (c.method || 'GET').toUpperCase();
    const id = `${c.domain}:${method}:${c.path}`;
    const existing = byId.get(id);
    if (existing) {
      // Overlay purpose/params/response/keywords ALWAYS win.
      existing.purpose = c.purpose || existing.purpose;
      if (Array.isArray(c.params) && c.params.length > 0) existing.params = c.params;
      if (c.response) existing.response = c.response;
      if (c.sample) existing.sample = c.sample;
      // Merge keywords, dedup.
      existing.keywords = [...new Set([...(existing.keywords || []), ...(c.keywords || [])])];
      existing._curated = true;
      applied += 1;
    } else {
      // Curated-only endpoint (e.g. new one not yet in scan).
      byId.set(id, {
        id,
        domain: c.domain,
        path: c.path,
        method,
        purpose: c.purpose,
        params: c.params || [],
        response: c.response || 'object',
        sample: c.sample,
        keywords: extractKeywords(c.purpose || '', c.path, c.keywords || []),
        source: c.source || 'curated',
        _extras: {},
        _curated: true,
      });
      created += 1;
    }
  }
  return { records: [...byId.values()], applied, created };
}

function loadCuratedFiles() {
  if (!existsSync(CURATED_DIR)) return [];
  return readdirSync(CURATED_DIR)
    .filter((f) => f.endsWith('.json') && f !== 'allowlist.json')
    .map((f) => readJson(join(CURATED_DIR, f)))
    .filter(Boolean);
}

// -----------------------------------------------------------------------------
// Index-time write filter —— 索引级剥离写动词
// scanner 已经只挑 GetMapping，但 path 里的写动词命名（如 /user/delete、
// /createUser、/edit-role）依然可能进来。我们在**建索引这一步**就把它们扔掉，
// 保证 LLM 通过 api_search 根本看不到这些接口，从源头杜绝调用可能。
//
// 判定：任何一个 path segment 满足以下任一条件就整条 endpoint drop：
//   1. 整段等于写动词  ── /user/delete
//   2. camelCase 起头   ── /createUser、/editById
//   3. kebab-case 分段  ── /reset-password、/enable-user、/user-delete
//
// 假阳性可接受：如果一个读接口被命名成 "createNameList" 这种，用户也说过要一起去。
// -----------------------------------------------------------------------------
const WRITE_VERBS = [
  'delete', 'remove', 'del',
  'update', 'edit', 'modify', 'change',
  'save', 'insert', 'create', 'add', 'new',
  'reset', 'enable', 'disable', 'toggle',
  'upload', 'import',
  // 导出/下载/打印类：我们有自己的 export_excel 工具，不用后端触发（后端 export
  // 可能有并发限制/生成异步任务等副作用，不适合 agent 无脑调）
  'export', 'download', 'print',
  'refund', 'withdraw', 'transfer', 'pay', 'topup', 'recharge',
  'register', 'logout', 'signup', 'signout',
  'publish', 'unpublish', 'kill', 'stop', 'restart',
  'send', 'push',
  'approve', 'reject', 'confirm', 'cancel', 'revoke', 'reopen',
  'submit', 'assign', 'unassign', 'reassign',
  'grant', 'deny', 'revoke',
];
const WRITE_VERBS_SET = new Set(WRITE_VERBS);
// **大小写敏感**：camelCase 判定要求"小写动词 + 大写字母"，避免 payment/address 假阳性。
// Ruoyi 惯例 saveUser / deleteById 全部动词小写起头，大写字母紧跟，正好命中。
const WRITE_CAMEL_RE = new RegExp(`^(?:${WRITE_VERBS.join('|')})[A-Z]`);
const WRITE_KEBAB_RE = new RegExp(`(?:^|-)(?:${WRITE_VERBS.join('|')})(?:$|-)`, 'i');

function pathIsWriteShaped(path) {
  if (!path) return false;
  const segs = path.split('/').filter(Boolean);
  for (const raw of segs) {
    // 去除 {pathParam}、$pathParam
    if (raw.startsWith('{') || raw.startsWith('$')) continue;
    const seg = raw.toLowerCase();
    if (WRITE_VERBS_SET.has(seg)) return true;
    if (WRITE_CAMEL_RE.test(raw)) return true;
    if (WRITE_KEBAB_RE.test(seg)) return true;
  }
  return false;
}

// -------- 中文语义级过滤 --------
// 有些接口标了 @GetMapping 但 Java 方法真实做写（发短信/注册/新增数据），Javadoc
// 里 purpose 会暴露真相。用**强写动词**（不含歧义的名词/形容词）过一遍 purpose 文本。
//
// 不用弱词（如 "标记" / "归档" / "发起" 可作名词 / 形容词 / 历史事件），避免误伤：
//   "标记类型提醒时间" — 标记名词
//   "回访完成之后发起售后工单记录" — 历史事件
// 只保留**动词开头**或**强动作**词。
// 只列**明确的动词开头**词。有歧义的名词（付款单/退款记录/发起报销的历史）不列。
// 财务名词（付款/打款/充值/退款/扣款/转账）在中文里几乎全是名词化，不当动词，剔除。
const WRITE_ZH_STRONG = [
  '新增', '新建', '创建', '添加',
  '删除', '移除', '删掉',
  '修改', '编辑', '变更',
  '保存', '更新', '重置',
  '提交', '发送', '推送', '上传', '导入', '发布',
  '注册', '注销',
  '绑定', '解绑',
  '发送短信', '发送邮件', '推送消息',
  '生成代码', '执行任务', '测试运行',
  '审批通过', '审批驳回', '通过审批', '驳回申请',
];

function purposeIsWrite(purpose) {
  if (!purpose) return false;
  const p = String(purpose).trim();
  return WRITE_ZH_STRONG.some((kw) => p.includes(kw));
}

function filterOutWrites(records) {
  const kept = [];
  const dropped = [];
  for (const r of records) {
    if ((r.method || 'GET').toUpperCase() !== 'GET') { dropped.push({ r, why: 'non-GET' }); continue; }
    if (pathIsWriteShaped(r.path)) { dropped.push({ r, why: 'path-verb' }); continue; }
    if (purposeIsWrite(r.purpose)) { dropped.push({ r, why: 'purpose-verb' }); continue; }
    kept.push(r);
  }
  return { kept, dropped: dropped.map((x) => x.r), droppedByReason: dropped };
}

// Attack-surface reduction: `_allowed` decides whether api_call will execute
// this endpoint. api_search still surfaces everything (so the LLM sees the full
// index and can request promotion), but call attempts on non-allowed paths get
// a clear 4xx-shaped rejection. See curated/allowlist.json for policies.
function applyAllowlist(records, policy) {
  if (!policy) {
    for (const r of records) r._allowed = true;
    return { allowed: records.length, denied: 0 };
  }
  const policies = policy.policies || {};
  let allowed = 0;
  let denied = 0;
  for (const r of records) {
    const p = policies[r.domain];
    if (!p) {
      r._allowed = false;
      denied += 1;
      continue;
    }
    if (p.mode === 'allow_all') {
      // 默认允许，除非命中 deny_prefixes 或 deny_keywords
      // deny_prefixes: 精确路径前缀（如 "/admin/setting/"）
      // deny_keywords: path 里出现就拒（写动词专用："delete/remove/update/edit/save/create/insert"）
      const denyPrefixes = p.deny_prefixes || [];
      const denyKeywords = p.deny_keywords || [];
      const pLower = r.path.toLowerCase();
      const denied1 = denyPrefixes.some((pfx) => r.path === pfx || r.path.startsWith(pfx));
      const denied2 = denyKeywords.some((kw) => pLower.includes(String(kw).toLowerCase()));
      r._allowed = !denied1 && !denied2;
      if (r._allowed) allowed += 1;
      else denied += 1;
      continue;
    }
    if (p.mode === 'explicit') {
      const prefixes = p.allow_prefixes || [];
      r._allowed = prefixes.some((pfx) => r.path === pfx || r.path.startsWith(pfx));
      if (r._allowed) allowed += 1;
      else denied += 1;
      continue;
    }
    r._allowed = false;
    denied += 1;
  }
  return { allowed, denied };
}

function main() {
  const oaRaw = readJson(OA_RAW);
  const ordersysRaw = readJson(ORDERSYS_RAW);

  if (!oaRaw) throw new Error(`OA index missing: ${OA_RAW}. Run generate-oa-api-index.mjs first.`);
  if (!ordersysRaw) throw new Error(`ordersys raw missing: ${ORDERSYS_RAW}. Run scan-ordersys-api.mjs first.`);

  const oaRecords = (oaRaw.endpoints || []).map(buildOaRecord);
  let ordersysRecords = (ordersysRaw.endpoints || []).map(buildOrdersysRecord);

  // Java 是**唯一真相源**：Vue 里的路径可能过时/重命名（前端老代码没删），
  // 只保留 Java @GetMapping 里真实存在的路径。避免 LLM 撞 404。
  //
  // 关键：ordersys 是**多模块微服务**，不同模块走不同网关：
  //   - order-admin-api  → https://order.liantucn.com/api/*   ✅ 我们能访问
  //   - order-mall-api   → https://mall.liantucn.com/api/*    ❌ 不同网关
  //   - order-seller-api → https://seller.liantucn.com/api/*  ❌
  //   - order-outlets-api → 门店端另一个网关                    ❌
  // 只保留 order-admin-api（和 order-framework 公共模块）避免 LLM 撞 404。
  const ORDERSYS_INCLUDE_MODULES = new Set(['order-admin-api', 'order-framework']);
  const ordersysJavaRaw = readJson(ORDERSYS_JAVA);
  if (ordersysJavaRaw) {
    const javaById = new Map();
    let moduleFiltered = 0;
    for (const ep of ordersysJavaRaw.endpoints || []) {
      // 判定模块归属
      const src = ep.source || '';
      const m = src.match(/\/ordersys\/([^/]+)\//);
      const mod = m ? m[1] : 'unknown';
      if (!ORDERSYS_INCLUDE_MODULES.has(mod)) { moduleFiltered += 1; continue; }
      const rec = buildOrdersysJavaRecord(ep);
      javaById.set(rec.id, rec);
    }
    console.log(`ordersys module filter: kept ${javaById.size}, dropped ${moduleFiltered} (mall/seller/outlets 走不同网关)`);
    const beforeCount = ordersysRecords.length;
    const kept = [];
    let vueOnlyDropped = 0;
    for (const rec of ordersysRecords) {
      const java = javaById.get(rec.id);
      if (java) {
        // Vue + Java 都有：以 Java purpose/params 为准，保留 Vue 的 keywords
        if (java.purpose) rec.purpose = java.purpose;
        if (java.params.length > 0) rec.params = java.params;
        if (java.source) rec.source = java.source;
        javaById.delete(rec.id);
        kept.push(rec);
      } else {
        // Vue-only —— 后端已经没了这个接口，扔掉避免 LLM 撞 404
        vueOnlyDropped += 1;
      }
    }
    // Java 里独有（后端有但 Vue 里没调过的）也加进来
    const javaOnly = [...javaById.values()];
    ordersysRecords = [...kept, ...javaOnly];
    console.log(`ordersys java-source-of-truth: ${beforeCount} vue → ${kept.length} matched + ${javaOnly.length} java-only = ${ordersysRecords.length}, dropped ${vueOnlyDropped} vue-only stale paths`);
  }

  let records = [...oaRecords, ...ordersysRecords];

  // Deduplicate by id — later wins (should not normally collide).
  const seen = new Map();
  for (const r of records) seen.set(r.id, r);
  records = [...seen.values()];

  // Apply hand-curated overlays.
  const curatedFiles = loadCuratedFiles();
  let totalApplied = 0;
  let totalCreated = 0;
  for (const overlay of curatedFiles) {
    const result = applyCuratedOverlay(records, overlay);
    records = result.records;
    totalApplied += result.applied;
    totalCreated += result.created;
  }

  records.sort((a, b) => {
    if (a.domain !== b.domain) return a.domain < b.domain ? -1 : 1;
    return a.path < b.path ? -1 : 1;
  });

  // ★ 索引级剥离：写动词整段/驼峰/连字符命名的 endpoint 直接扔掉
  //   LLM 通过 api_search 根本看不到，从源头杜绝调用可能。
  const beforeCount = records.length;
  const { kept, dropped } = filterOutWrites(records);
  records = kept;
  console.log(`write-verb filter: dropped ${dropped.length}/${beforeCount} (kept ${kept.length})`);
  if (dropped.length > 0) {
    const preview = dropped.slice(0, 8).map((r) => r.path).join(', ');
    console.log(`  sample dropped: ${preview}${dropped.length > 8 ? ' ...' : ''}`);
  }

  const allowlistPolicy = readJson(ALLOWLIST, null);
  const allowStats = applyAllowlist(records, allowlistPolicy);

  const oaCount = records.filter((r) => r.domain === 'oa').length;
  const ordersysCount = records.filter((r) => r.domain === 'ordersys').length;
  const curatedCount = records.filter((r) => r._curated).length;
  const ordersysAllowed = records.filter((r) => r.domain === 'ordersys' && r._allowed).length;

  const output = {
    meta: {
      generatedAt: new Date().toISOString(),
      totalEndpoints: records.length,
      oaCount,
      ordersysCount,
      curatedOverlayApplied: totalApplied,
      curatedOverlayCreated: totalCreated,
      curatedTotal: curatedCount,
      allowedTotal: allowStats.allowed,
      deniedTotal: allowStats.denied,
      ordersysAllowed,
      schemaVersion: 1,
      readOnly: true,
      note: 'Unified endpoint index. Consumed by src/apiIndex.js at boot. See docs/api-index-schema.md.',
    },
    endpoints: records,
  };

  writeFileSync(OUTPUT, JSON.stringify(output, null, 2));
  console.log(`OA: ${oaCount}, ordersys: ${ordersysCount}, total: ${records.length}`);
  console.log(`curated overlay: applied=${totalApplied} created=${totalCreated}`);
  console.log(`allowlist: allowed=${allowStats.allowed}, denied=${allowStats.denied}`);
  console.log(`  ordersys allowed: ${ordersysAllowed}/${ordersysCount}`);
  console.log(`output: ${OUTPUT}`);
}

main();
