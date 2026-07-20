# legacy/

旧实现**仅作能力对照**，不是 nexa 运行依赖，也不作为推荐集成路径。

| 目录 | 来源 | 说明 |
|------|------|------|
| `agent-node/` | ltoa `kyx-service-agent` | Node Agent：工具、导出、看板、权限 |
| `dingtalk-java/` | ltoa `kyx-service-hr` 钉钉相关 | Java 同步/通知/API/DAL |

nexa 目标栈：

- Agent / 业务 / 数据中心 / CDC → **Go**
- 掌上客户端 → **Flutter**（`apps/mobile`）
- 钉钉产品资产 → `integrations/dingtalk`（sql + frontend 参考）

重写完成后可删除本目录或移出默认发布物。
