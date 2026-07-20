# nexa AI-Native Enterprise Architecture

> Enterprise assistant is not "chat on top of OA".  
> **OA capabilities are designed as AI-addressable skills** with perception and automation loops.

## Layers

```
┌─────────────────────────────────────────────────────────────┐
│ Channels: DingTalk / Mobile / Admin / OpenAPI                │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Gateway (Go)  auth · route · AI edge hints · rate limit      │
└───────────────┬─────────────────────────────┬───────────────┘
                ▼                             ▼
┌───────────────────────────┐   ┌─────────────────────────────┐
│ Agent (NeoX Node)         │   │ AI Control Plane (Go)       │
│ dialog · tool loop        │◄─►│ skills · intent · sense ·   │
│ enterprise system prompt  │   │ automation rules · memory   │
└─────────────┬─────────────┘   └──────────────┬──────────────┘
              │ tools/HTTP                     │
              ▼                                ▼
     Domain services (Go): iam bpm hr business erp finance im op
     Data plane: data-center · cdc-mysql · warehouse
```

## AI design principles for every OA capability

| Principle | Meaning |
|-----------|---------|
| **Skill-first** | Each capability exposes a stable skill id + JSON schema, not only REST |
| **Perception** | Domain events (attendance, approval, reception, stock, risk) become sense signals |
| **Automation** | Rules: `when sense → then skill/action` without waiting for a human prompt |
| **Explainable** | Agent answers cite skill id + API path + data snapshot summary |
| **Least privilege** | Skills inherit IAM permissions; gateway enforces token |
| **Safe data plane** | Analytics/export via warehouse/data-center, never raw OLTP scans |

## Skill catalog (enterprise)

| Skill ID | Domain | Example NL |
|----------|--------|------------|
| `iam.whoami` | iam | 我是谁 / 我有哪些权限 |
| `hr.employees.search` | hr | 查花名册 / 找某人 |
| `hr.dingtalk.sync` | hr | 同步钉钉通讯录 |
| `bpm.todo.list` | bpm | 我的待办审批 |
| `bpm.task.approve` | bpm | 通过/驳回某单 |
| `biz.todo.list` | business | 我的待办事项 |
| `biz.calendar.today` | business | 今天有什么会 |
| `erp.stock.summary` | erp | 库存概况 |
| `finance.month.summary` | finance | 本月收支 |
| `op.health` | op | 系统是否正常 |
| `ai.reception.latest` | ai | 前台最近访客 |
| `data.export` | data-center | 导出赔付/订单明细 |

## Perception (sense)

Events ingested by `services/ai`:

- `bpm.task.created` / `bpm.task.overdue`
- `hr.employee.joined` / `hr.dingtalk.sync.finished`
- `biz.reception.detected`
- `erp.stock.low`
- `op.service.down`

Stored as sense log; automation engine matches rules.

## Automation examples

1. **审批催办**：sense `bpm.task.overdue` → notify assignee via IM skill  
2. **入职欢迎**：sense `hr.employee.joined` → create onboarding todos  
3. **库存预警**：sense `erp.stock.low` → create work requirement + notify ops  
4. **前台访客**：sense `biz.reception.detected` → AI summary to host  

## Gateway AI edge

- `GET /v1/ai/skills` — skill catalog (proxied)
- `POST /v1/ai/intent/route` — NL → ranked skills (no LLM required for baseline)
- `POST /v1/ai/sense` — push perception events
- `GET /v1/ai/automation/rules` — list rules
- Agent remains public path; domain routes stay auth-protected

## Implementation status

See `docs/GOAL.md` milestone **M8 AI-Native**.
