import 'dart:convert';

class OaChatShareField {
  final String label;
  final String value;

  const OaChatShareField({required this.label, required this.value});

  Map<String, dynamic> toJson() => {'label': label, 'value': value};
}

class OaChatSharePayload {
  final String module;
  final String objectType;
  final String? objectId;
  final String title;
  final String? status;
  final String? summary;
  final List<OaChatShareField> fields;
  final Map<String, dynamic> extraData;

  const OaChatSharePayload({
    required this.module,
    required this.objectType,
    required this.title,
    this.objectId,
    this.status,
    this.summary,
    this.fields = const [],
    this.extraData = const {},
  });

  String get notificationTitle => '$module通知';

  String get previewText {
    final normalizedTitle = title.trim();
    return normalizedTitle.isEmpty
        ? '[$module] 业务通知'
        : '[$module] $normalizedTitle';
  }

  String get plainText {
    final lines = <String>[
      '【$module】${title.trim().isEmpty ? '业务通知' : title.trim()}',
      if (_hasText(status)) '状态：${status!.trim()}',
      for (final field in fields)
        if (_hasText(field.value)) '${field.label}：${field.value.trim()}',
      if (_hasText(summary)) '说明：${summary!.trim()}',
      '请在 OA App 中查看详情',
    ];
    return lines.join('\n');
  }

  String get customData {
    return jsonEncode({
      'type': 'oa_share',
      'version': 1,
      'module': module,
      'objectType': objectType,
      if (_hasText(objectId)) 'objectId': objectId!.trim(),
      'title': notificationTitle,
      'content': title.trim().isEmpty ? '业务通知' : title.trim(),
      'remark': previewText,
      if (_hasText(status)) 'status': status!.trim(),
      if (_hasText(summary)) 'summary': summary!.trim(),
      if (extraData.isNotEmpty) 'extraData': extraData,
      'fields': fields
          .where((field) => _hasText(field.value))
          .map((field) => field.toJson())
          .toList(),
      'message': _escapeHtml(title.trim().isEmpty ? '业务通知' : title.trim()),
      'extra': _extraHtml,
      'actionText': '查看详情',
    });
  }

  String get _extraHtml {
    final lines = <String>[
      if (_hasText(status)) '状态：${status!.trim()}',
      for (final field in fields)
        if (_hasText(field.value)) '${field.label}：${field.value.trim()}',
      if (_hasText(summary)) '说明：${summary!.trim()}',
    ];
    return lines.map((line) => '<div>${_escapeHtml(line)}</div>').join();
  }

  static bool _hasText(String? value) =>
      value != null && value.trim().isNotEmpty;

  static String _escapeHtml(String value) {
    return const HtmlEscape().convert(value);
  }
}
