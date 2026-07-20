# 企业钉钉（产品侧资产）

nexa 的钉钉能力：**登录入口、同步管理、SQL 模型**。  
运行时实现目标为 **Go**（Agent 通道 + 同步服务），不用 Java。

## 本目录

| 路径 | 说明 |
|------|------|
| `frontend/` | OA 管理页、钉钉登录 / 移动入口（Vue/TS 参考） |
| `sql/` | 绑定、同步历史、快照、花名册、菜单等 DDL |

## 能力（产品）

- 通讯录 / 部门 / 员工 / 花名册同步
- 考勤 / 请假
- 用户绑定（含移动端）
- Stream / 审批通知
- 与 Agent 对话入口联动

## 对照与重写

- Java 旧实现：`legacy/dingtalk-java/`（只读参考）
- Agent 侧钉钉通道：`services/agent`（Go）
- 架构文档：`docs/hr/hr-dingtalk-sync-architecture.md`
