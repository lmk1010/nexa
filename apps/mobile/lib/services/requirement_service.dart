import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class RequirementService {
  static const String featureCode = 'work.requirement';
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Future<RequirementPagedResult<RequirementItem>> getPage({
    int pageNo = 1,
    int pageSize = 20,
    String? keyword,
    int? status,
    int? priority,
    int? approvalStatus,
    String? processInstanceIds,
    int? userId,
    int? proposerUserId,
    int? assigneeUserId,
    bool? commentUnreadOnly,
  }) async {
    final data = await _get('/business/work/requirement/page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'keyword': keyword,
      'status': status,
      'priority': priority,
      'approvalStatus': approvalStatus,
      'processInstanceIds': processInstanceIds,
      'userId': userId,
      'proposerUserId': proposerUserId,
      'assigneeUserId': assigneeUserId,
      'commentUnreadOnly': commentUnreadOnly,
    });
    return RequirementPagedResult.fromJson(data, RequirementItem.fromJson);
  }

  static Future<RequirementOverview> getOverview({
    String? keyword,
    int? status,
    String? processInstanceIds,
    int? userId,
    int? proposerUserId,
    int? assigneeUserId,
    bool? commentUnreadOnly,
  }) async {
    final data = await _get('/business/work/requirement/overview', {
      'keyword': keyword,
      'status': status,
      'processInstanceIds': processInstanceIds,
      'userId': userId,
      'proposerUserId': proposerUserId,
      'assigneeUserId': assigneeUserId,
      'commentUnreadOnly': commentUnreadOnly,
    });
    return RequirementOverview.fromJson(_asMap(data));
  }

  static Future<RequirementItem> getDetail(int id) async {
    final data = await _get('/business/work/requirement/get', {'id': id});
    return RequirementItem.fromJson(_asMap(data));
  }

  static Future<List<RequirementLog>> getLogs(int requirementId) async {
    final data = await _get('/business/work/requirement/logs', {
      'requirementId': requirementId,
    });
    return _asList(
      data,
    ).map((item) => RequirementLog.fromJson(_asMap(item))).toList();
  }

  static Future<List<RequirementComment>> getComments(int requirementId) async {
    final data = await _get('/business/work/requirement/comment/list', {
      'requirementId': requirementId,
    });
    return _asList(
      data,
    ).map((item) => RequirementComment.fromJson(_asMap(item))).toList();
  }

  static Future<void> readAllComments(int requirementId) async {
    await _putQuery('/business/work/requirement/comment/read-all', {
      'requirementId': requirementId,
    });
  }

  static Future<void> createComment({
    required int requirementId,
    required String content,
    String commentType = 'COMMENT',
    List<String> attachmentUrls = const [],
    int? targetUserId,
  }) async {
    await _post('/business/work/requirement/comment/create', {
      'requirementId': requirementId,
      'commentType': commentType,
      'content': content,
      'attachmentUrls': attachmentUrls,
      'targetUserId': targetUserId,
    });
  }

  static Future<int?> createRequirement(RequirementSaveRequest request) async {
    final data = await _post(
      '/business/work/requirement/create',
      request.toJson(),
    );
    return _parseInt(data);
  }

  static Future<void> updateRequirement(RequirementSaveRequest request) async {
    await _put('/business/work/requirement/update', request.toJson());
  }

  static Future<void> assignRequirement({
    required int id,
    required int assigneeUserId,
    int? assigneeTenantId,
    String? assigneeName,
    List<int> collaboratorUserIds = const [],
    bool transfer = false,
  }) async {
    await _put(
      transfer
          ? '/business/work/requirement/transfer-assign'
          : '/business/work/requirement/assign',
      {
        'id': id,
        'assigneeUserId': assigneeUserId,
        'assigneeTenantId': assigneeTenantId,
        'assigneeName': assigneeName,
        'collaboratorUserIds': collaboratorUserIds,
      },
    );
  }

  static Future<void> submitApproval(int id) =>
      _action('/business/work/requirement/submit-approval', id);

  static Future<void> startDev(int id) =>
      _action('/business/work/requirement/start-dev', id);

  static Future<void> submitTest(int id) =>
      _action('/business/work/requirement/submit-test', id);

  static Future<void> testPass(int id) =>
      _action('/business/work/requirement/test-pass', id);

  static Future<void> testReject(int id, String remark) =>
      _action('/business/work/requirement/test-reject', id, remark: remark);

  static Future<void> devReject(int id, String remark) =>
      _action('/business/work/requirement/dev-reject', id, remark: remark);

  static Future<void> acceptPass(
    int id, {
    String? remark,
    List<String> attachmentUrls = const [],
  }) => _action(
    '/business/work/requirement/accept-pass',
    id,
    remark: remark,
    attachmentUrls: attachmentUrls,
  );

  static Future<void> acceptReject(int id, String remark) =>
      _action('/business/work/requirement/accept-reject', id, remark: remark);

  static Future<void> cancel(int id) =>
      _action('/business/work/requirement/cancel', id);

  static Future<void> suspend(int id) =>
      _action('/business/work/requirement/suspend', id);

  static Future<void> reopen(int id) =>
      _action('/business/work/requirement/reopen', id);

  static Future<List<String>> getTodoApprovalProcessInstanceIds() async {
    final data = await _get(
      '/business/work/requirement/todo-approval-process-instance-ids',
    );
    return _asList(data).map((item) => item.toString()).toList();
  }

  static Future<RequirementApprovalDetail?> getApprovalDetail(
    String processInstanceId,
  ) async {
    if (processInstanceId.trim().isEmpty) return null;
    final data = await _get('/bpm/process-instance/get-approval-detail', {
      'processInstanceId': processInstanceId.trim(),
    });
    return RequirementApprovalDetail.fromJson(_asMap(data));
  }

  static Future<void> approveTask({
    required String taskId,
    String? reason,
  }) async {
    await _put('/bpm/task/approve', {'id': taskId, 'reason': reason});
  }

  static Future<void> rejectTask({
    required String taskId,
    required String reason,
  }) async {
    await _put('/bpm/task/reject', {'id': taskId, 'reason': reason});
  }

  static Future<List<RequirementUser>> getUsers({
    String? tenantIds,
    String? keyword,
  }) async {
    final data = await _get('/system/user/simple-list-by-tenants', {
      'tenantIds': tenantIds,
      'featureCode': featureCode,
    });
    final query = keyword?.trim().toLowerCase();
    final users = _asList(data)
        .map((item) => RequirementUser.fromJson(_asMap(item)))
        .where((item) => item.id != null)
        .toList();
    if (query == null || query.isEmpty) return users;
    return users.where((item) => item.matches(query)).toList();
  }

  static Future<RequirementScopeOptions> getScopeOptions() async {
    final data = await _get('/business/work/requirement/scope-options');
    return RequirementScopeOptions.fromJson(_asMap(data));
  }

  static Future<String> uploadFile({
    required String path,
    String? fileName,
    String directory = 'work/requirement',
  }) async {
    final request = http.MultipartRequest(
      'POST',
      _adminUri('/infra/file/upload'),
    );
    request.headers.addAll(await _authorizedHeaders(jsonBody: false));
    request.fields['directory'] = directory;
    request.files.add(
      await http.MultipartFile.fromPath('file', path, filename: fileName),
    );

    final streamed = await request.send().timeout(_requestTimeout);
    final response = await http.Response.fromStream(streamed);
    final data = _extractData(response, '/infra/file/upload');
    if (data is String && data.isNotEmpty) return data;
    final map = _asMap(data);
    final fileId = map['fileId']?.toString() ?? map['id']?.toString();
    if (fileId == null || fileId.isEmpty) {
      throw ApiException('上传成功但未返回文件编号');
    }
    return fileId;
  }

  static Future<RequirementAttachment> resolveAttachment(String rawUrl) async {
    final raw = rawUrl.trim();
    if (raw.isEmpty) {
      return RequirementAttachment(raw: raw, name: '附件', url: '');
    }

    final uri = Uri.tryParse(raw);
    if (uri != null && (uri.scheme == 'http' || uri.scheme == 'https')) {
      return RequirementAttachment(
        raw: raw,
        name: _fileNameFromUrl(raw),
        url: raw,
      );
    }

    try {
      final data = await _get('/infra/file/info/${Uri.encodeComponent(raw)}');
      final info = _asMap(data);
      final name = _stringValue(info['name']) ?? raw;
      final url = _stringValue(info['url']) ?? _downloadUrl(raw);
      return RequirementAttachment(raw: raw, name: name, url: url);
    } catch (_) {
      return RequirementAttachment(raw: raw, name: raw, url: _downloadUrl(raw));
    }
  }

  static Future<int?> currentUserId() async {
    final cert = await ApiService.getFreshLoginCertificate();
    return _parseInt(cert?['userId'] ?? cert?['userID']);
  }

  static Future<void> _action(
    String path,
    int id, {
    String? remark,
    List<String>? attachmentUrls,
  }) async {
    await _put(path, {
      'id': id,
      'remark': remark,
      'attachmentUrls': attachmentUrls,
    });
  }

  static Future<dynamic> _get(
    String path, [
    Map<String, dynamic>? query,
  ]) async {
    final response = await http
        .get(_adminUri(path, query), headers: await _authorizedHeaders())
        .timeout(_requestTimeout);
    return _extractData(response, path);
  }

  static Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    final response = await http
        .post(
          _adminUri(path),
          headers: await _authorizedHeaders(),
          body: json.encode(_compactMap(body)),
        )
        .timeout(_requestTimeout);
    return _extractData(response, path);
  }

  static Future<dynamic> _put(String path, Map<String, dynamic> body) async {
    final response = await http
        .put(
          _adminUri(path),
          headers: await _authorizedHeaders(),
          body: json.encode(_compactMap(body)),
        )
        .timeout(_requestTimeout);
    return _extractData(response, path);
  }

  static Future<dynamic> _putQuery(
    String path,
    Map<String, dynamic> query,
  ) async {
    final response = await http
        .put(_adminUri(path, query), headers: await _authorizedHeaders())
        .timeout(_requestTimeout);
    return _extractData(response, path);
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

  static String _downloadUrl(String fileId) {
    return '${AppConfig.baseUrl}/admin-api/infra/file/download/'
        '${Uri.encodeComponent(fileId)}';
  }

  static String _fileNameFromUrl(String value) {
    final path = Uri.tryParse(value)?.pathSegments;
    if (path != null && path.isNotEmpty) return path.last;
    return value;
  }
}

