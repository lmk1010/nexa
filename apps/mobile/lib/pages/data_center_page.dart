// 数据中心 —— 明细导出平台入口页
// 4 tabs: 模板 / 我的导出 / 大厅 / 预约
import 'dart:async';
import 'package:flutter/material.dart';

import '../services/data_center_service.dart';
import '../services/file_download_service.dart';

class DataCenterPage extends StatefulWidget {
  const DataCenterPage({super.key});

  @override
  State<DataCenterPage> createState() => _DataCenterPageState();
}

class _DataCenterPageState extends State<DataCenterPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tab;

  @override
  void initState() {
    super.initState();
    _tab = TabController(length: 4, vsync: this);
  }

  @override
  void dispose() {
    _tab.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _DCTheme.canvas,
      appBar: AppBar(
        backgroundColor: _DCTheme.canvas,
        foregroundColor: _DCTheme.ink,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleSpacing: 20,
        title: const Text('数据中心',
            style: TextStyle(
                fontSize: 17, fontWeight: FontWeight.w700, letterSpacing: 0.2)),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(36),
          child: Align(
            alignment: Alignment.centerLeft,
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.only(left: 12),
              child: TabBar(
                controller: _tab,
                isScrollable: true,
                tabAlignment: TabAlignment.start,
                indicatorColor: _DCTheme.accent,
                indicatorWeight: 2,
                indicatorSize: TabBarIndicatorSize.label,
                indicatorPadding: const EdgeInsets.only(bottom: 2),
                dividerColor: Colors.transparent,
                labelColor: _DCTheme.ink,
                unselectedLabelColor: _DCTheme.dim,
                labelPadding: const EdgeInsets.symmetric(horizontal: 12),
                labelStyle: const TextStyle(
                    fontSize: 13.5, fontWeight: FontWeight.w700, letterSpacing: 0.2),
                unselectedLabelStyle: const TextStyle(
                    fontSize: 13.5, fontWeight: FontWeight.w500, letterSpacing: 0.2),
                tabs: const [
                  Tab(text: '模板', height: 32),
                  Tab(text: '我的导出', height: 32),
                  Tab(text: '公共大厅', height: 32),
                  Tab(text: '预约', height: 32),
                ],
              ),
            ),
          ),
        ),
      ),
      body: TabBarView(
        controller: _tab,
        children: [
          _TemplatesTab(onCreated: () => _tab.animateTo(1)),
          const _MyJobsTab(),
          const _HallTab(),
          const _ScheduleTab(),
        ],
      ),
    );
  }
}

class _DCTheme {
  static const canvas = Color(0xFFF7F7F5);
  static const ink = Color(0xFF0E0E10);
  static const dim = Color(0xFF6E6E76);
  static const faint = Color(0xFFB0B0B6);
  static const hairline = Color(0xFFE8E8E4);
  static const accent = Color(0xFF1D4ED8);
  static const surface = Colors.white;
  static const success = Color(0xFF10B981);
  static const warn = Color(0xFFF59E0B);
  static const danger = Color(0xFFEF4444);
}

// ══════════════════════════════════════════════════
// Tab 1: 模板卡片 → 过滤 → 提交
// ══════════════════════════════════════════════════

class _TemplatesTab extends StatefulWidget {
  final VoidCallback onCreated;
  const _TemplatesTab({required this.onCreated});

  @override
  State<_TemplatesTab> createState() => _TemplatesTabState();
}

class _TemplatesTabState extends State<_TemplatesTab> {
  late Future<List<TemplateInfo>> _future;

  @override
  void initState() {
    super.initState();
    _future = DataCenterService.listTemplates();
  }

  Future<void> _refresh() async {
    setState(() {
      _future = DataCenterService.listTemplates();
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _refresh,
      color: _DCTheme.ink,
      child: FutureBuilder<List<TemplateInfo>>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            return _ErrorHint(msg: snap.error.toString(), onRetry: _refresh);
          }
          final list = snap.data ?? const [];
          if (list.isEmpty) {
            return const _EmptyHint(text: '暂无可用模板');
          }
          // 按 category 分组
          final byCat = <String, List<TemplateInfo>>{};
          for (final t in list) {
            byCat.putIfAbsent(t.category.isEmpty ? '其他' : t.category, () => [])
                .add(t);
          }
          final cats = byCat.keys.toList()..sort();
          return ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
            children: [
              for (final c in cats) ...[
                Padding(
                  padding: const EdgeInsets.only(top: 12, bottom: 8),
                  child: Text(c,
                      style: const TextStyle(
                          fontSize: 13,
                          color: _DCTheme.dim,
                          letterSpacing: 0.3,
                          fontWeight: FontWeight.w600)),
                ),
                for (final t in byCat[c]!) _TemplateCard(
                  template: t,
                  onTap: () => _openFilter(t),
                ),
              ],
            ],
          );
        },
      ),
    );
  }

  Future<void> _openFilter(TemplateInfo t) async {
    final filters = await showModalBottomSheet<Map<String, dynamic>>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _FilterSheet(template: t),
    );
    if (filters == null || !mounted) return;
    try {
      final resp = await DataCenterService.createJob(
        templateId: t.id,
        filters: filters,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('已入队（第 ${resp.queuePos} 位）', style: const TextStyle(fontSize: 14)),
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
      ));
      widget.onCreated();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('提交失败: $e'),
        backgroundColor: _DCTheme.danger,
      ));
    }
  }
}

