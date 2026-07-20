// KYX agent tool surface — thin. The harness rule: expose primitives, let
// the LLM reason. Only three "smart" tools are defined here:
//
//   • get_request_context — self-awareness
//   • render_chart       — the ONLY sanctioned chart-emission path
//   • fetch_url          — public web fetch, SSRF-guarded
//
// The two heavy hitters live next door in apiTools.js:
//
//   • api_search — hybrid retriever over the unified endpoint index
//   • api_call   — single/batch executor
//
// callInternalApi is exported so apiTools.js can dispatch to OA endpoints
// through the same forwarded-auth + per-user queue pipeline. Two thin
// wrappers (queryExecutiveCockpit*) stay for the HTTP-only cockpit routes
// in http.js — those are direct proxies, not agent tools.

import dns from 'node:dns/promises';
import net from 'node:net';
import { tool } from '@mk-co/neox-sdk';
import { z } from 'zod';
import { generateExcel, EXPORT_EXCEL_SCHEMA, uploadWorkspaceFile } from './exportTool.js';
import {
  nowTool,
  parseTimeTool,
  shellExecTool,
} from './foundationTools.js';
import { paginateAllTool } from './apiTools.js';
import { pythonExecTool } from './pythonExec.js';
import { config } from './config.js';
import { forwardHeaders, publicRequestContext } from './context.js';
import {
  getServiceAccountAuthHeaders,
  invalidateServiceAccountCredential,
} from './serviceAccountAuth.js';
import { createPerUserQueue } from './perUserQueue.js';
import { apiSearchTool, apiCallTool } from './apiTools.js';

const ADMIN_PREFIX = '/admin-api';
const EXECUTIVE_COCKPIT_BASE = `${ADMIN_PREFIX}/business/executive-cockpit`;

const internalApiQueue = createPerUserQueue({
  name: 'internal-api',
  maxGlobal: config.internalApi.maxConcurrentRequests,
  maxPerUser: config.internalApi.maxConcurrentRequestsPerUser,
  queueTimeoutMs: config.internalApi.queueTimeoutMs,
});

export function internalApiQueueSnapshot() {
  return internalApiQueue.snapshot();
}

export function _resetInternalApiQueueForTest() {
  internalApiQueue.reset();
}

function jsonLike(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function assertInternalPath(path) {
  if (!path.startsWith('/')) {
    throw new Error('Internal API path must start with /');
  }
  if (/^\/\//.test(path) || /^[a-z][a-z0-9+.-]*:/i.test(path)) {
    throw new Error('Absolute URLs are not allowed');
  }
  if (path.startsWith('/app-api/agent/') || path.startsWith('/admin-api/agent/')) {
    throw new Error('Calling the agent service through its own gateway route is not allowed');
  }
  if (!config.internalApi.allowPrefixes.some((prefix) => path.startsWith(prefix))) {
    throw new Error(`Internal API path must start with one of: ${config.internalApi.allowPrefixes.join(', ')}`);
  }
}

function appendQuery(url, query) {
  if (!query) return;
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined && item !== null && item !== '') {
          url.searchParams.append(key, String(item));
        }
      }
      continue;
    }
    url.searchParams.set(key, String(value));
  }
}

function enforceInternalToolBudget(context, action) {
  context.internalToolCallCount = (context.internalToolCallCount || 0) + 1;
  if (context.internalToolCallCount > config.internalApi.maxToolCallsPerRun) {
    throw new Error(
      `Internal API tool call limit exceeded: ${action} is over ${config.internalApi.maxToolCallsPerRun} calls per agent run`,
    );
  }
}

async function responseTextLimited(response) {
  const reader = response.body?.getReader();
  if (!reader) return { text: '', truncated: false };

  const chunks = [];
  let total = 0;
  let truncated = false;

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total <= config.internalApi.maxResponseBytes) {
      chunks.push(value);
      continue;
    }
    const remaining = config.internalApi.maxResponseBytes - (total - value.byteLength);
    if (remaining > 0) chunks.push(value.slice(0, remaining));
    truncated = true;
    await reader.cancel();
    break;
  }

  const bytes = Buffer.concat(chunks.map((chunk) => Buffer.from(chunk)));
  return { text: bytes.toString('utf8'), truncated };
}