class RequirementPagedResult<T> {
  final List<T> list;
  final int total;

  const RequirementPagedResult({required this.list, required this.total});

  factory RequirementPagedResult.fromJson(
    dynamic json,
    T Function(Map<String, dynamic>) parse,
  ) {
    final map = _asMap(json);
    final list = _asList(
      map['list'],
    ).map((item) => parse(_asMap(item))).toList();
    return RequirementPagedResult(
      list: list,
      total: _parseInt(map['total']) ?? list.length,
    );
  }
}

class RequirementItem {
  final int? id;
  final String title;
  final String description;
  final int priority;
  final int status;
  final String? processInstanceId;
  final int? approvalStatus;
  final String? proposerDept;
  final String? targetDept;
  final String? proposerName;
  final int? proposerUserId;
  final int? assigneeUserId;
  final String? assigneeName;
  final List<RequirementDeveloper> developerMembers;
  final List<int> collaboratorUserIds;
  final String? collaboratorNames;
  final DateTime? expectedFinishDate;
  final DateTime? submitTestTime;
  final DateTime? testPassTime;
  final DateTime? acceptedTime;
  final DateTime? closeTime;
  final int? previousStatus;
  final String? lastRejectReason;
  final double? integral;
  final String? useType;
  final String? sourceIp;
  final List<String> attachmentUrls;
  final int commentCount;
  final int commentUnreadCount;
  final DateTime? createTime;
  final DateTime? updateTime;

