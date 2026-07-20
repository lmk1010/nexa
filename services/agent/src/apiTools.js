// Two-tool harness surface: api_search + api_call. Everything else is dead
// weight for the LLM. Retrieval and execution are the only primitives it
// needs — the LLM does all reasoning about which endpoint to pick and when
// to fan out.

import { tool } from '@mk-co/neox-sdk';
import { z } from 'zod';
import { search, getByPath, stats } from './apiIndex.js';
import { callInternalApi } from './tools.js';
import { ordersysGet } from './ordersysClient.js';
import { shortCircuit, remember } from './hotErrorCache.js';
import { logApiCall } from './auditLogger.js';
import { offloadIfLarge } from './workspace.js';

const BATCH_MAX = 8;

function normalizeResult(response, action) {
  return {
    ok: response.ok,
    status: response.status,
    action,
    code: response.data?.code,
    msg: response.data?.msg,
    data: response.data?.data ?? response.data,
    truncated: response.truncated,
  };
}

// ------------- 服务端 pluck / where -------------
// 目的：LLM 只关心的字段过一遍 —— API 返回可能 40 字段，LLM 只要 5 个
// 好处：offload 落盘的数据更小，python 处理更快，磁盘节省 80%+
function applyPostFilters(data, { pluck, where }) {
  if (!pluck && !where) return data;
  // 找到数组：直接是 array / obj.list / obj.rows / obj.records
  const pickArrayRef = (d) => {
    if (Array.isArray(d)) return { arr: d, setArr: (a) => a };
    if (d && Array.isArray(d.list)) return { arr: d.list, setArr: (a) => ({ ...d, list: a }) };
    if (d && Array.isArray(d.rows)) return { arr: d.rows, setArr: (a) => ({ ...d, rows: a }) };
    if (d && Array.isArray(d.records)) return { arr: d.records, setArr: (a) => ({ ...d, records: a }) };
    return null;
  };
  const ref = pickArrayRef(data);
  if (!ref) return data;
  let arr = ref.arr;
  // where —— 简单的 {field: value} 或 {field: {op: 'gt', value: 10}}
  if (where && typeof where === 'object') {
    arr = arr.filter((row) => {
      if (row == null || typeof row !== 'object') return false;
      for (const [k, cond] of Object.entries(where)) {
        const v = row[k];
        if (cond != null && typeof cond === 'object' && 'op' in cond) {
          const { op, value } = cond;
          switch (op) {
            case 'eq': if (v !== value) return false; break;
            case 'ne': if (v === value) return false; break;
            case 'gt': if (!(v > value)) return false; break;
            case 'gte': if (!(v >= value)) return false; break;
            case 'lt': if (!(v < value)) return false; break;
            case 'lte': if (!(v <= value)) return false; break;
            case 'in': if (!Array.isArray(value) || !value.includes(v)) return false; break;
            case 'contains': if (typeof v !== 'string' || !v.includes(String(value))) return false; break;
            default: return false;
          }
        } else if (v !== cond) return false;
      }
      return true;
    });
  }
  // pluck —— 只留指定字段（支持 rename：{from: 'total_money', to: '总金额'}）
  if (Array.isArray(pluck) && pluck.length > 0) {
    arr = arr.map((row) => {
      if (row == null || typeof row !== 'object') return row;
      const out = {};
      for (const p of pluck) {
        if (typeof p === 'string') out[p] = row[p];
        else if (p && typeof p === 'object' && p.from) out[p.to || p.from] = row[p.from];
      }
      return out;
    });
  }
  return ref.setArr(arr);
}

