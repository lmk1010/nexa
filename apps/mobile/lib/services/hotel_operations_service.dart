import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';
import 'api_service.dart';

class HotelOperationsService {
  static const String _ordersKey = 'hotel_front_desk_work_orders_v2';
  static const String _talksKey = 'hotel_front_desk_intercom_v2';
  static HotelPermission? _cachedPermission;
  static String? _cachedPermissionKey;

  static const List<String> stores = ['万达店', '聚云店', '高新店'];
  static const List<String> housekeepers = ['客房大姐'];
  static const List<String> issueTypes = ['客房清洁', '补充物品', '设施维修', '客诉协助', '其他'];

  static Future<HotelPermission> getPermission() async {
    final cacheKey = await _currentPermissionCacheKey();
    try {
      final data = await _get('/permission');
      final permission = HotelPermission.fromJson(_asMap(data));
      _cachedPermission = permission;
      _cachedPermissionKey = cacheKey;
      return permission;
    } catch (_) {
      // 网络抖动/刚进 App token 刷新时，不要把已确认的酒店权限瞬间覆盖成无权限，
      // 否则前台入口会出现“一会有一会没有”的闪烁。只复用当前用户/租户的成功缓存。
      if (_cachedPermission != null && _cachedPermissionKey == cacheKey) {
        return _cachedPermission!;
      }
      // 无成功缓存时必须收敛，避免普通员工看到不该看的模块。
      return HotelPermission.empty;
    }
  }

  static Future<String> _currentPermissionCacheKey() async {
    final cert = await ApiService.getFreshLoginCertificate();
    final tenantId = cert == null ? '' : (cert['tenantId'] ?? '').toString();
    final userId = cert == null
        ? ''
        : (cert['userId'] ?? cert['userID'] ?? '').toString();
    return '$tenantId:$userId';
  }

  static Future<List<HotelWorkOrder>> getWorkOrders({
    String? store,
    bool myOnly = false,
    int pageNo = 1,
    int pageSize = 100,
    String? keyword,
    HotelWorkOrderStatus? status,
  }) async {
    try {
      final data = await _get(myOnly ? '/my-page' : '/page', {
        'pageNo': pageNo,
        'pageSize': pageSize,
        'store': store,
        'keyword': keyword,
        'status': status == null ? null : _statusCode(status),
      });
      final list =
          _extractPageList(
              data,
            ).map((item) => HotelWorkOrder.fromJson(_asMap(item))).toList()
            ..sort((a, b) => b.createTime.compareTo(a.createTime));
      return list;
    } catch (_) {
      final local = await _getLocalWorkOrders();
      Iterable<HotelWorkOrder> filtered = local;
      if (store != null && store.trim().isNotEmpty) {
        filtered = filtered.where((item) => item.store == store);
      }
      if (status != null) {
        filtered = filtered.where((item) => item.status == status);
      }
      final query = keyword?.trim().toLowerCase();
      if (query != null && query.isNotEmpty) {
        filtered = filtered.where((item) => item.matchesKeyword(query));
      }
      return filtered.skip((pageNo - 1) * pageSize).take(pageSize).toList();
    }
  }

  static Future<HotelWorkOrder> getWorkOrder(int id) async {
    try {
      final data = await _get('/get', {'id': id});
      return HotelWorkOrder.fromJson(_asMap(data));
    } catch (_) {
      final local = await _getLocalWorkOrders();
      final matched = local.where((item) => item.id == id);
      if (matched.isNotEmpty) return matched.first;
      throw ApiException('工单不存在或暂时无法加载');
    }
  }

