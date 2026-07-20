# 掌上企业 App

从 `ltoa/app` 抽出的 **完整 Flutter 工程**（Android/iOS/桌面/Web 壳 + 业务页）。

在 nexa 中作为开源「掌上企业」客户端：登录、工作台、数据中心、运维监控、驾驶舱、审批、HR 自助等。

## 运行

```bash
cd apps/mobile
flutter pub get
flutter run
```

需配置后端网关 / Agent 地址（见 `lib/config` 与各 service）。

## 与 nexa 后端

| App 能力 | 后端 |
|----------|------|
| 数据中心导出 | `services/data-center` |
| 对话 / 工具取数 | `services/agent` |
| 分析明细 | warehouse（由 `services/cdc-mysql` 灌入） |
| 钉钉登录 / 同步 | `integrations/dingtalk` + agent 钉钉通道 |

## 说明

原工程与内部 OA 网关、IM 等有耦合；开源化时逐步改为 nexa 自有 API 与配置，不依赖 Java 服务。
