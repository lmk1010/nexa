import http from 'node:http';
import { once } from 'node:events';
import { config } from './config.js';
import { getRequestContext } from './context.js';
import { runAgent, streamAgent } from './neoxAgent.js';
import { serviceAccountPublicStatus } from './serviceAccountAuth.js';
import {
  queryExecutiveCockpitChat,
  queryExecutiveCockpitOverview,
} from './tools.js';
import { ordersysPublicStatus } from './ordersysClient.js';
import {
  listConversations,
  getConversationMessages,
  renameConversation,
  deleteConversation,
} from './conversationStore.js';
import { collectCanalStatus, isOpsAdmin, listCanalErrors } from './opsStatus.js';
import { collectCanalRates } from './opsRates.js';
import { listDdlAudit } from './opsDdlAudit.js';
import { collectMyAccess, canAccessDashboard, canAccessOps, canUseChat } from './permissions.js';
import { handlePfMonthly, handleOrderMonthly, handleOrderCountMonthly, handleWorkMonthly, handleAttributionMonthly, handleSlaMonthly, handleReissueMonthly } from './dashboardEndpoints.js';
import { collectAuditAnalytics } from './auditAnalytics.js';
import { listUserExports } from './fileHistoryStore.js';

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

// Gateway (TokenAuthenticationFilter) validates the JWT and injects
// `login-user` + `authorization` before forwarding to us. If either is missing,
// the caller either bypassed the gateway (blocked by firewall in prod, but
// belt-and-suspenders) or presented no token. Reject with the same 401 shape
// that KYX's Java services return, so the app / OA UI treat it as
// "please log in again" — not a mystery 500 or a wide-open response.
//
// Header trust model: `login-user` is trusted only because we assume the
// gateway strips any client-provided value before setting its own. That is the
// standard TokenAuthenticationFilter behavior; if the gateway ever gets
// rewritten in a way that doesn't strip it, this check MUST be upgraded to
// verify the JWT directly (agent would need the signing key).
function requireLogin(context) {
  if (!context?.loginUser?.id) {
    throw new HttpError(401, '账号未登录');
  }
}

// 数据看板 / 总裁驾驶舱 —— 老板 (biz_boss) + 租户管理员 (tenant_admin)
// env AGENT_OPS_ADMIN_USER_IDS 白名单作为应急后门（本地测试/紧急场景）
async function requireDashboardAdmin(req) {
  const context = getRequestContext(req);
  requireLogin(context);
  if (isOpsAdmin(context)) return context;
  if (await canAccessDashboard(context)) return context;
  throw new HttpError(403, '无权访问数据看板');
}

// 运维监控 —— 租户管理员 + 技术部 tech_maintenance（老板不放）
async function requireOpsAccess(req) {
  const context = getRequestContext(req);
  requireLogin(context);
  if (isOpsAdmin(context)) return context;
  if (await canAccessOps(context)) return context;
  throw new HttpError(403, '无权访问运维监控');
}

// 对话（AI chat）—— 权限点 app:chat:use
async function requireChatAccess(req) {
  const context = getRequestContext(req);
  requireLogin(context);
  if (isOpsAdmin(context)) return context;
  if (await canUseChat(context)) return context;
  throw new HttpError(403, '无权使用对话功能');
}

function setCorsHeaders(res) {
  if (!config.service.corsAllowOrigin) {
    return;
  }

  res.setHeader('Access-Control-Allow-Origin', config.service.corsAllowOrigin);
  res.setHeader(
    'Access-Control-Allow-Headers',
    'Authorization, Content-Type, tenant-id, visit-tenant-id, tenant-ignore, x-request-id',
  );
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE, OPTIONS');
}

function sendJson(res, status, payload) {
  setCorsHeaders(res);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
  });
  res.end(JSON.stringify(payload));
}

function sendApiSuccess(res, data) {
  sendJson(res, 200, {
    code: 0,
    data,
    msg: '',
  });
}

function sendApiError(res, error) {
  const status = error instanceof HttpError ? error.status : 500;
  sendJson(res, status, {
    code: status,
    data: null,
    msg: error instanceof Error ? error.message : String(error),
  });
}

async function readJsonBody(req) {
  const chunks = [];
  let total = 0;

  for await (const chunk of req) {
    total += chunk.byteLength;
    if (total > config.service.bodyLimitBytes) {
      throw new HttpError(413, 'Request body is too large');
    }
    chunks.push(chunk);
  }

  if (chunks.length === 0) {
    return {};
  }

  const raw = Buffer.concat(chunks).toString('utf8');
  if (!raw.trim()) {
    return {};
  }

  try {
    return JSON.parse(raw);
  } catch {
    throw new HttpError(400, 'Request body must be valid JSON');
  }
}

function normalizePath(pathname) {
  const prefixes = ['/app-api/agent', '/admin-api/agent'];
  for (const prefix of prefixes) {
    if (pathname === prefix) {
      return '/';
    }
    if (pathname.startsWith(`${prefix}/`)) {
      return pathname.slice(prefix.length);
    }
  }
  return pathname;
}

