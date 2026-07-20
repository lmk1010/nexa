# nexa-cdc-mysql

Go 重写的 **MySQL CDC** 服务，用于替代原先 canal 三件套，将源库 ROW binlog 实时同步到 `warehouse` ODS。

> 本目录是 monorepo 内实现；独立仓目标：`git@github.com:lmk1010/nexa-cdc-mysql.git`（当前远程尚未初始化）。

## 目标能力

| 能力 | 说明 | 状态 |
|------|------|------|
| binlog dump | 伪装 slave 拉取 ROW 事件 | 骨架 / TODO |
| 表过滤 | 按 schema.table 白名单 | ✅ |
| 位点持久化 | file:pos / GTID | 接口 + MySQL DDL |
| sink | upsert 到 warehouse | 接口骨架 |
| 指标 | lag / events / errors | stub |
| 全量 backfill | 一次性灌数 | 规划中 |

## 架构

```
source MySQL (binlog ROW, server-id unique)
        │
        ▼
  binlog.Dumper  ──► parser.RowEvent ──► filter ──► sink.Writer
        │                                              │
        └── position.Store ◄────────────────────────────┘
```

设计约束（与 `docs/design/flexible-data-delivery-plan.md` 一致）：

- **只读 binlog**，不在热路径 SELECT 业务表
- sink 幂等 upsert；短暂断连可追赶
- 超过长时间断连走 mysqldump / 受控 backfill

## 配置

```bash
cp configs/config.example.yaml configs/config.yaml
# 编辑 source / sink / include_tables
```

## 运行

```bash
go mod tidy
go run ./cmd/nexa-cdc-mysql -config ./configs/config.yaml
```

健康检查：`GET /healthz`

## 与 canal 对照

| canal | nexa-cdc-mysql |
|-------|----------------|
| canal-server | `internal/binlog` dumper |
| canal-adapter / MQ | 可选后续；默认直写 sink |
| instance.toml 表过滤 | `include_tables` |
| meta 位点 | `internal/store` |

## 目录

```
cmd/nexa-cdc-mysql/     入口
internal/config/        配置
internal/binlog/        dump 客户端（TODO）
internal/parser/        事件模型
internal/filter/        表白名单
internal/sink/          warehouse writer
internal/store/         位点
internal/metrics/       指标
configs/                示例配置
```

## 下一步

1. 接入 `go-mysql-org/go-mysql` 完成 dump + row 解析  
2. 实现 MySQL sink：按主键 upsert / delete  
3. 位点表 + 优雅退出刷盘  
4. 对齐 ordersys 核心表清单（见 design 文档）  
5. 推送到 `lmk1010/nexa-cdc-mysql` 独立仓  
