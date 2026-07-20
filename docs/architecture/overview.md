# nexa 架构总览

## 定位

`nexa` 将 `ltoa` 中与 **数据平台 / 企业钉钉 / 掌上数据分析** 相关的能力抽成独立 monorepo：

| 子系统 | 职责 | 语言（目标） |
|--------|------|--------------|
| `services/data-center` | 模板化明细导出、任务队列、XLSX | Go（已完成初版） |
| `services/cdc-mysql` | MySQL binlog CDC → warehouse | Go（重写中，替代 canal） |
| `integrations/dingtalk` | 通讯录/花名册/考勤/审批/通知 | Java 源码抽出 → 可逐步 Go 化 |
| `apps/mobile` | 掌上企业：数据中心 / 运维 / 驾驶舱 | Flutter 页面抽出 |

## 数据流

```
ordersys / 业务 MySQL
        │ ROW binlog (只读)
        ▼
  nexa-cdc-mysql  ──位点(GTID/file:pos)──► store
        │
        │ upsert / soft-delete
        ▼
 warehouse.ordersys_dw (ODS 明细)
        │ 定时/增量
        ▼
 ADS 预聚合 (agg_*_daily)
        │
   ┌────┴────┬────────────┐
   ▼         ▼            ▼
 data-center  Agent/API   BI/Metabase
   App导出     对话取数     自助分析
```

## 约束

1. CDC **只读** 源库 binlog，禁止业务表热路径 SELECT（全量 backfill 除外）。
2. warehouse 对业务账号默认 **只读**；写权限仅给 cdc sink 与 ADS job。
3. data-center 导出走 warehouse，不直连生产 OLTP。
4. App / 管理端权限双校验：前端隐藏 + 后端 permission point。

## 与 ltoa 的关系

- **ltoa**：OA 业务单体（HR / BPM / IAM / App 壳）。
- **nexa**：数据与集成能力中台；可被 ltoa gateway 反代，也可独立部署。

抽出后 ltoa 内原模块可逐步改为调用 nexa 服务，或继续内嵌运行（过渡期双轨）。

## 仓库拆分建议

| 阶段 | 动作 |
|------|------|
| 现在 | monorepo `nexa` 聚合所有抽出代码 |
| 下一阶段 | `services/cdc-mysql` 成熟后推到 `lmk1010/nexa-cdc-mysql` 独立仓 |
| 可选 | `services/data-center` → `nexa-data-center`；钉钉 → `nexa-dingtalk` |
