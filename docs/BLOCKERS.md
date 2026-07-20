# BLOCKERS

| 日期 | 问题 | 绕过 |
|------|------|------|
| 2026-07-20 | `proxy.golang.org` 超时 | 骨架服务改 **stdlib JSON 配置**，零第三方依赖可 `go build`；需要依赖时用 `GOPROXY=https://goproxy.cn,direct` |
| 2026-07-20 | `@mk-co/neox-sdk` 可能私有 registry | agent 代码已就位；安装需配置 npm 源；可用 `AGENT_USE_MOCK=true` 先起 HTTP |
| 2026-07-20 | 全量 admin/java 抽取曾超时 | **不**以 Java 为运行时；legacy 部分已有；优先 Go 重写 |

更新规则：遇到阻塞记一行，换路径继续，不阻塞整体 GOAL。
