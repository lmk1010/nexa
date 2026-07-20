# nexa

**企业全能助手平台**（开源通用）：对话 + 掌上 App + 管理端 + 全领域后端。

> 总目标与里程碑：**[docs/GOAL.md](docs/GOAL.md)**  
> 能力集成图：[docs/architecture/capability-map.md](docs/architecture/capability-map.md)

## 原则

1. **领域服务全部 Go**，不用 Java 作运行时  
2. **Agent = Node + `@mk-co/neox-sdk`（NeoX）**，tools 调 Go  
3. 数据：`cdc-mysql` → warehouse → `data-center` / 分析  
4. `legacy/**` 仅对照  

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
