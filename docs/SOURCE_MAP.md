# 抽出清单（ltoa → nexa）

生成日期：2026-07-20

## 已复制

### services/data-center
- 源：`ltoa/backend/kyx-data-center/**`
- 含：Go 导出服务、templates、Dockerfile、legacy-agent（exportTool/templateExport/dashboardEndpoints）

### apps/mobile
- 源：`ltoa/app/lib/pages/{data_center,ops_monitor,executive_cockpit}_page.dart`
- 源：`ltoa/app/lib/services/{data_center,ops,executive_cockpit,permissions,user_permission}_service.dart`

### integrations/dingtalk
- Java integration 全量
- API / API impl / Controller / DAL / Config / Attendance token
- 前端：`dingtalk/index.vue`、API ts、登录入口
- SQL：`hr-dingtalk-*.sql` + clean 脚本

### docs
- `flexible-data-delivery-plan.md`
- `ordersys-oa-dashboard-data-plan.md`
- `hr-dingtalk-sync-architecture.md`

### services/cdc-mysql
- **新建** Go 骨架（非 ltoa 直接复制；对应原先 canal 运行态 + 方案文档）

## 未整包迁入（有意）

| 项 | 原因 |
|----|------|
| 完整 Flutter App 壳 | 体量大，与 IM/登录强耦合；只抽数据相关页 |
| kyx-service-hr 全服务 | 只抽钉钉相关；员工主数据仍在 ltoa |
| canal 二进制 / 部署机配置 | 运行态在服务器；本仓用 Go 重写替代 |
| gateway / nginx 路由 | 部署层，后续在 nexa/deploy 补 |

## 独立仓

- `lmk1010/nexa` — 本 monorepo ✅
- `lmk1010/nexa-cdc-mysql` — 远程尚无仓库；代码在 `services/cdc-mysql`，成熟后 `git subtree split` 或单独 push