  static Future<List<HotelWorkOrder>> _getLocalWorkOrders() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_ordersKey);
    if (raw == null || raw.trim().isEmpty) return <HotelWorkOrder>[];
    try {
      final list = json.decode(raw) as List;
      return list
          .map(
            (item) => HotelWorkOrder.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList()
        ..sort((a, b) => b.createTime.compareTo(a.createTime));
    } catch (_) {
      return <HotelWorkOrder>[];
    }
  }

  static Future<HotelWorkOrder> createWorkOrder({
    required String store,
    required String title,
    required String type,
    required String priority,
    required String content,
    String roomNo = '',
    String customerEmotion = '平静',
    String assigneeName = '客房大姐',
    int? assigneeUserId,
    String assigneeImUserId = '',
    int? sourceRecordId,
    String sourceRecordTitle = '',
    String source = '手动创建',
  }) async {
    final normalizedTitle = title.trim().isEmpty
        ? _fallbackTitle(type, roomNo)
        : title.trim();
    final normalizedEmotion = customerEmotion.trim().isEmpty
        ? '平静'
        : customerEmotion.trim();
    if (assigneeUserId != null) {
      try {
        final id = await _post('/create', {
          'store': store,
          'roomNo': roomNo.trim(),
          'title': normalizedTitle,
          'type': type,
          'priority': priority,
          'content': content.trim(),
          'source': source,
          'sourceRecordId': sourceRecordId,
          'sourceRecordTitle': sourceRecordTitle,
          'customerEmotion': normalizedEmotion,
          'assigneeUserId': assigneeUserId,
          'assigneeName': assigneeName,
          'assigneeImUserId': assigneeImUserId,
        });
        final orderId = _intValue(id) ?? DateTime.now().microsecondsSinceEpoch;
        try {
          final detail = await _get('/get', {'id': orderId});
          return HotelWorkOrder.fromJson(_asMap(detail));
        } catch (_) {
          return HotelWorkOrder(
            id: orderId,
            store: store,
            roomNo: roomNo.trim(),
            title: normalizedTitle,
            type: type,
            priority: priority,
            status: HotelWorkOrderStatus.pending,
            content: content.trim(),
            source: source,
            assigneeName: assigneeName,
            assigneeUserId: assigneeUserId,
            assigneeImUserId: assigneeImUserId,
            sourceRecordId: sourceRecordId,
            sourceRecordTitle: sourceRecordTitle,
            customerEmotion: normalizedEmotion,
            createTime: DateTime.now(),
            acceptedTime: null,
            finishTime: null,
          );
        }
      } catch (error) {
        // 已选择实际处理人时，正式派单必须写入服务端并生成 OA 待办；
        // 不能静默落本地，避免前台误以为客房/维修已收到。
        throw ApiException('工单未同步，服务端创建失败：$error');
      }
    }

    final order = HotelWorkOrder(
      id: DateTime.now().microsecondsSinceEpoch,
      store: store,
      roomNo: roomNo.trim(),
      title: normalizedTitle,
      type: type,
      priority: priority,
      status: HotelWorkOrderStatus.pending,
      content: content.trim(),
      source: source,
      assigneeName: assigneeName,
      assigneeUserId: assigneeUserId,
      assigneeImUserId: assigneeImUserId,
      sourceRecordId: sourceRecordId,
      sourceRecordTitle: sourceRecordTitle,
      customerEmotion: normalizedEmotion,
      createTime: DateTime.now(),
      acceptedTime: null,
      finishTime: null,
    );
    await saveWorkOrder(order);
    await sendIntercom(
      store: store,
      content: _buildNotifyText(order),
      targetRole: assigneeName,
      linkedOrderId: order.id,
    );
    return order;
  }

  static Future<HotelWorkOrder> updateWorkOrder({
    required int id,
    required String store,
    required String title,
    required String type,
    required String priority,
    required String content,
    String roomNo = '',
    String customerEmotion = '平静',
    required String source,
    int? sourceRecordId,
    String sourceRecordTitle = '',
    required String assigneeName,
    required int assigneeUserId,
    String assigneeImUserId = '',
  }) async {
    final normalizedTitle = title.trim().isEmpty
        ? _fallbackTitle(type, roomNo)
        : title.trim();
    final body = {
      'id': id,
      'store': store,
      'roomNo': roomNo.trim(),
      'title': normalizedTitle,
      'type': type,
      'priority': priority,
      'content': content.trim(),
      'source': source,
      'sourceRecordId': sourceRecordId,
      'sourceRecordTitle': sourceRecordTitle,
      'customerEmotion': customerEmotion.trim().isEmpty
          ? '平静'
          : customerEmotion.trim(),
      'assigneeUserId': assigneeUserId,
      'assigneeName': assigneeName,
      'assigneeImUserId': assigneeImUserId,
    };
    await _put('/update', body);
    final detail = await _get('/get', {'id': id});
    return HotelWorkOrder.fromJson(_asMap(detail));
  }

  static Future<void> deleteWorkOrder(int id) async {
    try {
      await _delete('/delete', {'id': id});
      return;
    } catch (_) {
      final orders = await _getLocalWorkOrders();
      orders.removeWhere((item) => item.id == id);
      await _saveOrders(orders);
    }
  }

  static Future<HotelWorkOrder> createWorkOrderFromSpeech({
    required String store,
    required String speechText,
    String source = '前台一句话',
    int? assigneeUserId,
    String assigneeImUserId = '',
  }) async {
    final analysis = analyzeFrontDeskSpeech(speechText);
    return createWorkOrder(
      store: store,
      title: analysis.title,
      type: analysis.type,
      priority: analysis.priority,
      content: analysis.actionText,
      roomNo: analysis.roomNo,
      customerEmotion: analysis.customerEmotion,
      assigneeName: analysis.assigneeName,
      assigneeUserId: assigneeUserId,
      assigneeImUserId: assigneeImUserId,
      source: source,
    );
  }

  static HotelSpeechAnalysis analyzeFrontDeskSpeech(String rawText) {
    final text = rawText.trim();
    final normalized = text.toLowerCase();
    final roomNo = _extractRoomNo(text);
    final emotion = _detectEmotion(text);
    final priority = _detectPriority(text, emotion);
    final type = _detectIssueType(normalized);
    final action = _buildActionText(text, type, roomNo, emotion, priority);
    return HotelSpeechAnalysis(
      roomNo: roomNo,
      type: type,
      priority: priority,
      customerEmotion: emotion,
      assigneeName: '客房大姐',
      actionText: action,
      title: _fallbackTitle(type, roomNo),
    );
  }

  static Future<void> saveWorkOrder(HotelWorkOrder order) async {
    final orders = await _getLocalWorkOrders();
    final index = orders.indexWhere((item) => item.id == order.id);
    if (index >= 0) {
      orders[index] = order;
    } else {
      orders.insert(0, order);
    }
    await _saveOrders(orders);
  }

  static Future<void> updateOrderStatus(
    int id,
    HotelWorkOrderStatus status, {
    String? remark,
  }) async {
    try {
      await _put('/status', {
        'id': id,
        'status': _statusCode(status),
        'remark': remark,
      });
      return;
    } catch (error) {
      final orders = await _getLocalWorkOrders();
      final isLocalOnly = orders.any((item) => item.id == id);
      if (!isLocalOnly) {
        throw ApiException('工单状态未同步，服务端更新失败：$error');
      }
      // 仅允许历史本地临时单继续本地流转，正式服务端工单不能静默假成功。
    }
    final orders = await _getLocalWorkOrders();
    final index = orders.indexWhere((item) => item.id == id);
    if (index < 0) return;
    final old = orders[index];
    final now = DateTime.now();
    final updated = old.copyWith(
      status: status,
      acceptedTime:
          (status == HotelWorkOrderStatus.doing ||
                  status == HotelWorkOrderStatus.done) &&
              old.acceptedTime == null
          ? now
          : old.acceptedTime,
      finishTime: status == HotelWorkOrderStatus.done ? now : old.finishTime,
      logs: [
        HotelWorkOrderLog(
          id: now.microsecondsSinceEpoch,
          fromStatus: _statusCode(old.status),
          toStatus: _statusCode(status),
          operatorName: '当前用户',
          content: (remark?.trim().isNotEmpty ?? false)
              ? remark!.trim()
              : status.label,
          createTime: now,
        ),
        ...old.logs,
      ],
    );
    orders[index] = updated;
    await _saveOrders(orders);
    await sendIntercom(
      store: updated.store,
      content: _buildStatusText(updated),
      targetRole: updated.assigneeName,
      linkedOrderId: updated.id,
    );
  }

  static Future<List<HotelIntercomMessage>> getIntercomMessages() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_talksKey);
    if (raw == null || raw.trim().isEmpty) return <HotelIntercomMessage>[];
    try {
      final list = json.decode(raw) as List;
      return list
          .map(
            (item) =>
                HotelIntercomMessage.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList()
        ..sort((a, b) => b.createTime.compareTo(a.createTime));
    } catch (_) {
      return <HotelIntercomMessage>[];
    }
  }

  static Future<void> sendIntercom({
    required String store,
    required String content,
    String targetRole = '客房大姐',
    int? linkedOrderId,
  }) async {
    final messages = await getIntercomMessages();
    messages.insert(
      0,
      HotelIntercomMessage(
        id: DateTime.now().microsecondsSinceEpoch,
        store: store,
        targetRole: targetRole,
        content: content.trim().isEmpty ? '前台需要协助' : content.trim(),
        linkedOrderId: linkedOrderId,
        createTime: DateTime.now(),
      ),
    );
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      _talksKey,
      json.encode(messages.take(80).map((item) => item.toJson()).toList()),
    );
  }

  static Future<HotelDashboardData> getDashboard({String? store}) async {
    try {
      final data = await _get('/dashboard', {'store': store});
      return HotelDashboardData.fromJson(_asMap(data));
    } catch (_) {
      final orders = await getWorkOrders(store: store);
      final talks = await getIntercomMessages();
      return HotelDashboardData(orders: orders, talks: talks);
    }
  }

  static Future<int> getBadge() async {
    try {
      return _intValue(await _get('/badge')) ?? 0;
    } catch (_) {
      final cert = await ApiService.getFreshLoginCertificate();
      final currentUserId = _intValue(cert?['userId'] ?? cert?['userID']);
      final orders = await _getLocalWorkOrders();
      final unfinished = orders.where(
        (order) => order.status != HotelWorkOrderStatus.done,
      );
      final mine = currentUserId == null
          ? unfinished
          : unfinished.where(
              (order) =>
                  order.assigneeUserId == null ||
                  order.assigneeUserId == currentUserId,
            );
      return mine.length;
    }
  }

  static Future<void> _saveOrders(List<HotelWorkOrder> orders) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      _ordersKey,
      json.encode(orders.map((item) => item.toJson()).toList()),
    );
  }

  static Future<dynamic> _get(
    String path, [
    Map<String, dynamic>? query,
  ]) async {
    final response = await http
        .get(_adminUri(path, query), headers: await _authorizedHeaders())
        .timeout(const Duration(seconds: 12));
    return _extractData(response, path);
  }

  static Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    final response = await http
        .post(
          _adminUri(path),
          headers: await _authorizedHeaders(),
          body: json.encode(_compactMap(body)),
        )
        .timeout(const Duration(seconds: 15));
    return _extractData(response, path);
  }

  static Future<dynamic> _put(String path, Map<String, dynamic> body) async {
    final response = await http
        .put(
          _adminUri(path),
          headers: await _authorizedHeaders(),
          body: json.encode(_compactMap(body)),
        )
        .timeout(const Duration(seconds: 12));
    return _extractData(response, path);
  }

  static Future<dynamic> _delete(
    String path,
    Map<String, dynamic> query,
  ) async {
    final response = await http
        .delete(_adminUri(path, query), headers: await _authorizedHeaders())
        .timeout(const Duration(seconds: 12));
    return _extractData(response, path);
  }

  static Uri _adminUri(String path, [Map<String, dynamic>? query]) {
    final normalized = path.startsWith('/') ? path : '/$path';
    final uri = Uri.parse(
      '${AppConfig.baseUrl}/admin-api/business/hotel/work-order$normalized',
    );
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
    if (cert == null) throw ApiException('用户未登录，请先登录');
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

  static dynamic _extractData(http.Response response, String path) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('酒店接口请求失败($path)：${response.statusCode}');
    }
    final decoded = json.decode(utf8.decode(response.bodyBytes));
    if (decoded is Map<String, dynamic> && decoded.containsKey('code')) {
      final code = _intValue(decoded['code']);
      if (code == 0) return decoded['data'];
      throw ApiException(decoded['msg']?.toString() ?? '酒店接口请求失败');
    }
    return decoded;
  }

  static Map<String, dynamic> _compactMap(Map<String, dynamic> map) {
    final result = <String, dynamic>{};
    for (final entry in map.entries) {
      final value = entry.value;
      if (value == null) continue;
      if (value is String && value.trim().isEmpty) continue;
      result[entry.key] = value;
    }
    return result;
  }

  static Map<String, dynamic> _asMap(dynamic value) {
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    return <String, dynamic>{};
  }

  static List<dynamic> _extractPageList(dynamic data) {
    if (data is List) return data;
    final map = _asMap(data);
    final value = map['list'] ?? map['records'] ?? map['rows'] ?? map['data'];
    return value is List ? value : <dynamic>[];
  }

  static int _statusCode(HotelWorkOrderStatus status) {
    switch (status) {
      case HotelWorkOrderStatus.pending:
        return 0;
      case HotelWorkOrderStatus.doing:
        return 1;
      case HotelWorkOrderStatus.done:
        return 2;
    }
  }

  static String _extractRoomNo(String text) {
    final patterns = [
      RegExp(r'(?:房间|房号|客房|住在|住的是|是)([A-Za-z]?\d{3,5})'),
      RegExp(r'([A-Za-z]?\d{3,5})(?:房|房间|客房)?'),
    ];
    for (final pattern in patterns) {
      final match = pattern.firstMatch(text);
      final value = match?.group(1)?.trim().toUpperCase();
      if (value != null && value.isNotEmpty) return value;
    }
    return '';
  }

  static String _detectIssueType(String text) {
    if (_containsAny(text, ['打扫', '清洁', '卫生', '脏', '垃圾', '换床单', '换被套'])) {
      return '客房清洁';
    }
    if (_containsAny(text, [
      '毛巾',
      '牙刷',
      '纸巾',
      '矿泉水',
      '拖鞋',
      '洗发水',
      '沐浴露',
      '补',
      '送',
    ])) {
      return '补充物品';
    }
    if (_containsAny(text, [
      '空调',
      '电视',
      '热水',
      '马桶',
      '灯',
      '门锁',
      '漏水',
      '坏',
      '维修',
      '异响',
    ])) {
      return '设施维修';
    }
    if (_containsAny(text, ['投诉', '生气', '不满意', '吵', '退房', '差评', '赔偿', '经理'])) {
      return '客诉协助';
    }
    return '其他';
  }

  static String _detectEmotion(String text) {
    final normalized = text.toLowerCase();
    if (_containsAny(normalized, [
      '很生气',
      '发火',
      '骂',
      '投诉',
      '差评',
      '非常不满意',
      '怒',
    ])) {
      return '生气';
    }
    if (_containsAny(normalized, ['着急', '马上', '立刻', '赶紧', '等很久', '催'])) {
      return '着急';
    }
    if (_containsAny(normalized, ['不满意', '抱怨', '不高兴', '烦'])) {
      return '不满';
    }
    if (_containsAny(normalized, ['谢谢', '麻烦', '可以吗', '帮忙'])) {
      return '平静';
    }
    return '平静';
  }

  static String _detectPriority(String text, String emotion) {
    final normalized = text.toLowerCase();
    if (emotion == '生气' ||
        _containsAny(normalized, ['马上', '立刻', '紧急', '赶紧', '投诉', '差评'])) {
      return '紧急';
    }
    if (emotion == '着急' || _containsAny(normalized, ['尽快', '催一下'])) return '优先';
    return '普通';
  }

  static String _buildActionText(
    String rawText,
    String type,
    String roomNo,
    String emotion,
    String priority,
  ) {
    final parts = <String>[];
    if (roomNo.isNotEmpty) {
      parts.add('房间 $roomNo');
    }
    parts.add(_typeAction(type));
    parts.add('客户情绪：$emotion');
    parts.add('优先级：$priority');
    if (rawText.trim().isNotEmpty) parts.add('原话：${rawText.trim()}');
    return parts.join('；');
  }

  static String _typeAction(String type) {
    switch (type) {
      case '客房清洁':
        return '请安排打扫/清洁';
      case '补充物品':
        return '请补送客用品';
      case '设施维修':
        return '请检查房间设施';
      case '客诉协助':
        return '请优先协助安抚客户';
      default:
        return '请到房间确认并处理';
    }
  }

  static String _fallbackTitle(String type, String roomNo) {
    final prefix = roomNo.trim().isEmpty ? '' : '$roomNo ';
    switch (type) {
      case '客房清洁':
        return '$prefix需要打扫';
      case '补充物品':
        return '$prefix需要补物品';
      case '设施维修':
        return '$prefix设施需要处理';
      case '客诉协助':
        return '$prefix客诉需要协助';
      default:
        return '$prefix客户需求处理';
    }
  }

  static String _buildStatusText(HotelWorkOrder order) {
    final room = order.roomNo.isEmpty ? '' : '${order.roomNo}房，';
    switch (order.status) {
      case HotelWorkOrderStatus.pending:
        return '$room${order.title}，待确认';
      case HotelWorkOrderStatus.doing:
        return '$room${order.title}，客房大姐已收到';
      case HotelWorkOrderStatus.done:
        return '$room${order.title}，已完成';
    }
  }

  static String _buildNotifyText(HotelWorkOrder order) {
    final room = order.roomNo.isEmpty ? '' : '${order.roomNo}房，';
    return '$room${order.title}。${order.content}';
  }

  static bool _containsAny(String text, List<String> keywords) {
    return keywords.any(text.contains);
  }
}

