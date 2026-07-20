# legacy/java

**只读对照**，从内部 ltoa Java 服务抽出的源码快照。

## 规则

- **禁止**作为 nexa 生产运行时启动  
- **禁止**在文档中写 “部署此 Java 服务”  
- Go 重写时打开对应目录对照 API/表结构/业务规则  

## 目录

| 目录 | 原服务 |
|------|--------|
| iam / bpm / hr / business / erp / finance / im / op / ai / gateway | kyx-service-* |
| foundation | kyx-foundation |

nexa 运行时见 `services/*`（Go）与 `services/agent`（Node+NeoX）。
