import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

/// 会话元数据 + 消息拉取的 App 端封装，对应 kyx-service-agent 的
/// /conversations 系列接口 (list/get/rename/delete)。
class AgentConversationService {
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  /// 拉取当前用户在指定 scene 下的会话列表 (按最近更新时间倒序)。
  static Future<List<AgentConversationSummary>> list({
    String scene = 'cockpit',
    int limit = 50,
  }) async {
    final data = await _get('/conversations', {
      'scene': scene,
      'limit': limit,
    });
    final map = _asMap(data);
    final rawList = map['list'];
    if (rawList is! List) return const [];
    return rawList
        .whereType<Map>()
        .map((e) => AgentConversationSummary.fromJson(
              Map<String, dynamic>.from(e),
            ))
        .toList();
  }

  /// 拉取单个会话的完整消息历史 (按 turn_index 升序)。
  static Future<List<AgentConversationMessage>> messages(
    String conversationId, {
    int limit = 200,
  }) async {
    final id = conversationId.trim();
    if (id.isEmpty) return const [];
    final data = await _get('/conversations/$id', {'limit': limit});
    final map = _asMap(data);
    final rawList = map['messages'];
    if (rawList is! List) return const [];
    return rawList
        .whereType<Map>()
        .map((e) => AgentConversationMessage.fromJson(
              Map<String, dynamic>.from(e),
            ))
        .toList();
  }

  /// 重命名会话，服务端会校验标题非空。
  static Future<void> rename(String conversationId, String title) async {
    final id = conversationId.trim();
    final t = title.trim();
    if (id.isEmpty || t.isEmpty) {
      throw ApiException('标题不能为空');
    }
    await _send(
      'PATCH',
      '/conversations/$id',
      body: {'title': t},
    );
  }

  /// 删除会话 (软删除元数据，硬删除消息)。
  static Future<void> delete(String conversationId) async {
    final id = conversationId.trim();
    if (id.isEmpty) return;
    await _send('DELETE', '/conversations/$id');
  }

  static Future<dynamic> _get(
    String path, [
    Map<String, dynamic>? query,
  ]) async {
    final response = await _sendWithRetry(
      () async => http
          .get(_agentUri(path, query), headers: await _authorizedHeaders())
          .timeout(_requestTimeout),
    );
    return _extractData(response, path);
  }

  static Future<dynamic> _send(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    final response = await _sendWithRetry(() async {
      final request = http.Request(method, _agentUri(path));
      final headers = await _authorizedHeaders();
      request.headers.addAll(headers);
      if (body != null) {
        request.body = json.encode(_compactMap(body));
      }
      final streamed = await request.send().timeout(_requestTimeout);
      return http.Response.fromStream(streamed);
    });
    return _extractData(response, path);
  }

  static Future<http.Response> _sendWithRetry(
    Future<http.Response> Function() send,
  ) async {
    Object? lastError;
    for (var attempt = 0; attempt < 3; attempt++) {
      try {
        return await send();
      } catch (error) {
        lastError = error;
        if (!_isTransientNetworkError(error) || attempt == 2) {
          break;
        }
        await Future<void>.delayed(Duration(milliseconds: 250 * (attempt + 1)));
      }
    }
    throw ApiException(_networkErrorMessage(lastError));
  }

  static Uri _agentUri(String path, [Map<String, dynamic>? query]) {
    final normalizedPath = path.startsWith('/') ? path : '/$path';
    final uri = Uri.parse('${AppConfig.baseUrl}/app-api/agent$normalizedPath');
    final queryParameters = <String, String>{};
    for (final entry in (query ?? {}).entries) {
      final value = entry.value;
      if (value == null) continue;
      if (value is String && value.trim().isEmpty) continue;
      queryParameters[entry.key] = value.toString();
    }
    return queryParameters.isEmpty
        ? uri
        : uri.replace(queryParameters: queryParameters);
  }

