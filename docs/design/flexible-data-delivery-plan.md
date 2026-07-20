# 业务部门灵活数据获取方案

**背景**：连图 ordersys 分表族每张 80w+ 行，6-9 张 join 一次查询 40-60 秒；老板/业务部门经常要"灵活"取数（不同维度/时间范围/明细/导出），现有 API 方案覆盖不了，也扛不住。

## 现状痛点

| 场景 | 现方式 | 卡在哪 |
|---|---|---|
| 老板问 "本月赔付按部门排" | Agent 调 `sh/pf` 接口 | 6 表 join 慢查 40s+ |
| 业务问 "过去 3 月所有订单" | 直接 SELECT | 分表跨年 union 慢 + 数据量大 |
| 财务导出 "上个月赔付明细" | 手工从后台下载 | 每次要 IT 帮忙 |
| 部门经理 "我组员的工单情况" | 找 IT 手动 SQL | IT 变瓶颈 |
| 新需求"按客户维度分析" | 现有 API 没有 | 要新写接口，2 周 |

**核心矛盾**：ordersys 原库设计给"业务系统"用（点查/写入），不适合"分析查询"。

## 三层架构落地

```
┌──────────────────────────────────────────────────────┐
│  App/驾驶舱/agent                                    │
│  ↓ HTTP                                              │
│  agent 网关（超时/权限/审计）                        │
└──┬──────────────────────┬─────────────────────────┬──┘
   │ 90% 老板 KPI         │ 8% 灵活探索/导出       │ 2% 详情点查
   ↓                      ↓                        ↓
┌────────────────┐  ┌────────────────┐  ┌──────────────────┐
│ ADS 预聚合表    │  │ ODS 明细数据仓  │  │ 连图 CDB 原库    │
│ agg_sh_pf_daily│  │ (canal 实时流入)│  │ (仅点查、免加载)  │
│ agg_sh_order_.. │  │ 8 张核心表      │  │                  │
│ agg_sh_work_.. │  │ 5000 万行合计   │  │                  │
│ 100 KB × N     │  │ ~10 GB         │  │                  │
│ <10ms          │  │ 100ms-1s       │  │ 20-40s (要绕开)  │
└────────────────┘  └────────────────┘  └──────────────────┘
```

### 🥇 L3-ADS（应用数据层） — 90% 场景走这里

**做法**：每类 KPI 一张预聚合表 + 5 分钟定时 upsert

**已完成**：`agg_sh_pf_daily`（部门 × 日 × 赔付金额/单数）

**待做（3~5 天工作量）**：
- `agg_sh_order_daily` — 部门 × 日 × 营业额/单数/毛利
- `agg_sh_work_daily` — 部门 × 日 × 工单量/完结率/超时率
- `agg_sh_rate_daily` — 部门 × 日 × 好评/差评/解决率
- `agg_sh_bf_daily` — 部门 × 日 × 补发率

**每张表结构**：
```sql
CREATE TABLE agg_sh_xxx_daily (
  dept_id BIGINT,
  ymd DATE,
  month CHAR(7),
  cnt INT, ...金额/率...
  last_refresh DATETIME,
  PRIMARY KEY (dept_id, ymd)
);
```

**存储成本**：每张表 300 部门 × 400 天 = 12万行 × 200B ≈ 25 MB。**5 张一共 <150 MB**。

**查询成本**：
```sql
SELECT SUM(my_money) FROM agg_sh_pf_daily WHERE month = '2026-07';
```
命中 `idx_month` 索引 → 扫 300 行 → **<5 ms**

**关键收益**：
- 老板 90% 问题秒回
- 不再打扰源库 ordersys CDB
- 老板"任意时间范围"都是 O(300 × 天数)，天数 30/60/90 都毫秒级

### 🥈 L2-ODS（数据仓明细层） — 8% 灵活探索

**做法**：canal 实时把 ordersys 8-10 张核心表流到 warehouse.ordersys_dw，走 InnoDB partition by year，加对应索引。

**已完成**：canal 三件套 + `t_task/t_order_pf/t_order_pf_seller/t_order_revoke/sys_user/sys_dept`

**待补齐**：`t_order/t_order_detail/t_work_order/t_work_order_timeout`（按 [清单文档](../design/ordersys-oa-dashboard-data-plan.md) 分批 grant）

