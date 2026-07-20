# 钉钉连接器（可选）

在 nexa 产品中，**钉钉 OpenAPI 是可选连接器**，用于从已有钉钉企业导入部门/成员。

**不是** nexa 的产品定义，也不是主数据权威。

主数据与协作能力在 nexa 本体：`services/iam` `hr` `bpm` `im` …

运行时 Go 客户端：`services/hr/internal/dingtalk`
环境变量：`NEXA_DINGTALK_APP_KEY` / `NEXA_DINGTALK_APP_SECRET`
