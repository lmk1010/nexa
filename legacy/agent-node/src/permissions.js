// 用户角色/权限查询 + 60s 缓存
//
// 后端"数据看板"/"运维监控"/"数据中心"三个受限功能的权限判定统一入口。
// 逻辑：
//   - 直接查 kyx_oa 的 system_user_role + system_role + system_role_menu + system_menu
//   - 60s per-user 内存缓存 —— 换来 hasAnyRole/hasPermission 几乎零延迟
//   - 前端登录后 loadOnLogin() 拉一次 /me/permissions 就本地缓存所有，
//     UI 判定 sync 读，避免时序 bug 越权

import mysql from 'mysql2/promise';

const CFG = {
  host: process.env.OA_DB_HOST || 'kyx-mysql-master',
  port: Number.parseInt(process.env.OA_DB_PORT || '3306', 10),
  user: process.env.OA_DB_USER || 'kyx_user',
  password: process.env.OA_DB_PASS || 'kyx123456',
  database: process.env.OA_DB_NAME || 'kyx_oa',
};

// APP 受限功能的 4 个权限点（menu tree: "APP 权限管理" → 4 个 permission）
// 后端只检查权限点，不再看具体角色 code。OA 后台"角色权限管理"里
// 勾谁给谁 = 立刻生效，改权限不用发版。
export const PERM_DASHBOARD_VIEW  = 'app:dashboard:view';   // 数据看板 / 总裁驾驶舱
export const PERM_CHAT_USE        = 'app:chat:use';         // AI 对话
export const PERM_OPS_VIEW        = 'app:ops:view';         // 运维监控
export const PERM_DATA_CENTER_USE = 'app:data-center:use';  // 数据中心导出

// 默认分配（在 OA 的 setup_app_perms 脚本里做的）：
//   biz_boss         → dashboard + chat
//   tenant_admin     → dashboard + chat + ops + data-center
//   tech_maintenance → ops + data-center
// 想给某个非上述角色的人临时开某项 → 直接在 OA 后台勾权限点，无需改代码。

let pool = null;
function getPool() {
  if (!pool) {
    pool = mysql.createPool({
      ...CFG,
      connectionLimit: 3,
      waitForConnections: true,
      connectTimeout: 3000,
      dateStrings: true,
    });
  }
  return pool;
}

// per-user 缓存 TTL —— 15s。缩短是为了配合前端"权限变了 30s 内感知"的目标：
// 前端 30s 轮询 + 后端 15s TTL ⇒ 权限变更最坏 ~45s 生效（不算网络），一般 15-30s
const CACHE_TTL_MS = 15 * 1000;
const cache = new Map();

// 拉取并缓存 —— 一次 SQL 拿角色 + 权限，另一次拿 profile（nickname/username/mobile/dept）
export async function loadUserAccess(userId) {
  if (!userId) return { roles: new Set(), permissions: new Set(), profile: null };
  const uid = String(userId);
  const now = Date.now();
  const hit = cache.get(uid);
  if (hit && (now - hit.at) < CACHE_TTL_MS) {
    return { roles: hit.roles, permissions: hit.permissions, profile: hit.profile };
  }
  const p = getPool();
  const [rows, prof] = await Promise.all([
    p.query(
      `SELECT DISTINCT r.code AS role_code, m.permission AS perm
       FROM system_user_role ur
       JOIN system_role r        ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 0
       LEFT JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = 0
       LEFT JOIN system_menu m       ON m.id = rm.menu_id AND m.deleted = 0 AND m.status = 0
       WHERE ur.user_id = ? AND ur.deleted = 0`,
      [Number(userId)],
    ),
    p.query(
      `SELECT u.id, u.username, u.nickname, u.mobile, u.email, u.avatar,
              u.dept_id, d.name AS dept_name
       FROM system_users u
       LEFT JOIN system_dept d ON d.id = u.dept_id AND d.deleted = 0
       WHERE u.id = ? AND u.deleted = 0 LIMIT 1`,
      [Number(userId)],
    ),
  ]);
  const roles = new Set();
  const permissions = new Set();
  for (const r of rows[0]) {
    if (r.role_code) roles.add(r.role_code);
    if (r.perm) permissions.add(r.perm);
  }
  const p0 = prof[0][0] || null;
  const profile = p0 ? {
    id: p0.id,
    username: p0.username || null,
    nickname: p0.nickname || null,
    mobile: p0.mobile || null,
    email: p0.email || null,
    avatar: p0.avatar || null,
    deptId: p0.dept_id != null ? String(p0.dept_id) : null,
    deptName: p0.dept_name || null,
  } : null;
  cache.set(uid, { at: now, roles, permissions, profile });
  return { roles, permissions, profile };
}

