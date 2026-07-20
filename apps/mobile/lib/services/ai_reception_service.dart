import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class AiReceptionService {
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Future<int> uploadRecording({
    required String path,
    required int duration,
  }) async {
    final request = http.MultipartRequest(
      'POST',
      _adminUri('/ai/reception/upload'),
    );
    request.headers.addAll(await _authorizedHeaders(jsonBody: false));
    request.fields['duration'] = duration.toString();
    request.files.add(
      await http.MultipartFile.fromPath(
        'file',
        path,
        filename: _uploadFileName(path),
      ),
    );

    final streamed = await request.send().timeout(_requestTimeout);
    final response = await http.Response.fromStream(streamed);
    final data = _extractData(response, '/ai/reception/upload');
    final id = _parseInt(data);
    if (id == null) {
      throw ApiException('录音上传成功但未返回记录编号');
    }
    return id;
  }

  static Future<List<AiReceptionRecord>> getMyRecords() async {
    final response = await http
        .get(
          _adminUri('/ai/reception/my-list'),
          headers: await _authorizedHeaders(),
        )
        .timeout(_requestTimeout);
    final data = _extractData(response, '/ai/reception/my-list');
    return _asList(
      data,
    ).map((item) => AiReceptionRecord.fromJson(_asMap(item))).toList();
  }


  static Future<void> deleteRecord(int id) async {
    final response = await http
        .delete(
          _adminUri('/ai/reception/$id'),
          headers: await _authorizedHeaders(),
        )
        .timeout(_requestTimeout);
    _extractData(response, '/ai/reception/$id');
  }

  static Future<AiReceptionRecord> getRecord(int id) async {
    final response = await http
        .get(
          _adminUri('/ai/reception/$id'),
          headers: await _authorizedHeaders(),
        )
        .timeout(_requestTimeout);
    final data = _extractData(response, '/ai/reception/$id');
    return AiReceptionRecord.fromJson(_asMap(data));
  }

  static String? resolveFileDownloadUrl(String? rawValue) {
    final raw = rawValue?.trim();
    if (raw == null || raw.isEmpty) return null;

    final uri = Uri.tryParse(raw);
    if (uri != null && (uri.scheme == 'http' || uri.scheme == 'https')) {
      return raw;
    }

    final baseUrl = AppConfig.baseUrl.replaceAll(RegExp(r'/+$'), '');
    if (raw.startsWith('/')) return '$baseUrl$raw';
    if (raw.startsWith('admin-api/')) return '$baseUrl/$raw';

    return '$baseUrl/admin-api/infra/file/download/'
        '${Uri.encodeComponent(raw)}';
  }

  static String _uploadFileName(String path) {
    final rawName = path.split(RegExp(r'[/\\]')).last.trim();
    final dotIndex = rawName.lastIndexOf('.');
    final extension = dotIndex >= 0 ? rawName.substring(dotIndex) : '.wav';
    return 'front_desk_${DateTime.now().millisecondsSinceEpoch}$extension';
  }

  static Uri _adminUri(String path, [Map<String, dynamic>? query]) {
    final normalizedPath = path.startsWith('/') ? path : '/$path';
    final uri = Uri.parse('${AppConfig.baseUrl}/admin-api$normalizedPath');
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

  static Future<Map<String, String>> _authorizedHeaders({
    bool jsonBody = true,
  }) async {
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
    if (!jsonBody) headers.remove('Content-Type');
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

class AiReceptionRecord {
  final int? id;
  final String? audioUrl;
  final int duration;
  final String? transcriptText;
  final String? summaryJson;
  final String? visitorName;
  final String? visitorCompany;
  final String? purpose;
  final String? contactPerson;
  final String? urgency;
  final String? processInstanceId;
  final int? status;
  final int? operatorId;
  final String? operatorName;
  final int? deptId;
  final String? notifyUserIds;
  final int? notifyEnabled;
  final String? createTime;
  final String? errorMessage;

  const AiReceptionRecord({
    required this.id,
    required this.audioUrl,
    required this.duration,
    required this.transcriptText,
    required this.summaryJson,
    required this.visitorName,
    required this.visitorCompany,
    required this.purpose,
    required this.contactPerson,
    required this.urgency,
    required this.processInstanceId,
    required this.status,
    required this.operatorId,
    required this.operatorName,
    required this.deptId,
    required this.notifyUserIds,
    required this.notifyEnabled,
    required this.createTime,
    required this.errorMessage,
  });

  factory AiReceptionRecord.fromJson(Map<String, dynamic> json) {
    return AiReceptionRecord(
      id: _parseInt(json['id']),
      audioUrl: _stringValue(json['audioUrl']),
      duration: _parseInt(json['audioDuration']) ?? 0,
      transcriptText: _stringValue(json['transcriptText']),
      summaryJson: _stringValue(json['summaryJson']),
      visitorName: _stringValue(json['visitorName']),
      visitorCompany: _stringValue(json['visitorCompany']),
      purpose: _stringValue(json['purpose']),
      contactPerson: _stringValue(json['contactPerson']),
      urgency: _stringValue(json['urgency']),
      processInstanceId: _stringValue(json['processInstanceId']),
      status: _parseInt(json['status']),
      operatorId: _parseInt(json['operatorId']),
      operatorName: _stringValue(json['operatorName']),
      deptId: _parseInt(json['deptId']),
      notifyUserIds: _stringValue(json['notifyUserIds']),
      notifyEnabled: _parseInt(json['notifyEnabled']),
      createTime: _stringValue(json['createTime']),
      errorMessage: _stringValue(json['errorMessage']),
    );
  }

  Map<String, dynamic> get parsedSummary {
    final raw = summaryJson;
    if (raw == null || raw.trim().isEmpty) return const {};
    try {
      final decoded = json.decode(raw);
      return _asMap(decoded);
    } catch (_) {
      return const {};
    }
  }

  String? get audioPlaybackUrl =>
      AiReceptionService.resolveFileDownloadUrl(audioUrl);

  String? get summaryText => _stringValue(parsedSummary['summary']);

  List<String> get todoTexts {
    final todos = parsedSummary['todos'];
    if (todos is! List) return const [];
    return todos
        .map((item) {
          if (item is String) return item.trim();
          if (item is Map) {
            final map = Map<String, dynamic>.from(item);
            return _stringValue(
                  map['content'] ??
                      map['title'] ??
                      map['todo'] ??
                      map['text'] ??
                      map['name'],
                ) ??
                '';
          }
          return item.toString().trim();
        })
        .where((text) => text.isNotEmpty)
        .toList(growable: false);
  }

  Map<String, dynamic> get roleDetection =>
      _asMap(parsedSummary['role_detection']);

  bool? get canDistinguishRoles =>
      _boolValue(roleDetection['can_distinguish_roles']);

  String? get roleConfidence => _stringValue(roleDetection['confidence']);

  String? get roleDetectionReason => _stringValue(roleDetection['reason']);

  List<AiReceptionDialogueTurn> get dialogueTurns {
    final turns = parsedSummary['dialogue_turns'];
    if (turns is! List) return const [];
    return turns
        .map(AiReceptionDialogueTurn.fromJson)
        .where((turn) => turn.text.isNotEmpty)
        .toList(growable: false);
  }

  Map<String, dynamic> get customerProfile =>
      _asMap(parsedSummary['customer_profile']);

  Map<String, dynamic> get employeeProfile =>
      _asMap(parsedSummary['employee_profile']);

  Map<String, dynamic> get qualityReview =>
      _asMap(parsedSummary['quality_review']);

  Map<String, dynamic> get sentiment => _asMap(parsedSummary['sentiment']);

  List<String> get keyPointTexts => _stringList(parsedSummary['key_points']);

  List<String> get riskPointTexts => _stringList(parsedSummary['risk_points']);

  List<String> get followUpQuestionTexts =>
      _stringList(parsedSummary['follow_up_questions']);

  List<String> get customerNeedTexts => _stringList(customerProfile['needs']);

  List<String> get customerConcernTexts =>
      _stringList(customerProfile['concerns']);

  List<String> get employeeMissingActionTexts =>
      _stringList(employeeProfile['missing_actions']);

  List<String> get qualityStrengthTexts =>
      _stringList(qualityReview['strengths']);

  List<String> get qualityImprovementTexts =>
      _stringList(qualityReview['improvements']);

  String? get customerEmotion =>
      _stringValue(customerProfile['emotion'] ?? sentiment['customer']);

  String? get employeeResponseQuality =>
      _stringValue(employeeProfile['response_quality']);

  String? get employeeSentiment => _stringValue(sentiment['employee']);

  int? get qualityScore => _parseInt(qualityReview['score']);
}

class AiReceptionDialogueTurn {
  final String speaker;
  final String speakerLabel;
  final String text;
  final String? intent;
  final String? confidence;

  const AiReceptionDialogueTurn({
    required this.speaker,
    required this.speakerLabel,
    required this.text,
    required this.intent,
    required this.confidence,
  });

  factory AiReceptionDialogueTurn.fromJson(dynamic value) {
    final map = _asMap(value);
    final speaker = _stringValue(map['speaker']) ?? 'unknown';
    return AiReceptionDialogueTurn(
      speaker: speaker,
      speakerLabel: _stringValue(map['speaker_label']) ?? _speakerText(speaker),
      text: _stringValue(map['text'] ?? map['content']) ?? '',
      intent: _stringValue(map['intent']),
      confidence: _stringValue(map['confidence']),
    );
  }
}

Map<String, dynamic> _asMap(dynamic value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  return <String, dynamic>{};
}

List<dynamic> _asList(dynamic value) {
  if (value is List) return value;
  return const [];
}

String? _stringValue(dynamic value) {
  if (value == null) return null;
  final text = value.toString().trim();
  return text.isEmpty ? null : text;
}

List<String> _stringList(dynamic value) {
  if (value is String) {
    final text = value.trim();
    return text.isEmpty ? const [] : [text];
  }
  if (value is! List) return const [];
  return value
      .map((item) {
        if (item is String) return item.trim();
        if (item is Map) {
          final map = Map<String, dynamic>.from(item);
          return _stringValue(
                map['content'] ??
                    map['text'] ??
                    map['title'] ??
                    map['name'] ??
                    map['summary'],
              ) ??
              '';
        }
        return item.toString().trim();
      })
      .where((text) => text.isNotEmpty)
      .toList(growable: false);
}

bool? _boolValue(dynamic value) {
  if (value is bool) return value;
  final text = value?.toString().trim().toLowerCase();
  if (text == null || text.isEmpty) return null;
  if (text == 'true' || text == '1' || text == 'yes') return true;
  if (text == 'false' || text == '0' || text == 'no') return false;
  return null;
}

int? _parseInt(dynamic value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is num) return value.toInt();
  if (value is String) return int.tryParse(value);
  return null;
}

String _speakerText(String speaker) {
  switch (speaker.toLowerCase()) {
    case 'customer':
    case 'visitor':
      return '客户';
    case 'employee':
    case 'staff':
    case 'operator':
      return '员工';
    default:
      return '未知';
  }
}