function writeHealth(res) {
  sendJson(res, 200, {
    status: 'UP',
    service: config.service.name,
    serviceAccount: serviceAccountPublicStatus(),
    ordersys: ordersysPublicStatus(),
  });
}

async function handleRun(req, res) {
  const body = await readJsonBody(req);
  const context = getRequestContext(req);
  requireLogin(context);
  const controller = new AbortController();
  let completed = false;

  res.on('close', () => {
    if (!completed) {
      controller.abort();
    }
  });

  const result = await runAgent(body, context, controller.signal);
  completed = true;
  sendApiSuccess(res, result);
}

function assertBusinessResult(result) {
  if (result?.ok) {
    return result.data;
  }

  const status = result?.permissionDenied ? 403 : 502;
  throw new HttpError(status, result?.message || 'KYX internal API failed');
}

async function handleExecutiveCockpitOverview(req, res, url) {
  const context = getRequestContext(req);
  requireLogin(context);
  if (!(await isDashboardAdmin(context))) {
    throw new HttpError(403, '无权访问总裁驾驶舱');
  }
  const controller = new AbortController();
  let completed = false;

  res.on('close', () => {
    if (!completed) {
      controller.abort();
    }
  });

  const days = url.searchParams.get('days');
  const result = await queryExecutiveCockpitOverview(
    context,
    { days: days ? Number.parseInt(days, 10) : undefined },
    controller.signal,
  );
  completed = true;
  sendApiSuccess(res, assertBusinessResult(result));
}

async function handleExecutiveCockpitChat(req, res) {
  const body = await readJsonBody(req);
  const context = getRequestContext(req);
  requireLogin(context);
  if (!(await isDashboardAdmin(context))) {
    throw new HttpError(403, '无权访问总裁驾驶舱');
  }
  const controller = new AbortController();
  let completed = false;

  res.on('close', () => {
    if (!completed) {
      controller.abort();
    }
  });

  const result = await queryExecutiveCockpitChat(context, body, controller.signal);
  completed = true;
  sendApiSuccess(res, assertBusinessResult(result));
}

async function writeSse(res, eventName, data) {
  const payload = `event: ${eventName}\ndata: ${JSON.stringify(data)}\n\n`;
  if (!res.write(payload)) {
    await once(res, 'drain');
  }
}

async function handleStream(req, res) {
  const body = await readJsonBody(req);
  const context = await requireChatAccess(req);
  const controller = new AbortController();
  let completed = false;
  let lastEventAt = Date.now();

  setCorsHeaders(res);
  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no',
  });

  res.on('close', () => {
    if (!completed) {
      controller.abort();
    }
  });

  // 心跳：每 15s 无事件就发一个 SSE comment 保连接活着（nginx/负载均衡 60s 超时之下）
  // 静默 90s（超过 2 个心跳周期还没事件）视作 LLM 假死 → 主动 abort + 发 error
  const HEARTBEAT_MS = 15_000;
  const SILENT_ABORT_MS = 90_000;
  const heartbeat = setInterval(() => {
    if (completed) return;
    const silentFor = Date.now() - lastEventAt;
    if (silentFor >= SILENT_ABORT_MS) {
      controller.abort();
      return;
    }
    try {
      res.write(': keep-alive\n\n');
    } catch {
      // res 已断，忽略
    }
  }, HEARTBEAT_MS);

  try {
    for await (const event of streamAgent(body, context, controller.signal)) {
      lastEventAt = Date.now();
      await writeSse(res, event.type || 'message', event);
    }
    completed = true;
    clearInterval(heartbeat);
    res.end();
  } catch (error) {
    completed = true;
    clearInterval(heartbeat);
    const isAbort = error?.name === 'AbortError' || controller.signal.aborted;
    // 给客户端明确的错误类型 + 提示，客户端(和下轮 LLM)能识别"超时了需要重试或换问法"
    await writeSse(res, 'error', {
      type: 'error',
      code: isAbort ? 'AGENT_TIMEOUT' : 'AGENT_ERROR',
      error: isAbort
        ? '本轮回答超时（可能因数据量大或工具调用慢）。请重新提问、缩小时间范围、或稍后再试。'
        : error instanceof Error
          ? error.message
          : String(error),
      timeoutMs: SILENT_ABORT_MS,
    });
    res.end();
  }
}

async function handleConversationList(req, res, url) {
  const context = await requireChatAccess(req);
  const scene = url.searchParams.get('scene') || 'cockpit';
  const limit = url.searchParams.get('limit');
  const list = await listConversations(context, {
    scene,
    limit: limit ? Number.parseInt(limit, 10) : undefined,
  });
  sendApiSuccess(res, { list });
}

async function handleConversationDetail(req, res, conversationId, url) {
  const context = await requireChatAccess(req);
  const limit = url.searchParams.get('limit');
  const messages = await getConversationMessages(context, conversationId, {
    limit: limit ? Number.parseInt(limit, 10) : undefined,
  });
  sendApiSuccess(res, { conversationId, messages });
}

