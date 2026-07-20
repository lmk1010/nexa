import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_service.dart';
import 'storage_service.dart';

class HrService {
  static Future<HrSelfServiceHome> getSelfServiceHome() async {
    final data = await _get('/hr/self-service/home');
    return HrSelfServiceHome.fromJson(_asMap(data));
  }

  static Future<HrPage<HrApplicationItem>> getApplications({
    int pageNo = 1,
    int pageSize = 20,
    String? businessType,
    String? status,
  }) async {
    final data = await _get('/hr/self-service/application/page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'businessType': businessType,
      'status': status,
    });
    return HrPage.fromJson(data, HrApplicationItem.fromJson);
  }

  static Future<List<HrLeaveBalance>> getMyLeaveBalances({int? year}) async {
    final data = await _get('/hr/leave/balance/my', {'year': year});
    return _asList(data).map((item) => HrLeaveBalance.fromJson(_asMap(item))).toList();
  }

  static Future<HrPage<HrLeaveRecord>> getMyLeavePage({
    int pageNo = 1,
    int pageSize = 20,
  }) async {
    final data = await _get('/hr/administrative/leave/my-page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
    });
    return HrPage.fromJson(data, HrLeaveRecord.fromJson);
  }

  static Future<void> createLeave(HrLeaveCreateRequest request) async {
    await _post('/hr/administrative/leave/create', request.toJson());
  }

  static Future<HrPage<HrPayslip>> getMyPayslips({
    int pageNo = 1,
    int pageSize = 20,
  }) async {
    final data = await _get('/hr/payroll/my-payslip/page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
    });
    return HrPage.fromJson(data, HrPayslip.fromJson);
  }

  static Future<void> confirmMyPayslip(int id) async {
    await _post('/hr/payroll/my-payslip/confirm', {'id': id});
  }

  static Future<void> issueMyPayslip({
    required int id,
    required String issueRemark,
  }) async {
    await _post('/hr/payroll/my-payslip/issue', {
      'id': id,
      'issueRemark': issueRemark,
    });
  }

  static Future<HrPage<HrQuestionnaireAssignment>> getMyQuestionnaires({
    int pageNo = 1,
    int pageSize = 20,
    int? status,
  }) async {
    final data = await _get('/hr/questionnaire-assignment/my-page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'status': status,
    });
    return HrPage.fromJson(data, HrQuestionnaireAssignment.fromJson);
  }

  static Future<HrQuestionnaire> getAccessibleQuestionnaire({
    required int questionnaireId,
    int? assignmentId,
    int? publishId,
  }) async {
    final data = await _get('/hr/questionnaire/accessible-get', {
      'id': questionnaireId,
      'assignmentId': assignmentId,
      'publishId': publishId,
    });
    return HrQuestionnaire.fromJson(_asMap(data));
  }

  static Future<void> submitQuestionnaireAnswers(
    HrQuestionnaireAnswerSubmitRequest request,
  ) async {
    await _post('/hr/questionnaire-answer/accessible-submit', request.toJson());
  }

  static Future<HrPage<HrExamPublish>> getMyExams({
    int pageNo = 1,
    int pageSize = 20,
    int? status,
  }) async {
    final data = await _get('/hr/exam-publish/page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'mine': true,
      'status': status,
    });
    return HrPage.fromJson(data, HrExamPublish.fromJson);
  }

  static Future<HrPage<HrTodoTask>> getMyTodos({
    int pageNo = 1,
    int pageSize = 20,
    String? status = 'OPEN',
  }) async {
    final data = await _get('/hr/todo/page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
      'mine': true,
      'status': status,
    });
    return HrPage.fromJson(data, HrTodoTask.fromJson);
  }

  static Future<void> completeTodo({
    required int id,
    String? remark,
  }) async {
    await _put('/hr/todo/complete', {'id': id, 'remark': remark});
  }

  static Future<HrPage<HrAttendanceRecord>> getMyAttendancePage({
    int pageNo = 1,
    int pageSize = 20,
  }) async {
    final data = await _get('/hr/attendance/my-page', {
      'pageNo': pageNo,
      'pageSize': pageSize,
    });
    return HrPage.fromJson(data, HrAttendanceRecord.fromJson);
  }

  static Future<void> clockIn({required String clockType}) async {
    await _post('/hr/attendance/clock-in', {
      'clockType': clockType,
      'deviceInfo': 'Flutter Mobile',
    });
  }

  static Future<dynamic> _get(
    String path, [
    Map<String, dynamic>? query,
  ]) async {
    final response = await http.get(
      _adminUri(path, query),
      headers: await _authorizedHeaders(),
    ).timeout(_requestTimeout);
    return _extractData(response, path);
  }

  static Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    final response = await http.post(
      _adminUri(path),
      headers: await _authorizedHeaders(),
      body: json.encode(_compactMap(body)),
    ).timeout(_requestTimeout);
    return _extractData(response, path);
  }

  static Future<dynamic> _put(String path, Map<String, dynamic> body) async {
    final response = await http.put(
      _adminUri(path),
      headers: await _authorizedHeaders(),
      body: json.encode(_compactMap(body)),
    ).timeout(_requestTimeout);
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
    final storageService = StorageService();
    final cert = await storageService.getLoginCertificateAsync();
    if (cert == null) {
      throw ApiException('用户未登录，请先登录');
    }

    final token = _resolveToken(cert);
    if (token == null || token.isEmpty) {
      throw ApiException('登录凭证缺少 accessToken，请重新登录');
    }

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
}