async function executeOne({ path, params, pluck, where, method = 'GET' }, context, signal) {
  const startedAt = Date.now();
  // Wrap the whole thing so every terminal path — early reject, cached failure,
  // real call, exception — ends up in the audit log exactly once. Fire-and-
  // forget: never awaits, never fails the caller if MySQL is unhappy.
  const audit = (result, domain) =>
    logApiCall(context, {
      path: result?.request?.path || path,
      method: (result?.request?.method || method || 'GET').toUpperCase(),
      params: result?.request?.params ?? params,
      domain: result?.request?.domain ?? domain ?? null,
      ok: !!result?.ok,
      code: result?.code ?? null,
      responseBytes: (() => {
        try {
          return result?.data ? JSON.stringify(result.data).length : 0;
        } catch {
          return null;
        }
      })(),
      durationMs: Date.now() - startedAt,
    });

  const record = getByPath(path, method);
  if (!record) {
    const r = {
      ok: false,
      status: 0,
      code: 'NOT_IN_INDEX',
      msg: `path 不在索引: ${method} ${path}. 用 api_search 找一下真实 path。`,
      request: { path, method, params },
    };
    audit(r);
    return r;
  }

  if (record._allowed === false) {
    const r = {
      ok: false,
      status: 0,
      code: 'NOT_ALLOWLISTED',
      msg: `该接口暂不在 agent 白名单：${record.path}。原因通常是明细报表 / 财务敏感 / 系统配置类，未开放给 agent 使用。请换一个 api_search 关键词找统计聚合类接口（cards 里 callable:true 的才能调），或直接告诉用户这个能力暂未开放。`,
      request: { path, method, params, domain: record.domain },
    };
    audit(r, record.domain);
    return r;
  }

  const cached = shortCircuit(context, record.path);
  if (cached) {
    const r = { ...cached, request: { path: record.path, method, params, domain: record.domain } };
    audit(r, record.domain);
    return r;
  }

  const domain = record.domain;
  const query = params && typeof params === 'object' ? params : undefined;

  try {
    let result;
    if (domain === 'oa') {
      const response = await callInternalApi(method, record.path, context, { query, signal });
      result = {
        ...normalizeResult(response, `api_call ${record.path}`),
        request: { path: record.path, method, params: query, domain },
      };
    } else if (domain === 'ordersys') {
      result = {
        ...(await ordersysGet(context, record.path, query, `api_call ${record.path}`, signal)),
        request: { path: record.path, method, params: query, domain },
      };
    } else {
      const r = {
        ok: false,
        status: 0,
        code: 'UNKNOWN_DOMAIN',
        msg: `domain=${domain} 未支持`,
        request: { path, method, params, domain },
      };
      audit(r, domain);
      return r;
    }
    remember(context, record.path, { status: result.status, data: { code: result.code, msg: result.msg } });
    audit(result, domain);
    // 服务端 pluck/where —— 先剪字段过滤，再判断是否 offload
    // 这样 offload 落盘的是已经瘦身的数据，磁盘/上下文双省
    if (result.ok && (pluck || where)) {
      const beforeSize = JSON.stringify(result.data || {}).length;
      result.data = applyPostFilters(result.data, { pluck, where });
      result._filtered = { pluck: pluck ? pluck.length : 0, where: where ? Object.keys(where).length : 0, bytes_before: beforeSize };
    }
    if (result.ok && result.data != null) {
      const pathTag = record.path.split('/').filter(Boolean).slice(-2).join('_') || 'api';
      const offloaded = await offloadIfLarge(
        context?.conversationId || context?.requestId,
        result.data,
        pathTag,
      );
      if (offloaded) {
        result = { ...result, data: offloaded, _offloaded: true };
      }
    }
    return result;
  } catch (err) {
    const r = {
      ok: false,
      status: 0,
      code: 'CALL_ERROR',
      msg: err instanceof Error ? err.message : String(err),
      request: { path, method, params, domain },
    };
    audit(r, domain);
    return r;
  }
}

export function apiSearchTool() {
  return tool({
    name: 'api_search',
    description: [
      '按自然语言在 KYX + 连图 ordersys 的接口索引里检索，返回 top-k 端点卡片。',
      '',
      '每张卡片含：path / method / purpose 一句话业务定义 / params 结构化列表 / response 关键字段 / sample 示例 / source 追溯 / **callable 是否可 api_call**。',
      '',
      '规则：',
      '- 不知道调什么就直接 search，别自己猜 path。',
      '- 搜到结果不满意可以换关键词再 search（没有次数限制、没有惩罚）。',
      '- 用 domain 缩小范围：`domain:"oa"` 查 KYX/OA 内部；`domain:"ordersys"` 查连图。',
      '- 拿到 path 后用 api_call 执行；多个独立读取用 api_call 的 batch 一次发出。',
      '- **卡片里 `callable:false` 的接口 api_call 会被拒（明细报表 / 财务敏感 / 系统配置类没开放给 agent），只挑 callable:true 的用**。真的没有可用接口就告诉用户该能力暂未开放。',
    ].join('\n'),
    schema: z
      .object({
        query: z.string().min(1).max(200).describe('自然语言问题，如「查技术部本月赔付」「找超时工单列表」'),
        k: z.number().int().min(1).max(20).optional().describe('返回条数，默认 8'),
        domain: z.enum(['oa', 'ordersys']).optional().describe('只查某个域时用；不传就跨域检索'),
      })
      .strict(),
    handler: async (input) => {
      const hits = await search(input.query, { k: input.k || 8, domain: input.domain });
      return {
        query: input.query,
        domain: input.domain || 'all',
        hits,
        stats: stats(),
      };
    },
  });
}