class _TemplateCard extends StatelessWidget {
  final TemplateInfo template;
  final VoidCallback onTap;
  const _TemplateCard({required this.template, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final color = _parseHex(template.color) ?? _DCTheme.accent;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: _DCTheme.surface,
        borderRadius: BorderRadius.circular(14),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(_iconFor(template.icon),
                      size: 22, color: color),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(template.label,
                          style: const TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: _DCTheme.ink,
                              letterSpacing: -0.1)),
                      const SizedBox(height: 3),
                      Text(
                        template.description.isEmpty
                            ? '导出为 xlsx'
                            : template.description,
                        style: const TextStyle(
                            fontSize: 12, color: _DCTheme.dim, height: 1.35),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: _DCTheme.faint, size: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════
// FilterSheet: 底弹起过滤面板
// ══════════════════════════════════════════════════

class _FilterSheet extends StatefulWidget {
  final TemplateInfo template;
  const _FilterSheet({required this.template});

  @override
  State<_FilterSheet> createState() => _FilterSheetState();
}

class _FilterSheetState extends State<_FilterSheet> {
  final Map<String, dynamic> _values = {};

  @override
  void initState() {
    super.initState();
    // 默认: date range = 近 30 天
    final now = DateTime.now();
    final from = now.subtract(const Duration(days: 29));
    for (final e in widget.template.filters.entries) {
      if (e.value.type == 'range') {
        _values[e.key] = {
          'from': _fmtDate(from),
          'to': _fmtDate(now),
        };
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = widget.template;
    final filterKeys = t.filters.keys.toList()..sort();
    return DraggableScrollableSheet(
      initialChildSize: 0.65,
      minChildSize: 0.4,
      maxChildSize: 0.92,
      builder: (context, scrollCtrl) => Container(
        decoration: const BoxDecoration(
          color: _DCTheme.surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
        ),
        child: Column(
          children: [
            Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(top: 8, bottom: 8),
              decoration: BoxDecoration(
                color: _DCTheme.hairline,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
              child: Row(
                children: [
                  Text('导出：${t.label}',
                      style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: _DCTheme.ink)),
                  const Spacer(),
                  Text('最多 ${_kNum(t.maxRows)} 行',
                      style: const TextStyle(
                          fontSize: 12, color: _DCTheme.dim)),
                ],
              ),
            ),
            const Divider(color: _DCTheme.hairline, height: 1),
            Expanded(
              child: ListView(
                controller: scrollCtrl,
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 100),
                children: [
                  for (final key in filterKeys)
                    _FilterField(
                      keyName: key,
                      filter: t.filters[key]!,
                      enums: t.enums,
                      value: _values[key],
                      onChanged: (v) => setState(() => _values[key] = v),
                    ),
                ],
              ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                child: Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => Navigator.pop(context),
                        style: OutlinedButton.styleFrom(
                            side: const BorderSide(color: _DCTheme.hairline),
                            padding: const EdgeInsets.symmetric(vertical: 12)),
                        child: const Text('取消',
                            style: TextStyle(color: _DCTheme.dim)),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      flex: 2,
                      child: ElevatedButton(
                        onPressed: _submit,
                        style: ElevatedButton.styleFrom(
                            backgroundColor: _DCTheme.ink,
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(10))),
                        child: const Text('提交导出',
                            style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w700)),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _submit() {
    // 校验必填
    for (final e in widget.template.filters.entries) {
      if (e.value.required) {
        final v = _values[e.key];
        if (v == null ||
            (v is Map && v.isEmpty) ||
            (v is List && v.isEmpty) ||
            (v is String && v.isEmpty)) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text('请填写「${e.value.label.isEmpty ? e.key : e.value.label}」'),
            backgroundColor: _DCTheme.warn,
          ));
          return;
        }
      }
    }
    Navigator.pop(context, _values);
  }
}

class _FilterField extends StatelessWidget {
  final String keyName;
  final TemplateFilter filter;
  final Map<String, Map<String, String>> enums;
  final dynamic value;
  final ValueChanged<dynamic> onChanged;

  const _FilterField({
    required this.keyName,
    required this.filter,
    required this.enums,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final label = filter.label.isEmpty ? keyName : filter.label;
    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Text(label,
                style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _DCTheme.ink)),
            if (filter.required)
              const Text(' *',
                  style: TextStyle(color: _DCTheme.danger, fontSize: 13)),
          ]),
          const SizedBox(height: 8),
          _buildInput(context),
        ],
      ),
    );
  }

  Widget _buildInput(BuildContext context) {
    switch (filter.type) {
      case 'range':
        final m = (value is Map) ? Map<String, dynamic>.from(value) : {};
        String fmt(DateTime d) => '${d.year}-${d.month.toString().padLeft(2,'0')}-${d.day.toString().padLeft(2,'0')}';
        void applyPreset(DateTime from, DateTime to) {
          onChanged({'from': fmt(from), 'to': fmt(to)});
        }
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: _DateField(
                    value: m['from']?.toString(),
                    hint: '起始',
                    onChanged: (v) => onChanged({...m, 'from': v}),
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 8),
                  child: Text('至', style: TextStyle(color: _DCTheme.dim)),
                ),
                Expanded(
                  child: _DateField(
                    value: m['to']?.toString(),
                    hint: '截止',
                    onChanged: (v) => onChanged({...m, 'to': v}),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            // 快捷选择：省点手输日期。用户可继续手动改精确到日
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: [
                _DatePresetChip(label: '今天', onTap: () => applyPreset(today, today)),
                _DatePresetChip(label: '昨天', onTap: () {
                  final y = today.subtract(const Duration(days: 1));
                  applyPreset(y, y);
                }),
                _DatePresetChip(label: '近7天', onTap: () =>
                    applyPreset(today.subtract(const Duration(days: 6)), today)),
                _DatePresetChip(label: '近30天', onTap: () =>
                    applyPreset(today.subtract(const Duration(days: 29)), today)),
                _DatePresetChip(label: '本月', onTap: () =>
                    applyPreset(DateTime(now.year, now.month, 1), today)),
                _DatePresetChip(label: '上月', onTap: () {
                  final firstThis = DateTime(now.year, now.month, 1);
                  final lastLast = firstThis.subtract(const Duration(days: 1));
                  final firstLast = DateTime(lastLast.year, lastLast.month, 1);
                  applyPreset(firstLast, lastLast);
                }),
                _DatePresetChip(label: '本季', onTap: () {
                  final qStart = DateTime(now.year, ((now.month - 1) ~/ 3) * 3 + 1, 1);
                  applyPreset(qStart, today);
                }),
                _DatePresetChip(label: '今年', onTap: () =>
                    applyPreset(DateTime(now.year, 1, 1), today)),
              ],
            ),
          ],
        );
      case 'in':
        final enumMap = filter.enumKey != null ? enums[filter.enumKey] : null;
        if (enumMap == null) {
          return _TextField(
            value: value is List ? (value as List).join(',') : '',
            hint: '多个值用逗号分隔',
            onChanged: (s) {
              final parts = s.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
              onChanged(parts);
            },
          );
        }
        // Multi-select chips
        final selected = (value is List)
            ? (value as List).map((e) => e.toString()).toSet()
            : <String>{};
        return Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final entry in enumMap.entries)
              _EnumChip(
                label: entry.value,
                selected: selected.contains(entry.key),
                onTap: () {
                  final next = Set<String>.from(selected);
                  if (!next.remove(entry.key)) next.add(entry.key);
                  onChanged(next.toList());
                },
              ),
          ],
        );
      case 'lookup':
        final selected = (value is List)
            ? (value as List).map((e) => e.toString()).toList()
            : <String>[];
        return _LookupField(
          lookupName: filter.lookupName ?? '',
          selectedIds: selected,
          hint: '搜索或输入 ID',
          onChanged: (ids) => onChanged(ids),
        );
      case 'like':
      case 'eq':
        return _TextField(
          value: value?.toString() ?? '',
          hint: '输入关键字',
          onChanged: onChanged,
        );
    }
    return const SizedBox.shrink();
  }
}

