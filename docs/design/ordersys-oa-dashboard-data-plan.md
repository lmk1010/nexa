# ordersys 到 OA 老板驾驶舱数据方案

日期：2026-07-07

## 目标

把 ordersys 里对老板最有价值的数据，以低风险方式同步/聚合到 OA MySQL，再由 OA 提供稳定接口给 App 首页、Agent、驾驶舱使用。

核心指标不是“任务数量”，而是：

- 营业额、订单数、利润/毛利率
- 平台赔付、客户/商家/公司承担金额
- 补发率、补发金额/次数
- 实际差评、好评率、售后率
- 工单超时、催安装、拒单、撤单、换店
- 车损、划痕、安装不到位、投诉等质量问题
- 部门、人员、卖家、门店维度归因

## 当前判断

ordersys 数据量大，且存在按年/月分表，不能让 App 首页或 Agent 高频实时打 ordersys 大查询。

正确路线：

1. Agent 可以临时查询 ordersys 已有读接口，但必须带日期范围、分页、超时、限流。
2. App 首页/老板驾驶舱不直接实时访问 ordersys。
3. OA 定时从 ordersys 拉取分类统计结果或增量明细。
4. OA MySQL 保存分类事实表。
5. OA 再聚合出驾驶舱宽表，供 App/Agent 快速读取。

## ordersys 分表风险

已从代码配置确认 ordersys 使用 ShardingSphere JDBC。

典型分表：

- `t_order`：按年分表，核心订单表。
- `t_work`：按年分表，核心工单表。
- `t_work_order`：按年分表，工单关联订单。
- `t_order_detail`、`t_order_log`、`t_order_flag` 等：按订单维度推年份。
- `t_order_revoke`、`outlets_record`、`t_sms`：按创建时间分表。
- `sys_oper_log`：按月分表。

性能结论：

- 没有日期范围或分片键的 SQL，可能打到全部年份表。
- 不要在 ordersys 上跑 `COUNT(*)` 估算体量。
- 首页不能实时查 ordersys 大表。
- Agent 工具必须默认加日期范围，查询失败或超时要停止重试，不能短时间大量打入。

## 明天安全估算 SQL

只查 `information_schema.tables`。这是元数据估算，不读业务明细，不要用 `COUNT(*)`。

先确认：

```sql
SHOW VARIABLES LIKE 'innodb_stats_on_metadata';
```

如果值是 `ON`，当前会话先关掉：

```sql
SET SESSION innodb_stats_on_metadata = OFF;
```

### 1. ordersys 总体体量

```sql
SELECT table_schema,
       COUNT(*) AS table_count,
       SUM(table_rows) AS approx_rows,
       ROUND(SUM(data_length) / 1024 / 1024 / 1024, 2) AS data_gb,
       ROUND(SUM(index_length) / 1024 / 1024 / 1024, 2) AS index_gb,
       ROUND(SUM(data_length + index_length) / 1024 / 1024 / 1024, 2) AS total_gb
FROM information_schema.tables
WHERE table_schema = 'order-sys'
  AND table_type = 'BASE TABLE'
GROUP BY table_schema;
```

### 2. 核心表/分表体量

```sql
SELECT table_name,
       engine,
       table_rows AS approx_rows,
       ROUND(data_length / 1024 / 1024, 1) AS data_mb,
       ROUND(index_length / 1024 / 1024, 1) AS index_mb,
       ROUND((data_length + index_length) / 1024 / 1024, 1) AS total_mb,
       update_time
FROM information_schema.tables
WHERE table_schema = 'order-sys'
  AND table_type = 'BASE TABLE'
  AND (
    table_name LIKE 't_order%'
    OR table_name LIKE 't_work%'
    OR table_name LIKE 't_transaction_record%'
    OR table_name LIKE 'shop_order_log%'
    OR table_name LIKE 'outlets_amount_record%'
    OR table_name LIKE 'outlets_record%'
    OR table_name LIKE 't_sms%'
    OR table_name IN (
      't_order_pf',
      't_order_reissue',
      't_work_rate',
      'order_good',
      't_order_seller_revoke',
      'user_points_record',
      'sys_seller',
      'sys_user',
      'sys_dept'
    )
  )
ORDER BY total_mb DESC
LIMIT 300;
```

### 3. MySQL 8 按逻辑表汇总分表

