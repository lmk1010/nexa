# nexa-core

合并后的业务进程（省资源）：一个端口托管网关边缘 + 全部业务域。

- `:48080` nexa-core（gateway + hr/bpm/business/erp/finance/im/op/ai/data-center）
- `:48081` nexa-iam（认证/租户，独立）
- `:48091` nexa-agent（NeoX，独立）
- 可选 CDC

`services/{gateway,hr,bpm,...}` 保留作对照，部署优先 core。
