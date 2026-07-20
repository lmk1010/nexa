# legacy/

旧实现**仅作能力对照**，不是 nexa 默认运行依赖。

| 目录 | 说明 |
|------|------|
| `agent-node/` | 与 `services/agent` 同源的历史快照（Node + NeoX） |
| `agent-go-skeleton/` | 已废弃的 Go Agent 占位（方案 A 不采用） |
| `dingtalk-java/` | 钉钉 Java 全量对照 |

## 正式运行路径

- Agent → `services/agent`（Node + `@mk-co/neox-sdk`）
- 导出 → `services/data-center`（Go）
- CDC → `services/cdc-mysql`（Go）
- App → `apps/mobile`（Flutter）
