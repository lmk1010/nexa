# 抽出清单（ltoa → nexa）

生成日期：2026-07-20  
**方案 A**：Agent = Node + NeoX；数据面 = Go。

## 主路径

### services/agent（Node + `@mk-co/neox-sdk`）
- 源：`ltoa/backend/kyx-service-agent`
- 包名：`@nexa/agent`
- 职责：对话、tool calling、stream；tools 调 Go data-center / warehouse / 只读 API
- **不是** Go 服务

### services/data-center
- 源：`ltoa/backend/kyx-data-center/**`
- Go 导出平台

### services/cdc-mysql
- 与 [`lmk1010/nexa-cdc-mysql`](https://github.com/lmk1010/nexa-cdc-mysql) 同构
- Go binlog CDC

### apps/mobile
- 源：完整 `ltoa/app` Flutter 工程

### integrations/dingtalk
- `sql/` + `frontend/` 产品资产
- Java → `legacy/dingtalk-java/`

## legacy（对照，非默认部署）

| 路径 | 说明 |
|------|------|
| `legacy/agent-node/` | agent 历史副本（与 services/agent 同源快照） |
| `legacy/agent-go-skeleton/` | 已废弃的 Go HTTP 占位 |
| `legacy/dingtalk-java/` | 钉钉 Java 对照 |

## 有意未整包迁入

| 项 | 原因 |
|----|------|
| ltoa 全量 Java 微服务 | nexa 业务 API 用 Go 重写 |
| canal JVM | 由 cdc-mysql 替代 |
| 生产密钥 / 内网 compose | 不进开源仓 |

## 仓库

| 远程 | 状态 |
|------|------|
| `git@github.com:lmk1010/nexa.git` | monorepo |
| `git@github.com:lmk1010/nexa-cdc-mysql.git` | 独立 CDC |
