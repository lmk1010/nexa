import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';

class BpmService {
  static const Duration _requestTimeout = Duration(
    milliseconds: AppConfig.connectionTimeout,
  );

  static Future<BpmPagedResult<BpmTaskItem>> getTodoTasks({
    int pageNo = 1,
    int pageSize = 20,
    String? keyword,
  }) async {
    final data = await _get('/bpm/task/todo-page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'name': keyword,
    });
    return BpmPagedResult.fromJson(data, BpmTaskItem.fromJson);
  }

  static Future<BpmPagedResult<BpmTaskItem>> getDoneTasks({
    int pageNo = 1,
    int pageSize = 20,
    String? keyword,
  }) async {
    final data = await _get('/bpm/task/done-page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'name': keyword,
    });
    return BpmPagedResult.fromJson(data, BpmTaskItem.fromJson);
  }

  static Future<BpmApprovalDetail> getApprovalDetail({
    required String processInstanceId,
    String? taskId,
  }) async {
    final data = await _get('/bpm/process-instance/get-approval-detail', {
      'processInstanceId': processInstanceId,
      'taskId': taskId,
    });
    return BpmApprovalDetail.fromJson(_asMap(data));
  }

  static Future<void> approveTask({
    required String taskId,
    String? reason,
    Map<String, dynamic>? variables,
    Map<String, List<int>>? nextAssignees,
  }) async {
    await _put('/bpm/task/approve', {
      'id': taskId,
      'reason': reason,
      'variables': variables,
      'nextAssignees': nextAssignees,
    });
  }

  static Future<void> rejectTask({
    required String taskId,
    required String reason,
  }) async {
    await _put('/bpm/task/reject', {'id': taskId, 'reason': reason});
  }

  static Future<void> copyTask({
    required String taskId,
    required List<int> copyUserIds,
    String? reason,
  }) async {
    await _put('/bpm/task/copy', {
      'id': taskId,
      'copyUserIds': copyUserIds,
      'reason': reason,
    });
  }

  static Future<void> transferTask({
    required String taskId,
    required int assigneeUserId,
    required String reason,
  }) async {
    await _put('/bpm/task/transfer', {
      'id': taskId,
      'assigneeUserId': assigneeUserId,
      'reason': reason,
    });
  }

  static Future<void> delegateTask({
    required String taskId,
    required int delegateUserId,
    required String reason,
  }) async {
    await _put('/bpm/task/delegate', {
      'id': taskId,
      'delegateUserId': delegateUserId,
      'reason': reason,
    });
  }

  static Future<List<BpmReturnTask>> getReturnTasks(String taskId) async {
    final data = await _get('/bpm/task/list-by-return', {'id': taskId});
    return _asList(
      data,
    ).map((item) => BpmReturnTask.fromJson(_asMap(item))).toList();
  }

  static Future<void> returnTask({
    required String taskId,
    required String targetTaskDefinitionKey,
    required String reason,
  }) async {
    await _put('/bpm/task/return', {
      'id': taskId,
      'targetTaskDefinitionKey': targetTaskDefinitionKey,
      'reason': reason,
    });
  }

  static Future<void> createSignTask({
    required String taskId,
    required List<int> userIds,
    required String type,
    required String reason,
  }) async {
    await _put('/bpm/task/create-sign', {
      'id': taskId,
      'userIds': userIds,
      'type': type,
      'reason': reason,
    });
  }

  static Future<void> deleteSignTask({
    required String taskId,
    required String reason,
  }) async {
    await _delete('/bpm/task/delete-sign', {'id': taskId, 'reason': reason});
  }

  static Future<List<BpmTaskItem>> getChildrenTasks(String parentTaskId) async {
    final data = await _get('/bpm/task/list-by-parent-task-id', {
      'parentTaskId': parentTaskId,
    });
    return _asList(
      data,
    ).map((item) => BpmTaskItem.fromJson(_asMap(item))).toList();
  }

  static Future<List<BpmUser>> getSimpleUsers({String? keyword}) async {
    final data = await _get('/system/user/simple-list');
    final normalizedKeyword = keyword?.trim().toLowerCase();
    final users = _asList(
      data,
    ).map((item) => BpmUser.fromJson(_asMap(item))).toList();
    if (normalizedKeyword == null || normalizedKeyword.isEmpty) {
      return users;
    }
    return users.where((user) {
      final haystack = [
        user.displayName,
        user.username,
        user.deptName,
        user.tenantName,
      ].whereType<String>().join(' ').toLowerCase();
      return haystack.contains(normalizedKeyword);
    }).toList();
  }

  static Future<List<BpmDeptNode>> getDeptTreeWithEmployeeCount() async {
    final data = await _get('/system/dept/tree-with-employee-count');
    return _asList(
      data,
    ).map((item) => BpmDeptNode.fromJson(_asMap(item))).toList();
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

  static Future<dynamic> _delete(String path, Map<String, dynamic> body) async {
    final response = await http
        .delete(
          _adminUri(path),
          headers: await _authorizedHeaders(),
          body: json.encode(_compactMap(body)),
        )
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

class BpmPagedResult<T> {
  final List<T> list;
  final int total;

  const BpmPagedResult({required this.list, required this.total});

  factory BpmPagedResult.fromJson(
    dynamic json,
    T Function(Map<String, dynamic>) parse,
  ) {
    final map = _asMap(json);
    final list = _asList(
      map['list'],
    ).map((item) => parse(_asMap(item))).toList();
    return BpmPagedResult(
      list: list,
      total: _parseInt(map['total']) ?? list.length,
    );
  }
}

class BpmTaskItem {
  final String id;
  final String name;
  final String? taskDefinitionKey;
  final String? parentTaskId;
  final DateTime? createTime;
  final DateTime? endTime;
  final int? status;
  final String? reason;
  final String processInstanceId;
  final BpmProcessInstance? processInstance;
  final BpmUser? ownerUser;
  final BpmUser? assigneeUser;
  final bool reasonRequire;
  final bool signEnable;
  final int? nodeType;
  final List<BpmTaskItem> children;
  final Map<int, BpmButtonSetting> buttonsSetting;
  final Map<String, dynamic> formVariables;

  const BpmTaskItem({
    required this.id,
    required this.name,
    required this.taskDefinitionKey,
    required this.parentTaskId,
    required this.createTime,
    required this.endTime,
    required this.status,
    required this.reason,
    required this.processInstanceId,
    required this.processInstance,
    required this.ownerUser,
    required this.assigneeUser,
    required this.reasonRequire,
    required this.signEnable,
    required this.nodeType,
    required this.children,
    required this.buttonsSetting,
    required this.formVariables,
  });

  factory BpmTaskItem.fromJson(Map<String, dynamic> json) {
    final processInstance = _asMap(json['processInstance']);
    return BpmTaskItem(
      id: _stringValue(json['id']) ?? '',
      name: _stringValue(json['name']) ?? '审批任务',
      taskDefinitionKey: _stringValue(json['taskDefinitionKey']),
      parentTaskId: _stringValue(json['parentTaskId']),
      createTime: _parseDateTime(json['createTime']),
      endTime: _parseDateTime(json['endTime']),
      status: _parseInt(json['status']),
      reason: _stringValue(json['reason']),
      processInstanceId:
          _stringValue(json['processInstanceId']) ??
          _stringValue(processInstance['id']) ??
          '',
      processInstance: processInstance.isEmpty
          ? null
          : BpmProcessInstance.fromJson(processInstance),
      ownerUser: json['ownerUser'] == null
          ? null
          : BpmUser.fromJson(_asMap(json['ownerUser'])),
      assigneeUser: json['assigneeUser'] == null
          ? null
          : BpmUser.fromJson(_asMap(json['assigneeUser'])),
      reasonRequire: _parseBool(json['reasonRequire']),
      signEnable: _parseBool(json['signEnable']),
      nodeType: _parseInt(json['nodeType']),
      children: _asList(
        json['children'],
      ).map((item) => BpmTaskItem.fromJson(_asMap(item))).toList(),
      buttonsSetting: _parseButtonSettings(json['buttonsSetting']),
      formVariables: _asMap(json['formVariables']),
    );
  }

  String get processName {
    final title = processInstance?.name.trim();
    if (title != null && title.isNotEmpty) return title;
    return name.trim().isNotEmpty ? name.trim() : '审批流程';
  }

  String get starterName => processInstance?.startUser?.displayName ?? '-';

  List<BpmSummaryItem> get summary => processInstance?.summary ?? const [];

  bool get canApprove {
    if (id.trim().isEmpty) return false;
    return status == null || status == 0 || status == 1;
  }

  bool get canOperate => canApprove;

  bool isButtonEnabled(int type) {
    final setting = buttonsSetting[type];
    return setting?.enable ?? true;
  }

  String buttonLabel(int type, String fallback) {
    final label = buttonsSetting[type]?.displayName.trim();
    if (label != null && label.isNotEmpty) return label;
    return fallback;
  }
}

class BpmButtonSetting {
  final String displayName;
  final bool enable;

  const BpmButtonSetting({required this.displayName, required this.enable});

  factory BpmButtonSetting.fromJson(Map<String, dynamic> json) {
    return BpmButtonSetting(
      displayName: _stringValue(json['displayName']) ?? '',
      enable: json.containsKey('enable') ? _parseBool(json['enable']) : true,
    );
  }
}

class BpmReturnTask {
  final String taskDefinitionKey;
  final String name;

  const BpmReturnTask({required this.taskDefinitionKey, required this.name});

  factory BpmReturnTask.fromJson(Map<String, dynamic> json) {
    return BpmReturnTask(
      taskDefinitionKey:
          _stringValue(json['taskDefinitionKey'] ?? json['id']) ?? '',
      name: _stringValue(json['name']) ?? '审批节点',
    );
  }
}

class BpmProcessInstance {
  final String id;
  final String name;
  final DateTime? createTime;
  final DateTime? startTime;
  final DateTime? endTime;
  final int? status;
  final String? businessKey;
  final String? category;
  final BpmUser? startUser;
  final List<BpmSummaryItem> summary;
  final Map<String, dynamic> formVariables;

  const BpmProcessInstance({
    required this.id,
    required this.name,
    required this.createTime,
    required this.startTime,
    required this.endTime,
    required this.status,
    required this.businessKey,
    required this.category,
    required this.startUser,
    required this.summary,
    required this.formVariables,
  });

  factory BpmProcessInstance.fromJson(Map<String, dynamic> json) {
    return BpmProcessInstance(
      id: _stringValue(json['id']) ?? '',
      name: _stringValue(json['name']) ?? '审批流程',
      createTime: _parseDateTime(json['createTime']),
      startTime: _parseDateTime(json['startTime']),
      endTime: _parseDateTime(json['endTime']),
      status: _parseInt(json['status'] ?? json['result']),
      businessKey: _stringValue(json['businessKey']),
      category: _stringValue(json['category']),
      startUser: json['startUser'] == null
          ? null
          : BpmUser.fromJson(_asMap(json['startUser'])),
      summary: _asList(
        json['summary'],
      ).map((item) => BpmSummaryItem.fromJson(_asMap(item))).toList(),
      formVariables: _asMap(json['formVariables']),
    );
  }
}

class BpmApprovalDetail {
  final int? status;
  final List<BpmApprovalNode> activityNodes;
  final BpmTaskItem? todoTask;
  final BpmProcessInstance? processInstance;

  const BpmApprovalDetail({
    required this.status,
    required this.activityNodes,
    required this.todoTask,
    required this.processInstance,
  });

  factory BpmApprovalDetail.fromJson(Map<String, dynamic> json) {
    return BpmApprovalDetail(
      status: _parseInt(json['status']),
      activityNodes: _asList(
        json['activityNodes'],
      ).map((item) => BpmApprovalNode.fromJson(_asMap(item))).toList(),
      todoTask: json['todoTask'] == null
          ? null
          : BpmTaskItem.fromJson(_asMap(json['todoTask'])),
      processInstance: json['processInstance'] == null
          ? null
          : BpmProcessInstance.fromJson(_asMap(json['processInstance'])),
    );
  }
}

class BpmApprovalNode {
  final String id;
  final String name;
  final int? nodeType;
  final int? status;
  final DateTime? startTime;
  final DateTime? endTime;
  final List<BpmApprovalNodeTask> tasks;
  final List<BpmUser> candidateUsers;

  const BpmApprovalNode({
    required this.id,
    required this.name,
    required this.nodeType,
    required this.status,
    required this.startTime,
    required this.endTime,
    required this.tasks,
    required this.candidateUsers,
  });

  factory BpmApprovalNode.fromJson(Map<String, dynamic> json) {
    return BpmApprovalNode(
      id: _stringValue(json['id']) ?? '',
      name: _stringValue(json['name']) ?? '审批节点',
      nodeType: _parseInt(json['nodeType']),
      status: _parseInt(json['status']),
      startTime: _parseDateTime(json['startTime']),
      endTime: _parseDateTime(json['endTime']),
      tasks: _asList(
        json['tasks'],
      ).map((item) => BpmApprovalNodeTask.fromJson(_asMap(item))).toList(),
      candidateUsers: _asList(
        json['candidateUsers'],
      ).map((item) => BpmUser.fromJson(_asMap(item))).toList(),
    );
  }
}

class BpmApprovalNodeTask {
  final String id;
  final int? status;
  final String? reason;
  final BpmUser? ownerUser;
  final BpmUser? assigneeUser;

  const BpmApprovalNodeTask({
    required this.id,
    required this.status,
    required this.reason,
    required this.ownerUser,
    required this.assigneeUser,
  });

  factory BpmApprovalNodeTask.fromJson(Map<String, dynamic> json) {
    return BpmApprovalNodeTask(
      id: _stringValue(json['id']) ?? '',
      status: _parseInt(json['status']),
      reason: _stringValue(json['reason']),
      ownerUser: json['ownerUser'] == null
          ? null
          : BpmUser.fromJson(_asMap(json['ownerUser'])),
      assigneeUser: json['assigneeUser'] == null
          ? null
          : BpmUser.fromJson(_asMap(json['assigneeUser'])),
    );
  }

  String get handlerName {
    return assigneeUser?.displayName ?? ownerUser?.displayName ?? '-';
  }
}

class BpmSummaryItem {
  final String key;
  final String value;

  const BpmSummaryItem({required this.key, required this.value});

  factory BpmSummaryItem.fromJson(Map<String, dynamic> json) {
    return BpmSummaryItem(
      key: _stringValue(json['key'] ?? json['label'] ?? json['name']) ?? '字段',
      value: _stringValue(json['value']) ?? '-',
    );
  }
}

class BpmUser {
  final int? id;
  final String username;
  final String nickname;
  final String? avatar;
  final int? deptId;
  final String? deptName;
  final int? tenantId;
  final String? tenantName;
  final String? mobile;
  final String? email;

  const BpmUser({
    required this.id,
    required this.username,
    required this.nickname,
    required this.avatar,
    required this.deptId,
    required this.deptName,
    required this.tenantId,
    required this.tenantName,
    required this.mobile,
    required this.email,
  });

  factory BpmUser.fromJson(Map<String, dynamic> json) {
    return BpmUser(
      id: _parseInt(json['id']),
      username: _stringValue(json['username']) ?? '',
      nickname: _stringValue(json['nickname'] ?? json['name']) ?? '',
      avatar: _stringValue(json['avatar']),
      deptId: _parseInt(json['deptId']),
      deptName: _stringValue(json['deptName']),
      tenantId: _parseInt(json['tenantId']),
      tenantName: _stringValue(json['tenantName']),
      mobile: _stringValue(json['mobile']),
      email: _stringValue(json['email']),
    );
  }

  String get displayName {
    if (nickname.isNotEmpty) return nickname;
    if (username.isNotEmpty) return username;
    return id?.toString() ?? '未知用户';
  }

  String get subtitle {
    final parts = [
      if ((deptName ?? '').trim().isNotEmpty) deptName!.trim(),
      if ((tenantName ?? '').trim().isNotEmpty) tenantName!.trim(),
      if (username.trim().isNotEmpty && username.trim() != displayName)
        username.trim(),
    ];
    return parts.isEmpty ? '组织信息未填写' : parts.join(' / ');
  }
}

class BpmDeptNode {
  final int? id;
  final String name;
  final int? parentId;
  final int? tenantId;
  final int userCount;

  const BpmDeptNode({
    required this.id,
    required this.name,
    required this.parentId,
    required this.tenantId,
    required this.userCount,
  });

  factory BpmDeptNode.fromJson(Map<String, dynamic> json) {
    return BpmDeptNode(
      id: _parseInt(json['id']),
      name: _stringValue(json['name'] ?? json['deptName']) ?? '未命名部门',
      parentId: _parseInt(json['parentId']),
      tenantId: _parseInt(json['tenantId']),
      userCount: _parseInt(json['userCount'] ?? json['employeeCount']) ?? 0,
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
    if (value is Map && value.isEmpty) continue;
    if (value is List && value.isEmpty) continue;
    result[entry.key] = value;
  }
  return result;
}

Map<int, BpmButtonSetting> _parseButtonSettings(dynamic value) {
  final map = _asMap(value);
  if (map.isEmpty) return const {};
  final result = <int, BpmButtonSetting>{};
  for (final entry in map.entries) {
    final key = _parseInt(entry.key);
    if (key == null) continue;
    result[key] = BpmButtonSetting.fromJson(_asMap(entry.value));
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
