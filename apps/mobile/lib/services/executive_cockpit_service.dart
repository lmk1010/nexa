import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class ExecutiveCockpitService {
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Future<ExecutiveCockpitOverview> getOverview({int days = 30}) async {
    final data = await _get('/executive-cockpit/overview', {'days': days});
    return ExecutiveCockpitOverview.fromJson(_asMap(data));
  }

  static Future<ExecutiveCockpitChatReply> chat({
    required String message,
    String? conversationId,
    int days = 30,
  }) async {
    final data = await _post('/executive-cockpit/chat', {
      'message': message,
      'conversationId': conversationId,
      'rangeDays': days,
    });
    return ExecutiveCockpitChatReply.fromJson(_asMap(data));
  }

  // 数仓 warehouse.agg_sh_pf_daily 直查 → <100ms 出结果
  // 不走 agent/LLM 推理，纯 SQL 聚合
  // 通用大盘响应（order/work/attribution 3 个共享的形态）
  // range: today / 7d / 30d / 365d，默认 30d
  static Future<DashboardOverview> getDashboard(String path, {String range = '30d'}) async {
    final data = await _get('$path?range=$range');
    return DashboardOverview.fromJson(_asMap(data));
  }

  // 赔付大盘：range = today / 7d / 30d / 365d，默认 today
  // 老 month=yyyy-MM 参数后端仍兼容（历史 APP 版本）
  static Future<PfMonthlyOverview> getPfMonthly({String range = 'today'}) async {
    final data = await _get('/dashboard/pf-monthly?range=$range');
    return PfMonthlyOverview.fromJson(_asMap(data));
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

  static Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    final response = await _sendWithRetry(
      () async => http
          .post(
            _agentUri(path),
            headers: await _authorizedHeaders(),
            body: json.encode(_compactMap(body)),
          )
          .timeout(_requestTimeout),
    );
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

bool _isTransientNetworkError(Object error) {
  if (error is TimeoutException || error is SocketException) {
    return true;
  }
  if (error is http.ClientException) {
    return true;
  }
  final message = error.toString().toLowerCase();
  return message.contains('connection abort') ||
      message.contains('connection reset') ||
      message.contains('connection closed') ||
      message.contains('broken pipe') ||
      message.contains('timed out');
}

String _networkErrorMessage(Object? error) {
  if (error is ApiException) {
    return error.message;
  }
  if (error is TimeoutException) {
    return '总裁驾驶舱请求超时，请稍后重试';
  }
  if (error is SocketException || error is http.ClientException) {
    return '网络连接中断，请检查网络后重试';
  }
  final message = error?.toString() ?? '';
  if (message.toLowerCase().contains('connection abort')) {
    return '网络连接中断，请检查网络后重试';
  }
  return message.replaceFirst(RegExp(r'^Exception:\s*'), '');
}

class ExecutiveCockpitOverview {
  final DateTime? generatedAt;
  final int rangeDays;
  final bool ordersysAvailable;
  final String ordersysMessage;
  final List<ExecutiveCockpitMetric> metrics;
  final List<ExecutiveCockpitTrendPoint> requirementTrend;
  final List<ExecutiveCockpitStatusCount> requirementStatus;
  final List<ExecutiveCockpitWorkload> workload;
  final List<ExecutiveCockpitRisk> risks;
  final List<ExecutiveCockpitRecentTask> recentOrdersysTasks;

  const ExecutiveCockpitOverview({
    required this.generatedAt,
    required this.rangeDays,
    required this.ordersysAvailable,
    required this.ordersysMessage,
    required this.metrics,
    required this.requirementTrend,
    required this.requirementStatus,
    required this.workload,
    required this.risks,
    required this.recentOrdersysTasks,
  });

  factory ExecutiveCockpitOverview.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitOverview(
      generatedAt: _parseDateTime(json['generatedAt']),
      rangeDays: _parseInt(json['rangeDays']) ?? 30,
      ordersysAvailable: _parseBool(json['ordersysAvailable']),
      ordersysMessage: _stringValue(json['ordersysMessage']) ?? '',
      metrics: _asList(
        json['metrics'],
      ).map((item) => ExecutiveCockpitMetric.fromJson(_asMap(item))).toList(),
      requirementTrend: _asList(json['requirementTrend'])
          .map((item) => ExecutiveCockpitTrendPoint.fromJson(_asMap(item)))
          .toList(),
      requirementStatus: _asList(json['requirementStatus'])
          .map((item) => ExecutiveCockpitStatusCount.fromJson(_asMap(item)))
          .toList(),
      workload: _asList(
        json['workload'],
      ).map((item) => ExecutiveCockpitWorkload.fromJson(_asMap(item))).toList(),
      risks: _asList(
        json['risks'],
      ).map((item) => ExecutiveCockpitRisk.fromJson(_asMap(item))).toList(),
      recentOrdersysTasks: _asList(json['recentOrdersysTasks'])
          .map((item) => ExecutiveCockpitRecentTask.fromJson(_asMap(item)))
          .toList(),
    );
  }

  ExecutiveCockpitMetric? metric(String code) {
    for (final item in metrics) {
      if (item.code == code) return item;
    }
    return null;
  }
}

class ExecutiveCockpitMetric {
  final String code;
  final String title;
  final String value;
  final String unit;
  final int? numericValue;
  final String description;
  final String trendLabel;
  final String tone;

  const ExecutiveCockpitMetric({
    required this.code,
    required this.title,
    required this.value,
    required this.unit,
    required this.numericValue,
    required this.description,
    required this.trendLabel,
    required this.tone,
  });

  factory ExecutiveCockpitMetric.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitMetric(
      code: _stringValue(json['code']) ?? '',
      title: _stringValue(json['title']) ?? '',
      value: _stringValue(json['value']) ?? '--',
      unit: _stringValue(json['unit']) ?? '',
      numericValue: _parseInt(json['numericValue']),
      description: _stringValue(json['description']) ?? '',
      trendLabel: _stringValue(json['trendLabel']) ?? '',
      tone: _stringValue(json['tone']) ?? 'blue',
    );
  }
}

