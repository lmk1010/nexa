import axios from 'axios';
import { Agent, provider, providerFromEnv } from '@mk-co/neox-sdk';
import { mockLlm } from '@mk-co/neox-sdk/testing';
import { resolveDatabaseAgentConfig } from './aiConfigStore.js';
import { config } from './config.js';
import { createTools } from './tools.js';
import { loadHistory, saveTurns } from './conversationStore.js';
import { compactMessages } from './contextCompactor.js';
// NOTE: `buildApiCapabilityManual` intentionally NOT imported into the system
// prompt. It expands into ~58k input tokens per turn, defeating the point of
// tool_search's on-demand API discovery and making every request eat a giant
// prompt. If a user hits the intent-mode fallback inside oa_read/ordersys_read,
// those tools can pull the manual themselves.

// zod-to-json-schema (used inside the SDK) emits draft-04 style tool schemas —
// {exclusiveMinimum:true, minimum:X}. Official DeepSeek validates draft-07
// strictly and rejects that. Walk the tool schemas and rewrite in-place to
// {exclusiveMinimum:X}. Kept minimal so we don't touch any other keys.
function normalizeSchemaForDraft07(node) {
  if (Array.isArray(node)) {
    for (const item of node) normalizeSchemaForDraft07(item);
    return;
  }
  if (!node || typeof node !== 'object') return;
  if (node.exclusiveMinimum === true && typeof node.minimum === 'number') {
    node.exclusiveMinimum = node.minimum;
    delete node.minimum;
  }
  if (node.exclusiveMaximum === true && typeof node.maximum === 'number') {
    node.exclusiveMaximum = node.maximum;
    delete node.maximum;
  }
  for (const key of Object.keys(node)) {
    if (key === 'exclusiveMinimum' || key === 'exclusiveMaximum') continue;
    normalizeSchemaForDraft07(node[key]);
  }
}

function normalizeToolsOnPayload(payload) {
  if (!payload || typeof payload !== 'object') return payload;
  if (Array.isArray(payload.tools) && payload.tools.length > 0) {
    normalizeSchemaForDraft07(payload.tools);
  }
  return payload;
}

let httpPatched = false;
function installLlmHttpNormalizer() {
  if (httpPatched) return;
  httpPatched = true;

  // 1. Patch axios at the class level so every instance (default and
  //    axios.create) runs our request transform. The SDK talks to
  //    OpenAI-compatible / Anthropic / Gemini endpoints via axios.
  const AxiosProto = axios.Axios?.prototype;
  if (AxiosProto && !AxiosProto.__llmSchemaPatched) {
    const originalRequest = AxiosProto.request;
    AxiosProto.request = function patchedRequest(configOrUrl, maybeConfig) {
      try {
        const cfg =
          typeof configOrUrl === 'string'
            ? { ...(maybeConfig || {}), url: configOrUrl }
            : configOrUrl || {};
        const url = String(cfg.url || cfg.baseURL || '');
        if (
          url.includes('/chat/completions') ||
          url.includes('/responses') ||
          url.includes('/messages')
        ) {
          if (cfg.data && typeof cfg.data === 'string') {
            try {
              const parsed = JSON.parse(cfg.data);
              normalizeToolsOnPayload(parsed);
              cfg.data = JSON.stringify(parsed);
            } catch {
              // leave untouched
            }
          } else if (cfg.data && typeof cfg.data === 'object') {
            normalizeToolsOnPayload(cfg.data);
          }
        }
        if (typeof configOrUrl === 'string') {
          return originalRequest.call(this, cfg.url, cfg);
        }
        return originalRequest.call(this, cfg);
      } catch {
        return originalRequest.apply(this, arguments);
      }
    };
    AxiosProto.__llmSchemaPatched = true;
  }

  // 2. Also patch globalThis.fetch as a belt-and-suspenders in case any
  //    provider uses fetch instead of axios.
  const originalFetch = globalThis.fetch;
  if (originalFetch && !globalThis.fetch.__llmSchemaPatched) {
    const patchedFetch = async (input, init) => {
      try {
        if (init?.body && typeof init.body === 'string') {
          const urlStr = typeof input === 'string' ? input : input?.url || '';
          if (
            urlStr.includes('/chat/completions') ||
            urlStr.includes('/responses') ||
            urlStr.includes('/messages')
          ) {
            const parsed = JSON.parse(init.body);
            normalizeToolsOnPayload(parsed);
            init = { ...init, body: JSON.stringify(parsed) };
          }
        }
      } catch {
        // fall through
      }
      return originalFetch(input, init);
    };
    patchedFetch.__llmSchemaPatched = true;
    globalThis.fetch = patchedFetch;
  }
}
installLlmHttpNormalizer();