const Duration _requestTimeout = Duration(milliseconds: AppConfig.connectionTimeout);

class HrPage<T> {
  final List<T> list;
  final int total;

  const HrPage({required this.list, required this.total});

  factory HrPage.fromJson(dynamic json, T Function(Map<String, dynamic>) parse) {
    final map = _asMap(json);
    final list = _asList(map['list']).map((item) => parse(_asMap(item))).toList();
    return HrPage(list: list, total: _parseInt(map['total']) ?? list.length);
  }
}

class HrSelfServiceHome {
  final bool hasProfile;
  final HrEmployeeProfile? profile;
  final HrEmployment? employment;
  final HrTodayAttendance? todayAttendance;
  final HrTodoSummary? todoSummary;
  final HrProfileHealth? profileHealth;
  final List<HrLifecycleItem> lifecycleItems;
  final List<HrQuickAction> quickActions;

  const HrSelfServiceHome({
    required this.hasProfile,
    this.profile,
    this.employment,
    this.todayAttendance,
    this.todoSummary,
    this.profileHealth,
    required this.lifecycleItems,
    required this.quickActions,
  });

  factory HrSelfServiceHome.fromJson(Map<String, dynamic> json) {
    return HrSelfServiceHome(
      hasProfile: _parseBool(json['hasProfile']),
      profile: json['profile'] == null ? null : HrEmployeeProfile.fromJson(_asMap(json['profile'])),
      employment: json['employment'] == null ? null : HrEmployment.fromJson(_asMap(json['employment'])),
      todayAttendance: json['todayAttendance'] == null
          ? null
          : HrTodayAttendance.fromJson(_asMap(json['todayAttendance'])),
      todoSummary: json['todoSummary'] == null ? null : HrTodoSummary.fromJson(_asMap(json['todoSummary'])),
      profileHealth: json['profileHealth'] == null
          ? null
          : HrProfileHealth.fromJson(_asMap(json['profileHealth'])),
      lifecycleItems: _asList(json['lifecycleItems'])
          .map((item) => HrLifecycleItem.fromJson(_asMap(item)))
          .toList(),
      quickActions: _asList(json['quickActions'])
          .map((item) => HrQuickAction.fromJson(_asMap(item)))
          .toList(),
    );
  }
}

class HrEmployeeProfile {
  final int? profileId;
  final String name;
  final String? profileNo;
  final String? mobile;
  final String? email;
  final String? onboardDate;
  final String? confirmationDate;

  const HrEmployeeProfile({
    this.profileId,
    required this.name,
    this.profileNo,
    this.mobile,
    this.email,
    this.onboardDate,
    this.confirmationDate,
  });