class HotelPermission {
  final bool canUseFrontDesk;
  final bool canViewDashboard;
  final bool canManageWorkOrder;
  final bool canDeleteWorkOrder;
  final bool canViewAllStores;
  final String scopedStore;
  final int? deptId;
  final String deptName;
  final List<String> stores;

  static const empty = HotelPermission(
    canUseFrontDesk: false,
    canViewDashboard: false,
    canManageWorkOrder: false,
    canDeleteWorkOrder: false,
    canViewAllStores: false,
    scopedStore: '',
    stores: [],
  );

  const HotelPermission({
    required this.canUseFrontDesk,
    required this.canViewDashboard,
    required this.canManageWorkOrder,
    required this.canDeleteWorkOrder,
    required this.canViewAllStores,
    this.scopedStore = '',
    this.deptId,
    this.deptName = '',
    this.stores = const [],
  });

  Map<String, dynamic> toJson() => {
    'canUseFrontDesk': canUseFrontDesk,
    'canViewDashboard': canViewDashboard,
    'canManageWorkOrder': canManageWorkOrder,
    'canDeleteWorkOrder': canDeleteWorkOrder,
    'canViewAllStores': canViewAllStores,
    'scopedStore': scopedStore,
    'deptId': deptId,
    'deptName': deptName,
    'stores': stores,
  };

