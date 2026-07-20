import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LoginFormCredential {
  final String account;
  final String password;
  final int lastUsedAt;

  const LoginFormCredential({
    required this.account,
    required this.password,
    required this.lastUsedAt,
  });

  factory LoginFormCredential.fromJson(Map<String, dynamic> json) {
    return LoginFormCredential(
      account: _readTrimmedString(json['account']),
      password: _readRawString(json['password']),
      lastUsedAt: _readInt(json['lastUsedAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {'account': account, 'password': password, 'lastUsedAt': lastUsedAt};
  }

  static String _readTrimmedString(dynamic value) {
    final string = value?.toString() ?? '';
    return string.trim();
  }

  static String _readRawString(dynamic value) {
    return value?.toString() ?? '';
  }

  static int _readInt(dynamic value) {
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }
}

/// 本地存储服务
/// 使用 SharedPreferences 实现持久化存储
/// 主要用于保存登录凭证和其他应用数据
/// 使用单例模式确保全局存储状态一致性
class StorageService {
  // 单例模式实现
  static final StorageService _instance = StorageService._internal();
  factory StorageService() => _instance;
  StorageService._internal();

  /// SharedPreferences 实例
  SharedPreferences? _prefs;

  static const String _loginFormCredentialHistoryKey =
      'loginFormCredentialHistory';
  static const int _maxLoginFormCredentialHistory = 8;

  /// 初始化 SharedPreferences
  /// 确保存储服务在使用前已正确初始化
  Future<void> _initPrefs() async {
    _prefs ??= await SharedPreferences.getInstance();
  }

  /// 获取账号密码登录历史，第一条为最近一次成功登录。
  Future<List<LoginFormCredential>> getLoginFormCredentialHistory() async {
    await _initPrefs();
    final jsonString = _prefs!.getString(_loginFormCredentialHistoryKey);
    if (jsonString == null || jsonString.trim().isEmpty) return [];

    try {
      final decoded = json.decode(jsonString);
      if (decoded is! List) return [];

      final credentials = decoded
          .whereType<Map>()
          .map(
            (item) =>
                LoginFormCredential.fromJson(Map<String, dynamic>.from(item)),
          )
          .where(
            (credential) =>
                credential.account.isNotEmpty && credential.password.isNotEmpty,
          )
          .toList();
      credentials.sort((a, b) => b.lastUsedAt.compareTo(a.lastUsedAt));
      return credentials.take(_maxLoginFormCredentialHistory).toList();
    } catch (e) {
      if (kDebugMode) {
        print('解析账号登录历史失败: $e');
      }
      return [];
    }
  }

  /// 保存账号密码登录历史。相同账号会更新到第一位，最多保留 8 条。
  Future<void> saveLoginFormCredential({
    required String account,
    required String password,
  }) async {
    await _initPrefs();

    final normalizedAccount = account.trim();
    final normalizedPassword = password;
    if (normalizedAccount.isEmpty || normalizedPassword.isEmpty) return;

    final currentHistory = await getLoginFormCredentialHistory();
    final nextHistory = <LoginFormCredential>[
      LoginFormCredential(
        account: normalizedAccount,
        password: normalizedPassword,
        lastUsedAt: DateTime.now().millisecondsSinceEpoch,
      ),
      ...currentHistory.where(
        (credential) => credential.account != normalizedAccount,
      ),
    ].take(_maxLoginFormCredentialHistory).toList();

    await _prefs!.setString(
      _loginFormCredentialHistoryKey,
      json.encode(
        nextHistory.map((credential) => credential.toJson()).toList(),
      ),
    );
  }

  /// 删除单个账号登录历史。
  Future<void> removeLoginFormCredential(String account) async {
    await _initPrefs();

    final normalizedAccount = account.trim();
    if (normalizedAccount.isEmpty) return;

    final nextHistory = (await getLoginFormCredentialHistory())
        .where((credential) => credential.account != normalizedAccount)
        .toList();

    await _prefs!.setString(
      _loginFormCredentialHistoryKey,
      json.encode(
        nextHistory.map((credential) => credential.toJson()).toList(),
      ),
    );
  }

  /// 保存登录凭证
  /// [certificate] 包含用户认证信息的凭证对象
  /// 检查是否已存在相同凭证，避免重复保存
  Future<void> saveLoginCertificate(Map<String, dynamic> certificate) async {
    await _initPrefs();
    final normalizedCertificate = _normalizeLoginCertificate(certificate);

    // 检查是否已经保存了相同的登录凭证
    final existingCert = await getLoginCertificateAsync();
    if (existingCert != null) {
      // 比较关键字段，如果相同则不重复保存
      if (_isSameCertificate(existingCert, normalizedCertificate)) {
        if (kDebugMode) {
          print('登录凭证已存在，跳过重复保存');
        }
        return;
      }
    }

    final jsonString = json.encode(normalizedCertificate);
    await _prefs!.setString('loginCertificate', jsonString);
    if (kDebugMode) {
      print('登录凭证已保存: $normalizedCertificate');
    }
  }

  Map<String, dynamic> _normalizeLoginCertificate(
    Map<String, dynamic> certificate,
  ) {
    final normalized = Map<String, dynamic>.from(certificate);
    final userId = _stringValue(normalized['userId']);
    final userID = _stringValue(normalized['userID']);
    if (userID == null && userId != null) {
      normalized['userID'] = userId;
    }
    return normalized;
  }

  /// 检查两个登录凭证是否相同
  /// [cert1] 第一个凭证
  /// [cert2] 第二个凭证
  /// 比较关键字段判断是否为同一用户的凭证
  bool _isSameCertificate(
    Map<String, dynamic> cert1,
    Map<String, dynamic> cert2,
  ) {
    // 比较关键字段
    final keyFields = [
      'userId',
      'userID',
      'accessToken',
      'refreshToken',
      'tenantId',
    ];
    for (final field in keyFields) {
      if (_stringValue(cert1[field]) != _stringValue(cert2[field])) {
        return false;
      }
    }
    return true;
  }

  /// 获取登录凭证（同步方法）
  /// 注意：如果 SharedPreferences 未初始化，可能返回 null
  /// 建议使用异步方法 getLoginCertificateAsync()
  Map<String, dynamic>? getLoginCertificate() {
    // 如果 _prefs 为 null，尝试同步初始化
    if (_prefs == null) {
      // 在同步方法中无法使用 await，所以这里先返回 null
      // 实际的初始化会在异步方法中进行
      return null;
    }
    final jsonString = _prefs!.getString('loginCertificate');
    if (jsonString == null) return null;
    try {
      return json.decode(jsonString) as Map<String, dynamic>;
    } catch (e) {
      if (kDebugMode) {
        print('解析登录凭证失败: $e');
      }
      return null;
    }
  }

  /// 异步获取登录凭证
  /// 确保 SharedPreferences 已初始化后获取凭证
  /// 返回解析后的登录凭证对象，如果不存在则返回 null
  Future<Map<String, dynamic>?> getLoginCertificateAsync() async {
    await _initPrefs();
    final jsonString = _prefs!.getString('loginCertificate');
    if (jsonString == null) return null;
    try {
      return json.decode(jsonString) as Map<String, dynamic>;
    } catch (e) {
      if (kDebugMode) {
        print('解析登录凭证失败: $e');
      }
      return null;
    }
  }

  /// 清除登录凭证
  /// 从本地存储中删除登录凭证
  Future<void> clearLoginCertificate() async {
    await _initPrefs();
    await _prefs!.remove('loginCertificate');
    if (kDebugMode) {
      print('登录凭证已清除');
    }
  }

  /// 检查是否已登录（同步方法）
  /// 验证登录凭证是否存在且包含必要的字段
  bool isLoggedIn() {
    final cert = getLoginCertificate();
    return cert != null && _hasUsableLoginCertificate(cert);
  }

  /// 异步检查是否已登录
  /// 确保 SharedPreferences 已初始化后检查登录状态
  Future<bool> isLoggedInAsync() async {
    final cert = await getLoginCertificateAsync();
    return cert != null && _hasUsableLoginCertificate(cert);
  }

  bool _hasUsableLoginCertificate(Map<String, dynamic> cert) {
    final hasUser =
        _stringValue(cert['userID']) != null ||
        _stringValue(cert['userId']) != null;
    final hasToken =
        _stringValue(cert['accessToken']) != null ||
        _stringValue(cert['imToken']) != null ||
        _stringValue(cert['chatToken']) != null;
    return hasUser && hasToken;
  }

  /// 获取用户ID
  /// 从登录凭证中提取用户唯一标识符
  String? getUserID() {
    final cert = getLoginCertificate();
    return _stringValue(cert?['userID']) ?? _stringValue(cert?['userId']);
  }

  /// 获取IM Token
  /// 从登录凭证中提取即时通讯令牌
  String? getIMToken() {
    final cert = getLoginCertificate();
    return _stringValue(cert?['imToken']);
  }

  /// 获取Chat Token
  /// 从登录凭证中提取聊天服务令牌
  String? getChatToken() {
    final cert = getLoginCertificate();
    return _stringValue(cert?['chatToken']);
  }

  /// 获取访问令牌
  /// 从登录凭证中提取访问令牌
  Future<String?> getToken() async {
    final cert = await getLoginCertificateAsync();
    return _stringValue(cert?['accessToken']) ??
        _stringValue(cert?['imToken']) ??
        _stringValue(cert?['chatToken']);
  }

  String? _stringValue(dynamic value) {
    final string = value?.toString().trim();
    if (string == null || string.isEmpty || string == 'null') return null;
    return string;
  }

  /// 保存通用数据
  /// [key] 存储键名
  /// [value] 要存储的值（支持多种数据类型）
  /// 自动根据数据类型选择合适的存储方式
  Future<void> saveData(String key, dynamic value) async {
    await _initPrefs();
    if (value is String) {
      await _prefs!.setString(key, value);
    } else if (value is int) {
      await _prefs!.setInt(key, value);
    } else if (value is double) {
      await _prefs!.setDouble(key, value);
    } else if (value is bool) {
      await _prefs!.setBool(key, value);
    } else {
      // 对于复杂对象，转换为JSON字符串存储
      final jsonString = json.encode(value);
      await _prefs!.setString(key, jsonString);
    }
  }

  /// 获取通用数据
  /// [key] 存储键名
  /// 返回指定类型的数据，如果不存在则返回 null
  T? getData<T>(String key) {
    if (_prefs == null) return null;

    if (T == String) {
      return _prefs!.getString(key) as T?;
    } else if (T == int) {
      return _prefs!.getInt(key) as T?;
    } else if (T == double) {
      return _prefs!.getDouble(key) as T?;
    } else if (T == bool) {
      return _prefs!.getBool(key) as T?;
    } else {
      // 对于复杂对象，从JSON字符串解析
      final jsonString = _prefs!.getString(key);
      if (jsonString == null) return null;
      try {
        return json.decode(jsonString) as T?;
      } catch (e) {
        if (kDebugMode) {
          print('解析数据失败: $e');
        }
        return null;
      }
    }
  }

  /// 清除所有存储数据
  /// 删除所有本地存储的数据，包括登录凭证
  Future<void> clearAll() async {
    await _initPrefs();
    await _prefs!.clear();
    if (kDebugMode) {
      print('所有存储数据已清除');
    }
  }

  /// 调试方法：检查存储状态
  /// 在调试模式下打印所有存储的键值对
  Future<void> debugStorageStatus() async {
    await _initPrefs();
    final allKeys = _prefs!.getKeys();
    if (kDebugMode) {
      print('当前存储的所有键: $allKeys');
      for (final key in allKeys) {
        final value = _prefs!.get(key);
        if (key == _loginFormCredentialHistoryKey) {
          print('键: $key, 值: <已隐藏>');
          continue;
        }
        print('键: $key, 值: $value');
      }
    }
  }
}
