// 审计日志聚合分析 —— 给运维监控页"审计" tab 用
// 从 kyx_agent_api_call_audit 里聚合 24h/7d 指标：成功率、P95、慢查询、工具频次、错误分布
import mysql from 'mysql2/promise';
import { config } from './config.js';

let pool = null;
function getPool() {
  if (pool) return pool;
  pool = mysql.createPool({
    host: config.aiConfig.db.host,
    port: config.aiConfig.db.port,
    user: config.aiConfig.db.user,
    password: config.aiConfig.db.password,
    database: config.aiConfig.db.database,
    connectionLimit: 2,
    waitForConnections: true,
    connectTimeout: config.aiConfig.db.connectTimeoutMs,
    dateStrings: true,
  });
  return pool;
}

function num(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

export async function collectAuditAnalytics({ hours = 24 } = {}) {
  const p = getPool();
  const h = Math.max(1, Math.min(24 * 30, Number(hours) || 24));

  const [totalRows] = await p.query(
    `SELECT
        COUNT(*) AS total,
        SUM(CASE WHEN ok = 1 THEN 1 ELSE 0 END) AS ok_cnt,
        SUM(CASE WHEN ok = 0 THEN 1 ELSE 0 END) AS err_cnt,
        AVG(duration_ms) AS avg_ms,
        MAX(duration_ms) AS max_ms
      FROM kyx_agent_api_call_audit
      WHERE created_at > NOW() - INTERVAL ? HOUR`,
    [h],
  );
  const tt = totalRows[0] || {};
  const total = num(tt.total);
  const okCnt = num(tt.ok_cnt);
  const errCnt = num(tt.err_cnt);

  // MySQL 5.7/8 兼容的近似 P95 —— 排序取 95% 分位
  let p95 = 0;
  if (total > 0) {
    const [p95Rows] = await p.query(
      `SELECT duration_ms FROM kyx_agent_api_call_audit
        WHERE created_at > NOW() - INTERVAL ? HOUR
        ORDER BY duration_ms DESC
        LIMIT 1 OFFSET ?`,
      [h, Math.max(0, Math.floor(total * 0.05))],
    );
    p95 = num(p95Rows[0]?.duration_ms);
  }

  const [slowRows] = await p.query(
    `SELECT path, duration_ms, code, ok, created_at
       FROM kyx_agent_api_call_audit
       WHERE created_at > NOW() - INTERVAL ? HOUR
       ORDER BY duration_ms DESC
       LIMIT 10`,
    [h],
  );

  const [pathRows] = await p.query(
    `SELECT path, COUNT(*) AS cnt,
            SUM(CASE WHEN ok=0 THEN 1 ELSE 0 END) AS err_cnt,
            ROUND(AVG(duration_ms)) AS avg_ms
       FROM kyx_agent_api_call_audit
       WHERE created_at > NOW() - INTERVAL ? HOUR
       GROUP BY path
       ORDER BY cnt DESC
       LIMIT 15`,
    [h],
  );

  const [errRows] = await p.query(
    `SELECT code, COUNT(*) AS cnt
       FROM kyx_agent_api_call_audit
       WHERE created_at > NOW() - INTERVAL ? HOUR AND ok = 0
       GROUP BY code
       ORDER BY cnt DESC
       LIMIT 10`,
    [h],
  );

  const [userRows] = await p.query(
    `SELECT user_id, username, COUNT(*) AS cnt
       FROM kyx_agent_api_call_audit
       WHERE created_at > NOW() - INTERVAL ? HOUR
       GROUP BY user_id, username
       ORDER BY cnt DESC
       LIMIT 8`,
    [h],
  );

  const [trendRows] = await p.query(
    `SELECT
        DATE_FORMAT(created_at, '%Y-%m-%d %H:00') AS hour,
        COUNT(*) AS cnt,
        SUM(CASE WHEN ok=0 THEN 1 ELSE 0 END) AS err_cnt
       FROM kyx_agent_api_call_audit
       WHERE created_at > NOW() - INTERVAL ? HOUR
       GROUP BY hour
       ORDER BY hour ASC`,
    [h],
  );

  return {
    windowHours: h,
    totals: {
      total,
      ok: okCnt,
      err: errCnt,
      successRate: total > 0 ? okCnt / total : 0,
      avgMs: Math.round(num(tt.avg_ms)),
      p95Ms: p95,
      maxMs: num(tt.max_ms),
    },
    slowest: slowRows.map((r) => ({
      path: r.path,
      duration_ms: num(r.duration_ms),
      code: r.code || null,
      ok: r.ok === 1,
      at: r.created_at,
    })),
    topPaths: pathRows.map((r) => ({
      path: r.path,
      cnt: num(r.cnt),
      err_cnt: num(r.err_cnt),
      avg_ms: num(r.avg_ms),
    })),
    errorBreakdown: errRows.map((r) => ({
      code: r.code || '(null)',
      cnt: num(r.cnt),
    })),
    topUsers: userRows.map((r) => ({
      user_id: num(r.user_id),
      username: r.username || `id=${r.user_id}`,
      cnt: num(r.cnt),
    })),
    trend: trendRows.map((r) => ({
      hour: r.hour,
      cnt: num(r.cnt),
      err_cnt: num(r.err_cnt),
    })),
  };
}
