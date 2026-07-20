# nexa 架构总览

## 定位

**开源通用**企业协作与数据平台：钉钉 Agent + 掌上企业 App + 后端业务（组织人事、审批协同、数据中心、运维分析等），能力覆盖接近传统 OA。

实现语言：**Go**（Agent / 业务 API / 数据中心 / CDC）、**Flutter**（掌上 App）。**不使用 Java 作为运行时**；`legacy/` 仅保留旧实现对照。

## 子系统

| 子系统 | 职责 | 语言 | 状态 |
|--------|------|------|------|
| `services/agent` | 钉钉/企业 Agent：会话、工具、权限、取数 | Go | 目录已建；参考 `legacy/agent-node` |
| `services/data-center` | 模板化明细导出、任务队列、XLSX | Go | 已从 ltoa 迁入可运行服务 |
| `services/cdc-mysql` | MySQL binlog → warehouse ODS | Go | 可运行骨架；独立仓 `nexa-cdc-mysql` |
| `integrations/dingtalk` | 登录、同步、通知（产品能力 + SQL/前端参考） | Go 目标 | SQL/前端已抽出；Java 在 `legacy/dingtalk-java` |
| `apps/mobile` | 掌上企业：数据中心 / 运维 / 驾驶舱 | Flutter | 页面与 service 已抽出 |

## 数据流

```
业务 MySQL（ordersys 等）
        │ ROW binlog（只读）
        ▼
  cdc-mysql  ──位点──► store
        │
        ▼
 warehouse (ODS 明细 → ADS 预聚合)
        ▲
   ┌────┴────┬────────────┐
   │         │            │
data-center  agent     BI 只读
   App导出   对话取数   Metabase 等
```

## 产品约束

1. CDC **只读** 源库 binlog；热路径不对业务表 SELECT（全量 backfill 除外）。
2. warehouse 对业务账号默认 **只读**；写权限仅 cdc sink 与 ADS job。
3. data-center 导出走 warehouse，不直连生产 OLTP。
4. App / 管理端：前端隐藏 + 后端 permission 双校验。

## 与内部 ltoa 的关系

- **ltoa**：既有内部 OA 单体（历史 Java/Node）。
- **nexa**：开源通用产品线；能力从 ltoa 抽离后 **Go 重写**，不绑定内部包名与部署。

## 仓库策略

| 阶段 | 动作 |
|------|------|
| 现在 | monorepo `nexa` 聚合 Agent、App、数据中心、钉钉、CDC |
| 并行 | `nexa-cdc-mysql` 独立仓与 `services/cdc-mysql` 同构演进 |
| 可选 | 再拆 `nexa-data-center` / `nexa-agent` 等 |
