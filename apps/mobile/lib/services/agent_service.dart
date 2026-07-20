import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class AgentService {
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Stream<AgentStreamEvent> stream({
    required List<AgentChatMessage> messages,
    String? conversationId,
    String? systemPrompt,
    int maxSteps = 8,
  }) async* {
    final compactMessages = messages
        .where((message) => message.content.trim().isNotEmpty)
        .map((message) => message.toJson())
        .toList();
    if (compactMessages.isEmpty) {
      throw ApiException('请输入要咨询的问题');
    }

    final client = http.Client();
    try {
      final uri = _appUri('/agent/stream');
      final request = http.Request('POST', uri);
      final headers = await _authorizedHeaders();
      headers['Accept'] = 'text/event-stream';
      request.headers.addAll(headers);
      request.body = json.encode(
        _compactMap({
          'messages': compactMessages,
          'conversationId': conversationId,
          'systemPrompt': systemPrompt,
          'maxSteps': maxSteps,
        }),
      );

      final response = await client.send(request).timeout(_requestTimeout);
      if (response.statusCode == 401) {
        throw ApiException('登录已过期，请重新登录');
      }
      if (response.statusCode != 200) {
        throw ApiException('接口请求失败(/agent/stream): ${response.statusCode}');
      }

      // SSE parser: Dart's built-in `Utf8Decoder` is chunk-boundary safe when
      // used with `Stream.transform` — it buffers partial multi-byte sequences
      // across chunks. Using `allowMalformed: true` guarantees we never throw
      // even if the upstream slips a stray byte in; corruption becomes visible
      // as U+FFFD rather than cascading over subsequent chars.
      const decoder = Utf8Decoder(allowMalformed: true);

      String eventName = 'message';
      final dataBuffer = StringBuffer();

      Iterable<AgentStreamEvent> flush() sync* {
        if (dataBuffer.isEmpty) return;
        final raw = dataBuffer.toString();
        dataBuffer.clear();
        dynamic decoded;
        try {
          decoded = json.decode(raw);
        } catch (_) {
          decoded = <String, dynamic>{'raw': raw};
        }
        final payload = decoded is Map
            ? Map<String, dynamic>.from(decoded)
            : <String, dynamic>{'value': decoded};
        yield AgentStreamEvent(
          type: (payload['type']?.toString().trim().isNotEmpty ?? false)
              ? payload['type'].toString()
              : eventName,
          data: payload,
        );
      }

      await for (final line
          in response.stream
              .transform(decoder)
              .transform(const LineSplitter())) {
        if (line.isEmpty) {
          for (final event in flush()) {
            yield event;
          }
          eventName = 'message';
          continue;
        }
        if (line.startsWith(':')) continue;
        if (line.startsWith('event:')) {
          eventName = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          if (dataBuffer.isNotEmpty) dataBuffer.write('\n');
          dataBuffer.write(line.substring(5).trimLeft());
        }
      }
      for (final event in flush()) {
        yield event;
      }
    } finally {
      client.close();
    }
  }

  static Future<AgentRunReply> run({
    required List<AgentChatMessage> messages,
    String? conversationId,
    int maxSteps = 8,
  }) async {
    final compactMessages = messages
        .where((message) => message.content.trim().isNotEmpty)
        .map((message) => message.toJson())
        .toList();
    if (compactMessages.isEmpty) {
      throw ApiException('请输入要咨询的问题');
    }

    final response = await http
        .post(
          _appUri('/agent/run'),
          headers: await _authorizedHeaders(),
          body: json.encode(
            _compactMap({
              'messages': compactMessages,
              'conversationId': conversationId,
              'maxSteps': maxSteps,
            }),
          ),
        )
        .timeout(_requestTimeout);

    final data = _extractData(response, '/agent/run');
    return AgentRunReply.fromJson(_asMap(data));
  }

  static Uri _appUri(String path) {
    final normalizedPath = path.startsWith('/') ? path : '/$path';
    return Uri.parse('${AppConfig.baseUrl}/app-api$normalizedPath');
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

    final message =
        map['msg']?.toString() ??
        map['message']?.toString() ??
        map['errMsg']?.toString() ??
        '未知错误';
    throw ApiException(message);
  }
}

class AgentChatMessage {
  final String role;
  final String content;

  const AgentChatMessage({required this.role, required this.content});

  Map<String, dynamic> toJson() {
    return {'role': role, 'content': content};
  }
}

class AgentStreamEvent {
  final String type;
  final Map<String, dynamic> data;

  const AgentStreamEvent({required this.type, required this.data});

  String? _stringField(List<String> keys) {
    for (final key in keys) {
      final value = data[key];
      if (value is String && value.trim().isNotEmpty) return value;
    }
    return null;
  }

  String? get delta => _stringField(const ['delta']);

  String? get fullText => _stringField(const ['text', 'content']);

  String? get toolName {
    final direct = _stringField(const ['name', 'toolName', 'tool']);
    if (direct != null) return direct.trim();
    final nested = data['toolCall'];
    if (nested is Map) {
      final name = nested['name'] ?? nested['toolName'];
      if (name is String && name.trim().isNotEmpty) return name.trim();
    }
    return null;
  }

  String? get toolCallId => _stringField(const ['id', 'toolCallId', 'callId']);

  bool? get toolSuccess {
    final raw = data['success'];
    if (raw is bool) return raw;
    return null;
  }

  int? get iteration {
    final raw = data['iteration'];
    if (raw is int) return raw;
    if (raw is num) return raw.toInt();
    return null;
  }

  int? get durationMs {
    final raw = data['durationMs'];
    if (raw is int) return raw;
    if (raw is num) return raw.toInt();
    return null;
  }

  String? get argsPreview => _stringField(const ['argsPreview']);

  // Server-side pre-rendered pretty label for this tool call, e.g.
  // "查询：售后【赔付/损失金额】月度统计" or "搜索接口：本月赔付部门排行".
  // Falls back to null when tool-specific formatting isn't defined server-side,
  // in which case the client synthesises a label from name + argsPreview.
  String? get humanLabel => _stringField(const ['humanLabel']);

  String? get outputPreview => _stringField(const ['outputPreview']);

  String? get errorMessage =>
      _stringField(const ['error', 'errorReason', 'message', 'msg']);
}

class AgentRunReply {
  final String text;
  final String? conversationId;
  final String stopReason;
  final Map<String, dynamic> usage;
  final List<dynamic> steps;

  const AgentRunReply({
    required this.text,
    required this.conversationId,
    required this.stopReason,
    required this.usage,
    required this.steps,
  });

  factory AgentRunReply.fromJson(Map<String, dynamic> json) {
    return AgentRunReply(
      text:
          _stringValue(json['text']) ??
          _stringValue(json['answer']) ??
          _stringValue(json['content']) ??
          '',
      conversationId: _stringValue(json['conversationId']),
      stopReason: _stringValue(json['stopReason']) ?? '',
      usage: _asMap(json['usage']),
      steps: _asList(json['steps']),
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

Map<String, dynamic> _asMap(dynamic value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  return const {};
}

List<dynamic> _asList(dynamic value) {
  if (value is List) return value;
  return const [];
}

String? _stringValue(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty ? null : text;
}

int? _parseInt(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '');
}
