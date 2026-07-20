// 运维监控：实时速率 + 压力指标
//
// 数据源都是 nexa-cdc 自己吐的：
//   - GET /metrics —— Prometheus 累积计数器（ins_total / upd_total / del_total / err_total per table）
//   - GET /stats   —— 进程内 snapshot（binlog 位点、reconnects、total_batches 等）
//
// 服务端保留上次 poll 的快照，本次减去得 rate。这样 APP 只管 3s 一次 poll，
// 后端把"per-table TPS + 每秒批数 + binlog 延迟"全算好塞回去，前端零逻辑。

import net from 'node:net';
import mysql from 'mysql2/promise';

const CFG = {
  cdcHost: process.env.OPS_CDC_HOST || 'nexa-cdc',
  cdcHttpPort: Number.parseInt(process.env.OPS_CDC_HTTP_PORT || '6060', 10),
  cdb: {
    host: process.env.OPS_CDB_HOST || 'nj-cdb-40g509zj.sql.tencentcdb.com',
    port: Number.parseInt(process.env.OPS_CDB_PORT || '29355', 10),
    user: process.env.OPS_CDB_USER || 'canal',
    password: process.env.OPS_CDB_PASS || 'Kyx123456++',
  },
};

// 上次 poll 的快照 —— 单实例进程内够用；重启后第一次 poll 会退化成 "rate=0"，是可接受的。
let lastSnapshot = null; // { at: DateTime, byTable: {name: {ins,upd,del,err}}, totals: {ins,upd,del,err,batches,reconnects}, binlogPos, binlogFile }

// 一次 fetch nexa-cdc /metrics 全部 counter，返回 { byTable, totals, binlogPos }
async function fetchMetrics() {
  const url = `http://${CFG.cdcHost}:${CFG.cdcHttpPort}/metrics`;
  const ac = new AbortController();
  const timer = setTimeout(() => ac.abort(), 2500);
  let text;
  try {
    const res = await fetch(url, { signal: ac.signal });
    if (!res.ok) return null;
    text = await res.text();
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
  return parseMetrics(text);
}

// 解析 Prometheus text format。
// 只关心 nexa_cdc_* 系列 —— 忽略 # HELP / # TYPE 注释和其他 metric。
export function parseMetrics(text) {
  const byTable = {};   // { tableName: { ins, upd, del, err } }
  const totals = { ins: 0, upd: 0, del: 0, err: 0, batches: 0, reconnects: 0 };
  let binlogPos = 0;
  let connected = false;

  for (const raw of text.split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    if (!line.startsWith('nexa_cdc_')) continue;
    // 格式1: nexa_cdc_ins_total{table="t_order"} 96
    // 格式2: nexa_cdc_binlog_position 1.54321648e+08
    // 格式3: nexa_cdc_last_batch_rows 1
    const labelStart = line.indexOf('{');
    const spaceIdx = line.lastIndexOf(' ');
    if (spaceIdx < 0) continue;
    const valStr = line.slice(spaceIdx + 1);
    const val = Number.parseFloat(valStr);
    if (!Number.isFinite(val)) continue;

    const metricName = labelStart >= 0
      ? line.slice(0, labelStart)
      : line.slice(0, spaceIdx);
    let table = null;
    if (labelStart >= 0) {
      // 只解析 table 标签
      const labelEnd = line.indexOf('}', labelStart);
      if (labelEnd > labelStart) {
        const labelBlob = line.slice(labelStart + 1, labelEnd);
        const m = /table="([^"]+)"/.exec(labelBlob);
        if (m) table = m[1];
      }
    }

    const op = tableOpFromMetric(metricName);
    if (op && table) {
      if (!byTable[table]) byTable[table] = { ins: 0, upd: 0, del: 0, err: 0 };
      byTable[table][op] = val;
      totals[op] += val;
      continue;
    }
    if (metricName === 'nexa_cdc_binlog_position') binlogPos = val;
    else if (metricName === 'nexa_cdc_connected') connected = val > 0;
    else if (metricName === 'nexa_cdc_reconnects_total') totals.reconnects = val;
    // nexa_cdc_last_batch_rows 是 gauge，不 diff，忽略
  }
  return { byTable, totals, binlogPos, connected };
}

function tableOpFromMetric(name) {
  switch (name) {
    case 'nexa_cdc_ins_total': return 'ins';
    case 'nexa_cdc_upd_total': return 'upd';
    case 'nexa_cdc_del_total': return 'del';
    case 'nexa_cdc_err_total': return 'err';
    default: return null;
  }
}

