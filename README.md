# nexa

开源通用企业协作与数据平台。

**钉钉 Agent + 掌上企业 App + 后端业务**：组织人事、审批协同、数据中心、运维与数据分析等，能力覆盖接近传统 OA。  
实现以 **Go**（Agent / API / 数据中心 / CDC）+ **Flutter**（掌上 App）为主，**不用 Java 作为运行时**。

## 仓库

| 仓库 | 说明 |
|------|------|
| [`lmk1010/nexa`](https://github.com/lmk1010/nexa) | 本 monorepo |
| [`lmk1010/nexa-cdc-mysql`](https://github.com/lmk1010/nexa-cdc-mysql) | MySQL CDC 独立仓（完整 Go 实现；本仓 `services/cdc-mysql` 与其同构） |

## 目录

```
nexa/
├── apps/mobile/                 # 掌上企业 Flutter App（完整工程）
├── services/
│   ├── agent/                   # 钉钉/企业 Agent（Go 骨架）
│   ├── data-center/             # 数据中心导出（Go，可运行）
│   └── cdc-mysql/               # MySQL→warehouse CDC（Go，同 nexa-cdc-mysql）
├── integrations/dingtalk/       # 钉钉 SQL + 前端参考（Go 重写目标）
├── docs/                        # 架构 / 数据方案 / 钉钉架构
├── legacy/                      # Node Agent、Java 钉钉（仅对照，非运行依赖）
├── sql/
└── scripts/
```

## 能力

| 能力 | 路径 | 语言 | 状态 |
|------|------|------|------|
| 企业钉钉 Agent | `services/agent` | Go | 骨架；对照 `legacy/agent-node` |
| 数据中心 | `services/data-center` | Go | 已迁入 |
| 掌上企业 | `apps/mobile` | Flutter | 完整 App 已迁入 |
| 钉钉集成 | `integrations/dingtalk` + legacy | Go 目标 | SQL/前端已抽；Java 仅 legacy |
| MySQL CDC | `services/cdc-mysql` | Go | 与独立仓同步的完整实现 |
| 数据分析方案 | `docs/design` | — | ODS/ADS/导出文档 |

## 架构

```
钉钉 / 掌上 App / Web
        │
        ▼
 services/agent  ──► data-center（导出）
        │        ──► warehouse（分析）
        │        ──► 业务 API（OA 类，Go 演进）
        ▼
   warehouse (ODS/ADS)
        ▲
 services/cdc-mysql  ← 只读业务库 binlog
```

详见 `docs/architecture/overview.md`。

## 快速开始

```bash
# 数据中心
cd services/data-center && cp config.yaml.example config.yaml
go mod tidy && go run ./cmd/kyx-data-center -config ./config.yaml

# CDC（或使用独立仓 nexa-cdc-mysql）
cd services/cdc-mysql && cp config.example.yaml config.yaml
go mod tidy && go run ./cmd/nexa-cdc -c config.yaml

# Agent
cd services/agent && cp configs/config.example.yaml configs/config.yaml
go mod tidy && go run ./cmd/nexa-agent -config ./configs/config.yaml

# 掌上 App
cd apps/mobile && flutter pub get && flutter run
```

## 来源与原则

能力与参考实现抽自内部项目，在 nexa 中按**开源通用产品**重组：

- 运行时 **Go / Flutter**，不用 Java  
- `legacy/` 只作对照  
- 密钥与生产配置不进仓  

抽出对照表：`docs/SOURCE_MAP.md`。

## License

待定（建议 Apache-2.0 或 MIT；CDC 子仓当前为 MIT）。
