# API 速查（Go 企业助手）

统一入口：`http://127.0.0.1:48080`（gateway）

| Method | Path | Service | 说明 |
|--------|------|---------|------|
| GET | `/healthz` | gateway | 网关健康 |
| GET | `/v1/platform/services` | gateway | 路由与 upstream 表 |
| POST | `/v1/iam/login` | iam | `{"username","password"}` demo: admin/boss |
| GET | `/v1/iam/me` | iam | Bearer token |
| GET | `/v1/iam/permissions` | iam | 权限点 |
| GET | `/v1/hr/employees` | hr | 员工列表（demo） |
| GET | `/v1/hr/departments/tree` | hr | 部门树 |
| GET | `/v1/bpm/tasks/todo` | bpm | 待办 |
| POST | `/v1/bpm/tasks/approve` | bpm | `{"taskId","action","reason"}` |

Agent tools 应只访问 gateway，见 `services/agent/curated/nexa-go-apis.json`。
