import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';

import '../services/api_service.dart';
import '../services/hotel_operations_service.dart';
import '../widgets/kyx_design.dart';

enum _HotelPeriod { week, month, quarter }

extension _HotelPeriodText on _HotelPeriod {
  String get label {
    switch (this) {
      case _HotelPeriod.week:
        return '本周';
      case _HotelPeriod.month:
        return '本月';
      case _HotelPeriod.quarter:
        return '本季度';
    }
  }

  DateTime get start {
    final now = DateTime.now();
    switch (this) {
      case _HotelPeriod.week:
        return DateTime(
          now.year,
          now.month,
          now.day,
        ).subtract(Duration(days: now.weekday - 1));
      case _HotelPeriod.month:
        return DateTime(now.year, now.month, 1);
      case _HotelPeriod.quarter:
        final month = ((now.month - 1) ~/ 3) * 3 + 1;
        return DateTime(now.year, month, 1);
    }
  }
}

class HotelManagerDashboardPage extends StatefulWidget {
  const HotelManagerDashboardPage({super.key});

  @override
  State<HotelManagerDashboardPage> createState() =>
      _HotelManagerDashboardPageState();
}

class _HotelManagerDashboardPageState extends State<HotelManagerDashboardPage> {
  late Future<HotelDashboardData> _future;
  _HotelPeriod _period = _HotelPeriod.week;

  @override
  void initState() {
    super.initState();
    _future = _loadDashboard();
  }

  Future<HotelDashboardData> _loadDashboard() async {
    final permission = await HotelOperationsService.getPermission();
    if (!permission.canViewDashboard) {
      throw ApiException('无酒店驾驶舱权限');
    }
    return HotelOperationsService.getDashboard();
  }

  Future<void> _refresh() async {
    setState(() => _future = _loadDashboard());
    await _future;
  }