  factory HrEmployeeProfile.fromJson(Map<String, dynamic> json) {
    return HrEmployeeProfile(
      profileId: _parseInt(json['profileId']),
      name: _readString(json, ['name', 'profileName', 'userNickname']) ?? '未匹配员工',
      profileNo: _readString(json, ['profileNo', 'employeeNo']),
      mobile: _readString(json, ['mobile', 'userMobile']),
      email: _readString(json, ['email']),
      onboardDate: _readString(json, ['onboardDate']),
      confirmationDate: _readString(json, ['confirmationDate']),
    );
  }
}

class HrEmployment {
  final int? entryId;
  final String? employeeNo;
  final String? jobTitle;
  final String? workStatusText;
  final String? entryDate;
  final String? contractEndDate;

  const HrEmployment({
    this.entryId,
    this.employeeNo,
    this.jobTitle,
    this.workStatusText,
    this.entryDate,
    this.contractEndDate,
  });

  factory HrEmployment.fromJson(Map<String, dynamic> json) {
    return HrEmployment(
      entryId: _parseInt(json['entryId']),
      employeeNo: _readString(json, ['employeeNo']),
      jobTitle: _readString(json, ['jobTitle', 'positionName']),
      workStatusText: _readString(json, ['workStatusText']),
      entryDate: _readString(json, ['entryDate']),
      contractEndDate: _readString(json, ['contractEndDate']),
    );
  }
}

class HrTodayAttendance {
  final String? attendanceDate;
  final String? clockInTime;
  final String? clockOutTime;
  final String? clockInStatus;
  final String? clockOutStatus;
  final int? monthClockDays;
  final bool onLeaveToday;
  final bool onTripToday;

  const HrTodayAttendance({
    this.attendanceDate,
    this.clockInTime,
    this.clockOutTime,
    this.clockInStatus,
    this.clockOutStatus,
    this.monthClockDays,
    required this.onLeaveToday,
    required this.onTripToday,
  });

  factory HrTodayAttendance.fromJson(Map<String, dynamic> json) {
    return HrTodayAttendance(
      attendanceDate: _readString(json, ['attendanceDate']),
      clockInTime: _readString(json, ['clockInTime', 'firstClockInTime']),
      clockOutTime: _readString(json, ['clockOutTime', 'lastClockOutTime']),
      clockInStatus: _readString(json, ['clockInStatus']),
      clockOutStatus: _readString(json, ['clockOutStatus']),
      monthClockDays: _parseInt(json['monthClockDays']),
      onLeaveToday: _parseBool(json['onLeaveToday']),
      onTripToday: _parseBool(json['onTripToday']),
    );
  }
}

class HrTodoSummary {
  final int pendingQuestionnaireCount;
  final int availableExamCount;
  final int inProgressExamCount;
  final int openTodoCount;
  final int pendingLifecycleTaskCount;
  final int runningLeaveCount;
  final int runningTripCount;

  const HrTodoSummary({
    required this.pendingQuestionnaireCount,
    required this.availableExamCount,
    required this.inProgressExamCount,
    required this.openTodoCount,
    required this.pendingLifecycleTaskCount,
    required this.runningLeaveCount,
    required this.runningTripCount,
  });

  int get totalOpen =>
      pendingQuestionnaireCount +
      availableExamCount +
      inProgressExamCount +
      openTodoCount +
      pendingLifecycleTaskCount +
      runningLeaveCount +
      runningTripCount;

  factory HrTodoSummary.fromJson(Map<String, dynamic> json) {
    return HrTodoSummary(
      pendingQuestionnaireCount: _parseInt(json['pendingQuestionnaireCount']) ?? 0,
      availableExamCount: _parseInt(json['availableExamCount']) ?? 0,
      inProgressExamCount: _parseInt(json['inProgressExamCount']) ?? 0,
      openTodoCount: _parseInt(json['openTodoCount']) ?? 0,
      pendingLifecycleTaskCount: _parseInt(json['pendingLifecycleTaskCount']) ?? 0,
      runningLeaveCount: _parseInt(json['runningLeaveCount']) ?? 0,
      runningTripCount: _parseInt(json['runningTripCount']) ?? 0,
    );
  }
}

