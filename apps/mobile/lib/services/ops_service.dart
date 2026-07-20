// 拉运维监控数据 — 只给 admin 用（后端 /ops/canal-status 会做白名单校验）
import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class OpsService {
  static const Duration _timeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Future<CanalStatus> getCanalStatus() async {
    final data = await _get('/ops/canal-status');
    return CanalStatus.fromJson(_asMap(data));
  }

  static Future<AuditAnalytics> getAuditAnalytics({int hours = 24}) async {
    final data = await _get('/ops/audit-analytics?hours=$hours');
    return AuditAnalytics.fromJson(_asMap(data));
  }

  static Future<CanalErrorsPage> getCanalErrorsPage({
    int pageNo = 1,
    int pageSize = 10,
    int hours = 24,
  }) async {
    final data =
        await _get('/ops/canal-errors?pageNo=$pageNo&pageSize=$pageSize&hours=$hours');
    return CanalErrorsPage.fromJson(_asMap(data));
  }

  // 实时速率 + 压力指标 —— 前端 3s poll 一次，后端会按上次 poll 差值算 rate
  static Future<CanalRates> getCanalRates() async {
    final data = await _get('/ops/canal-rates');
    return CanalRates.fromJson(_asMap(data));
  }

  // DDL 变更审计 —— warehouse._canal_ddl_applied 分页
  static Future<DdlAuditPage> getDdlAudit({
    int pageNo = 1,
    int pageSize = 20,
    int hours = 168,
    String? status,
  }) async {
    final qs = <String>[
      'pageNo=$pageNo',
      'pageSize=$pageSize',
      'hours=$hours',
      if (status != null && status.isNotEmpty) 'status=$status',
    ].join('&');
    final data = await _get('/ops/canal-ddl-audit?$qs');
    return DdlAuditPage.fromJson(_asMap(data));
  }

  static Future<dynamic> _get(String path) async {
    final response = await http
        .get(_uri(path), headers: await _authHeaders())
        .timeout(_timeout);
    return _extractData(response, path);
  }

  static Uri _uri(String path) {
    final normalized = path.startsWith('/') ? path : '/$path';
    return Uri.parse('${AppConfig.baseUrl}/app-api/agent$normalized');
  }

  static Future<Map<String, String>> _authHeaders() async {
    final cert = await ApiService.getFreshLoginCertificate();
    if (cert == null) throw ApiException('未登录');
    final token =
        cert['accessToken']?.toString() ??
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
    if (tenantId != null && tenantId.isNotEmpty) {
      headers['tenant-id'] = tenantId;
    }
    return headers;
  }

  static dynamic _extractData(http.Response response, String apiName) {
    if (response.statusCode == 401) {
      throw ApiException('登录已过期');
    }
    if (response.statusCode == 403) {
      throw ApiException('无权访问运维监控');
    }
    if (response.statusCode != 200) {
      throw ApiException('接口失败($apiName): ${response.statusCode}');
    }
    final body = json.decode(utf8.decode(response.bodyBytes));
    if (body is! Map) throw ApiException('响应格式错误');
    final map = Map<String, dynamic>.from(body);
    final code = map['code'];
    if (code == 0 || code == '0') return map['data'];
    throw ApiException(map['msg']?.toString() ?? '未知错误');
  }

  static Map<String, dynamic> _asMap(dynamic v) =>
      v is Map ? Map<String, dynamic>.from(v) : <String, dynamic>{};
}

class CanalStatus {
  final DateTime? generatedAt;
  final EndpointStatus canalServer;
  final ConsumerStatus consumer;
  final EndpointStatus warehouse;
  final MasterPosition? cdbMaster;
  final List<TableStatus> tables;
  final List<CanalError> recentErrors;

  const CanalStatus({
    required this.generatedAt,
    required this.canalServer,
    required this.consumer,
    required this.warehouse,
    required this.cdbMaster,
    required this.tables,
    required this.recentErrors,
  });