  Future<void> _pickPeriod() async {
    final value = await showModalBottomSheet<_HotelPeriod>(
      context: context,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 8),
            for (final item in _HotelPeriod.values)
              ListTile(
                title: Text(item.label, style: KyXText.bodyStrong),
                trailing: item == _period
                    ? const Icon(Icons.check, color: KyXColors.primary)
                    : null,
                onTap: () => Navigator.pop(context, item),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (value != null) setState(() => _period = value);
  }

  Future<void> _exportReport(_DashboardView view) async {
    final dir = await getApplicationDocumentsDirectory();
    final file = File(
      '${dir.path}/酒店驾驶舱_${_period.label}_${DateTime.now().millisecondsSinceEpoch}.txt',
    );
    await file.writeAsString(view.reportText, encoding: utf8);
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text('报告已导出：${file.path}')));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '酒店驾驶舱',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          FutureBuilder<HotelDashboardData>(
            future: _future,
            builder: (context, snapshot) {
              final view = _DashboardView(
                snapshot.data ??
                    const HotelDashboardData(orders: [], talks: []),
                _period,
              );
              return TextButton(
                onPressed: () => _exportReport(view),
                child: const Text('导出'),
              );
            },
          ),
        ],
      ),
      body: FutureBuilder<HotelDashboardData>(
        future: _future,
        builder: (context, snapshot) {
          final view = _DashboardView(
            snapshot.data ?? const HotelDashboardData(orders: [], talks: []),
            _period,
          );
          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.only(bottom: 28),
              children: [
                _Header(period: _period, onPeriodTap: _pickPeriod),
                const KyXSectionLabel('汇总'),
                KyXListSection(
                  children: [
                    _SummaryRow(
                      label: '总工单',
                      value: '${view.total}',
                      sub: '待办 ${view.todo} · 紧急 ${view.urgent}',
                      color: KyXColors.slate,
                    ),
                    _SummaryRow(
                      label: '完成率',
                      value: '${view.completionRate}%',
                      sub: '已完成 ${view.done} / 总计 ${view.total}',
                      color: KyXColors.green,
                    ),
                    _SummaryRow(
                      label: '情绪风险',
                      value: '${view.risk}',
                      sub: '不满/生气客户',
                      color: view.risk > 0
                          ? KyXColors.red
                          : KyXColors.textSecondary,
                      showDivider: false,
                    ),
                  ],
                ),
                const KyXSectionLabel('趋势曲线'),
                KyXListSection(
                  child: _TrendPanel(
                    points: view.trend,
                    labels: view.trendLabels,
                  ),
                ),
                const KyXSectionLabel('占比'),
                KyXListSection(
                  children: [
                    _DonutRow(
                      title: '类型占比',
                      data: view.typeCounts,
                      colors: const [
                        KyXColors.primary,
                        KyXColors.green,
                        KyXColors.amber,
                        KyXColors.red,
                        KyXColors.indigo,
                      ],
                    ),
                    const Divider(
                      height: 1,
                      indent: 16,
                      color: KyXColors.lineSoft,
                    ),
                    _DonutRow(
                      title: '状态占比',
                      data: view.statusCounts,
                      colors: const [
                        KyXColors.amber,
                        KyXColors.primary,
                        KyXColors.green,
                      ],
                    ),
                  ],
                ),
                const KyXSectionLabel('门店完成率'),
                KyXListSection(
                  children: _withDividers(
                    view.storeRows
                        .map((row) => _StoreRateRow(row: row))
                        .toList(),
                  ),
                ),
                const KyXSectionLabel('分析报告'),
                KyXListSection(child: _ReportPanel(lines: view.reportLines)),
                const KyXSectionLabel('最近工单'),
                view.orders.isEmpty
                    ? const KyXListSection(child: _EmptyRow(text: '暂无工单'))
                    : KyXListSection(
                        children: _withDividers(
                          view.orders
                              .take(10)
                              .map((order) => _RecentOrderRow(order: order))
                              .toList(),
                        ),
                      ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _DashboardView {
  final HotelDashboardData raw;
  final _HotelPeriod period;
  late final List<HotelWorkOrder> orders = raw.orders
      .where((item) => !item.createTime.isBefore(period.start))
      .toList();

  _DashboardView(this.raw, this.period);

  int get total => orders.length;
  int get done =>
      orders.where((item) => item.status == HotelWorkOrderStatus.done).length;
  int get todo =>
      orders.where((item) => item.status != HotelWorkOrderStatus.done).length;
  int get urgent => orders.where((item) => item.priority == '紧急').length;
  int get risk => orders
      .where(
        (item) => item.customerEmotion == '生气' || item.customerEmotion == '不满',
      )
      .length;
  int get completionRate => total == 0 ? 0 : (done * 100 / total).round();

  Map<String, int> get typeCounts => _countBy(orders, (item) => item.type);
  Map<String, int> get statusCounts => {
    '待确认': orders
        .where((item) => item.status == HotelWorkOrderStatus.pending)
        .length,
    '已收到': orders
        .where((item) => item.status == HotelWorkOrderStatus.doing)
        .length,
    '已完成': done,
  };

  List<_StoreRate> get storeRows {
    final stores = raw.orders
        .map((item) => item.store)
        .where((item) => item.isNotEmpty)
        .toSet()
        .toList();
    final scopedStores = stores.isNotEmpty
        ? stores
        : HotelOperationsService.stores;
    return scopedStores.map((store) {
      final list = orders.where((item) => item.store == store).toList();
      final finished = list
          .where((item) => item.status == HotelWorkOrderStatus.done)
          .length;
      final riskCount = list
          .where(
            (item) =>
                item.priority == '紧急' ||
                item.customerEmotion == '生气' ||
                item.customerEmotion == '不满',
          )
          .length;
      return _StoreRate(store, list.length, finished, riskCount);
    }).toList();
  }

  List<int> get trend {
    final days = period == _HotelPeriod.week
        ? 7
        : period == _HotelPeriod.month
        ? 6
        : 6;
    final result = List<int>.filled(days, 0);
    final now = DateTime.now();
    for (final order in orders) {
      int index;
      if (period == _HotelPeriod.week) {
        index = order.createTime.weekday - 1;
      } else {
        final span = period == _HotelPeriod.month ? 5 : 15;
        index = math.min(
          days - 1,
          (order.createTime.difference(period.start).inDays / span).floor(),
        );
      }
      if (index >= 0 &&
          index < result.length &&
          !order.createTime.isAfter(now)) {
        result[index]++;
      }
    }
    return result;
  }

  List<String> get trendLabels {
    if (period == _HotelPeriod.week) {
      return const ['一', '二', '三', '四', '五', '六', '日'];
    }
    if (period == _HotelPeriod.month) {
      return const ['1-5', '6-10', '11-15', '16-20', '21-25', '26+'];
    }
    return const ['1-15', '16-30', '31-45', '46-60', '61-75', '76+'];
  }

  List<String> get reportLines {
    final topType = typeCounts.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    final topStore = storeRows.toList()
      ..sort((a, b) => b.total.compareTo(a.total));
    final lines = <String>[];
    lines.add('${period.label}共 $total 单，完成率 $completionRate%，待办 $todo 单。');
    if (topType.isNotEmpty) {
      lines.add('问题最多：${topType.first.key}，${topType.first.value} 单。');
    }
    if (topStore.isNotEmpty) {
      lines.add('门店最多：${topStore.first.store}，${topStore.first.total} 单。');
    }
    lines.add(risk > 0 ? '存在 $risk 条情绪/紧急风险，建议店长跟进复盘。' : '暂无明显情绪风险。');
    if (completionRate < 80 && total > 0) lines.add('完成率低于 80%，建议关注大姐确认与闭环时效。');
    return lines;
  }

  String get reportText => [
    '酒店驾驶舱报告（${period.label}）',
    '生成时间：${DateTime.now()}',
    '',
    ...reportLines,
    '',
    '类型占比：${typeCounts.entries.map((e) => '${e.key}${e.value}单').join('，')}',
    '状态占比：${statusCounts.entries.map((e) => '${e.key}${e.value}单').join('，')}',
  ].join('\n');

  static Map<String, int> _countBy(
    List<HotelWorkOrder> items,
    String Function(HotelWorkOrder) keyOf,
  ) {
    final map = <String, int>{};
    for (final item in items) {
      final key = keyOf(item);
      map[key] = (map[key] ?? 0) + 1;
    }
    return map;
  }
}

class _StoreRate {
  final String store;
  final int total;
  final int done;
  final int risk;
  const _StoreRate(this.store, this.total, this.done, this.risk);
  int get rate => total == 0 ? 0 : (done * 100 / total).round();
}

class _Header extends StatelessWidget {
  final _HotelPeriod period;
  final VoidCallback onPeriodTap;

  const _Header({required this.period, required this.onPeriodTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
      child: Row(
        children: [
          const Icon(
            Icons.analytics_outlined,
            color: KyXColors.primary,
            size: 24,
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('店长看板', style: KyXText.title),
                SizedBox(height: 4),
                Text('趋势、占比、完成率', style: KyXText.secondary),
              ],
            ),
          ),
          InkWell(
            onTap: onPeriodTap,
            borderRadius: BorderRadius.circular(8),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
              decoration: BoxDecoration(
                color: KyXColors.bg,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: KyXColors.line),
              ),
              child: Row(
                children: [
                  Text(
                    period.label,
                    style: KyXText.secondary.copyWith(
                      color: KyXColors.text,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(width: 4),
                  const Icon(
                    Icons.keyboard_arrow_down,
                    size: 18,
                    color: KyXColors.textSecondary,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label;
  final String value;
  final String sub;
  final Color color;
  final bool showDivider;

  const _SummaryRow({
    required this.label,
    required this.value,
    required this.sub,
    required this.color,
    this.showDivider = true,
  });

  @override
  Widget build(BuildContext context) {
    final row = Padding(
      padding: const EdgeInsets.fromLTRB(16, 11, 16, 11),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: KyXText.bodyStrong),
                const SizedBox(height: 3),
                Text(sub, style: KyXText.secondary),
              ],
            ),
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w900,
              color: color,
            ),
          ),
        ],
      ),
    );
    if (!showDivider) return row;
    return Column(
      children: [
        row,
        const Divider(height: 1, indent: 16, color: KyXColors.lineSoft),
      ],
    );
  }
}

class _TrendPanel extends StatelessWidget {
  final List<int> points;
  final List<String> labels;

  const _TrendPanel({required this.points, required this.labels});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            height: 150,
            child: CustomPaint(
              painter: _TrendPainter(points),
              child: const SizedBox.expand(),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: labels
                .map(
                  (label) => Expanded(
                    child: Text(
                      label,
                      textAlign: TextAlign.center,
                      style: KyXText.caption,
                    ),
                  ),
                )
                .toList(),
          ),
        ],
      ),
    );
  }
}