  static Future<Map<String, String>> _authorizedHeaders() async {
    final cert = await ApiService.getFreshLoginCertificate();
    if (cert == null) {
      throw ApiException('用户未登录，请先登录');
    }
    final token = _resolveToken(cert);
    if (token == null || token.isEmpty) {
      throw ApiException('登录凭证缺少 accessToken，请重新登录');
    }
    final headers = <String, String>{
      ...AppConfig.defaultHeaders,
      'Authorization': 'Bearer $token',
      'token': token,
      'Connection': 'close',
      'Accept-Language': 'zh-CN',
      'operationID': DateTime.now().millisecondsSinceEpoch.toString(),
    };
    final tenantId = cert['tenantId']?.toString();
    if (tenantId != null && tenantId.isNotEmpty) {
      headers['tenant-id'] = tenantId;
    }
    return headers;
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
    if (response.statusCode == 404) {
      throw ApiException('会话不存在或已被删除');
    }
    if (response.statusCode != 200) {
      throw ApiException('接口请求失败($apiName): ${response.statusCode}');
    }
    final dynamic body;
    try {
      body = json.decode(utf8.decode(response.bodyBytes));
    } catch (_) {
      throw ApiException('接口响应格式错误($apiName)');
    }
    if (body is! Map) {
      throw ApiException('接口响应格式错误($apiName)');
    }
    final map = Map<String, dynamic>.from(body);
    final code = _parseInt(map['code'] ?? map['errCode']);
    if (code == 0) return map['data'];
    final message = map['msg']?.toString() ??
        map['message']?.toString() ??
        map['errMsg']?.toString() ??
        '未知错误';
    throw ApiException(message);
  }

  static Map<String, dynamic> _asMap(dynamic value) {
    if (value is Map) return Map<String, dynamic>.from(value);
    return const {};
  }
}

class AgentConversationSummary {
  final String conversationId;
  final String title;
  final String scene;
  final int messageCount;
  final String lastMessage;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const AgentConversationSummary({
    required this.conversationId,
    required this.title,
    required this.scene,
    required this.messageCount,
    required this.lastMessage,
    required this.createdAt,
    required this.updatedAt,
  });

  factory AgentConversationSummary.fromJson(Map<String, dynamic> json) {
    return AgentConversationSummary(
      conversationId: json['conversationId']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      scene: json['scene']?.toString() ?? 'cockpit',
      messageCount: _parseInt(json['messageCount']) ?? 0,
      lastMessage: json['lastMessage']?.toString() ?? '',
      createdAt: _parseDate(json['createdAt']),
      updatedAt: _parseDate(json['updatedAt']),
    );
  }

  /// 展示用标题：空标题 fallback 到"新会话"。
  String get displayTitle {
    final t = title.trim();
    if (t.isNotEmpty) return t;
    return '新会话';
  }
}

class AgentConversationMessage {
  final String role;
  final String content;
  final DateTime? createdAt;

  const AgentConversationMessage({
    required this.role,
    required this.content,
    required this.createdAt,
  });

  factory AgentConversationMessage.fromJson(Map<String, dynamic> json) {
    return AgentConversationMessage(
      role: json['role']?.toString() ?? 'user',
      content: json['content']?.toString() ?? '',
      createdAt: _parseDate(json['createdAt']),
    );
  }
}

Map<String, dynamic> _compactMap(Map<String, dynamic> source) {
  final result = <String, dynamic>{};
  for (final entry in source.entries) {
    final value = entry.value;
    if (value == null) continue;
    if (value is String && value.trim().isEmpty) continue;
    result[entry.key] = value;
  }
  return result;
}

int? _parseInt(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  if (value is String) return int.tryParse(value);
  return null;
}

DateTime? _parseDate(dynamic value) {
  if (value == null) return null;
  if (value is DateTime) return value;
  final str = value.toString().trim();
  if (str.isEmpty) return null;
  // MySQL DATETIME(3) 会以 "YYYY-MM-DD HH:mm:ss.sss" 返回，DateTime.parse 不识别空格分隔。
  final normalized = str.contains('T') ? str : str.replaceFirst(' ', 'T');
  return DateTime.tryParse(normalized);
}

bool _isTransientNetworkError(Object error) {
  if (error is TimeoutException || error is SocketException) return true;
  if (error is http.ClientException) return true;
  return false;
}

String _networkErrorMessage(Object? error) {
  if (error is ApiException) return error.message;
  if (error is TimeoutException) return '请求超时，请稍后重试';
  if (error is SocketException) return '网络连接失败，请检查网络';
  return '网络异常：${error ?? '未知错误'}';
}