class HrProfileHealth {
  final int completeness;
  final List<String> missingFields;

  const HrProfileHealth({
    required this.completeness,
    required this.missingFields,
  });

  factory HrProfileHealth.fromJson(Map<String, dynamic> json) {
    return HrProfileHealth(
      completeness: _parseInt(json['completeness']) ?? 0,
      missingFields: _asList(json['missingFields']).map((item) => item.toString()).toList(),
    );
  }
}

class HrLifecycleItem {
  final int? eventId;
  final String? eventTypeName;
  final String? eventStatusText;
  final String? effectiveDate;

  const HrLifecycleItem({
    this.eventId,
    this.eventTypeName,
    this.eventStatusText,
    this.effectiveDate,
  });

  factory HrLifecycleItem.fromJson(Map<String, dynamic> json) {
    return HrLifecycleItem(
      eventId: _parseInt(json['eventId']),
      eventTypeName: _readString(json, ['eventTypeName', 'eventType']),
      eventStatusText: _readString(json, ['eventStatusText', 'eventStatus']),
      effectiveDate: _readString(json, ['effectiveDate']),
    );
  }
}

class HrQuickAction {
  final String title;
  final String? icon;
  final String path;
  final String? category;

  const HrQuickAction({
    required this.title,
    this.icon,
    required this.path,
    this.category,
  });

  factory HrQuickAction.fromJson(Map<String, dynamic> json) {
    return HrQuickAction(
      title: _readString(json, ['title', 'actionName']) ?? '未命名入口',
      icon: _readString(json, ['icon']),
      path: _readString(json, ['path', 'routePath']) ?? '',
      category: _readString(json, ['category']),
    );
  }
}

class HrApplicationItem {
  final String? applyTime;
  final int? businessId;
  final String businessType;
  final String? title;
  final String? summary;
  final String? statusText;
  final String? startTime;
  final String? endTime;

  const HrApplicationItem({
    this.applyTime,
    this.businessId,
    required this.businessType,
    this.title,
    this.summary,
    this.statusText,
    this.startTime,
    this.endTime,
  });

  factory HrApplicationItem.fromJson(Map<String, dynamic> json) {
    return HrApplicationItem(
      applyTime: _readString(json, ['applyTime']),
      businessId: _parseInt(json['businessId']),
      businessType: _readString(json, ['businessType']) ?? '',
      title: _readString(json, ['title']),
      summary: _readString(json, ['summary']),
      statusText: _readString(json, ['statusText', 'status']),
      startTime: _readString(json, ['startTime']),
      endTime: _readString(json, ['endTime']),
    );
  }
}

class HrLeaveBalance {
  final String leaveTypeCode;
  final String leaveTypeName;
  final int? year;
  final double totalAmount;
  final double usedAmount;
  final double frozenAmount;
  final double remainAmount;

  const HrLeaveBalance({
    required this.leaveTypeCode,
    required this.leaveTypeName,
    this.year,
    required this.totalAmount,
    required this.usedAmount,
    required this.frozenAmount,
    required this.remainAmount,
  });

  factory HrLeaveBalance.fromJson(Map<String, dynamic> json) {
    final code = _readString(json, ['leaveTypeCode']) ?? '';
    return HrLeaveBalance(
      leaveTypeCode: code,
      leaveTypeName: _readString(json, ['leaveTypeName']) ?? _defaultLeaveTypeName(code),
      year: _parseInt(json['year']),
      totalAmount: _parseDouble(json['totalAmount']) ?? 0,
      usedAmount: _parseDouble(json['usedAmount']) ?? 0,
      frozenAmount: _parseDouble(json['frozenAmount']) ?? 0,
      remainAmount: _parseDouble(json['remainAmount']) ?? 0,
    );
  }
}

class HrLeaveRecord {
  final int? id;
  final String leaveType;
  final String leaveCategory;
  final String? startTime;
  final String? endTime;
  final double? duration;
  final int? status;
  final String? remark;
  final String? createTime;

