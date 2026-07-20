# API 速查（Go 企业助手）

统一入口：`http://127.0.0.1:48080`（gateway）

## 平台

| Method | Path | Service |
|--------|------|---------|
| GET | `/healthz` | gateway |
| GET | `/v1/platform/services` | gateway |

## IAM

| Method | Path | 说明 |
|--------|------|------|
| POST | `/v1/iam/login` | demo users: `admin`, `boss` |
| GET | `/v1/iam/me` | Bearer token |
| GET | `/v1/iam/permissions` | 权限点 |

## HR / BPM

| Method | Path |
|--------|------|
| GET | `/v1/hr/employees` |
| GET | `/v1/hr/departments/tree` |
| GET | `/v1/bpm/tasks/todo` |
| POST | `/v1/bpm/tasks/approve` |

## Business

| Method | Path |
|--------|------|
| GET/POST | `/v1/business/todos` |
| GET | `/v1/business/work-requirements` |
| GET | `/v1/business/calendar/events` |
| GET | `/v1/business/reception/latest` |

## ERP / Finance / IM

| Method | Path |
|--------|------|
| GET | `/v1/erp/stock/summary` |
| GET | `/v1/erp/purchase/orders` |
| GET | `/v1/finance/summary/monthly` |
| GET | `/v1/finance/ledger/recent` |
| GET | `/v1/im/conversations` |
| GET | `/v1/im/contacts` |

## OP / AI

| Method | Path |
|--------|------|
| GET | `/v1/op/status` |
| GET | `/v1/op/audit/recent` |
| GET | `/v1/ai/models` |
| GET | `/v1/ai/reception/config` |
| GET | `/v1/ai/reception/records` |

Agent 白名单：`services/agent/curated/nexa-go-apis.json`  
本地启动：`scripts/start-dev.sh` / `scripts/stop-dev.sh`


## 持久化（dev）

IAM/HR/BPM 默认 `dataDir=./data`（或 `NEXA_DATA_DIR` / config `dataDir`），JSON 文件：

- `data/iam.json`
- `data/hr.json`
- `data/bpm.json`

### 钉钉

| Method | Path | 说明 |
|--------|------|------|
| POST | `/v1/hr/dingtalk/sync` | `{"mode":"full|directory|roster"}` 模拟同步 |
| GET | `/v1/hr/dingtalk/jobs` | 同步任务历史 |
| GET | `/v1/hr/dingtalk/status` | 当前状态 |

### IAM 扩展

| Method | Path |
|--------|------|
| GET | `/v1/iam/users` |
| POST | `/v1/iam/token/introspect` |
| POST | `/v1/iam/logout` |

### BPM 扩展

| Method | Path |
|--------|------|
| GET | `/v1/bpm/tasks/done` |
| POST | `/v1/bpm/tasks/start` |
