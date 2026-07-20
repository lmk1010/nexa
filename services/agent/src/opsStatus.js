// 运维监控数据聚合 —— 给 APP 里的"同步监控 tab"用
// 所有查询都是只读的、轻量的：
//   - CDC service 存活：HTTP GET nexa-cdc:6060/health（Go 版 nexa-cdc）
//     （旧 canal-server:11111 已弃用，2026-07-09 之后 nexa-cdc 生效）
//   - CDC service 内部 stats：HTTP GET nexa-cdc:6060/stats（binlog 位点、总行数等）
//   - warehouse 心跳：读 _canal_stats.__heartbeat__ 最近一条（nexa-cdc 每 60s 写）
//   - CDB 位点：唯一一次 SHOW MASTER STATUS（read-only 元命令）
//   - warehouse 表行数 + 24h 活动：本机 SELECT
//   - 最近错误：warehouse._canal_errors

import net from 'node:net';
import mysql from 'mysql2/promise';

import { parseMetrics } from './opsRates.js';

const OPS_CFG = {
  // nexa-cdc（Go 版）替代了 canal-server + canal-consumer
  cdcHost: process.env.OPS_CDC_HOST || 'nexa-cdc',
  cdcHttpPort: Number.parseInt(process.env.OPS_CDC_HTTP_PORT || '6060', 10),
  warehouse: {
    host: process.env.OPS_WH_HOST || 'kyx-warehouse',
    port: Number.parseInt(process.env.OPS_WH_PORT || '3306', 10),
    user: process.env.OPS_WH_USER || 'agent_ro',
    password: process.env.OPS_WH_PASS || 'agent_ro_123',
    database: process.env.OPS_WH_DB || 'ordersys_dw',
  },
  cdb: {
    host: process.env.OPS_CDB_HOST || 'nj-cdb-40g509zj.sql.tencentcdb.com',
    port: Number.parseInt(process.env.OPS_CDB_PORT || '29355', 10),
    user: process.env.OPS_CDB_USER || 'canal',
    password: process.env.OPS_CDB_PASS || 'Kyx123456++',
  },
  // 硬编码表列表只作为 nexa-cdc /metrics 不可达时的兜底。
  // 正常情况下由 discoverSyncedTables() 从 /metrics 动态拉，永远不再"漏表"。
  fallbackTables: (
    process.env.OPS_TABLES ||
    't_task,t_task_assignee,t_task_subtask,t_order_pf,t_order_pf_seller,t_order_revoke,sys_user,sys_dept,' +
    't_order,t_work,t_work_order,t_order_detail,sys_seller,outlets_shop,' +
    't_order_time,t_order_reissue,t_work_order_timeout,t_project,t_transaction_record,t_work_comment'
  ).split(',').map((s) => s.trim()).filter(Boolean),
};

let whPool = null;
function getWhPool() {
  if (!whPool) {
    whPool = mysql.createPool({
      ...OPS_CFG.warehouse,
      connectionLimit: 2,
      waitForConnections: true,
      connectTimeout: 3000,
      dateStrings: true,
    });
  }
  return whPool;
}

let cdbConn = null;
async function getCdbConn() {
  if (!cdbConn) {
    try {
      cdbConn = await mysql.createConnection({
        ...OPS_CFG.cdb,
        connectTimeout: 5000,
      });
    } catch (err) {
      cdbConn = null;
      throw err;
    }
  }
  return cdbConn;
}

function probeTcp(host, port, timeoutMs = 2000) {
  return new Promise((resolve) => {
    const sock = new net.Socket();
    let done = false;
    const finish = (ok) => {
      if (done) return;
      done = true;
      try { sock.destroy(); } catch {}
      resolve(ok);
    };
    sock.setTimeout(timeoutMs);
    sock.once('connect', () => finish(true));
    sock.once('timeout', () => finish(false));
    sock.once('error', () => finish(false));
    sock.connect(port, host);
  });
}

async function getCdbMaster() {
  try {
    const conn = await getCdbConn();
    const [rows] = await conn.query('SHOW MASTER STATUS');
    if (!rows[0]) return null;
    return {
      file: rows[0].File,
      position: Number(rows[0].Position),
      gtid_set: rows[0].Executed_Gtid_Set || null,
    };
  } catch (err) {
    // 连接可能被断了，下次重连
    cdbConn = null;
    return { error: err.message };
  }
}

