# nexa GOAL — 企业钉钉（可接入协作平台）

> 用户指令（持续有效）：
> 1. **nexa 就是企业钉钉本体**，不是「旧 OA 对接钉钉数据源」。
> 2. **支持用户/企业接入开通**（多租户产品）。
> 3. 领域能力 **Go 重写**；Agent = **NeoX(Node)**；goal 驱动自主推进。
> 4. 产品定义见 `docs/PRODUCT.md`。

## 一句话

**nexa = 可接入的企业钉钉**：组织通讯录 · 审批 · 待办 · IM · 工作台 · 数据中心 · 企业 Agent。
外部业务系统通过 **连接器** 接入 nexa，而不是 nexa 去当旧 OA 的附属同步器。

## 终态架构

```
企业用户 ──注册/登录租户──► nexa（产品本体）
                               │
                    ┌──────────┼──────────┐
                    ▼          ▼          ▼
                 组织成员    审批/待办    IM/工作台
                    └──────────┬──────────┘
                               ▼
                      Agent（NeoX）企业大脑
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
         nexa 域服务      数据中心/CDC     外部连接器
         (Go 本体)        (经营分析)     (业务库/API 可选)
```

## 硬性约束

| # | 约束 |
|---|------|
| 1 | **产品定位**：协作平台本体，不是 OA→钉钉同步插件 |
| 2 | **运行时无 Java**；Java 只许 `legacy/**` 对照 |
| 3 | **Agent = Node + NeoX**，tools 调 nexa Go 服务 |
| 4 | **多租户可接入**：注册/邀请/成员/权限为产品主路径 |
| 5 | 钉钉 OpenAPI = **可选导入连接器**，不是主数据定义 |
| 6 | 密钥不进仓；大查询走 warehouse/data-center |


## 里程碑（自主执行顺序）

### M0 — 平台骨架 ✅
- [x] monorepo 目录
- [x] agent (NeoX) 主路径
- [x] data-center / cdc-mysql
- [x] 十大 Go 领域服务 skeleton（可 `/healthz`）
- [x] GOAL / 能力矩阵 / 集成图文档

### M1 — 统一入口与契约 ✅
- [x] gateway：反向代理路由到各服务（20 routes）
- [x] gateway auth：IAM introspect 鉴权（login/agent/health 放行）
- [x] `packages/nexa-common`：httpx + FileStore
- [x] 可用 API：iam 登录/me/perms + hr 员工/部门 + bpm 待办/审批
- [x] agent `.env.example` 指向 gateway；`curated/nexa-go-apis.json` 白名单

### M2 — IAM（企业助手底座） ✅ file-backed
- [x] 用户 admin/boss + 角色权限点（JSON store）
- [x] 登录 token / me / permissions / users / introspect / logout
- [ ] MySQL 与预登录/租户切换完整语义

### M3 — HR + 钉钉 ✅ file-backed + sync skeleton
- [x] employees / departments tree（JSON store）
- [x] DingTalk sync skeleton：`POST /v1/hr/dingtalk/sync` + jobs/status（模拟 OpenAPI，可换真客户端）
- [x] 真钉钉 OpenAPI client（环境变量开启）+ 无凭证时模拟同步
- [ ] 增量同步/字段全量映射增强

### M4 — BPM ✅ file-backed
- [x] todo/done/approve/start 持久化到 JSON
- [ ] 完整流程定义与多节点状态机

### M5 — Business + 数据 ✅ file-backed
- [x] 待办 / 工作要求 / 日历 / 接待 latest（JSON store）
- [ ] data-center 与 agent 导出工具打通
- [ ] cdc 表清单与 ADS 任务

### M6 — ERP / Finance / IM / OP / AI ✅ file-backed baseline
- [x] erp/finance/im/op file-backed
- [x] ai control plane skills/intent/sense/automation
- [x] op live health probe + sense on down
- [ ] MySQL 持久化与真实对接
- [ ] op 与 App 运维页联调

### M9 — 租户接入（产品主路径） 🔄
- [x] 企业注册 / 创建租户 API
- [x] 邀请成员 / 接受邀请 API
- [x] 开通状态 checklist
- [ ] 租户内角色权限默认模板增强
- [ ] 开通向导前端/App

