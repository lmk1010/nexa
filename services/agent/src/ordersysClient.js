import http from 'node:http';
import https from 'node:https';
import { config } from './config.js';
import { getByPath as apiIndexGetByPath } from './apiIndex.js';

// Adapter over the unified api-index. The old ordersysEndpointIndex.js used to
// carry per-endpoint protection tuning (`slow`, `pageSizeMax`, `defaultQuery`,
// `cooldownMs`, `maxCallsPerRun`). The new index is intentionally flatter — the
// LLM decides those things from the endpoint card. What the client still needs
// from the record is: (a) confirmation the path is in the read-only whitelist,
// (b) a stable object shape so the existing cooldown/budget helpers keep
// compiling. Both are provided here.
function ordersysEndpointByPath(path) {
  const record = apiIndexGetByPath(path, 'GET');
  if (!record || record.domain !== 'ordersys') return null;
  return {
    path: record.path,
    category: record._extras?.functionName || 'ordersys',
    purpose: record.purpose,
    protection: {},
  };
}
import { createPerUserQueue } from './perUserQueue.js';

let cachedCredential = null;
let pendingCredentialPromise = null;
const endpointCooldowns = new Map();

const ordersysQueue = createPerUserQueue({
  name: 'ordersys',
  maxGlobal: config.ordersys.maxConcurrentRequests,
  maxPerUser: config.ordersys.maxConcurrentRequestsPerUser,
  queueTimeoutMs: config.ordersys.queueTimeoutMs,
});

export function ordersysQueueSnapshot() {
  return ordersysQueue.snapshot();
}

function jsonLike(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return { raw: text.slice(0, 200) };
  }
}

function commonMessage(payload) {
  if (!payload || typeof payload !== 'object') {
    return '';
  }
  return payload.msg || payload.message || payload.error || '';
}

function isConfigured() {
  return Boolean(config.ordersys.baseURL && config.ordersys.username && config.ordersys.password);
}

function isCredentialUsable(credential) {
  if (!credential?.token) {
    return false;
  }
  return credential.expiresAt - Date.now() > config.ordersys.refreshSkewMs;
}

function assertOrdersysReadPath(path) {
  const cleaned = String(path || '').trim().split('?')[0];
  if (!cleaned.startsWith('/')) {
    throw new Error('ordersys API path must start with /');
  }
  if (/^\/\//.test(cleaned) || /^[a-z][a-z0-9+.-]*:/i.test(cleaned)) {
    throw new Error('Absolute ordersys URLs are not allowed');
  }
  const endpoint = ordersysEndpointByPath(cleaned);
  if (!endpoint) {
    throw new Error(`ordersys_read path 不在白名单: ${cleaned}. 请改用系统提示词里的 API 手册，或直接给 ordersys_read 传 intent 让工具选择端点。`);
  }
  return { path: cleaned, endpoint };
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]';
}

function appendQueryValue(url, key, value) {
  if (value === undefined || value === null || value === '') {
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item, index) => {
      if (item !== undefined && item !== null && item !== '') {
        url.searchParams.append(`${key}[${index}]`, String(item));
      }
    });
    return;
  }
  if (isPlainObject(value)) {
    for (const [childKey, childValue] of Object.entries(value)) {
      appendQueryValue(url, `${key}[${childKey}]`, childValue);
    }
    return;
  }
  url.searchParams.append(key, String(value));
}

function appendQuery(url, query) {
  if (!query) return;
  for (const [key, value] of Object.entries(query)) {
    appendQueryValue(url, key, value);
  }
}

function ordersysBrowserHeaders() {
  const origin = new URL(config.ordersys.baseURL).origin;
  return {
    Accept: 'application/json, text/plain, */*',
    'Accept-Language': 'zh-CN,zh;q=0.9',
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache',
    Origin: origin,
    Referer: `${origin}/login?redirect=%2Findex`,
    'Sec-Fetch-Dest': 'empty',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Site': 'same-origin',
    'User-Agent':
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36',
    isToken: 'false',
    uuid: '84f8b014-3268-4a86-8390-ec238e9f308d',
  };
}

function buildOrdersysUrl(path) {
  const base = new URL(config.ordersys.baseURL);
  const basePath = base.pathname.replace(/\/+$/, '');
  const requestPath = String(path || '').startsWith('/') ? String(path || '') : `/${path || ''}`;
  base.pathname = `${basePath}${requestPath}`.replace(/\/{2,}/g, '/');
  base.search = '';
  base.hash = '';
  return base;
}