class ExecutiveCockpitTrendPoint {
  final String date;
  final int createdCount;
  final int finishedCount;

  const ExecutiveCockpitTrendPoint({
    required this.date,
    required this.createdCount,
    required this.finishedCount,
  });

  factory ExecutiveCockpitTrendPoint.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitTrendPoint(
      date: _stringValue(json['date']) ?? '',
      createdCount: _parseInt(json['createdCount']) ?? 0,
      finishedCount: _parseInt(json['finishedCount']) ?? 0,
    );
  }
}

class ExecutiveCockpitStatusCount {
  final int? status;
  final String label;
  final int count;
  final String tone;

  const ExecutiveCockpitStatusCount({
    required this.status,
    required this.label,
    required this.count,
    required this.tone,
  });

  factory ExecutiveCockpitStatusCount.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitStatusCount(
      status: _parseInt(json['status']),
      label: _stringValue(json['label']) ?? '未知',
      count: _parseInt(json['count']) ?? 0,
      tone: _stringValue(json['tone']) ?? 'blue',
    );
  }
}

class ExecutiveCockpitWorkload {
  final int? assigneeUserId;
  final String assigneeName;
  final int totalCount;
  final int openCount;
  final int overdueCount;

  const ExecutiveCockpitWorkload({
    required this.assigneeUserId,
    required this.assigneeName,
    required this.totalCount,
    required this.openCount,
    required this.overdueCount,
  });

  factory ExecutiveCockpitWorkload.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitWorkload(
      assigneeUserId: _parseInt(json['assigneeUserId']),
      assigneeName: _stringValue(json['assigneeName']) ?? '未分派',
      totalCount: _parseInt(json['totalCount']) ?? 0,
      openCount: _parseInt(json['openCount']) ?? 0,
      overdueCount: _parseInt(json['overdueCount']) ?? 0,
    );
  }
}

class ExecutiveCockpitRisk {
  final String source;
  final int? id;
  final String title;
  final int? status;
  final String statusLabel;
  final int? priority;
  final String priorityLabel;
  final String assigneeName;
  final DateTime? expectedFinishDate;
  final DateTime? updateTime;
  final String riskReason;

