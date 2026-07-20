// Append-only audit trail for every api_call attempt. Writes are asynchronous
// and batched — never block the SSE handler, never fail the user request if
// MySQL is down. Table gets auto-created on first use so a fresh env doesn't
// need a migration. Reuses the same OA MySQL (kyx_oa) that
// aiConfigStore / conversationStore already talk to.
//
// Query examples the ops / compliance team will actually run:
//   -- 谁在打非白名单接口
//   SELECT user_id, path, COUNT(*) FROM kyx_agent_api_call_audit
//     WHERE code='NOT_ALLOWLISTED' AND created_at > NOW() - INTERVAL 1 DAY
//     GROUP BY user_id, path ORDER BY COUNT(*) DESC;
//   -- 最近谁问了什么
//   SELECT * FROM kyx_agent_api_call_audit
//     WHERE user_id = ? AND created_at > NOW() - INTERVAL 1 HOUR
//     ORDER BY created_at DESC;
//   -- 大盘：单日 api_call 总数、错误率、p95 耗时
//   SELECT DATE(created_at), COUNT(*), SUM(ok=0)/COUNT(*), MAX(duration_ms)
//     FROM kyx_agent_api_call_audit
//     WHERE created_at > NOW() - INTERVAL 7 DAY GROUP BY DATE(created_at);

import mysql from 'mysql2/promise';
import { config } from './config.js';

const FLUSH_INTERVAL_MS = 500;
const FLUSH_BATCH_MAX = 50;
const PARAMS_SUMMARY_MAX = 500;
const PATH_MAX = 255;

let pool = null;
let schemaReady = null;
let buffer = [];
let flushScheduled = false;
let disabled = false;

function getPool() {
  if (pool) return pool;
  pool = mysql.createPool({
    host: config.aiConfig.db.host,
    port: config.aiConfig.db.port,
    user: config.aiConfig.db.user,
    password: config.aiConfig.db.password,
    database: config.aiConfig.db.database,
    connectionLimit: 3,
    waitForConnections: true,
    queueLimit: 0,
    connectTimeout: config.aiConfig.db.connectTimeoutMs,
    dateStrings: true,
  });
  return pool;
}

async function ensureSchema() {
  if (schemaReady) return schemaReady;
  schemaReady = (async () => {
    await getPool().query(`
      CREATE TABLE IF NOT EXISTS kyx_agent_api_call_audit (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        tenant_id BIGINT NOT NULL DEFAULT 0,
        user_id BIGINT NOT NULL DEFAULT 0,
        username VARCHAR(64) NULL,
        request_id VARCHAR(64) NULL,
        domain VARCHAR(16) NULL,
        path VARCHAR(255) NOT NULL,
        method VARCHAR(8) NOT NULL DEFAULT 'GET',
        params_summary VARCHAR(500) NULL,
        ok TINYINT(1) NOT NULL DEFAULT 0,
        code VARCHAR(32) NULL,
        response_bytes INT UNSIGNED NULL,
        duration_ms INT UNSIGNED NULL,
        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        PRIMARY KEY (id),
        INDEX idx_user_time (tenant_id, user_id, created_at),
        INDEX idx_path_time (path, created_at),
        INDEX idx_error (ok, created_at)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        COMMENT='KYX agent api_call audit log (fire-and-forget, no back-pressure)';
    `);
  })().catch((err) => {
    schemaReady = null;
    throw err;
  });
  return schemaReady;
}

function summarizeParams(params) {
  if (!params || typeof params !== 'object') return null;
  try {
    const s = JSON.stringify(params);
    if (s.length <= PARAMS_SUMMARY_MAX) return s;
    return s.slice(0, PARAMS_SUMMARY_MAX - 1) + '…';
  } catch {
    return null;
  }
}

async function flush() {
  flushScheduled = false;
  if (disabled) return;
  if (buffer.length === 0) return;
  const rows = buffer.splice(0, FLUSH_BATCH_MAX);
  try {
    await ensureSchema();
    const values = rows.map((r) => [
      r.tenantId,
      r.userId,
      r.username,
      r.requestId,
      r.domain,
      r.path,
      r.method,
      r.paramsSummary,
      r.ok ? 1 : 0,
      r.code,
      r.responseBytes,
      r.durationMs,
    ]);
    await getPool().query(
      `INSERT INTO kyx_agent_api_call_audit
        (tenant_id, user_id, username, request_id, domain, path, method,
         params_summary, ok, code, response_bytes, duration_ms)
       VALUES ?`,
      [values],
    );
  } catch (err) {
    // Never bring down the agent because auditing is unhappy — log and drop.
    // If the DB is permanently gone we'll surface it via /actuator/health
    // later; for now the important thing is that failures don't affect
    // user-facing requests.
    console.warn(`[audit] flush failed (${rows.length} rows dropped): ${err.message}`);
  }
  if (buffer.length > 0) {
    scheduleFlush();
  }
}

function scheduleFlush() {
  if (flushScheduled || disabled) return;
  flushScheduled = true;
  setTimeout(flush, FLUSH_INTERVAL_MS).unref?.();
}

export function logApiCall(context, payload) {
  if (disabled) return;
  try {
    const userId = Number(context?.loginUser?.id ?? context?.loginUser?.userId ?? 0) || 0;
    const tenantId = Number(context?.tenantId ?? context?.loginUser?.tenantId ?? 0) || 0;
    const username = context?.loginUser?.username || context?.loginUser?.nickname || null;
    buffer.push({
      tenantId,
      userId,
      username: username ? String(username).slice(0, 64) : null,
      requestId: context?.requestId ? String(context.requestId).slice(0, 64) : null,
      domain: payload.domain ? String(payload.domain).slice(0, 16) : null,
      path: String(payload.path || '').slice(0, PATH_MAX),
      method: (payload.method || 'GET').toUpperCase().slice(0, 8),
      paramsSummary: summarizeParams(payload.params),
      ok: !!payload.ok,
      code: payload.code == null ? null : String(payload.code).slice(0, 32),
      responseBytes: Number.isFinite(payload.responseBytes) ? payload.responseBytes : null,
      durationMs: Number.isFinite(payload.durationMs) ? payload.durationMs : null,
    });
    if (buffer.length >= FLUSH_BATCH_MAX) {
      flush();
    } else {
      scheduleFlush();
    }
  } catch (err) {
    console.warn(`[audit] enqueue failed: ${err.message}`);
  }
}

export function _resetForTest() {
  buffer = [];
  disabled = false;
}

export function disableAuditForTest() {
  disabled = true;
  buffer = [];
}

export async function _flushForTest() {
  return flush();
}