// 可搜索多选下拉 —— compact 触发条：点击唤起全屏 bottom sheet 选择；
// 表单里始终只占一行/一 chip 行的高度，不再挤爆界面。
class _LookupField extends StatefulWidget {
  final String lookupName;
  final List<String> selectedIds;
  final String hint;
  final ValueChanged<List<String>> onChanged;
  const _LookupField({
    required this.lookupName,
    required this.selectedIds,
    required this.hint,
    required this.onChanged,
  });
  @override
  State<_LookupField> createState() => _LookupFieldState();
}

class _LookupFieldState extends State<_LookupField> {
  // 记录 id → label，方便 chip 直接展示中文（重新打开 picker 也复用）
  final Map<String, LookupItem> _labelCache = {};

  @override
  void initState() {
    super.initState();
    for (final id in widget.selectedIds) {
      _labelCache[id] = LookupItem(id: id, label: id);
    }
  }

  Future<void> _openPicker() async {
    final result = await showModalBottomSheet<List<String>>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _LookupPickerSheet(
        lookupName: widget.lookupName,
        initialSelected: widget.selectedIds,
        initialLabelCache: Map<String, LookupItem>.from(_labelCache),
        onLabelSeen: (item) => _labelCache[item.id] = item,
        title: widget.hint,
      ),
    );
    if (result != null) widget.onChanged(result);
  }

  @override
  Widget build(BuildContext context) {
    final ids = widget.selectedIds;
    return InkWell(
      onTap: _openPicker,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        constraints: const BoxConstraints(minHeight: 44),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: const Color(0xFFF4F5F2),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          children: [
            Expanded(
              child: ids.isEmpty
                  ? Text('点击选择（可搜索）',
                      style: const TextStyle(color: _DCTheme.dim, fontSize: 13))
                  : Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        for (final id in ids)
                          _SelectedChip(
                            label: _labelCache[id]?.label ?? id,
                            onRemove: () {
                              final next = List<String>.from(ids)..remove(id);
                              widget.onChanged(next);
                            },
                          ),
                      ],
                    ),
            ),
            const SizedBox(width: 6),
            if (ids.isNotEmpty) ...[
              Text('${ids.length} 项',
                  style: const TextStyle(color: _DCTheme.dim, fontSize: 11)),
              const SizedBox(width: 4),
            ],
            const Icon(Icons.keyboard_arrow_down_rounded,
                size: 20, color: _DCTheme.dim),
          ],
        ),
      ),
    );
  }
}

