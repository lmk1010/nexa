// 驾驶舱聚合接口 — 直接读 warehouse ADS 表，不走 agent 推理
// 老板 KPI 需要毫秒级响应，走 agent+LLM 6 秒起步
// 这类"确定的、有 SQL 就能回答的"问题，就直接跳过 LLM
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
      connectionLimit: 3,
      waitForConnections: true,
      connectTimeout: 3000,
      dateStrings: true,
    });
  }
  return pool;
}

// 部门名字缓存（sys_dept 表，热重载）— 15 分钟 TTL
let deptNameCache = { at: 0, map: new Map() };
async function getDeptNameMap() {
  const now = Date.now();
  if (now - deptNameCache.at < 15 * 60_000 && deptNameCache.map.size > 0) {
    return deptNameCache.map;
  }
  const map = new Map();
  try {
    // ordersys 的 sys_dept 用 dept_id/dept_name 命名（不是 id/name）
    const [rows] = await getPool().query(
      'SELECT dept_id, dept_name FROM sys_dept WHERE deleted = 0',
    );
    for (const r of rows) map.set(Number(r.dept_id), r.dept_name);
  } catch {
    // 空 map 让调用方降级
  }
  deptNameCache = { at: now, map };
  return map;
}

// GET /dashboard/pf-monthly?range=today|7d|30d|365d
// 兼容旧的 month=yyyy-MM，走 normalizeRange 统一逻辑
export async function handlePfMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  // 本月合计
  const [totalRows] = await pool.query(
    `SELECT
       COALESCE(SUM(cnt), 0)             AS cnt,
       COALESCE(SUM(total_money), 0)     AS total_money,
       COALESCE(SUM(my_money), 0)        AS my_money,
       COALESCE(SUM(outlets_money), 0)   AS outlets_money,
       COALESCE(SUM(seller_money), 0)    AS seller_money,
       COALESCE(SUM(bond_money), 0)      AS bond_money,
       COALESCE(SUM(urgent_cnt), 0)      AS urgent_cnt
     FROM agg_sh_pf_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );
  const total = totalRows[0];

  // 部门排行（按 my_money 从大到小）
  const [deptRows] = await pool.query(
    `SELECT dept_id,
            SUM(cnt) AS cnt,
            SUM(total_money) AS total_money,
            SUM(my_money) AS my_money,
            SUM(outlets_money) AS outlets_money,
            SUM(seller_money) AS seller_money,
            SUM(bond_money) AS bond_money,
            SUM(urgent_cnt) AS urgent_cnt
     FROM agg_sh_pf_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY dept_id
     ORDER BY my_money DESC
     LIMIT 30`,
    [r.from, r.to],
  );

  const deptNames = await getDeptNameMap();
  const deptRanking = deptRows.map((row) => ({
    dept_id: Number(row.dept_id),
    dept_name: deptNames.get(Number(row.dept_id)) || `未知(${row.dept_id})`,
    cnt: Number(row.cnt) || 0,
    total_money: Number(row.total_money) || 0,
    my_money: Number(row.my_money) || 0,
    outlets_money: Number(row.outlets_money) || 0,
    seller_money: Number(row.seller_money) || 0,
    bond_money: Number(row.bond_money) || 0,
    urgent_cnt: Number(row.urgent_cnt) || 0,
  }));

  // 按日趋势
  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(cnt) AS cnt,
            SUM(my_money) AS my_money,
            SUM(outlets_money) AS outlets_money,
            SUM(seller_money) AS seller_money
     FROM agg_sh_pf_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd
     ORDER BY ymd ASC`,
    [r.from, r.to],
  );
  const trend = trendRows.map((row) => ({
    ymd: row.ymd,
    cnt: Number(row.cnt) || 0,
    my_money: Number(row.my_money) || 0,
    outlets_money: Number(row.outlets_money) || 0,
    seller_money: Number(row.seller_money) || 0,
  }));

  // agg 表刷新语义：event 每 5 分钟无条件跑一次，"数据已经是最新"就是 NOW()
  // 用 MAX(last_refresh) 会被 ON UPDATE CURRENT_TIMESTAMP 骗——当天没打款(dk_time)时行不变,
  // 时间戳一直停在上一次有 dk 的时刻,APP 看着像"没刷新"
  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  return {
    range: r.range,
    range_label: r.label,
    month: r.label, // 兼容旧 APP：把 label 塞给 month 字段
    total: {
      cnt: Number(total.cnt) || 0,
      total_money: Number(total.total_money) || 0,
      my_money: Number(total.my_money) || 0,
      outlets_money: Number(total.outlets_money) || 0,
      seller_money: Number(total.seller_money) || 0,
      bond_money: Number(total.bond_money) || 0,
      urgent_cnt: Number(total.urgent_cnt) || 0,
    },
    dept_ranking: deptRanking,
    trend,
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_sh_pf_daily',
  };
}