async function getConsumerLastHeartbeat() {
  try {
    const [rows] = await getWhPool().query(
      `SELECT MAX(ts) AS last_ts FROM _canal_stats WHERE ts > NOW() - INTERVAL 5 MINUTE`,
    );
    return rows[0]?.last_ts ? new Date(rows[0].last_ts) : null;
  } catch {
    return null;
  }
}

// 从 nexa-cdc /metrics 里拉当前实际在同步的表列表 —— 60s 缓存避免频繁 parse。
let discoveryCache = { at: 0, tables: null };
async function discoverSyncedTables() {
  const now = Date.now();
  if (discoveryCache.tables && (now - discoveryCache.at) < 60 * 1000) {
    return discoveryCache.tables;
  }
  const url = `http://${OPS_CFG.cdcHost}:${OPS_CFG.cdcHttpPort}/metrics`;
  try {
    const ac = new AbortController();
    const timer = setTimeout(() => ac.abort(), 2500);
    const res = await fetch(url, { signal: ac.signal });
    clearTimeout(timer);
    if (!res.ok) return OPS_CFG.fallbackTables;
    const text = await res.text();
    const m = parseMetrics(text);
    // /metrics 里每个 counter 系列都有 table 标签，取并集即为当前同步的所有表
    const names = Object.keys(m.byTable);
    if (names.length === 0) return OPS_CFG.fallbackTables;
    names.sort();
    discoveryCache = { at: now, tables: names };
    return names;
  } catch {
    return OPS_CFG.fallbackTables;
  }
}

async function getTableRowCounts() {
  const pool = getWhPool();
  const tables = await discoverSyncedTables();

  // 一次性拿 information_schema —— 免费获得 size + approx_rows（不精确但很快）。
  // 老实现每次刷新对每张表做 COUNT(*)，最慢那张（t_work_comment 1400w 行）单表 1.4s，
  // 一次刷新 2 秒起步，APP 一直转圈。改成 approx 后整个函数 <100ms。
  let sizeMap = new Map();
  try {
    const [sz] = await pool.query(
      `SELECT table_name,
              data_length + index_length AS bytes,
              table_rows AS approx_rows
       FROM information_schema.TABLES
       WHERE table_schema = DATABASE()`,
    );
    for (const r of sz) {
      sizeMap.set(String(r.table_name), {
        bytes: Number(r.bytes || 0),
        approxRows: Number(r.approx_rows || 0),
      });
    }
  } catch {
    // information_schema 拿不到就算了，size 字段返 null
  }

  // 一次 GROUP BY 拿所有表的 24h 累积 —— 比逐表 N 次查询快很多
  const actMap = new Map();
  try {
    const [act] = await pool.query(
      `SELECT table_name,
              SUM(ins_count) AS ins24, SUM(upd_count) AS upd24,
              SUM(del_count) AS del24, SUM(err_count) AS err24,
              MAX(last_apply_at) AS last_apply_at
       FROM _canal_stats
       WHERE ts > NOW() - INTERVAL 1 DAY
       GROUP BY table_name`,
    );
    for (const r of act) {
      actMap.set(String(r.table_name), r);
    }
  } catch {
    // _canal_stats 拿不到就 0 填充
  }

  return tables.map((name) => {
    const act = actMap.get(name) || {};
    const sz = sizeMap.get(name) || {};
    const ins = Number(act.ins24 || 0);
    const upd = Number(act.upd24 || 0);
    const del = Number(act.del24 || 0);
    const err = Number(act.err24 || 0);
    const total = ins + upd + del + err;
    const errorRate = total > 0 ? err / total : 0;
    return {
      name,
      // approx_rows 来自 information_schema —— InnoDB 表估算，跟真值差 1-5%，
      // 运维监控完全够用；换来响应从 2s+ 到 <100ms
      rows: sz.approxRows || 0,
      rows_approx: true,
      size_bytes: sz.bytes || null,
      size_mb: sz.bytes ? Math.round((sz.bytes / 1024 / 1024) * 100) / 100 : null,
      ops_24h: { ins, upd, del, err, total },
      error_rate: Math.round(errorRate * 10000) / 100,
      last_apply_at: act.last_apply_at || null,
    };
  });
}

async function getRecentErrors(limit = 5) {
  try {
    // 只显示**近 24 小时**的错误 —— 老错误不该一直挂在页面上
    const [rows] = await getWhPool().query(
      `SELECT ts, table_name, event_type, row_id, err_msg
       FROM _canal_errors
       WHERE ts > NOW() - INTERVAL 24 HOUR
       ORDER BY ts DESC LIMIT ?`,
      [limit],
    );
    return rows;
  } catch {
    return [];
  }
}