// bottom sheet 版全屏选择器 —— 顶部搜索栏 + 列表 + 底部"确定 (n)"
class _LookupPickerSheet extends StatefulWidget {
  final String lookupName;
  final List<String> initialSelected;
  final Map<String, LookupItem> initialLabelCache;
  final ValueChanged<LookupItem> onLabelSeen;
  final String title;
  const _LookupPickerSheet({
    required this.lookupName,
    required this.initialSelected,
    required this.initialLabelCache,
    required this.onLabelSeen,
    required this.title,
  });
  @override
  State<_LookupPickerSheet> createState() => _LookupPickerSheetState();
}

class _LookupPickerSheetState extends State<_LookupPickerSheet> {
  final _ctrl = TextEditingController();
  static const _pageSize = 30;
  Timer? _debounce;
  List<LookupItem> _results = [];
  late Set<String> _selected;
  late Map<String, LookupItem> _labelCache;
  bool _loading = false;
  bool _loadingMore = false;
  bool _hasMore = false;
  String _currentQ = '';
  int _nextOffset = 0;

  @override
  void initState() {
    super.initState();
    _selected = Set<String>.from(widget.initialSelected);
    _labelCache = Map<String, LookupItem>.from(widget.initialLabelCache);
    _doSearch('', reset: true);
  }

