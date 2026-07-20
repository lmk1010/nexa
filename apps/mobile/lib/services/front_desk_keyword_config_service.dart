import 'package:shared_preferences/shared_preferences.dart';

class FrontDeskKeywordConfig {
  static const int minKeywords = 1;
  static const int maxKeywords = 5;
  static const List<String> defaultStartKeywords = ['欢迎光临'];
  static const List<String> defaultStopKeywords = ['欢迎下次光临'];
  static const List<String> startCandidates = [
    '欢迎光临',
    '您好欢迎光临',
    '你好欢迎光临',
    '欢迎来到快易修',
    '欢迎来快易修',
    '开始接待',
    '开始录音',
    '接待录音',
  ];
  static const List<String> stopCandidates = [
    '欢迎下次光临',
    '下次光临',
    '谢谢',
    '谢谢光临',
    '谢谢再见',
    '谢谢您',
    '感谢光临',
    '再见',
  ];

  final List<String> startKeywords;
  final List<String> stopKeywords;

  const FrontDeskKeywordConfig({
    required this.startKeywords,
    required this.stopKeywords,
  });

  factory FrontDeskKeywordConfig.defaults() {
    return const FrontDeskKeywordConfig(
      startKeywords: defaultStartKeywords,
      stopKeywords: defaultStopKeywords,
    );
  }

  factory FrontDeskKeywordConfig.sanitize({
    required Iterable<String> startKeywords,
    required Iterable<String> stopKeywords,
  }) {
    return FrontDeskKeywordConfig(
      startKeywords: _sanitizeList(
        startKeywords,
        candidates: startCandidates,
        fallback: defaultStartKeywords,
      ),
      stopKeywords: _sanitizeList(
        stopKeywords,
        candidates: stopCandidates,
        fallback: defaultStopKeywords,
      ),
    );
  }

  Set<String> get startKeywordSet => startKeywords.map(normalize).toSet();
  Set<String> get stopKeywordSet => stopKeywords.map(normalize).toSet();

  static String normalize(String value) {
    return value.replaceAll(RegExp("[\\s,，.。!！?？:：;；\"“”'‘’、-]+"), '').trim();
  }

  static List<String> _sanitizeList(
    Iterable<String> values, {
    required List<String> candidates,
    required List<String> fallback,
  }) {
    final candidateSet = candidates.map(normalize).toSet();
    final selected = <String>[];
    final seen = <String>{};
    for (final value in values) {
      final normalized = normalize(value);
      if (normalized.isEmpty ||
          !candidateSet.contains(normalized) ||
          seen.contains(normalized)) {
        continue;
      }
      selected.add(normalized);
      seen.add(normalized);
      if (selected.length >= maxKeywords) break;
    }
    if (selected.length >= minKeywords) return List.unmodifiable(selected);
    return List.unmodifiable(fallback.map(normalize));
  }
}

class FrontDeskKeywordConfigService {
  static const String _startKeywordsKey = 'front_desk_keyword_start_keywords';
  static const String _stopKeywordsKey = 'front_desk_keyword_stop_keywords';

  Future<FrontDeskKeywordConfig> load() async {
    final prefs = await SharedPreferences.getInstance();
    return FrontDeskKeywordConfig.sanitize(
      startKeywords:
          prefs.getStringList(_startKeywordsKey) ??
          FrontDeskKeywordConfig.defaultStartKeywords,
      stopKeywords:
          prefs.getStringList(_stopKeywordsKey) ??
          FrontDeskKeywordConfig.defaultStopKeywords,
    );
  }

  Future<void> save(FrontDeskKeywordConfig config) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(_startKeywordsKey, config.startKeywords);
    await prefs.setStringList(_stopKeywordsKey, config.stopKeywords);
  }
}