  const RequirementItem({
    required this.id,
    required this.title,
    required this.description,
    required this.priority,
    required this.status,
    required this.processInstanceId,
    required this.approvalStatus,
    required this.proposerDept,
    required this.targetDept,
    required this.proposerName,
    required this.proposerUserId,
    required this.assigneeUserId,
    required this.assigneeName,
    required this.developerMembers,
    required this.collaboratorUserIds,
    required this.collaboratorNames,
    required this.expectedFinishDate,
    required this.submitTestTime,
    required this.testPassTime,
    required this.acceptedTime,
    required this.closeTime,
    required this.previousStatus,
    required this.lastRejectReason,
    required this.integral,
    required this.useType,
    required this.sourceIp,
    required this.attachmentUrls,
    required this.commentCount,
    required this.commentUnreadCount,
    required this.createTime,
    required this.updateTime,
  });

  factory RequirementItem.fromJson(Map<String, dynamic> json) {
    return RequirementItem(
      id: _parseInt(json['id']),
      title: _stringValue(json['title']) ?? '未命名需求',
      description: _stringValue(json['description']) ?? '',
      priority: _parseInt(json['priority']) ?? 2,
      status: _parseInt(json['status']) ?? 0,
      processInstanceId: _stringValue(json['processInstanceId']),
      approvalStatus: _parseInt(json['approvalStatus']),
      proposerDept: _stringValue(json['proposerDept']),
      targetDept: _stringValue(json['targetDept']),
      proposerName: _stringValue(json['proposerName']),
      proposerUserId: _parseInt(json['proposerUserId']),
      assigneeUserId: _parseInt(json['assigneeUserId']),
      assigneeName: _stringValue(json['assigneeName']),
      developerMembers: _asList(
        json['developerMembers'],
      ).map((item) => RequirementDeveloper.fromJson(_asMap(item))).toList(),
      collaboratorUserIds: _asList(
        json['collaboratorUserIds'],
      ).map(_parseInt).whereType<int>().toList(),
      collaboratorNames: _stringValue(json['collaboratorNames']),
      expectedFinishDate: _parseDateTime(json['expectedFinishDate']),
      submitTestTime: _parseDateTime(json['submitTestTime']),
      testPassTime: _parseDateTime(json['testPassTime']),
      acceptedTime: _parseDateTime(json['acceptedTime']),
      closeTime: _parseDateTime(json['closeTime']),
      previousStatus: _parseInt(json['previousStatus']),
      lastRejectReason: _stringValue(json['lastRejectReason']),
      integral: _parseDouble(json['integral']),
      useType: _stringValue(json['useType']),
      sourceIp: _stringValue(json['sourceIp']),
      attachmentUrls: _asList(json['attachmentUrls'])
          .map((item) => item.toString())
          .where((item) => item.trim().isNotEmpty)
          .toList(),
      commentCount: _parseInt(json['commentCount']) ?? 0,
      commentUnreadCount: _parseInt(json['commentUnreadCount']) ?? 0,
      createTime: _parseDateTime(json['createTime']),
      updateTime: _parseDateTime(json['updateTime']),
    );
  }

