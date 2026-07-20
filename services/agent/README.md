# Agent (Go)

钉钉 / 企业 Agent 服务——nexa 的对话入口：会话、工具调用、权限、取数与导出编排。

## 目标

| 能力 | 说明 | 状态 |
|------|------|------|
| HTTP API | 健康检查 / 对话入口 | skeleton |
| 鉴权 | 用户 / 权限点 | TODO |
| 工具 | warehouse 查询、data-center 导出、业务 API | TODO |
| 钉钉接入 | 机器人 / Stream / 卡片 | TODO |
| 会话存储 | 多轮上下文 | TODO |

参考实现（**不作为运行依赖**）：`legacy/agent-node`（原 `kyx-service-agent` Node 服务）。

## 运行

```bash
cd services/agent
cp configs/config.example.yaml configs/config.yaml
go mod tidy
go run ./cmd/nexa-agent -config ./configs/config.yaml
```

## 与周边服务

```
钉钉 / App  ──►  agent  ──►  data-center（导出）
                      ├──►  warehouse（分析 SQL）
                      └──►  业务 API（OA 类）
```

CDC 由 `services/cdc-mysql`（或独立仓 `nexa-cdc-mysql`）负责灌仓，Agent 只读消费。
