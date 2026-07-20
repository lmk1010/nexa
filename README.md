# nexa

企业数据与协同能力平台（从 `ltoa` 抽离）。

目标：把原先嵌在 `ltoa` 单体里的 **数据管理 / CDC / 掌上企业钉钉 / 数据分析** 能力，拆成独立 monorepo，便于 Go 重写、独立部署与复用。

## 仓库

| 仓库 | 说明 |
|------|------|
| `git@github.com:lmk1010/nexa.git` | 本 monorepo（数据中心、钉钉、App 数据页、CDC 骨架） |
| `git@github.com:lmk1010/nexa-cdc-mysql.git` | 预留：MySQL CDC 独立仓库（尚未初始化；本仓 `services/cdc-mysql` 为 Go 重写起点） |

## 目录结构

```
nexa/
├── apps/
│   └── mobile/                 # 掌上企业（Flutter 数据相关页，从 ltoa/app 抽出）
│       └── lib/
│           ├── pages/          # 数据中心 / 运维监控 / 驾驶舱
│           └── services/       # 对应 API service + 权限
├── services/
│   ├── data-center/            # Go 数据中心导出平台（原 kyx-data-center）
│   │   ├── cmd/
│   │   ├── internal/
│   │   ├── templates/          # 业务导出模板 JSON
│   │   └── legacy-agent/       # 原 agent 侧 export/dashboard 参考实现
│   └── cdc-mysql/              # Go 重写的 MySQL CDC（canal 替代方案骨架）
├── integrations/
│   └── dingtalk/               # 企业钉钉集成（Java 源 + Vue + SQL）
│       ├── java/               # OpenAPI client / sync services / stream
│       ├── java-api/           # RPC API 接口
│       ├── java-api-impl/
│       ├── controller/         # 管理端 API
│       ├── dal/                # DO + Mapper
│       ├── config/
│       ├── attendance/
│       ├── frontend/           # OA 管理页 + 登录入口
│       └── sql/
├── docs/
│   ├── architecture/           # 总架构
│   ├── design/                 # 数据交付 / 看板方案
│   └── hr/                     # 钉钉同步架构
├── sql/                        # 跨服务公共 SQL
└── scripts/
```

## 能力矩阵

| 能力 | 来源（ltoa） | 当前位置 | 状态 |
|------|--------------|----------|------|
| 数据中心明细导出 | `backend/kyx-data-center` | `services/data-center` | 已复制，可独立编译 |
| App 数据中心 / 运维 / 驾驶舱 | `app/lib/pages|services` | `apps/mobile/lib/...` | 页面/服务已抽出（依赖宿主 App 壳） |
| 企业钉钉同步（通讯录/花名册/考勤/审批） | `kyx-service-hr/.../dingtalk` | `integrations/dingtalk` | Java 源 + SQL + 前端已抽出 |
| 灵活取数 / ODS / ADS 方案 | `docs/design/*` | `docs/design` | 文档已迁入 |
| MySQL CDC（binlog → warehouse） | canal 运行态 + 方案文档 | `services/cdc-mysql` | **Go 重写骨架**（替代 canal 三件套） |
| Agent 导出 / 看板 | `kyx-service-agent` | `services/data-center/legacy-agent` | 参考代码 |

## 架构（目标）

```
┌─────────────────────────────────────────────────────────────┐
│  掌上企业 App / OA / Agent / BI                              │
└───────────────┬───────────────────┬─────────────────────────┘
                │                   │
        ┌───────▼───────┐   ┌───────▼────────┐
        │ data-center   │   │ dingtalk       │
        │ (Go 导出/API) │   │ (同步/通知)    │
        └───────┬───────┘   └───────┬────────┘
                │                   │
        ┌───────▼───────────────────▼────────┐
        │           warehouse (MySQL ODS/ADS) │
        └───────▲────────────────────────────┘
                │ upsert / stream
        ┌───────┴───────┐
        │  cdc-mysql    │  ← Go 重写（读源库 binlog）
        │  (nexa-cdc)   │
        └───────┬───────┘
                │ binlog ROW
        ┌───────▼───────┐
        │ ordersys 等源库│
        └───────────────┘
```

三层数据（详见 `docs/design/flexible-data-delivery-plan.md`）：

1. **L3 ADS** — 预聚合 KPI（秒级）
2. **L2 ODS** — CDC 同步明细仓（灵活探索 / 导出）
3. **L1 源库** — 仅点查兜底

## 快速开始

### 1. 数据中心（Go）

```bash
cd services/data-center
cp config.yaml.example config.yaml   # 改 DSN / 路径
go mod tidy
go run ./cmd/kyx-data-center -config ./config.yaml
```

默认健康检查：`GET /actuator/health`  
业务路由需网关透传登录用户，并具备 `app:data-center:use` 权限点。

### 2. CDC MySQL（Go 骨架）

```bash
cd services/cdc-mysql
cp configs/config.example.yaml configs/config.yaml
go mod tidy
go run ./cmd/nexa-cdc-mysql -config ./configs/config.yaml
```

当前为可运行骨架：配置加载 + 生命周期 + 表过滤 + 位点存储接口。  
binlog dump / row 解析 / sink 到 warehouse 为下一阶段实现。

### 3. 钉钉集成

`integrations/dingtalk` 保留原 Java 包路径与 SQL，便于对照重写或作为 library 迁回。  
架构说明：`docs/hr/hr-dingtalk-sync-architecture.md`。

## 源码出处

从 `E:\code\ltoa` 抽取，对应主要 commit 线：

- `feat(data-center+ops+perms)` 数据中心明细导出平台
- HR 钉钉花名册 / 通讯录同步
- 灵活数据交付方案文档

`ltoa` 原仓继续服务业务；本仓用于独立演进与 Go 化。

## 许可

内部项目，未开源。