  const HrLeaveRecord({
    this.id,
    required this.leaveType,
    required this.leaveCategory,
    this.startTime,
    this.endTime,
    this.duration,
    this.status,
    this.remark,
    this.createTime,
  });

  factory HrLeaveRecord.fromJson(Map<String, dynamic> json) {
    return HrLeaveRecord(
      id: _parseInt(json['id']),
      leaveType: _readString(json, ['leaveType']) ?? '',
      leaveCategory: _readString(json, ['leaveCategory']) ?? '',
      startTime: _readString(json, ['startTime']),
      endTime: _readString(json, ['endTime']),
      duration: _parseDouble(json['duration']),
      status: _parseInt(json['status']),
      remark: _readString(json, ['remark']),
      createTime: _readString(json, ['createTime']),
    );
  }
}

class HrLeaveCreateRequest {
  final String leaveCategory;
  final String leaveType;
  final DateTime startTime;
  final DateTime endTime;
  final double duration;
  final String emergencyPhone;
  final String workHandover;
  final String? remark;

  const HrLeaveCreateRequest({
    required this.leaveCategory,
    required this.leaveType,
    required this.startTime,
    required this.endTime,
    required this.duration,
    required this.emergencyPhone,
    required this.workHandover,
    this.remark,
  });

  Map<String, dynamic> toJson() {
    return {
      'leaveCategory': leaveCategory,
      'leaveType': leaveType,
      'startTime': _formatDateTime(startTime),
      'endTime': _formatDateTime(endTime),
      'duration': double.parse(duration.toStringAsFixed(2)),
      'emergencyPhone': emergencyPhone,
      'workHandover': workHandover,
      'remark': remark,
      'attachments': <String>[],
    };
  }
}

class HrPayslip {
  final int? id;
  final String? payrollMonth;
  final String? currency;
  final double baseSalary;
  final double attendanceDeduction;
  final double overtimePay;
  final double bonus;
  final double allowance;
  final double deduction;
  final double socialInsurance;
  final double housingFund;
  final double tax;
  final double netSalary;
  final String? status;
  final String? confirmedTime;
  final String? issueRemark;

  const HrPayslip({
    this.id,
    this.payrollMonth,
    this.currency,
    required this.baseSalary,
    required this.attendanceDeduction,
    required this.overtimePay,
    required this.bonus,
    required this.allowance,
    required this.deduction,
    required this.socialInsurance,
    required this.housingFund,
    required this.tax,
    required this.netSalary,
    this.status,
    this.confirmedTime,
    this.issueRemark,
  });

  factory HrPayslip.fromJson(Map<String, dynamic> json) {
    return HrPayslip(
      id: _parseInt(json['id']),
      payrollMonth: _readString(json, ['payrollMonth']),
      currency: _readString(json, ['currency']) ?? 'CNY',
      baseSalary: _parseDouble(json['baseSalary']) ?? 0,
      attendanceDeduction: _parseDouble(json['attendanceDeduction']) ?? 0,
      overtimePay: _parseDouble(json['overtimePay']) ?? 0,
      bonus: _parseDouble(json['bonus']) ?? 0,
      allowance: _parseDouble(json['allowance']) ?? 0,
      deduction: _parseDouble(json['deduction']) ?? 0,
      socialInsurance: _parseDouble(json['socialInsurance']) ?? 0,
      housingFund: _parseDouble(json['housingFund']) ?? 0,
      tax: _parseDouble(json['tax']) ?? 0,
      netSalary: _parseDouble(json['netSalary']) ?? 0,
      status: _readString(json, ['status']),
      confirmedTime: _readString(json, ['confirmedTime']),
      issueRemark: _readString(json, ['issueRemark']),
    );
  }
}

class HrQuestionnaireAssignment {
  final int? id;
  final int? questionnaireId;
  final int? publishId;
  final String? batchLabel;
  final String? targetName;
  final String? role;
  final int? status;

  const HrQuestionnaireAssignment({
    this.id,
    this.questionnaireId,
    this.publishId,
    this.batchLabel,
    this.targetName,
    this.role,
    this.status,
  });