  factory HotelPermission.fromJson(Map<String, dynamic> json) {
    final parsedStores = (json['stores'] is List)
        ? (json['stores'] as List)
              .map((item) => item?.toString().trim() ?? '')
              .where((item) => item.isNotEmpty)
              .toList()
        : <String>[];
    final scoped = _stringValue(json['scopedStore']) ?? '';
    return HotelPermission(
      canUseFrontDesk: _boolValue(json['canUseFrontDesk']),
      canViewDashboard: _boolValue(json['canViewDashboard']),
      canManageWorkOrder: _boolValue(json['canManageWorkOrder']),
      canDeleteWorkOrder: _boolValue(json['canDeleteWorkOrder']),
      canViewAllStores: _boolValue(json['canViewAllStores']),
      scopedStore: scoped,
      deptId: _intValue(json['deptId']),
      deptName: _stringValue(json['deptName']) ?? '',
      stores: parsedStores.isNotEmpty
          ? parsedStores
          : (scoped.isNotEmpty ? <String>[scoped] : const <String>[]),
    );
  }
}

enum HotelWorkOrderStatus { pending, doing, done }

extension HotelWorkOrderStatusText on HotelWorkOrderStatus {
  String get label {
    switch (this) {
      case HotelWorkOrderStatus.pending:
        return '待确认';
      case HotelWorkOrderStatus.doing:
        return '已收到';
      case HotelWorkOrderStatus.done:
        return '已完成';
    }
  }
}

