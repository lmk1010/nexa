// 运维监控：DDL 变更审计
//
// 数据源：warehouse 的 _canal_ddl_applied 表（nexa-cdc 每次处理 QueryEvent 都会落一行）
// 字段：id, ts, hostname, src_schema, src_table, target_table, binlog_pos,
//       status(applied/skipped/pending/failed), source_stmt, applied_stmt, err_msg

import mysql from 'mysql2/promise';

const CFG = {
  warehouse: {
    host: process.env.OPS_WH_HOST || 'kyx-warehouse',
    port: Number.parseInt(process.env.OPS_WH_PORT || '3306', 10),
    user: process.env.OPS_WH_USER || 'agent_ro',
    password: process.env.OPS_WH_PASS || 'agent_ro_123',
    database: process.env.OPS_WH_DB || 'ordersys_dw',
  },
};

let pool = null;
function getPool() {
  if (!pool) {
    pool = mysql.createPool({
      ...CFG.warehouse,
      connectionLimit: 2,
      waitForConnections: true,
      connectTimeout: 3000,
      dateStrings: true,
    });
  }
  return pool;
}

const ALLOWED_STATUSES = new Set(['applied', 'skipped', 'pending', 'failed']);

// 主入口 —— 分页 + 可选状态过滤 + 按 hours 时间窗
export async function listDdlAudit({ pageNo = 1, pageSize = 20, hours = 168, status = null } = {}) {
  const page = Math.max(1, Number(pageNo) || 1);
  const size = Math.min(100, Math.max(1, Number(pageSize) || 20));
  const offset = (page - 1) * size;
  const windowH = Math.max(1, Math.min(24 * 30, Number(hours) || 168)); // 最多 30 天

  const filters = ['ts > NOW() - INTERVAL ? HOUR'];
  const params = [windowH];
  if (status && ALLOWED_STATUSES.has(status)) {
    filters.push('status = ?');
    params.push(status);
  }
  const whereSql = 'WHERE ' + filters.join(' AND ');

  try {
    const p = getPool();
    const [rows] = await p.query(
      `SELECT id, ts, hostname, src_schema, src_table, target_table, binlog_pos,
              status, source_stmt, applied_stmt, err_msg
       FROM _canal_ddl_applied
       ${whereSql}
       ORDER BY id DESC
       LIMIT ? OFFSET ?`,
      [...params, size, offset],
    );
    const [countRes] = await p.query(
      `SELECT COUNT(*) AS n FROM _canal_ddl_applied ${whereSql}`,
      params,
    );
    const total = Number(countRes[0]?.n || 0);

    // 顺便算个 status 分布，前端直接展示
    const [distRes] = await p.query(
      `SELECT status, COUNT(*) AS cnt FROM _canal_ddl_applied
       WHERE ts > NOW() - INTERVAL ? HOUR
       GROUP BY status`,
      [windowH],
    );
    const dist = { applied: 0, skipped: 0, pending: 0, failed: 0 };
    for (const r of distRes) {
      if (dist.hasOwnProperty(r.status)) dist[r.status] = Number(r.cnt);
    }

    return {
      pageNo: page,
      pageSize: size,
      total,
      hasMore: offset + rows.length < total,
      windowHours: windowH,
      status,
      distribution: dist,
      list: rows,
    };
  } catch (err) {
    // 首次部署 warehouse 里可能还没这张表 —— 优雅退化，不报 500
    if (isTableMissing(err)) {
      return {
        pageNo: page,
        pageSize: size,
        total: 0,
        hasMore: false,
        windowHours: windowH,
        status,
        distribution: { applied: 0, skipped: 0, pending: 0, failed: 0 },
        list: [],
        note: '_canal_ddl_applied 尚未建表（nexa-cdc 首次启动会自动建）',
      };
    }
    throw err;
  }
}

function isTableMissing(err) {
  const msg = String(err?.message || '');
  return msg.includes("doesn't exist") || err?.code === 'ER_NO_SUCH_TABLE';
}

export function _resetForTest() {
  pool = null;
}
