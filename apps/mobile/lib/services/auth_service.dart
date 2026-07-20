import 'dart:async';
import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import 'api_service.dart';
import 'chat_service.dart';
import 'permissions_service.dart';
import 'storage_service.dart';

/// 用户认证服务
/// 负责管理用户登录状态、认证信息和相关操作
/// 使用单例模式确保全局认证状态一致性
class AuthService extends ChangeNotifier {
  // 单例模式实现
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  /// 存储服务实例
  final StorageService _storageService = StorageService();

  /// 当前登录状态
  bool _isLoggedIn = false;

  /// 当前用户信息
  Map<String, dynamic>? _currentUser;

  /// 获取当前登录状态
  bool get isLoggedIn => _isLoggedIn;

  /// 获取当前用户信息
  Map<String, dynamic>? get currentUser => _currentUser;

  /// 初始化认证服务
  /// 从本地存储读取登录状态和用户信息
  /// 如果存在有效的登录凭证，则自动登录
  Future<void> initialize() async {
    try {
      final isLoggedIn = await _storageService.isLoggedInAsync();
      _isLoggedIn = isLoggedIn;

      if (isLoggedIn) {
        _currentUser = await _storageService.getLoginCertificateAsync();
        if (_currentUser == null || !await isTokenValid()) {
          _isLoggedIn = false;
          _currentUser = null;
        } else {
          developer.log('自动登录成功，用户ID: ${getUserId()}', name: 'AuthService');
        }
      }

      notifyListeners();
    } catch (e) {
      developer.log('初始化认证状态失败: $e', name: 'AuthService');
      _isLoggedIn = false;
      _currentUser = null;
      notifyListeners();
    }
  }

  /// 保存登录凭证并更新状态
  /// [certificate] 包含用户认证信息的凭证对象
  /// 保存到本地存储并更新当前用户状态
  Future<void> saveLoginCertificate(Map<String, dynamic> certificate) async {
    try {
      final previousCertificate = await _storageService
          .getLoginCertificateAsync();
      final identityChanged = _isDifferentLoginIdentity(previousCertificate, certificate);
      if (identityChanged) {
        await ChatService().resetForAccountChange();
        // 换账号了 —— 先清掉旧用户的权限缓存，避免新用户短暂"继承"旧权限（越权）
        await PermissionsService.clear();
      }

      await _storageService.saveLoginCertificate(certificate);
      _isLoggedIn = true;
      _currentUser = certificate;
      notifyListeners();

      // OA 登录响应根本没返 nickname —— 得单独调 profile 接口拿全用户信息，
      // 合进 cert 存本地。settings/侧边栏等 UI 直接读 cert['nickname'] 即可。
      // 不阻塞主 flow：await 一次拿到就 setState，慢了也不影响导航。
      unawaited(_hydrateProfile(certificate));

      // 权限点（角色/菜单）另拉一次落本地缓存，UI 判权限走 sync 读避免越权
      unawaited(PermissionsService.refreshFromServer());

      developer.log('登录凭证已保存，用户ID: ${getUserId()}', name: 'AuthService');
    } catch (e) {
      developer.log('保存登录凭证失败: $e', name: 'AuthService');
      rethrow;
    }
  }

  /// 登录后拉 OA profile 合进本地 cert（`/admin-api/system/user/profile/get`）。
  /// OA 登录响应只有 userId/token，得单独调 profile 才有 nickname/mobile/dept —— 否则
  /// APP 里所有"当前用户"展示位都只能显示 userId 数字（用户吐过槽的 "显示 1111"）。
  Future<void> _hydrateProfile(Map<String, dynamic> cert) async {
    final token = cert['accessToken']?.toString();
    if (token == null || token.isEmpty) return;
    final tenantId = cert['tenantId']?.toString();
    final profile = await ApiService.fetchUserProfile(
      accessToken: token,
      tenantId: tenantId,
    );
    if (profile == null) return;
    final merged = Map<String, dynamic>.from(cert);
    for (final entry in profile.entries) {
      if (entry.value != null && entry.value.toString().isNotEmpty) {
        merged[entry.key] = entry.value;
      }
    }
    await _storageService.saveLoginCertificate(merged);
    _currentUser = merged;
    notifyListeners();
    developer.log('用户 profile 已合进 cert: nickname=${merged['nickname']}', name: 'AuthService');
  }

  /// 清除登录状态
  /// 清除本地存储的登录凭证并重置用户状态
  Future<void> logout() async {
    try {
      await ChatService().resetForAccountChange();
      await _storageService.clearLoginCertificate();
      await PermissionsService.clear();
      _isLoggedIn = false;
      _currentUser = null;
      notifyListeners();

      developer.log('用户已登出', name: 'AuthService');
    } catch (e) {
      developer.log('登出失败: $e', name: 'AuthService');
      rethrow;
    }
  }

  bool _isDifferentLoginIdentity(
    Map<String, dynamic>? previousCertificate,
    Map<String, dynamic> nextCertificate,
  ) {
    if (previousCertificate == null) return false;

    final previousUserId =
        _stringValue(previousCertificate['userID']) ??
        _stringValue(previousCertificate['userId']);
    final nextUserId =
        _stringValue(nextCertificate['userID']) ??
        _stringValue(nextCertificate['userId']);
    final previousTenantId = _stringValue(previousCertificate['tenantId']);
    final nextTenantId = _stringValue(nextCertificate['tenantId']);

    return previousUserId != nextUserId || previousTenantId != nextTenantId;
  }