// ------------- Phase A: 营业额 / 工单 / 归因 3 个大盘 -------------
// 都用相同的模式：total + dept_ranking + trend + last_refresh
// range 参数：today / 7d / 30d / 365d，默认 30d（当月）
// 也兼容旧的 month=yyyy-MM 参数

function normalizeRange(url) {
  const range = url.searchParams.get('range') || '30d';
  const month = url.searchParams.get('month');
  const now = new Date();

  // 兼容旧的 month 参数
  if (month && /^\d{4}-\d{2}$/.test(month)) {
    const [y, m] = month.split('-').map(Number);
    const from = new Date(y, m - 1, 1);
    const to = new Date(y, m, 0);
    return { range: 'month', label: month, from: fmtDate(from), to: fmtDate(to) };
  }

  const to = fmtDate(now);
  let from;
  let label;
  switch (range) {
    case 'today':
      from = to;
      label = '今日';
      break;
    case '7d':
      from = fmtDate(new Date(now.getTime() - 6 * 86400_000));
      label = '近 7 天';
      break;
    case '30d':
      from = fmtDate(new Date(now.getTime() - 29 * 86400_000));
      label = '近 30 天';
      break;
    case '90d':
      from = fmtDate(new Date(now.getTime() - 89 * 86400_000));
      label = '近 90 天';
      break;
    case '365d':
      from = fmtDate(new Date(now.getTime() - 364 * 86400_000));
      label = '近 1 年';
      break;
    case '730d':
      from = fmtDate(new Date(now.getTime() - 729 * 86400_000));
      label = '近 2 年';
      break;
    case '1095d':
      from = fmtDate(new Date(now.getTime() - 1094 * 86400_000));
      label = '近 3 年';
      break;
    case 'all':
      from = '2020-01-01'; // agg 表数据不早于此
      label = '全部';
      break;
    default:
      throw new Error(`range 不支持: ${range}`);
  }
  return { range, label, from, to };
}

function fmtDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// GET /dashboard/order-monthly?range=today|7d|30d|365d — 营业额大盘
// 走 agg_order_daily（宽表已包含 GMV/pending/active/revoked 桶），毫秒级返回
// order_cnt = 非撤销订单数 (总量 - revoked)
// total_money = GMV (非撤销订单总额，符合老板视角"营业额")
export async function handleOrderMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  const [totalRows] = await pool.query(
    `SELECT COALESCE(SUM(order_cnt),0)-COALESCE(SUM(revoked_cnt),0) AS order_cnt,
            COALESCE(SUM(paid_cnt),0) AS paid_cnt,
            COALESCE(SUM(gmv_money),0) AS total_money,
            COALESCE(SUM(gmv_install_money),0) AS install_money
     FROM agg_order_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );

  const [deptRows] = await pool.query(
    `SELECT dept_id,
            SUM(order_cnt)-SUM(revoked_cnt) AS order_cnt,
            SUM(paid_cnt) AS paid_cnt,
            SUM(gmv_money) AS total_money,
            SUM(gmv_install_money) AS install_money
     FROM agg_order_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY dept_id ORDER BY total_money DESC LIMIT 30`,
    [r.from, r.to],
  );

  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(order_cnt)-SUM(revoked_cnt) AS order_cnt,
            SUM(paid_cnt) AS paid_cnt,
            SUM(gmv_money) AS total_money
     FROM agg_order_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd ORDER BY ymd ASC`,
    [r.from, r.to],
  );

  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  const deptNames = await getDeptNameMap();
  const t = totalRows[0];
  return {
    range: r.range,
    range_label: r.label,
    month: r.label,
    total: {
      order_cnt: Number(t.order_cnt) || 0,
      paid_cnt: Number(t.paid_cnt) || 0,
      total_money: Number(t.total_money) || 0,
      install_money: Number(t.install_money) || 0,
    },
    dept_ranking: deptRows.map((row) => ({
      dept_id: Number(row.dept_id),
      dept_name: deptNames.get(Number(row.dept_id)) || `未知(${row.dept_id})`,
      order_cnt: Number(row.order_cnt) || 0,
      paid_cnt: Number(row.paid_cnt) || 0,
      total_money: Number(row.total_money) || 0,
    })),
    trend: trendRows.map((row) => ({
      ymd: row.ymd,
      order_cnt: Number(row.order_cnt) || 0,
      paid_cnt: Number(row.paid_cnt) || 0,
      total_money: Number(row.total_money) || 0,
    })),
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_order_daily',
  };
}