async function fetchOrdersysJson(method, path, { query, body, headers, signal, timeoutMs } = {}) {
  const url = buildOrdersysUrl(path);
  appendQuery(url, query);
  const bodyText = body === undefined ? null : JSON.stringify(body);
  const requestHeaders = {
    ...ordersysBrowserHeaders(),
    ...(bodyText === null
      ? {}
      : {
          'Content-Type': 'application/json;charset=UTF-8',
          'Content-Length': Buffer.byteLength(bodyText),
        }),
    ...headers,
  };

  return new Promise((resolve, reject) => {
    const transport = url.protocol === 'http:' ? http : https;
    let settled = false;
    const finishReject = (err) => {
      if (settled) return;
      settled = true;
      reject(err);
    };
    const finishResolve = (value) => {
      if (settled) return;
      settled = true;
      resolve(value);
    };

    const request = transport.request(
      url,
      {
        method,
        headers: requestHeaders,
        timeout: timeoutMs || config.ordersys.timeoutMs,
      },
      (response) => {
        // Cap the raw body at INTERNAL_API_MAX_RESPONSE_BYTES (default 512 KB).
        // Ordersys has endpoints that happily return 20+ MB when called without
        // pagination — audit log caught /admin/outlets/getShopOrderList returning
        // 21 MB, which stalls streaming and blows the LLM context. Truncate at
        // the transport layer so no downstream code has to care about the size.
        const cap = config.internalApi.maxResponseBytes;
        const chunks = [];
        let total = 0;
        let truncated = false;
        response.on('data', (chunk) => {
          if (truncated) return;
          total += chunk.byteLength;
          if (total <= cap) {
            chunks.push(Buffer.from(chunk));
            return;
          }
          const remaining = cap - (total - chunk.byteLength);
          if (remaining > 0) chunks.push(Buffer.from(chunk.subarray(0, remaining)));
          truncated = true;
          response.destroy();
        });
        response.on('end', () => {
          finishResolve({
            status: response.statusCode || 0,
            ok: (response.statusCode || 0) >= 200 && (response.statusCode || 0) < 300,
            data: jsonLike(Buffer.concat(chunks).toString('utf8')),
            truncated,
            totalBytes: total,
          });
        });
        response.on('close', () => {
          if (!truncated) return;
          finishResolve({
            status: response.statusCode || 0,
            ok: false,
            data: {
              code: 'ORDERSYS_RESPONSE_TOO_LARGE',
              msg: `ordersys 响应超过 ${(cap / 1024).toFixed(0)}KB 上限（已抓 ${(total / 1024).toFixed(0)}KB），已截断。请在 api_call params 里加 pageSize（10~30）或缩窄时间范围重试。`,
            },
            truncated: true,
            totalBytes: total,
          });
        });
      },
    );

    const abort = () => {
      const err = new Error('ordersys request aborted');
      err.name = 'AbortError';
      request.destroy(err);
    };
    if (signal) {
      if (signal.aborted) {
        abort();
      } else {
        signal.addEventListener('abort', abort, { once: true });
      }
    }

    request.on('timeout', () => {
      const err = new Error('ordersys request timeout');
      err.name = 'AbortError';
      request.destroy(err);
    });
    request.on('error', finishReject);
    request.on('close', () => {
      if (signal) {
        signal.removeEventListener('abort', abort);
      }
    });
    if (bodyText !== null) {
      request.write(bodyText);
    }
    request.end();
  });
}

function unwrapOrdersysSuccess(result, action) {
  const code = typeof result.data?.code === 'number' ? result.data.code : result.status;
  const success = result.ok && (result.data?.code === undefined || result.data.code === 0 || result.data.code === 200);
  if (!success) {
    throw new Error(`${action} failed: status=${result.status}, code=${code}, msg=${commonMessage(result.data)}`);
  }
  return result.data;
}

function normalizeCredential(payload) {
  const token = payload?.token || payload?.data?.token || payload?.accessToken || payload?.data?.accessToken;
  if (!token) {
    throw new Error('ordersys login succeeded without a token');
  }
  return {
    token,
    expiresAt: Date.now() + config.ordersys.tokenCacheMs,
  };
}