// 命中任一 role code 即返 true
export async function hasAnyRole(context, ...roleCodes) {
  const uid = context?.loginUser?.id ?? context?.loginUser?.userId;
  if (!uid) return false;
  const { roles } = await loadUserAccess(uid);
  for (const c of roleCodes) if (roles.has(c)) return true;
  return false;
}

// 拥有该 permission code 即返 true
export async function hasPermission(context, permCode) {
  const uid = context?.loginUser?.id ?? context?.loginUser?.userId;
  if (!uid) return false;
  const { permissions } = await loadUserAccess(uid);
  return permissions.has(permCode);
}

// 4 个 access helper —— 都走 permission point，一次 SQL 缓存 60s
export async function canAccessDashboard(context) { return hasPermission(context, PERM_DASHBOARD_VIEW); }
export async function canUseChat(context)         { return hasPermission(context, PERM_CHAT_USE); }
export async function canAccessOps(context)       { return hasPermission(context, PERM_OPS_VIEW); }
export async function canUseDataCenter(context)   { return hasPermission(context, PERM_DATA_CENTER_USE); }

// 向后兼容 alias
export async function isDashboardAdmin(context) { return canAccessDashboard(context); }

// 给前端一次拿全的接口用 —— APP 登录后调 /me/permissions 就本地缓存所有
export async function collectMyAccess(context) {
  const uid = context?.loginUser?.id ?? context?.loginUser?.userId;
  const tenantId = context?.loginUser?.tenantId ?? null;
  if (!uid) {
    return {
      userId: null,
      tenantId,
      roles: [],
      permissions: [],
      isDashboardAdmin: false,
      isOpsAdmin: false,
      canUseDataCenter: false,
    };
  }
  const { roles, permissions, profile } = await loadUserAccess(uid);
  const rolesArr = [...roles];
  const permsArr = [...permissions];
  const dashOk       = permissions.has(PERM_DASHBOARD_VIEW);
  const chatOk       = permissions.has(PERM_CHAT_USE);
  const opsOk        = permissions.has(PERM_OPS_VIEW);
  const dataCenterOk = permissions.has(PERM_DATA_CENTER_USE);
  return {
    userId: String(uid),
    tenantId: tenantId != null ? String(tenantId) : null,
    // profile（settings/侧边栏用；避免只显示裸 userId）
    nickname: profile?.nickname || null,
    username: profile?.username || null,
    mobile: profile?.mobile || null,
    email: profile?.email || null,
    avatar: profile?.avatar || null,
    deptId: profile?.deptId || null,
    deptName: profile?.deptName || null,
    roles: rolesArr,
    permissions: permsArr,
    // UI 判权限直接用这 4 个 bool（sync 读，来自 permission point，不是角色 code）
    canAccessDashboard: dashOk,
    canUseChat: chatOk,
    canAccessOps: opsOk,
    canUseDataCenter: dataCenterOk,
    // 向后兼容 alias（等 APP 全部升级可删）
    isDashboardAdmin: dashOk,
    isOpsAdmin: opsOk,
  };
}

// 供测试重置
export function _resetCacheForTest(uid = null) {
  if (uid) cache.delete(String(uid));
  else cache.clear();
}
export function _resetPoolForTest() {
  pool = null;
}