class HotelSpeechAnalysis {
  final String roomNo;
  final String type;
  final String priority;
  final String customerEmotion;
  final String assigneeName;
  final String actionText;
  final String title;

  const HotelSpeechAnalysis({
    required this.roomNo,
    required this.type,
    required this.priority,
    required this.customerEmotion,
    required this.assigneeName,
    required this.actionText,
    required this.title,
  });
}

class HotelWorkOrder {
  final int id;
  final String store;
  final String roomNo;
  final String title;
  final String type;
  final String priority;
  final HotelWorkOrderStatus status;
  final String content;
  final String source;
  final String assigneeName;
  final int? assigneeUserId;
  final String assigneeImUserId;
  final int? sourceRecordId;
  final String sourceRecordTitle;
  final String customerEmotion;
  final DateTime createTime;
  final DateTime? acceptedTime;
  final int? acceptedUserId;
  final String acceptedUserName;
  final DateTime? finishTime;
  final int? finishUserId;
  final String finishUserName;
  final List<HotelWorkOrderLog> logs;

  const HotelWorkOrder({
    required this.id,
    required this.store,
    required this.roomNo,
    required this.title,
    required this.type,
    required this.priority,
    required this.status,
    required this.content,
    required this.source,
    required this.assigneeName,
    required this.assigneeUserId,
    required this.assigneeImUserId,
    required this.sourceRecordId,
    required this.sourceRecordTitle,
    required this.customerEmotion,
    required this.createTime,
    required this.acceptedTime,
    this.acceptedUserId,
    this.acceptedUserName = '',
    required this.finishTime,
    this.finishUserId,
    this.finishUserName = '',
    this.logs = const [],
  });