export async function callInternalApi(method, path, context, { query, body, signal } = {}) {
  assertInternalPath(path);
  enforceInternalToolBudget(context, `${method} ${path}`);

  const url = new URL(path, config.internalApi.baseURL);
  appendQuery(url, query);

  const releaseSlot = await internalApiQueue.acquire(context, signal);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.internalApi.timeoutMs);
  const signals = [controller.signal];
  if (signal) signals.push(signal);

  try {
    const forwardedHeaders = forwardHeaders(context);
    let serviceAccountHeaders = await getServiceAccountAuthHeaders(context, signal);

    const execute = async (extraHeaders) => {
      const response = await fetch(url, {
        method,
        headers: {
          ...forwardedHeaders,
          ...extraHeaders,
          Accept: 'application/json',
          ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: AbortSignal.any(signals),
      });
      const { text, truncated } = await responseTextLimited(response);
      return {
        status: response.status,
        ok: response.ok,
        data: jsonLike(text),
        truncated,
      };
    };

    let result = await execute(serviceAccountHeaders);
    if (result.status === 401 && serviceAccountHeaders.Authorization) {
      invalidateServiceAccountCredential();
      serviceAccountHeaders = await getServiceAccountAuthHeaders(context, signal);
      result = await execute(serviceAccountHeaders);
    }
    return result;
  } finally {
    releaseSlot();
    clearTimeout(timer);
  }
}

function commonResultMessage(data) {
  if (!data || typeof data !== 'object') return '';
  return data.msg || data.message || data.error || '';
}

function normalizeInternalResult(response, action) {
  const code = typeof response.data?.code === 'number' ? response.data.code : response.status;
  const success = response.ok && (response.data?.code === undefined || response.data.code === 0);
  const permissionDenied = response.status === 401 || response.status === 403 || code === 401 || code === 403;

  if (!success) {
    return {
      ok: false,
      action,
      status: response.status,
      code,
      permissionDenied,
      message: commonResultMessage(response.data) || `KYX internal API failed with status ${response.status}`,
      data: response.data,
      truncated: response.truncated,
    };
  }
  return {
    ok: true,
    action,
    status: response.status,
    code: response.data?.code ?? 0,
    data: response.data?.data ?? response.data,
    truncated: response.truncated,
  };
}

async function businessGet(context, path, query, action, signal) {
  const response = await callInternalApi('GET', path, context, { query, signal });
  return normalizeInternalResult(response, action);
}

async function businessPost(context, path, body, action, signal) {
  const response = await callInternalApi('POST', path, context, { body, signal });
  return normalizeInternalResult(response, action);
}

// http.js proxies these two straight through for the Flutter cockpit tab.
// Not agent tools — direct HTTP surfaces.
export function queryExecutiveCockpitOverview(context, input = {}, signal) {
  return businessGet(
    context,
    `${EXECUTIVE_COCKPIT_BASE}/overview`,
    { days: input.days },
    'executive_cockpit_overview',
    signal,
  );
}

export function queryExecutiveCockpitChat(context, input, signal) {
  return businessPost(
    context,
    `${EXECUTIVE_COCKPIT_BASE}/chat`,
    input,
    'executive_cockpit_chat',
    signal,
  );
}

function getContextTool(context) {
  return tool({
    name: 'get_request_context',
    description: 'Get the current KYX request context without exposing secrets.',
    schema: z.object({}).strict(),
    handler: () => publicRequestContext(context),
    cacheable: true,
  });
}

// ============================================================================
// fetch_url — public web fetch with SSRF guards.
// ============================================================================

const FETCH_BLOCKED_HOSTS = new Set([
  'localhost',
  '127.0.0.1',
  '0.0.0.0',
  '::1',
  '169.254.169.254', // AWS/Aliyun instance metadata
  '100.100.100.200', // Aliyun metadata
]);
const FETCH_MAX_BYTES = 2 * 1024 * 1024;
const FETCH_TIMEOUT_MS = 15_000;
const FETCH_MAX_REDIRECTS = 3;

export function isPrivateOrLoopbackIP(addr) {
  const family = net.isIP(addr);
  if (family === 4) {
    const [a, b] = addr.split('.').map(Number);
    if (a === 0 || a === 10 || a === 127) return true;
    if (a === 169 && b === 254) return true;
    if (a === 172 && b >= 16 && b <= 31) return true;
    if (a === 192 && b === 168) return true;
    if (a === 100 && b >= 64 && b <= 127) return true;
    if (a >= 224) return true;
    return false;
  }
  if (family === 6) {
    const norm = addr.toLowerCase();
    if (norm === '::1' || norm === '::' || norm === '::0') return true;
    if (norm.startsWith('fc') || norm.startsWith('fd')) return true;
    if (norm.startsWith('fe8') || norm.startsWith('fe9') || norm.startsWith('fea') || norm.startsWith('feb')) return true;
    if (norm.startsWith('::ffff:')) return isPrivateOrLoopbackIP(norm.slice(7));
    return false;
  }
  return true;
}