```sql
SELECT CASE
         WHEN table_name REGEXP '_[0-9]{4}_[0-9]{2}$'
           THEN REGEXP_REPLACE(table_name, '_[0-9]{4}_[0-9]{2}$', '')
         WHEN table_name REGEXP '_[0-9]{4}$'
           THEN REGEXP_REPLACE(table_name, '_[0-9]{4}$', '')
         ELSE table_name
       END AS logical_table,
       COUNT(*) AS shard_count,
       SUM(table_rows) AS approx_rows,
       ROUND(SUM(data_length + index_length) / 1024 / 1024 / 1024, 2) AS total_gb,
       MIN(table_name) AS first_table,
       MAX(table_name) AS last_table
FROM information_schema.tables
WHERE table_schema = 'order-sys'
  AND table_type = 'BASE TABLE'
GROUP BY logical_table
ORDER BY total_gb DESC;
```

### 4. MySQL 5.7 兼容版逻辑表汇总

MySQL 5.7 没有 `REGEXP_REPLACE`，先用核心分表前缀粗略汇总：

```sql
SELECT logical_table,
       COUNT(*) AS shard_count,
       SUM(approx_rows) AS approx_rows,
       ROUND(SUM(total_bytes) / 1024 / 1024 / 1024, 2) AS total_gb
FROM (
  SELECT CASE
           WHEN table_name LIKE 't_order\_%' THEN 't_order*'
           WHEN table_name LIKE 't_work\_%' THEN 't_work*'
           WHEN table_name LIKE 't_transaction_record\_%' THEN 't_transaction_record*'
           WHEN table_name LIKE 'shop_order_log\_%' THEN 'shop_order_log*'
           WHEN table_name LIKE 'outlets_amount_record\_%' THEN 'outlets_amount_record*'
           WHEN table_name LIKE 'outlets_record\_%' THEN 'outlets_record*'
           WHEN table_name LIKE 'sys_oper_log\_%' THEN 'sys_oper_log*'
           ELSE table_name
         END AS logical_table,
         table_rows AS approx_rows,
         data_length + index_length AS total_bytes
  FROM information_schema.tables
  WHERE table_schema = 'order-sys'
    AND table_type = 'BASE TABLE'
) t
GROUP BY logical_table
ORDER BY total_gb DESC;
```

## 已有高价值 ordersys 接口

优先补进 Agent/API 白名单，但默认只允许读、必须带日期。

### 售后统计

- `/user/statistics/sh/order`：售后订单、收入、安装价、利润口径。
- `/user/statistics/sh/work`：售后工单处理/完成。
- `/user/statistics/sh/revoke`：撤单。
- `/user/statistics/sh/rate`：评价、差评、超时类不满原因。
- `/user/statistics/sh/bf`：补发。
- `/user/statistics/sh/pf`：赔付。
- `/user/statistics/sh/workHint`：质量问题标记，含车损。
- `/user/statistics/sh/work/goodRate`：按质量类型的好评/图片。

### 派单统计

- `/user/statistics/pd/order`：派单订单、营业额、安装价、利润。
- `/user/statistics/pd/work`：派单工单、超时、催安装、换店、拒单、撤单等。
- `/user/statistics/pd/Profit`：派单积分/门店盈利数据。

### 排行榜

- `/user/statistics/rank/order`：订单/收入排行。
- `/user/statistics/rank/evaluation`：评价排行。
- `/user/statistics/rank/reissue`：补发排行。
- `/user/statistics/rank/work`：工单排行。
- `/user/statistics/rank/compensation`：赔付排行。
- `/user/statistics/rank/weight`：质量/权重问题排行。

## OA 侧建议表设计

不要一开始只做一张巨宽表。先落分类事实表，再汇总宽表。

### 1. 订单收入日统计表

表名建议：`biz_ordersys_order_daily`

粒度：日期 + 业务类型 + 部门 + 人员 + 卖家/门店。

核心字段：

- `stat_date`
- `biz_scene`：`sh` / `pd`
- `dept_id`
- `user_id`
- `seller_id`
- `outlets_id`
- `order_count`
- `seller_count`
- `sum_order_total`
- `sum_install_price`
- `gross_profit`
- `gross_profit_rate`
- `source_start_time`
- `source_end_time`
- `sync_time`

### 2. 工单交付日统计表

表名建议：`biz_ordersys_work_daily`

核心字段：

- `stat_date`
- `biz_scene`
- `dept_id`
- `user_id`
- `handle_work_count`
- `finish_work_count`
- `timeout_count`
- `urge_install_count`
- `reject_count`
- `change_outlets_count`
- `revoke_count`
- `avg_finish_minutes`
- `sync_time`

### 3. 赔付日统计表

表名建议：`biz_ordersys_compensation_daily`

核心字段：

- `stat_date`
- `dept_id`
- `user_id`
- `seller_id`
- `compensation_count`
- `total_money`
- `outlets_money`
- `seller_money`
- `my_money`
- `customer_service_money`
- `accident_money`
- `cost_money`
- `my_money_rate`
- `sync_time`