  HotelWorkOrder copyWith({
    HotelWorkOrderStatus? status,
    DateTime? acceptedTime,
    int? acceptedUserId,
    String? acceptedUserName,
    DateTime? finishTime,
    int? finishUserId,
    String? finishUserName,
    List<HotelWorkOrderLog>? logs,
  }) {
    return HotelWorkOrder(
      id: id,
      store: store,
      roomNo: roomNo,
      title: title,
      type: type,
      priority: priority,
      status: status ?? this.status,
      content: content,
      source: source,
      assigneeName: assigneeName,
      assigneeUserId: assigneeUserId,
      assigneeImUserId: assigneeImUserId,
      sourceRecordId: sourceRecordId,
      sourceRecordTitle: sourceRecordTitle,
      customerEmotion: customerEmotion,
      createTime: createTime,
      acceptedTime: acceptedTime ?? this.acceptedTime,
      acceptedUserId: acceptedUserId ?? this.acceptedUserId,
      acceptedUserName: acceptedUserName ?? this.acceptedUserName,
      finishTime: finishTime ?? this.finishTime,
      finishUserId: finishUserId ?? this.finishUserId,
      finishUserName: finishUserName ?? this.finishUserName,
      logs: logs ?? this.logs,
    );
  }

  factory HotelWorkOrder.fromJson(Map<String, dynamic> json) {
    return HotelWorkOrder(
      id: _intValue(json['id']) ?? 0,
      store: _stringValue(json['store']) ?? HotelOperationsService.stores.first,
      roomNo: _stringValue(json['roomNo']) ?? '',
      title: _stringValue(json['title']) ?? '酒店问题',
      type: _stringValue(json['type']) ?? '其他',
      priority: _stringValue(json['priority']) ?? '普通',
      status: _parseStatus(json['status']),
      content: _stringValue(json['content']) ?? '',
      source: _stringValue(json['source']) ?? '手动创建',
      assigneeName: _stringValue(json['assigneeName']) ?? '客房大姐',
      assigneeUserId: _intValue(json['assigneeUserId']),
      assigneeImUserId: _stringValue(json['assigneeImUserId']) ?? '',
      sourceRecordId: _intValue(json['sourceRecordId']),
      sourceRecordTitle: _stringValue(json['sourceRecordTitle']) ?? '',
      customerEmotion: _stringValue(json['customerEmotion']) ?? '平静',
      createTime: _dateTimeValue(json['createTime']) ?? DateTime.now(),
      acceptedTime: _dateTimeValue(json['acceptedTime']),
      acceptedUserId: _intValue(json['acceptedUserId']),
      acceptedUserName: _stringValue(json['acceptedUserName']) ?? '',
      finishTime: _dateTimeValue(json['finishTime']),
      finishUserId: _intValue(json['finishUserId']),
      finishUserName: _stringValue(json['finishUserName']) ?? '',
      logs: _parseLogs(json['logs']),
    );
  }