**查询能力**：
- Agent 可以直接 SQL warehouse 做灵活分析（例如"按门店维度聚合，我没预算表"），10ms - 1s
- 业务部门也可以拿到只读账号做 Metabase / Grafana / Excel 直连
- 导出场景：SELECT + `export_excel` 工具 → minio 下载链接

**关键收益**：
- 老板临时问"我要按平台/城市/项目 X 维度看" → agent 直接 SQL warehouse
- 完全不打扰 ordersys 生产

### 🥉 L1-源库（点查兜底） — 2% 场景

只用于：
- 用户主动"打开某个订单详情"
- 老数据（warehouse 只留最近 3 年，更老年份直接查源）

**规则**：agent 网关强制 ordersys 直查 **每次 ≤50 行 / 1 秒超时**，超过就拒。

## 业务部门自助能力（关键升级）

### 阶段 1（当下）— agent 对话式取数
- 财务："导出上月我部门赔付明细" → agent 走 warehouse.t_order_pf → `export_excel` → 下载链接
- 老板："按部门排本月工单完结率" → agent 走 `agg_sh_work_daily` → 表格 + 图表

### 阶段 2（1-2 周）— 数据服务 API
- 抽出常用查询做成参数化 API：`/data/{topic}?dept=X&month=Y`
- Agent 内部走这些 API，业务 App 也能直接调
- 例：`/data/pf?dept=105&month=2026-07` 返回赔付概览 JSON

### 阶段 3（1 个月）— Metabase/Superset 直连 warehouse
- warehouse 建 `bi_ro` 只读账号
- Metabase 部署到 kyx-network（内网），部门经理自己拖表出报表
- 不用 IT 帮忙，业务自助

## 什么时候引入 ClickHouse / StarRocks

**不需要**。你数据量在千万 - 亿量级，MySQL 8 + 分区表 + 预聚合完全扛得住，5 年后再看。

引入分析型 DB 的临界点：
- 单表 5 亿行以上
- 多维分析并发用户 20+
- 秒级响应 P99 要求

你目前场景（几十人用 + 300 部门维度）**距离这个临界点还很远**。

## 时间线

| 周 | 交付 |
|---|---|
| **W1（已完成）** | canal + t_order_pf 全流 + agg_sh_pf_daily + agent /dashboard/pf-monthly + APP 展示 |
| **W2** | 扩 canal 到 t_order/t_order_detail/t_work_order，加对应 3 张 agg 表 |
| **W3** | Agent export_excel + minio 打通（已做 backend，本次一起装 APK） |
| **W4** | 数据服务 API `/data/*` 系列，业务 App 也能调 |
| **W5-6** | Metabase 部署 + `bi_ro` 只读账号 + 部门经理培训 |

## 关键约束（生产必须遵守）

- **canal 只读源库 binlog**，不 SELECT 业务表（只在 backfill 时一次性拿数据）
- **warehouse 只读 agent**，Metabase / BI 走 `bi_ro` 账号，杜绝写权限
- **agent 网关**每次调用都在 `kyx_agent_api_call_audit` 落日志，慢查询自动进热错误缓存
- **超过 30 天时间跨度**的查询，agent 主动跟用户确认再执行（本轮 system prompt 已加）

## FAQ

**Q：canal 挂了怎么办？**
A：Consumer 有 rollback + 幂等 upsert；短暂断连 30 分钟内会自动追赶。真挂了 4 小时以上就走 mysqldump 全量重灌（我们已经跑通过一次）。

**Q：warehouse 磁盘不够怎么办？**
A：现在 68G 富余。每张表加 `PARTITION BY RANGE (YEAR(create_time))`，超过 3 年的分区可以 DROP 归档到 minio。

**Q：老板问"实时"数据怎么办？**
A：canal 延迟 <5 秒，ADS 聚合表每 5 分钟刷一次。老板"实时"要求 = "5 分钟内"，足够。真需要"当前秒级"（比如实时监控 dashboard），走 warehouse.t_order_pf 直查而非 agg。

**Q：如果业务方要求跑分析 SQL 但不会写？**
A：Metabase 拖拽 UI 就够；不够就 agent 对话式转 SQL（未来加"sql 生成" tool，从 warehouse schema 里生成安全 SQL）。
