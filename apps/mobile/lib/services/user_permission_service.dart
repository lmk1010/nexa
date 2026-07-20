import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class UserPermissionService {
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static const Set<String> _executiveRoles = {
    'super_admin',
    'tenant_admin',
    'system_admin',
    'biz_boss',
  };

  static const Set<String> _executivePermissions = {
    'business:executive-cockpit:query',
    'work:requirement:query-all',
  };

  static Future<UserPermissionInfo> getPermissionInfo() async {
    final cert = await ApiService.getFreshLoginCertificate();
    if (cert == null) {
      throw ApiException('用户未登录，请先登录');
    }

    final token = _resolveToken(cert);
    if (token == null || token.isEmpty) {
      throw ApiException('登录凭证缺少 accessToken，请重新登录');
    }

    final response = await http
        .get(
          Uri.parse(
            '${AppConfig.baseUrl}/admin-api/system/auth/get-permission-info',
          ),
          headers: _authorizedHeaders(cert, token),
        )
        .timeout(_requestTimeout);
    final data = _extractData(response, '权限信息');
    return UserPermissionInfo.fromJson(_asMap(data));
  }

  static Future<bool> canViewExecutiveCockpit() async {
    final info = await getPermissionInfo();
    return info.canViewExecutiveCockpit;
  }

  static Map<String, String> _authorizedHeaders(
    Map<String, dynamic> cert,
    String token,
  ) {
    final tenantId = cert['tenantId']?.toString();
    return {
      ...AppConfig.defaultHeaders,
      'Authorization': 'Bearer $token',
      'token': token,
      'Accept-Language': 'zh-CN',
      'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
      if (tenantId != null && tenantId.isNotEmpty) 'tenant-id': tenantId,
    };
  }

  static String? _resolveToken(Map<String, dynamic> cert) {
    for (final key in const ['accessToken', 'imToken', 'chatToken']) {
      final value = cert[key]?.toString();
      if (value != null && value.isNotEmpty) return value;
    }
    return null;
  }

  static dynamic _extractData(http.Response response, String apiName) {
    if (response.statusCode == 401) {
      throw ApiException('登录已过期，请重新登录');
    }
    if (response.statusCode != 200) {
      throw ApiException('接口请求失败($apiName): ${response.statusCode}');
    }

    final dynamic body;
    try {
      body = json.decode(utf8.decode(response.bodyBytes));
    } catch (_) {
      throw ApiException('$apiName响应格式错误');
    }

    if (body is! Map) {
      throw ApiException('$apiName响应格式错误');
    }

    final map = Map<String, dynamic>.from(body);
    final code = _parseInt(map['code'] ?? map['errCode']);
    if (code == 0) return map['data'];

    final message =
        map['msg']?.toString() ??
        map['message']?.toString() ??
        map['errMsg']?.toString() ??
        '未知错误';
    throw ApiException('$apiName失败: $message');
  }
}

class UserPermissionInfo {
  final Set<String> roles;
  final Set<String> permissions;
  final bool hasRole;

  const UserPermissionInfo({
    required this.roles,
    required this.permissions,
    required this.hasRole,
  });

  factory UserPermissionInfo.fromJson(Map<String, dynamic> json) {
    return UserPermissionInfo(
      roles: _stringSet(json['roles']),
      permissions: _stringSet(json['permissions']),
      hasRole: _boolValue(json['hasRole'], fallback: true),
    );
  }

  bool get canViewExecutiveCockpit {
    if (!hasRole) return false;
    return roles.any(UserPermissionService._executiveRoles.contains) ||
        permissions.any(UserPermissionService._executivePermissions.contains);
  }
}

Map<String, dynamic> _asMap(dynamic value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  return const {};
}

Set<String> _stringSet(dynamic value) {
  if (value is Iterable) {
    return value
        .map((item) => item?.toString().trim() ?? '')
        .where((item) => item.isNotEmpty && item != 'null')
        .toSet();
  }
  final text = value?.toString().trim();
  if (text == null || text.isEmpty || text == 'null') return const {};
  return text
      .split(',')
      .map((item) => item.trim())
      .where((item) => item.isNotEmpty)
      .toSet();
}

bool _boolValue(dynamic value, {required bool fallback}) {
  if (value is bool) return value;
  if (value is num) return value != 0;
  final text = value?.toString().trim().toLowerCase();
  if (text == 'true' || text == '1') return true;
  if (text == 'false' || text == '0') return false;
  return fallback;
}

int? _parseInt(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  if (value is String) return int.tryParse(value);
  return null;
}