### M8 — AI-Native 企业能力 ✅ baseline
- [x] AI control plane：skills / intent / sense / automation
- [x] 企业 skill 目录（26+）覆盖 OA 全域
- [x] Gateway 对 AI 路由/bootstrap 边缘放行
- [x] Agent curated skills + enterprise prompt + nexa_* tools
- [x] 域事件自动上报 sense（bpm/hr/business/im/op）
- [ ] 真 LLM intent（可选，当前关键词路由可用）

### M7 — 客户端与发布 ✅ partial
- [x] mobile baseUrl 默认 nexa-gateway :48080
- [x] agent 默认加载 enterprise prompt + nexa skill tools
- [ ] admin 最小控制台
- [ ] docker compose 一键：gateway+iam+agent+data-center+cdc+mysql

## 能力矩阵（企业助手必须覆盖）

| 域 | 服务 | Agent 能力示例 | 状态 |
|----|------|----------------|------|
| 对话 | agent | 自然语言查数/审批/人事 | NeoX 已迁入 |
| 身份 | iam | 我是谁、权限、组织 | file-backed |
| 审批 | bpm | 待办、通过、进度 | file-backed |
| 人事 | hr | 花名册、考勤、入职 | file-backed + dingtalk skeleton |
| 协同 | business | 待办、要求、接待 | demo API |
| 经营数据 | data-center + cdc | 导出、KPI、明细 | 导出/CDC 已有 |
| 进销存 | erp | 库存/采购查询 | demo API |
| 财务 | finance | 报表/流水查询 | demo API |
| 消息 | im | 会话/通知 | demo API |
| 运维 | op | 健康、发布、审计 | demo API |
| 智能 | ai | 接待、ASR | demo API |
| 协作本体 | iam/hr/bpm/im/business | 组织/审批/IM/待办（钉钉类能力） | 主路径建设中 |
| 连接器-钉钉导入 | hr dingtalk | 可选通讯录导入 | openapi client |
| 连接器-业务库 | cdc/data-center | 外部经营数据 | 已有 |

## 明确不做

- 把 Java 服务原样当 nexa 运行时启动
- 全量翻译 neox-core/kernel 到 Go（除非单独开 neox-sdk-go 项目）
- 在 agent 进程内跑 CDC

## 进度记录

| 日期 | 完成 |
|------|------|
| 2026-07-20 | 抽 data-center/cdc/钉钉/app/agent；方案 A；Go 领域 skeleton×10；本 GOAL |
| 2026-07-20 | gateway 反向代理；IAM/HR/BPM 可调用 demo API；agent 对接 gateway；smoke 通过 |
| 2026-07-20 | business/erp/finance/im/op/ai demo API；start-dev/stop-dev；agent curated 全量 path |
| 2026-07-20 | file-backed IAM/HR/BPM；钉钉 sync skeleton；Dockerfiles + compose；nexa-common httpx/store |
| 2026-07-20 | gateway IAM introspect auth |
| 2026-07-20 | business/erp/finance file-backed stores |
| 2026-07-20 | AI control plane skills/intent/sense/automation；gateway AI edge；agent enterprise prompt |
| 2026-07-20 | im/op file-backed + agent nexa tools + mobile gateway default |
| 2026-07-20 | dingtalk openapi client (env-gated) + data-center lite local export surface |

## 执行原则（无人值守）

1. 始终 **Go 写领域服务**，对照 `legacy/java/*` 只读。  
2. 每完成一域：README 状态、`/healthz`、基础路由、agent 可调 path 写进 `docs/api/`。  
3. 能编译的默认要能 `go build`（优先 stdlib，少依赖）。  
4. 定期 commit + push `nexa`（noreply 作者）。  
5. 阻塞（私有 npm、外网 proxy）记入 `docs/BLOCKERS.md`，换路径继续，不空等。

| 2026-07-20 | PRODUCT.md 定位纠正：nexa=可接入企业钉钉本体；租户注册/邀请 API |
| 2026-07-20 | nexa-core 合并业务端口；logo+GitHub README；start-dev 仅 iam+core |