  bool get canOperateByApproval =>
      approvalStatus == null || approvalStatus == 2;

  bool isDeveloper(int? currentUserId) {
    if (currentUserId == null) return false;
    if (assigneeUserId == currentUserId) return true;
    if (collaboratorUserIds.contains(currentUserId)) return true;
    return developerMembers.any((item) => item.userId == currentUserId);
  }

  bool isProposer(int? currentUserId) =>
      currentUserId != null && proposerUserId == currentUserId;
}

class RequirementDeveloper {
  final int? id;
  final int? requirementId;
  final int? userId;
  final String? userName;
  final int? userTenantId;
  final String? memberRole;

  const RequirementDeveloper({
    this.id,
    this.requirementId,
    this.userId,
    this.userName,
    this.userTenantId,
    this.memberRole,
  });

  factory RequirementDeveloper.fromJson(Map<String, dynamic> json) {
    return RequirementDeveloper(
      id: _parseInt(json['id']),
      requirementId: _parseInt(json['requirementId']),
      userId: _parseInt(json['userId']),
      userName: _stringValue(json['userName']),
      userTenantId: _parseInt(json['userTenantId']),
      memberRole: _stringValue(json['memberRole']),
    );
  }
}

class RequirementOverview {
  final int totalCount;
  final int pendingCount;
  final int developingCount;
  final int completedCount;
  final int myTodoCount;
  final int overdueCount;
  final int unreadCount;

