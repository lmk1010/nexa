// Mechanical short-circuit for endpoints known to be down. NOT a fallback —
// we do NOT reroute to a different endpoint. We just tell the LLM "this one
// is dead right now" via `cached_failure:true` on the result. The LLM decides
// whether to try something else or answer the user.
//
// This is the "事实" cache (a fact about the system's current state), not the
// "规则" cache (a rule about what to do). The LLM stays in charge.

const cache = new Map(); // key -> { until, response }

const TTL = {
  // Service unavailable / instance missing — usually persists for a while.
  UNAVAILABLE: 60_000,
  // Auth failures re-authenticate on the next login; for the lifetime of one
  // request we won't recover, so cache until end-of-request (see key scoping).
  UNAUTHORIZED: 30_000,
  // Not found is stable-ish; cache modestly.
  NOT_FOUND: 60_000,
};

const MAX_ENTRIES = 500;

function normalizePath(path) {
  return String(path || '').replace(/\/\d+(?=\/|$)/g, '/:id');
}

function keyFor(context, path) {
  const tenant = context?.tenantId ?? 'anon';
  return `${tenant}:${normalizePath(path)}`;
}

function evictIfFull() {
  if (cache.size <= MAX_ENTRIES) return;
  // Drop the 25% oldest entries by insertion order.
  const drop = Math.floor(MAX_ENTRIES * 0.25);
  let i = 0;
  for (const k of cache.keys()) {
    if (i >= drop) break;
    cache.delete(k);
    i += 1;
  }
}

function classify(response) {
  const status = response?.status;
  const code = response?.data?.code;
  const msg = String(response?.data?.msg || response?.data?.message || '');
  if (code === 503 || /Unable to find instance/i.test(msg)) {
    return { kind: 'UNAVAILABLE', ttl: TTL.UNAVAILABLE };
  }
  if (status === 401 || code === 401) {
    return { kind: 'UNAUTHORIZED', ttl: TTL.UNAUTHORIZED };
  }
  if (status === 403 || code === 403) {
    return { kind: 'FORBIDDEN', ttl: TTL.UNAUTHORIZED };
  }
  if (status === 404 || code === 404) {
    return { kind: 'NOT_FOUND', ttl: TTL.NOT_FOUND };
  }
  return null;
}

export function shortCircuit(context, path) {
  const k = keyFor(context, path);
  const entry = cache.get(k);
  if (!entry) return null;
  if (entry.until <= Date.now()) {
    cache.delete(k);
    return null;
  }
  return {
    ...entry.response,
    cached_failure: true,
    first_seen_at: entry.firstSeenAt,
    ttl_remaining_ms: entry.until - Date.now(),
    hint: '这个接口最近确认返回该错误。工具层不做自动重试或改路径 — 请自行判断换个 api_search 关键词或告知用户。',
  };
}

export function remember(context, path, response) {
  const cls = classify(response);
  if (!cls) return;
  const k = keyFor(context, path);
  evictIfFull();
  const existing = cache.get(k);
  cache.set(k, {
    until: Date.now() + cls.ttl,
    firstSeenAt: existing?.firstSeenAt || new Date().toISOString(),
    response: {
      ok: false,
      status: response.status ?? 0,
      code: response.data?.code ?? cls.kind,
      msg: response.data?.msg || response.data?.message || cls.kind,
      failure_kind: cls.kind,
    },
  });
}

export function _resetForTest() {
  cache.clear();
}

export function snapshot() {
  return {
    size: cache.size,
    max: MAX_ENTRIES,
    entries: [...cache.entries()].map(([k, v]) => ({
      key: k,
      kind: v.response.failure_kind,
      ttl_ms: Math.max(0, v.until - Date.now()),
    })),
  };
}