// GET /dashboard/work-monthly?range=... — 工单产能大盘
export async function handleWorkMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  const [totalRows] = await pool.query(
    `SELECT COALESCE(SUM(work_cnt),0) AS work_cnt,
            COALESCE(SUM(finished_cnt),0) AS finished_cnt,
            COALESCE(AVG(avg_dispose_ms),0) AS avg_dispose_ms,
            COALESCE(SUM(hint_cnt),0) AS hint_cnt
     FROM agg_work_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );

  const [deptRows] = await pool.query(
    `SELECT dept_id,
            SUM(work_cnt) AS work_cnt,
            SUM(finished_cnt) AS finished_cnt,
            AVG(avg_dispose_ms) AS avg_dispose_ms
     FROM agg_work_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY dept_id ORDER BY work_cnt DESC LIMIT 30`,
    [r.from, r.to],
  );

  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(work_cnt) AS work_cnt,
            SUM(finished_cnt) AS finished_cnt
     FROM agg_work_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd ORDER BY ymd ASC`,
    [r.from, r.to],
  );

  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  const deptNames = await getDeptNameMap();
  return {
    range: r.range,
    range_label: r.label,
    month: r.label,
    total: {
      work_cnt: Number(totalRows[0].work_cnt) || 0,
      finished_cnt: Number(totalRows[0].finished_cnt) || 0,
      avg_dispose_ms: Number(totalRows[0].avg_dispose_ms) || 0,
      hint_cnt: Number(totalRows[0].hint_cnt) || 0,
    },
    dept_ranking: deptRows.map((row) => ({
      dept_id: Number(row.dept_id),
      dept_name: deptNames.get(Number(row.dept_id)) || `未知(${row.dept_id})`,
      work_cnt: Number(row.work_cnt) || 0,
      finished_cnt: Number(row.finished_cnt) || 0,
      avg_dispose_ms: Number(row.avg_dispose_ms) || 0,
    })),
    trend: trendRows.map((row) => ({
      ymd: row.ymd,
      work_cnt: Number(row.work_cnt) || 0,
      finished_cnt: Number(row.finished_cnt) || 0,
    })),
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_work_daily',
  };
}