  const ExecutiveCockpitRisk({
    required this.source,
    required this.id,
    required this.title,
    required this.status,
    required this.statusLabel,
    required this.priority,
    required this.priorityLabel,
    required this.assigneeName,
    required this.expectedFinishDate,
    required this.updateTime,
    required this.riskReason,
  });

  factory ExecutiveCockpitRisk.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitRisk(
      source: _stringValue(json['source']) ?? 'OA',
      id: _parseInt(json['id']),
      title: _stringValue(json['title']) ?? '未命名需求',
      status: _parseInt(json['status']),
      statusLabel: _stringValue(json['statusLabel']) ?? '未知',
      priority: _parseInt(json['priority']),
      priorityLabel: _stringValue(json['priorityLabel']) ?? '未知',
      assigneeName: _stringValue(json['assigneeName']) ?? '未分派',
      expectedFinishDate: _parseDateTime(json['expectedFinishDate']),
      updateTime: _parseDateTime(json['updateTime']),
      riskReason: _stringValue(json['riskReason']) ?? '需要关注',
    );
  }
}

class ExecutiveCockpitRecentTask {
  final String source;
  final int? id;
  final String title;
  final String status;
  final String statusLabel;
  final String operatorName;
  final String assigneeName;
  final DateTime? taskTime;
  final DateTime? updateTime;

  const ExecutiveCockpitRecentTask({
    required this.source,
    required this.id,
    required this.title,
    required this.status,
    required this.statusLabel,
    required this.operatorName,
    required this.assigneeName,
    required this.taskTime,
    required this.updateTime,
  });

  factory ExecutiveCockpitRecentTask.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitRecentTask(
      source: _stringValue(json['source']) ?? 'ordersys',
      id: _parseInt(json['id']),
      title: _stringValue(json['title']) ?? '未命名工单',
      status: _stringValue(json['status']) ?? '',
      statusLabel: _stringValue(json['statusLabel']) ?? '未知',
      operatorName: _stringValue(json['operatorName']) ?? '',
      assigneeName: _stringValue(json['assigneeName']) ?? '',
      taskTime: _parseDateTime(json['taskTime']),
      updateTime: _parseDateTime(json['updateTime']),
    );
  }
}

class ExecutiveCockpitChatReply {
  final String conversationId;
  final String reply;
  final List<String> suggestions;
  final List<ExecutiveCockpitChartHint> chartHints;
  final ExecutiveCockpitOverview? snapshot;

  const ExecutiveCockpitChatReply({
    required this.conversationId,
    required this.reply,
    required this.suggestions,
    required this.chartHints,
    required this.snapshot,
  });

  factory ExecutiveCockpitChatReply.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitChatReply(
      conversationId: _stringValue(json['conversationId']) ?? '',
      reply: _stringValue(json['reply']) ?? '',
      suggestions: _asList(json['suggestions'])
          .map((item) => item.toString())
          .where((item) => item.trim().isNotEmpty)
          .toList(),
      chartHints: _asList(json['chartHints'])
          .map((item) => ExecutiveCockpitChartHint.fromJson(_asMap(item)))
          .toList(),
      snapshot: json['snapshot'] == null
          ? null
          : ExecutiveCockpitOverview.fromJson(_asMap(json['snapshot'])),
    );
  }
}

class ExecutiveCockpitChartHint {
  final String type;
  final String title;
  final String dataKey;

  const ExecutiveCockpitChartHint({
    required this.type,
    required this.title,
    required this.dataKey,
  });

