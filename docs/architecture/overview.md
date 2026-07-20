# nexa 架构总览

## 定位

开源通用企业协作与数据平台：钉钉 Agent + 掌上企业 App + 后端业务（组织人事、审批协同、数据中心、运维分析等）。

## 方案 A（已采纳）

| 子系统 | 职责 | 运行时 | 状态 |
|--------|------|--------|------|
| `services/agent` | 钉钉/企业对话：NeoX loop + tools | **Node + `@mk-co/neox-sdk`** | 主路径 |
| `services/data-center` | 模板导出、任务队列、XLSX | **Go** | 已迁入 |
| `services/cdc-mysql` | binlog → warehouse ODS | **Go** | 与独立仓同步 |
| `integrations/dingtalk` | 登录/同步产品资产 | SQL + 前端参考；同步 Go 演进 | 资产已抽 |
| `apps/mobile` | 掌上企业 | Flutter | 完整工程 |

**融合**：Agent 与 Go 服务通过 **HTTP tools** 集成，不共享进程、不强行 Go 化 NeoX。

## 数据流

```
业务 MySQL
    │ ROW binlog
    ▼
cdc-mysql (Go)
    ▼
warehouse (ODS → ADS)
    ▲
    │ RO / 导出任务
agent (Node/NeoX) ──► data-center (Go)
    │
    └──► OA/ordersys 只读 API（网关）
```

## 约束

1. CDC 只读源库 binlog。  
2. warehouse 业务账号默认只读。  
3. 大导出优先 data-center，不直连生产 OLTP。  
4. Agent 鉴权依赖网关透传 + 下游 API 权限。  

## 与 neox 的关系

- **neox-sdk（TS/Node）**：Agent 运行时（`Agent.run` / `stream` / `tool`）。  
- **nexa**：产品与业务编排；agent 服务是 neox 的宿主。  
- **不**在 nexa 内维护 Go 版全量 neox；若未来需要，独立做 `neox-sdk-go` 最小子集。

## 仓库策略

- monorepo `nexa`：Agent(Node) + App + data-center + cdc 副本 + 文档  
- `nexa-cdc-mysql`：CDC 独立演进  