function pad2(value) {
  return String(value).padStart(2, '0');
}

function formatDate(date, endOfDay = false) {
  const yyyy = date.getFullYear();
  const mm = pad2(date.getMonth() + 1);
  const dd = pad2(date.getDate());
  const time = endOfDay ? '23:59:59' : '00:00:00';
  return `${yyyy}-${mm}-${dd} ${time}`;
}

function formatDateOnly(date) {
  const yyyy = date.getFullYear();
  const mm = pad2(date.getMonth() + 1);
  const dd = pad2(date.getDate());
  return `${yyyy}-${mm}-${dd}`;
}

function currentMonthRange() {
  const now = new Date();
  return [
    formatDate(new Date(now.getFullYear(), now.getMonth(), 1), false),
    formatDate(now, true),
  ];
}

function relativeDayRange(daysBack = 30) {
  const now = new Date();
  const start = new Date(now);
  start.setDate(start.getDate() - Number(daysBack || 0));
  return [formatDate(start, false), formatDate(now, true)];
}

function cloneQuery(query) {
  if (!query || typeof query !== 'object') {
    return {};
  }
  return JSON.parse(JSON.stringify(query));
}

function getNestedValue(query, target, key) {
  if (!target) return query[key];
  if (isPlainObject(query[target]) && query[target][key] !== undefined) {
    return query[target][key];
  }
  return query[`${target}[${key}]`];
}

function setNestedValue(query, target, key, value) {
  if (!target) {
    query[key] = value;
    return;
  }
  if (!isPlainObject(query[target])) {
    query[target] = {};
  }
  query[target][key] = value;
}

function hasNestedValue(query, target, key) {
  const value = getNestedValue(query, target, key);
  return value !== undefined && value !== null && value !== '';
}

function mergeDefaultQuery(target, defaults) {
  if (!isPlainObject(defaults)) {
    return;
  }
  for (const [key, value] of Object.entries(defaults)) {
    if (isPlainObject(value)) {
      if (!isPlainObject(target[key])) {
        target[key] = {};
      }
      mergeDefaultQuery(target[key], value);
      continue;
    }
    if (target[key] === undefined || target[key] === null || target[key] === '') {
      target[key] = value;
    }
  }
}

function applyDefaultDateRanges(query, ranges = []) {
  for (const range of ranges) {
    const target = range.target || '';
    const startKey = range.startKey || 'beginCreateTime';
    const endKey = range.endKey || 'endCreateTime';
    if (hasNestedValue(query, target, startKey) || hasNestedValue(query, target, endKey)) {
      continue;
    }

    const [start, end] =
      range.type === 'currentMonth'
        ? currentMonthRange()
        : relativeDayRange(range.daysBack ?? 30);
    setNestedValue(query, target, startKey, start);
    setNestedValue(query, target, endKey, end);
  }
}

function applyDefaultDateParams(query, params = []) {
  for (const item of params) {
    const target = item.target || '';
    const key = item.key || 'queryTime';
    if (hasNestedValue(query, target, key)) {
      continue;
    }
    const now = new Date();
    const value = item.date ? formatDateOnly(now) : formatDate(now, Boolean(item.endOfDay));
    setNestedValue(query, target, key, value);
  }
}

function applyPageLimits(query, protection = {}) {
  const pageSizeDefault = protection.pageSizeDefault;
  const pageSizeMax = protection.pageSizeMax || config.ordersys.pageSizeMax || 30;
  if ((query.pageNum === undefined || query.pageNum === null || query.pageNum === '') && pageSizeDefault) {
    query.pageNum = 1;
  }
  if (query.pageSize === undefined || query.pageSize === null || query.pageSize === '') {
    if (pageSizeDefault) {
      query.pageSize = pageSizeDefault;
    }
    return;
  }
  const numeric = Number.parseInt(query.pageSize, 10);
  if (Number.isFinite(numeric) && numeric > pageSizeMax) {
    query.pageSize = pageSizeMax;
  }
}