  factory ExecutiveCockpitChartHint.fromJson(Map<String, dynamic> json) {
    return ExecutiveCockpitChartHint(
      type: _stringValue(json['type']) ?? '',
      title: _stringValue(json['title']) ?? '',
      dataKey: _stringValue(json['dataKey']) ?? '',
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

Map<String, dynamic> _compactMap(Map<String, dynamic> source) {
  final result = <String, dynamic>{};
  for (final entry in source.entries) {
    final value = entry.value;
    if (value == null) continue;
    if (value is String && value.trim().isEmpty) continue;
    if (value is List && value.isEmpty) continue;
    result[entry.key] = value;
  }
  return result;
}

String? _stringValue(dynamic value) {
  final string = value?.toString().trim();
  if (string == null || string.isEmpty || string == 'null') return null;
  return string;
}

int? _parseInt(dynamic value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString());
}

bool _parseBool(dynamic value) {
  if (value is bool) return value;
  if (value is num) return value != 0;
  if (value is String) {
    final normalized = value.toLowerCase();
    return normalized == 'true' || normalized == '1';
  }
  return false;
}

DateTime? _parseDateTime(dynamic value) {
  if (value == null) return null;
  if (value is List && value.length >= 3) {
    final parts = value.map(_parseInt).whereType<int>().toList();
    if (parts.length >= 3) {
      return DateTime(
        parts[0],
        parts[1],
        parts[2],
        parts.length > 3 ? parts[3] : 0,
        parts.length > 4 ? parts[4] : 0,
        parts.length > 5 ? parts[5] : 0,
      );
    }
  }
  final intValue = _parseInt(value);
  if (intValue != null) {
    if (intValue > 999999999999) {
      return DateTime.fromMillisecondsSinceEpoch(intValue);
    }
    if (intValue > 1000000000) {
      return DateTime.fromMillisecondsSinceEpoch(intValue * 1000);
    }
  }
  final text = value.toString().trim();
  if (text.isEmpty) return null;
  final normalized = text.contains(' ') && !text.contains('T')
      ? text.replaceFirst(' ', 'T')
      : text;
  return DateTime.tryParse(normalized);
}

// ── 赔付月度概览（数仓 agg_sh_pf_daily 直查） ─────────────────────

// 通用大盘响应 —— order/work/attribution 3 个新大盘共享的形态
// total: {k: v} 任意 KPI；dept_ranking: 部门排行；trend: 日趋势
class DashboardOverview {
  final String month;
  final Map<String, num> total;
  final List<DashDeptRow> deptRanking;
  final List<DashTrendRow> trend;
  final DateTime? lastRefresh;
  final String source;

  const DashboardOverview({
    required this.month,
    required this.total,
    required this.deptRanking,
    required this.trend,
    required this.lastRefresh,
    required this.source,
  });

  factory DashboardOverview.fromJson(Map<String, dynamic> j) {
    final tot = <String, num>{};
    (j['total'] as Map?)?.forEach((k, v) {
      if (v is num) tot[k.toString()] = v;
    });
    return DashboardOverview(
      month: (j['month'] ?? '').toString(),
      total: tot,
      deptRanking: (j['dept_ranking'] as List? ?? const [])
          .whereType<Map>()
          .map((e) => DashDeptRow.fromJson(Map<String, dynamic>.from(e)))
          .toList(),
      trend: (j['trend'] as List? ?? const [])
          .whereType<Map>()
          .map((e) => DashTrendRow.fromJson(Map<String, dynamic>.from(e)))
          .toList(),
      lastRefresh: DateTime.tryParse(j['last_refresh']?.toString() ?? ''),
      source: (j['source'] ?? '').toString(),
    );
  }
}

class DashDeptRow {
  final int deptId;
  final String deptName;
  final Map<String, num> metrics;
  const DashDeptRow({required this.deptId, required this.deptName, required this.metrics});
  factory DashDeptRow.fromJson(Map<String, dynamic> j) {
    final m = <String, num>{};
    j.forEach((k, v) {
      if (v is num) m[k.toString()] = v;
    });
    return DashDeptRow(
      deptId: (j['dept_id'] is num) ? (j['dept_id'] as num).toInt() : 0,
      deptName: (j['dept_name'] ?? '').toString(),
      metrics: m,
    );
  }
}

class DashTrendRow {
  final String ymd;
  final Map<String, num> values;
  const DashTrendRow({required this.ymd, required this.values});
  factory DashTrendRow.fromJson(Map<String, dynamic> j) {
    final m = <String, num>{};
    j.forEach((k, v) {
      if (v is num) m[k.toString()] = v;
    });
    return DashTrendRow(ymd: (j['ymd'] ?? '').toString(), values: m);
  }
}

class PfMonthlyOverview {
  final String month; // yyyy-MM
  final PfMonthlyTotal total;
  final List<PfDeptRank> deptRanking;
  final List<PfTrendPoint> trend;
  final DateTime? lastRefresh;

  const PfMonthlyOverview({
    required this.month,
    required this.total,
    required this.deptRanking,
    required this.trend,
    required this.lastRefresh,
  });

  factory PfMonthlyOverview.fromJson(Map<String, dynamic> j) =>
      PfMonthlyOverview(
        month: (j['month'] ?? '').toString(),
        total: PfMonthlyTotal.fromJson(_asMapStatic(j['total'])),
        deptRanking: (j['dept_ranking'] as List? ?? const [])
            .whereType<Map>()
            .map((e) => PfDeptRank.fromJson(Map<String, dynamic>.from(e)))
            .toList(),
        trend: (j['trend'] as List? ?? const [])
            .whereType<Map>()
            .map((e) => PfTrendPoint.fromJson(Map<String, dynamic>.from(e)))
            .toList(),
        lastRefresh: _parseDateTime(j['last_refresh']),
      );

  static Map<String, dynamic> _asMapStatic(dynamic v) =>
      v is Map ? Map<String, dynamic>.from(v) : <String, dynamic>{};
}

class PfMonthlyTotal {
  final int cnt;
  final double totalMoney;
  final double myMoney;
  final double outletsMoney;
  final double sellerMoney;
  final double bondMoney;
  final int urgentCnt;

  const PfMonthlyTotal({
    required this.cnt,
    required this.totalMoney,
    required this.myMoney,
    required this.outletsMoney,
    required this.sellerMoney,
    required this.bondMoney,
    required this.urgentCnt,
  });

  factory PfMonthlyTotal.fromJson(Map<String, dynamic> j) => PfMonthlyTotal(
    cnt: _pfParseInt(j['cnt']) ?? 0,
    totalMoney: _pfParseDouble(j['total_money']),
    myMoney: _pfParseDouble(j['my_money']),
    outletsMoney: _pfParseDouble(j['outlets_money']),
    sellerMoney: _pfParseDouble(j['seller_money']),
    bondMoney: _pfParseDouble(j['bond_money']),
    urgentCnt: _pfParseInt(j['urgent_cnt']) ?? 0,
  );
}

class PfDeptRank {
  final int deptId;
  final String deptName;
  final int cnt;
  final double totalMoney;
  final double myMoney;
  final double outletsMoney;
  final double sellerMoney;

  const PfDeptRank({
    required this.deptId,
    required this.deptName,
    required this.cnt,
    required this.totalMoney,
    required this.myMoney,
    required this.outletsMoney,
    required this.sellerMoney,
  });

  factory PfDeptRank.fromJson(Map<String, dynamic> j) => PfDeptRank(
    deptId: _pfParseInt(j['dept_id']) ?? 0,
    deptName: (j['dept_name'] ?? '').toString(),
    cnt: _pfParseInt(j['cnt']) ?? 0,
    totalMoney: _pfParseDouble(j['total_money']),
    myMoney: _pfParseDouble(j['my_money']),
    outletsMoney: _pfParseDouble(j['outlets_money']),
    sellerMoney: _pfParseDouble(j['seller_money']),
  );
}

class PfTrendPoint {
  final String ymd; // yyyy-MM-dd
  final int cnt;
  final double myMoney;
  final double outletsMoney;
  final double sellerMoney;

  const PfTrendPoint({
    required this.ymd,
    required this.cnt,
    required this.myMoney,
    required this.outletsMoney,
    required this.sellerMoney,
  });

  factory PfTrendPoint.fromJson(Map<String, dynamic> j) => PfTrendPoint(
    ymd: (j['ymd'] ?? '').toString(),
    cnt: _pfParseInt(j['cnt']) ?? 0,
    myMoney: _pfParseDouble(j['my_money']),
    outletsMoney: _pfParseDouble(j['outlets_money']),
    sellerMoney: _pfParseDouble(j['seller_money']),
  );
}

int? _pfParseInt(dynamic v) {
  if (v == null) return null;
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v.toString());
}

double _pfParseDouble(dynamic v) {
  if (v == null) return 0;
  if (v is num) return v.toDouble();
  return double.tryParse(v.toString()) ?? 0;
}
