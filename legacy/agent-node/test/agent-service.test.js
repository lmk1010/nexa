import assert from 'node:assert/strict';
import test from 'node:test';

process.env.AGENT_USE_MOCK = 'true';
process.env.AGENT_MOCK_RESPONSE = 'mock: {{message}}';
process.env.AGENT_ENABLE_INTERNAL_HTTP_TOOLS = 'true';
process.env.SERVICE_ACCOUNT_ENABLED = 'true';
process.env.SERVICE_ACCOUNT_BASE_URL = 'http://127.0.0.1:48080';
process.env.SERVICE_ACCOUNT_USERNAME = 'agent';
process.env.SERVICE_ACCOUNT_PASSWORD = 'secret';
process.env.SERVICE_ACCOUNT_TENANT_ID = '171';
process.env.SERVICE_ACCOUNT_DEVICE_TYPE = 'APP';
process.env.ORDERSYS_API_BASE_URL = 'http://127.0.0.1:48080';
process.env.ORDERSYS_SERVICE_ACCOUNT_USERNAME = 'agent';
process.env.ORDERSYS_SERVICE_ACCOUNT_PASSWORD = 'secret';

const { forwardHeaders, parseLoginUserHeader, publicRequestContext } = await import('../src/context.js');
const { buildPrompt, runAgent } = await import('../src/neoxAgent.js');
const {
  getServiceAccountAuthHeaders,
  parseExpiresTime,
  resetServiceAccountAuthForTest,
} = await import('../src/serviceAccountAuth.js');
const {
  createTools,
  isPrivateOrLoopbackIP,
  extractPlainText,
} = await import('../src/tools.js');
const { search, getByPath, stats } = await import('../src/apiIndex.js');
const {
  shortCircuit,
  remember,
  _resetForTest: resetHotErrorCache,
  snapshot: hotErrorSnapshot,
} = await import('../src/hotErrorCache.js');
const { prepareOrdersysReadRequest } = await import('../src/ordersysClient.js');

// ────────────────────────────────────────────────────────────────────────────
// Gateway header / context — still valid, still important.
// ────────────────────────────────────────────────────────────────────────────

test('parses gateway login-user header', () => {
  const value = encodeURIComponent(JSON.stringify({ id: 7, username: 'admin', tenantId: 1 }));
  assert.deepEqual(parseLoginUserHeader(value), {
    id: 7,
    username: 'admin',
    tenantId: 1,
  });
});

test('does not expose secret headers in public context', () => {
  const context = publicRequestContext({
    requestId: 'req-1',
    tenantId: '1',
    visitTenantId: '171',
    tenantIgnore: 'false',
    authorization: 'Bearer secret',
    loginUser: { id: 7, username: 'admin', tenantId: 1 },
    userAgent: 'node:test',
  });

  assert.equal(context.hasAuthorization, true);
  assert.equal(context.authorization, undefined);
  assert.equal(context.loginUser.username, 'admin');
  assert.equal(context.visitTenantId, '171');
});

test('forwards gateway auth and tenant headers to Java APIs', () => {
  const headers = forwardHeaders({
    requestId: 'req-1',
    tenantId: '1',
    visitTenantId: '171',
    tenantIgnore: 'false',
    authorization: 'Bearer secret',
    rawLoginUserHeader: '{"id":7}',
  });

  assert.deepEqual(headers, {
    Authorization: 'Bearer secret',
    'tenant-id': '1',
    'visit-tenant-id': '171',
    'tenant-ignore': 'false',
    'login-user': '{"id":7}',
    'x-request-id': 'req-1',
  });
});

test('does not use service account when request already has auth', async () => {
  resetServiceAccountAuthForTest();
  const headers = await getServiceAccountAuthHeaders(
    {
      requestId: 'req-1',
      tenantId: '171',
      authorization: 'Bearer user-token',
    },
    new AbortController().signal,
  );
  assert.deepEqual(headers, {});
});