const pluckSchema = z.union([
  z.string(),
  z.object({ from: z.string(), to: z.string() }),
]);

const whereSchema = z.record(z.any()).describe(
  '服务端过滤：{field:value} 精确匹配 / {field:{op:"gte",value:100}} 支持 op: eq/ne/gt/gte/lt/lte/in/contains',
);

const singleCallSchema = z
  .object({
    path: z.string().min(1),
    method: z.enum(['GET']).optional(),
    params: z.any().optional().describe('query 参数 JSON object。日期区间用数组 ["2026-07-01","2026-07-31"]'),
    pluck: z.array(pluckSchema).optional().describe('只留这些字段（服务端剪，减少落盘/上下文体积）'),
    where: whereSchema.optional(),
  })
  .strict();

export function apiCallTool(context) {
  return tool({
    name: 'api_call',
    description: [
      '执行 GET 接口。path 必须是 api_search 返回过的真实 path — 工具会在索引里核对，不在就直接拒绝。',
      '',
      '## 参数',
      '- `path` + `params`：单调用',
      `- \`batch:[{path,params,pluck?,where?}, ...]\`：并发批量（最多 ${BATCH_MAX}）。**多个独立读取一定要 batch，别串行**`,
      '- `pluck`（重要）：**服务端字段剪裁**。列出你要的字段，其他丢掉。返回体从 40 字段 → 5 字段，上下文/磁盘省 80%。支持重命名 `{from:"total_money", to:"金额"}`',
      '- `where`（重要）：**服务端行过滤**。`{status:1}` 精确匹配；`{money:{op:"gte",value:1000}}` 大于等于；op 还支持 ne/gt/lt/lte/in/contains',
      '',
      '## 何时用 pluck/where',
      '- 用户问"技术部人员" → pluck: ["id","name","nickname","deptName"]（别拿 40 字段）',
      '- 只要金额 > 1000 → where: {payAmount: {op:"gt", value:1000}}',
      '- 拿完先自己过滤浪费两遍：**能在服务端做就在服务端做**',
      '',
      '## 规则',
      '- 只支持 GET',
      '- 大响应自动落盘到 workspace，返回 `{file, preview, rows}`',
      '- 403/503 = 权限或服务未部署，不 retry',
    ].join('\n'),
    schema: z
      .object({
        path: z.string().min(1).optional(),
        method: z.enum(['GET']).optional(),
        params: z.any().optional().describe('query 参数 JSON object。日期区间用数组 ["2026-07-01","2026-07-31"]'),
        pluck: z.array(pluckSchema).optional().describe('只留这些字段（服务端剪，减少落盘体积）'),
        where: whereSchema.optional().describe('服务端行过滤'),
        batch: z.array(singleCallSchema).min(1).max(BATCH_MAX).optional(),
      })
      .strict()
      .refine((value) => Boolean(value.batch?.length || value.path), {
        message: 'path 或 batch 至少一个必填',
      }),
    handler: async (input, toolContext) => {
      const signal = toolContext?.signal;
      if (Array.isArray(input.batch) && input.batch.length > 0) {
        const results = await Promise.all(
          input.batch.map(async (call, index) => {
            const result = await executeOne(call, context, signal);
            return { index, ...result };
          }),
        );
        return {
          batch: true,
          count: results.length,
          ok: results.every((r) => r.ok),
          results,
        };
      }
      return executeOne(
        { path: input.path, method: input.method || 'GET', params: input.params },
        context,
        signal,
      );
    },
  });
}

