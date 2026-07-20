// 权限缓存服务 —— 登录后拉一次 /me/permissions，本地持久化 + 内存 cache，
// 所有 UI 权限判定走 sync 读，避免时序 bug 越权。
//
// 安全默认：未加载完成时 hasRole/hasPermission 均返 false，
//   宁可让 admin 短暂看不到入口，也不能让普通人闪现看到。
//
// 触发点：
//   1) 登录成功后 refreshFromServer()（best-effort，失败仍持久化上次值）
//   2) 应用启动 loadFromCache()（磁盘快照，1-2ms 出结果）
//   3) 进入 workbench initState → 先 loadFromCache 立即 setState，再 refreshFromServer 后台更新
//   4) 退出登录 clear()

import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';
import 'api_service.dart';

/// 权限变更事件 —— 后台轮询发现服务端权限跟本地不同时抛出。
/// gained: 新拿到的权限点；lost: 被撤销的权限点。
class PermissionsChange {
  final Set<String> gained;
  final Set<String> lost;
  const PermissionsChange({required this.gained, required this.lost});
  bool get anyLost => lost.isNotEmpty;
  bool get anyGained => gained.isNotEmpty;
}

class PermissionsService {
  static const _kStorageKey = 'ky_permissions_v1';
  static const _staleAfter = Duration(hours: 24);

  static Set<String> _roles = <String>{};
  static Set<String> _permissions = <String>{};
  static bool _loaded = false;
  static DateTime? _fetchedAt;

  // 变更回调：权限集合发生任何变化就通知所有订阅者（工作台/聊天主页等）。
  // 每个订阅者收到 PermissionsChange 事件，据此弹提示/隐藏入口/pop 页面。
  static final List<void Function(PermissionsChange)> _listeners = [];
  static Timer? _pollTimer;

  static void addListener(void Function(PermissionsChange) cb) => _listeners.add(cb);
  static void removeListener(void Function(PermissionsChange) cb) => _listeners.remove(cb);

  /// 启动前台轮询（默认 30s）。在 APP 主页面 initState 里调；后台/退登时 stop。
  /// 幂等：重复调不会开多个 timer。
  static void startForegroundPolling({Duration interval = const Duration(seconds: 30)}) {
    _pollTimer?.cancel();
    _pollTimer = Timer.periodic(interval, (_) => refreshFromServer());
  }

  static void stopForegroundPolling() {
    _pollTimer?.cancel();
    _pollTimer = null;
  }

  // Profile 字段（登录后从 /me/permissions 拿，避免 LoginCertificate 没存 nickname 的问题）
  static String? _nickname;
  static String? _username;
  static String? _mobile;
  static String? _deptName;

  // ── sync 判定接口（UI 判权限直接调这些）────────────
  static bool get isLoaded => _loaded;
  static bool hasRole(String code) => _roles.contains(code);
  static bool hasPermission(String code) => _permissions.contains(code);

  // 展示用（nickname → username → mobile → 空）—— sync 读，UI 直接用
  static String? get nickname => _nickname;
  static String? get username => _username;
  static String? get mobile => _mobile;
  static String? get deptName => _deptName;
  static String displayName({String fallback = ''}) {
    for (final v in [_nickname, _username, _mobile]) {
      if (v != null && v.trim().isNotEmpty) return v.trim();
    }
    return fallback;
  }

  // 4 个受限功能全都查权限点（不再看角色 code）—— OA 后台勾谁给谁 = 立即生效
  static bool get canAccessDashboard => _permissions.contains('app:dashboard:view');
  static bool get canUseChat         => _permissions.contains('app:chat:use');
  static bool get canAccessOps       => _permissions.contains('app:ops:view');
  static bool get canUseDataCenter   => _permissions.contains('app:data-center:use');

  // 向后兼容 alias
  static bool get isDashboardAdmin => canAccessDashboard;
  static bool get isOpsAdmin       => canAccessOps;

  static Set<String> get roles => Set.unmodifiable(_roles);
  static Set<String> get permissions => Set.unmodifiable(_permissions);

  // ── 加载 / 刷新 ─────────────────────────────────────

