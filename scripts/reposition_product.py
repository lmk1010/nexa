from pathlib import Path

# GOAL header rewrite
goal = Path("E:/code/nexa/docs/GOAL.md")
t = goal.read_text(encoding="utf-8")
header = """# nexa GOAL — 企业钉钉（可接入协作平台）

> 用户指令（持续有效）：
> 1. **nexa 就是企业钉钉本体**，不是「旧 OA 对接钉钉数据源」。
> 2. **支持用户/企业接入开通**（多租户产品）。
> 3. 领域能力 **Go 重写**；Agent = **NeoX(Node)**；goal 驱动自主推进。
> 4. 产品定义见 `docs/PRODUCT.md`。

## 一句话

**nexa = 可接入的企业钉钉**：组织通讯录 · 审批 · 待办 · IM · 工作台 · 数据中心 · 企业 Agent。
外部业务系统通过 **连接器** 接入 nexa，而不是 nexa 去当旧 OA 的附属同步器。

## 终态架构

```
企业用户 ──注册/登录租户──► nexa（产品本体）
                               │
                    ┌──────────┼──────────┐
                    ▼          ▼          ▼
                 组织成员    审批/待办    IM/工作台
                    └──────────┬──────────┘
                               ▼
                      Agent（NeoX）企业大脑
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
         nexa 域服务      数据中心/CDC     外部连接器
         (Go 本体)        (经营分析)     (业务库/API 可选)
```

## 硬性约束

| # | 约束 |
|---|------|
| 1 | **产品定位**：协作平台本体，不是 OA→钉钉同步插件 |
| 2 | **运行时无 Java**；Java 只许 `legacy/**` 对照 |
| 3 | **Agent = Node + NeoX**，tools 调 nexa Go 服务 |
| 4 | **多租户可接入**：注册/邀请/成员/权限为产品主路径 |
| 5 | 钉钉 OpenAPI = **可选导入连接器**，不是主数据定义 |
| 6 | 密钥不进仓；大查询走 warehouse/data-center |

"""
idx = t.find("## 里程碑")
if idx < 0:
    idx = t.find("### M0")
if idx > 0:
    t = header + "\n" + t[idx:]
else:
    t = header + t

if "M9" not in t:
    t = t.replace(
        "### M8 — AI-Native",
        """### M9 — 租户接入（产品主路径） 🔄
- [x] 企业注册 / 创建租户 API
- [x] 邀请成员 / 接受邀请 API
- [x] 开通状态 checklist
- [ ] 租户内角色权限默认模板增强
- [ ] 开通向导前端/App

### M8 — AI-Native""",
    )

if "PRODUCT.md 定位纠正" not in t:
    t += "\n| 2026-07-20 | PRODUCT.md 定位纠正：nexa=可接入企业钉钉本体；租户注册/邀请 API |\n"

t = t.replace(
    "| 钉钉 | hr + agent | 登录、同步、机器人 | sync skeleton + 资产 |",
    "| 协作本体 | iam/hr/bpm/im/business | 组织/审批/IM/待办（钉钉类能力） | 主路径建设中 |\n| 连接器-钉钉导入 | hr dingtalk | 可选通讯录导入 | openapi client |\n| 连接器-业务库 | cdc/data-center | 外部经营数据 | 已有 |",
)
goal.write_text(t, encoding="utf-8")
print("goal ok")

# README
readme = Path("E:/code/nexa/README.md")
r = readme.read_text(encoding="utf-8")
if "## 服务一览" in r:
    tail = r[r.find("## 服务一览") :]
else:
    tail = r
