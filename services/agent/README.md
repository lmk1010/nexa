# nexa Agent（Node + NeoX）

nexa 的**一等 Agent 服务**：`@mk-co/neox-sdk` 驱动对话循环，业务 tools 调 **Go** 子系统（data-center / warehouse / 业务 API）。

> 选型（方案 A）：**不**用 Go 重写 agent 内核；智能面继续 NeoX，数据面 Go。

## 架构

```
钉钉 / 掌上 App / Web / Gateway
              │
              ▼
     services/agent  (Node 20+, this package)
       ├── @mk-co/neox-sdk   Agent.run / Agent.stream + tools
       └── tools ──HTTP──► services/data-center (Go)
                        ► warehouse MySQL (RO)
                        ► OA / ordersys 只读 API
                        ► MinIO 导出文件
```

## 依赖

| 包 | 用途 |
|----|------|
| `@mk-co/neox-sdk` | Agent loop、tool calling、stream |
| `zod` | tool schema |
| `mysql2` / `exceljs` / `minio` | 导出与存储（可逐步迁到 data-center） |

需 **Node.js ≥ 20**。

## 本地运行

```bash
cd services/agent
cp .env.example .env   # 按需改
npm install
AGENT_USE_MOCK=true npm run dev
```

```bash
curl http://localhost:48091/health
curl -X POST http://localhost:48091/run \
  -H 'Content-Type: application/json' \
  -d '{"message":"ping"}'
```

## 与 Go 服务融合

| 环境变量 / 配置 | 指向 |
|-----------------|------|
| data-center base URL | `services/data-center`（默认 `:48092`） |
| warehouse DSN | CDC 灌入的 ODS（只读账号） |
| OA / ordersys base | 网关只读 API（tool_search → oa_read / ordersys_read） |

工具约定（docs-first）：

1. `tool_search` — 查 API 索引  
2. `oa_read` / `ordersys_read` — 执行白名单 GET  
3. 导出 — 优先走 **data-center**（Go），本服务可保留 legacy export 作过渡  

Gateway 透传：`login-user`、`tenant-id`、用户 `Authorization`。

## 脚本

```bash
npm run dev                  # watch
npm start                    # 生产
npm test
npm run generate:api-index   # 从控制器生成只读 API 索引（需源码路径配置）
```

## 目录

```
src/                 服务入口、neoxAgent、tools、HTTP
curated/             API 索引 curated 覆盖
templates/           导出模板（若仍走本进程）
scripts/             api-index 生成等
test/
Dockerfile
```

## 非目标

- 在本服务内实现 binlog CDC（见 `services/cdc-mysql`）
- 用 Go 替换 NeoX runtime（见 `legacy/agent-go-skeleton` 仅历史占位）
- 直连生产 OLTP 做大查询（走 warehouse / data-center）
