# 掌上企业 · 数据相关页面（从 ltoa/app 抽出）

Flutter 源文件快照，**不能单独 `flutter run`**，需挂回 App 壳（路由、鉴权、主题、网络层）。

## 页面

| 文件 | 说明 |
|------|------|
| `pages/data_center_page.dart` | 数据中心：模板列表 / 筛选 / 导出任务 |
| `pages/ops_monitor_page.dart` | 运维监控 v2 |
| `pages/executive_cockpit_page.dart` | 经营驾驶舱 |

## 服务

| 文件 | 说明 |
|------|------|
| `services/data_center_service.dart` | 调 data-center / gateway |
| `services/ops_service.dart` | 运维指标 |
| `services/executive_cockpit_service.dart` | 驾驶舱聚合 |
| `services/permissions_service.dart` | 权限点 |
| `services/user_permission_service.dart` | 用户权限缓存 |

## 权限点

- `app:data-center:use` — 数据中心导出（与 Go 服务一致）

## 后端

- 导出：`services/data-center`（Go）
- 看板部分历史实现参考：`services/data-center/legacy-agent/dashboardEndpoints.js`