// GET /dashboard/attribution-monthly?range=... — 差评/补发/超时归因大盘
export async function handleAttributionMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  const [totalRows] = await pool.query(
    `SELECT COALESCE(SUM(bad_cnt),0) AS bad_cnt,
            COALESCE(SUM(good_cnt),0) AS good_cnt,
            COALESCE(SUM(reissue_cnt),0) AS reissue_cnt,
            COALESCE(SUM(timeout_cnt),0) AS timeout_cnt
     FROM agg_bad_reissue_timeout_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );

  const [deptRows] = await pool.query(
    `SELECT dept_id,
            SUM(bad_cnt) AS bad_cnt,
            SUM(good_cnt) AS good_cnt,
            SUM(reissue_cnt) AS reissue_cnt,
            SUM(timeout_cnt) AS timeout_cnt
     FROM agg_bad_reissue_timeout_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY dept_id ORDER BY bad_cnt DESC LIMIT 30`,
    [r.from, r.to],
  );

  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(bad_cnt) AS bad_cnt,
            SUM(reissue_cnt) AS reissue_cnt,
            SUM(timeout_cnt) AS timeout_cnt
     FROM agg_bad_reissue_timeout_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd ORDER BY ymd ASC`,
    [r.from, r.to],
  );

  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  const deptNames = await getDeptNameMap();
  return {
    range: r.range,
    range_label: r.label,
    month: r.label,
    total: {
      bad_cnt: Number(totalRows[0].bad_cnt) || 0,
      good_cnt: Number(totalRows[0].good_cnt) || 0,
      reissue_cnt: Number(totalRows[0].reissue_cnt) || 0,
      timeout_cnt: Number(totalRows[0].timeout_cnt) || 0,
    },
    dept_ranking: deptRows.map((row) => ({
      dept_id: Number(row.dept_id),
      dept_name: deptNames.get(Number(row.dept_id)) || `未知(${row.dept_id})`,
      bad_cnt: Number(row.bad_cnt) || 0,
      good_cnt: Number(row.good_cnt) || 0,
      reissue_cnt: Number(row.reissue_cnt) || 0,
      timeout_cnt: Number(row.timeout_cnt) || 0,
    })),
    trend: trendRows.map((row) => ({
      ymd: row.ymd,
      bad_cnt: Number(row.bad_cnt) || 0,
      reissue_cnt: Number(row.reissue_cnt) || 0,
      timeout_cnt: Number(row.timeout_cnt) || 0,
    })),
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_bad_reissue_timeout_daily',
  };
}

// ============================================================
// Phase B: /dashboard/sla-monthly —— 派单/回访 时效达标率
// 主 KPI = 达标率（sla_ok / ops）；副：ops_cnt、avg_diff、max_diff
// 部门排行按 ops_cnt 降序；类型饼 = 派单 vs 回访
// ============================================================
export async function handleSlaMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  const [totalRows] = await pool.query(
    `SELECT COALESCE(SUM(ops_cnt),0) AS ops_cnt,
            COALESCE(SUM(sla_ok_cnt),0) AS sla_ok_cnt,
            COALESCE(SUM(CASE WHEN type=1 THEN ops_cnt END),0) AS dispatch_cnt,
            COALESCE(SUM(CASE WHEN type=2 THEN ops_cnt END),0) AS callback_cnt,
            COALESCE(FLOOR(SUM(avg_diff_sec * ops_cnt) / NULLIF(SUM(ops_cnt),0)),0) AS avg_diff_sec,
            COALESCE(MAX(max_diff_sec),0) AS max_diff_sec
     FROM agg_time_sla_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );

  const [deptRows] = await pool.query(
    `SELECT dept_id,
            SUM(ops_cnt) AS ops_cnt,
            SUM(sla_ok_cnt) AS sla_ok_cnt,
            SUM(CASE WHEN type=1 THEN ops_cnt END) AS dispatch_cnt,
            SUM(CASE WHEN type=2 THEN ops_cnt END) AS callback_cnt
     FROM agg_time_sla_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY dept_id ORDER BY ops_cnt DESC LIMIT 30`,
    [r.from, r.to],
  );

  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(ops_cnt) AS ops_cnt,
            SUM(sla_ok_cnt) AS sla_ok_cnt,
            FLOOR(SUM(avg_diff_sec * ops_cnt) / NULLIF(SUM(ops_cnt),0)) AS avg_diff_sec
     FROM agg_time_sla_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd ORDER BY ymd ASC`,
    [r.from, r.to],
  );

  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  const deptNames = await getDeptNameMap();
  const t = totalRows[0];
  const opsCnt = Number(t.ops_cnt) || 0;
  const okCnt = Number(t.sla_ok_cnt) || 0;
  const rate = opsCnt > 0 ? Math.round((okCnt / opsCnt) * 1000) / 10 : 0; // 一位小数
  return {
    range: r.range,
    range_label: r.label,
    month: r.label,
    total: {
      ops_cnt: opsCnt,
      sla_ok_cnt: okCnt,
      sla_rate: rate, // 0-100
      dispatch_cnt: Number(t.dispatch_cnt) || 0,
      callback_cnt: Number(t.callback_cnt) || 0,
      avg_diff_sec: Number(t.avg_diff_sec) || 0,
      max_diff_sec: Number(t.max_diff_sec) || 0,
    },
    dept_ranking: deptRows.map((row) => ({
      dept_id: Number(row.dept_id),
      dept_name: deptNames.get(Number(row.dept_id)) || `未知(${row.dept_id})`,
      ops_cnt: Number(row.ops_cnt) || 0,
      sla_ok_cnt: Number(row.sla_ok_cnt) || 0,
      dispatch_cnt: Number(row.dispatch_cnt) || 0,
      callback_cnt: Number(row.callback_cnt) || 0,
    })),
    trend: trendRows.map((row) => ({
      ymd: row.ymd,
      ops_cnt: Number(row.ops_cnt) || 0,
      sla_ok_cnt: Number(row.sla_ok_cnt) || 0,
      avg_diff_sec: Number(row.avg_diff_sec) || 0,
    })),
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_time_sla_daily',
  };
}