  bool matchesKeyword(String keyword) {
    final query = keyword.trim().toLowerCase();
    if (query.isEmpty) return true;
    return [
      title,
      content,
      roomNo,
      type,
      priority,
      customerEmotion,
      assigneeName,
      source,
      sourceRecordTitle,
      acceptedUserName,
      finishUserName,
    ].any((item) => item.toLowerCase().contains(query));
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'store': store,
    'roomNo': roomNo,
    'title': title,
    'type': type,
    'priority': priority,
    'status': status.name,
    'content': content,
    'source': source,
    'assigneeName': assigneeName,
    'assigneeUserId': assigneeUserId,
    'assigneeImUserId': assigneeImUserId,
    'sourceRecordId': sourceRecordId,
    'sourceRecordTitle': sourceRecordTitle,
    'customerEmotion': customerEmotion,
    'createTime': createTime.toIso8601String(),
    'acceptedTime': acceptedTime?.toIso8601String(),
    'acceptedUserId': acceptedUserId,
    'acceptedUserName': acceptedUserName,
    'finishTime': finishTime?.toIso8601String(),
    'finishUserId': finishUserId,
    'finishUserName': finishUserName,
    'logs': logs.map((item) => item.toJson()).toList(),
  };
}

class HotelWorkOrderLog {
  final int id;
  final int? fromStatus;
  final int? toStatus;
  final String operatorName;
  final String content;
  final DateTime? createTime;

  const HotelWorkOrderLog({
    required this.id,
    this.fromStatus,
    this.toStatus,
    this.operatorName = '',
    this.content = '',
    this.createTime,
  });

  factory HotelWorkOrderLog.fromJson(Map<String, dynamic> json) {
    return HotelWorkOrderLog(
      id: _intValue(json['id']) ?? 0,
      fromStatus: _intValue(json['fromStatus']),
      toStatus: _intValue(json['toStatus']),
      operatorName: _stringValue(json['operatorName']) ?? '',
      content: _stringValue(json['content']) ?? '',
      createTime: _dateTimeValue(json['createTime']),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'fromStatus': fromStatus,
    'toStatus': toStatus,
    'operatorName': operatorName,
    'content': content,
    'createTime': createTime?.toIso8601String(),
  };
}

class HotelIntercomMessage {
  final int id;
  final String store;
  final String targetRole;
  final String content;
  final int? linkedOrderId;
  final DateTime createTime;