class _TrendPainter extends CustomPainter {
  final List<int> points;
  _TrendPainter(this.points);

  @override
  void paint(Canvas canvas, Size size) {
    final grid = Paint()
      ..color = KyXColors.lineSoft
      ..strokeWidth = 1;
    for (var i = 0; i < 4; i++) {
      final y = size.height * i / 3;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), grid);
    }
    if (points.isEmpty) return;
    final maxValue = math.max(1, points.reduce(math.max));
    final step = points.length == 1
        ? size.width
        : size.width / (points.length - 1);
    final path = Path();
    for (var i = 0; i < points.length; i++) {
      final x = step * i;
      final y = size.height - (points[i] / maxValue) * (size.height - 12) - 6;
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
      final dotPaint = Paint()..color = KyXColors.primary;
      canvas.drawCircle(Offset(x, y), 3.5, dotPaint);
    }
    final paint = Paint()
      ..color = KyXColors.primary
      ..strokeWidth = 2.2
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _TrendPainter oldDelegate) =>
      oldDelegate.points != points;
}

class _DonutRow extends StatelessWidget {
  final String title;
  final Map<String, int> data;
  final List<Color> colors;

  const _DonutRow({
    required this.title,
    required this.data,
    required this.colors,
  });

  @override
  Widget build(BuildContext context) {
    final entries = data.entries.where((item) => item.value > 0).toList();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 13, 16, 13),
      child: Row(
        children: [
          SizedBox(
            width: 86,
            height: 86,
            child: CustomPaint(
              painter: _DonutPainter(
                entries.map((e) => e.value).toList(),
                colors,
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: KyXText.bodyStrong),
                const SizedBox(height: 8),
                if (entries.isEmpty)
                  const Text('暂无数据', style: KyXText.secondary)
                else
                  ...entries.take(5).toList().asMap().entries.map((item) {
                    final entry = item.value;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 4),
                      child: Row(
                        children: [
                          Container(
                            width: 7,
                            height: 7,
                            decoration: BoxDecoration(
                              color: colors[item.key % colors.length],
                              borderRadius: BorderRadius.circular(99),
                            ),
                          ),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              entry.key,
                              style: KyXText.secondary,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Text('${entry.value}', style: KyXText.secondary),
                        ],
                      ),
                    );
                  }),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DonutPainter extends CustomPainter {
  final List<int> values;
  final List<Color> colors;
  _DonutPainter(this.values, this.colors);

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Offset.zero & size;
    final stroke = math.min(size.width, size.height) * 0.16;
    final bg = Paint()
      ..color = KyXColors.lineSoft
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke;
    canvas.drawArc(rect.deflate(stroke), -math.pi / 2, math.pi * 2, false, bg);
    final total = values.fold<int>(0, (a, b) => a + b);
    if (total <= 0) return;
    var start = -math.pi / 2;
    for (var i = 0; i < values.length; i++) {
      final sweep = math.pi * 2 * values[i] / total;
      final paint = Paint()
        ..color = colors[i % colors.length]
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeWidth = stroke;
      canvas.drawArc(rect.deflate(stroke), start, sweep, false, paint);
      start += sweep;
    }
  }

  @override
  bool shouldRepaint(covariant _DonutPainter oldDelegate) => true;
}

class _StoreRateRow extends StatelessWidget {
  final _StoreRate row;
  const _StoreRateRow({required this.row});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 11, 16, 11),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(row.store, style: KyXText.bodyStrong)),
              Text(
                '${row.rate}%',
                style: KyXText.bodyStrong.copyWith(
                  color: row.rate >= 80 ? KyXColors.green : KyXColors.amber,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(99),
            child: LinearProgressIndicator(
              value: row.rate / 100,
              minHeight: 6,
              backgroundColor: KyXColors.bg,
              color: row.rate >= 80 ? KyXColors.green : KyXColors.amber,
            ),
          ),
          const SizedBox(height: 5),
          Text(
            '总计 ${row.total} · 完成 ${row.done}${row.risk > 0 ? ' · 风险 ${row.risk}' : ''}',
            style: KyXText.caption,
          ),
        ],
      ),
    );
  }
}