// ============================================================
// Phase B: /dashboard/reissue-monthly —— 补发概览
// 主 KPI = 补发笔数；副：产品/施工分布、通过率
// 排行按 shop_id —— agg_reissue_daily 表设计漏了 seller/dept 维度，
// outlets_shop 表也没有 dept_id 字段（sql 里之前 JOIN o.dept_id 直接 1054 报错 → 500）。
// 只能按门店排行，dept 维度等 agg 表加字段后再补。
// ============================================================
export async function handleReissueMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  const [totalRows] = await pool.query(
    `SELECT COALESCE(SUM(cnt),0) AS total_cnt,
            COALESCE(SUM(CASE WHEN type=1 THEN cnt END),0) AS product_cnt,
            COALESCE(SUM(CASE WHEN type=2 THEN cnt END),0) AS install_cnt,
            COALESCE(SUM(CASE WHEN state='1' THEN cnt END),0) AS approved_cnt,
            COALESCE(SUM(CASE WHEN state='0' THEN cnt END),0) AS pending_cnt,
            COALESCE(SUM(CASE WHEN state='-1' THEN cnt END),0) AS rejected_cnt
     FROM agg_reissue_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );

  // 按门店（shop_id）聚合排行 —— 关联 outlets_shop 只取 name
  const [shopRows] = await pool.query(
    `SELECT r.shop_id,
            o.name AS shop_name,
            SUM(r.cnt) AS total_cnt,
            SUM(CASE WHEN r.type=1 THEN r.cnt END) AS product_cnt,
            SUM(CASE WHEN r.type=2 THEN r.cnt END) AS install_cnt
     FROM agg_reissue_daily r
     LEFT JOIN outlets_shop o ON o.id = r.shop_id
     WHERE r.ymd BETWEEN ? AND ?
     GROUP BY r.shop_id, o.name
     ORDER BY total_cnt DESC LIMIT 30`,
    [r.from, r.to],
  );

  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(cnt) AS total_cnt,
            SUM(CASE WHEN state='1' THEN cnt END) AS approved_cnt
     FROM agg_reissue_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd ORDER BY ymd ASC`,
    [r.from, r.to],
  );

  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  const t = totalRows[0];
  return {
    range: r.range,
    range_label: r.label,
    month: r.label,
    total: {
      total_cnt: Number(t.total_cnt) || 0,
      product_cnt: Number(t.product_cnt) || 0,
      install_cnt: Number(t.install_cnt) || 0,
      approved_cnt: Number(t.approved_cnt) || 0,
      pending_cnt: Number(t.pending_cnt) || 0,
      rejected_cnt: Number(t.rejected_cnt) || 0,
    },
    // 前端 key 保持 dept_ranking / dept_name 不变 —— 门店占位到"部门"槽位，
    // 展示层直接把 rankLabel 改成"门店排行"，不用再改协议
    dept_ranking: shopRows.map((row) => ({
      dept_id: Number(row.shop_id) || 0,
      dept_name: row.shop_name || `门店(${row.shop_id})`,
      total_cnt: Number(row.total_cnt) || 0,
      product_cnt: Number(row.product_cnt) || 0,
      install_cnt: Number(row.install_cnt) || 0,
    })),
    trend: trendRows.map((row) => ({
      ymd: row.ymd,
      total_cnt: Number(row.total_cnt) || 0,
      approved_cnt: Number(row.approved_cnt) || 0,
    })),
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_reissue_daily',
  };
}