  factory HrQuestionnaireAssignment.fromJson(Map<String, dynamic> json) {
    return HrQuestionnaireAssignment(
      id: _parseInt(json['id']),
      questionnaireId: _parseInt(json['questionnaireId']),
      publishId: _parseInt(json['publishId']),
      batchLabel: _readString(json, ['batchLabel']),
      targetName: _readString(json, ['targetName']),
      role: _readString(json, ['role']),
      status: _parseInt(json['status']),
    );
  }
}

class HrQuestionnaire {
  final int? id;
  final String name;
  final String? type;
  final int? status;
  final String multiScoreMode;
  final List<HrQuestionnaireItem> items;

  const HrQuestionnaire({
    this.id,
    required this.name,
    this.type,
    this.status,
    required this.multiScoreMode,
    required this.items,
  });

  factory HrQuestionnaire.fromJson(Map<String, dynamic> json) {
    return HrQuestionnaire(
      id: _parseInt(json['id']),
      name: _readString(json, ['name', 'title']) ?? '问卷填写',
      type: _readString(json, ['type']),
      status: _parseInt(json['status']),
      multiScoreMode: _parseMultiScoreMode(json['targetRuleJson']),
      items: _asList(json['items'])
          .map((item) => HrQuestionnaireItem.fromJson(_asMap(item)))
          .toList(),
    );
  }
}

class HrQuestionnaireItem {
  final int? id;
  final String title;
  final String itemType;
  final bool required;
  final double maxScore;
  final int sortNo;
  final List<HrQuestionnaireOption> options;

  const HrQuestionnaireItem({
    this.id,
    required this.title,
    required this.itemType,
    required this.required,
    required this.maxScore,
    required this.sortNo,
    required this.options,
  });

  factory HrQuestionnaireItem.fromJson(Map<String, dynamic> json) {
    return HrQuestionnaireItem(
      id: _parseInt(json['id']),
      title: _readString(json, ['title']) ?? '未命名题目',
      itemType: _readString(json, ['itemType']) ?? 'text',
      required: _parseBool(json['required']),
      maxScore: _parseDouble(json['maxScore']) ?? 10,
      sortNo: _parseInt(json['sortNo']) ?? 0,
      options: _asList(json['options'])
          .map((item) => HrQuestionnaireOption.fromJson(_asMap(item)))
          .toList(),
    );
  }
}

class HrQuestionnaireOption {
  final int? id;
  final String optionText;
  final double? optionScore;
  final int sortNo;

  const HrQuestionnaireOption({
    this.id,
    required this.optionText,
    this.optionScore,
    required this.sortNo,
  });

  factory HrQuestionnaireOption.fromJson(Map<String, dynamic> json) {
    return HrQuestionnaireOption(
      id: _parseInt(json['id']),
      optionText: _readString(json, ['optionText']) ?? '',
      optionScore: _parseDouble(json['optionScore']),
      sortNo: _parseInt(json['sortNo']) ?? 0,
    );
  }
}

class HrQuestionnaireAnswerSubmitRequest {
  final int assignmentId;
  final int questionnaireId;
  final List<HrQuestionnaireAnswerItem> answers;

  const HrQuestionnaireAnswerSubmitRequest({
    required this.assignmentId,
    required this.questionnaireId,
    required this.answers,
  });

  Map<String, dynamic> toJson() {
    return {
      'assignmentId': assignmentId,
      'questionnaireId': questionnaireId,
      'answers': answers.map((item) => item.toJson()).toList(),
    };
  }
}

class HrQuestionnaireAnswerItem {
  final int itemId;
  final String? answerText;
  final double? answerScore;
  final String? answerJson;

  const HrQuestionnaireAnswerItem({
    required this.itemId,
    this.answerText,
    this.answerScore,
    this.answerJson,
  });

  Map<String, dynamic> toJson() {
    return {
      'itemId': itemId,
      'answerText': answerText,
      'answerScore': answerScore,
      'answerJson': answerJson,
    };
  }
}