// 从 nexa-cdc /stats 拿 { binlog_file, binlog_pos, total_batches, reconnects, connected, tables }
async function fetchStats() {
  const url = `http://${CFG.cdcHost}:${CFG.cdcHttpPort}/stats`;
  const ac = new AbortController();
  const timer = setTimeout(() => ac.abort(), 2500);
  try {
    const res = await fetch(url, { signal: ac.signal });
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

// 从 CDB SHOW MASTER STATUS 拿源库当前 binlog tail，用于算延迟
let cdbConn = null;
async function getCdbTail() {
  try {
    if (!cdbConn) {
      cdbConn = await mysql.createConnection({ ...CFG.cdb, connectTimeout: 5000 });
    }
    const [rows] = await cdbConn.query('SHOW MASTER STATUS');
    if (!rows[0]) return null;
    return { file: rows[0].File, pos: Number(rows[0].Position) };
  } catch {
    cdbConn = null;
    return null;
  }
}

// 主入口：给 /ops/canal-rates 用
export async function collectCanalRates() {
  const [metrics, stats, cdbTail] = await Promise.all([
    fetchMetrics(),
    fetchStats(),
    getCdbTail(),
  ]);
  const now = Date.now();

  if (!metrics) {
    return {
      generated_at: new Date(now).toISOString(),
      available: false,
      reason: 'nexa-cdc /metrics unreachable',
    };
  }

  // 计算 rate（本次 vs 上次快照）
  let elapsedSec = null;
  let ratesByTable = {};
  let aggregateRates = { ins: 0, upd: 0, del: 0, err: 0, total: 0, batches: 0 };

  if (lastSnapshot) {
    elapsedSec = (now - lastSnapshot.at) / 1000;
    if (elapsedSec >= 1 && elapsedSec <= 300) {
      // 只在合理窗口里算 rate（>300s 说明太久没 poll，diff 没意义）
      for (const [t, cur] of Object.entries(metrics.byTable)) {
        const prev = lastSnapshot.byTable[t] || { ins: 0, upd: 0, del: 0, err: 0 };
        const dIns = Math.max(0, cur.ins - prev.ins);
        const dUpd = Math.max(0, cur.upd - prev.upd);
        const dDel = Math.max(0, cur.del - prev.del);
        const dErr = Math.max(0, cur.err - prev.err);
        const dTotal = dIns + dUpd + dDel + dErr;
        if (dTotal > 0 || cur.ins + cur.upd + cur.del + cur.err > 0) {
          ratesByTable[t] = {
            ins: round1(dIns / elapsedSec),
            upd: round1(dUpd / elapsedSec),
            del: round1(dDel / elapsedSec),
            err: round1(dErr / elapsedSec),
            total: round1(dTotal / elapsedSec),
            // 累计值一并返 —— 前端可展示"进程启动以来"总量
            cum_ins: cur.ins,
            cum_upd: cur.upd,
            cum_del: cur.del,
            cum_err: cur.err,
          };
        }
      }
      aggregateRates = {
        ins: round1((metrics.totals.ins - lastSnapshot.totals.ins) / elapsedSec),
        upd: round1((metrics.totals.upd - lastSnapshot.totals.upd) / elapsedSec),
        del: round1((metrics.totals.del - lastSnapshot.totals.del) / elapsedSec),
        err: round1((metrics.totals.err - lastSnapshot.totals.err) / elapsedSec),
        total: 0,
        batches: stats && lastSnapshot.batches != null
          ? round1(((stats.total_batches || 0) - lastSnapshot.batches) / elapsedSec)
          : 0,
      };
      aggregateRates.total = round1(
        aggregateRates.ins + aggregateRates.upd + aggregateRates.del + aggregateRates.err
      );
    }
  }

  // 更新快照
  lastSnapshot = {
    at: now,
    byTable: metrics.byTable,
    totals: metrics.totals,
    batches: stats ? stats.total_batches || 0 : null,
  };

  // 压力指标
  const cdcPos = metrics.binlogPos || 0;
  const cdcFile = stats?.binlog_file || null;
  let lagBytes = null;
  let lagWarn = null;
  if (cdbTail && cdcFile) {
    if (cdbTail.file === cdcFile) {
      lagBytes = Math.max(0, cdbTail.pos - cdcPos);
    } else {
      // 跨 binlog 文件，无法精确算 bytes；标一下方便前端提示
      lagWarn = `source at ${cdbTail.file}, cdc at ${cdcFile} (cross-file, byte lag N/A)`;
    }
  }

  // 累计吞吐（cumulative） —— 进程启动以来
  const cum = {
    ins: metrics.totals.ins,
    upd: metrics.totals.upd,
    del: metrics.totals.del,
    err: metrics.totals.err,
    total: metrics.totals.ins + metrics.totals.upd + metrics.totals.del + metrics.totals.err,
    batches: stats?.total_batches || 0,
    reconnects: metrics.totals.reconnects || stats?.reconnects || 0,
  };

  // 平均 batch size = total_rows / total_batches （仅供参考）
  const avgBatchSize = cum.batches > 0
    ? Math.round((cum.total / cum.batches) * 100) / 100
    : null;

  // 错误率（累计维度）
  const errorRatePct = cum.total > 0
    ? Math.round((cum.err / cum.total) * 10000) / 100
    : 0;

  return {
    generated_at: new Date(now).toISOString(),
    available: true,
    elapsed_sec: elapsedSec, // 距上次 poll 的时间；首次或过久返 null
    connected: metrics.connected,
    aggregate_rates: aggregateRates,
    rates_by_table: ratesByTable,
    cumulative: cum,
    pressure: {
      binlog_pos: cdcPos,
      binlog_file: cdcFile,
      source_binlog_pos: cdbTail?.pos ?? null,
      source_binlog_file: cdbTail?.file ?? null,
      lag_bytes: lagBytes,
      lag_warn: lagWarn,
      lag_human: lagBytes != null ? humanizeBytes(lagBytes) : null,
      avg_batch_size: avgBatchSize,
      error_rate_pct: errorRatePct,
      reconnects_total: cum.reconnects,
      last_batch_at: stats?.last_batch_at || null,
      started_at: stats?.started_at || null,
    },
  };
}

function round1(n) {
  return Math.round(n * 10) / 10;
}

function humanizeBytes(b) {
  if (b == null || b < 0) return null;
  if (b < 1024) return `${b}B`;
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)}KB`;
  if (b < 1024 * 1024 * 1024) return `${(b / 1024 / 1024).toFixed(1)}MB`;
  return `${(b / 1024 / 1024 / 1024).toFixed(2)}GB`;
}

export function _resetForTest() {
  lastSnapshot = null;
  cdbConn = null;
}
