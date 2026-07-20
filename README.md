# nexa

**可接入的企业钉钉** — 开源通用企业协作与智能助手平台。

| 文档 | 说明 |
|------|------|
| [docs/PRODUCT.md](docs/PRODUCT.md) | **产品定位（必读）** |
| [docs/GOAL.md](docs/GOAL.md) | 里程碑与执行原则 |
| [docs/architecture/ai-native.md](docs/architecture/ai-native.md) | Skill / 感知 / 自动化 |
| [docs/api/README.md](docs/api/README.md) | API 速查 |
| [deploy/README.md](deploy/README.md) | 本地与 Docker 部署 |

## 产品是什么

**nexa 就是企业钉钉本体**，不是「旧 OA 对接钉钉数据源」。

- 组织通讯录 · 审批 · 待办 · IM · 工作台 · 数据中心 · **企业 Agent**
- 企业可 **注册租户并接入开通**
- 外部业务系统通过 **连接器** 挂入（CDC / 导出 / 可选钉钉导入）

```
企业注册租户 ──► nexa 协作本体 ──► Agent 智能入口
                      │
              连接器：业务库 / 钉钉导入（可选）
```

## 架构

| 层 | 技术 | 路径 | 端口 |
|----|------|------|------|
| 网关 | Go | `services/gateway` | 48080 |
| Agent | Node + NeoX | `services/agent` | 48091 |
| AI 控制面 | Go | `services/ai` | 48089 |
| 身份/租户 | Go | `services/iam` | 48081 |
| 审批 | Go | `services/bpm` | 48082 |
| 组织人事 | Go | `services/hr` | 48083 |
| 协同 | Go | `services/business` | 48084 |
| ERP / 财务 / IM / 运维 | Go | `services/erp` `finance` `im` `op` | 48085-88 |
| 数据中心 | Go | `services/data-center` | 48092 |
| CDC | Go | `services/cdc-mysql` | 6060 |
| 掌上 App | Flutter | `apps/mobile` | — |

**无 Java 运行时。** `legacy/**` 仅对照。

## 快速开始

```bash
export PATH="/e/tools/go/bin:$PATH"
export GOTOOLCHAIN=local

./scripts/start-dev.sh
./scripts/stop-dev.sh
```

### 开通一家企业

```bash
curl -s -X POST http://127.0.0.1:48080/v1/iam/tenants/register \
  -H "Content-Type: application/json" \
  -d "{\"company\":\"Acme\",\"adminUsername\":\"acme_admin\",\"password\":\"pass123\"}"

curl -s -X POST http://127.0.0.1:48080/v1/iam/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"acme_admin\",\"password\":\"pass123\"}"
```

### Agent

```bash
cd services/agent && cp .env.example .env
npm install
AGENT_USE_MOCK=true npm run dev
```

### 掌上 App

```bash
cd apps/mobile
flutter pub get && flutter run
```

### Docker

```bash
cd deploy && docker compose up -d --build
```

## 主要 API

| 能力 | 方法 | 路径 |
|------|------|------|
| 注册企业 | POST | `/v1/iam/tenants/register` |
| 登录 | POST | `/v1/iam/login` |
| 邀请 / 加入 | POST | `/v1/iam/invites` · `/v1/iam/invites/accept` |
| 开通状态 | GET | `/v1/iam/onboarding/status` |
| 技能目录 | GET | `/v1/ai/skills` |
| 意图路由 | POST | `/v1/ai/intent/route` |
| 连接器目录 | GET | `/v1/ai/connectors` |
| 审批待办 | GET | `/v1/bpm/tasks/todo` |
| 员工 / 组织 | GET | `/v1/hr/employees` · `/v1/hr/departments/tree` |
| 钉钉导入（可选） | POST | `/v1/hr/dingtalk/sync` |
| 数据导出 | GET/POST | `/v1/data-center/templates` · `/jobs` |

业务路由需 `Authorization: Bearer <token>`。Gateway 校验 IAM 并注入 `X-Tenant-Id`。

## 环境变量

| 变量 | 用途 |
|------|------|
| `NEXA_GATEWAY_URL` | Agent 指向网关 |
| `NEXA_AI_URL` | 感知事件上报 |
| `NEXA_DATA_DIR` | JSON 持久化目录 |
| `NEXA_DINGTALK_APP_KEY` / `SECRET` | 可选钉钉导入 |
| `AGENT_USE_MOCK` | Agent mock LLM |
| `GOTOOLCHAIN=local` | 避免强制下载高版本 Go |

## 仓库

- https://github.com/lmk1010/nexa
- CDC: https://github.com/lmk1010/nexa-cdc-mysql

## License

待定（CDC 子仓 MIT）。