// 分页版本 —— 独立 endpoint 用，配合 APP 的"查看更多"按钮
export async function listCanalErrors({ pageNo = 1, pageSize = 10, hours = 24 } = {}) {
  const page = Math.max(1, Number(pageNo) || 1);
  const size = Math.min(50, Math.max(1, Number(pageSize) || 10));
  const offset = (page - 1) * size;
  try {
    const [rows] = await getWhPool().query(
      `SELECT ts, table_name, event_type, row_id, err_msg
       FROM _canal_errors
       WHERE ts > NOW() - INTERVAL ? HOUR
       ORDER BY ts DESC LIMIT ? OFFSET ?`,
      [hours, size, offset],
    );
    const [countRes] = await getWhPool().query(
      `SELECT COUNT(*) AS n FROM _canal_errors WHERE ts > NOW() - INTERVAL ? HOUR`,
      [hours],
    );
    const total = Number(countRes[0]?.n || 0);
    return {
      pageNo: page,
      pageSize: size,
      total,
      hasMore: offset + rows.length < total,
      list: rows,
    };
  } catch (err) {
    return { pageNo: page, pageSize: size, total: 0, hasMore: false, list: [], error: err.message };
  }
}

// GET nexa-cdc:6060/health
async function probeCdcHealth() {
  const url = `http://${OPS_CFG.cdcHost}:${OPS_CFG.cdcHttpPort}/health`;
  try {
    const ac = new AbortController();
    const timer = setTimeout(() => ac.abort(), 2500);
    const res = await fetch(url, { signal: ac.signal });
    clearTimeout(timer);
    return res.ok;
  } catch {
    return false;
  }
}

// GET nexa-cdc:6060/stats — 拿 CDC 内部状态（位点、总行、reconnect 数等）
async function fetchCdcStats() {
  const url = `http://${OPS_CFG.cdcHost}:${OPS_CFG.cdcHttpPort}/stats`;
  try {
    const ac = new AbortController();
    const timer = setTimeout(() => ac.abort(), 2500);
    const res = await fetch(url, { signal: ac.signal });
    clearTimeout(timer);
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function collectCanalStatus() {
  const [cdcUp, warehouseUp, cdcHeartbeat, cdcStats, cdbMaster, tables, errors] =
    await Promise.all([
      probeCdcHealth(),
      probeTcp(OPS_CFG.warehouse.host, OPS_CFG.warehouse.port),
      getConsumerLastHeartbeat(), // 从 warehouse._canal_stats 拿 last_ts
      fetchCdcStats(),
      getCdbMaster(),
      getTableRowCounts(),
      getRecentErrors(5),
    ]);

  const now = new Date();
  const idleSec = cdcHeartbeat
    ? Math.floor((now.getTime() - cdcHeartbeat.getTime()) / 1000)
    : null;

  return {
    generated_at: now.toISOString(),
    // 保持 canal_server / consumer 两个字段的 shape 兼容（APP 不用改就能看）
    // 但语义变了：现在两个字段本质是同一个进程（nexa-cdc）的两个探针视角
    canal_server: {
      up: cdcUp,
      host: `${OPS_CFG.cdcHost}:${OPS_CFG.cdcHttpPort}`,
      label: 'nexa-cdc (HTTP /health)',
    },
    consumer: {
      // heartbeat 90s 内视作活着（nexa-cdc 每 60s 写一次 __heartbeat__）
      up: !!cdcHeartbeat && idleSec !== null && idleSec < 90,
      last_heartbeat_at: cdcHeartbeat ? cdcHeartbeat.toISOString() : null,
      idle_seconds: idleSec,
      label: 'nexa-cdc (_canal_stats heartbeat)',
    },
    warehouse: {
      up: warehouseUp,
      host: `${OPS_CFG.warehouse.host}:${OPS_CFG.warehouse.port}/${OPS_CFG.warehouse.database}`,
    },
    cdc: {
      // nexa-cdc /stats 的富数据 —— APP 后续可以展示 total_rows, reconnects 等
      stats: cdcStats,
    },
    cdb_master: cdbMaster,
    tables,
    recent_errors: errors,
  };
}

const ADMIN_IDS = new Set(
  (process.env.AGENT_OPS_ADMIN_USER_IDS || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean),
);

export function isOpsAdmin(context) {
  const uid = context?.loginUser?.id ?? context?.loginUser?.userId;
  return uid != null && ADMIN_IDS.has(String(uid));
}

export function _resetForTest() {
  whPool = null;
  cdbConn = null;
}