  @override
  void dispose() {
    _ctrl.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _scheduleSearch(String q) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 220), () => _doSearch(q, reset: true));
  }

  Future<void> _doSearch(String q, {required bool reset}) async {
    if (reset) {
      setState(() {
        _loading = true;
        _currentQ = q;
        _nextOffset = 0;
        _results = [];
        _hasMore = false;
      });
    }
    try {
      final page = await DataCenterService.searchLookup(
        widget.lookupName, q: q, limit: _pageSize, offset: 0);
      if (!mounted) return;
      setState(() {
        _results = page.items;
        _hasMore = page.hasMore;
        _nextOffset = page.items.length;
        _loading = false;
      });
      for (final it in page.items) {
        _labelCache[it.id] = it;
        widget.onLabelSeen(it);
      }
    } catch (_) {
      if (!mounted) return;
      setState(() { _loading = false; _results = []; _hasMore = false; });
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || !_hasMore) return;
    setState(() => _loadingMore = true);
    try {
      final page = await DataCenterService.searchLookup(
        widget.lookupName, q: _currentQ, limit: _pageSize, offset: _nextOffset);
      if (!mounted) return;
      setState(() {
        _results.addAll(page.items);
        _hasMore = page.hasMore;
        _nextOffset += page.items.length;
        _loadingMore = false;
      });
      for (final it in page.items) {
        _labelCache[it.id] = it;
        widget.onLabelSeen(it);
      }
    } catch (_) {
      if (!mounted) return;
      setState(() => _loadingMore = false);
    }
  }

  void _toggle(LookupItem it) {
    setState(() {
      if (_selected.contains(it.id)) {
        _selected.remove(it.id);
      } else {
        _selected.add(it.id);
      }
    });
  }

  void _addManualId(String raw) {
    final id = raw.trim();
    if (id.isEmpty) return;
    setState(() {
      _selected.add(id);
      _labelCache[id] ??= LookupItem(id: id, label: id);
      _ctrl.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return SafeArea(
      top: false,
      child: DraggableScrollableSheet(
        initialChildSize: 0.75,
        minChildSize: 0.5,
        maxChildSize: 0.95,
        expand: false,
        builder: (_, scroll) => Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
          ),
          child: Column(
            children: [
              // 拖拽把手
              Container(
                margin: const EdgeInsets.only(top: 6, bottom: 4),
                height: 4, width: 40,
                decoration: BoxDecoration(
                  color: _DCTheme.hairline,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              // 标题栏
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 6, 8, 6),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(widget.title,
                          style: const TextStyle(
                              fontSize: 15, fontWeight: FontWeight.w700)),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 20),
                      onPressed: () => Navigator.pop(context, null),
                    ),
                  ],
                ),
              ),
              // 搜索框
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Container(
                  decoration: BoxDecoration(
                    color: const Color(0xFFF4F5F2),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Row(
                    children: [
                      const Icon(Icons.search,
                          size: 18, color: _DCTheme.dim),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: _ctrl,
                          autofocus: false,
                          decoration: const InputDecoration(
                            hintText: '搜索名称/账号/手机，或输入 ID 后回车',
                            hintStyle:
                                TextStyle(color: _DCTheme.dim, fontSize: 13),
                            border: InputBorder.none,
                            isDense: true,
                            contentPadding:
                                EdgeInsets.symmetric(vertical: 12),
                          ),
                          style: const TextStyle(fontSize: 13),
                          onChanged: _scheduleSearch,
                          onSubmitted: _addManualId,
                        ),
                      ),
                      if (_loading)
                        const SizedBox(
                          width: 14, height: 14,
                          child:
                              CircularProgressIndicator(strokeWidth: 2),
                        ),
                    ],
                  ),
                ),
              ),
              // 已选 chip 条（滚动的）
              if (_selected.isNotEmpty)
                Container(
                  width: double.infinity,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                  color: const Color(0xFFF8FAFC),
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        for (final id in _selected)
                          Padding(
                            padding: const EdgeInsets.only(right: 6),
                            child: _SelectedChip(
                              label: _labelCache[id]?.label ?? id,
                              onRemove: () =>
                                  setState(() => _selected.remove(id)),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              const Divider(height: 1, color: _DCTheme.hairline),
              // 结果列表 —— 支持滚动到底自动加载下一页
              Expanded(
                child: _results.isEmpty && !_loading
                    ? const Center(
                        child: Padding(
                          padding: EdgeInsets.all(24),
                          child: Text('无结果 · 换个关键字试试',
                              style: TextStyle(
                                  color: _DCTheme.dim, fontSize: 13)),
                        ),
                      )
                    : NotificationListener<ScrollNotification>(
                        onNotification: (n) {
                          // 快到底就加载下一页
                          if (n.metrics.pixels > n.metrics.maxScrollExtent - 120) {
                            _loadMore();
                          }
                          return false;
                        },
                        child: ListView.separated(
                          controller: scroll, // 用 sheet 提供的，保证拖拽 sheet 顺畅
                          padding: EdgeInsets.zero,
                          itemCount: _results.length + 1, // +1 for footer
                          separatorBuilder: (_, __) => const Divider(
                              height: 1, color: _DCTheme.hairline),
                          itemBuilder: (_, i) {
                            if (i == _results.length) {
                              // footer: 加载状态 / 到底提示
                              return Padding(
                                padding: const EdgeInsets.symmetric(vertical: 14),
                                child: Center(
                                  child: _loadingMore
                                      ? const SizedBox(
                                          width: 16, height: 16,
                                          child: CircularProgressIndicator(strokeWidth: 2))
                                      : Text(
                                          _hasMore ? '上拉加载更多' : '— 已到底 · 共 ${_results.length} 条 —',
                                          style: const TextStyle(color: _DCTheme.dim, fontSize: 12)),
                                ),
                              );
                            }
                            final it = _results[i];
                            final picked = _selected.contains(it.id);
                            return InkWell(
                              onTap: () => _toggle(it),
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 16, vertical: 12),
                                child: Row(
                                  children: [
                                    Icon(
                                        picked
                                            ? Icons.check_box
                                            : Icons.check_box_outline_blank,
                                        size: 20,
                                        color: picked ? _DCTheme.accent : _DCTheme.dim),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(it.label,
                                              style: const TextStyle(
                                                  fontSize: 14,
                                                  fontWeight: FontWeight.w600),
                                              overflow: TextOverflow.ellipsis),
                                          if (it.subLabel.isNotEmpty)
                                            Text(it.subLabel,
                                                style: const TextStyle(
                                                    fontSize: 11.5, color: _DCTheme.dim),
                                                overflow: TextOverflow.ellipsis),
                                        ],
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Text('#${it.id}',
                                        style: const TextStyle(
                                            fontSize: 11, color: _DCTheme.dim)),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      ),
              ),
              // 底部确定
              Container(
                padding: EdgeInsets.fromLTRB(
                    16, 10, 16, 10 + bottomInset),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  border: Border(
                      top: BorderSide(color: _DCTheme.hairline)),
                ),
                child: Row(
                  children: [
                    TextButton(
                      onPressed: () => setState(() => _selected.clear()),
                      child: const Text('清空',
                          style: TextStyle(color: _DCTheme.dim)),
                    ),
                    const Spacer(),
                    FilledButton(
                      onPressed: () =>
                          Navigator.pop(context, _selected.toList()),
                      style: FilledButton.styleFrom(
                        backgroundColor: _DCTheme.ink,
                        padding: const EdgeInsets.symmetric(
                            horizontal: 24, vertical: 12),
                      ),
                      child: Text('确定 (${_selected.length})',
                          style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w700)),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SelectedChip extends StatelessWidget {
  final String label;
  final VoidCallback onRemove;
  const _SelectedChip({required this.label, required this.onRemove});
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.only(left: 10, right: 4, top: 4, bottom: 4),
      decoration: BoxDecoration(
        color: _DCTheme.accent.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Flexible(
            child: Text(label,
                style: const TextStyle(fontSize: 12, color: _DCTheme.accent, fontWeight: FontWeight.w600),
                overflow: TextOverflow.ellipsis),
          ),
          const SizedBox(width: 4),
          InkWell(
            onTap: onRemove,
            borderRadius: BorderRadius.circular(999),
            child: const Padding(
              padding: EdgeInsets.all(2),
              child: Icon(Icons.close, size: 14, color: _DCTheme.accent),
            ),
          ),
        ],
      ),
    );
  }
}

class _DateField extends StatelessWidget {
  final String? value;
  final String hint;
  final ValueChanged<String?> onChanged;
  const _DateField({required this.value, required this.hint, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(10),
      onTap: () async {
        final initial = value != null && value!.isNotEmpty
            ? DateTime.tryParse(value!) ?? DateTime.now()
            : DateTime.now();
        final picked = await showDatePicker(
          context: context,
          initialDate: initial,
          firstDate: DateTime(2020),
          lastDate: DateTime(2030),
        );
        if (picked != null) {
          onChanged(_fmtDate(picked));
        }
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: const Color(0xFFF6F6F3),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: _DCTheme.hairline),
        ),
        child: Row(
          children: [
            const Icon(Icons.calendar_today_outlined,
                size: 14, color: _DCTheme.dim),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                value ?? hint,
                style: TextStyle(
                    fontSize: 13,
                    color: value == null ? _DCTheme.faint : _DCTheme.ink),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TextField extends StatelessWidget {
  final String value;
  final String hint;
  final ValueChanged<String> onChanged;
  const _TextField(
      {required this.value, required this.hint, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      initialValue: value,
      onChanged: onChanged,
      style: const TextStyle(fontSize: 13, color: _DCTheme.ink),
      decoration: InputDecoration(
        isDense: true,
        hintText: hint,
        hintStyle: const TextStyle(color: _DCTheme.faint, fontSize: 13),
        filled: true,
        fillColor: const Color(0xFFF6F6F3),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: _DCTheme.hairline),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: _DCTheme.hairline),
        ),
      ),
    );
  }
}

// 日期快捷预设小 chip（今天/近7天/本月 等）
class _DatePresetChip extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  const _DatePresetChip({required this.label, required this.onTap});
  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
        decoration: BoxDecoration(
          color: const Color(0xFFEEF2FF),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(label, style: const TextStyle(fontSize: 11.5, color: _DCTheme.accent, fontWeight: FontWeight.w600)),
      ),
    );
  }
}

class _EnumChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _EnumChip(
      {required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? _DCTheme.ink : const Color(0xFFF6F6F3),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
              color: selected ? _DCTheme.ink : _DCTheme.hairline),
        ),
        child: Text(label,
            style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
                color: selected ? Colors.white : _DCTheme.ink)),
      ),
    );
  }
}