function requestModel(body) {
  if (typeof body?.model === 'string' && body.model.trim()) {
    return body.model.trim();
  }
  if (config.aiConfig.source === 'database' && config.aiConfig.model) {
    return config.aiConfig.model;
  }
  return config.agent.model;
}

async function resolveRuntimeConfig(body, prompt) {
  if (config.agent.useMock) {
    return {
      model: requestModel(body),
      provider: mockLlm({
        responses: [
          {
            type: 'text',
            content: config.agent.mockResponse.replaceAll('{{message}}', prompt),
          },
        ],
      }),
    };
  }

  const requestedModel = requestModel(body);
  const databaseConfig = await resolveDatabaseAgentConfig(requestedModel);
  if (databaseConfig) {
    return {
      model: databaseConfig.model,
      provider: provider({
        type: databaseConfig.providerType,
        apiKey: databaseConfig.apiKey,
        baseURL: databaseConfig.baseURL,
        timeout: config.agent.timeoutMs,
      }),
      databaseConfig,
    };
  }

  if (config.agent.apiKey) {
    return {
      model: requestedModel,
      provider: provider({
        type: config.agent.providerType,
        apiKey: config.agent.apiKey,
        baseURL: config.agent.baseURL || undefined,
        timeout: config.agent.timeoutMs,
      }),
    };
  }

  return {
    model: requestedModel,
    provider: providerFromEnv() || undefined,
  };
}

export function buildPrompt(body) {
  if (typeof body?.message === 'string' && body.message.trim()) {
    return body.message.trim();
  }

  if (Array.isArray(body?.messages) && body.messages.length > 0) {
    return body.messages
      .map((message) => {
        const role = typeof message?.role === 'string' ? message.role : 'user';
        const content = typeof message?.content === 'string' ? message.content : JSON.stringify(message?.content ?? '');
        return `${role}: ${content}`;
      })
      .join('\n');
  }

  throw new Error('Request body must include message or messages');
}

function formatUserContext(context, profile) {
  const login = context?.loginUser ?? {};
  const lines = ['## 当前会话上下文（每次请求会变，别被这段影响你的判断）'];

  const id = login.id ?? login.userId ?? null;
  const nickname = login.nickname ?? login.username ?? '当前用户';
  const username = login.username ?? '';
  lines.push(
    `- 用户：${nickname}${
      username && username !== nickname ? ` (${username})` : ''
    }${id ? ` · id=${id}` : ''}`,
  );

  const tenantId = context?.tenantId ?? null;
  const tenantName = profile?.tenantName ?? profile?.company ?? null;
  if (tenantName || tenantId) {
    lines.push(`- 租户：${tenantName ?? `id=${tenantId}`}`);
  }

  const deptName =
    profile?.deptName ?? profile?.department?.name ?? login.deptName ?? null;
  const deptId = profile?.deptId ?? login.deptId ?? null;
  if (deptName || deptId) {
    lines.push(`- 部门：${deptName ?? `id=${deptId}`}`);
  }

  const jobTitle =
    profile?.jobTitle ?? profile?.job?.name ?? profile?.position ?? null;
  if (jobTitle) lines.push(`- 岗位：${jobTitle}`);

  const jobLevel = profile?.jobLevel?.name ?? profile?.jobLevelName ?? null;
  if (jobLevel) lines.push(`- 职级：${jobLevel}`);

  const roles = profile?.roles ?? profile?.roleNames ?? null;
  if (Array.isArray(roles) && roles.length > 0) {
    lines.push(`- 角色：${roles.join(' / ')}`);
  }

  const employeeNo = profile?.employeeNo ?? profile?.no ?? null;
  if (employeeNo) lines.push(`- 工号：${employeeNo}`);

  const onboardDate =
    profile?.onboardDate ?? profile?.regularDate ?? null;
  if (onboardDate) lines.push(`- 入职日期：${onboardDate}`);

  const now = new Date();
  lines.push(`- 当前时间：${now.toISOString()}`);

  return lines.join('\n');
}

