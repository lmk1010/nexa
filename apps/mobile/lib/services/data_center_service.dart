// 数据中心 (kyx-data-center) 的 HTTP client
// 路由: gateway ${baseUrl}/app-api/data-center/*
import 'dart:convert';
import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class DataCenterService {
  static const Duration _requestTimeout = Duration(seconds: 30);

  // ── 模板 ────────────────────────────────────────
  static Future<List<TemplateInfo>> listTemplates() async {
    final data = await _get('/templates');
    if (data is! List) return const [];
    return data
        .whereType<Map>()
        .map((e) => TemplateInfo.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  // ── 任务 ────────────────────────────────────────
  static Future<CreateJobResponse> createJob({
    required String templateId,
    required Map<String, dynamic> filters,
  }) async {
    final data = await _post('/jobs', {
      'template_id': templateId,
      'filters': filters,
    });
    return CreateJobResponse.fromJson(_asMap(data));
  }

  static Future<JobInfo> getJob(String id) async {
    final data = await _get('/jobs/$id');
    return JobInfo.fromJson(_asMap(data));
  }

  static Future<List<JobInfo>> listMyJobs({int limit = 20}) async {
    final data = await _get('/jobs?limit=$limit');
    if (data is! List) return const [];
    return data
        .whereType<Map>()
        .map((e) => JobInfo.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  static Future<void> cancelJob(String id) async {
    await _delete('/jobs/$id');
  }

  /// 搜索 lookup 分页
  static Future<LookupPage> searchLookup(String name,
      {String q = '', int limit = 30, int offset = 0}) async {
    final qEnc = Uri.encodeQueryComponent(q);
    final data = await _get('/lookups/$name?q=$qEnc&limit=$limit&offset=$offset');
    final m = _asMap(data);
    final list = (m['items'] as List?) ?? const [];
    return LookupPage(
      items: list
          .whereType<Map>()
          .map((e) => LookupItem.fromJson(Map<String, dynamic>.from(e)))
          .toList(),
      offset: (m['offset'] is num) ? (m['offset'] as num).toInt() : offset,
      limit: (m['limit'] is num) ? (m['limit'] as num).toInt() : limit,
      hasMore: m['has_more'] == true,
    );
  }

  static String xlsxUrl(String jobId) =>
      '${AppConfig.baseUrl}/app-api/data-center/jobs/$jobId/xlsx';

  /// 供 FileDownloadService 使用的 auth headers (JWT + tenant-id)
  static Future<Map<String, String>> downloadHeaders() async {
    return _headers();
  }

  // ── 大厅 ────────────────────────────────────────
  static Future<HallSnapshot> getHall() async {
    final data = await _get('/hall');
    return HallSnapshot.fromJson(_asMap(data));
  }

  // ── 内部 ────────────────────────────────────────
  static Uri _uri(String path) {
    final norm = path.startsWith('/') ? path : '/$path';
    return Uri.parse('${AppConfig.baseUrl}/app-api/data-center$norm');
  }

  static Future<Map<String, String>> _headers() async {
    final cert = await ApiService.getFreshLoginCertificate();
    if (cert == null) {
      throw ApiException('用户未登录，请先登录');
    }
    final token = cert['accessToken']?.toString();
    if (token == null || token.isEmpty) {
      throw ApiException('登录凭证缺少 accessToken');
    }
    return {
      ...AppConfig.defaultHeaders,
      'Authorization': 'Bearer $token',
      'tenant-id': cert['tenantId']?.toString() ?? '',
    };
  }

  static Future<dynamic> _get(String path) async {
    final res = await http
        .get(_uri(path), headers: await _headers())
        .timeout(_requestTimeout);
    return _decode(res, path);
  }

  static Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    final res = await http
        .post(_uri(path),
            headers: {
              ...await _headers(),
              'Content-Type': 'application/json',
            },
            body: json.encode(body))
        .timeout(_requestTimeout);
    return _decode(res, path);
  }

  static Future<dynamic> _delete(String path) async {
    final res = await http
        .delete(_uri(path), headers: await _headers())
        .timeout(_requestTimeout);
    return _decode(res, path);
  }

  static dynamic _decode(http.Response res, String path) {
    if (res.statusCode >= 200 && res.statusCode < 300) {
      if (res.body.isEmpty) return null;
      try {
        return json.decode(utf8.decode(res.bodyBytes));
      } catch (_) {
        return null;
      }
    }
    // 后端错误结构: {"code":xxx,"msg":"..."}
    String msg = 'HTTP ${res.statusCode}';
    try {
      final b = json.decode(utf8.decode(res.bodyBytes));
      if (b is Map && b['msg'] != null) msg = b['msg'].toString();
    } catch (_) {}
    throw ApiException('$path 失败: $msg');
  }

  static Map<String, dynamic> _asMap(dynamic v) {
    if (v is Map) return Map<String, dynamic>.from(v);
    return <String, dynamic>{};
  }
}

// ── Models ─────────────────────────────────────────

class TemplateInfo {
  final String id;
  final String label;
  final String category;
  final String description;
  final String icon;
  final String color;
  final int maxRows;
  final Map<String, TemplateFilter> filters;
  final Map<String, Map<String, String>> enums;

  const TemplateInfo({
    required this.id,
    required this.label,
    required this.category,
    required this.description,
    required this.icon,
    required this.color,
    required this.maxRows,
    required this.filters,
    required this.enums,
  });

  factory TemplateInfo.fromJson(Map<String, dynamic> j) {
    final filters = <String, TemplateFilter>{};
    if (j['filters'] is Map) {
      (j['filters'] as Map).forEach((k, v) {
        filters[k.toString()] =
            TemplateFilter.fromJson(Map<String, dynamic>.from(v as Map));
      });
    }
    final enums = <String, Map<String, String>>{};
    if (j['enums'] is Map) {
      (j['enums'] as Map).forEach((k, v) {
        if (v is Map) {
          enums[k.toString()] = v.map(
              (kk, vv) => MapEntry(kk.toString(), vv.toString()));
        }
      });
    }
    return TemplateInfo(
      id: (j['id'] ?? '').toString(),
      label: (j['label'] ?? '').toString(),
      category: (j['category'] ?? '').toString(),
      description: (j['description'] ?? '').toString(),
      icon: (j['icon'] ?? '').toString(),
      color: (j['color'] ?? '#4C6EF5').toString(),
      maxRows: (j['max_rows'] is num) ? (j['max_rows'] as num).toInt() : 100000,
      filters: filters,
      enums: enums,
    );
  }
}

class TemplateFilter {
  final String type; // range / in / eq / like / lookup
  final String col;
  final bool required;
  final String? enumKey;
  final String? lookupName; // 命中即为可搜索多选下拉，走 /lookups/{name}
  final String label;

  const TemplateFilter({
    required this.type,
    required this.col,
    this.required = false,
    this.enumKey,
    this.lookupName,
    this.label = '',
  });

  factory TemplateFilter.fromJson(Map<String, dynamic> j) => TemplateFilter(
        type: (j['type'] ?? '').toString(),
        col: (j['col'] ?? '').toString(),
        required: j['required'] == true,
        enumKey: j['enum']?.toString(),
        lookupName: j['lookup']?.toString(),
        label: (j['label'] ?? '').toString(),
      );
}

/// 分页结果
class LookupPage {
  final List<LookupItem> items;
  final int offset;
  final int limit;
  final bool hasMore;
  const LookupPage({
    required this.items,
    required this.offset,
    required this.limit,
    required this.hasMore,
  });
}

/// Lookup item —— /lookups/{name} 返回的一条
class LookupItem {
  final String id;
  final String label;
  final String subLabel;
  const LookupItem({required this.id, required this.label, this.subLabel = ''});
  factory LookupItem.fromJson(Map<String, dynamic> j) => LookupItem(
        id: (j['id'] ?? '').toString(),
        label: (j['label'] ?? '').toString(),
        subLabel: (j['sub_label'] ?? '').toString(),
      );
}

class CreateJobResponse {
  final String jobId;
  final int queuePos;
  final int queueLen;

  const CreateJobResponse({
    required this.jobId,
    required this.queuePos,
    required this.queueLen,
  });

  factory CreateJobResponse.fromJson(Map<String, dynamic> j) => CreateJobResponse(
        jobId: (j['job_id'] ?? '').toString(),
        queuePos: (j['queue_pos'] is num) ? (j['queue_pos'] as num).toInt() : 0,
        queueLen: (j['queue_len'] is num) ? (j['queue_len'] as num).toInt() : 0,
      );
}

class JobInfo {
  final String id;
  final int ownerId;
  final String ownerName;
  final String templateId;
  final String templateLabel; // 服务端从当前 Registry 补的中文名；模板被删/改名时兜底为 templateId
  final String state; // pending / queued / running / done / failed / cancelled
  final int rowsWritten;
  final int? rowsTotal;
  final int? fileSize;
  final int? queuePos;
  final String? errorMsg;
  final DateTime createdAt;
  final DateTime? startedAt;
  final DateTime? finishedAt;

  const JobInfo({
    required this.id,
    required this.ownerId,
    required this.ownerName,
    required this.templateId,
    required this.templateLabel,
    required this.state,
    required this.rowsWritten,
    this.rowsTotal,
    this.fileSize,
    this.queuePos,
    this.errorMsg,
    required this.createdAt,
    this.startedAt,
    this.finishedAt,
  });

  bool get isDone => state == 'done';
  bool get isFailed => state == 'failed';
  bool get isCancelled => state == 'cancelled';
  bool get isFinal => isDone || isFailed || isCancelled;

  /// UI 显示用：优先中文 label，兜底 template_id
  String get displayName {
    final l = templateLabel.trim();
    return l.isEmpty ? templateId : l;
  }

  factory JobInfo.fromJson(Map<String, dynamic> j) {
    DateTime? dt(dynamic v) {
      if (v == null) return null;
      final s = v.toString();
      if (s.isEmpty) return null;
      return DateTime.tryParse(s);
    }

    final tid = (j['template_id'] ?? '').toString();
    return JobInfo(
      id: (j['id'] ?? '').toString(),
      ownerId: (j['owner_id'] is num) ? (j['owner_id'] as num).toInt() : 0,
      ownerName: (j['owner_name'] ?? '').toString(),
      templateId: tid,
      templateLabel: (j['template_label']?.toString().trim().isNotEmpty ?? false)
          ? j['template_label'].toString()
          : tid,
      state: (j['state'] ?? 'pending').toString(),
      rowsWritten:
          (j['rows_written'] is num) ? (j['rows_written'] as num).toInt() : 0,
      rowsTotal:
          (j['rows_total'] is num) ? (j['rows_total'] as num).toInt() : null,
      fileSize:
          (j['file_size'] is num) ? (j['file_size'] as num).toInt() : null,
      queuePos:
          (j['queue_pos'] is num) ? (j['queue_pos'] as num).toInt() : null,
      errorMsg: j['error_msg']?.toString(),
      createdAt: dt(j['created_at']) ?? DateTime.now(),
      startedAt: dt(j['started_at']),
      finishedAt: dt(j['finished_at']),
    );
  }
}

class HallSnapshot {
  final int queueLen;
  final int workerCap;
  final List<JobInfo> activeJobs;

  const HallSnapshot({
    required this.queueLen,
    required this.workerCap,
    required this.activeJobs,
  });

  factory HallSnapshot.fromJson(Map<String, dynamic> j) {
    final list = <JobInfo>[];
    if (j['active_jobs'] is List) {
      for (final e in j['active_jobs'] as List) {
        if (e is Map) list.add(JobInfo.fromJson(Map<String, dynamic>.from(e)));
      }
    }
    return HallSnapshot(
      queueLen: (j['queue_len'] is num) ? (j['queue_len'] as num).toInt() : 0,
      workerCap:
          (j['worker_cap'] is num) ? (j['worker_cap'] as num).toInt() : 0,
      activeJobs: list,
    );
  }
}