// ══════════════════════════════════════════════════
// Tab 2: 我的导出
// ══════════════════════════════════════════════════

class _MyJobsTab extends StatefulWidget {
  const _MyJobsTab();

  @override
  State<_MyJobsTab> createState() => _MyJobsTabState();
}

class _MyJobsTabState extends State<_MyJobsTab>
    with AutomaticKeepAliveClientMixin {
  Future<List<JobInfo>>? _future;
  Timer? _poller;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _future = DataCenterService.listMyJobs();
    _poller = Timer.periodic(const Duration(seconds: 3), (_) {
      if (!mounted) return;
      setState(() {
        _future = DataCenterService.listMyJobs();
      });
    });
  }

  @override
  void dispose() {
    _poller?.cancel();
    super.dispose();
  }

  Future<void> _refresh() async {
    setState(() {
      _future = DataCenterService.listMyJobs();
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return RefreshIndicator(
      onRefresh: _refresh,
      color: _DCTheme.ink,
      child: FutureBuilder<List<JobInfo>>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting && !snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final list = snap.data ?? const [];
          if (list.isEmpty) {
            return const _EmptyHint(text: '还没导出过，去「模板」tab 挑一个试试');
          }
          return ListView.builder(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
            itemCount: list.length,
            itemBuilder: (context, i) => _JobRow(job: list[i], showOwner: false),
          );
        },
      ),
    );
  }
}

// ══════════════════════════════════════════════════
// Tab 3: 公共大厅
// ══════════════════════════════════════════════════

class _HallTab extends StatefulWidget {
  const _HallTab();

  @override
  State<_HallTab> createState() => _HallTabState();
}