function buildStableSystemPrefix(body) {
  // Everything in this prefix is byte-for-byte identical across turns of the
  // same client, so DeepSeek's automatic prefix caching will hit. The
  // client-provided systemPrompt is usually a stable per-feature string
  // (e.g. "你是嵌在总裁驾驶舱里的经营分析助手") so it stays in the prefix too.
  const parts = [config.agent.systemPrompt];
  if (typeof body?.systemPrompt === 'string' && body.systemPrompt.trim()) {
    parts.push(body.systemPrompt.trim());
  }
  return parts.join('\n\n');
}

async function buildDynamicSystemSuffix(context, _signal) {
  // Prompt user context comes from the gateway's login-user header directly —
  // no synchronous HR call on the hot path. The HR profile enrichment can be
  // done later via oa_read when the model actually needs job title / dept.
  return formatUserContext(context, null);
}


// Floor for maxSteps: clients (Flutter app in particular) have historically
// hardcoded very small values (12) that cause boss-scale queries to hit
// "Reached max iterations" mid-answer. Enforce a server-side minimum so client
// undersizing can't strand an in-flight query — the config.agent.maxSteps env
// (default 20) doubles as the floor.
function requestMaxSteps(body) {
  const floor = config.agent.maxSteps;
  if (body?.maxSteps === undefined || body.maxSteps === null) {
    return floor;
  }

  const value = Number.parseInt(String(body.maxSteps), 10);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error('maxSteps must be a positive integer');
  }
  return Math.max(value, floor);
}

async function createAgent(body, context, signal, prompt) {
  const runtimeConfig = await resolveRuntimeConfig(body, prompt);
  // Prime the dynamic suffix once at run start so `systemPrompt()` (which the
  // SDK may call multiple times per iteration) returns a stable string within
  // this run.
  const stablePrefix = buildStableSystemPrefix(body);
  let dynamicSuffix = null;
  try {
    dynamicSuffix = await buildDynamicSystemSuffix(context, signal);
  } catch {
    dynamicSuffix = formatUserContext(context, null);
  }
  const systemPromptFactory = () =>
    [stablePrefix, dynamicSuffix ?? formatUserContext(context, null)].join(
      '\n\n',
    );
  return new Agent({
    model: runtimeConfig.model,
    provider: runtimeConfig.provider,
    systemPrompt: systemPromptFactory,
    tools: createTools(context),
    maxSteps: requestMaxSteps(body),
    permission: config.agent.permission,
    signal,
  });
}

function conversationIdOf(body) {
  const raw = body?.conversationId;
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  return trimmed.length > 0 ? trimmed : null;
}

async function bodyWithLoadedHistory(body, context, conversationId, signal) {
  if (!conversationId) return body;
  const existing = Array.isArray(body?.messages) ? body.messages : [];
  // If the client sent >1 message it's managing history locally in memory —
  // don't double-load from DB (would duplicate turns). Only pull DB history
  // when the client sends just the current user turn (thin client).
  if (existing.length > 1) return body;
  const history = await loadHistory(context, conversationId, { limit: 20 });
  if (!history.length) return body;
  const merged = [...history, ...existing];
  // 上下文自动压缩：> 70% 触发点直接 summarize 老部分，防超限崩溃
  const { messages: compacted, compressed, compressionMethod, originalChars, compressedChars } =
    await compactMessages(merged, signal);
  if (compressed) {
    console.log(
      `[context-compact] ${compressionMethod} ${originalChars}→${compressedChars} chars`,
    );
  }
  return { ...body, messages: compacted };
}