  /// 检查登录凭证是否有效
  /// 验证当前登录凭证是否过期
  /// 如果凭证过期，自动执行登出操作
  /// 返回凭证是否有效
  Future<bool> isTokenValid() async {
    if (!_isLoggedIn || _currentUser == null) {
      developer.log('用户未登录或凭证为空', name: 'AuthService');
      return false;
    }

    try {
      final expiresTime = _currentUser!['expiresTime'];

      // 调试：打印凭证信息
      developer.log('检查登录凭证有效性', name: 'AuthService');
      developer.log('用户ID: ${getUserId()}', name: 'AuthService');
      developer.log(
        '原始expiresTime: $expiresTime (类型: ${expiresTime.runtimeType})',
        name: 'AuthService',
      );

      if (expiresTime == null) {
        developer.log('expiresTime为null，本地登录态按有效处理', name: 'AuthService');
        return true;
      }

      // 处理时间戳格式
      final expiresTimeMs = _resolveAbsoluteExpiresTimeMs(expiresTime);
      if (expiresTimeMs == null) {
        developer.log(
          'expiresTime不是明确的绝对过期时间，本地登录态按有效处理: $expiresTime',
          name: 'AuthService',
        );
        return true;
      }

      final currentTime = DateTime.now().millisecondsSinceEpoch;
      final remainingMs = expiresTimeMs - currentTime;
      final isValid = remainingMs > 0;

      // 调试：时间比较
      final currentTimeDate = DateTime.fromMillisecondsSinceEpoch(currentTime);
      final expiresTimeDate = DateTime.fromMillisecondsSinceEpoch(
        expiresTimeMs,
      );
      developer.log(
        '当前时间: $currentTimeDate ($currentTime)',
        name: 'AuthService',
      );
      developer.log(
        '过期时间: $expiresTimeDate ($expiresTimeMs)',
        name: 'AuthService',
      );
      developer.log(
        '剩余时间: ${remainingMs / 1000 / 60} 分钟',
        name: 'AuthService',
      );
      developer.log('Token是否有效: $isValid', name: 'AuthService');

      if (!isValid) {
        developer.log('登录凭证已过期，尝试刷新', name: 'AuthService');
        if (await refreshToken()) {
          developer.log('登录凭证刷新成功，保持登录', name: 'AuthService');
          return true;
        }
        developer.log('登录凭证刷新失败，自动登出', name: 'AuthService');
        await logout();
        return false;
      }

      if (remainingMs <= const Duration(minutes: 5).inMilliseconds) {
        await refreshToken();
      }

      return true;
    } catch (e) {
      developer.log('检查登录凭证有效性失败: $e', name: 'AuthService');
      return false;
    }
  }

  /// 刷新登录凭证
  /// 使用刷新令牌获取新的访问令牌
  /// 返回刷新是否成功
  Future<bool> refreshToken() async {
    if (!_isLoggedIn || _currentUser == null) {
      return false;
    }

    try {
      final refreshToken = _stringValue(_currentUser!['refreshToken']);
      if (refreshToken == null) {
        return false;
      }

      final refreshed = await ApiService.refreshAccessToken(
        refreshToken: refreshToken,
        tenantId: _parseInt(_currentUser!['tenantId']),
      );
      final nextUser = _mergeLoginCertificate(_currentUser!, refreshed.toJson());
      await _storageService.saveLoginCertificate(nextUser);
      _currentUser = nextUser;
      _isLoggedIn = true;
      notifyListeners();
      developer.log('刷新登录凭证成功，用户ID: ${getUserId()}', name: 'AuthService');
      return true;
    } catch (e) {
      developer.log('刷新登录凭证失败: $e', name: 'AuthService');
      return false;
    }
  }

  /// 获取用户ID
  /// 返回当前登录用户的唯一标识符
  String? getUserId() {
    return _stringValue(_currentUser?['userID']) ??
        _stringValue(_currentUser?['userId']);
  }

  /// 获取访问令牌
  /// 返回用于API调用的访问令牌
  String? getAccessToken() {
    return _stringValue(_currentUser?['accessToken']);
  }

  /// 获取刷新令牌
  /// 返回用于刷新访问令牌的刷新令牌
  String? getRefreshToken() {
    return _stringValue(_currentUser?['refreshToken']);
  }

  /// 获取租户信息
  /// 返回当前用户所属的租户信息
  /// 包含租户ID和租户名称
  Map<String, dynamic>? getTenantInfo() {
    if (_currentUser == null) return null;

    return {
      'tenantId': _currentUser!['tenantId'],
      'tenantName': _currentUser!['tenantName'],
    };
  }

  int? _resolveAbsoluteExpiresTimeMs(dynamic value) {
    if (value == null) return null;

    final intValue = _parseInt(value);
    if (intValue != null) {
      if (intValue <= 0) return null;
      if (intValue > 999999999999) {
        return intValue;
      }
      if (intValue > 1000000000) {
        return intValue * 1000;
      }
      return null;
    }

    final string = value.toString().trim();
    if (string.isEmpty) return null;
    final normalized = string.contains(' ') && !string.contains('T')
        ? string.replaceFirst(' ', 'T')
        : string;
    final parsedDate = DateTime.tryParse(normalized);
    return parsedDate?.millisecondsSinceEpoch;
  }

  int? _parseInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  String? _stringValue(dynamic value) {
    final string = value?.toString().trim();
    if (string == null || string.isEmpty || string == 'null') return null;
    return string;
  }

  Map<String, dynamic> _mergeLoginCertificate(
    Map<String, dynamic> current,
    Map<String, dynamic> refreshed,
  ) {
    final merged = Map<String, dynamic>.from(current);
    refreshed.forEach((key, value) {
      final stringValue = value?.toString().trim();
      if (stringValue != null &&
          stringValue.isNotEmpty &&
          stringValue != 'null') {
        merged[key] = value;
      }
    });
    return merged;
  }
}
