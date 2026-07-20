# nexa 现状：部署 vs 开发

规范见 [CONVENTIONS.md](CONVENTIONS.md) · 变更见 [CHANGELOG.md](../CHANGELOG.md)

> 产品：可接入的企业钉钉本体（见 PRODUCT.md）
> 进程：core(:48080) + iam(:48081) + 可选 agent/cdc

## 部署

### 已有

- 本地 `./scripts/start-dev.sh`（仅 iam+core）
- `./scripts/smoke.sh` 开通链路冒烟
- `services/core` / `services/iam` Dockerfile
- `deploy/docker-compose.yml`（core+iam；agent/cdc 用 profile）
- `deploy/nginx.example.conf`、`.env.example`
- `Makefile`、GitHub Actions CI 编译+boot smoke
- README + logo、数据卷 JSON 持久化

### 还差

- 生产 TLS/域名证书
- 密钥注入（LLM、钉钉）、禁用弱口令 demo 依赖
- 卷备份 / 迁 MySQL 后备份策略
- 监控告警（Prometheus/uptime）
- 镜像推送到仓库、linux release 二进制
- 生产 CORS/限流/WAF

## 开发

### 已有

- 租户注册/邀请/加入/onboarding
- 登录+introspect+网关鉴权+租户头
- 组织/审批/待办/ERP/财务/IM/AI skills 面
- 可选钉钉导入客户端、data-center lite
- Agent NeoX + nexa tools + enterprise prompt
- Mobile 默认指向 :48080

### 还差（优先级）

1. core 内全域 `X-Tenant-Id` 过滤写实
2. ~~审批状态机 + IM 会话/消息~~ ✅
3. 连接器按租户配置（不仅目录）
4. MySQL 替换 JSON store
5. Agent 真 LLM 联调
6. App 开通向导 UI
7. 管理端最小控制台
8. 密码安全增强（bcrypt）、审计
9. 单测 + e2e
10. 全量 data-center 引擎 + CDC 生产配置

## 一键

```bash
./scripts/start-dev.sh && ./scripts/smoke.sh
cd deploy && docker compose up -d --build
```

### 2026-07-20 update
- core tenant filter across domains
- per-tenant connector config
- IM create/list/send/read + contacts from employees; password change; users tenant-scoped