  /// 从 SharedPreferences 读上次持久化的权限（磁盘同步 IO，很快）。
  /// 应用启动或页面 initState 调用一次，立即出可用结果。
  static Future<void> loadFromCache() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_kStorageKey);
      if (raw == null || raw.isEmpty) {
        _loaded = true; // 加载完成但没缓存 —— 视为无权限（安全默认）
        return;
      }
      final decoded = json.decode(raw);
      if (decoded is! Map) return;
      _roles = _asStringSet(decoded['roles']);
      _permissions = _asStringSet(decoded['permissions']);
      _nickname = decoded['nickname']?.toString();
      _username = decoded['username']?.toString();
      _mobile = decoded['mobile']?.toString();
      _deptName = decoded['deptName']?.toString();
      final ts = decoded['savedAt'];
      if (ts is int) _fetchedAt = DateTime.fromMillisecondsSinceEpoch(ts);
    } catch (_) {
      // 磁盘坏了就当没缓存
    } finally {
      _loaded = true;
    }
  }

  /// 从后端 /me/permissions 拉最新权限并持久化。
  /// 失败不清空本地缓存（避免网络抖动导致 admin 一瞬间"被降权"看不见入口）。
  static Future<void> refreshFromServer() async {
    try {
      final cert = await ApiService.getFreshLoginCertificate();
      if (cert == null) return; // 未登录
      final token = cert['accessToken']?.toString() ??
          cert['imToken']?.toString() ??
          cert['chatToken']?.toString();
      if (token == null || token.isEmpty) return;

      final headers = <String, String>{
        ...AppConfig.defaultHeaders,
        'Authorization': 'Bearer $token',
        'token': token,
        'Connection': 'close',
      };
      final tenantId = cert['tenantId']?.toString();
      if (tenantId != null && tenantId.isNotEmpty) headers['tenant-id'] = tenantId;

      final resp = await http
          .get(Uri.parse('${AppConfig.baseUrl}/app-api/agent/me/permissions'), headers: headers)
          .timeout(const Duration(seconds: 6));
      if (resp.statusCode != 200) return;
      final body = json.decode(utf8.decode(resp.bodyBytes));
      if (body is! Map) return;
      if (body['code'] != 0 && body['code'] != '0') return;
      final data = body['data'];
      if (data is! Map) return;

      final newRoles = _asStringSet(data['roles']);
      final newPerms = _asStringSet(data['permissions']);
      // Diff 侦测：gain (新拿到) / lose (被撤销)。空的表示没变。
      final gained = newPerms.difference(_permissions);
      final lost = _permissions.difference(newPerms);
      _roles = newRoles;
      _permissions = newPerms;
      _nickname = data['nickname']?.toString();
      _username = data['username']?.toString();
      _mobile = data['mobile']?.toString();
      _deptName = data['deptName']?.toString();
      _fetchedAt = DateTime.now();
      _loaded = true;

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_kStorageKey, json.encode({
        'roles': _roles.toList(),
        'permissions': _permissions.toList(),
        'nickname': _nickname,
        'username': _username,
        'mobile': _mobile,
        'deptName': _deptName,
        'savedAt': _fetchedAt!.millisecondsSinceEpoch,
      }));

      if (gained.isNotEmpty || lost.isNotEmpty) {
        final change = PermissionsChange(gained: gained, lost: lost);
        for (final cb in List.of(_listeners)) {
          try { cb(change); } catch (_) {}
        }
      }
    } catch (_) {
      // 静默失败：保持既有值
    }
  }

  /// initState 里推荐调用：先出 cache，再后台 refresh。
  /// 返回 Future 完成 = cache 已就绪；refresh 是 fire-and-forget。
  static Future<void> loadCacheAndScheduleRefresh() async {
    if (!_loaded) await loadFromCache();
    // 缓存过期就同步等一下 refresh 结果，避免用旧到发臭的权限
    if (_fetchedAt == null || DateTime.now().difference(_fetchedAt!) > _staleAfter) {
      await refreshFromServer();
    } else {
      // 后台刷（不阻塞）
      unawaited(refreshFromServer());
    }
  }

  /// 退登时调用 —— 清内存 + 磁盘 + 停轮询。
  static Future<void> clear() async {
    stopForegroundPolling();
    _roles = <String>{};
    _permissions = <String>{};
    _nickname = null;
    _username = null;
    _mobile = null;
    _deptName = null;
    _loaded = false;
    _fetchedAt = null;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_kStorageKey);
    } catch (_) {}
  }

  static Set<String> _asStringSet(dynamic v) {
    if (v is List) return v.map((e) => e.toString()).toSet();
    return <String>{};
  }
}