export function prepareOrdersysReadRequest(path, query) {
  const { path: cleanedPath, endpoint } = assertOrdersysReadPath(path);
  const preparedQuery = cloneQuery(query);
  const protection = endpoint.protection || {};
  mergeDefaultQuery(preparedQuery, protection.defaultQuery);
  applyDefaultDateParams(preparedQuery, protection.defaultDateParams);
  applyDefaultDateRanges(preparedQuery, protection.defaultDateRanges);
  applyPageLimits(preparedQuery, protection);
  return {
    path: cleanedPath,
    endpoint,
    query: preparedQuery,
    timeoutMs: protection.timeoutMs || config.ordersys.timeoutMs,
  };
}

async function loginOrdersys(signal) {
  if (!isConfigured()) {
    throw new Error('ordersys service account is not configured');
  }

  const result = await fetchOrdersysJson('POST', config.ordersys.loginPath, {
    body: {
      username: config.ordersys.username,
      password: config.ordersys.password,
    },
    signal,
  });
  return normalizeCredential(unwrapOrdersysSuccess(result, 'ordersys login'));
}

async function resolveOrdersysCredential(signal) {
  if (isCredentialUsable(cachedCredential)) {
    return cachedCredential;
  }

  if (pendingCredentialPromise) {
    return pendingCredentialPromise;
  }

  pendingCredentialPromise = (async () => {
    try {
      cachedCredential = await loginOrdersys(signal);
      return cachedCredential;
    } finally {
      pendingCredentialPromise = null;
    }
  })();

  return pendingCredentialPromise;
}

function normalizeOrdersysResult(response, action) {
  const code = typeof response.data?.code === 'number' ? response.data.code : response.status;
  const success = response.ok && (response.data?.code === undefined || response.data.code === 0 || response.data.code === 200);
  const permissionDenied = response.status === 401 || response.status === 403 || code === 401 || code === 403;

  if (!success) {
    return {
      ok: false,
      action,
      status: response.status,
      code,
      permissionDenied,
      message: commonMessage(response.data) || `ordersys API failed with status ${response.status}`,
      data: response.data,
    };
  }

  return {
    ok: true,
    action,
    status: response.status,
    code: response.data?.code ?? 200,
    data: response.data?.data ?? response.data,
  };
}

export function enforceOrdersysToolBudget(context, action) {
  context.ordersysToolCallCount = (context.ordersysToolCallCount || 0) + 1;
  if (context.ordersysToolCallCount > config.ordersys.maxToolCallsPerRun) {
    throw new Error(
      `ordersys tool call limit exceeded: ${action} is over ${config.ordersys.maxToolCallsPerRun} calls per agent run`,
    );
  }
}

function endpointCooldownKey(endpoint, path) {
  return endpoint?.path || path;
}

function endpointCooldownStatus(endpoint, path) {
  const key = endpointCooldownKey(endpoint, path);
  const item = endpointCooldowns.get(key);
  const now = Date.now();
  if (!item || item.until <= now) {
    if (item) endpointCooldowns.delete(key);
    return null;
  }
  return {
    key,
    until: item.until,
    retryAfterMs: item.until - now,
    reason: item.reason,
  };
}

function markEndpointCooldown(endpoint, path, reason) {
  const protection = endpoint?.protection || {};
  const cooldownMs = protection.cooldownMs || config.ordersys.cooldownMs || 60_000;
  const key = endpointCooldownKey(endpoint, path);
  endpointCooldowns.set(key, {
    until: Date.now() + cooldownMs,
    reason,
  });
}

function enforceOrdersysEndpointBudget(context, endpoint, path) {
  const protection = endpoint?.protection || {};
  const maxCalls = protection.maxCallsPerRun || (protection.slow ? 1 : 2);
  const key = endpointCooldownKey(endpoint, path);
  if (!context.ordersysEndpointCallCounts) {
    context.ordersysEndpointCallCounts = {};
  }
  context.ordersysEndpointCallCounts[key] = (context.ordersysEndpointCallCounts[key] || 0) + 1;
  if (context.ordersysEndpointCallCounts[key] > maxCalls) {
    return {
      ok: false,
      status: 0,
      code: 'ORDERSYS_ENDPOINT_REPEAT_GUARD',
      permissionDenied: false,
      protected: true,
      repeated: true,
      endpoint: key,
      maxCallsPerRun: maxCalls,
      message:
        `连图接口 ${key} 本轮已经查过 ${maxCalls} 次，已停止重复请求。` +
        '请基于已有工具结果回答；如果必须继续查，换更窄日期/分页条件或使用 API 手册里的其它统计/详情接口。',
    };
  }
  return null;
}

