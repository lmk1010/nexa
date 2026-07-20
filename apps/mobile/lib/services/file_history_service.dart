// 拉当前用户的 agent 导出文件历史 —— GET /files/history
import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class FileHistoryService {
  static const Duration _timeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Future<List<ExportedFile>> list({int limit = 50}) async {
    final data = await _get('/files/history?limit=$limit');
    final map = data is Map ? Map<String, dynamic>.from(data) : {};
    final list = (map['list'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => ExportedFile.fromJson(Map<String, dynamic>.from(e)))
        .toList();
    return list;
  }

  static Future<dynamic> _get(String path) async {
    final normalized = path.startsWith('/') ? path : '/$path';
    final uri = Uri.parse('${AppConfig.baseUrl}/app-api/agent$normalized');
    final res = await http
        .get(uri, headers: await _authHeaders())
        .timeout(_timeout);
    if (res.statusCode == 401) throw ApiException('登录已过期');
    if (res.statusCode != 200) throw ApiException('接口失败: ${res.statusCode}');
    final body = json.decode(utf8.decode(res.bodyBytes));
    if (body is! Map) throw ApiException('响应格式错误');
    final map = Map<String, dynamic>.from(body);
    if (map['code'] == 0 || map['code'] == '0') return map['data'];
    throw ApiException(map['msg']?.toString() ?? '未知错误');
  }

  static Future<Map<String, String>> _authHeaders() async {
    final cert = await ApiService.getFreshLoginCertificate();
    if (cert == null) throw ApiException('未登录');
    final token = cert['accessToken']?.toString() ??
        cert['imToken']?.toString() ??
        cert['chatToken']?.toString();
    if (token == null || token.isEmpty) throw ApiException('缺少 token');
    final headers = <String, String>{
      ...AppConfig.defaultHeaders,
      'Authorization': 'Bearer $token',
      'token': token,
      'Connection': 'close',
    };
    final tenantId = cert['tenantId']?.toString();
    if (tenantId != null && tenantId.isNotEmpty) headers['tenant-id'] = tenantId;
    return headers;
  }
}

class ExportedFile {
  final int id;
  final String filename;
  final String downloadUrl;
  final int rows;
  final int bytes;
  final DateTime? createdAt;

  const ExportedFile({
    required this.id,
    required this.filename,
    required this.downloadUrl,
    required this.rows,
    required this.bytes,
    required this.createdAt,
  });

  factory ExportedFile.fromJson(Map<String, dynamic> j) => ExportedFile(
    id: _intOrZero(j['id']),
    filename: (j['filename'] ?? 'file').toString(),
    downloadUrl: (j['downloadUrl'] ?? '').toString(),
    rows: _intOrZero(j['rows']),
    bytes: _intOrZero(j['bytes']),
    createdAt: DateTime.tryParse(j['createdAt']?.toString() ?? ''),
  );
}

int _intOrZero(dynamic v) {
  if (v == null) return 0;
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v.toString()) ?? 0;
}