export async function assertPublicHost(hostname) {
  if (!hostname) throw new Error('host missing');
  const lowered = hostname.toLowerCase();
  if (FETCH_BLOCKED_HOSTS.has(lowered)) {
    throw new Error(`host blocked: ${hostname}`);
  }
  if (net.isIP(hostname)) {
    if (isPrivateOrLoopbackIP(hostname)) {
      throw new Error(`private IP blocked: ${hostname}`);
    }
    return;
  }
  const results = await dns.lookup(hostname, { all: true });
  for (const r of results) {
    if (isPrivateOrLoopbackIP(r.address)) {
      throw new Error(`hostname ${hostname} resolves to private ${r.address}`);
    }
  }
}

export function extractPlainText(html) {
  return String(html)
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<\/?(br|p|div|section|article|li|h[1-6]|tr|hr|table)\b[^>]*>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function fetchUrlTool(_context) {
  return tool({
    name: 'fetch_url',
    description: '拉取一个公网 http/https URL 并返回正文文本（HTML 会自动去标签抽取正文）。用于阅读文章、公开文档、公开 API 描述页等。',
    schema: z
      .object({
        url: z.string().url(),
        maxChars: z.number().int().positive().max(20000).default(8000),
      })
      .strict(),
    handler: async (input, toolContext) => {
      const parsed = new URL(input.url);
      if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
        throw new Error(`protocol not allowed: ${parsed.protocol}`);
      }
      await assertPublicHost(parsed.hostname);

      let currentUrl = parsed.toString();
      let redirects = 0;
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);
      const signals = [controller.signal];
      if (toolContext?.signal) signals.push(toolContext.signal);

      try {
        while (true) {
          const response = await fetch(currentUrl, {
            method: 'GET',
            headers: {
              'User-Agent': 'KYX-Agent/1.0 (+internal)',
              Accept: 'text/html,text/plain,application/json;q=0.9,*/*;q=0.8',
            },
            redirect: 'manual',
            signal: AbortSignal.any(signals),
          });

          if (response.status >= 300 && response.status < 400) {
            redirects += 1;
            if (redirects > FETCH_MAX_REDIRECTS) throw new Error('too many redirects');
            const location = response.headers.get('location');
            if (!location) throw new Error('redirect without location');
            const nextUrl = new URL(location, currentUrl);
            if (nextUrl.protocol !== 'http:' && nextUrl.protocol !== 'https:') {
              throw new Error(`redirect protocol not allowed: ${nextUrl.protocol}`);
            }
            await assertPublicHost(nextUrl.hostname);
            currentUrl = nextUrl.toString();
            continue;
          }

          const contentType = response.headers.get('content-type') || '';
          const reader = response.body?.getReader();
          if (!reader) {
            return {
              ok: response.ok,
              status: response.status,
              finalUrl: currentUrl,
              contentType,
              text: '',
              truncated: false,
            };
          }

          const chunks = [];
          let total = 0;
          let truncated = false;
          while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            total += value.byteLength;
            if (total > FETCH_MAX_BYTES) {
              truncated = true;
              await reader.cancel();
              break;
            }
            chunks.push(Buffer.from(value));
          }
          const buffer = Buffer.concat(chunks);
          let text = buffer.toString('utf8');
          if (contentType.includes('html')) text = extractPlainText(text);
          if (text.length > input.maxChars) {
            text = text.slice(0, input.maxChars) + '\n…[truncated]';
            truncated = true;
          }
          return {
            ok: response.ok,
            status: response.status,
            finalUrl: currentUrl,
            contentType,
            text,
            truncated,
          };
        }
      } finally {
        clearTimeout(timer);
      }
    },
  });
}

// ============================================================================
// render_chart — sanctioned chart emitter. Handler validates the spec and
// echoes it back; the streaming layer picks it out of the args-capture queue
// and emits a `chart` event to the client.
// ============================================================================