  const HotelIntercomMessage({
    required this.id,
    required this.store,
    required this.targetRole,
    required this.content,
    required this.linkedOrderId,
    required this.createTime,
  });

  factory HotelIntercomMessage.fromJson(Map<String, dynamic> json) {
    return HotelIntercomMessage(
      id: _intValue(json['id']) ?? 0,
      store: _stringValue(json['store']) ?? HotelOperationsService.stores.first,
      targetRole: _stringValue(json['targetRole']) ?? '客房大姐',
      content: _stringValue(json['content']) ?? '',
      linkedOrderId: _intValue(json['linkedOrderId']),
      createTime: _dateTimeValue(json['createTime']) ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'store': store,
    'targetRole': targetRole,
    'content': content,
    'linkedOrderId': linkedOrderId,
    'createTime': createTime.toIso8601String(),
  };
}

class HotelDashboardData {
  final List<HotelWorkOrder> orders;
  final List<HotelIntercomMessage> talks;

  const HotelDashboardData({required this.orders, required this.talks});

  factory HotelDashboardData.fromJson(Map<String, dynamic> json) {
    final orders = HotelOperationsService._extractPageList(json['orders'])
        .map(
          (item) =>
              HotelWorkOrder.fromJson(HotelOperationsService._asMap(item)),
        )
        .toList();
    return HotelDashboardData(orders: orders, talks: const []);
  }

  int get total => orders.length;
  int get pending => orders
      .where((item) => item.status == HotelWorkOrderStatus.pending)
      .length;
  int get doing =>
      orders.where((item) => item.status == HotelWorkOrderStatus.doing).length;
  int get done =>
      orders.where((item) => item.status == HotelWorkOrderStatus.done).length;
  int get urgent => orders.where((item) => item.priority == '紧急').length;
  int get angry => orders
      .where(
        (item) => item.customerEmotion == '生气' || item.customerEmotion == '不满',
      )
      .length;

  Map<String, int> get storeIssueCounts {
    final map = {for (final store in HotelOperationsService.stores) store: 0};
    for (final order in orders) {
      map[order.store] = (map[order.store] ?? 0) + 1;
    }
    return map;
  }

  Map<String, int> get typeCounts {
    final map = <String, int>{};
    for (final order in orders) {
      map[order.type] = (map[order.type] ?? 0) + 1;
    }
    return map;
  }
}

List<HotelWorkOrderLog> _parseLogs(dynamic value) {
  if (value is! List) return const <HotelWorkOrderLog>[];
  return value
      .map(
        (item) =>
            HotelWorkOrderLog.fromJson(HotelOperationsService._asMap(item)),
      )
      .toList()
    ..sort((a, b) {
      final at = a.createTime ?? DateTime.fromMillisecondsSinceEpoch(0);
      final bt = b.createTime ?? DateTime.fromMillisecondsSinceEpoch(0);
      return bt.compareTo(at);
    });
}

HotelWorkOrderStatus _parseStatus(dynamic value) {
  if (value is HotelWorkOrderStatus) return value;
  if (value is int) {
    switch (value) {
      case 1:
        return HotelWorkOrderStatus.doing;
      case 2:
        return HotelWorkOrderStatus.done;
      default:
        return HotelWorkOrderStatus.pending;
    }
  }
  final text = _stringValue(value);
  if (text == '1') return HotelWorkOrderStatus.doing;
  if (text == '2') return HotelWorkOrderStatus.done;
  return HotelWorkOrderStatus.values.firstWhere(
    (item) => item.name == text,
    orElse: () => HotelWorkOrderStatus.pending,
  );
}

DateTime? _dateTimeValue(dynamic value) {
  if (value == null) return null;
  if (value is DateTime) return value;
  if (value is int) {
    final millis = value > 1000000000000 ? value : value * 1000;
    return DateTime.fromMillisecondsSinceEpoch(millis);
  }
  final text = _stringValue(value);
  if (text == null) return null;
  final numeric = int.tryParse(text);
  if (numeric != null) return _dateTimeValue(numeric);
  return DateTime.tryParse(text);
}

String? _stringValue(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty ? null : text;
}

bool _boolValue(dynamic value) {
  if (value is bool) return value;
  if (value is num) return value != 0;
  final text = value?.toString().trim().toLowerCase();
  return text == 'true' || text == '1' || text == 'yes';
}

int? _intValue(dynamic value) {
  if (value is int) return value;
  return int.tryParse(value?.toString() ?? '');
}
