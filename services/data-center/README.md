> **Reference module** — production uses `services/core` (+ `services/iam`). See `docs/CONVENTIONS.md`.

# kyx-data-center

数据平台中心 —— Go 服务，提供**流式 Excel 导出**能力，直查 warehouse (ordersys_dw)。

## 定位

- **不是 dashboard 服务** —— 那是 kyx-service-agent 干的活
- **是明细导出服务** —— 大表 join、时间过滤、xlsx 流式生成、并发排队、预约

## 架构

```
APP / OA前端 → gateway (login-user透传) → kyx-data-center:48092
                                            ├── HTTP (chi)
                                            ├── Template Registry (JSON /templates/*.json)
                                            ├── Job Queue (in-memory + MySQL 持久化)
                                            ├── Worker Pool (3 goroutine)
                                            ├── xlsx Streamer (excelize StreamWriter)
                                            └── Scheduler (cron for 预约)
                                                    ↓
                                          kyx-warehouse (agent_ro 读)
                                                    ↓
                                          /data/exports/{job_id}.xlsx (24h GC)
```

## 性能目标

| 数据量 | 耗时 | 内存 |
|---|---|---|
| 1w 行 | <2s | <50MB |
| 10w 行 | <30s | <100MB |
| 100w 行 | <5min | <300MB |

**并发**：全局 3 个 worker，>3 任务排队；单用户同时最多 1 个任务。

## API 草案

```
POST   /jobs                  { template, filters }  →  { job_id, queue_pos }
GET    /jobs/:id              → 状态 + rows_written 进度
GET    /jobs/:id/xlsx         → 下载
DELETE /jobs/:id              → 取消
GET    /jobs?owner=me         → 我的任务列表
GET    /admin/jobs            → 全部（需要 admin 角色）
POST   /schedules             → 创建预约（cron）
GET    /templates             → 列出可用模板
```

## 模板 JSON schema (示意)

```json
{
  "id": "order_dispatch_time",
  "label": "下单时间",
  "max_rows": 200000,
  "filters": {
    "date": {"col": "t.create_time", "required": true, "type": "range"},
    "state": {"col": "t.state", "type": "in", "enum": "OrderState"}
  },
  "sql": "SELECT ... FROM t_order t LEFT JOIN sys_seller s ... WHERE {WHERE}",
  "sort": ["t.create_time desc"],
  "columns": [
    {"header": "订单号",  "field": "order_no",  "width": 22},
    {"header": "订单状态","field": "state",     "width": 10, "enum": "OrderState"},
    ...
  ],
  "enums": {
    "OrderState": {"0":"未派单","1":"已派单","2":"已接单","3":"已安装","4":"回访完成","5":"已完结","401":"待打款","-1":"派单已撤单"}
  }
}
```

## 目录结构

```
kyx-data-center/
├── cmd/kyx-data-center/main.go
├── internal/
│   ├── config/       配置加载
│   ├── warehouse/    mysql 连接池
│   ├── auth/         login-user header 解析
│   ├── api/          chi HTTP handlers
│   ├── template/     模板 loader + SQL 组装
│   ├── queue/        任务队列
│   ├── worker/       worker pool
│   ├── writer/       excelize 流式写
│   ├── store/        export_jobs / export_schedule CRUD
│   └── scheduler/    cron 触发预约
├── templates/        JSON 模板（下单时间/回访数据/积分记录/赔付 …）
└── data/exports/     xlsx 输出（24h GC）
```

## 部署

alpine 静态二进制，image <20MB。docker run 挂 `/data/exports` 和 `/etc/kyx-data-center/config.yaml`。