function renderChartTool() {
  const dataPoint = z.object({
    label: z.string().min(1),
    value: z.number(),
    tone: z.string().optional(),
  });
  const lineSeries = z.object({
    name: z.string().optional(),
    points: z
      .array(
        z.object({
          x: z.union([z.string(), z.number()]),
          y: z.number(),
        }),
      )
      .min(1),
  });
  const schema = z.object({
    type: z.enum(['bar', 'pie', 'line', 'stat']),
    title: z.string().optional(),
    data: z.array(dataPoint).min(1).max(30).optional(),
    series: z.array(lineSeries).min(1).max(6).optional(),
    value: z.union([z.string().min(1), z.number()]).optional(),
    delta: z.string().optional(),
    tone: z.string().optional(),
  });

  return tool({
    name: 'render_chart',
    description: [
      '把图表画出来给用户看。**画图必须调这个工具**，不要往文本里塞 ```chart 代码块（客户端不解析那种）。每次要给一张图就调一次。数字必须来自你刚查到的 tool_result，不要脑补。',
      '',
      '### 用法示例',
      '**柱状图（bar）**：`render_chart({ type:"bar", title:"各部门在职人数", data:[{label:"技术部",value:9},{label:"业务部",value:14}] })`',
      '**饼图（pie）**：`render_chart({ type:"pie", title:"需求状态分布", data:[{label:"已完成",value:900},{label:"待验收",value:138}] })`',
      '**折线图（line）**：`render_chart({ type:"line", title:"近7天新增趋势", series:[{name:"新增",points:[{x:"7/1",y:32},{x:"7/2",y:15}]}] })`',
      '**指标卡（stat）**：`render_chart({ type:"stat", title:"闭环率", value:"78%", tone:"green" })`',
      '',
      '### 规则',
      '- bar / pie 必须传 `data` 数组（1~30 条），每条 `{label, value:数字}`',
      '- line 必须传 `series` 数组，每个 series 至少 1 个 point',
      '- stat 必须传 `value`（字符串或数字都行）',
      '- **值全为 0 或没数据就别调**，用文字或 markdown 表格说明即可',
    ].join('\n'),
    schema,
    handler: async (input) => {
      const t = input.type;
      if ((t === 'bar' || t === 'pie') && (!Array.isArray(input.data) || input.data.length === 0)) {
        throw new Error(`type="${t}" 必须传 data 数组（每条 {label, value:数字}），当前为空`);
      }
      if (t === 'line' && (!Array.isArray(input.series) || input.series.length === 0)) {
        throw new Error('type="line" 必须传 series 数组（每个 series 有 points:[{x,y}]），当前为空');
      }
      if (t === 'stat' && (input.value === undefined || input.value === '')) {
        throw new Error('type="stat" 必须传 value（字符串或数字）');
      }
      return { ok: true, rendered: true, type: t };
    },
  });
}

// ============================================================================
// Wrap each handler with two cross-cutting concerns:
//   • args-capture queue so SSE can attach parsed args to tool_result events
//   • Zod validation error rewrite — the SDK stringifies issue arrays as JSON,
//     which reads as noise to the LLM. Rewrite into actionable one-liners.
// ============================================================================

function formatZodIssuesForLlm(rawMessage) {
  try {
    const jsonStart = rawMessage.indexOf('[');
    if (jsonStart < 0) return rawMessage;
    const issues = JSON.parse(rawMessage.slice(jsonStart));
    if (!Array.isArray(issues) || issues.length === 0) return rawMessage;
    const lines = issues.map((issue) => {
      const path = Array.isArray(issue.path) ? issue.path.join('.') : issue.path;
      const pathText = path ? `字段 ${path}: ` : '';
      switch (issue.code) {
        case 'invalid_type':
          return `${pathText}期望 ${issue.expected}，实际收到 ${issue.received}`;
        case 'too_small':
          return `${pathText}${issue.type === 'string' ? '不能为空字符串' : `不小于 ${issue.minimum}`}`;
        case 'too_big':
          return `${pathText}不能超过 ${issue.maximum}`;
        case 'invalid_enum_value':
          return `${pathText}只能取 ${JSON.stringify(issue.options)}`;
        case 'unrecognized_keys':
          return `多传了不认识的字段：${(issue.keys || []).join(', ')}`;
        default:
          return `${pathText}${issue.message || issue.code}`;
      }
    });
    return `参数校验失败（改这里就能通过）：\n- ${lines.join('\n- ')}`;
  } catch {
    return rawMessage;
  }
}