function isAbortError(err) {
  return err?.name === 'AbortError' || /aborted|abort|timeout/i.test(String(err?.message || err));
}

export async function ordersysGet(context, path, query, action, signal) {
  const request = prepareOrdersysReadRequest(path, query);
  const cooldown = endpointCooldownStatus(request.endpoint, request.path);
  if (cooldown) {
    return {
      ok: false,
      action,
      status: 0,
      code: 'ORDERSYS_ENDPOINT_COOLDOWN',
      permissionDenied: false,
      protected: true,
      cooldown: true,
      retryAfterMs: cooldown.retryAfterMs,
      message:
        `ordersys endpoint ${cooldown.key} recently timed out or was marked slow; ` +
        `wait about ${Math.ceil(cooldown.retryAfterMs / 1000)}s, narrow the date range, or use another documented route.`,
      request: {
        path: request.path,
        query: request.query,
        endpoint: request.endpoint.path,
        protected: Boolean(request.endpoint.protection?.slow),
      },
    };
  }
  enforceOrdersysToolBudget(context, action);
  // Note: legacy per-endpoint repeat guard (maxCallsPerRun ≤ 2) removed. It was
  // designed for the old oa_read/ordersys_read intent-loop pattern but under the
  // new harness the LLM legitimately batches parallel calls to the same
  // endpoint with different params (e.g. `/community/list` for societyId 1/2/3
  // side-by-side). Audit log 2026-07-08 showed 6 legit calls in a row all
  // rejected as ORDERSYS_ENDPOINT_REPEAT_GUARD. The global 8-calls-per-run
  // budget + hotErrorCache + api_call.batch max-8 already gate runaway loops.

  const releaseSlot = await ordersysQueue.acquire(context, signal);
  try {
    let credential = await resolveOrdersysCredential(signal);
    let response = await fetchOrdersysJson('GET', request.path, {
      query: request.query,
      headers: { Authorization: `Bearer ${credential.token}` },
      timeoutMs: request.timeoutMs,
      signal,
    });

    if (response.status === 401 || response.data?.code === 401) {
      cachedCredential = null;
      credential = await resolveOrdersysCredential(signal);
      response = await fetchOrdersysJson('GET', request.path, {
        query: request.query,
        headers: { Authorization: `Bearer ${credential.token}` },
        timeoutMs: request.timeoutMs,
        signal,
      });
    }

    return {
      ...normalizeOrdersysResult(response, action),
      request: {
        path: request.path,
        query: request.query,
        endpoint: request.endpoint.path,
        protected: Boolean(request.endpoint.protection?.slow),
      },
    };
  } catch (err) {
    if (isAbortError(err)) {
      markEndpointCooldown(request.endpoint, request.path, 'timeout');
      return {
        ok: false,
        action,
        status: 0,
        code: 'ORDERSYS_TIMEOUT',
        permissionDenied: false,
        protected: true,
        cooldown: true,
        message:
          `ordersys endpoint ${request.endpoint.path} timed out; it has been put into cooldown. ` +
          'Do not retry immediately. Narrow the date range/pageSize or use another documented route.',
        request: {
          path: request.path,
          query: request.query,
          endpoint: request.endpoint.path,
        },
      };
    }
    throw err;
  } finally {
    releaseSlot();
  }
}

export function ordersysPublicStatus() {
  return {
    authMode: config.ordersys.authMode,
    configured: isConfigured(),
    baseURL: config.ordersys.baseURL,
    loginPath: config.ordersys.loginPath,
    maxConcurrentRequests: config.ordersys.maxConcurrentRequests,
    cooldowns: Array.from(endpointCooldowns.entries()).map(([key, value]) => ({
      endpoint: key,
      retryAfterMs: Math.max(0, value.until - Date.now()),
      reason: value.reason,
    })),
    cached: Boolean(cachedCredential?.token),
  };
}

export function resetOrdersysClientForTest() {
  cachedCredential = null;
  pendingCredentialPromise = null;
  endpointCooldowns.clear();
  ordersysQueue.reset();
}

export function markOrdersysEndpointCooldownForTest(path, reason = 'test-timeout') {
  const { path: cleanedPath, endpoint } = assertOrdersysReadPath(path);
  markEndpointCooldown(endpoint, cleanedPath, reason);
}