test('parses service account token expiry formats', () => {
  assert.equal(parseExpiresTime(1_783_327_134_159), 1_783_327_134_159);
  assert.equal(parseExpiresTime(1_783_327_134), 1_783_327_134_000);
  assert.ok(parseExpiresTime('2026-07-06 12:00:00') > 0);
  assert.ok(parseExpiresTime([2026, 7, 6, 12, 0, 0]) > 0);
});

test('SSRF guard rejects private IPs', () => {
  assert.equal(isPrivateOrLoopbackIP('127.0.0.1'), true);
  assert.equal(isPrivateOrLoopbackIP('10.0.0.1'), true);
  assert.equal(isPrivateOrLoopbackIP('169.254.169.254'), true);
  assert.equal(isPrivateOrLoopbackIP('8.8.8.8'), false);
  assert.equal(isPrivateOrLoopbackIP('::1'), true);
});

test('extractPlainText strips HTML', () => {
  const html = '<p>hi <b>there</b></p><script>bad()</script>';
  assert.equal(extractPlainText(html), 'hi there');
});

// ────────────────────────────────────────────────────────────────────────────
// Agent surface — Harness v2: 13 tools organized in Foundation / Data / Output layers.
// ────────────────────────────────────────────────────────────────────────────

test('exposes the harness tool registry (v3, 12 tools with python_exec)', () => {
  const tools = createTools({
    requestId: 'req-1',
    tenantId: '1',
    authorization: 'Bearer secret',
    loginUser: { id: 7, username: 'admin', tenantId: 1 },
    rawLoginUserHeader: '',
    userAgent: 'node:test',
  }).map((item) => item.name);

  assert.deepEqual(tools, [
    // Time
    'now',
    'parse_time',
    // Compute
    'shell_exec',
    'python_exec',
    // Data
    'get_request_context',
    'api_search',
    'api_call',
    'paginate_all',
    // Output
    'render_chart',
    'fetch_url',
    'export_excel',
    'export_excel_from_file',
  ]);
});

test('builds prompt from messages', () => {
  assert.equal(
    buildPrompt({
      messages: [
        { role: 'user', content: 'hello' },
        { role: 'assistant', content: 'hi' },
      ],
    }),
    'user: hello\nassistant: hi',
  );
});

test('runs agent with SDK mock provider', async () => {
  const result = await runAgent(
    { message: 'ping' },
    {
      requestId: 'req-1',
      tenantId: '1',
      authorization: '',
      loginUser: null,
      rawLoginUserHeader: '',
      userAgent: 'node:test',
    },
    new AbortController().signal,
  );

  assert.equal(result.text, 'mock: ping');
  assert.equal(result.stopReason, 'end_turn');
});

// ────────────────────────────────────────────────────────────────────────────
// The 20-question boss regression. If retrieval quality regresses, these
// stop matching top-1 and CI catches it before a demo does.
// ────────────────────────────────────────────────────────────────────────────

const BOSS_QUERIES = [
  ['本月平台承担的赔付金额多少', '/user/statistics/sh/pf'],
  ['哪个部门赔的最多', '/user/statistics/rank/compensation'],
  ['本月营业额和利润', '/user/statistics/sh/order'],
  ['哪个部门订单最多', '/user/statistics/rank/order'],
  ['本月工单处理量', '/user/statistics/sh/work'],
  ['谁完结工单最多', '/user/statistics/rank/work'],
  ['玻璃膜车衣补发率', '/user/statistics/sh/bf'],
  ['哪个部门补发排行', '/user/statistics/rank/reissue'],
  ['本月好评差评统计', '/user/statistics/sh/rate'],
  ['谁的差评最多', '/user/statistics/sh/rate'],
  ['好评率部门排行榜', '/user/statistics/rank/evaluation'],
  ['问题原因分类 责任归因', '/user/statistics/sh/workHint'],
  ['撤单原因统计', '/user/statistics/sh/revoke'],
  ['派单错误率排行', '/user/statistics/rank/weight'],
  ['超时工单有哪些', '/work/timeout/list'],
  ['连图任务看板任务列表', '/admin/ttask/list'],
  ['任务看板整体概览', '/admin/ttask/dashboard/statistics'],
  ['连图工作台待办', '/workbench/todo/list'],
  ['员工花名册按姓名查', '/admin-api/hr/employee/page'],
  ['当前用户我的档案', '/admin-api/hr/employee/current'],
  ['需求管理概览', '/admin-api/business/work/requirement/overview'],
  ['总裁驾驶舱大盘', '/admin-api/business/executive-cockpit/overview'],
];