function lastUserMessageContent(body) {
  if (typeof body?.message === 'string' && body.message.trim()) {
    return body.message.trim();
  }
  const arr = Array.isArray(body?.messages) ? body.messages : [];
  for (let i = arr.length - 1; i >= 0; i--) {
    const m = arr[i];
    if (m?.role === 'user' && typeof m?.content === 'string' && m.content.trim()) {
      return m.content.trim();
    }
  }
  return null;
}

async function persistTurn(context, conversationId, body, assistantText) {
  if (!conversationId) return;
  const turns = [];
  const userContent = lastUserMessageContent(body);
  if (userContent) turns.push({ role: 'user', content: userContent });
  if (assistantText && assistantText.trim()) {
    turns.push({ role: 'assistant', content: assistantText.trim() });
  }
  if (!turns.length) return;
  await saveTurns(context, conversationId, turns);
}

export async function runAgent(body, context, signal) {
  const conversationId = conversationIdOf(body);
  const enriched = await bodyWithLoadedHistory(body, context, conversationId, signal);
  const prompt = buildPrompt(enriched);
  const agent = await createAgent(enriched, context, signal, prompt);
  const result = sanitizeAgentResult(await agent.run(prompt));
  if (conversationId) {
    persistTurn(context, conversationId, body, result.text).catch(() => {});
  }
  return { ...result, conversationId };
}

const TOOL_START_TYPES = new Set([
  'tool_call',
  'tool_call_start',
  'tool_call_item',
  'tool_use',
  'function_call',
]);
const TOOL_END_TYPES = new Set([
  'tool_result',
  'tool_output',
  'tool_call_output_item',
  'tool_call_done',
  'function_call_output',
  'tool_error',
]);

function pickToolName(event) {
  return event?.tool || event?.name || event?.toolName || null;
}

function pickArgs(event) {
  if (event?.input !== undefined) return event.input;
  if (event?.arguments !== undefined) return event.arguments;
  if (event?.args !== undefined) return event.args;
  return null;
}

// Compute a human-facing sentence for one tool call so the UI can show
// "正在查询本月赔付统计" instead of "api_call 处理好了". Falls back
// gracefully when args are missing (tool_call_start under openai-compat
// providers doesn't have deltas aggregated yet).
async function humanLabelFor(toolName, args) {
  if (!toolName) return null;
  const argObj = args && typeof args === 'object' ? args : null;
  switch (toolName) {
    case 'api_search': {
      const query = argObj?.query;
      const domain = argObj?.domain;
      if (query) return `搜索接口：${truncate(query, 40)}${domain ? `（${domain}）` : ''}`;
      return '正在搜索接口';
    }
    case 'api_call': {
      if (Array.isArray(argObj?.batch) && argObj.batch.length > 0) {
        const labels = [];
        for (const call of argObj.batch.slice(0, 3)) {
          const label = await purposeForPath(call?.path);
          if (label) labels.push(label);
        }
        const more = argObj.batch.length - labels.length;
        const suffix = more > 0 ? `等 ${argObj.batch.length} 个查询` : '';
        return labels.length > 0 ? `并发查询：${labels.join('、')}${suffix}` : `批量查询 ${argObj.batch.length} 个接口`;
      }
      if (argObj?.path) {
        const label = await purposeForPath(argObj.path);
        return label ? `查询：${label}` : `查询：${argObj.path}`;
      }
      return '正在执行查询';
    }
    case 'render_chart': {
      const type = argObj?.type;
      const title = argObj?.title;
      const typeName = type === 'bar' ? '柱状图' : type === 'pie' ? '饼图' : type === 'line' ? '折线图' : type === 'stat' ? '指标卡' : '图表';
      return title ? `画${typeName}：${truncate(title, 30)}` : `生成${typeName}`;
    }
    case 'get_request_context':
      return '确认当前用户';
    case 'export_excel': {
      const filename = argObj?.filename;
      const rowCount = Array.isArray(argObj?.rows) ? argObj.rows.length : null;
      if (filename && rowCount != null) return `导出 Excel：${truncate(filename, 30)}（${rowCount} 行）`;
      if (filename) return `导出 Excel：${truncate(filename, 30)}`;
      if (rowCount != null) return `导出 Excel（${rowCount} 行）`;
      return '正在生成 Excel';
    }
    case 'fetch_url': {
      const url = argObj?.url;
      if (!url) return '抓取网页';
      try {
        return `抓取：${new URL(url).hostname}`;
      } catch {
        return `抓取网页`;
      }
    }
    default:
      return null;
  }
}