// ------------- paginate_all -------------
// 分页神器：给一个 /page 或 /list 接口，agent 内部循环把所有页拉完合并
// 用户"导出全部花名册"这类场景，LLM 一次调用就完事，不用自己算 pageNo 循环
// 也不会触发 REPEAT_GUARD（因为 params 不同）
export function paginateAllTool(context) {
  return tool({
    name: 'paginate_all',
    description: [
      '**分页神器**：给一个 /page 或 /list 类接口，工具内部循环拉完所有页 → 合并为一个数组返回。',
      '',
      '**优先于自己循环调 api_call**。用户说"导出全部/所有 X"、你看到 total > pageSize、需要完整数据 → 直接调这个。',
      '',
      '参数：',
      '- `path` (必)：接口路径（要在索引里，且卡片 params 含 pageNo/pageSize 或 pageNum/pageSize）',
      '- `pageSize` (可，默认 50)：每页条数，最大 100',
      '- `maxPages` (可，默认 20)：最多拉几页；防止无脑扫全库。**超过就停并告诉用户"数据超过 X 条，请缩范围"**',
      '- `params` (可)：其他 query 参数（keyword、时间范围等）',
      '- `dataPath` (可，默认 `data.list`)：从响应体里取"这一页数据数组"的 jq 路径（Ruoyi 是 `data.list` 或 `data.rows` 或 `rows`）',
      '',
      '返回：',
      '- `list`：合并后的完整数组',
      '- `total`：总条数（如果接口返了 total 字段）',
      '- `pages_fetched`：实际拉了多少页',
      '- `truncated`：true 表示 maxPages 顶到了',
    ].join('\n'),
    schema: z.object({
      path: z.string().min(1),
      pageSize: z.number().int().min(1).max(100).default(50),
      maxPages: z.number().int().min(1).max(50).default(20),
      params: z.record(z.any()).default({}),
      dataPath: z.string().default('data.list'),
      pluck: z.array(pluckSchema).optional().describe('每页服务端剪字段（省磁盘/上下文）'),
      where: whereSchema.optional().describe('每页服务端行过滤'),
    }).strict(),
    handler: async ({ path, pageSize, maxPages, params, dataPath, pluck, where }, toolContext) => {
      const signal = toolContext?.signal;
      const record = getByPath(path, 'GET');
      if (!record) {
        return { ok: false, code: 'NOT_IN_INDEX', msg: `path 不在索引: ${path}` };
      }
      // 探测分页字段名：pageNo 还是 pageNum
      const paramNames = new Set((record.params || []).map((p) => p.name));
      const pageNoField = paramNames.has('pageNo') ? 'pageNo' : (paramNames.has('pageNum') ? 'pageNum' : null);
      if (!pageNoField) {
        return {
          ok: false,
          code: 'NO_PAGINATION',
          msg: `接口 ${path} 卡片里没声明 pageNo/pageNum 字段，可能不支持分页。用 api_call 直接调试试。`,
        };
      }
      const list = [];
      let total = null;
      let page = 1;
      let truncated = false;
      let lastError = null;
      while (page <= maxPages) {
        const merged = { ...params, [pageNoField]: page, pageSize };
        const res = await executeOne({ path, method: 'GET', params: merged }, context, signal);
        if (!res.ok) { lastError = res; break; }
        // 沿 dataPath 取当页数据
        const rows = dataPath.split('.').reduce((acc, k) => (acc == null ? acc : acc[k]), res.data);
        // 也拿 total（大多数 Ruoyi 接口是 data.total）
        if (total == null) {
          const tot = res.data?.data?.total ?? res.data?.total;
          if (Number.isFinite(tot)) total = Number(tot);
        }
        if (!Array.isArray(rows) || rows.length === 0) break;
        // 每页立即 pluck/where 剪一遍再累加，累计内存不爆
        const filtered = applyPostFilters({ list: rows }, { pluck, where });
        const kept = filtered.list || rows;
        list.push(...kept);
        if (rows.length < pageSize) break; // 最后一页
        page += 1;
      }
      if (page > maxPages) truncated = true;
      // 完整 list 自动 offload —— 只给 LLM 前 3 行 + count + file 路径
      const pathTag = path.split('/').filter(Boolean).slice(-2).join('_') || 'paged';
      const offloaded = await offloadIfLarge(
        context?.conversationId || context?.requestId,
        list,
        pathTag,
      );
      return {
        ok: !lastError,
        list: offloaded || list,
        total,
        pages_fetched: page - 1 + (truncated ? 1 : 0),
        truncated,
        error: lastError,
        note: truncated
          ? `拉取了 ${maxPages} 页 (${list.length} 条) 已到上限；如需更多请缩小筛选条件`
          : total != null && list.length < total
            ? `已拿 ${list.length}/${total}；如需完整请增大 maxPages 或缩范围`
            : undefined,
      };
    },
  });
}
