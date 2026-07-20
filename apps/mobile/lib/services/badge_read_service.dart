import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import 'api_service.dart';

class BadgeReadService {
  static const String _prefix = 'kyx_badge_seen_v1';

  static Future<Set<int>> getSeenIds(String module) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(await _key(module));
    if (raw == null || raw.trim().isEmpty) return <int>{};
    try {
      final decoded = json.decode(raw);
      if (decoded is! List) return <int>{};
      return decoded
          .map((item) => int.tryParse(item.toString()))
          .whereType<int>()
          .toSet();
    } catch (_) {
      return <int>{};
    }
  }

  static Future<void> markSeen(String module, Iterable<int?> ids) async {
    final nextIds = ids.whereType<int>().where((id) => id > 0).toSet();
    if (nextIds.isEmpty) return;
    final prefs = await SharedPreferences.getInstance();
    final key = await _key(module);
    final current = await getSeenIds(module);
    current.addAll(nextIds);
    // 控制本地集合大小，避免长期使用无限增长。保留最近按 id 排序的 1000 条足够覆盖移动端角标。
    final values = current.toList()..sort();
    final trimmed = values.length > 1000
        ? values.sublist(values.length - 1000)
        : values;
    await prefs.setString(key, json.encode(trimmed));
  }

  static Future<int> unseenCount(String module, Iterable<int?> ids) async {
    final values = ids.whereType<int>().where((id) => id > 0).toSet();
    if (values.isEmpty) return 0;
    final seen = await getSeenIds(module);
    return values.where((id) => !seen.contains(id)).length;
  }

  static Future<String> _key(String module) async {
    final cert = await ApiService.getFreshLoginCertificate();
    final tenantId = _stringValue(cert?['tenantId']) ?? 'tenant';
    final userId = _stringValue(cert?['userId'] ?? cert?['userID']) ?? 'user';
    return '$_prefix:$tenantId:$userId:$module';
  }

  static String? _stringValue(dynamic value) {
    final text = value?.toString().trim();
    return text == null || text.isEmpty ? null : text;
  }
}