class HrExamPublish {
  final int? id;
  final int? examId;
  final String? examName;
  final String? batchLabel;
  final String? startAt;
  final String? endAt;
  final int? durationMin;
  final int? maxAttempts;
  final int? status;

  const HrExamPublish({
    this.id,
    this.examId,
    this.examName,
    this.batchLabel,
    this.startAt,
    this.endAt,
    this.durationMin,
    this.maxAttempts,
    this.status,
  });

  factory HrExamPublish.fromJson(Map<String, dynamic> json) {
    return HrExamPublish(
      id: _parseInt(json['id']),
      examId: _parseInt(json['examId']),
      examName: _readString(json, ['examName']),
      batchLabel: _readString(json, ['batchLabel']),
      startAt: _readString(json, ['startAt']),
      endAt: _readString(json, ['endAt']),
      durationMin: _parseInt(json['durationMin']),
      maxAttempts: _parseInt(json['maxAttempts']),
      status: _parseInt(json['status']),
    );
  }
}

class HrTodoTask {
  final int? id;
  final String title;
  final String? content;
  final String? businessType;
  final String status;
  final String priority;
  final String? dueTime;
  final String? createTime;

  const HrTodoTask({
    this.id,
    required this.title,
    this.content,
    this.businessType,
    required this.status,
    required this.priority,
    this.dueTime,
    this.createTime,
  });

  factory HrTodoTask.fromJson(Map<String, dynamic> json) {
    return HrTodoTask(
      id: _parseInt(json['id']),
      title: _readString(json, ['title']) ?? '未命名待办',
      content: _readString(json, ['content']),
      businessType: _readString(json, ['businessType']),
      status: _readString(json, ['status']) ?? '',
      priority: _readString(json, ['priority']) ?? '',
      dueTime: _readString(json, ['dueTime']),
      createTime: _readString(json, ['createTime']),
    );
  }
}

class HrAttendanceRecord {
  final int? id;
  final String? attendanceDate;
  final String? clockType;
  final String? clockTime;
  final String? clockStatus;
  final String? sourceType;
  final String? locationName;

  const HrAttendanceRecord({
    this.id,
    this.attendanceDate,
    this.clockType,
    this.clockTime,
    this.clockStatus,
    this.sourceType,
    this.locationName,
  });

  factory HrAttendanceRecord.fromJson(Map<String, dynamic> json) {
    return HrAttendanceRecord(
      id: _parseInt(json['id']),
      attendanceDate: _readString(json, ['attendanceDate']),
      clockType: _readString(json, ['clockType']),
      clockTime: _readString(json, ['clockTime']),
      clockStatus: _readString(json, ['clockStatus']),
      sourceType: _readString(json, ['sourceType']),
      locationName: _readString(json, ['locationName']),
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
  return <String, dynamic>{};
}

List<dynamic> _asList(dynamic value) {
  if (value is List) return value;
  return const [];
}

String? _readString(Map<String, dynamic> json, List<String> keys) {
  for (final key in keys) {
    final value = json[key];
    if (value == null) continue;
    final text = value.toString();
    if (text.isNotEmpty) return text;
  }
  return null;
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

String _parseMultiScoreMode(dynamic targetRuleJson) {
  if (targetRuleJson == null) return 'none';
  try {
    final decoded = json.decode(targetRuleJson.toString());
    if (decoded is Map) {
      final mode = decoded['scoreConfig'] is Map
          ? decoded['scoreConfig']['multiScoreMode']?.toString()
          : null;
      if (mode == 'avg' || mode == 'max' || mode == 'sum') return mode!;
    }
  } catch (_) {
    return 'none';
  }
  return 'none';
}

String _formatDateTime(DateTime value) {
  String two(int number) => number.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)} '
      '${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
}

String _defaultLeaveTypeName(String code) {
  switch (code) {
    case 'annual':
      return '年假';
    case 'rest':
      return '休息';
    case 'personal':
      return '事假';
    case 'sick':
      return '病假';
    case 'marriage':
      return '婚假';
    case 'maternity':
      return '产假';
    case 'dingtalk':
      return '钉钉请假';
    default:
      return code.isEmpty ? '假期' : code;
  }
}
