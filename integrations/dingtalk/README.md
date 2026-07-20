# 企业钉钉集成（从 ltoa/kyx-service-hr 抽出）

原包路径：`com.kyx.service.hr.integration.dingtalk` 等。  
此处按职责分目录存放，**保留原 Java 文件内容**，方便对照重写或迁回 monorepo 模块。

## 目录

| 目录 | 原位置 | 内容 |
|------|--------|------|
| `java/` | `.../integration/dingtalk` | client / job / listener / model / service |
| `java-api/` | `hr-api/.../api/dingtalk` | RPC 接口 + DTO |
| `java-api-impl/` | `hr-core/.../api/dingtalk` | RPC 实现 |
| `controller/` | `.../controller/admin/integration` | 管理端 API + VO |
| `dal/` | `.../dal/.../integration` | DO + Mapper |
| `config/` | `.../config/DingTalkProperties` | 配置项 |
| `attendance/` | `.../service/attendance/dingtalk` | 考勤 access token |
| `frontend/` | `web-antd` HR 钉钉页 + 登录入口 | Vue / TS |
| `sql/` | `backend/sql/business/hr-dingtalk-*` | 表结构 / 菜单 / 清理脚本 |

## 能力清单

- 通讯录 / 部门 / 员工同步
- 花名册同步
- 考勤 / 请假同步
- 用户绑定（含移动端）
- Stream 事件监听
- 审批状态 Kafka 监听
- 审批 / 需求 / 系统更新通知
- 同步历史与快照
- 定时同步 Job

架构文档：`docs/hr/hr-dingtalk-sync-architecture.md`

## 使用说明

当前为 **源码快照**，未改 package / 依赖，不能单独编译。后续可选：

1. 继续作为 ltoa HR 子模块引用  
2. 抽成独立 Java module  
3. 用 Go 按同样能力重写（与 data-center / cdc 统一）  