### 4. 补发日统计表

表名建议：`biz_ordersys_reissue_daily`

核心字段：

- `stat_date`
- `dept_id`
- `user_id`
- `seller_id`
- `reissue_count`
- `reissue_order_count`
- `reissue_rate`
- `reissue_money`
- `sync_time`

### 5. 评价质量日统计表

表名建议：`biz_ordersys_quality_daily`

核心字段：

- `stat_date`
- `biz_scene`
- `dept_id`
- `user_id`
- `seller_id`
- `rate_count`
- `good_rate_count`
- `bad_rate_count`
- `actual_bad_rate_count`
- `good_rate`
- `after_sale_rate`
- `car_damage_count`
- `scratch_count`
- `dirty_part_count`
- `bad_install_count`
- `complaint_count`
- `work_hint_count`
- `sync_time`

### 6. 驾驶舱宽表

表名建议：`biz_boss_dashboard_daily`

粒度：日期 + 维度类型 + 维度 ID。

示例：

- `stat_date`
- `dimension_type`：`company` / `dept` / `user` / `seller` / `outlets`
- `dimension_id`
- `dimension_name`
- `order_count`
- `sum_order_total`
- `gross_profit`
- `gross_profit_rate`
- `compensation_money`
- `company_compensation_money`
- `reissue_count`
- `reissue_rate`
- `bad_rate_count`
- `actual_bad_rate_count`
- `timeout_count`
- `car_damage_count`
- `after_sale_rate`
- `risk_score`
- `sync_time`

App 首页只读这张宽表或对应 OA 聚合接口。

## 同步策略

### 第一阶段：最简单可落地

每天夜里同步昨天、近 7 天修正窗口、当月汇总。

建议任务：

- 每 30 分钟同步今天轻量统计。
- 每晚 02:00 同步昨天完整统计。
- 每晚 03:00 重算近 7 天，修正迟到数据。
- 每月 1 日重算上月月报。

### 第二阶段：更稳

增加同步游标和幂等：

- 按接口 + 日期 + 维度保存同步批次。
- 同一天同维度用 `REPLACE INTO` 或唯一键 `ON DUPLICATE KEY UPDATE` 覆盖。
- 失败只重试当前日期窗口，不扩大范围。

### 第三阶段：如果后续要实时

再考虑 binlog/Flink，但不是第一版必须。

当前不建议一上来 Flink：

- ordersys 分表和历史口径需要先理清。
- 当前核心目标是驾驶舱和 Agent 能答老板问题。
- 先用定时同步 + 宽表，风险最低。

## Agent 查询保护

ordersys 工具必须有保护规则：

- 默认日期范围：今天、本月、最近 7 天之一，不能无日期查。
- 默认分页：列表类接口必须分页，不允许全表拉取。
- 超时：单次请求建议 5-8 秒。
- 熔断：同一接口连续超时 2 次，短时间内停止打入。
- 降级：提示用户缩小日期或改查 OA 聚合结果。
- 缓存：同一问题、同一日期范围、同一接口短时间复用结果。
- 白名单：只放读接口，不放删除、修改、审批、导出类危险接口。

## 明天试跑步骤

1. 在能访问 ordersys MySQL 的机器上执行安全估算 SQL。
2. 先看 `t_order*`、`t_work*`、`t_order_pf`、`t_order_reissue`、`t_work_rate`、`order_good` 的体量。
3. 不跑任何业务表 `COUNT(*)`。
4. 把估算结果保存回来。
5. 根据最大表、最大年份、索引大小，决定首版同步窗口。
6. 首版优先接已有统计接口，不直接写复杂跨分表 SQL。
7. OA 先建分类事实表和驾驶舱宽表。
8. App 首页只访问 OA 聚合接口。

## 首版优先级

P0：

- 今日/本月营业额
- 今日/本月利润/毛利率
- 订单数
- 平台/公司赔付
- 补发率
- 实际差评
- 超时工单
- 车损

P1：

- 售后率
- 部门排行
- 人员排行
- 卖家排行
- 门店排行
- 工单交付效率

P2：

- 质量问题趋势
- 赔付责任归因
- 异常预警
- 自动老板日报

## 明确禁止

- 禁止 App 首页实时打 ordersys 大接口。
- 禁止无日期范围查 ordersys。
- 禁止 `COUNT(*)` 大表估算。
- 禁止全量导出后在 Agent 里自己统计。
- 禁止把每个业务问题都做成定制语义工具。
- 禁止短时间大量重试慢接口。