// ============================================================
// /dashboard/order-count-monthly —— 订单数量 / 状态分布
// 走 agg_order_daily 宽表（state 桶已聚合），毫秒级返回
// order_cnt 是含撤单在内的下单总数；revoke_rate = revoked/order_cnt
// ============================================================
export async function handleOrderCountMonthly(url) {
  const r = normalizeRange(url);
  const pool = getPool();

  const [totalRows] = await pool.query(
    `SELECT COALESCE(SUM(order_cnt),0)      AS order_cnt,
            COALESCE(SUM(pending_cnt),0)    AS pending_cnt,
            COALESCE(SUM(active_cnt),0)     AS active_cnt,
            COALESCE(SUM(paid_cnt),0)       AS finished_cnt,
            COALESCE(SUM(revoked_cnt),0)    AS revoked_cnt
     FROM agg_order_daily WHERE ymd BETWEEN ? AND ?`,
    [r.from, r.to],
  );

  const [deptRows] = await pool.query(
    `SELECT dept_id,
            SUM(order_cnt)   AS order_cnt,
            SUM(pending_cnt) AS pending_cnt,
            SUM(active_cnt)  AS active_cnt,
            SUM(paid_cnt)    AS finished_cnt,
            SUM(revoked_cnt) AS revoked_cnt
     FROM agg_order_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY dept_id ORDER BY order_cnt DESC LIMIT 30`,
    [r.from, r.to],
  );

  const [trendRows] = await pool.query(
    `SELECT ymd,
            SUM(order_cnt)   AS order_cnt,
            SUM(paid_cnt)    AS finished_cnt,
            SUM(revoked_cnt) AS revoked_cnt
     FROM agg_order_daily WHERE ymd BETWEEN ? AND ?
     GROUP BY ymd ORDER BY ymd ASC`,
    [r.from, r.to],
  );

  const [refreshRows] = await pool.query(`SELECT NOW() AS last_refresh`);

  const deptNames = await getDeptNameMap();
  const t = totalRows[0];
  const orderCnt = Number(t.order_cnt) || 0;
  const revokedCnt = Number(t.revoked_cnt) || 0;
  const revokeRate =
    orderCnt > 0 ? Math.round((revokedCnt / orderCnt) * 1000) / 10 : 0;

  return {
    range: r.range,
    range_label: r.label,
    month: r.label,
    total: {
      order_cnt: orderCnt,
      pending_cnt: Number(t.pending_cnt) || 0,
      active_cnt: Number(t.active_cnt) || 0,
      finished_cnt: Number(t.finished_cnt) || 0,
      revoked_cnt: revokedCnt,
      revoke_rate: revokeRate,
    },
    dept_ranking: deptRows.map((row) => ({
      dept_id: Number(row.dept_id) || 0,
      dept_name: deptNames.get(Number(row.dept_id)) || `未知(${row.dept_id})`,
      order_cnt: Number(row.order_cnt) || 0,
      pending_cnt: Number(row.pending_cnt) || 0,
      active_cnt: Number(row.active_cnt) || 0,
      finished_cnt: Number(row.finished_cnt) || 0,
      revoked_cnt: Number(row.revoked_cnt) || 0,
    })),
    trend: trendRows.map((row) => ({
      ymd: row.ymd,
      order_cnt: Number(row.order_cnt) || 0,
      finished_cnt: Number(row.finished_cnt) || 0,
      revoked_cnt: Number(row.revoked_cnt) || 0,
    })),
    last_refresh: refreshRows[0]?.last_refresh || null,
    source: 'warehouse.agg_order_daily',
  };
}

export function _resetForTest() {
  pool = null;
  deptNameCache = { at: 0, map: new Map() };
}