class _HallTabState extends State<_HallTab>
    with AutomaticKeepAliveClientMixin {
  Future<HallSnapshot>? _future;
  Timer? _poller;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _future = DataCenterService.getHall();
    _poller = Timer.periodic(const Duration(seconds: 3), (_) {
      if (!mounted) return;
      setState(() => _future = DataCenterService.getHall());
    });
  }

  @override
  void dispose() {
    _poller?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return FutureBuilder<HallSnapshot>(
      future: _future,
      builder: (context, snap) {
        if (snap.connectionState == ConnectionState.waiting && !snap.hasData) {
          return const Center(child: CircularProgressIndicator());
        }
        final s = snap.data;
        return ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _DCTheme.surface,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Row(
                children: [
                  _hallStat('并发上限', '${s?.workerCap ?? '-'}'),
                  Container(
                      width: 1, height: 40, color: _DCTheme.hairline),
                  _hallStat('排队中', '${s?.queueLen ?? '-'}'),
                  Container(
                      width: 1, height: 40, color: _DCTheme.hairline),
                  _hallStat('当前在跑',
                      '${s?.activeJobs.where((j) => j.state == 'running').length ?? '-'}'),
                ],
              ),
            ),
            const SizedBox(height: 16),
            const Text('实时任务',
                style: TextStyle(
                    fontSize: 13,
                    color: _DCTheme.dim,
                    fontWeight: FontWeight.w600)),
            const SizedBox(height: 8),
            if (s == null || s.activeJobs.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 30),
                child: Center(
                    child: Text('当前没人在导',
                        style:
                            TextStyle(color: _DCTheme.dim, fontSize: 13))),
              ),
            for (final j in s?.activeJobs ?? const <JobInfo>[])
              _JobRow(job: j, showOwner: true),
          ],
        );
      },
    );
  }

  Widget _hallStat(String label, String value) {
    return Expanded(
      child: Column(
        children: [
          Text(value,
              style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  color: _DCTheme.ink,
                  letterSpacing: -0.5)),
          const SizedBox(height: 3),
          Text(label,
              style: const TextStyle(fontSize: 11, color: _DCTheme.dim)),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════
// Tab 4: 预约 (TODO)
// ══════════════════════════════════════════════════

class _ScheduleTab extends StatelessWidget {
  const _ScheduleTab();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.schedule, color: _DCTheme.faint, size: 48),
            SizedBox(height: 16),
            Text('预约导出',
                style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: _DCTheme.ink)),
            SizedBox(height: 6),
            Text('每周/每月定时自动导出，即将上线',
                style: TextStyle(fontSize: 13, color: _DCTheme.dim)),
          ],
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════
// JobRow: 一个任务的一行卡片
// ══════════════════════════════════════════════════

class _JobRow extends StatefulWidget {
  final JobInfo job;
  final bool showOwner;
  const _JobRow({required this.job, required this.showOwner});

  @override
  State<_JobRow> createState() => _JobRowState();
}

class _JobRowState extends State<_JobRow> {
  double? _downloadProgress; // null = 未下载, 0-1 = 下载中, -1 = 已存
  String? _localPath;

  @override
  void initState() {
    super.initState();
    _checkCached();
  }

  Future<void> _checkCached() async {
    if (!widget.job.isDone) return;
    final name = _fileName(widget.job);
    final path = await FileDownloadService.cachedPath(name);
    if (mounted && path != null) {
      setState(() {
        _localPath = path;
        _downloadProgress = -1; // 已存
      });
    }
  }

  String _fileName(JobInfo job) {
    final label = job.displayName;
    final ts = job.createdAt;
    final stamp = '${ts.year}${ts.month.toString().padLeft(2, '0')}${ts.day.toString().padLeft(2, '0')}'
        '_${ts.hour.toString().padLeft(2, '0')}${ts.minute.toString().padLeft(2, '0')}';
    return '${label}_$stamp.xlsx';
  }

  Future<void> _download() async {
    final job = widget.job;
    setState(() => _downloadProgress = 0);
    try {
      final headers = await DataCenterService.downloadHeaders();
      final name = _fileName(job);
      final res = await FileDownloadService.downloadAndOpen(
        url: DataCenterService.xlsxUrl(job.id),
        filename: name,
        headers: headers,
        onProgress: (p) {
          if (mounted) setState(() => _downloadProgress = p);
        },
      );
      final path = await FileDownloadService.cachedPath(name);
      if (mounted) {
        setState(() {
          _localPath = path;
          _downloadProgress = -1;
        });
        if (res.message.isNotEmpty && res.message.toLowerCase() != 'done') {
          // OpenFilex 有些机型没装 xlsx viewer 会 fail, 提示下
          final tone = res.message.toLowerCase().contains('no') ||
                  res.message.toLowerCase().contains('error')
              ? _DCTheme.warn
              : null;
          if (tone != null) {
            ScaffoldMessenger.of(context).showSnackBar(SnackBar(
              content: Text('已下载到本地, 但没找到 xlsx 应用打开 (${res.message})'),
              backgroundColor: tone,
            ));
          }
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _downloadProgress = null);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text('下载失败: $e'),
          backgroundColor: _DCTheme.danger,
        ));
      }
    }
  }

  Future<void> _openLocal() async {
    if (_localPath == null) return;
    // 通过 downloadAndOpen 复用 open 逻辑 (已缓存不会重下)
    await _download();
  }

  @override
  Widget build(BuildContext context) {
    final job = widget.job;
    final stateColor = _stateColor(job.state);
    final stateLabel = _stateLabel(job);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: _DCTheme.surface,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(job.displayName,
                      style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: _DCTheme.ink)),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: stateColor.withValues(alpha: 0.14),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(stateLabel,
                      style: TextStyle(
                          fontSize: 11,
                          color: stateColor,
                          fontWeight: FontWeight.w700)),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Text(_fmtDateTime(job.createdAt),
                    style: const TextStyle(fontSize: 11, color: _DCTheme.dim)),
                const SizedBox(width: 10),
                if (job.rowsWritten > 0)
                  Text('${_kNum(job.rowsWritten)} 行',
                      style: const TextStyle(fontSize: 11, color: _DCTheme.dim)),
                if (widget.showOwner && job.ownerName.isNotEmpty) ...[
                  const SizedBox(width: 10),
                  Text('· ${job.ownerName}',
                      style: const TextStyle(fontSize: 11, color: _DCTheme.dim)),
                ],
              ],
            ),
            if (job.state == 'running') ...[
              const SizedBox(height: 10),
              const LinearProgressIndicator(
                minHeight: 3,
                backgroundColor: _DCTheme.hairline,
                color: _DCTheme.accent,
              ),
            ],
            if (job.errorMsg != null && job.errorMsg!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(job.errorMsg!,
                  style: const TextStyle(fontSize: 11, color: _DCTheme.danger)),
            ],
            if (job.isDone) ...[
              const SizedBox(height: 10),
              _buildDownloadRow(job),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDownloadRow(JobInfo job) {
    if (_downloadProgress != null && _downloadProgress! >= 0 && _downloadProgress! < 1) {
      // 下载中
      return Row(children: [
        Expanded(
          child: LinearProgressIndicator(
            value: _downloadProgress,
            minHeight: 3,
            backgroundColor: _DCTheme.hairline,
            color: _DCTheme.accent,
          ),
        ),
        const SizedBox(width: 10),
        Text('${(_downloadProgress! * 100).toStringAsFixed(0)}%',
            style: const TextStyle(fontSize: 11, color: _DCTheme.dim)),
      ]);
    }
    // 未下载 / 已下载
    final downloaded = _downloadProgress == -1 && _localPath != null;
    return Row(children: [
      if (job.fileSize != null)
        Text(_fmtSize(job.fileSize!),
            style: const TextStyle(fontSize: 11, color: _DCTheme.dim)),
      const Spacer(),
      if (downloaded) ...[
        const Icon(Icons.check_circle, size: 14, color: _DCTheme.success),
        const SizedBox(width: 4),
        Text('已保存',
            style: const TextStyle(fontSize: 11, color: _DCTheme.success)),
        const SizedBox(width: 8),
        TextButton.icon(
          onPressed: _openLocal,
          icon: const Icon(Icons.open_in_new, size: 16),
          label: const Text('打开'),
          style: TextButton.styleFrom(
              foregroundColor: _DCTheme.accent,
              padding: const EdgeInsets.symmetric(horizontal: 8)),
        ),
      ] else
        TextButton.icon(
          onPressed: _download,
          icon: const Icon(Icons.download, size: 16),
          label: const Text('下载并打开'),
          style: TextButton.styleFrom(
              foregroundColor: _DCTheme.accent,
              padding: const EdgeInsets.symmetric(horizontal: 8)),
        ),
    ]);
  }
}