function wrapWithArgsCapture(t, argsQueue) {
  const cfg = t?.config;
  if (!cfg || typeof cfg.handler !== 'function') return t;
  const original = cfg.handler;
  cfg.handler = (input, toolContext) => {
    try {
      argsQueue.push({ name: t.name, args: input, at: Date.now() });
      if (argsQueue.length > 64) argsQueue.shift();
    } catch {
      // don't let capture errors break the tool
    }
    return original(input, toolContext);
  };
  if (typeof t.invoke === 'function') {
    const originalInvoke = t.invoke;
    t.invoke = async (input, ctx) => {
      try {
        return await originalInvoke(input, ctx);
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        if (msg.includes('input validation failed')) {
          const rewritten = formatZodIssuesForLlm(msg);
          if (rewritten !== msg) throw new Error(rewritten);
        }
        throw err;
      }
    };
  }
  return t;
}

// 从 workspace 里的文件（python 写出的 xlsx/csv）直接上传，返回下载链接
// 比 export_excel 更快：xlsxwriter constant_memory 100 万行不占内存
function exportFileTool(context) {
  return tool({
    name: 'export_excel_from_file',
    description: [
      '**把 workspace 里的一个文件（xlsx/csv/pdf）上传给用户下载**，返回下载链接。',
      '',
      '**典型用法**：你在 python_exec 里用 xlsxwriter 写好了 `out.xlsx` → 调 `export_excel_from_file({file:"out.xlsx", filename:"7月赔付明细"})` → 得到下载 URL。',
      '',
      '**优势**：xlsxwriter constant_memory 模式流式写盘，100 万行也不占内存；比 export_excel 直接传 rows 快 10 倍。',
      '',
      '**返回**：`{url, markdown}` —— 把 markdown 字段原样贴到回答里。',
    ].join('\n'),
    schema: z.object({
      file: z.string().min(1).describe('workspace 里的文件名，比如 "out.xlsx"'),
      filename: z.string().optional().describe('给用户看的下载文件名（不带后缀会自动加 .xlsx）'),
    }).strict(),
    async handler(input) {
      return uploadWorkspaceFile({
        file: input.file,
        filename: input.filename,
        context,
        conversationId: context?.conversationId,
      });
    },
  });
}

function exportExcelTool(context) {
  return tool({
    name: 'export_excel',
    description: [
      '把数据表导出为 xlsx 文件，返回下载链接（7天有效）。用户说"导出/下载/给我 Excel"时用。',
      '**用法**：`export_excel({ rows: [{...}, ...], columns: [{key,header,money?,numeric?}], filename:"7月赔付明细" })`',
      '- **rows**：数据数组，每行一个对象',
      '- **columns**（可选）：列定义。字段：`key`（对应 rows 里的字段名）、`header`（表头显示名）、`money:true`（金额格式带千分符 + 2位小数）、`numeric:true`（整数带千分符）',
      '- **filename**（可选）：文件名（不带 .xlsx 后缀）',
      '- 返回 `{url, rows, bytes}` —— 给用户输出时**必须用 markdown 链接语法**：`[点击下载 XXX.xlsx](url)`，直接贴 URL 前端点不了',
      '- **禁忌**：不要把 api_call 的 raw 响应 body 硬塞进来 —— 先字段挑选/重命名成人看得懂的中文列名再导出',
    ].join('\n'),
    schema: EXPORT_EXCEL_SCHEMA,
    async handler(input) {
      const rows = Array.isArray(input?.rows) ? input.rows : [];
      if (rows.length === 0) {
        throw new Error('rows 为空，没有可导出的数据');
      }
      if (rows.length > 50000) {
        throw new Error(
          `导出 ${rows.length} 行超出上限 50000，请先在 api_call 侧收敛`,
        );
      }
      return generateExcel({
        rows,
        columns: input?.columns,
        filename: input?.filename,
        sheetName: input?.sheetName,
        context,
        conversationId: context?.conversationId,
      });
    },
  });
}

export function createTools(context) {
  const argsQueue = [];
  context._toolArgsQueue = argsQueue;

  const rawTools = [
    // Foundation —— 只留 LLM 真正常用的时间/shell；jq/pick/math/format 已被 python_exec 取代
    nowTool(),
    parseTimeTool(),
    shellExecTool(),
    // Compute —— 通用数据处理层，取代所有粗粒度 jq/pick/math 工具
    pythonExecTool(context),
    // Data
    getContextTool(context),
    apiSearchTool(),
    apiCallTool(context),
    paginateAllTool(context),
    // Output
    renderChartTool(),
    fetchUrlTool(context),
    exportExcelTool(context),         // 老 API：接受 rows[]（兼容惯性）
    exportFileTool(context),           // 新 API：接 workspace 文件（python xlsxwriter 流式）
  ];
  return rawTools.map((t) => wrapWithArgsCapture(t, argsQueue));
}