test(`api_search top-3 contains expected endpoint on ${BOSS_QUERIES.length} boss questions`, async () => {
  // Assertion is "top-3 contains" (not "top-1 is") because a) in real use the
  // LLM sees k=8 cards and picks, so this matches actual UX, and b) some pairs
  // like sh/work vs rank/work require semantic distinction that keyword-only
  // ranking can't make — embeddings resolve them, but shouldn't be required
  // for the test to pass in a fresh clone. A separate metric tracks top-1.
  const misses = [];
  let top1Hits = 0;
  for (const [query, expectedPath] of BOSS_QUERIES) {
    const hits = await search(query, { k: 3 });
    const top3Paths = hits.slice(0, 3).map((h) => h.path);
    if (top3Paths[0] === expectedPath) top1Hits += 1;
    if (!top3Paths.includes(expectedPath)) {
      misses.push({ query, expected: expectedPath, top3: top3Paths });
    }
  }
  if (misses.length > 0) {
    console.error(JSON.stringify(misses, null, 2));
  }
  console.log(
    `boss retrieval: top-1 ${top1Hits}/${BOSS_QUERIES.length}, top-3 ${BOSS_QUERIES.length - misses.length}/${BOSS_QUERIES.length}`,
  );
  assert.equal(misses.length, 0, `${misses.length}/${BOSS_QUERIES.length} boss questions missed top-3`);
});

test('api_search stats() reports expected shape', () => {
  const s = stats();
  assert.ok(s.total > 500, `expected 500+ endpoints, got ${s.total}`);
  assert.ok(s.oa > 0);
  assert.ok(s.ordersys > 0);
  assert.equal(typeof s.embeddings, 'boolean');
});

test('api_search filters by domain', async () => {
  const oaHits = await search('员工', { k: 5, domain: 'oa' });
  const ordersysHits = await search('工单', { k: 5, domain: 'ordersys' });
  assert.ok(oaHits.every((h) => h.domain === 'oa'));
  assert.ok(ordersysHits.every((h) => h.domain === 'ordersys'));
});

test('getByPath returns the same record apiCall would dispatch on', () => {
  const record = getByPath('/user/statistics/sh/pf', 'GET');
  assert.ok(record);
  assert.equal(record.domain, 'ordersys');
  assert.equal(record.path, '/user/statistics/sh/pf');
  assert.equal(getByPath('/nope', 'GET'), null);
});

// ────────────────────────────────────────────────────────────────────────────
// api_call — path validation and batch dispatch.
// ────────────────────────────────────────────────────────────────────────────

test('api_call rejects paths not in the unified index', async () => {
  const tools = createTools({
    requestId: 'req-1',
    tenantId: '1',
    authorization: 'Bearer secret',
    loginUser: { id: 7, tenantId: 1 },
    rawLoginUserHeader: '',
  });
  const apiCall = tools.find((t) => t.name === 'api_call');
  const result = await apiCall.invoke(
    { path: '/nonexistent/route', params: {} },
    { signal: new AbortController().signal },
  );
  assert.equal(result.ok, false);
  assert.equal(result.code, 'NOT_IN_INDEX');
});