readme.write_text(
    """# nexa

**可接入的企业钉钉**（开源通用企业协作平台）。

> 定位：**[docs/PRODUCT.md](docs/PRODUCT.md)**
> 里程碑：**[docs/GOAL.md](docs/GOAL.md)**
> AI：**[docs/architecture/ai-native.md](docs/architecture/ai-native.md)**

**nexa 不是「旧 OA 对接钉钉」**。
nexa 本身就是组织、审批、待办、IM、工作台与企业 Agent；企业可 **注册接入**；外部业务系统用连接器挂进来。

## AI-Native

- 控制面：`services/ai`（skills · intent · sense · automation）
- Agent：`services/agent`（NeoX）+ `prompts/enterprise-assistant.md`

## 原则

1. **产品本体 = 企业钉钉类协作平台**（多租户可接入）
2. **领域服务全部 Go**，不用 Java 运行时
3. **Agent = Node + NeoX**，tools 调 nexa
4. 钉钉 OpenAPI / 业务库 = **可选连接器**
5. `legacy/**` 仅对照

"""
    + tail,
    encoding="utf-8",
)
print("readme ok")

Path("E:/code/nexa/services/agent/prompts/enterprise-assistant.md").write_text(
    """# Nexa 企业钉钉助手

你是 **Nexa** 的企业助手。Nexa 是 **企业协作平台本体**（企业钉钉），不是旧 OA 的钉钉同步插件。

## 你服务的产品

- 组织与成员、审批、待办、日历、IM、工作台
- 数据中心 / 经营分析（连接器接入的业务数据）
- 企业用户在自己的 **租户** 内使用上述能力

## 工作方式（必须）

1. **先路由技能**：`nexa_intent_route` 或 `GET /v1/ai/skills`
2. **再调用 nexa 网关** API（用户 Bearer token）
3. 写操作（审批、建待办、邀请成员）先确认风险
4. 外部业务数据走 data-center / warehouse 连接器能力
5. 无权限就说明，不编造

## 表述禁忌

- 不要说「我帮你对接钉钉 OA」
- 不要把 Nexa 描述成旁路数据同步工具
- 正确：Nexa 就是企业协作与智能助手平台；钉钉 OpenAPI 仅是可选导入源

## 开通/接入相关

- 注册企业：`POST /v1/iam/tenants/register`
- 邀请成员：`POST /v1/iam/invites`
- 接受邀请：`POST /v1/iam/invites/accept`
- 开通状态：`GET /v1/iam/onboarding/status`

## 覆盖技能域

IAM 租户成员 · BPM 审批 · HR 组织 · Business 待办/接待 · ERP/Finance 连接数据 · IM · OP · AI 感知自动化 · 数据导出
""",
    encoding="utf-8",
)

Path("E:/code/nexa/integrations/dingtalk/README.md").write_text(
    """# 钉钉连接器（可选）

在 nexa 产品中，**钉钉 OpenAPI 是可选连接器**，用于从已有钉钉企业导入部门/成员。

**不是** nexa 的产品定义，也不是主数据权威。

主数据与协作能力在 nexa 本体：`services/iam` `hr` `bpm` `im` …

运行时 Go 客户端：`services/hr/internal/dingtalk`
环境变量：`NEXA_DINGTALK_APP_KEY` / `NEXA_DINGTALK_APP_SECRET`
""",
    encoding="utf-8",
)

Path("E:/code/nexa/docs/AUTONOMOUS.md").write_text(
    """# Autonomous execution log

## Product truth
nexa = **enterprise DingTalk product** (multi-tenant, joinable).
NOT legacy OA syncing DingTalk as a datasource.

## Latest
- docs/PRODUCT.md positioning correction
- Tenant register / invite / accept / onboarding status APIs
- Agent prompt + GOAL/README aligned

## Next
1. Tenant-aware default data isolation across domains
2. First-class org/approval/IM product APIs polish
3. Connector framework naming
4. App onboarding screens
""",
    encoding="utf-8",
)

ov = Path("E:/code/nexa/docs/architecture/overview.md")
o = ov.read_text(encoding="utf-8")
if "可接入的企业钉钉" not in o:
    if "## 方案 A" in o:
        o = (
            """# nexa 架构总览

## 定位

**nexa = 可接入的企业钉钉（协作平台本体）**，不是「传统 OA 对接钉钉数据源」。

详见 [PRODUCT.md](../PRODUCT.md)。

"""
            + o[o.find("## 方案 A") :]
        )
    ov.write_text(o, encoding="utf-8")
print("docs done")