let _apiIndexModule = null;
async function purposeForPath(path) {
  if (!path) return null;
  try {
    if (!_apiIndexModule) _apiIndexModule = await import('./apiIndex.js');
    const record = _apiIndexModule.getByPath(String(path), 'GET');
    if (!record) return String(path);
    // Extract the leading business phrase from purpose. Curated purpose starts
    // with a 【...】 chunk or a period-terminated leading clause. Fall back to
    // the raw path when purpose is generic ("获取员工列表").
    const p = record.purpose || '';
    const brace = /^([^【】]*【[^【】]+】[^。]*)/.exec(p);
    if (brace) return truncate(brace[1], 28);
    const clause = p.split(/[。！？.!?]/)[0] || p;
    return truncate(clause, 28);
  } catch {
    return null;
  }
}

function truncate(text, max) {
  const s = String(text || '');
  return s.length > max ? s.slice(0, max - 1) + '…' : s;
}

function pickOutput(event) {
  if (event?.output !== undefined) return event.output;
  if (event?.result !== undefined) return event.result;
  if (event?.content !== undefined) return event.content;
  return null;
}

function truncateForPreview(value, maxChars = 320) {
  if (value === undefined || value === null) return null;
  const raw = typeof value === 'string' ? value : safeStringify(value);
  if (!raw) return null;
  if (raw.length <= maxChars) return raw;
  return raw.slice(0, maxChars) + '…';
}

