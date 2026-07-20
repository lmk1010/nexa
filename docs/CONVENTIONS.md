# nexa 仓库规范

## 进程与端口（标准）

| 进程 | 端口 | 是否默认启动 | 说明 |
|------|------|--------------|------|
| nexa-core | 48080 | 是 | 网关 + 全部业务域 |
| nexa-iam | 48081 | 是 | 认证 / 租户 |
| nexa-agent | 48091 | 否（可选） | NeoX Agent |
| nexa-cdc | 6060 | 否（可选） | MySQL CDC |

禁止在默认 `start-dev` / compose 中再增加业务端口，除非有独立扩展需求并更新本文档与 `docs/STATUS.md`。

## 目录约定

```
services/core/          # 生产业务入口（优先改这里）
services/iam/           # 认证独立
services/agent/         # Agent 独立
services/cdc-mysql/     # 可选
services/{domain}/      # 参考实现，勿当作默认部署单元
legacy/                 # 只读历史
docs/                   # 文档
deploy/                 # compose / nginx
scripts/                # start/stop/smoke 与生成脚本
assets/                 # logo 等静态品牌
```

## 配置约定

| 文件 | 用途 |
|------|------|
| `configs/config.example.json` | 本地示例 |
| `configs/config.docker.json` | 镜像内默认 |
| 环境变量 `NEXA_*` | 覆盖敏感与地址 |

常用环境变量：

- `NEXA_DATA_DIR` — 持久化目录  
- `NEXA_IAM_URL` / `NEXA_AGENT_URL` — core 反代目标  
- `NEXA_GATEWAY_URL` — agent 调 core  
- `NEXA_AI_URL` — sense 上报  
- `NEXA_DINGTALK_APP_KEY` / `SECRET` — 可选导入连接器  
- `GOTOOLCHAIN=local` — 禁止自动下高版本 Go  

## API 约定

- 成功：`{"code":0,"data":...}`  
- 失败：`{"code":非0,"msg":"..."}`  
- 鉴权：`Authorization: Bearer <token>`  
- 租户头：`X-Tenant-Id`（由 core/gateway 注入）  
- 路径：`/v1/<domain>/...` 为主；兼容 `/app-api` `/admin-api` 仅移动端  

## 文档约定

| 文档 | 内容 |
|------|------|
| PRODUCT.md | 产品是什么 / 不是什么 |
| GOAL.md | 里程碑与执行约束 |
| STATUS.md | 部署/开发已有与还差 |
| CONTRIBUTING.md | 贡献与开发流 |
| CHANGELOG.md | 版本记录 |
| api/README.md | API 速查 |
| architecture/* | 架构 |

改行为时同步 STATUS；改定位时同步 PRODUCT。

## 脚本约定

| 脚本 | 用途 |
|------|------|
| `scripts/start-dev.sh` | 仅 iam+core |
| `scripts/stop-dev.sh` | 停本地进程 |
| `scripts/smoke.sh` | 健康+注册+鉴权 API |
| `scripts/write_*.py` | 代码生成/修补（可保留，非运行依赖） |

## Git 约定

- `main` 为发布线  
- commit 信息：`type(scope): summary`  
- 不提交密钥、`.run/`、本地 `data/`  
- CI 必须能 `go build` core 与 iam  