  const RequirementOverview({
    required this.totalCount,
    required this.pendingCount,
    required this.developingCount,
    required this.completedCount,
    required this.myTodoCount,
    required this.overdueCount,
    required this.unreadCount,
  });

  factory RequirementOverview.fromJson(Map<String, dynamic> json) {
    return RequirementOverview(
      totalCount: _parseInt(json['totalCount']) ?? 0,
      pendingCount: _parseInt(json['pendingCount']) ?? 0,
      developingCount: _parseInt(json['developingCount']) ?? 0,
      completedCount: _parseInt(json['completedCount']) ?? 0,
      myTodoCount: _parseInt(json['myTodoCount']) ?? 0,
      overdueCount: _parseInt(json['overdueCount']) ?? 0,
      unreadCount: _parseInt(json['unreadCount']) ?? 0,
    );
  }
}

class RequirementLog {
  final int? id;
  final int? requirementId;
  final String actionType;
  final int? fromStatus;
  final int? toStatus;
  final String? remark;
  final int? operatorUserId;
  final String? operatorName;
  final DateTime? createTime;

  const RequirementLog({
    required this.id,
    required this.requirementId,
    required this.actionType,
    required this.fromStatus,
    required this.toStatus,
    required this.remark,
    required this.operatorUserId,
    required this.operatorName,
    required this.createTime,
  });

  factory RequirementLog.fromJson(Map<String, dynamic> json) {
    return RequirementLog(
      id: _parseInt(json['id']),
      requirementId: _parseInt(json['requirementId']),
      actionType: _stringValue(json['actionType']) ?? '',
      fromStatus: _parseInt(json['fromStatus']),
      toStatus: _parseInt(json['toStatus']),
      remark: _stringValue(json['remark']),
      operatorUserId: _parseInt(json['operatorUserId']),
      operatorName: _stringValue(json['operatorName']),
      createTime: _parseDateTime(json['createTime']),
    );
  }
}

class RequirementComment {
  final int? id;
  final int? requirementId;
  final String commentType;
  final String content;
  final int? fromUserId;
  final String? fromUserName;
  final int? targetUserId;
  final String? targetUserName;
  final List<String> attachmentUrls;
  final bool readStatus;
  final DateTime? createTime;

  const RequirementComment({
    required this.id,
    required this.requirementId,
    required this.commentType,
    required this.content,
    required this.fromUserId,
    required this.fromUserName,
    required this.targetUserId,
    required this.targetUserName,
    required this.attachmentUrls,
    required this.readStatus,
    required this.createTime,
  });

