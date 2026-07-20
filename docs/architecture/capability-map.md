# nexa 企业助手 · 能力集成图

Agent（NeoX）是**唯一对话入口**；所有企业能力通过 **gateway → Go 服务** 暴露，tools 只调白名单 API。

```
                    ┌─────────────────────┐
                    │  钉钉机器人 / 掌上App  │
                    │  Web Admin / 开放API  │
                    └──────────┬──────────┘
                               ▼
                    ┌─────────────────────┐
                    │  gateway :48080     │
                    │  auth · route · lim │
                    └──────────┬──────────┘
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
    agent :48091         iam :48081          其它 Go 服务
    (NeoX tools)         bpm hr business
           │             erp finance im
           │             op ai data-center
           └────────────► warehouse(RO) ◄── cdc-mysql
```

## 端口表

| 服务 | 端口 | 运行时 |
|------|------|--------|
| gateway | 48080 | Go |
| iam | 48081 | Go |
| bpm | 48082 | Go |
| hr | 48083 | Go |
| business | 48084 | Go |
| erp | 48085 | Go |
| finance | 48086 | Go |
| im | 48087 | Go |
| op | 48088 | Go |
| ai | 48089 | Go |
| agent | 48091 | Node+NeoX |
| data-center | 48092 | Go |
| cdc-mysql | 6060 | Go |

## Agent tool → 后端映射（目标）

| Tool / 意图 | 后端 |
|-------------|------|
| 身份/权限/组织 | `iam` |
| 审批待办/动作 | `bpm` |
| 员工/考勤/花名册 | `hr` |
| 待办/工作要求/接待 | `business` |
| 库存/采购 | `erp` |
| 财务查询 | `finance` |
| 会话消息 | `im` |
| 运维状态 | `op` + agent ops* |
| 智能接待/ASR | `ai` |
| 明细导出 | `data-center` |
| 灵活分析 SQL | warehouse via agent 受控工具 |
| 钉钉事件/同步 | `hr` dingtalk 模块 + agent 通道 |

## 网关路径约定（目标）

```
/app-api/**     → 掌上 / 钉钉
/admin-api/**   → 管理端
/agent/**       → agent 反代（/run /stream）
/internal/**    → 服务间
```

领域服务自身可先用：

```
/healthz
/v1/<domain>/...
```

gateway 再映射到旧路径语义以兼容 App。

## 数据

- **写路径**：各 Go 服务 → 业务库  
- **分析路径**：cdc-mysql → warehouse → data-center / agent  
- **禁止**：agent 无限制扫生产 OLTP  

详见 `docs/GOAL.md`。