// ══════════════════════════════════════════════════
// helpers
// ══════════════════════════════════════════════════

class _EmptyHint extends StatelessWidget {
  final String text;
  const _EmptyHint({required this.text});
  @override
  Widget build(BuildContext context) => ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: 80),
          Center(
              child: Text(text,
                  style:
                      const TextStyle(color: _DCTheme.dim, fontSize: 13))),
        ],
      );
}

class _ErrorHint extends StatelessWidget {
  final String msg;
  final VoidCallback onRetry;
  const _ErrorHint({required this.msg, required this.onRetry});
  @override
  Widget build(BuildContext context) => ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: 80),
          const Center(
              child: Icon(Icons.error_outline,
                  color: _DCTheme.danger, size: 32)),
          const SizedBox(height: 8),
          Center(
              child: Text(msg,
                  style: const TextStyle(color: _DCTheme.dim, fontSize: 12),
                  textAlign: TextAlign.center)),
          const SizedBox(height: 12),
          Center(
              child: TextButton(
                  onPressed: onRetry, child: const Text('重试'))),
        ],
      );
}

String _stateLabel(JobInfo j) {
  switch (j.state) {
    case 'pending':
      return '待处理';
    case 'queued':
      return j.queuePos != null ? '排队 (第${j.queuePos})' : '排队中';
    case 'running':
      return '导出中 ${_kNum(j.rowsWritten)}';
    case 'done':
      return '已完成';
    case 'failed':
      return '失败';
    case 'cancelled':
      return '已取消';
  }
  return j.state;
}

Color _stateColor(String state) {
  switch (state) {
    case 'done':
      return _DCTheme.success;
    case 'failed':
      return _DCTheme.danger;
    case 'running':
    case 'queued':
      return _DCTheme.accent;
    case 'cancelled':
      return _DCTheme.dim;
  }
  return _DCTheme.warn;
}

String _fmtDate(DateTime d) =>
    '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

String _fmtDateTime(DateTime d) {
  return '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')} '
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
}

String _kNum(int n) {
  if (n >= 10000) return '${(n / 10000).toStringAsFixed(1)}万';
  if (n >= 1000) return '${(n / 1000).toStringAsFixed(1)}k';
  return n.toString();
}

String _fmtSize(int bytes) {
  if (bytes >= 1024 * 1024) return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  if (bytes >= 1024) return '${(bytes / 1024).toStringAsFixed(0)} KB';
  return '$bytes B';
}

Color? _parseHex(String s) {
  if (s.isEmpty) return null;
  final hex = s.replaceFirst('#', '');
  final v = int.tryParse(hex, radix: 16);
  if (v == null) return null;
  if (hex.length == 6) return Color(0xFF000000 | v);
  return Color(v);
}

IconData _iconFor(String name) {
  const map = {
    'receipt_long': Icons.receipt_long,
    'storefront': Icons.storefront,
    'verified': Icons.verified,
    'account_balance_wallet': Icons.account_balance_wallet,
    'payments': Icons.payments,
    'schedule': Icons.schedule,
  };
  return map[name] ?? Icons.description_outlined;
}
