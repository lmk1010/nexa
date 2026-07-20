# nexa

开源通用企业协作与数据平台。

**钉钉 Agent + 掌上企业 App + 后端业务**：组织人事、审批协同、数据中心、运维与数据分析等，能力覆盖接近传统 OA。

## 技术选型（方案 A）

| 层 | 技术 | 说明 |
|----|------|------|
| **Agent（智能面）** | **Node.js 20+ + `@mk-co/neox-sdk`** | 对话循环、tool calling、stream；**不重写内核** |
| **数据 / CDC / 导出** | **Go** | `data-center`、`cdc-mysql` |
| **业务 API** | **Go（演进）** | 从内部 Java OA 能力重写，不用 Java 作 nexa 运行时 |
| **掌上客户端** | **Flutter** | `apps/mobile` |
| **对照** | `legacy/` | 旧 Node 副本、Java 钉钉、废弃 Go agent 占位 |

融合方式：Agent tools **HTTP 调用** Go 服务与 warehouse，而不是同进程绑死。

## 仓库

| 仓库 | 说明 |
|------|------|
| [`lmk1010/nexa`](https://github.com/lmk1010/nexa) | 本 monorepo |
| [`lmk1010/nexa-cdc-mysql`](https://github.com/lmk1010/nexa-cdc-mysql) | MySQL CDC 独立仓（与 `services/cdc-mysql` 同构） |

## 目录

```
nexa/
├── apps/mobile/                 # 掌上企业 Flutter App
├── services/
│   ├── agent/                   # NeoX Agent（Node）★ 智能入口
│   ├── data-center/             # 数据中心导出（Go）
│   └── cdc-mysql/               # MySQL→warehouse CDC（Go）
├── integrations/dingtalk/       # 钉钉 SQL + 前端参考
├── docs/
├── legacy/                      # 对照实现（非默认运行路径）
├── sql/
└── scripts/
```

## 能力

| 能力 | 路径 | 运行时 | 状态 |
|------|------|--------|------|
| 企业钉钉 / 对话 Agent | `services/agent` | Node + NeoX | 主路径 |
| 数据中心导出 | `services/data-center` | Go | 已迁入 |
| MySQL CDC | `services/cdc-mysql` | Go | 与独立仓同步 |
| 掌上企业 | `apps/mobile` | Flutter | 完整工程已迁入 |
| 钉钉产品资产 | `integrations/dingtalk` | — | SQL/前端；同步逻辑 Go 演进 |
| 数据分析方案 | `docs/design` | — | ODS/ADS 文档 |

## 架构

```
钉钉 / 掌上 App / Web / Gateway
              │
              ▼
     services/agent (Node + neox-sdk)
              │ tools (HTTP)
     ┌────────┼──────────────┐
     ▼        ▼              ▼
 data-center  warehouse   业务 API
   (Go 导出)  (ODS/ADS)   (Go 演进)
                 ▲
          cdc-mysql (Go，只读 binlog)
```

详见 `docs/architecture/overview.md`。

## 快速开始

```bash
# Agent（NeoX）
cd services/agent
cp .env.example .env
npm install
AGENT_USE_MOCK=true npm run dev
# http://localhost:48091/health

# 数据中心（Go）
export PATH="/e/tools/go/bin:$PATH"   # 若本机 Go 在此
cd services/data-center
cp config.yaml.example config.yaml
go mod tidy && go run ./cmd/kyx-data-center -config ./config.yaml

# CDC（Go）
cd services/cdc-mysql
cp config.example.yaml config.yaml
go mod tidy && go run ./cmd/nexa-cdc -c config.yaml

# 掌上 App
cd apps/mobile && flutter pub get && flutter run
```

## 来源与原则

- Agent：**融合 NeoX（Node）**，tools 对接 Go 数据面  
- 数据面 / 业务面：**Go**；不用 Java 作 nexa 运行时  
- `legacy/` 只作对照  
- 密钥不进仓  

抽出对照：`docs/SOURCE_MAP.md`。

## License

待定（CDC 子仓为 MIT；整体许可随后续发布选定）。
