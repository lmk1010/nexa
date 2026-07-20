# nexa GOAL — 企业全能助手平台

> 用户指令（2026-07-20）：**nexa = 公司企业助手，包含企业所有能力；全部用 Go 重写（不用 Java 运行时）；Agent 融合 NeoX(Node)；设定 goal 后自主推进，勿再追问。**

## 一句话

把内部 OA/业务能力抽到 **nexa**，做成**开源通用企业助手**：对话入口 + 掌上 App + 管理端 + 全领域后端（Go）+ 数据中心 + CDC。

## 终态架构

```
钉钉 / 掌上 App / Web Admin
            │
            ▼
     services/gateway        (Go)  统一鉴权透传、路由
            │
            ├── services/agent     (Node + @mk-co/neox-sdk)  ★ 对话大脑
            │         tools ──HTTP──► 各 Go 领域服务 + data-center + warehouse
            │
            ├── services/iam       (Go)  登录/租户/用户/角色/权限
            ├── services/bpm       (Go)  审批流
            ├── services/hr        (Go)  人事/组织/考勤/入职 + 钉钉同步
            ├── services/business  (Go)  待办/工作要求/日历/接待/酒店
            ├── services/erp       (Go)  进销存
            ├── services/finance   (Go)  财务
            ├── services/im        (Go)  即时通讯对接
            ├── services/op        (Go)  运维监控
            ├── services/ai        (Go)  ASR/视觉/接待智能
            ├── services/data-center (Go) 明细导出
            └── services/cdc-mysql   (Go) binlog → warehouse
```

## 硬性约束

| # | 约束 |
|---|------|
| 1 | **运行时无 Java**；Java 只许在 `legacy/java/**` 作对照 |
| 2 | **Agent = Node + NeoX**（方案 A），不重写 neox 内核 |
| 3 | 数据面 **Go**（cdc / data-center / 领域 API） |
| 4 | App = Flutter `apps/mobile`；Admin 后续 Vue 或重做，源码可参考 legacy |
| 5 | 密钥/生产配置不进仓 |
| 6 | 大查询走 warehouse / data-center，不打爆 OLTP |

## 里程碑（自主执行顺序）

### M0 — 平台骨架 ✅
- [x] monorepo 目录
- [x] agent (NeoX) 主路径
- [x] data-center / cdc-mysql
- [x] 十大 Go 领域服务 skeleton（可 `/healthz`）
- [x] GOAL / 能力矩阵 / 集成图文档

### M1 — 统一入口与契约 ✅ partial
- [x] gateway：反向代理路由到各服务（20 routes），透传 Authorization
- [ ] `packages/nexa-common`：错误码、分页、鉴权上下文
- [x] 可用 API：iam 登录/me/perms + hr 员工/部门 + bpm 待办/审批
- [x] agent `.env.example` 指向 gateway；`curated/nexa-go-apis.json` 白名单

### M2 — IAM（企业助手底座） ✅ demo
- [x] 内存用户 admin/boss + 角色权限点
- [x] 登录发 token；`/v1/iam/me` `/v1/iam/permissions`
- [ ] MySQL 持久化 / 预登录 / 租户切换完整语义

### M3 — HR + 钉钉 ✅ demo employees
- [x] `/v1/hr/employees` `/v1/hr/departments/tree`（demo 数据）
- [ ] MySQL 员工主数据
- [ ] 钉钉同步 Go 实现

### M4 — BPM ✅ demo todo
- [x] `/v1/bpm/tasks/todo` + `/v1/bpm/tasks/approve`
- [ ] 持久化流程实例 / 完整状态机

### M5 — Business + 数据
- [ ] 待办 / 工作要求 / 日历
- [ ] data-center 与 agent 导出工具打通
- [ ] cdc 表清单与 ADS 任务

### M6 — ERP / Finance / IM / OP / AI
- [ ] 按业务优先级逐域 API
- [ ] op 对接运维监控页
- [ ] ai 对接接待/ASR 配置

### M7 — 客户端与发布
- [ ] mobile 配置切 nexa gateway
- [ ] admin 最小控制台
- [ ] docker compose 一键：gateway+iam+agent+data-center+cdc+mysql

## 能力矩阵（企业助手必须覆盖）

| 域 | 服务 | Agent 能力示例 | 状态 |
|----|------|----------------|------|
| 对话 | agent | 自然语言查数/审批/人事 | NeoX 已迁入 |
| 身份 | iam | 我是谁、权限、组织 | skeleton |
| 审批 | bpm | 待办、通过、进度 | skeleton |
| 人事 | hr | 花名册、考勤、入职 | skeleton + legacy 对照 |
| 协同 | business | 待办、要求、接待 | skeleton |
| 经营数据 | data-center + cdc | 导出、KPI、明细 | 导出/CDC 已有 |
| 进销存 | erp | 库存/采购查询 | skeleton |
| 财务 | finance | 报表/流水查询 | skeleton |
| 消息 | im | 会话/通知 | skeleton |
| 运维 | op | 健康、发布、审计 | skeleton |
| 智能 | ai | 接待、ASR | skeleton |
| 钉钉 | hr + agent | 登录、同步、机器人 | 资产+legacy |

## 明确不做

- 把 Java 服务原样当 nexa 运行时启动
- 全量翻译 neox-core/kernel 到 Go（除非单独开 neox-sdk-go 项目）
- 在 agent 进程内跑 CDC

## 进度记录

| 日期 | 完成 |
|------|------|
| 2026-07-20 | 抽 data-center/cdc/钉钉/app/agent；方案 A；Go 领域 skeleton×10；本 GOAL |
| 2026-07-20 | gateway 反向代理；IAM/HR/BPM 可调用 demo API；agent 对接 gateway；smoke 通过 |

## 执行原则（无人值守）

1. 始终 **Go 写领域服务**，对照 `legacy/java/*` 只读。  
2. 每完成一域：README 状态、`/healthz`、基础路由、agent 可调 path 写进 `docs/api/`。  
3. 能编译的默认要能 `go build`（优先 stdlib，少依赖）。  
4. 定期 commit + push `nexa`（noreply 作者）。  
5. 阻塞（私有 npm、外网 proxy）记入 `docs/BLOCKERS.md`，换路径继续，不空等。