  factory CanalStatus.fromJson(Map<String, dynamic> j) => CanalStatus(
    generatedAt: DateTime.tryParse(j['generated_at']?.toString() ?? ''),
    canalServer: EndpointStatus.fromJson(_m(j['canal_server'])),
    consumer: ConsumerStatus.fromJson(_m(j['consumer'])),
    warehouse: EndpointStatus.fromJson(_m(j['warehouse'])),
    cdbMaster: j['cdb_master'] is Map
        ? MasterPosition.fromJson(_m(j['cdb_master']))
        : null,
    tables: (j['tables'] as List? ?? const [])
        .whereType<Map>()
        .map((t) => TableStatus.fromJson(Map<String, dynamic>.from(t)))
        .toList(),
    recentErrors: (j['recent_errors'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => CanalError.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
  );

  static Map<String, dynamic> _m(dynamic v) =>
      v is Map ? Map<String, dynamic>.from(v) : <String, dynamic>{};
}

class EndpointStatus {
  final bool up;
  final String host;
  const EndpointStatus({required this.up, required this.host});
  factory EndpointStatus.fromJson(Map<String, dynamic> j) => EndpointStatus(
    up: j['up'] == true,
    host: (j['host'] ?? '').toString(),
  );
}

class ConsumerStatus {
  final bool up;
  final DateTime? lastHeartbeatAt;
  final int? idleSeconds;
  const ConsumerStatus({
    required this.up,
    required this.lastHeartbeatAt,
    required this.idleSeconds,
  });
  factory ConsumerStatus.fromJson(Map<String, dynamic> j) => ConsumerStatus(
    up: j['up'] == true,
    lastHeartbeatAt: DateTime.tryParse(
      j['last_heartbeat_at']?.toString() ?? '',
    ),
    idleSeconds: _intOrNull(j['idle_seconds']),
  );
}

class MasterPosition {
  final String? file;
  final int? position;
  final String? gtidSet;
  final String? error;
  const MasterPosition({
    required this.file,
    required this.position,
    required this.gtidSet,
    required this.error,
  });
  factory MasterPosition.fromJson(Map<String, dynamic> j) => MasterPosition(
    file: j['file']?.toString(),
    position: _intOrNull(j['position']),
    gtidSet: j['gtid_set']?.toString(),
    error: j['error']?.toString(),
  );
}

class TableStatus {
  final String name;
  final int? rows;
  final double? sizeMb;         // 表磁盘占用 MB（data+index）
  final int ins24h;
  final int upd24h;
  final int del24h;
  final int err24h;
  final int total24h;            // ins+upd+del+err = 流量
  final double errorRate;        // 错误率 % (0-100)
  final DateTime? lastApplyAt;
  final String? error;

  const TableStatus({
    required this.name,
    required this.rows,
    required this.sizeMb,
    required this.ins24h,
    required this.upd24h,
    required this.del24h,
    required this.err24h,
    required this.total24h,
    required this.errorRate,
    required this.lastApplyAt,
    required this.error,
  });

  factory TableStatus.fromJson(Map<String, dynamic> j) {
    final ops = j['ops_24h'] as Map? ?? {};
    return TableStatus(
      name: (j['name'] ?? '').toString(),
      rows: _intOrNull(j['rows']),
      sizeMb: (j['size_mb'] as num?)?.toDouble(),
      ins24h: _intOrNull(ops['ins']) ?? 0,
      upd24h: _intOrNull(ops['upd']) ?? 0,
      del24h: _intOrNull(ops['del']) ?? 0,
      err24h: _intOrNull(ops['err']) ?? 0,
      total24h: _intOrNull(ops['total']) ?? 0,
      errorRate: (j['error_rate'] as num?)?.toDouble() ?? 0,
      lastApplyAt: DateTime.tryParse(j['last_apply_at']?.toString() ?? ''),
      error: j['error']?.toString(),
    );
  }
}

class CanalError {
  final DateTime? ts;
  final String? tableName;
  final String? eventType;
  final String? rowId;
  final String? errMsg;
  const CanalError({
    required this.ts,
    required this.tableName,
    required this.eventType,
    required this.rowId,
    required this.errMsg,
  });
  factory CanalError.fromJson(Map<String, dynamic> j) => CanalError(
    ts: DateTime.tryParse(j['ts']?.toString() ?? ''),
    tableName: j['table_name']?.toString(),
    eventType: j['event_type']?.toString(),
    rowId: j['row_id']?.toString(),
    errMsg: j['err_msg']?.toString(),
  );
}

class CanalErrorsPage {
  final int pageNo;
  final int pageSize;
  final int total;
  final bool hasMore;
  final List<CanalError> list;
  const CanalErrorsPage({
    required this.pageNo,
    required this.pageSize,
    required this.total,
    required this.hasMore,
    required this.list,
  });
  factory CanalErrorsPage.fromJson(Map<String, dynamic> j) => CanalErrorsPage(
    pageNo: _intOrNull(j['pageNo']) ?? 1,
    pageSize: _intOrNull(j['pageSize']) ?? 10,
    total: _intOrNull(j['total']) ?? 0,
    hasMore: j['hasMore'] == true,
    list: (j['list'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => CanalError.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
  );
}

int? _intOrNull(dynamic v) {
  if (v == null) return null;
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v.toString());
}

// ── 实时速率 ───────────────────────────────────────────────

class CanalRates {
  final DateTime? generatedAt;
  final bool available;
  final String? reason;
  final double? elapsedSec;
  final bool connected;
  final CanalAggregateRates aggregate;
  final Map<String, CanalTableRate> byTable; // key = target table name
  final CanalCumulative cumulative;
  final CanalPressure pressure;

  const CanalRates({
    required this.generatedAt,
    required this.available,
    required this.reason,
    required this.elapsedSec,
    required this.connected,
    required this.aggregate,
    required this.byTable,
    required this.cumulative,
    required this.pressure,
  });

  factory CanalRates.fromJson(Map<String, dynamic> j) {
    final rbt = <String, CanalTableRate>{};
    final rawRbt = j['rates_by_table'];
    if (rawRbt is Map) {
      rawRbt.forEach((k, v) {
        if (v is Map) rbt[k.toString()] = CanalTableRate.fromJson(Map<String, dynamic>.from(v));
      });
    }
    return CanalRates(
      generatedAt: DateTime.tryParse(j['generated_at']?.toString() ?? ''),
      available: j['available'] == true,
      reason: j['reason']?.toString(),
      elapsedSec: (j['elapsed_sec'] as num?)?.toDouble(),
      connected: j['connected'] == true,
      aggregate: CanalAggregateRates.fromJson(_m(j['aggregate_rates'])),
      byTable: rbt,
      cumulative: CanalCumulative.fromJson(_m(j['cumulative'])),
      pressure: CanalPressure.fromJson(_m(j['pressure'])),
    );
  }

  static Map<String, dynamic> _m(dynamic v) =>
      v is Map ? Map<String, dynamic>.from(v) : <String, dynamic>{};
}

class CanalAggregateRates {
  final double ins;
  final double upd;
  final double del;
  final double err;
  final double total;
  final double batches;
  const CanalAggregateRates({
    required this.ins,
    required this.upd,
    required this.del,
    required this.err,
    required this.total,
    required this.batches,
  });
  factory CanalAggregateRates.fromJson(Map<String, dynamic> j) => CanalAggregateRates(
        ins: (j['ins'] as num?)?.toDouble() ?? 0,
        upd: (j['upd'] as num?)?.toDouble() ?? 0,
        del: (j['del'] as num?)?.toDouble() ?? 0,
        err: (j['err'] as num?)?.toDouble() ?? 0,
        total: (j['total'] as num?)?.toDouble() ?? 0,
        batches: (j['batches'] as num?)?.toDouble() ?? 0,
      );
}

class CanalTableRate {
  final double ins;
  final double upd;
  final double del;
  final double err;
  final double total;
  final int cumIns;
  final int cumUpd;
  final int cumDel;
  final int cumErr;
  const CanalTableRate({
    required this.ins,
    required this.upd,
    required this.del,
    required this.err,
    required this.total,
    required this.cumIns,
    required this.cumUpd,
    required this.cumDel,
    required this.cumErr,
  });
  factory CanalTableRate.fromJson(Map<String, dynamic> j) => CanalTableRate(
        ins: (j['ins'] as num?)?.toDouble() ?? 0,
        upd: (j['upd'] as num?)?.toDouble() ?? 0,
        del: (j['del'] as num?)?.toDouble() ?? 0,
        err: (j['err'] as num?)?.toDouble() ?? 0,
        total: (j['total'] as num?)?.toDouble() ?? 0,
        cumIns: _intOrNull(j['cum_ins']) ?? 0,
        cumUpd: _intOrNull(j['cum_upd']) ?? 0,
        cumDel: _intOrNull(j['cum_del']) ?? 0,
        cumErr: _intOrNull(j['cum_err']) ?? 0,
      );
}

class CanalCumulative {
  final int ins;
  final int upd;
  final int del;
  final int err;
  final int total;
  final int batches;
  final int reconnects;
  const CanalCumulative({
    required this.ins,
    required this.upd,
    required this.del,
    required this.err,
    required this.total,
    required this.batches,
    required this.reconnects,
  });
  factory CanalCumulative.fromJson(Map<String, dynamic> j) => CanalCumulative(
        ins: _intOrNull(j['ins']) ?? 0,
        upd: _intOrNull(j['upd']) ?? 0,
        del: _intOrNull(j['del']) ?? 0,
        err: _intOrNull(j['err']) ?? 0,
        total: _intOrNull(j['total']) ?? 0,
        batches: _intOrNull(j['batches']) ?? 0,
        reconnects: _intOrNull(j['reconnects']) ?? 0,
      );
}

class CanalPressure {
  final int? binlogPos;
  final String? binlogFile;
  final int? sourceBinlogPos;
  final String? sourceBinlogFile;
  final int? lagBytes;
  final String? lagHuman;
  final String? lagWarn;
  final double? avgBatchSize;
  final double errorRatePct;
  final int reconnectsTotal;
  final DateTime? lastBatchAt;
  final DateTime? startedAt;
  const CanalPressure({
    required this.binlogPos,
    required this.binlogFile,
    required this.sourceBinlogPos,
    required this.sourceBinlogFile,
    required this.lagBytes,
    required this.lagHuman,
    required this.lagWarn,
    required this.avgBatchSize,
    required this.errorRatePct,
    required this.reconnectsTotal,
    required this.lastBatchAt,
    required this.startedAt,
  });
  factory CanalPressure.fromJson(Map<String, dynamic> j) => CanalPressure(
        binlogPos: _intOrNull(j['binlog_pos']),
        binlogFile: j['binlog_file']?.toString(),
        sourceBinlogPos: _intOrNull(j['source_binlog_pos']),
        sourceBinlogFile: j['source_binlog_file']?.toString(),
        lagBytes: _intOrNull(j['lag_bytes']),
        lagHuman: j['lag_human']?.toString(),
        lagWarn: j['lag_warn']?.toString(),
        avgBatchSize: (j['avg_batch_size'] as num?)?.toDouble(),
        errorRatePct: (j['error_rate_pct'] as num?)?.toDouble() ?? 0,
        reconnectsTotal: _intOrNull(j['reconnects_total']) ?? 0,
        lastBatchAt: DateTime.tryParse(j['last_batch_at']?.toString() ?? ''),
        startedAt: DateTime.tryParse(j['started_at']?.toString() ?? ''),
      );
}

// ── DDL 变更审计 ───────────────────────────────────────────────

class DdlAuditPage {
  final int pageNo;
  final int pageSize;
  final int total;
  final bool hasMore;
  final int windowHours;
  final String? status;
  final DdlAuditDistribution distribution;
  final List<DdlAuditItem> list;
  final String? note;
  const DdlAuditPage({
    required this.pageNo,
    required this.pageSize,
    required this.total,
    required this.hasMore,
    required this.windowHours,
    required this.status,
    required this.distribution,
    required this.list,
    required this.note,
  });
  factory DdlAuditPage.fromJson(Map<String, dynamic> j) => DdlAuditPage(
        pageNo: _intOrNull(j['pageNo']) ?? 1,
        pageSize: _intOrNull(j['pageSize']) ?? 20,
        total: _intOrNull(j['total']) ?? 0,
        hasMore: j['hasMore'] == true,
        windowHours: _intOrNull(j['windowHours']) ?? 168,
        status: j['status']?.toString(),
        distribution: DdlAuditDistribution.fromJson(_m(j['distribution'])),
        list: (j['list'] as List? ?? const [])
            .whereType<Map>()
            .map((e) => DdlAuditItem.fromJson(Map<String, dynamic>.from(e)))
            .toList(),
        note: j['note']?.toString(),
      );
  static Map<String, dynamic> _m(dynamic v) =>
      v is Map ? Map<String, dynamic>.from(v) : <String, dynamic>{};
}

class DdlAuditDistribution {
  final int applied;
  final int skipped;
  final int pending;
  final int failed;
  const DdlAuditDistribution({
    required this.applied,
    required this.skipped,
    required this.pending,
    required this.failed,
  });
  int get total => applied + skipped + pending + failed;
  factory DdlAuditDistribution.fromJson(Map<String, dynamic> j) => DdlAuditDistribution(
        applied: _intOrNull(j['applied']) ?? 0,
        skipped: _intOrNull(j['skipped']) ?? 0,
        pending: _intOrNull(j['pending']) ?? 0,
        failed: _intOrNull(j['failed']) ?? 0,
      );
}

class DdlAuditItem {
  final int id;
  final DateTime? ts;
  final String? hostname;
  final String? srcSchema;
  final String? srcTable;
  final String? targetTable;
  final String? binlogPos;
  final String status;
  final String? sourceStmt;
  final String? appliedStmt;
  final String? errMsg;
  const DdlAuditItem({
    required this.id,
    required this.ts,
    required this.hostname,
    required this.srcSchema,
    required this.srcTable,
    required this.targetTable,
    required this.binlogPos,
    required this.status,
    required this.sourceStmt,
    required this.appliedStmt,
    required this.errMsg,
  });
  factory DdlAuditItem.fromJson(Map<String, dynamic> j) => DdlAuditItem(
        id: _intOrNull(j['id']) ?? 0,
        ts: DateTime.tryParse(j['ts']?.toString() ?? ''),
        hostname: j['hostname']?.toString(),
        srcSchema: j['src_schema']?.toString(),
        srcTable: j['src_table']?.toString(),
        targetTable: j['target_table']?.toString(),
        binlogPos: j['binlog_pos']?.toString(),
        status: (j['status'] ?? '').toString(),
        sourceStmt: j['source_stmt']?.toString(),
        appliedStmt: j['applied_stmt']?.toString(),
        errMsg: j['err_msg']?.toString(),
      );
}

// ── 审计分析 ───────────────────────────────────────────────

class AuditAnalytics {
  final int windowHours;
  final AuditTotals totals;
  final List<AuditSlow> slowest;
  final List<AuditPath> topPaths;
  final List<AuditError> errorBreakdown;
  final List<AuditUser> topUsers;
  final List<AuditTrend> trend;

  const AuditAnalytics({
    required this.windowHours,
    required this.totals,
    required this.slowest,
    required this.topPaths,
    required this.errorBreakdown,
    required this.topUsers,
    required this.trend,
  });

  factory AuditAnalytics.fromJson(Map<String, dynamic> j) => AuditAnalytics(
    windowHours: _intOrNull(j['windowHours']) ?? 24,
    totals: AuditTotals.fromJson(_m(j['totals'])),
    slowest: (j['slowest'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => AuditSlow.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
    topPaths: (j['topPaths'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => AuditPath.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
    errorBreakdown: (j['errorBreakdown'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => AuditError.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
    topUsers: (j['topUsers'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => AuditUser.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
    trend: (j['trend'] as List? ?? const [])
        .whereType<Map>()
        .map((e) => AuditTrend.fromJson(Map<String, dynamic>.from(e)))
        .toList(),
  );

  static Map<String, dynamic> _m(dynamic v) =>
      v is Map ? Map<String, dynamic>.from(v) : <String, dynamic>{};
}

class AuditTotals {
  final int total;
  final int ok;
  final int err;
  final double successRate;
  final int avgMs;
  final int p95Ms;
  final int maxMs;
  const AuditTotals({
    required this.total,
    required this.ok,
    required this.err,
    required this.successRate,
    required this.avgMs,
    required this.p95Ms,
    required this.maxMs,
  });
  factory AuditTotals.fromJson(Map<String, dynamic> j) => AuditTotals(
    total: _intOrNull(j['total']) ?? 0,
    ok: _intOrNull(j['ok']) ?? 0,
    err: _intOrNull(j['err']) ?? 0,
    successRate: (j['successRate'] as num?)?.toDouble() ?? 0,
    avgMs: _intOrNull(j['avgMs']) ?? 0,
    p95Ms: _intOrNull(j['p95Ms']) ?? 0,
    maxMs: _intOrNull(j['maxMs']) ?? 0,
  );
}

class AuditSlow {
  final String path;
  final int durationMs;
  final String? code;
  final bool ok;
  final String? at;
  const AuditSlow({
    required this.path,
    required this.durationMs,
    required this.code,
    required this.ok,
    required this.at,
  });
  factory AuditSlow.fromJson(Map<String, dynamic> j) => AuditSlow(
    path: (j['path'] ?? '').toString(),
    durationMs: _intOrNull(j['duration_ms']) ?? 0,
    code: j['code']?.toString(),
    ok: j['ok'] == true,
    at: j['at']?.toString(),
  );
}

class AuditPath {
  final String path;
  final int cnt;
  final int errCnt;
  final int avgMs;
  const AuditPath({
    required this.path,
    required this.cnt,
    required this.errCnt,
    required this.avgMs,
  });
  factory AuditPath.fromJson(Map<String, dynamic> j) => AuditPath(
    path: (j['path'] ?? '').toString(),
    cnt: _intOrNull(j['cnt']) ?? 0,
    errCnt: _intOrNull(j['err_cnt']) ?? 0,
    avgMs: _intOrNull(j['avg_ms']) ?? 0,
  );
}

class AuditError {
  final String code;
  final int cnt;
  const AuditError({required this.code, required this.cnt});
  factory AuditError.fromJson(Map<String, dynamic> j) => AuditError(
    code: (j['code'] ?? '').toString(),
    cnt: _intOrNull(j['cnt']) ?? 0,
  );
}

class AuditUser {
  final int userId;
  final String username;
  final int cnt;
  const AuditUser({
    required this.userId,
    required this.username,
    required this.cnt,
  });
  factory AuditUser.fromJson(Map<String, dynamic> j) => AuditUser(
    userId: _intOrNull(j['user_id']) ?? 0,
    username: (j['username'] ?? '').toString(),
    cnt: _intOrNull(j['cnt']) ?? 0,
  );
}

class AuditTrend {
  final String hour;
  final int cnt;
  final int errCnt;
  const AuditTrend({
    required this.hour,
    required this.cnt,
    required this.errCnt,
  });
  factory AuditTrend.fromJson(Map<String, dynamic> j) => AuditTrend(
    hour: (j['hour'] ?? '').toString(),
    cnt: _intOrNull(j['cnt']) ?? 0,
    errCnt: _intOrNull(j['err_cnt']) ?? 0,
  );
}