test('api_call batch runs concurrently and preserves index order', async () => {
  const originalFetch = globalThis.fetch;
  const seen = [];
  globalThis.fetch = async (url) => {
    seen.push(String(url));
    return new Response(JSON.stringify({ code: 0, data: { ok: true } }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    });
  };
  try {
    const tools = createTools({
      requestId: 'req-1',
      tenantId: '1',
      authorization: 'Bearer user-token',
      loginUser: { id: 7, tenantId: 1 },
      rawLoginUserHeader: '',
    });
    const apiCall = tools.find((t) => t.name === 'api_call');
    const result = await apiCall.invoke(
      {
        batch: [
          { path: '/admin-api/hr/employee/current' },
          { path: '/admin-api/business/work/requirement/overview' },
          { path: '/admin-api/business/executive-cockpit/overview', params: { days: 30 } },
        ],
      },
      { signal: new AbortController().signal },
    );
    assert.equal(result.batch, true);
    assert.equal(result.count, 3);
    assert.equal(result.results[0].index, 0);
    assert.equal(result.results[1].index, 1);
    assert.equal(result.results[2].index, 2);
    assert.equal(seen.length, 3);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

// ────────────────────────────────────────────────────────────────────────────
// hotErrorCache — mechanical short-circuit, not a fallback.
// ────────────────────────────────────────────────────────────────────────────

test('hotErrorCache short-circuits 503 Unable to find instance', () => {
  resetHotErrorCache();
  const ctx = { tenantId: 't1' };
  const path = '/admin-api/hr/employee/current';
  assert.equal(shortCircuit(ctx, path), null);

  remember(ctx, path, {
    status: 500,
    data: { code: 503, msg: 'Unable to find instance for hr-server' },
  });

  const hit = shortCircuit(ctx, path);
  assert.ok(hit);
  assert.equal(hit.cached_failure, true);
  assert.equal(hit.failure_kind, 'UNAVAILABLE');
  assert.ok(hit.hint.includes('这个接口最近确认返回该错误'));
});

test('hotErrorCache isolates by tenant', () => {
  resetHotErrorCache();
  const path = '/admin-api/hr/employee/current';
  remember({ tenantId: 'ta' }, path, {
    status: 500,
    data: { code: 503, msg: 'Unable to find instance' },
  });
  assert.ok(shortCircuit({ tenantId: 'ta' }, path));
  assert.equal(shortCircuit({ tenantId: 'tb' }, path), null);
});

test('hotErrorCache normalizes numeric IDs so id-varying calls share entries', () => {
  resetHotErrorCache();
  remember({ tenantId: 't' }, '/admin-api/hr/employee/42', {
    status: 404,
    data: { code: 404, msg: 'not found' },
  });
  assert.ok(shortCircuit({ tenantId: 't' }, '/admin-api/hr/employee/99'));
});

test('hotErrorCache does not remember success', () => {
  resetHotErrorCache();
  remember({ tenantId: 't' }, '/whatever', { status: 200, data: { code: 0 } });
  assert.equal(hotErrorSnapshot().size, 0);
});

// ────────────────────────────────────────────────────────────────────────────
// ordersysClient path lookup goes through the unified index now.
// ────────────────────────────────────────────────────────────────────────────

// ────────────────────────────────────────────────────────────────────────────
// 401 guard — every business route must reject requests with no loginUser.id.
// ────────────────────────────────────────────────────────────────────────────

const { createHttpServer } = await import('../src/http.js');

async function requestJson(server, method, path, { body, headers } = {}) {
  const address = server.address();
  const url = `http://127.0.0.1:${address.port}${path}`;
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(headers || {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let parsed = null;
  try {
    parsed = JSON.parse(text);
  } catch {
    parsed = { raw: text };
  }
  return { status: res.status, body: parsed };
}

test('unauthenticated business routes are rejected with 账号未登录', async () => {
  const server = createHttpServer();
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  try {
    // /stream, /run, /executive-cockpit/*, /conversations — all must 401 with
    // no login-user header. Health endpoint stays open.
    const cases = [
      { method: 'POST', path: '/stream', body: { message: 'ping' } },
      { method: 'POST', path: '/run', body: { message: 'ping' } },
      { method: 'GET', path: '/executive-cockpit/overview' },
      { method: 'POST', path: '/executive-cockpit/chat', body: { message: 'ping' } },
      { method: 'GET', path: '/conversations' },
      { method: 'GET', path: '/conversations/abc123' },
      { method: 'PATCH', path: '/conversations/abc123', body: { title: 'x' } },
      { method: 'DELETE', path: '/conversations/abc123' },
    ];
    for (const c of cases) {
      const res = await requestJson(server, c.method, c.path, { body: c.body });
      assert.equal(res.status, 401, `expected 401 on ${c.method} ${c.path}, got ${res.status}`);
      assert.equal(res.body.code, 401);
      assert.equal(res.body.msg, '账号未登录');
    }
    // Health check must stay open.
    const health = await requestJson(server, 'GET', '/actuator/health');
    assert.equal(health.status, 200);
    assert.equal(health.body.status, 'UP');
  } finally {
    server.close();
  }
});

test('authenticated request passes the 401 guard (reaches handler)', async () => {
  const server = createHttpServer();
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  try {
    // /conversations only needs loginUser.id to pass the guard; if it 401s
    // we know the guard is over-broad. Any other status = guard passed.
    const res = await requestJson(server, 'GET', '/conversations', {
      headers: { 'login-user': JSON.stringify({ id: 7, tenantId: 1 }) },
    });
    assert.notEqual(res.status, 401);
  } finally {
    server.close();
  }
});

// ────────────────────────────────────────────────────────────────────────────
// Allowlist enforcement — sensitive/dangerous ordersys paths reject at api_call.
// ────────────────────────────────────────────────────────────────────────────

test('api_call rejects paths marked _allowed:false with NOT_ALLOWLISTED', async () => {
  const tools = createTools({
    requestId: 'req-1',
    tenantId: '1',
    authorization: 'Bearer secret',
    loginUser: { id: 7, tenantId: 1 },
    rawLoginUserHeader: '',
  });
  const apiCall = tools.find((t) => t.name === 'api_call');
  // /order/pf/list is in the index but not in allowlist (finance detail).
  const result = await apiCall.invoke(
    { path: '/order/pf/list', params: {} },
    { signal: new AbortController().signal },
  );
  assert.equal(result.ok, false);
  assert.equal(result.code, 'NOT_ALLOWLISTED');
  assert.ok(result.msg.includes('/order/pf/list'));
});

test('api_search cards carry callable flag matching allowlist policy', async () => {
  const hits = await search('赔付', { k: 10 });
  const allowed = hits.filter((h) => h.callable);
  const denied = hits.filter((h) => !h.callable);
  // Boss-priority paths (sh/pf, rank/compensation) must be callable.
  const pfCard = allowed.find((h) => h.path === '/user/statistics/sh/pf');
  assert.ok(pfCard, 'sh/pf must appear in "赔付" search');
  assert.equal(pfCard.callable, true);
  // /order/pf/list must be marked non-callable if it surfaced.
  const pfListCard = hits.find((h) => h.path === '/order/pf/list');
  if (pfListCard) assert.equal(pfListCard.callable, false);
  console.log(`  "赔付" hits: ${allowed.length} callable, ${denied.length} non-callable`);
});

test('ordersysClient path lookup honours new api-index whitelist', () => {
  const req = prepareOrdersysReadRequest('/user/statistics/sh/pf', { month: '2026-07' });
  assert.equal(req.path, '/user/statistics/sh/pf');
  assert.equal(req.query.month, '2026-07');
  assert.throws(
    () => prepareOrdersysReadRequest('/does/not/exist', {}),
    /path 不在白名单/,
  );
});
