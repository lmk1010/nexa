# 抽出清单（ltoa → nexa）

生成日期：2026-07-20  
定位修正：**nexa = 开源通用「钉钉 Agent + 掌上企业 + 业务后端」**；实现用 **Go / Flutter**，不用 Java。

## 已迁入

### services/data-center
- 源：`ltoa/backend/kyx-data-center/**`
- Go 导出平台 + 业务 templates + Dockerfile

### services/cdc-mysql
- 与独立仓 [`lmk1010/nexa-cdc-mysql`](https://github.com/lmk1010/nexa-cdc-mysql) **同构**（go-mysql 完整 CDC，非空骨架）
- monorepo 内副本便于联调；独立仓已有生产向提交历史

### services/agent
- **新建** Go 骨架（HTTP / 配置 / chat 占位）
- 能力对照：`legacy/agent-node`（原 Node agent）

### apps/mobile
- 源：完整 `ltoa/app` Flutter 工程（掌上企业 APK）
- 含数据中心 / 运维 / 驾驶舱 / 登录 / 工作台等

### integrations/dingtalk
- 产品资产：`sql/` + `frontend/`（登录、同步管理页）
- Java 运行时代码 → `legacy/dingtalk-java/`（仅对照）

### docs
- 灵活取数 / 看板方案、钉钉同步架构、本清单

### legacy/
- `agent-node`：完整 Node agent 参考（无 node_modules/generated）
- `dingtalk-java`：钉钉 Java 全量参考

## 有意未整包迁入

| 项 | 原因 |
|----|------|
| ltoa 全量 Java 微服务 | nexa 不跑 Java；业务能力 Go 重写 |
| canal JVM 部署 | 由 `nexa-cdc-mysql` Go 二进制替代 |
| 内部网关/密钥/生产 compose | 开源仓不带私密配置 |

## 仓库

| 远程 | 状态 |
|------|------|
| `git@github.com:lmk1010/nexa.git` | monorepo |
| `git@github.com:lmk1010/nexa-cdc-mysql.git` | 已有完整 Go CDC（main 领先 monorepo 副本应保持同步） |
