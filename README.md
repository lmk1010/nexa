# nexa

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

## 服务一览

| 服务 | 目录 | 端口 | 运行时 | 状态 |
|------|------|------|--------|------|
| Gateway | `services/gateway` | 48080 | Go | skeleton |
| IAM | `services/iam` | 48081 | Go | skeleton |
| BPM | `services/bpm` | 48082 | Go | skeleton |
| HR | `services/hr` | 48083 | Go | skeleton |
| Business | `services/business` | 48084 | Go | skeleton |
| ERP | `services/erp` | 48085 | Go | skeleton |
| Finance | `services/finance` | 48086 | Go | skeleton |
| IM | `services/im` | 48087 | Go | skeleton |
| OP | `services/op` | 48088 | Go | skeleton |
| AI | `services/ai` | 48089 | Go | skeleton |
| **Agent** | `services/agent` | 48091 | **Node+NeoX** | 主路径已迁入 |
| Data Center | `services/data-center` | 48092 | Go | 已迁入 |
| CDC MySQL | `services/cdc-mysql` | 6060 | Go | 完整实现（同独立仓） |
| Mobile | `apps/mobile` | — | Flutter | 完整工程 |

## 架构

```
钉钉 / App / Admin → gateway → agent(NeoX) + Go 领域服务
                                      ↘ data-center / warehouse
                                         ▲
                                      cdc-mysql
```

## 快速开始

```bash
export PATH="/e/tools/go/bin:$PATH"

# 任一领域服务
cd services/iam
cp configs/config.example.json configs/config.json
go run ./cmd/nexa-iam -config ./configs/config.json

# Agent
cd services/agent
cp .env.example .env
npm install   # 需 neox-sdk 源
AGENT_USE_MOCK=true npm run dev
```

## 仓库

- Monorepo: `git@github.com:lmk1010/nexa.git`
- CDC 独立仓: `git@github.com:lmk1010/nexa-cdc-mysql.git`

## License

待定（CDC 子仓 MIT）。