  factory RequirementComment.fromJson(Map<String, dynamic> json) {
    return RequirementComment(
      id: _parseInt(json['id']),
      requirementId: _parseInt(json['requirementId']),
      commentType: _stringValue(json['commentType']) ?? 'COMMENT',
      content: _stringValue(json['content']) ?? '',
      fromUserId: _parseInt(json['fromUserId']),
      fromUserName: _stringValue(json['fromUserName']),
      targetUserId: _parseInt(json['targetUserId']),
      targetUserName: _stringValue(json['targetUserName']),
      attachmentUrls: _asList(json['attachmentUrls'])
          .map((item) => item.toString())
          .where((item) => item.trim().isNotEmpty)
          .toList(),
      readStatus: _parseBool(json['readStatus']),
      createTime: _parseDateTime(json['createTime']),
    );
  }
}

class RequirementSaveRequest {
  final int? id;
  final String title;
  final String description;
  final int priority;
  final DateTime? expectedFinishDate;
  final int? assigneeUserId;
  final int? assigneeTenantId;
  final String? assigneeName;
  final List<int> collaboratorUserIds;
  final String? proposerName;
  final String? proposerDept;
  final String? targetDept;
  final List<String> attachmentUrls;
  final String? remark;

  const RequirementSaveRequest({
    this.id,
    required this.title,
    required this.description,
    required this.priority,
    this.expectedFinishDate,
    this.assigneeUserId,
    this.assigneeTenantId,
    this.assigneeName,
    this.collaboratorUserIds = const [],
    this.proposerName,
    this.proposerDept,
    this.targetDept,
    this.attachmentUrls = const [],
    this.remark,
  });

  Map<String, dynamic> toJson() {
    return _compactMap({
      'id': id,
      'title': title,
      'description': description,
      'priority': priority,
      'expectedFinishDate': _formatDateTime(expectedFinishDate),
      'assigneeUserId': assigneeUserId,
      'assigneeTenantId': assigneeTenantId,
      'assigneeName': assigneeName,
      'collaboratorUserIds': collaboratorUserIds,
      'proposerName': proposerName,
      'proposerDept': proposerDept,
      'targetDept': targetDept,
      'attachmentUrls': attachmentUrls,
      'remark': remark,
    });
  }
}

class RequirementUser {
  final int? id;
  final String username;
  final String nickname;
  final int? tenantId;
  final String? tenantName;
  final String? deptName;
  final String? mobile;
  final String? avatar;

  const RequirementUser({
    required this.id,
    required this.username,
    required this.nickname,
    required this.tenantId,
    required this.tenantName,
    required this.deptName,
    required this.mobile,
    required this.avatar,
  });

  factory RequirementUser.fromJson(Map<String, dynamic> json) {
    return RequirementUser(
      id: _parseInt(json['id']),
      username: _stringValue(json['username']) ?? '',
      nickname: _stringValue(json['nickname']) ?? '',
      tenantId: _parseInt(json['tenantId']),
      tenantName: _stringValue(json['tenantName']),
      deptName: _stringValue(json['deptName']),
      mobile: _stringValue(json['mobile']),
      avatar: _stringValue(json['avatar']),
    );
  }

  String get displayName {
    if (nickname.isNotEmpty) return nickname;
    if (username.isNotEmpty) return username;
    return id?.toString() ?? '未知用户';
  }

  bool matches(String query) {
    final haystack = [
      username,
      nickname,
      deptName,
      mobile,
      tenantName,
      id?.toString(),
    ].whereType<String>().join(' ').toLowerCase();
    return haystack.contains(query);
  }
}

class RequirementScopeOptions {
  final int? currentTenantId;
  final bool crossTenantEnabled;
  final bool queryAllEnabled;
  final List<int> selectableTenantIds;

  const RequirementScopeOptions({
    required this.currentTenantId,
    required this.crossTenantEnabled,
    required this.queryAllEnabled,
    required this.selectableTenantIds,
  });

  factory RequirementScopeOptions.fromJson(Map<String, dynamic> json) {
    return RequirementScopeOptions(
      currentTenantId: _parseInt(json['currentTenantId']),
      crossTenantEnabled: _parseBool(json['crossTenantEnabled']),
      queryAllEnabled: _parseBool(json['queryAllEnabled']),
      selectableTenantIds: _asList(
        json['selectableTenantIds'],
      ).map(_parseInt).whereType<int>().toList(),
    );
  }

