import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import '../config/app_config.dart';
import 'storage_service.dart';

/// 用户已存在异常
/// 当尝试注册已存在的用户时抛出
class UserAlreadyExistsException implements Exception {
  final String message;
  UserAlreadyExistsException(this.message);

  @override
  String toString() => message;
}

/// API服务类
/// 负责处理与后端服务器的所有网络通信
/// 包括用户认证、注册、验证码发送等功能
class ApiService {
  static const int dingtalkSocialType = 20;

  /// 用户登录
  /// 支持多种登录方式：手机号、邮箱、用户名
  /// 支持密码登录和验证码登录
  ///
  /// 参数说明：
  /// [areaCode] 区号（手机号登录时使用）
  /// [phoneNumber] 手机号
  /// [account] 用户名
  /// [email] 邮箱
  /// [password] 密码
  /// [verificationCode] 验证码
  ///
  /// 返回登录凭证对象
  static Future<LoginCertificate> login({
    String? areaCode,
    String? phoneNumber,
    String? account,
    String? email,
    String? password,
    String? verificationCode,
  }) async {
    final deviceType = _getDeviceType();
    final deviceId = 'flutter_app_${DateTime.now().millisecondsSinceEpoch}';

    try {
      if (AppConfig.isProduction) {
        return _loginWithAdminAuth(
          phoneNumber: phoneNumber,
          account: account,
          email: email,
          password: password,
          verificationCode: verificationCode,
          deviceType: deviceType,
          deviceId: deviceId,
        );
      }

      Map<String, dynamic> requestBody = {
        'deviceType': deviceType,
        'deviceId': deviceId,
      };

      if (phoneNumber != null && phoneNumber.isNotEmpty) {
        requestBody['loginType'] = 'MOBILE';
        requestBody['mobile'] = phoneNumber;

        if (verificationCode != null && verificationCode.isNotEmpty) {
          requestBody['code'] = verificationCode;
        } else if (password != null && password.isNotEmpty) {
          requestBody['password'] = password;
        } else {
          throw ApiException('登录参数错误：需要提供密码或验证码');
        }
      } else if (email != null && email.isNotEmpty) {
        requestBody['loginType'] = 'EMAIL';
        requestBody['email'] = email;

        if (verificationCode != null && verificationCode.isNotEmpty) {
          requestBody['code'] = verificationCode;
        } else if (password != null && password.isNotEmpty) {
          requestBody['password'] = password;
        } else {
          throw ApiException('登录参数错误：需要提供密码或验证码');
        }
      } else if (account != null && account.isNotEmpty) {
        requestBody['loginType'] = 'USERNAME';
        requestBody['username'] = account;
        if (password == null || password.isEmpty) {
          throw ApiException('用户名登录需要提供密码');
        }
        requestBody['password'] = password;
      } else {
        throw ApiException('登录参数错误：需要提供手机号、邮箱或账号');
      }

      developer.log('AppAuth登录请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/system/app/auth/login',
        name: 'ApiService',
      );
      developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/system/app/auth/login'),
        headers: {
          ...AppConfig.defaultHeaders,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log(
        'AppAuth登录响应状态码: ${response.statusCode}',
        name: 'ApiService',
      );
      developer.log('AppAuth登录响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        if (data['code'] != null) {
          if (data['code'] == 0) {
            developer.log('AppAuth登录成功', name: 'ApiService');
            return LoginCertificate.fromAppAuthJson(data['data']);
          } else {
            developer.log('AppAuth登录失败: ${data['msg']}', name: 'ApiService');

            if (data['code'] == 1002000009) {
              throw UserNotRegisteredException('用户未注册，请先注册');
            }

            throw ApiException('登录失败: ${data['msg']}');
          }
        } else {
          final apiResp = ApiResponse.fromJson(data);
          developer.log('API响应错误码: ${apiResp.errCode}', name: 'ApiService');
          developer.log('API响应错误信息: ${apiResp.errMsg}', name: 'ApiService');

          if (apiResp.errCode == 0) {
            developer.log('登录成功', name: 'ApiService');
            return LoginCertificate.fromJson(apiResp.data);
          } else {
            developer.log('登录失败: ${apiResp.errMsg}', name: 'ApiService');
            throw ApiException('登录失败: ${apiResp.errMsg}');
          }
        }
      } else {
        developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
        throw ApiException('网络错误: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('AppAuth登录异常: $e', name: 'ApiService');
      if (e is ApiException || e is UserNotRegisteredException) {
        rethrow;
      }
      throw ApiException('网络错误: $e');
    }
  }

  static Future<AdminPreLoginResult> preLogin({
    String? phoneNumber,
    String? account,
    String? email,
    String? password,
    String? verificationCode,
  }) async {
    final deviceType = _getDeviceType();
    final deviceId = 'flutter_app_${DateTime.now().millisecondsSinceEpoch}';
    return _preLoginWithAdminAuth(
      phoneNumber: phoneNumber,
      account: account,
      email: email,
      password: password,
      verificationCode: verificationCode,
      deviceType: deviceType,
      deviceId: deviceId,
    );
  }

  static Future<LoginCertificate> tenantLogin({
    required String preAuthToken,
    required int tenantId,
    required String deviceType,
    required String deviceId,
  }) async {
    final tenantLoginBody = {
      'preAuthToken': preAuthToken,
      'tenantId': tenantId,
      'deviceType': deviceType,
      'deviceId': deviceId,
    };

    developer.log('Admin租户登录请求开始', name: 'ApiService');
    developer.log(
      '请求URL: ${AppConfig.baseUrl}/admin-api/system/auth/tenant-login',
      name: 'ApiService',
    );
    developer.log('请求参数: ${json.encode(tenantLoginBody)}', name: 'ApiService');

    final tenantLoginResponse = await http.post(
      Uri.parse('${AppConfig.baseUrl}/admin-api/system/auth/tenant-login'),
      headers: {
        ...AppConfig.defaultHeaders,
        'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
      },
      body: json.encode(tenantLoginBody),
    );

    developer.log(
      'Admin租户登录响应状态码: ${tenantLoginResponse.statusCode}',
      name: 'ApiService',
    );
    developer.log(
      'Admin租户登录响应内容: ${tenantLoginResponse.body}',
      name: 'ApiService',
    );

    if (tenantLoginResponse.statusCode != 200) {
      throw ApiException('网络错误: ${tenantLoginResponse.statusCode}');
    }

    final loginData = _extractCommonResultMap(
      _parseJsonBody(tenantLoginResponse, '租户登录'),
      '租户登录',
    );

    developer.log('Admin租户登录成功，tenantId: $tenantId', name: 'ApiService');
    return LoginCertificate.fromAppAuthJson(loginData);
  }

  static Future<String> socialAuthRedirect({
    required int type,
    required String redirectUri,
  }) async {
    final uri =
        Uri.parse(
          '${AppConfig.baseUrl}/admin-api/system/auth/social-auth-redirect',
        ).replace(
          queryParameters: {
            'type': type.toString(),
            'redirectUri': redirectUri,
          },
        );

    developer.log('社交授权跳转请求开始', name: 'ApiService');
    developer.log('请求URL: $uri', name: 'ApiService');

    final response = await http.get(
      uri,
      headers: {
        ...AppConfig.defaultHeaders,
        'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
      },
    );

    developer.log('社交授权跳转响应状态码: ${response.statusCode}', name: 'ApiService');
    developer.log('社交授权跳转响应内容: ${response.body}', name: 'ApiService');

    if (response.statusCode != 200) {
      throw ApiException('网络错误: ${response.statusCode}');
    }

    final body = _parseJsonBody(response, '社交授权跳转');
    final code = body['code'];
    if (code == 0) {
      final data = body['data']?.toString() ?? '';
      if (data.isEmpty) {
        throw ApiException('社交授权跳转失败: 未返回授权地址');
      }
      return data;
    }

    final message =
        body['msg']?.toString() ?? body['message']?.toString() ?? '未知错误';
    throw ApiException('社交授权跳转失败: $message');
  }

  static Future<SocialBrowserHandoffStatus> getSocialBrowserHandoffStatus(
    String handoffId,
  ) async {
    final uri = Uri.parse(
      '${AppConfig.baseUrl}/admin-api/system/auth/social-browser-handoff/status',
    ).replace(queryParameters: {'handoffId': handoffId});

    final response = await http.get(
      uri,
      headers: {
        ...AppConfig.defaultHeaders,
        'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
      },
    );

    developer.log('社交登录握手状态响应状态码: ${response.statusCode}', name: 'ApiService');
    developer.log('社交登录握手状态响应内容: ${response.body}', name: 'ApiService');

    if (response.statusCode != 200) {
      throw ApiException('网络错误: ${response.statusCode}');
    }

    final data = _extractCommonResultMap(
      _parseJsonBody(response, '社交登录握手状态'),
      '社交登录握手状态',
    );
    return SocialBrowserHandoffStatus.fromJson(
      data,
      deviceType: _getDeviceType(),
      deviceId: 'flutter_app_${DateTime.now().millisecondsSinceEpoch}',
    );
  }

  static Future<TencentImLoginTicket> getTencentImLoginTicket() async {
    final userCert = await getFreshLoginCertificate();
    if (userCert == null) {
      throw ApiException('User is not logged in');
    }

    final token = _resolveAuthToken(userCert);
    if (token == null || token.isEmpty) {
      throw ApiException('Access token is empty');
    }

    final response = await http.get(
      Uri.parse('${AppConfig.baseUrl}/admin-api/im/tencent/session'),
      headers: _authorizedImHeaders(userCert, token),
    );

    developer.log(
      'Tencent IM ticket response: ${response.statusCode}',
      name: 'ApiService',
    );
    developer.log(
      'Tencent IM ticket body: ${response.body}',
      name: 'ApiService',
    );

    if (response.statusCode != 200) {
      throw ApiException(
        'Tencent IM ticket request failed: ${response.statusCode}',
      );
    }

    final body = _parseJsonBody(response, 'Tencent IM ticket');
    final code = body['code'] ?? body['errCode'];
    if (code != 0) {
      throw ApiException(
        body['msg']?.toString() ??
            body['errMsg']?.toString() ??
            'Tencent IM ticket request failed',
      );
    }

    final data = body['data'];
    if (data is! Map<String, dynamic>) {
      throw ApiException('Tencent IM ticket data is invalid');
    }
    return TencentImLoginTicket.fromJson(data);
  }

  static Future<TencentImUserIdMapping> getTencentImUserId(
    String oaUserId,
  ) async {
    final userCert = await getFreshLoginCertificate();
    if (userCert == null) {
      throw ApiException('User is not logged in');
    }

    final token = _resolveAuthToken(userCert);
    if (token == null || token.isEmpty) {
      throw ApiException('Access token is empty');
    }

    final response = await http.get(
      Uri.parse(
        '${AppConfig.baseUrl}/admin-api/im/tencent/user-id?oaUserId=${Uri.encodeQueryComponent(oaUserId)}',
      ),
      headers: _authorizedImHeaders(userCert, token),
    );

    if (response.statusCode != 200) {
      throw ApiException(
        'Tencent IM user id request failed: ${response.statusCode}',
      );
    }

    final body = _parseJsonBody(response, 'Tencent IM user id');
    final code = body['code'] ?? body['errCode'];
    if (code != 0) {
      throw ApiException(
        body['msg']?.toString() ??
            body['errMsg']?.toString() ??
            'Tencent IM user id request failed',
      );
    }

    final data = body['data'];
    if (data is! Map<String, dynamic>) {
      throw ApiException('Tencent IM user id data is invalid');
    }
    final mapping = TencentImUserIdMapping.fromJson(data);
    if (mapping.userID.isEmpty) {
      throw ApiException('Tencent IM user id is empty');
    }
    return mapping;
  }

  static Future<List<TencentImContact>> getTencentImContacts({
    String? keyword,
    int limit = 200,
  }) async {
    final userCert = await getFreshLoginCertificate();
    if (userCert == null) {
      throw ApiException('User is not logged in');
    }

    final token = _resolveAuthToken(userCert);
    if (token == null || token.isEmpty) {
      throw ApiException('Access token is empty');
    }

    final queryParameters = <String, String>{
      'limit': limit.toString(),
      if (keyword != null && keyword.trim().isNotEmpty)
        'keyword': keyword.trim(),
    };
    final uri = Uri.parse(
      '${AppConfig.baseUrl}/admin-api/im/tencent/contacts',
    ).replace(queryParameters: queryParameters);
    final response = await http.get(
      uri,
      headers: _authorizedImHeaders(userCert, token),
    );

    if (response.statusCode != 200) {
      throw ApiException(
        'Tencent IM contacts request failed: ${response.statusCode}',
      );
    }

    final body = _parseJsonBody(response, 'Tencent IM contacts');
    final code = body['code'] ?? body['errCode'];
    if (code != 0) {
      throw ApiException(
        body['msg']?.toString() ??
            body['errMsg']?.toString() ??
            'Tencent IM contacts request failed',
      );
    }

    final data = body['data'];
    if (data is! List) {
      throw ApiException('Tencent IM contacts data is invalid');
    }
    return data
        .whereType<Map>()
        .map(
          (item) => TencentImContact.fromJson(Map<String, dynamic>.from(item)),
        )
        .where((item) => item.imUserId.isNotEmpty)
        .toList();
  }

  static Future<List<SystemSimpleUser>> getSystemSimpleUsers() async {
    final data = await _getAdminData('/system/user/simple-list');
    if (data is! List) throw ApiException('用户列表数据格式错误');
    return data
        .whereType<Map>()
        .map(
          (item) => SystemSimpleUser.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList();
  }

  static Future<List<SystemSimpleDept>> getSystemSimpleDepts() async {
    final data = await _getAdminData('/system/dept/simple-list');
    if (data is! List) throw ApiException('部门列表数据格式错误');
    return data
        .whereType<Map>()
        .map(
          (item) => SystemSimpleDept.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList();
  }

  static Future<dynamic> _getAdminData(String path) async {
    final userCert = await getFreshLoginCertificate();
    if (userCert == null) throw ApiException('User is not logged in');
    final token = _resolveAuthToken(userCert);
    if (token == null || token.isEmpty) {
      throw ApiException('Access token is empty');
    }
    final response = await http.get(
      Uri.parse('${AppConfig.baseUrl}/admin-api$path'),
      headers: _authorizedImHeaders(userCert, token),
    );
    if (response.statusCode != 200) {
      throw ApiException('Admin request failed($path): ${response.statusCode}');
    }
    final body = _parseJsonBody(response, path);
    final code = body['code'] ?? body['errCode'];
    if (code != 0) {
      throw ApiException(
        body['msg']?.toString() ?? body['errMsg']?.toString() ?? '请求失败',
      );
    }
    return body['data'];
  }

  static Future<LoginCertificate> refreshAccessToken({
    required String refreshToken,
    int? tenantId,
  }) async {
    final normalizedRefreshToken = refreshToken.trim();
    if (normalizedRefreshToken.isEmpty) {
      throw ApiException('刷新令牌为空');
    }

    final uri = Uri.parse(
      '${AppConfig.baseUrl}/admin-api/system/auth/refresh-token',
    ).replace(queryParameters: {'refreshToken': normalizedRefreshToken});

    final response = await http.post(
      uri,
      headers: {
        ...AppConfig.defaultHeaders,
        'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        if (tenantId != null) 'tenant-id': tenantId.toString(),
      },
    );

    developer.log('刷新登录凭证响应状态码: ${response.statusCode}', name: 'ApiService');
    developer.log('刷新登录凭证响应内容: ${response.body}', name: 'ApiService');

    if (response.statusCode != 200) {
      throw ApiException('刷新登录凭证网络错误: ${response.statusCode}');
    }

    final data = _extractCommonResultMap(
      _parseJsonBody(response, '刷新登录凭证'),
      '刷新登录凭证',
    );
    return LoginCertificate.fromAppAuthJson(data);
  }

  /// 拉当前登录用户的 OA profile（`/admin-api/system/user/profile/get`）—— nickname/username/mobile/dept/roles。
  /// 登录后调一次，把结果 merge 进 LoginCertificate 存本地。settings 页 sync 读，避免只显示 userId。
  static Future<Map<String, dynamic>?> fetchUserProfile({
    required String accessToken,
    String? tenantId,
  }) async {
    try {
      final headers = <String, String>{
        ...AppConfig.defaultHeaders,
        'Authorization': 'Bearer $accessToken',
        'token': accessToken,
      };
      if (tenantId != null && tenantId.isNotEmpty) headers['tenant-id'] = tenantId;
      final resp = await http
          .get(
            Uri.parse('${AppConfig.baseUrl}/admin-api/system/user/profile/get'),
            headers: headers,
          )
          .timeout(const Duration(seconds: 6));
      if (resp.statusCode != 200) return null;
      final body = json.decode(utf8.decode(resp.bodyBytes));
      if (body is! Map) return null;
      final code = body['code'];
      if (code != 0 && code != '0') return null;
      final data = body['data'];
      if (data is! Map) return null;
      final dept = data['dept'];
      final roles = data['roles'];
      return {
        'nickname': data['nickname']?.toString(),
        'username': data['username']?.toString(),
        'mobile': data['mobile']?.toString(),
        'email': data['email']?.toString(),
        'avatar': data['avatar']?.toString(),
        'deptId': dept is Map ? dept['id']?.toString() : null,
        'deptName': dept is Map ? dept['name']?.toString() : null,
        'roleIds': roles is List
            ? roles.whereType<Map>().map((r) => r['id']?.toString()).where((v) => v != null).toList()
            : null,
      };
    } catch (_) {
      return null;
    }
  }

  static Future<Map<String, dynamic>?> getFreshLoginCertificate() async {
    final storageService = StorageService();
    final currentCert = await storageService.getLoginCertificateAsync();
    if (currentCert == null) return null;

    if (!_shouldRefreshLoginCertificate(currentCert)) {
      return currentCert;
    }

    final refreshToken = _stringValue(currentCert['refreshToken']);
    if (refreshToken == null) {
      return currentCert;
    }

    try {
      final refreshed = await refreshAccessToken(
        refreshToken: refreshToken,
        tenantId: _parseInt(currentCert['tenantId']),
      );
      final nextCert = _mergeLoginCertificate(currentCert, refreshed.toJson());
      await storageService.saveLoginCertificate(nextCert);
      return nextCert;
    } catch (e) {
      developer.log('自动刷新登录凭证失败: $e', name: 'ApiService');
      return currentCert;
    }
  }

  static String? _resolveAuthToken(Map<String, dynamic> userCert) {
    final candidates = [
      userCert['accessToken'],
      userCert['imToken'],
      userCert['chatToken'],
    ];
    for (final value in candidates) {
      final token = value?.toString();
      if (token != null && token.isNotEmpty) {
        return token;
      }
    }
    return null;
  }

  static bool _shouldRefreshLoginCertificate(Map<String, dynamic> cert) {
    final expiresTimeMs = _resolveAbsoluteExpiresTimeMs(cert['expiresTime']);
    if (expiresTimeMs == null) return false;

    final remainingMs = expiresTimeMs - DateTime.now().millisecondsSinceEpoch;
    return remainingMs <= const Duration(minutes: 5).inMilliseconds;
  }

  static int? _resolveAbsoluteExpiresTimeMs(dynamic value) {
    final intValue = _parseInt(value);
    if (intValue != null) {
      if (intValue <= 0) return null;
      if (intValue > 999999999999) return intValue;
      if (intValue > 1000000000) return intValue * 1000;
      return null;
    }

    final string = value?.toString().trim() ?? '';
    if (string.isEmpty) return null;
    final normalized = string.contains(' ') && !string.contains('T')
        ? string.replaceFirst(' ', 'T')
        : string;
    return DateTime.tryParse(normalized)?.millisecondsSinceEpoch;
  }

  static Map<String, dynamic> _mergeLoginCertificate(
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

  static String? _stringValue(dynamic value) {
    final string = value?.toString().trim();
    if (string == null || string.isEmpty || string == 'null') return null;
    return string;
  }

  static int? _parseInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static Map<String, String> _authorizedImHeaders(
    Map<String, dynamic> userCert,
    String token,
  ) {
    final tenantId = userCert['tenantId']?.toString();
    return {
      ...AppConfig.defaultHeaders,
      'Authorization': 'Bearer $token',
      'token': token,
      'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
      if (tenantId != null && tenantId.isNotEmpty) 'tenant-id': tenantId,
    };
  }

  static Future<AdminPreLoginResult> _preLoginWithAdminAuth({
    String? phoneNumber,
    String? account,
    String? email,
    String? password,
    String? verificationCode,
    required String deviceType,
    required String deviceId,
  }) async {
    if (password == null || password.isEmpty) {
      if (verificationCode != null && verificationCode.isNotEmpty) {
        throw ApiException('生产环境暂只支持密码登录');
      }
      throw ApiException('登录参数错误：需要提供密码');
    }

    final username = _resolveAdminLoginUsername(
      phoneNumber: phoneNumber,
      account: account,
      email: email,
    );

    final preLoginBody = {
      'username': username,
      'password': password,
      'deviceType': deviceType,
      'deviceId': deviceId,
    };

    developer.log('Admin预登录请求开始', name: 'ApiService');
    developer.log(
      '请求URL: ${AppConfig.baseUrl}/admin-api/system/auth/pre-login',
      name: 'ApiService',
    );
    developer.log('请求参数: ${json.encode(preLoginBody)}', name: 'ApiService');

    final preLoginResponse = await http.post(
      Uri.parse('${AppConfig.baseUrl}/admin-api/system/auth/pre-login'),
      headers: {
        ...AppConfig.defaultHeaders,
        'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
      },
      body: json.encode(preLoginBody),
    );

    developer.log(
      'Admin预登录响应状态码: ${preLoginResponse.statusCode}',
      name: 'ApiService',
    );
    developer.log('Admin预登录响应内容: ${preLoginResponse.body}', name: 'ApiService');

    if (preLoginResponse.statusCode != 200) {
      throw ApiException('网络错误: ${preLoginResponse.statusCode}');
    }

    final preLoginData = _extractCommonResultMap(
      _parseJsonBody(preLoginResponse, '预登录'),
      '预登录',
    );
    final result = AdminPreLoginResult.fromJson(
      preLoginData,
      deviceType: deviceType,
      deviceId: deviceId,
    );

    if (result.preAuthToken.isEmpty) {
      throw ApiException('预登录失败: 未返回 preAuthToken');
    }
    if (result.tenantList.isEmpty) {
      throw ApiException('预登录失败: 未找到可用租户');
    }
    return result;
  }

  static Future<LoginCertificate> _loginWithAdminAuth({
    String? phoneNumber,
    String? account,
    String? email,
    String? password,
    String? verificationCode,
    required String deviceType,
    required String deviceId,
  }) async {
    final preLoginResult = await _preLoginWithAdminAuth(
      phoneNumber: phoneNumber,
      account: account,
      email: email,
      password: password,
      verificationCode: verificationCode,
      deviceType: deviceType,
      deviceId: deviceId,
    );

    final selectedTenant = preLoginResult.preferredTenant;
    final tenantId = selectedTenant?.tenantId;
    if (tenantId == null) {
      throw ApiException('预登录失败: 租户编号无效');
    }

    return tenantLogin(
      preAuthToken: preLoginResult.preAuthToken,
      tenantId: tenantId,
      deviceType: preLoginResult.deviceType,
      deviceId: preLoginResult.deviceId,
    );
  }

  static String _resolveAdminLoginUsername({
    String? phoneNumber,
    String? account,
    String? email,
  }) {
    if (phoneNumber != null && phoneNumber.isNotEmpty) {
      return phoneNumber;
    }
    if (account != null && account.isNotEmpty) {
      return account;
    }
    if (email != null && email.isNotEmpty) {
      return email;
    }
    throw ApiException('登录参数错误：需要提供手机号、邮箱或账号');
  }

  static Map<String, dynamic> _parseJsonBody(
    http.Response response,
    String apiName,
  ) {
    try {
      final decoded = json.decode(response.body);
      if (decoded is Map<String, dynamic>) {
        return decoded;
      }
      if (decoded is Map) {
        return Map<String, dynamic>.from(decoded);
      }
    } catch (_) {
      throw ApiException('$apiName响应格式错误');
    }
    throw ApiException('$apiName响应格式错误');
  }

  static Map<String, dynamic> _extractCommonResultMap(
    Map<String, dynamic> body,
    String apiName,
  ) {
    final code = body['code'];
    if (code == 0) {
      final data = body['data'];
      if (data is Map<String, dynamic>) {
        return data;
      }
      if (data is Map) {
        return Map<String, dynamic>.from(data);
      }
      throw ApiException('$apiName返回数据格式错误');
    }

    if (code == 1002000009) {
      throw UserNotRegisteredException('用户未注册，请先注册');
    }

    final message =
        body['msg']?.toString() ?? body['message']?.toString() ?? '未知错误';
    throw ApiException('$apiName失败: $message');
  }

  static Map<String, dynamic>? _selectAvailableTenant(dynamic tenantListData) {
    if (tenantListData is! List) {
      return null;
    }

    final tenants = <Map<String, dynamic>>[];
    for (final item in tenantListData) {
      if (item is Map) {
        tenants.add(Map<String, dynamic>.from(item));
      }
    }

    for (final tenant in tenants) {
      if (tenant['isDefault'] == true && _isTenantAvailable(tenant)) {
        return tenant;
      }
    }
    for (final tenant in tenants) {
      if (_isTenantAvailable(tenant)) {
        return tenant;
      }
    }
    return tenants.isEmpty ? null : tenants.first;
  }

  static bool _isTenantAvailable(Map<String, dynamic> tenant) {
    final tenantId = _parseApiInt(tenant['tenantId']);
    final expired = tenant['expired'] == true;
    final hasRole = tenant['hasRole'];
    final status = _parseApiInt(tenant['status']);
    final statusAllowed = status == null || status == 0 || status == 1;
    return tenantId != null && !expired && hasRole != false && statusAllowed;
  }

  static int? _parseApiInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  /// 检查用户是否已注册
  /// 支持通过手机号、邮箱或用户名检查
  ///
  /// 参数说明：
  /// [phoneNumber] 手机号
  /// [email] 邮箱
  /// [account] 用户名
  ///
  /// 返回用户是否存在
  static Future<bool> checkUserExists({
    String? phoneNumber,
    String? email,
    String? account,
  }) async {
    try {
      Map<String, dynamic> requestBody = {};

      if (phoneNumber != null && phoneNumber.isNotEmpty) {
        requestBody['mobile'] = phoneNumber;
      } else if (email != null && email.isNotEmpty) {
        requestBody['email'] = email;
      } else if (account != null && account.isNotEmpty) {
        requestBody['username'] = account;
      } else {
        throw ApiException('检查用户参数错误：需要提供手机号、邮箱或账号');
      }

      developer.log('检查用户是否存在请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/system/app/auth/check-user',
        name: 'ApiService',
      );
      developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/system/app/auth/check-user'),
        headers: {
          ...AppConfig.defaultHeaders,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log(
        '检查用户是否存在响应状态码: ${response.statusCode}',
        name: 'ApiService',
      );
      developer.log('检查用户是否存在响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        // 检查是否是CommonResult格式
        if (data['code'] != null) {
          if (data['code'] == 0) {
            // 用户存在
            return data['data'] == true;
          } else {
            // 用户不存在
            return false;
          }
        } else {
          // 兼容原有的ApiResponse格式
          final apiResp = ApiResponse.fromJson(data);
          if (apiResp.errCode == 0) {
            return apiResp.data == true;
          } else {
            return false;
          }
        }
      } else {
        developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
        return false;
      }
    } catch (e) {
      developer.log('检查用户是否存在异常: $e', name: 'ApiService');
      return false;
    }
  }

  // 发送验证码（使用Spring服务的短信接口）
  static Future<void> sendVerificationCode({
    String? areaCode,
    String? phoneNumber,
    String? email,
  }) async {
    try {
      Map<String, dynamic> requestBody = {
        'mobile': phoneNumber ?? '',
        'scene': 3, // 登录验证码场景
      };

      if (phoneNumber != null && phoneNumber.isNotEmpty) {
        developer.log('发送验证码请求开始', name: 'ApiService');
        developer.log(
          '请求URL: ${AppConfig.baseUrl}/app-api/system/app/auth/send-sms-code',
          name: 'ApiService',
        );
        developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

        final response = await http.post(
          Uri.parse(
            '${AppConfig.baseUrl}/app-api/system/app/auth/send-sms-code',
          ),
          headers: {
            ...AppConfig.defaultHeaders,
            'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
          },
          body: json.encode(requestBody),
        );

        developer.log('发送验证码响应状态码: ${response.statusCode}', name: 'ApiService');
        developer.log('发送验证码响应内容: ${response.body}', name: 'ApiService');

        if (response.statusCode == 200) {
          final data = json.decode(response.body);

          // 检查是否是CommonResult格式
          if (data['code'] != null) {
            if (data['code'] == 0) {
              developer.log('验证码发送成功', name: 'ApiService');
              return;
            } else {
              developer.log('验证码发送失败: ${data['msg']}', name: 'ApiService');
              throw ApiException('验证码发送失败: ${data['msg']}');
            }
          } else {
            // 兼容原有的ApiResponse格式
            final apiResp = ApiResponse.fromJson(data);
            if (apiResp.errCode == 0) {
              developer.log('验证码发送成功', name: 'ApiService');
              return;
            } else {
              developer.log('验证码发送失败: ${apiResp.errMsg}', name: 'ApiService');
              throw ApiException('验证码发送失败: ${apiResp.errMsg}');
            }
          }
        } else {
          developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
          throw ApiException('网络错误: ${response.statusCode}');
        }
      } else {
        throw ApiException('发送验证码参数错误：需要提供手机号');
      }
    } catch (e) {
      developer.log('发送验证码异常: $e', name: 'ApiService');
      if (e is ApiException) {
        rethrow;
      }
      throw ApiException('网络错误: $e');
    }
  }

  // 用户注册（使用Spring服务的注册接口）
  static Future<LoginCertificate> register({
    required String nickname,
    required String password,
    String? faceURL,
    String? areaCode,
    String? phoneNumber,
    String? email,
    String? account,
    int birth = 0,
    int gender = 1,
    String? verificationCode,
    String? invitationCode,
  }) async {
    try {
      // 构建注册请求参数
      Map<String, dynamic> requestBody = {
        'nickname': nickname,
        'password': password,
        'deviceType': _getDeviceType(),
        'deviceId': 'flutter_app_${DateTime.now().millisecondsSinceEpoch}',
      };

      // 根据注册类型设置参数
      if (phoneNumber != null && phoneNumber.isNotEmpty) {
        requestBody['mobile'] = phoneNumber;
        if (verificationCode != null && verificationCode.isNotEmpty) {
          requestBody['code'] = verificationCode;
        }
      } else if (email != null && email.isNotEmpty) {
        requestBody['email'] = email;
        if (verificationCode != null && verificationCode.isNotEmpty) {
          requestBody['code'] = verificationCode;
        }
      } else if (account != null && account.isNotEmpty) {
        requestBody['username'] = account;
      } else {
        throw ApiException('注册参数错误：需要提供手机号、邮箱或账号');
      }

      // 可选参数
      if (faceURL != null && faceURL.isNotEmpty) {
        requestBody['avatar'] = faceURL;
      }
      if (invitationCode != null && invitationCode.isNotEmpty) {
        requestBody['invitationCode'] = invitationCode;
      }

      developer.log('用户注册请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/system/app/auth/register',
        name: 'ApiService',
      );
      developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/system/app/auth/register'),
        headers: {
          ...AppConfig.defaultHeaders,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log('用户注册响应状态码: ${response.statusCode}', name: 'ApiService');
      developer.log('用户注册响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        // 检查是否是CommonResult格式
        if (data['code'] != null) {
          if (data['code'] == 0) {
            developer.log('用户注册成功', name: 'ApiService');
            return LoginCertificate.fromAppAuthJson(data['data']);
          } else {
            developer.log('用户注册失败: ${data['msg']}', name: 'ApiService');

            // 检查是否是手机号已存在错误
            if (data['msg'] != null &&
                (data['msg'].toString().contains('已存在') ||
                    data['msg'].toString().contains('already exists') ||
                    data['msg'].toString().contains('用户已存在'))) {
              throw UserAlreadyExistsException('该手机号已注册，请直接登录');
            }

            throw ApiException('注册失败: ${data['msg']}');
          }
        } else {
          // 兼容原有的ApiResponse格式
          final apiResp = ApiResponse.fromJson(data);
          if (apiResp.errCode == 0) {
            developer.log('用户注册成功', name: 'ApiService');
            return LoginCertificate.fromJson(apiResp.data);
          } else {
            developer.log('用户注册失败: ${apiResp.errMsg}', name: 'ApiService');

            // 检查是否是手机号已存在错误
            if (apiResp.errMsg != null &&
                (apiResp.errMsg.toString().contains('已存在') ||
                    apiResp.errMsg.toString().contains('already exists') ||
                    apiResp.errMsg.toString().contains('用户已存在'))) {
              throw UserAlreadyExistsException('该手机号已注册，请直接登录');
            }

            throw ApiException('注册失败: ${apiResp.errMsg}');
          }
        }
      } else {
        developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
        throw ApiException('网络错误: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('用户注册异常: $e', name: 'ApiService');
      if (e is ApiException) {
        rethrow;
      }
      throw ApiException('网络错误: $e');
    }
  }

  // 重置密码
  static Future<void> resetPassword({
    required String mobile,
    required String code,
    required String password,
  }) async {
    try {
      final requestBody = {
        'mobile': mobile,
        'code': code,
        'password': password,
      };

      developer.log('重置密码请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/system/app/auth/reset-password',
        name: 'ApiService',
      );
      developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

      final response = await http.post(
        Uri.parse(
          '${AppConfig.baseUrl}/app-api/system/app/auth/reset-password',
        ),
        headers: {
          ...AppConfig.defaultHeaders,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log('重置密码响应状态码: ${response.statusCode}', name: 'ApiService');
      developer.log('重置密码响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        // 检查是否是CommonResult格式
        if (data['code'] != null) {
          if (data['code'] == 0) {
            developer.log('重置密码成功', name: 'ApiService');
            return;
          } else {
            developer.log('重置密码失败: ${data['msg']}', name: 'ApiService');
            throw ApiException('重置密码失败: ${data['msg']}');
          }
        } else {
          // 兼容原有的ApiResponse格式
          final apiResp = ApiResponse.fromJson(data);
          if (apiResp.errCode == 0) {
            developer.log('重置密码成功', name: 'ApiService');
            return;
          } else {
            developer.log('重置密码失败: ${apiResp.errMsg}', name: 'ApiService');
            throw ApiException('重置密码失败: ${apiResp.errMsg}');
          }
        }
      } else {
        developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
        throw ApiException('网络错误: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('重置密码异常: $e', name: 'ApiService');
      if (e is ApiException) {
        rethrow;
      }
      throw ApiException('网络错误: $e');
    }
  }

  // 兼容性方法：账号注册（不需要验证码）
  static Future<LoginCertificate> registerAccount({
    required String userID,
    required String nickname,
    required String password,
    String? faceURL,
    String? invitationCode,
  }) async {
    return register(
      nickname: nickname,
      password: password,
      account: userID,
      faceURL: faceURL,
      invitationCode: invitationCode,
    );
  }

  // 兼容性方法：手机验证码注册
  static Future<LoginCertificate> registerWithVerification({
    required String nickname,
    required String password,
    required String phoneNumber,
    required String areaCode,
    required String verificationCode,
    String? faceURL,
    String? invitationCode,
  }) async {
    return register(
      nickname: nickname,
      password: password,
      phoneNumber: phoneNumber,
      verificationCode: verificationCode,
      faceURL: faceURL,
      invitationCode: invitationCode,
    );
  }

  // 兼容性方法：邮箱注册
  static Future<LoginCertificate> registerWithEmail({
    required String email,
    required String password,
    required String nickname,
    String? verificationCode,
    String? invitationCode,
  }) async {
    return register(
      nickname: nickname,
      password: password,
      email: email,
      verificationCode: verificationCode,
      invitationCode: invitationCode,
    );
  }

  // 验证邀请码
  static Future<Map<String, dynamic>> validateInvitationCode(
    String invitationCode,
  ) async {
    try {
      developer.log('验证邀请码请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/im/invite-code/validate?code=$invitationCode',
        name: 'ApiService',
      );
      developer.log('请求参数: code=$invitationCode', name: 'ApiService');

      final response = await http.get(
        Uri.parse(
          '${AppConfig.baseUrl}/app-api/im/invite-code/validate?code=$invitationCode',
        ),
        headers: {
          ...AppConfig.defaultHeaders,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
      );

      developer.log('验证邀请码响应状态码: ${response.statusCode}', name: 'ApiService');
      developer.log('验证邀请码响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        // 检查是否是CommonResult格式
        if (data['code'] != null) {
          if (data['code'] == 0) {
            developer.log('邀请码验证成功', name: 'ApiService');
            return data['data'];
          } else {
            developer.log('邀请码验证失败: ${data['msg']}', name: 'ApiService');
            throw ApiException('邀请码验证失败: ${data['msg']}');
          }
        } else {
          // 兼容原有的ApiResponse格式
          final apiResp = ApiResponse.fromJson(data);
          if (apiResp.errCode == 0) {
            developer.log('邀请码验证成功', name: 'ApiService');
            return apiResp.data;
          } else {
            developer.log('邀请码验证失败: ${apiResp.errMsg}', name: 'ApiService');
            throw ApiException('邀请码验证失败: ${apiResp.errMsg}');
          }
        }
      } else {
        developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
        throw ApiException('网络错误: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('验证邀请码异常: $e', name: 'ApiService');
      if (e is ApiException) {
        rethrow;
      }
      throw ApiException('网络错误: $e');
    }
  }

  // 使用邀请码
  static Future<void> useInvitationCode(String invitationCode) async {
    try {
      developer.log('使用邀请码请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/im/invite-code/use',
        name: 'ApiService',
      );
      developer.log('请求参数: code=$invitationCode', name: 'ApiService');

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/im/invite-code/use'),
        headers: {
          ...AppConfig.defaultHeaders,
          'Content-Type': 'application/x-www-form-urlencoded',
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: 'code=$invitationCode',
      );

      developer.log('使用邀请码响应状态码: ${response.statusCode}', name: 'ApiService');
      developer.log('使用邀请码响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        // 检查是否是CommonResult格式
        if (data['code'] != null) {
          if (data['code'] == 0) {
            developer.log('邀请码使用成功', name: 'ApiService');
            return;
          } else {
            developer.log('邀请码使用失败: ${data['msg']}', name: 'ApiService');
            throw ApiException('邀请码使用失败: ${data['msg']}');
          }
        } else {
          // 兼容原有的ApiResponse格式
          final apiResp = ApiResponse.fromJson(data);
          if (apiResp.errCode == 0) {
            developer.log('邀请码使用成功', name: 'ApiService');
            return;
          } else {
            developer.log('邀请码使用失败: ${apiResp.errMsg}', name: 'ApiService');
            throw ApiException('邀请码使用失败: ${apiResp.errMsg}');
          }
        }
      } else {
        developer.log('HTTP错误: ${response.statusCode}', name: 'ApiService');
        throw ApiException('网络错误: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('使用邀请码异常: $e', name: 'ApiService');
      if (e is ApiException) {
        rethrow;
      }
      throw ApiException('网络错误: $e');
    }
  }

  static String _generateMD5(String input) {
    return md5.convert(utf8.encode(input)).toString();
  }

  static int _getPlatform() {
    // 1: iOS, 2: Android, 3: Windows, 4: OSX, 5: Web, 6: Linux
    if (kIsWeb) {
      return 5; // Web
    }

    // 根据平台返回对应的值
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return 2; // Android
      case TargetPlatform.iOS:
        return 1; // iOS
      case TargetPlatform.windows:
        return 3; // Windows
      case TargetPlatform.macOS:
        return 4; // OSX
      case TargetPlatform.linux:
        return 6; // Linux
      default:
        return 2; // 默认返回Android
    }
  }

  static String _getDeviceType() {
    if (AppConfig.deviceTypeOverride.isNotEmpty) {
      return AppConfig.deviceTypeOverride;
    }

    if (kIsWeb) {
      return 'WEB';
    }

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return 'MOBILE_ANDROID';
      case TargetPlatform.iOS:
        return 'MOBILE_IOS';
      case TargetPlatform.windows:
        return 'DESKTOP_WINDOWS';
      case TargetPlatform.macOS:
        return 'DESKTOP_MACOS';
      case TargetPlatform.linux:
        return 'DESKTOP_LINUX';
      default:
        return 'MOBILE_ANDROID';
    }
  }

  /// 获取好友申请列表
  static Future<List<FriendInfo>> getFriendApplyList() async {
    try {
      // 获取认证token
      final storageService = StorageService();
      final userCert = await storageService.getLoginCertificateAsync();

      developer.log('好友申请列表 - 登录凭证: $userCert');

      if (userCert == null) {
        throw ApiException('用户未登录，请先登录');
      }
      final userID = userCert['userID'] ?? userCert['userId']?.toString();
      if (userID == null || userID.isEmpty) {
        throw ApiException('用户ID不存在，请重新登录');
      }

      // 尝试多种token字段，优先使用非空的token
      String? token;
      if (userCert['imToken'] != null &&
          userCert['imToken'].toString().isNotEmpty) {
        token = userCert['imToken'];
      } else if (userCert['chatToken'] != null &&
          userCert['chatToken'].toString().isNotEmpty) {
        token = userCert['chatToken'];
      } else if (userCert['accessToken'] != null &&
          userCert['accessToken'].toString().isNotEmpty) {
        token = userCert['accessToken'];
      }

      if (token == null || token.isEmpty) {
        throw ApiException('获取认证token失败，所有token字段都为空');
      }

      final requestBody = {
        'userID': userID,
        'pagination': {'pageNumber': 1, 'showNumber': 200},
      };

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/friend/get_friend_apply_list'),
        headers: {
          'Content-Type': 'application/json',
          'token': token,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log('获取好友申请列表请求: ${response.request?.url}');
      developer.log('请求体: ${json.encode(requestBody)}');
      developer.log('响应状态码: ${response.statusCode}');
      developer.log('响应内容: ${response.body}');

      if (response.statusCode == 200) {
        final result = json.decode(response.body);
        final apiResponse = ApiResponse.fromJson(result);

        if (apiResponse.errCode == 0) {
          // 检查响应数据格式，处理可能的嵌套结构
          final responseData = apiResponse.data;
          if (responseData == null) {
            return [];
          }

          // 如果是直接的列表格式
          if (responseData is List) {
            return responseData
                .map((item) => FriendInfo.fromJson(item))
                .toList();
          }

          // 如果是嵌套的对象格式，尝试获取列表字段
          if (responseData is Map<String, dynamic>) {
            // 尝试常见的字段名
            final possibleListFields = [
              'friendApplyList',
              'applyList',
              'data',
              'list',
            ];
            for (final fieldName in possibleListFields) {
              if (responseData.containsKey(fieldName)) {
                final fieldValue = responseData[fieldName];
                if (fieldValue is List) {
                  return fieldValue
                      .map((item) => FriendInfo.fromJson(item))
                      .toList();
                }
              }
            }
            // 如果没有找到列表字段，返回空列表
            return [];
          }

          throw ApiException('好友申请列表数据格式不正确');
        } else {
          throw ApiException(apiResponse.errMsg ?? '获取好友申请列表失败');
        }
      } else if (response.statusCode == 401) {
        throw ApiException('认证失败，请重新登录');
      } else {
        throw ApiException('网络请求失败: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('获取好友申请列表失败: $e');
      rethrow;
    }
  }

  /// 获取好友列表
  static Future<List<FriendInfo>> getFriendList() async {
    try {
      // 获取认证token
      final storageService = StorageService();
      final userCert = await storageService.getLoginCertificateAsync();

      developer.log('登录凭证: $userCert');

      if (userCert == null) {
        throw ApiException('用户未登录，请先登录');
      }

      final userID = userCert['userID'] ?? userCert['userId']?.toString();
      if (userID == null || userID.isEmpty) {
        throw ApiException('用户ID不存在，请重新登录');
      }

      // 尝试多种token字段，优先使用非空的token
      String? token;
      if (userCert['imToken'] != null &&
          userCert['imToken'].toString().isNotEmpty) {
        token = userCert['imToken'];
      } else if (userCert['chatToken'] != null &&
          userCert['chatToken'].toString().isNotEmpty) {
        token = userCert['chatToken'];
      } else if (userCert['accessToken'] != null &&
          userCert['accessToken'].toString().isNotEmpty) {
        token = userCert['accessToken'];
      }

      developer.log('可用token字段:');
      developer.log('- imToken: ${userCert['imToken']}');
      developer.log('- chatToken: ${userCert['chatToken']}');
      developer.log('- accessToken: ${userCert['accessToken']}');
      developer.log('- 最终使用token: $token');

      if (token == null || token.isEmpty) {
        throw ApiException('获取认证token失败，所有token字段都为空');
      }

      final requestBody = {
        'userID': userID,
        'pagination': {'pageNumber': 1, 'showNumber': 200},
      };

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/friend/get_friend_list'),
        headers: {
          'Content-Type': 'application/json',
          'token': token,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log('获取好友列表请求: ${response.request?.url}');
      developer.log('请求体: ${json.encode(requestBody)}');
      developer.log('响应状态码: ${response.statusCode}');
      developer.log('响应内容: ${response.body}');

      if (response.statusCode == 200) {
        final result = json.decode(response.body);
        final apiResponse = ApiResponse.fromJson(result);

        if (apiResponse.errCode == 0) {
          // 解析好友列表数据结构：data.friendsInfo
          final responseData = apiResponse.data;
          if (responseData is Map<String, dynamic>) {
            final friendsInfo = responseData['friendsInfo'];
            if (friendsInfo == null) {
              // 好友列表为空
              return [];
            }
            if (friendsInfo is List) {
              return friendsInfo
                  .map((item) => FriendInfo.fromJson(item))
                  .toList();
            } else {
              throw ApiException('好友列表数据格式错误: friendsInfo should be a list');
            }
          } else {
            throw ApiException('响应数据格式错误: data should be an object');
          }
        } else {
          throw ApiException(apiResponse.errMsg ?? '获取好友列表失败');
        }
      } else if (response.statusCode == 401) {
        throw ApiException('认证失败，请重新登录');
      } else {
        throw ApiException('网络请求失败: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('获取好友列表失败: $e');
      rethrow;
    }
  }

  /// 搜索用户完整信息
  /// 根据关键词搜索用户，支持手机号、用户名、ID、昵称、邮箱搜索
  static Future<List<UserInfo>> searchUsers(
    String keyword, {
    int pageNumber = 1,
    int showNumber = 20,
  }) async {
    try {
      // 获取认证token
      final storageService = StorageService();
      final userCert = await storageService.getLoginCertificateAsync();

      if (userCert == null) {
        throw ApiException('用户未登录，请先登录');
      }

      // 尝试多种token字段，优先使用非空的token
      String? token;
      if (userCert['imToken'] != null &&
          userCert['imToken'].toString().isNotEmpty) {
        token = userCert['imToken'];
      } else if (userCert['chatToken'] != null &&
          userCert['chatToken'].toString().isNotEmpty) {
        token = userCert['chatToken'];
      } else if (userCert['accessToken'] != null &&
          userCert['accessToken'].toString().isNotEmpty) {
        token = userCert['accessToken'];
      }

      if (token == null || token.isEmpty) {
        throw ApiException('获取认证token失败，所有token字段都为空');
      }

      final requestBody = {
        'keyword': keyword,
        'pagination': {'pageNumber': pageNumber, 'showNumber': showNumber},
        'normal': 1, // 搜索普通用户
      };

      developer.log('搜索用户请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/user/search/full',
        name: 'ApiService',
      );
      developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/user/search/full'),
        headers: {
          'Content-Type': 'application/json',
          'token': token,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log('搜索用户响应状态码: ${response.statusCode}', name: 'ApiService');
      developer.log('搜索用户响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final result = json.decode(response.body);
        final apiResponse = ApiResponse.fromJson(result);

        if (apiResponse.errCode == 0) {
          final responseData = apiResponse.data;
          if (responseData is Map<String, dynamic> &&
              responseData['users'] is List) {
            final usersList = responseData['users'] as List;
            return usersList.map((user) => UserInfo.fromJson(user)).toList();
          }
          return [];
        } else {
          throw ApiException(apiResponse.errMsg ?? '搜索用户失败');
        }
      } else if (response.statusCode == 401) {
        throw ApiException('认证失败，请重新登录');
      } else {
        throw ApiException('网络请求失败: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('搜索用户失败: $e', name: 'ApiService');
      rethrow;
    }
  }

  /// 发送好友申请
  static Future<void> sendFriendRequest(String userID, String reqMsg) async {
    try {
      // 获取认证token
      final storageService = StorageService();
      final userCert = await storageService.getLoginCertificateAsync();

      if (userCert == null) {
        throw ApiException('用户未登录，请先登录');
      }

      final myUserID = userCert['userID'] ?? userCert['userId']?.toString();
      if (myUserID == null || myUserID.isEmpty) {
        throw ApiException('用户ID不存在，请重新登录');
      }

      // 尝试多种token字段，优先使用非空的token
      String? token;
      if (userCert['imToken'] != null &&
          userCert['imToken'].toString().isNotEmpty) {
        token = userCert['imToken'];
      } else if (userCert['chatToken'] != null &&
          userCert['chatToken'].toString().isNotEmpty) {
        token = userCert['chatToken'];
      } else if (userCert['accessToken'] != null &&
          userCert['accessToken'].toString().isNotEmpty) {
        token = userCert['accessToken'];
      }

      if (token == null || token.isEmpty) {
        throw ApiException('获取认证token失败，所有token字段都为空');
      }

      final requestBody = {
        'fromUserID': myUserID,
        'toUserID': userID,
        'reqMsg': reqMsg,
      };

      developer.log('发送好友申请请求开始', name: 'ApiService');
      developer.log(
        '请求URL: ${AppConfig.baseUrl}/app-api/friend/add_friend',
        name: 'ApiService',
      );
      developer.log('请求参数: ${json.encode(requestBody)}', name: 'ApiService');

      final response = await http.post(
        Uri.parse('${AppConfig.baseUrl}/app-api/friend/add_friend'),
        headers: {
          'Content-Type': 'application/json',
          'token': token,
          'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
        },
        body: json.encode(requestBody),
      );

      developer.log('发送好友申请响应状态码: ${response.statusCode}', name: 'ApiService');
      developer.log('发送好友申请响应内容: ${response.body}', name: 'ApiService');

      if (response.statusCode == 200) {
        final result = json.decode(response.body);
        final apiResponse = ApiResponse.fromJson(result);

        if (apiResponse.errCode == 0) {
          developer.log('好友申请发送成功', name: 'ApiService');
        } else {
          throw ApiException(apiResponse.errMsg ?? '发送好友申请失败');
        }
      } else if (response.statusCode == 401) {
        throw ApiException('认证失败，请重新登录');
      } else {
        throw ApiException('网络请求失败: ${response.statusCode}');
      }
    } catch (e) {
      developer.log('发送好友申请失败: $e', name: 'ApiService');
      rethrow;
    }
  }
}

class TenantOption {
  final int? tenantId;
  final String tenantName;
  final int? status;
  final bool expired;
  final bool isDefault;
  final bool hasRole;
  final int? globalView;
  final String? viewScope;

  TenantOption({
    this.tenantId,
    required this.tenantName,
    this.status,
    this.expired = false,
    this.isDefault = false,
    this.hasRole = true,
    this.globalView,
    this.viewScope,
  });

  bool get isAvailable {
    final statusAllowed = status == null || status == 0 || status == 1;
    return tenantId != null && !expired && hasRole && statusAllowed;
  }

  factory TenantOption.fromJson(Map<String, dynamic> json) {
    return TenantOption(
      tenantId: _parseInt(json['tenantId']) ?? _parseInt(json['id']),
      tenantName:
          json['tenantName']?.toString() ?? json['name']?.toString() ?? '未命名租户',
      status: _parseInt(json['status']),
      expired: _parseBool(json['expired']),
      isDefault: _parseBool(json['isDefault']),
      hasRole: !_isFalse(json['hasRole']),
      globalView: _parseInt(json['globalView']),
      viewScope: json['viewScope']?.toString(),
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static bool _parseBool(dynamic value) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) {
      final normalized = value.toLowerCase();
      return normalized == 'true' || normalized == '1';
    }
    return false;
  }

  static bool _isFalse(dynamic value) {
    if (value == null) return false;
    if (value is bool) return value == false;
    if (value is num) return value == 0;
    if (value is String) {
      final normalized = value.toLowerCase();
      return normalized == 'false' || normalized == '0';
    }
    return false;
  }
}

class SocialBrowserHandoffStatus {
  final String status;
  final String? message;
  final String preAuthToken;
  final List<TenantOption> tenantList;
  final String deviceType;
  final String deviceId;

  SocialBrowserHandoffStatus({
    required this.status,
    this.message,
    required this.preAuthToken,
    required this.tenantList,
    required this.deviceType,
    required this.deviceId,
  });

  bool get isPending => status == 'PENDING';
  bool get isSuccess => status == 'SUCCESS';
  bool get isFailed => status == 'FAILED';

  AdminPreLoginResult toPreLoginResult() {
    return AdminPreLoginResult(
      preAuthToken: preAuthToken,
      tenantList: tenantList,
      deviceType: deviceType,
      deviceId: deviceId,
    );
  }

  factory SocialBrowserHandoffStatus.fromJson(
    Map<String, dynamic> json, {
    required String deviceType,
    required String deviceId,
  }) {
    final tenantListData = json['tenantList'];
    final tenants = <TenantOption>[];
    if (tenantListData is List) {
      for (final item in tenantListData) {
        if (item is Map) {
          tenants.add(TenantOption.fromJson(Map<String, dynamic>.from(item)));
        }
      }
    }

    return SocialBrowserHandoffStatus(
      status: json['status']?.toString() ?? 'PENDING',
      message: json['message']?.toString(),
      preAuthToken: json['preAuthToken']?.toString() ?? '',
      tenantList: tenants,
      deviceType: deviceType,
      deviceId: deviceId,
    );
  }
}

class AdminPreLoginResult {
  final String preAuthToken;
  final List<TenantOption> tenantList;
  final String deviceType;
  final String deviceId;

  AdminPreLoginResult({
    required this.preAuthToken,
    required this.tenantList,
    required this.deviceType,
    required this.deviceId,
  });

  TenantOption? get preferredTenant {
    for (final tenant in tenantList) {
      if (tenant.isDefault && tenant.isAvailable) {
        return tenant;
      }
    }
    for (final tenant in tenantList) {
      if (tenant.isAvailable) {
        return tenant;
      }
    }
    return tenantList.isEmpty ? null : tenantList.first;
  }

  factory AdminPreLoginResult.fromJson(
    Map<String, dynamic> json, {
    required String deviceType,
    required String deviceId,
  }) {
    final tenantListData = json['tenantList'];
    final tenants = <TenantOption>[];
    if (tenantListData is List) {
      for (final item in tenantListData) {
        if (item is Map) {
          tenants.add(TenantOption.fromJson(Map<String, dynamic>.from(item)));
        }
      }
    }

    return AdminPreLoginResult(
      preAuthToken: json['preAuthToken']?.toString() ?? '',
      tenantList: tenants,
      deviceType: deviceType,
      deviceId: deviceId,
    );
  }
}

class TencentImLoginTicket {
  final int sdkAppId;
  final String userID;
  final String userSig;
  final int expire;
  final int? oaUserId;
  final int? tenantId;

  TencentImLoginTicket({
    required this.sdkAppId,
    required this.userID,
    required this.userSig,
    required this.expire,
    this.oaUserId,
    this.tenantId,
  });

  factory TencentImLoginTicket.fromJson(Map<String, dynamic> json) {
    return TencentImLoginTicket(
      sdkAppId: _parseInt(json['sdkAppId']) ?? _parseInt(json['SDKAppID']) ?? 0,
      userID: json['userID']?.toString() ?? json['userId']?.toString() ?? '',
      userSig: json['userSig']?.toString() ?? '',
      expire: _parseInt(json['expire']) ?? 0,
      oaUserId: _parseInt(json['oaUserId']),
      tenantId: _parseInt(json['tenantId']),
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}

class TencentImUserIdMapping {
  final int? oaUserId;
  final int? tenantId;
  final String userID;

  TencentImUserIdMapping({this.oaUserId, this.tenantId, required this.userID});

  factory TencentImUserIdMapping.fromJson(Map<String, dynamic> json) {
    return TencentImUserIdMapping(
      oaUserId: _parseInt(json['oaUserId']),
      tenantId: _parseInt(json['tenantId']),
      userID: json['userID']?.toString() ?? json['userId']?.toString() ?? '',
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}

class TencentImContact {
  final int? id;
  final int? oaUserId;
  final int? tenantId;
  final String oaUsername;
  final String ordersysUsername;
  final String imUserId;
  final String? remark;
  final int? deptId;
  final String? deptName;
  final String? storeName;

  TencentImContact({
    this.id,
    this.oaUserId,
    this.tenantId,
    required this.oaUsername,
    required this.ordersysUsername,
    required this.imUserId,
    this.remark,
    this.deptId,
    this.deptName,
    this.storeName,
  });

  String get searchableText => [
    displayName,
    oaUsername,
    ordersysUsername,
    imUserId,
    remark,
    deptName,
    storeName,
  ].whereType<String>().join(' ').toLowerCase();

  String get displayName {
    final candidates = [oaUsername, ordersysUsername, imUserId, remark];
    for (final value in candidates) {
      final text = value?.trim();
      if (text != null &&
          text.isNotEmpty &&
          !text.toLowerCase().startsWith('ordersys prod export')) {
        return text;
      }
    }
    return '未知用户';
  }

  factory TencentImContact.fromJson(Map<String, dynamic> json) {
    return TencentImContact(
      id: _parseInt(json['id']),
      oaUserId: _parseInt(json['oaUserId']),
      tenantId: _parseInt(json['tenantId']),
      oaUsername: json['oaUsername']?.toString() ?? '',
      ordersysUsername: json['ordersysUsername']?.toString() ?? '',
      imUserId:
          json['imUserId']?.toString() ??
          json['imUserID']?.toString() ??
          json['userID']?.toString() ??
          json['userId']?.toString() ??
          '',
      remark: json['remark']?.toString(),
      deptId: _parseInt(json['deptId']),
      deptName: json['deptName']?.toString(),
      storeName: json['storeName']?.toString(),
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}

class SystemSimpleUser {
  final int? id;
  final String username;
  final String nickname;
  final int? deptId;
  final String deptName;
  final int? tenantId;

  SystemSimpleUser({
    this.id,
    required this.username,
    required this.nickname,
    this.deptId,
    this.deptName = '',
    this.tenantId,
  });

  factory SystemSimpleUser.fromJson(Map<String, dynamic> json) {
    return SystemSimpleUser(
      id: TencentImContact._parseInt(json['id']),
      username: json['username']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      deptId: TencentImContact._parseInt(json['deptId']),
      deptName: json['deptName']?.toString() ?? '',
      tenantId: TencentImContact._parseInt(json['tenantId']),
    );
  }
}

class SystemSimpleDept {
  final int? id;
  final String name;
  final int? parentId;

  SystemSimpleDept({this.id, required this.name, this.parentId});

  factory SystemSimpleDept.fromJson(Map<String, dynamic> json) {
    return SystemSimpleDept(
      id: TencentImContact._parseInt(json['id']),
      name: json['name']?.toString() ?? '',
      parentId: TencentImContact._parseInt(json['parentId']),
    );
  }
}

class LoginCertificate {
  final int? userId; // int类型，和后端一致
  final String? accessToken;
  final String? refreshToken;
  final int? expiresTime; // int类型，和后端一致
  final int? tenantId; // 租户ID
  final String? tenantName; // 租户名称

  // 兼容老字段
  final String userID;
  final String imToken;
  final String chatToken;

  LoginCertificate({
    this.userId,
    this.accessToken,
    this.refreshToken,
    this.expiresTime,
    this.tenantId,
    this.tenantName,
    this.userID = '',
    this.imToken = '',
    this.chatToken = '',
  });

  factory LoginCertificate.fromAppAuthJson(Map<String, dynamic> json) {
    return LoginCertificate(
      userId: _parseInt(json['userId']),
      accessToken: json['accessToken']?.toString(),
      refreshToken: json['refreshToken']?.toString(),
      expiresTime: _parseExpiresTime(json['expiresTime']),
      tenantId: _parseInt(json['tenantId']),
      tenantName: json['tenantName']?.toString(),
      userID: json['userId']?.toString() ?? '',
      imToken: json['imToken']?.toString() ?? '',
      chatToken: json['chatToken']?.toString() ?? '',
    );
  }

  factory LoginCertificate.fromJson(Map<String, dynamic> json) {
    return LoginCertificate(
      userId: _parseInt(json['userId']),
      accessToken: json['accessToken']?.toString(),
      refreshToken: json['refreshToken']?.toString(),
      expiresTime: _parseExpiresTime(json['expiresTime']),
      tenantId: _parseInt(json['tenantId']),
      tenantName: json['tenantName']?.toString(),
      userID: json['userID']?.toString() ?? json['userId']?.toString() ?? '',
      imToken: json['imToken']?.toString() ?? '',
      chatToken: json['chatToken']?.toString() ?? '',
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static int? _parseExpiresTime(dynamic value) {
    if (value == null) return null;

    final parsedInt = _parseInt(value);
    if (parsedInt != null) {
      if (parsedInt <= 0) return null;
      if (parsedInt > 999999999999) return parsedInt;
      if (parsedInt > 1000000000) return parsedInt * 1000;
      return DateTime.now().millisecondsSinceEpoch + parsedInt * 1000;
    }

    if (value is String) {
      final normalized = value.contains(' ') && !value.contains('T')
          ? value.replaceFirst(' ', 'T')
          : value;
      final parsedDate = DateTime.tryParse(normalized);
      return parsedDate?.millisecondsSinceEpoch;
    }

    return null;
  }

  Map<String, dynamic> toJson() {
    return {
      'userId': userId,
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'expiresTime': expiresTime,
      'tenantId': tenantId,
      'tenantName': tenantName,
      'userID': userID,
      'imToken': imToken,
      'chatToken': chatToken,
    };
  }

  Map<String, dynamic> toMap() {
    return {
      'userId': userId,
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'expiresTime': expiresTime,
      'tenantId': tenantId,
      'tenantName': tenantName,
      'userID': userID,
      'imToken': imToken,
      'chatToken': chatToken,
    };
  }
}

class ApiResponse {
  final int errCode;
  final String? errMsg;
  final String? errDlt;
  final dynamic data;

  ApiResponse({required this.errCode, this.errMsg, this.errDlt, this.data});

  factory ApiResponse.fromJson(Map<String, dynamic> json) {
    return ApiResponse(
      errCode: json['errCode'] ?? -1,
      errMsg: json['errMsg'],
      errDlt: json['errDlt'],
      data: json['data'],
    );
  }
}

/// 好友信息模型
class FriendInfo {
  final String userID;
  final String nickname;
  final String? faceURL;
  final String? remark;
  final bool isOnline;
  final String? department;

  FriendInfo({
    required this.userID,
    required this.nickname,
    this.faceURL,
    this.remark,
    required this.isOnline,
    this.department,
  });

  factory FriendInfo.fromJson(Map<String, dynamic> json) {
    return FriendInfo(
      userID: (json['userID'] ?? json['userId'] ?? json['user_id'] ?? '')
          .toString(),
      nickname: json['nickname'] ?? json['nick_name'] ?? json['name'] ?? '未知用户',
      faceURL: json['faceURL'] ?? json['face_url'] ?? json['avatar'],
      remark: json['remark'] ?? json['alias'],
      isOnline: json['isOnline'] ?? json['is_online'] ?? false,
      department: json['department'] ?? json['dept_name'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'userID': userID,
      'nickname': nickname,
      'faceURL': faceURL,
      'remark': remark,
      'isOnline': isOnline,
      'department': department,
    };
  }
}

class ApiException implements Exception {
  final String message;
  ApiException(this.message);

  @override
  String toString() => message;
}

class UserNotRegisteredException implements Exception {
  final String message;
  UserNotRegisteredException(this.message);

  @override
  String toString() => message;
}

/// 用户信息模型
class UserInfo {
  final String userID;
  final String nickname;
  final String? faceURL;
  final String? account;
  final String? phoneNumber;
  final String? email;
  final int gender;
  final int birth;

  UserInfo({
    required this.userID,
    required this.nickname,
    this.faceURL,
    this.account,
    this.phoneNumber,
    this.email,
    required this.gender,
    required this.birth,
  });

  factory UserInfo.fromJson(Map<String, dynamic> json) {
    return UserInfo(
      userID: json['userID'] ?? json['user_id'] ?? '',
      nickname: json['nickname'] ?? json['nick_name'] ?? '未知用户',
      faceURL: json['faceURL'] ?? json['face_url'] ?? json['avatar'],
      account: json['account'],
      phoneNumber: json['phoneNumber'] ?? json['phone_number'],
      email: json['email'],
      gender: json['gender'] ?? 0,
      birth: json['birth'] ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'userID': userID,
      'nickname': nickname,
      'faceURL': faceURL,
      'account': account,
      'phoneNumber': phoneNumber,
      'email': email,
      'gender': gender,
      'birth': birth,
    };
  }
}