async function handleConversationRename(req, res, conversationId) {
  const body = await readJsonBody(req);
  const title = typeof body?.title === 'string' ? body.title : '';
  if (!title.trim()) {
    throw new HttpError(400, 'title is required');
  }
  const context = await requireChatAccess(req);
  const ok = await renameConversation(context, conversationId, title);
  if (!ok) {
    throw new HttpError(404, 'conversation not found');
  }
  sendApiSuccess(res, { conversationId, title: title.trim() });
}

async function handleOpsCanalStatus(req, res) {
  await requireOpsAccess(req);
  const status = await collectCanalStatus();
  sendApiSuccess(res, status);
}

async function handleConversationDelete(req, res, conversationId) {
  const context = await requireChatAccess(req);
  const ok = await deleteConversation(context, conversationId);
  if (!ok) {
    throw new HttpError(404, 'conversation not found');
  }
  sendApiSuccess(res, { conversationId, deleted: true });
}

const CONVERSATION_ID_RE = /^\/conversations\/([A-Za-z0-9_\-:.]{1,128})$/;

async function route(req, res) {
  setCorsHeaders(res);

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  const pathname = normalizePath(url.pathname);

  if (req.method === 'GET' && (pathname === '/health' || pathname === '/actuator/health')) {
    writeHealth(res);
    return;
  }

  if (req.method === 'POST' && pathname === '/run') {
    await handleRun(req, res);
    return;
  }

  if (req.method === 'POST' && pathname === '/stream') {
    await handleStream(req, res);
    return;
  }

  if (req.method === 'GET' && pathname === '/executive-cockpit/overview') {
    await handleExecutiveCockpitOverview(req, res, url);
    return;
  }

  if (req.method === 'POST' && pathname === '/executive-cockpit/chat') {
    await handleExecutiveCockpitChat(req, res);
    return;
  }

  if (req.method === 'GET' && pathname === '/ops/canal-status') {
    await handleOpsCanalStatus(req, res);
    return;
  }

  if (req.method === 'GET' && pathname === '/ops/canal-errors') {
    await requireOpsAccess(req);
    const pageNo = Number.parseInt(url.searchParams.get('pageNo') || '1', 10);
    const pageSize = Number.parseInt(url.searchParams.get('pageSize') || '10', 10);
    const hours = Number.parseInt(url.searchParams.get('hours') || '24', 10);
    const data = await listCanalErrors({ pageNo, pageSize, hours });
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/ops/canal-rates') {
    await requireOpsAccess(req);
    const data = await collectCanalRates();
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/ops/canal-ddl-audit') {
    await requireOpsAccess(req);
    const pageNo = Number.parseInt(url.searchParams.get('pageNo') || '1', 10);
    const pageSize = Number.parseInt(url.searchParams.get('pageSize') || '20', 10);
    const hours = Number.parseInt(url.searchParams.get('hours') || '168', 10);
    const status = url.searchParams.get('status') || null;
    const data = await listDdlAudit({ pageNo, pageSize, hours, status });
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/files/history') {
    const context = getRequestContext(req);
    requireLogin(context);
    const limit = Number.parseInt(url.searchParams.get('limit') || '50', 10);
    const list = await listUserExports(context, { limit });
    sendApiSuccess(res, { list });
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/pf-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handlePfMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/order-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handleOrderMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/order-count-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handleOrderCountMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/work-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handleWorkMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/attribution-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handleAttributionMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/sla-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handleSlaMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/dashboard/reissue-monthly') {
    const context = await requireDashboardAdmin(req);
    const data = await handleReissueMonthly(url);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/me/permissions') {
    const context = getRequestContext(req);
    requireLogin(context);
    const data = await collectMyAccess(context);
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/ops/audit-analytics') {
    await requireOpsAccess(req);
    const hours = Number.parseInt(url.searchParams.get('hours') || '24', 10);
    const data = await collectAuditAnalytics({ hours });
    sendApiSuccess(res, data);
    return;
  }

  if (req.method === 'GET' && pathname === '/conversations') {
    await handleConversationList(req, res, url);
    return;
  }

  const convMatch = CONVERSATION_ID_RE.exec(pathname);
  if (convMatch) {
    const conversationId = convMatch[1];
    if (req.method === 'GET') {
      await handleConversationDetail(req, res, conversationId, url);
      return;
    }
    if (req.method === 'PATCH') {
      await handleConversationRename(req, res, conversationId);
      return;
    }
    if (req.method === 'DELETE') {
      await handleConversationDelete(req, res, conversationId);
      return;
    }
  }

  throw new HttpError(404, 'Not found');
}

export function createHttpServer() {
  return http.createServer(async (req, res) => {
    try {
      await route(req, res);
    } catch (error) {
      if (!res.headersSent) {
        sendApiError(res, error);
      } else {
        res.end();
      }
    }
  });
}