function safeStringify(value) {
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function isEmptyOutput(value) {
  if (value === undefined || value === null) return true;
  if (typeof value === 'string') return value.trim().length === 0;
  if (typeof value === 'object') {
    if (Array.isArray(value)) return value.length === 0;
    return Object.keys(value).length === 0;
  }
  return false;
}

export async function* streamAgent(body, context, signal) {
  const conversationId = conversationIdOf(body);
  const enriched = await bodyWithLoadedHistory(body, context, conversationId, signal);
  const prompt = buildPrompt(enriched);
  const agent = await createAgent(enriched, context, signal, prompt);
  const inflight = new Map();
  const finalized = new Set();
  let assistantText = '';
  let seenTextDelta = false;

  if (conversationId) {
    yield { type: 'conversation_id', conversationId };
  }

  for await (const event of agent.stream(prompt)) {
    if (!event || typeof event !== 'object') continue;
    if (event.type === 'thinking' && !config.agent.streamThinking) continue;

    // Some upstream OpenAI-compat providers (DeepSeek included) occasionally
    // slip U+FFFD replacement chars into `delta` when a BPE token straddles a
    // multi-byte boundary. Strip them so they never reach the boss's screen —
    // losing a rare char is far less bad than leaking "?" placeholders.
    if (event.type === 'text_delta' && typeof event.delta === 'string') {
      const clean = event.delta.replace(/�/g, '');
      if (clean !== event.delta) event = { ...event, delta: clean };
      assistantText += clean;
      seenTextDelta = true;
    } else if (
      event.type === 'text' &&
      typeof event.text === 'string' &&
      !seenTextDelta
    ) {
      const clean = event.text.replace(/�/g, '');
      if (clean !== event.text) event = { ...event, text: clean };
      assistantText = clean;
    }

    const type = event.type;
    const id = event.id ?? null;

    if (TOOL_START_TYPES.has(type)) {
      const name = pickToolName(event);
      const args = pickArgs(event);
      const startedAt = Date.now();
      if (id) inflight.set(id, { startedAt, args, name });
      // Emit the START chip. Under openai-compat providers args are typically
      // {} at this point (SDK hasn't aggregated argument deltas yet) — so the
      // START-side humanLabel is a generic phrase per tool. The END event
      // below carries the real, specific label once we know what was called.
      const startLabel = await humanLabelFor(name, args);
      yield {
        ...sanitizeAgentEvent(event),
        startedAt,
        humanLabel: startLabel,
      };
      continue;
    }

    if (TOOL_END_TYPES.has(type)) {
      // The OpenAI-compatible provider emits a placeholder `tool_result` with
      // an empty `output` right when the tool starts executing, then a second
      // one with the real payload when it finishes. Suppress the placeholder
      // so the client only ever sees one completion per tool call — and the
      // model's own loop guard doesn't mistake the empty preview for a real
      // "already succeeded" cache hit.
      const output = pickOutput(event);
      const isError = type === 'tool_error';
      const explicitFailure = event.success === false;

      if (!isError && !explicitFailure && id && isEmptyOutput(output) && !finalized.has(id)) {
        // Placeholder: skip and keep the inflight open so the real one still
        // gets a correct durationMs.
        continue;
      }

      if (id && finalized.has(id)) {
        // Duplicate finalization for an already-emitted id (rare, seen when
        // both `tool_result` and `tool_output` arrive). Drop.
        continue;
      }

      const info = id ? inflight.get(id) : null;
      const finishedAt = Date.now();
      const durationMs = info ? Math.max(0, finishedAt - info.startedAt) : null;
      if (id) {
        finalized.add(id);
        if (info) inflight.delete(id);
      }
      const success =
        event.success !== undefined ? Boolean(event.success) : !isError;

      // Pull the real, Zod-parsed args from the handler-side capture queue
      // (FIFO by tool name — SDK invokes handlers sequentially for
      // openai-compat providers).
      const toolName = pickToolName(event) || info?.name || null;
      const argsQueue = context?._toolArgsQueue;
      let realArgs;
      if (Array.isArray(argsQueue) && toolName) {
        const idx = argsQueue.findIndex((entry) => entry.name === toolName);
        if (idx >= 0) {
          realArgs = argsQueue[idx].args;
          argsQueue.splice(idx, 1);
        }
      }

      const endLabel = await humanLabelFor(toolName, realArgs ?? info?.args ?? pickArgs(event));
      yield {
        ...sanitizeAgentEvent(event),
        finishedAt,
        durationMs,
        success,
        outputPreview: truncateForPreview(output),
        argsPreview:
          realArgs !== undefined
            ? truncateForPreview(realArgs)
            : event.argsPreview ??
              truncateForPreview(info?.args ?? pickArgs(event)),
        toolName,
        humanLabel: endLabel,
      };

      // Chart tool: after the normal tool_result chip, emit an untruncated
      // `chart` event so the client can render the actual figure inline. This
      // is the ONLY sanctioned chart rendering path — markdown ```chart fences
      // are unreliable (model claims a chart but forgets the fence / uses
      // wrong field names / emits all-zero data).
      if (toolName === 'render_chart' && success && realArgs && typeof realArgs === 'object') {
        yield {
          type: 'chart',
          id: id ? `${id}:chart` : `chart-${finishedAt}`,
          toolCallId: id ?? null,
          spec: realArgs,
        };
      }
      continue;
    }

    yield sanitizeAgentEvent(event);
  }

  if (conversationId) {
    persistTurn(context, conversationId, body, assistantText).catch(() => {});
  }
}

function sanitizeMessage(message) {
  if (!message || message.role === 'system') {
    return null;
  }

  return {
    role: message.role,
    content: typeof message.content === 'string' ? message.content : JSON.stringify(message.content ?? ''),
  };
}

export function sanitizeAgentResult(result) {
  return {
    text: result.text,
    usage: result.usage,
    steps: result.steps || [],
    messages: Array.isArray(result.messages) ? result.messages.map(sanitizeMessage).filter(Boolean) : [],
    stopReason: result.stopReason,
  };
}

export function sanitizeAgentEvent(event) {
  if (event?.type === 'error') {
    return {
      type: 'error',
      error: event.error instanceof Error ? event.error.message : String(event.error ?? 'Unknown error'),
    };
  }

  return event;
}