class _ReportPanel extends StatelessWidget {
  final List<String> lines;
  const _ReportPanel({required this.lines});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: lines
            .map(
              (line) => Padding(
                padding: const EdgeInsets.only(bottom: 7),
                child: Text('• $line', style: KyXText.body),
              ),
            )
            .toList(),
      ),
    );
  }
}

class _RecentOrderRow extends StatelessWidget {
  final HotelWorkOrder order;
  const _RecentOrderRow({required this.order});

  @override
  Widget build(BuildContext context) {
    final isRisk = order.priority == '紧急' || order.customerEmotion == '生气';
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 11, 16, 11),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            order.status == HotelWorkOrderStatus.done
                ? Icons.check_circle_outline
                : isRisk
                ? Icons.error_outline
                : Icons.assignment_outlined,
            color: order.status == HotelWorkOrderStatus.done
                ? KyXColors.green
                : isRisk
                ? KyXColors.red
                : KyXColors.amber,
            size: 22,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  order.title,
                  style: KyXText.bodyStrong,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 3),
                Text(
                  '${order.store} · ${order.roomNo.isEmpty ? '未识别房号' : order.roomNo} · ${order.status.label}',
                  style: KyXText.secondary,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  '情绪 ${order.customerEmotion} · ${order.assigneeName}',
                  style: KyXText.caption,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyRow extends StatelessWidget {
  final String text;
  const _EmptyRow({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 18),
      child: Text(text, textAlign: TextAlign.center, style: KyXText.secondary),
    );
  }
}

List<Widget> _withDividers(List<Widget> rows) {
  final result = <Widget>[];
  for (var i = 0; i < rows.length; i++) {
    result.add(rows[i]);
    if (i != rows.length - 1) {
      result.add(
        const Divider(height: 1, indent: 16, color: KyXColors.lineSoft),
      );
    }
  }
  return result;
}