  String? get tenantIdsParam {
    if (selectableTenantIds.isEmpty) return currentTenantId?.toString();
    return selectableTenantIds.join(',');
  }
}

class RequirementApprovalDetail {
  final int? status;
  final List<RequirementApprovalNode> activityNodes;
  final RequirementApprovalTask? todoTask;

  const RequirementApprovalDetail({
    required this.status,
    required this.activityNodes,
    required this.todoTask,
  });

  factory RequirementApprovalDetail.fromJson(Map<String, dynamic> json) {
    return RequirementApprovalDetail(
      status: _parseInt(json['status']),
      activityNodes: _asList(
        json['activityNodes'],
      ).map((item) => RequirementApprovalNode.fromJson(_asMap(item))).toList(),
      todoTask: json['todoTask'] == null
          ? null
          : RequirementApprovalTask.fromJson(_asMap(json['todoTask'])),
    );
  }
}

class RequirementApprovalNode {
  final String id;
  final String name;
  final int? status;
  final DateTime? startTime;
  final DateTime? endTime;

  const RequirementApprovalNode({
    required this.id,
    required this.name,
    required this.status,
    required this.startTime,
    required this.endTime,
  });

  factory RequirementApprovalNode.fromJson(Map<String, dynamic> json) {
    return RequirementApprovalNode(
      id: _stringValue(json['id']) ?? '',
      name: _stringValue(json['name']) ?? '审批节点',
      status: _parseInt(json['status']),
      startTime: _parseDateTime(json['startTime']),
      endTime: _parseDateTime(json['endTime']),
    );
  }
}

class RequirementApprovalTask {
  final String id;
  final int? status;
  final String? taskDefinitionKey;
  final bool reasonRequire;
  final int? assigneeUserId;
  final int? ownerUserId;

  const RequirementApprovalTask({
    required this.id,
    required this.status,
    required this.taskDefinitionKey,
    required this.reasonRequire,
    required this.assigneeUserId,
    required this.ownerUserId,
  });

  factory RequirementApprovalTask.fromJson(Map<String, dynamic> json) {
    final assigneeUser = _asMap(json['assigneeUser']);
    final ownerUser = _asMap(json['ownerUser']);
    return RequirementApprovalTask(
      id: _stringValue(json['id']) ?? '',
      status: _parseInt(json['status']),
      taskDefinitionKey: _stringValue(json['taskDefinitionKey']),
      reasonRequire: _parseBool(json['reasonRequire']),
      assigneeUserId: _parseInt(
        assigneeUser['id'] ?? json['assigneeUserId'] ?? json['assignee'],
      ),
      ownerUserId: _parseInt(
        ownerUser['id'] ?? json['ownerUserId'] ?? json['owner'],
      ),
    );
  }

  bool canHandle(int? currentUserId) {
    if (id.isEmpty) return false;
    if (status != 0 && status != 1) return false;
    final handlerId = assigneeUserId ?? ownerUserId;
    return handlerId == null || handlerId == currentUserId;
  }
}

class RequirementAttachment {
  final String raw;
  final String name;
  final String url;

  const RequirementAttachment({
    required this.raw,
    required this.name,
    required this.url,
  });

  bool get isImage {
    final lower = name.toLowerCase();
    return lower.endsWith('.png') ||
        lower.endsWith('.jpg') ||
        lower.endsWith('.jpeg') ||
        lower.endsWith('.gif') ||
        lower.endsWith('.webp') ||
        lower.endsWith('.bmp');
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

double? _parseDouble(dynamic value) {
  if (value == null) return null;
  if (value is double) return value;
  if (value is num) return value.toDouble();
  return double.tryParse(value.toString());
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

String? _formatDateTime(DateTime? value) {
  if (value == null) return null;
  String two(int input) => input.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)} '
      '${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
}
