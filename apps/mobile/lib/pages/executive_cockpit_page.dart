import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:open_filex/open_filex.dart';

import '../services/file_download_service.dart';

import '../services/agent_conversation_service.dart';
import '../services/agent_service.dart';
import '../services/executive_cockpit_service.dart';

class ExecutiveCockpitPage extends StatefulWidget {
  const ExecutiveCockpitPage({super.key});

  @override
  State<ExecutiveCockpitPage> createState() => _ExecutiveCockpitPageState();
}

class _ExecutiveCockpitPageState extends State<ExecutiveCockpitPage> {
  late Future<ExecutiveCockpitOverview> _future;
  ExecutiveCockpitOverview? _overview;
  int _rangeDays = 30;
  String _range = 'today'; // 大盘时间范围，抬到 AppBar 右上角选
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<ExecutiveCockpitOverview> _load() async {
    final overview = await ExecutiveCockpitService.getOverview(
      days: _rangeDays,
    );
    if (mounted) setState(() => _overview = overview);
    return overview;
  }

  Future<void> _refresh() async {
    setState(() {
      _future = _load();
    });
    await _future;
  }

  void _selectRange(String r) {
    if (r == _range) return;
    setState(() => _range = r);
  }

  void _changeRange(int days) {
    if (_rangeDays == days) return;
    setState(() {
      _rangeDays = days;
      _future = _load();
    });
  }

  @override
  Widget build(BuildContext context) {
    // "数据" 页 —— 只显示指标（对话已独立成底部 tab "对话"）
    // 无 top TabBar，无 + 新建/历史 按钮（都在对话 tab 里做）
    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: _CockpitTheme.canvas,
      appBar: AppBar(
        backgroundColor: _CockpitTheme.canvas,
        foregroundColor: _CockpitTheme.ink,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleSpacing: 20,
        automaticallyImplyLeading: false,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        title: const Text(
          '数据',
          style: TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.2,
            color: _CockpitTheme.ink,
          ),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(
              child: _RangeMenuButton(selected: _range, onSelected: _selectRange),
            ),
          ),
        ],
      ),
      body: _MetricsTab(
        future: _future,
        overview: _overview,
        rangeDays: _rangeDays,
        range: _range,
        onRefresh: _refresh,
        onRangeChanged: _changeRange,
      ),
    );
  }
}

class _PillTabs extends StatelessWidget {
  final int index;
  final List<String> labels;
  final ValueChanged<int> onChanged;

  const _PillTabs({
    required this.index,
    required this.labels,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final segmentWidth = width / labels.length;
        return Container(
          height: 40,
          decoration: BoxDecoration(
            color: _CockpitTheme.hairline.withValues(alpha: 0.55),
            borderRadius: BorderRadius.circular(999),
          ),
          child: Stack(
            children: [
              AnimatedPositioned(
                duration: const Duration(milliseconds: 260),
                curve: Curves.easeOutCubic,
                left: 4 + index * (segmentWidth - 8 / labels.length),
                top: 4,
                bottom: 4,
                width: segmentWidth - 8,
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(999),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.06),
                        blurRadius: 8,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                ),
              ),
              Row(
                children: List.generate(labels.length, (i) {
                  final selected = i == index;
                  return Expanded(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: () => onChanged(i),
                      child: Center(
                        child: AnimatedDefaultTextStyle(
                          duration: const Duration(milliseconds: 200),
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 0.4,
                            color: selected
                                ? _CockpitTheme.ink
                                : _CockpitTheme.dim,
                          ),
                          child: Text(labels[i]),
                        ),
                      ),
                    ),
                  );
                }),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _CockpitTheme {
  static const canvas = Color(0xFFF7F7F5);
  static const ink = Color(0xFF0E0E10);
  static const dim = Color(0xFF6E6E76);
  static const faint = Color(0xFFB0B0B6);
  static const hairline = Color(0xFFE8E8E4);
  static const accent = Color(0xFF1D4ED8);
  static const green = Color(0xFF14804A);
  static const red = Color(0xFFB42318);
  static const amber = Color(0xFFB25E09);
  static const surface = Colors.white;
}

// -------------------- METRICS TAB --------------------

// 大盘定义：id / 标签 / 后端 endpoint path / 主 KPI 字段 / 主 KPI 标签
class _DashKind {
  final String id;
  final String label;
  final String path;
  final List<_KpiSpec> kpis; // 头部 4 个 KPI（大盘专属）
  final String rankMetric;   // 部门排行按此字段排
  final String rankLabel;    // 排行榜标题
  // Phase C —— 部门排行按派生指标（率）排：找差生 / 找质量差的团队
  final _KpiSpec? rankKpi;   // null → 用 rankMetric 字段值排（默认）
  final bool rankAsc;         // true → 升序（差生优先，如 SLA 达标率最低的先出）
  final String? rankMinDenom; // 最低分母字段（如 'ops_cnt'），小样本刷榜过滤
  const _DashKind({
    required this.id,
    required this.label,
    required this.path,
    required this.kpis,
    required this.rankMetric,
    required this.rankLabel,
    this.rankKpi,
    this.rankAsc = false,
    this.rankMinDenom,
  });
}

enum _KpiKind { count, money, percent, ms }

typedef _KpiCompute = num? Function(Map<String, num> src);

class _KpiSpec {
  final String key;
  final String label;
  final _KpiKind kind;
  // 派生指标（比率/客单价等）：从 total 或 trend row 里现算。
  // 返回 null 表示当前上下文缺依赖字段，无法算 —— 调用方会跳过。
  final _KpiCompute? compute;
  const _KpiSpec(
    this.key,
    this.label, {
    this.kind = _KpiKind.count,
    this.compute,
  });

  bool get isDerived => compute != null;
}

// 派生指标闭包 —— 依赖字段缺失时返回 null，让调用方回退到"-"
num? _rate(Map<String, num> t, String numer, String denom) {
  final n = t[numer];
  final d = t[denom];
  if (n == null || d == null) return null;
  final dv = d.toDouble();
  if (dv <= 0) return 0;
  return n.toDouble() / dv * 100;
}

num? _avg(Map<String, num> t, String numer, String denom) {
  final n = t[numer];
  final d = t[denom];
  if (n == null || d == null) return null;
  final dv = d.toDouble();
  if (dv <= 0) return 0;
  return n.toDouble() / dv;
}

num? _badRate(Map<String, num> t) {
  final bad = t['bad_cnt'];
  final good = t['good_cnt'];
  if (bad == null || good == null) return null;
  final tot = bad.toDouble() + good.toDouble();
  if (tot <= 0) return 0;
  return bad.toDouble() / tot * 100;
}

// SLA 达标率 —— dept row 里有 sla_ok_cnt + ops_cnt
num? _slaRate(Map<String, num> t) => _rate(t, 'sla_ok_cnt', 'ops_cnt');

final List<_DashKind> _dashKinds = [
  const _DashKind(
    id: 'pf',
    label: '赔付',
    path: '/dashboard/pf-monthly',
    kpis: [], // 赔付走原 _PfSection，KPI 不用通用渲染
    rankMetric: 'my_money',
    rankLabel: '',
  ),
  // 营业额 —— 老板视角：本月金额 + 成单率 + 客单价 + 施工额
  _DashKind(
    id: 'order',
    label: '营业额',
    path: '/dashboard/order-monthly',
    kpis: [
      const _KpiSpec('total_money', '金额', kind: _KpiKind.money),
      _KpiSpec('conv_rate', '成单率',
          kind: _KpiKind.percent,
          compute: (t) => _rate(t, 'paid_cnt', 'order_cnt')),
      _KpiSpec('avg_price', '客单价',
          kind: _KpiKind.money,
          compute: (t) => _avg(t, 'total_money', 'order_cnt')),
      const _KpiSpec('install_money', '施工额', kind: _KpiKind.money),
    ],
    rankMetric: 'total_money',
    rankLabel: '部门营业额排行',
  ),
  // 订单量 —— 老板视角：本月新增订单数 + 状态分布 + 撤单率
  // 与"营业额" tab 区分：金额侧在 order-monthly（agg_order_daily 表），
  // 这里是"多少单、卡在哪一步"（直查 t_order，状态桶按 t_order.state 语义分箱）
  _DashKind(
    id: 'ordercnt',
    label: '订单',
    path: '/dashboard/order-count-monthly',
    kpis: [
      const _KpiSpec('order_cnt', '订单'),
      const _KpiSpec('pending_cnt', '未派单'),
      const _KpiSpec('finished_cnt', '已完结'),
      const _KpiSpec('revoke_rate', '撤单率', kind: _KpiKind.percent),
    ],
    rankMetric: 'order_cnt',
    rankLabel: '部门订单量排行',
  ),
  // 工单产能 —— 完成率优先，均处理换单位（秒/分）
  _DashKind(
    id: 'work',
    label: '工单产能',
    path: '/dashboard/work-monthly',
    kpis: [
      const _KpiSpec('work_cnt', '工单'),
      _KpiSpec('finish_rate', '完成率',
          kind: _KpiKind.percent,
          compute: (t) => _rate(t, 'finished_cnt', 'work_cnt')),
      const _KpiSpec('avg_dispose_ms', '均处理', kind: _KpiKind.ms),
      const _KpiSpec('hint_cnt', '标记'),
    ],
    rankMetric: 'work_cnt',
    rankLabel: '部门工单量排行',
  ),
  // 差评归因 —— 差评率放 Hero，绝对数副位；排行按差评率降序（找质量最差的团队）
  _DashKind(
    id: 'attr',
    label: '差评归因',
    path: '/dashboard/attribution-monthly',
    kpis: [
      _KpiSpec('bad_rate', '差评率', kind: _KpiKind.percent, compute: _badRate),
      const _KpiSpec('bad_cnt', '差评'),
      const _KpiSpec('reissue_cnt', '补发'),
      const _KpiSpec('timeout_cnt', '超时'),
    ],
    rankMetric: 'bad_cnt',
    rankLabel: '部门差评率排行',
    rankKpi: _KpiSpec('bad_rate', '差评率',
        kind: _KpiKind.percent, compute: _badRate),
    // rankAsc=false → 降序，差评率最高的部门排前面
    // 分母 = bad+good；小样本用 bad_cnt 近似过滤（good 通常没在 total 里没关系，逻辑里用两者之和）
    rankMinDenom: 'bad_cnt',
  ),
  // 时效达标 —— 后端已返回 sla_rate（0-100），排行按达标率升序（找差生）
  _DashKind(
    id: 'sla',
    label: '时效达标',
    path: '/dashboard/sla-monthly',
    kpis: [
      const _KpiSpec('sla_rate', '达标率', kind: _KpiKind.percent),
      const _KpiSpec('ops_cnt', '总操作'),
      const _KpiSpec('dispatch_cnt', '派单'),
      const _KpiSpec('callback_cnt', '回访'),
    ],
    rankMetric: 'ops_cnt',
    rankLabel: '部门达标率排行',
    rankKpi: _KpiSpec('sla_rate', '达标率',
        kind: _KpiKind.percent,
        compute: _slaRate),
    rankAsc: true, // 差生优先
    rankMinDenom: 'ops_cnt',
  ),
  // 补发 —— 通过率主位
  _DashKind(
    id: 'reissue',
    label: '补发',
    path: '/dashboard/reissue-monthly',
    kpis: [
      const _KpiSpec('total_cnt', '总补发'),
      _KpiSpec('approval_rate', '通过率',
          kind: _KpiKind.percent,
          compute: (t) => _rate(t, 'approved_cnt', 'total_cnt')),
      const _KpiSpec('product_cnt', '产品补发'),
      const _KpiSpec('install_cnt', '施工补发'),
    ],
    rankMetric: 'total_cnt',
    // agg_reissue_daily 表设计漏了 seller/dept 维度，先按门店排行。
    // 后端返回 dept_ranking 结构但填的是 shop_id / shop_name。
    rankLabel: '门店补发排行',
  ),
];

class _MetricsTab extends StatefulWidget {
  final Future<ExecutiveCockpitOverview> future;
  final ExecutiveCockpitOverview? overview;
  final int rangeDays;
  final String range; // 大盘时间范围，由父组件 AppBar 控制
  final Future<void> Function() onRefresh;
  final ValueChanged<int> onRangeChanged;

  const _MetricsTab({
    required this.future,
    required this.overview,
    required this.rangeDays,
    required this.range,
    required this.onRefresh,
    required this.onRangeChanged,
  });

  @override
  State<_MetricsTab> createState() => _MetricsTabState();
}

class _MetricsTabState extends State<_MetricsTab> {
  int _pageIndex = 0;
  String get _range => widget.range;
  Timer? _autoRefresh;
  late final PageController _pageCtrl = PageController();
  final Map<String, Future<DashboardOverview>> _dashFutures = {};
  final Map<String, Future<PfMonthlyOverview>> _pfFutures = {};

  @override
  void didUpdateWidget(covariant _MetricsTab old) {
    super.didUpdateWidget(old);
    if (old.range != widget.range) {
      // range 变了，清缓存让 FutureBuilder 重新拉
      _dashFutures.clear();
      _pfFutures.clear();
    }
  }

  @override
  void initState() {
    super.initState();
    // 每 30 秒刷一次当前大盘（实现"实时推送"翻页数字效果）
    _autoRefresh = Timer.periodic(const Duration(seconds: 30), (_) {
      if (!mounted) return;
      final kind = _dashKinds[_pageIndex];
      setState(() {
        if (kind.id == 'pf') {
          _pfFutures.remove('pf|$_range');
        } else {
          _dashFutures.remove('${kind.id}|$_range');
        }
      });
    });
  }

  @override
  void dispose() {
    _autoRefresh?.cancel();
    _pageCtrl.dispose();
    super.dispose();
  }

  Future<DashboardOverview> _load(_DashKind k) {
    final key = '${k.id}|$_range';
    return _dashFutures.putIfAbsent(
      key,
      () => ExecutiveCockpitService.getDashboard(k.path, range: _range),
    );
  }

  Future<PfMonthlyOverview> _loadPf() {
    final key = 'pf|$_range';
    return _pfFutures.putIfAbsent(
      key,
      () => ExecutiveCockpitService.getPfMonthly(range: _range),
    );
  }

  Future<void> _refresh() async {
    setState(() {
      _dashFutures.clear();
      _pfFutures.clear();
    });
    await widget.onRefresh();
  }

  void _selectKind(String id) {
    final i = _dashKinds.indexWhere((k) => k.id == id);
    if (i < 0) return;
    _pageCtrl.animateToPage(i,
        duration: const Duration(milliseconds: 260), curve: Curves.easeOutCubic);
  }

  Widget _pageFor(_DashKind kind) {
    if (kind.id == 'pf') {
      return FutureBuilder<PfMonthlyOverview>(
        future: _loadPf(),
        builder: (context, snap) {
          final data = snap.data;
          if (data == null && snap.connectionState == ConnectionState.waiting) {
            return const _LoadingState();
          }
          if (data == null) {
            return _ErrorState(
              message: snap.hasError ? _cleanError(snap.error) : '暂无赔付数据',
              onRetry: _refresh,
            );
          }
          return _PfSection(data: data);
        },
      );
    }
    return FutureBuilder<DashboardOverview>(
      future: _load(kind),
      builder: (context, snap) {
        final data = snap.data;
        if (data == null && snap.connectionState == ConnectionState.waiting) {
          return const _LoadingState();
        }
        if (data == null) {
          return _ErrorState(
            message: snap.hasError ? _cleanError(snap.error) : '暂无 ${kind.label} 数据',
            onRetry: _refresh,
          );
        }
        return _GenericDashSection(kind: kind, data: data);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final currentId = _dashKinds[_pageIndex].id;
    return Column(
      children: [
        const SizedBox(height: 6),
        // 大盘切换 —— 极简下划线（range 已抬到 AppBar 右上角）
        _DashSelector(
          kinds: _dashKinds,
          selected: currentId,
          onSelected: _selectKind,
        ),
        const SizedBox(height: 4),
        // 左右滑动 PageView + 单页面 RefreshIndicator
        Expanded(
          child: PageView.builder(
            controller: _pageCtrl,
            physics: const BouncingScrollPhysics(),
            onPageChanged: (i) => setState(() => _pageIndex = i),
            itemCount: _dashKinds.length,
            itemBuilder: (context, i) {
              final kind = _dashKinds[i];
              return RefreshIndicator(
                onRefresh: _refresh,
                color: _CockpitTheme.ink,
                child: ListView(
                  padding: const EdgeInsets.only(top: 6, bottom: 40),
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: [_pageFor(kind)],
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

// 时间范围筛选 —— 无边框 · 4 个 chip · 选中带极淡阴影
class _RangeItem {
  final String id;
  final String label;
  const _RangeItem(this.id, this.label);
}

const _rangeItems = <_RangeItem>[
  _RangeItem('today', '今日'),
  _RangeItem('7d', '7 天'),
  _RangeItem('30d', '30 天'),
  _RangeItem('90d', '90 天'),
  _RangeItem('365d', '一年'),
  _RangeItem('730d', '两年'),
  _RangeItem('1095d', '三年'),
  _RangeItem('all', '全部'),
];

// 无边框时间范围下拉 —— 塞在 _DashSelector 右上角，只占一行末尾几十像素
class _RangeMenuButton extends StatelessWidget {
  final String selected;
  final ValueChanged<String> onSelected;
  const _RangeMenuButton({required this.selected, required this.onSelected});

  @override
  Widget build(BuildContext context) {
    final label = _rangeItems.firstWhere(
      (it) => it.id == selected,
      orElse: () => _rangeItems[2],
    ).label;
    return PopupMenuButton<String>(
      tooltip: '切换时间范围',
      position: PopupMenuPosition.under,
      elevation: 4,
      offset: const Offset(0, 6),
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      onSelected: onSelected,
      itemBuilder: (context) => [
        for (final it in _rangeItems)
          PopupMenuItem<String>(
            value: it.id,
            height: 36,
            child: Row(
              children: [
                Text(
                  it.label,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: it.id == selected ? FontWeight.w700 : FontWeight.w500,
                    color: it.id == selected
                        ? const Color(0xFF0A84FF)
                        : const Color(0xFF111827),
                  ),
                ),
                if (it.id == selected) ...[
                  const Spacer(),
                  const Icon(Icons.check, size: 14, color: Color(0xFF0A84FF)),
                ],
              ],
            ),
          ),
      ],
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: Color(0xFF6B7280),
                letterSpacing: 0.2,
              ),
            ),
            const SizedBox(width: 2),
            const Icon(
              Icons.expand_more,
              size: 16,
              color: Color(0xFF9CA3AF),
            ),
          ],
        ),
      ),
    );
  }
}

// 数字翻页动画 —— 老虎机式（每位数字独立滚动）
class _FlipNumber extends StatelessWidget {
  final String value;
  final TextStyle style;
  const _FlipNumber({required this.value, required this.style});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (int i = 0; i < value.length; i++)
          _FlipDigit(char: value[i], style: style),
      ],
    );
  }
}

class _FlipDigit extends StatelessWidget {
  final String char;
  final TextStyle style;
  const _FlipDigit({required this.char, required this.style});

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 380),
      switchInCurve: Curves.easeOutCubic,
      switchOutCurve: Curves.easeInCubic,
      transitionBuilder: (child, anim) {
        final slide = Tween<Offset>(
          begin: const Offset(0, 0.4),
          end: Offset.zero,
        ).animate(anim);
        return ClipRect(
          child: SlideTransition(
            position: slide,
            child: FadeTransition(opacity: anim, child: child),
          ),
        );
      },
      layoutBuilder: (curr, prev) => Stack(alignment: Alignment.centerLeft, children: [
        ...prev,
        if (curr != null) curr,
      ]),
      child: Text(char, key: ValueKey(char), style: style),
    );
  }
}

// 极简大盘选择器 —— iOS/Notion 风格，无 box 无 pill
// 选中项：文字加粗 + 下方 2px 蓝色下划线
// 未选中：灰色文字
class _DashSelector extends StatefulWidget {
  final List<_DashKind> kinds;
  final String selected;
  final ValueChanged<String> onSelected;
  const _DashSelector({
    required this.kinds,
    required this.selected,
    required this.onSelected,
  });

  @override
  State<_DashSelector> createState() => _DashSelectorState();
}

class _DashSelectorState extends State<_DashSelector> {
  final Map<String, GlobalKey> _itemKeys = {};

  GlobalKey _keyFor(String id) => _itemKeys.putIfAbsent(id, () => GlobalKey());

  @override
  void didUpdateWidget(covariant _DashSelector oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.selected != widget.selected) {
      // 帧回调里滚，等 layout 完成拿到真实位置
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        final ctx = _keyFor(widget.selected).currentContext;
        if (ctx == null) return;
        Scrollable.ensureVisible(
          ctx,
          alignment: 0.5, // 尽量把选中 tab 摆在可视区域中间
          duration: const Duration(milliseconds: 260),
          curve: Curves.easeOutCubic,
        );
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        physics: const BouncingScrollPhysics(),
        child: Row(
          children: [
            for (final k in widget.kinds) ...[
              _DashSelectorItem(
                key: _keyFor(k.id),
                label: k.label,
                selected: widget.selected == k.id,
                onTap: () => widget.onSelected(k.id),
              ),
              const SizedBox(width: 20),
            ],
          ],
        ),
      ),
    );
  }
}

class _DashSelectorItem extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _DashSelectorItem({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(4),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: selected ? const Color(0xFF0A84FF) : Colors.transparent,
              width: 2,
            ),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 15,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            color: selected ? const Color(0xFF111827) : const Color(0xFF9CA3AF),
            letterSpacing: 0.2,
          ),
        ),
      ),
    );
  }
}

// 通用大盘 —— 完全复用 _PfSection 的 calm 视觉规格：hero 大数字 + 3 分栏 +
// 部门排行（细横条）+ 饼图分布 + 日趋势折线
class _GenericDashSection extends StatelessWidget {
  final _DashKind kind;
  final DashboardOverview data;
  const _GenericDashSection({required this.kind, required this.data});

  // 同 _PfSection 的调色板
  static const _accent = Color(0xFF0A84FF);
  static const _tint = Color(0xFFE8ECF3);
  static const _ink = Color(0xFF111114);
  static const _dim = Color(0xFF8A8A8E);
  static const _grid = Color(0xFFECECE8);

  // ---- helpers ----
  String _fmtNum(num? n) {
    if (n == null) return '-';
    final v = n.toDouble();
    if (v >= 1e6) return '${(v / 1e6).toStringAsFixed(2)}M';
    if (v >= 1e4) return '${(v / 1e4).toStringAsFixed(1)}万';
    if (v >= 1e3) return '${(v / 1e3).toStringAsFixed(1)}K';
    return v.toStringAsFixed(v.roundToDouble() == v ? 0 : 2);
  }

  String _fmtMoney(num? n) {
    if (n == null) return '¥-';
    final v = n.toDouble();
    if (v >= 1e8) return '¥${(v / 1e8).toStringAsFixed(2)}亿';
    if (v >= 1e6) return '¥${(v / 1e4).toStringAsFixed(1)}万';
    if (v >= 1e4) return '¥${(v / 1e4).toStringAsFixed(2)}万';
    return '¥${v.toStringAsFixed(0)}';
  }

  // 均处理毫秒 → 老板一眼能看懂的单位
  String _fmtMs(num? ms) {
    if (ms == null) return '-';
    final s = ms.toDouble() / 1000;
    if (s < 60) return '${s.toStringAsFixed(1)}秒';
    final m = s / 60;
    if (m < 60) return '${m.toStringAsFixed(1)}分';
    final h = m / 60;
    return '${h.toStringAsFixed(1)}时';
  }

  String _fmtPercent(num? v) {
    if (v == null) return '-';
    return '${v.toDouble().toStringAsFixed(1)}%';
  }

  String _fmtDisplay(_KpiSpec k, num? v) {
    switch (k.kind) {
      case _KpiKind.money:
        return _fmtMoney(v);
      case _KpiKind.percent:
        return _fmtPercent(v);
      case _KpiKind.ms:
        return _fmtMs(v);
      case _KpiKind.count:
        return _fmtNum(v);
    }
  }

  // 从 total / trend row 里取值：派生指标现算，普通指标读 map。
  num? _valueFor(_KpiSpec k, Map<String, num> src) {
    if (k.compute != null) return k.compute!(src);
    return src[k.key];
  }

  Widget _label(String text) => Text(
        text,
        style: const TextStyle(
          fontSize: 20, fontWeight: FontWeight.w700, color: _ink, letterSpacing: -0.2,
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 30),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _label('${kind.label} · ${data.month}'),
          if (data.lastRefresh != null)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                '更新于 ${_formatMinute(data.lastRefresh!)}',
                style: const TextStyle(fontSize: 11, color: _dim),
              ),
            ),
          const SizedBox(height: 28),
          _hero(),
          const SizedBox(height: 40),
          if (data.deptRanking.isNotEmpty) ...[
            _label('部门排行'),
            const SizedBox(height: 4),
            Text(
              '${kind.rankLabel.replaceAll("部门", "").replaceAll("排行", "").trim()} · ${kind.rankAsc ? "差生前" : "前"} ${data.deptRanking.take(6).length} 名',
              style: const TextStyle(fontSize: 11, color: _dim, letterSpacing: 0.2),
            ),
            const SizedBox(height: 18),
            _deptRanks(),
            const SizedBox(height: 40),
          ],
          // 饼图已删 —— KPI 之间量纲不一致（金额+计数+百分比），饼图本身没意义
          if (_trendKpi() != null) ...[
            _label('${_trendKpi()!.label} 日趋势'),
            const SizedBox(height: 18),
            _trendPart(),
          ],
        ],
      ),
    );
  }

  // Hero：主 KPI 大数字 + 副标签 + 3 分栏
  Widget _hero() {
    if (kind.kpis.isEmpty) return const SizedBox.shrink();
    final main = kind.kpis.first;
    final rest = kind.kpis.length > 1 ? kind.kpis.sublist(1) : <_KpiSpec>[];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.baseline,
          textBaseline: TextBaseline.alphabetic,
          children: [
            Flexible(
              child: _FlipNumber(
                value: _fmtDisplay(main, _valueFor(main, data.total)),
                style: const TextStyle(
                  fontSize: 42,
                  height: 1.05,
                  fontWeight: FontWeight.w800,
                  color: _ink,
                  letterSpacing: -1.6,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Text(
                main.label,
                style: const TextStyle(fontSize: 14, color: _dim),
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),
        if (rest.isNotEmpty)
          Row(
            children: [
              for (int i = 0; i < rest.length && i < 3; i++) ...[
                if (i > 0) Container(width: 0.5, height: 34, color: _grid),
                Expanded(child: _split(rest[i], _valueFor(rest[i], data.total))),
              ],
            ],
          ),
      ],
    );
  }

  Widget _split(_KpiSpec k, num? v) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(k.label, style: const TextStyle(fontSize: 11, color: _dim, letterSpacing: 0.2)),
          const SizedBox(height: 6),
          Text(_fmtDisplay(k, v),
              style: const TextStyle(
                  fontSize: 17, fontWeight: FontWeight.w700, color: _ink, letterSpacing: -0.4)),
        ],
      );

  // 部门排行（横细蓝条）
  // 默认按 rankMetric 字段值排；如 rankKpi 有派生（如 SLA 达标率），先按分母过滤小样本再按率排。
  Widget _deptRanks() {
    final source = data.deptRanking;
    if (source.isEmpty) return const SizedBox.shrink();

    // ── 1. 挑排行 KPI：优先派生（率），否则退化到 rankMetric
    final rankKpi = kind.rankKpi ?? kind.kpis.firstWhere(
      (k) => k.key == kind.rankMetric,
      orElse: () => _KpiSpec(kind.rankMetric, ''),
    );

    // ── 2. 小样本过滤：分母 < max(5, total_denom * 2%) 的部门刷不上榜
    final scored = <MapEntry<DashDeptRow, num>>[];
    if (kind.rankKpi != null && kind.rankMinDenom != null) {
      final totalDenom = (data.total[kind.rankMinDenom!] ?? 0).toDouble();
      final threshold = math.max<double>(5, totalDenom * 0.02);
      for (final r in source) {
        final denom = (r.metrics[kind.rankMinDenom!] ?? 0).toDouble();
        if (denom < threshold) continue;
        final v = _valueFor(rankKpi, r.metrics);
        if (v == null) continue;
        scored.add(MapEntry(r, v));
      }
    } else {
      for (final r in source) {
        scored.add(MapEntry(r, (r.metrics[kind.rankMetric] ?? 0)));
      }
    }
    if (scored.isEmpty) return const SizedBox.shrink();

    // ── 3. 排序：默认降序（大的靠前），rankAsc=true 时升序（差生靠前）
    scored.sort((a, b) => kind.rankAsc
        ? a.value.compareTo(b.value)
        : b.value.compareTo(a.value));

    final list = scored.take(6).toList();
    final maxVal = list
        .map((e) => e.value.toDouble().abs())
        .reduce((a, b) => a > b ? a : b);
    return Column(
      children: [
        for (final entry in list) ...[
          _DeptRankRow(
            name: entry.key.deptName,
            value: entry.value.toDouble(),
            maxValue: maxVal <= 0 ? 1 : maxVal,
            display: _fmtDisplay(rankKpi, entry.value),
            accent: _accent,
            tint: _tint,
          ),
          const SizedBox(height: 14),
        ],
      ],
    );
  }

  // 决定折线画哪个 KPI —— 单条线，就是 Hero KPI 的时间序列。
  // 混轴（金额+计数同图）没有信息价值，只制造视觉噪声，所以只画一条。
  // Hero KPI 在 trend 里算不出（如差评率缺 good_cnt）就退到下一个能算的 KPI。
  _KpiSpec? _trendKpi() {
    if (data.trend.isEmpty) return null;
    final firstRow = data.trend.first.values;
    for (final k in kind.kpis) {
      if (k.compute != null) {
        if (k.compute!(firstRow) != null) return k;
      } else if (firstRow.containsKey(k.key)) {
        return k;
      }
    }
    return null;
  }

  // Y 轴 label 紧凑格式（避免 34px 塞不下）
  String _fmtAxis(_KpiSpec k, num v) {
    switch (k.kind) {
      case _KpiKind.percent:
        return '${v.toStringAsFixed(0)}%';
      case _KpiKind.money:
        final d = v.toDouble();
        if (d >= 1e4) return '${(d / 1e4).toStringAsFixed(1)}万';
        if (d >= 1e3) return '${(d / 1e3).toStringAsFixed(1)}K';
        return d.toStringAsFixed(0);
      case _KpiKind.ms:
        final s = v.toDouble() / 1000;
        if (s < 60) return '${s.toStringAsFixed(0)}s';
        return '${(s / 60).toStringAsFixed(0)}m';
      case _KpiKind.count:
        return _fmtNum(v);
    }
  }

  // 日趋势 —— 单条 Hero KPI 折线
  Widget _trendPart() {
    final trend = data.trend;
    final k = _trendKpi();
    if (trend.isEmpty || k == null) return const SizedBox.shrink();
    double maxY = 1;
    for (final row in trend) {
      final v = _valueFor(k, row.values)?.toDouble() ?? 0;
      if (v > maxY) maxY = v;
    }
    // 百分比走 0-100 固定区间；其余按数据扩 20% 头部空间
    final capped = k.kind == _KpiKind.percent ? 100.0 : maxY * 1.2;
    return SizedBox(
      height: 160,
      child: LineChart(LineChartData(
        minY: 0,
        maxY: capped,
        gridData: FlGridData(
          show: true, drawVerticalLine: false,
          getDrawingHorizontalLine: (v) => FlLine(color: _grid, strokeWidth: 0.5, dashArray: [3, 3]),
        ),
        borderData: FlBorderData(show: false),
        titlesData: FlTitlesData(
          topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          leftTitles: AxisTitles(sideTitles: SideTitles(
            showTitles: true, reservedSize: 40,
            getTitlesWidget: (v, _) => Text(_fmtAxis(k, v),
              style: const TextStyle(fontSize: 10, color: _dim)),
          )),
          bottomTitles: AxisTitles(sideTitles: SideTitles(
            showTitles: true, reservedSize: 22,
            interval: (trend.length / 4).ceil().toDouble(),
            getTitlesWidget: (v, _) {
              final i = v.toInt();
              if (i < 0 || i >= trend.length) return const SizedBox.shrink();
              final ymd = trend[i].ymd;
              return Text(ymd.length >= 5 ? ymd.substring(5).replaceAll('-', '/') : ymd,
                style: const TextStyle(fontSize: 10, color: _dim));
            },
          )),
        ),
        lineBarsData: [
          LineChartBarData(
            spots: [
              for (int i = 0; i < trend.length; i++)
                FlSpot(i.toDouble(),
                    _valueFor(k, trend[i].values)?.toDouble() ?? 0),
            ],
            isCurved: true,
            curveSmoothness: 0.35,
            barWidth: 2,
            color: _accent,
            dotData: const FlDotData(show: false),
            belowBarData: BarAreaData(
              show: true,
              color: _accent.withValues(alpha: 0.08),
            ),
          ),
        ],
      )),
    );
  }
}

// 部门排行行 —— 与 _PfSection 视觉一致
class _DeptRankRow extends StatelessWidget {
  final String name;
  final double value;
  final double maxValue;
  final String display;
  final Color accent;
  final Color tint;
  const _DeptRankRow({
    required this.name,
    required this.value,
    required this.maxValue,
    required this.display,
    required this.accent,
    required this.tint,
  });
  @override
  Widget build(BuildContext context) {
    final ratio = (value / maxValue).clamp(0.02, 1.0).toDouble();
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        SizedBox(
          width: 60,
          child: Text(name,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 13, color: Color(0xFF3F4046))),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: LayoutBuilder(
            builder: (context, cs) => Stack(children: [
              Container(height: 6, decoration: BoxDecoration(color: tint, borderRadius: BorderRadius.circular(3))),
              Container(
                height: 6, width: cs.maxWidth * ratio,
                decoration: BoxDecoration(color: accent, borderRadius: BorderRadius.circular(3)),
              ),
            ]),
          ),
        ),
        const SizedBox(width: 12),
        // 自适应右列：金额可长可短，用 IntrinsicWidth + FittedBox 避免换行
        ConstrainedBox(
          constraints: const BoxConstraints(minWidth: 66, maxWidth: 110),
          child: FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerRight,
            child: Text(display,
                maxLines: 1,
                textAlign: TextAlign.right,
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: Color(0xFF111114))),
          ),
        ),
      ],
    );
  }
}

class _MiniKpi extends StatelessWidget {
  final String label;
  final String value;
  const _MiniKpi({required this.label, required this.value});
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(fontSize: 11, color: Colors.black45)),
          const SizedBox(height: 4),
          Text(value,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }
}

class _RankBar extends StatelessWidget {
  final String label;
  final double value;
  final double maxValue;
  final String display;
  const _RankBar({required this.label, required this.value, required this.maxValue, required this.display});
  @override
  Widget build(BuildContext context) {
    final ratio = maxValue > 0 ? (value / maxValue).clamp(0.0, 1.0) : 0.0;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(label, style: const TextStyle(fontSize: 13))),
              Text(display,
                  style: const TextStyle(fontSize: 12, color: Colors.black54)),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: BorderRadius.circular(3),
            child: LinearProgressIndicator(
              value: ratio,
              minHeight: 6,
              backgroundColor: const Color(0xFFE8ECF3),
              color: const Color(0xFF0A84FF),
            ),
          ),
        ],
      ),
    );
  }
}

class _HeroSection extends StatelessWidget {
  final ExecutiveCockpitOverview overview;
  final int rangeDays;
  final ValueChanged<int> onRangeChanged;

  const _HeroSection({
    required this.overview,
    required this.rangeDays,
    required this.onRangeChanged,
  });

  @override
  Widget build(BuildContext context) {
    final open = overview.metric('requirement_open')?.numericValue ?? 0;
    final overdue = overview.metric('requirement_overdue')?.numericValue ?? 0;
    final generatedAt = overview.generatedAt == null
        ? '刚刚'
        : _formatMinute(overview.generatedAt!);

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '近 $rangeDays 天 · 更新于 $generatedAt',
            style: const TextStyle(
              fontSize: 12,
              color: _CockpitTheme.dim,
              letterSpacing: 0.3,
            ),
          ),
          const SizedBox(height: 14),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                '$open',
                style: const TextStyle(
                  fontSize: 56,
                  height: 1,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -1.5,
                  color: _CockpitTheme.ink,
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                '件在办',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: _CockpitTheme.dim,
                ),
              ),
              const Spacer(),
              _OverdueChip(count: overdue),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              for (final days in const [7, 30, 90])
                Padding(
                  padding: EdgeInsets.only(right: days == 90 ? 0 : 8),
                  child: _RangePill(
                    label: '$days 天',
                    selected: rangeDays == days,
                    onTap: () => onRangeChanged(days),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _OverdueChip extends StatelessWidget {
  final int count;

  const _OverdueChip({required this.count});

  @override
  Widget build(BuildContext context) {
    if (count <= 0) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: const [
          Icon(
            Icons.check_circle_outline,
            size: 15,
            color: _CockpitTheme.green,
          ),
          SizedBox(width: 4),
          Text(
            '无逾期',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _CockpitTheme.green,
            ),
          ),
        ],
      );
    }
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 6,
          height: 6,
          decoration: const BoxDecoration(
            color: _CockpitTheme.red,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 6),
        Text(
          '$count 件逾期',
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: _CockpitTheme.red,
          ),
        ),
      ],
    );
  }
}

class _RangePill extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _RangePill({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
        decoration: BoxDecoration(
          color: selected ? _CockpitTheme.ink : Colors.transparent,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: selected ? _CockpitTheme.ink : _CockpitTheme.hairline,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.2,
            color: selected ? Colors.white : _CockpitTheme.dim,
          ),
        ),
      ),
    );
  }
}

class _StatStrip extends StatelessWidget {
  final List<ExecutiveCockpitMetric> metrics;

  const _StatStrip({required this.metrics});

  @override
  Widget build(BuildContext context) {
    final display = metrics
        .where((m) => m.code != 'requirement_open')
        .take(4)
        .toList();
    if (display.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        children: [
          Row(
            children: [
              for (var i = 0; i < math.min(2, display.length); i++) ...[
                if (i > 0)
                  Container(
                    width: 1,
                    height: 44,
                    color: _CockpitTheme.hairline,
                    margin: const EdgeInsets.symmetric(horizontal: 8),
                  ),
                Expanded(child: _StatCell(metric: display[i])),
              ],
            ],
          ),
          if (display.length > 2) ...[
            Container(
              height: 1,
              margin: const EdgeInsets.symmetric(vertical: 18),
              color: _CockpitTheme.hairline,
            ),
            Row(
              children: [
                for (var i = 2; i < math.min(4, display.length); i++) ...[
                  if (i > 2)
                    Container(
                      width: 1,
                      height: 44,
                      color: _CockpitTheme.hairline,
                      margin: const EdgeInsets.symmetric(horizontal: 8),
                    ),
                  Expanded(child: _StatCell(metric: display[i])),
                ],
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _StatCell extends StatelessWidget {
  final ExecutiveCockpitMetric metric;

  const _StatCell({required this.metric});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          metric.title,
          style: const TextStyle(
            fontSize: 12,
            color: _CockpitTheme.dim,
            letterSpacing: 0.2,
          ),
        ),
        const SizedBox(height: 6),
        Row(
          crossAxisAlignment: CrossAxisAlignment.baseline,
          textBaseline: TextBaseline.alphabetic,
          children: [
            Flexible(
              child: Text(
                metric.value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 26,
                  height: 1.1,
                  fontWeight: FontWeight.w800,
                  color: _CockpitTheme.ink,
                  letterSpacing: -0.6,
                ),
              ),
            ),
            if (metric.unit.isNotEmpty) ...[
              const SizedBox(width: 4),
              Text(
                metric.unit,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: _CockpitTheme.dim,
                ),
              ),
            ],
          ],
        ),
        if (metric.trendLabel.isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(
            metric.trendLabel,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 11,
              color: _toneColor(metric.tone),
              fontWeight: FontWeight.w600,
              letterSpacing: 0.2,
            ),
          ),
        ],
      ],
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String label;

  const _SectionHeader({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 0),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          letterSpacing: 1.6,
          color: _CockpitTheme.dim,
        ),
      ),
    );
  }
}

class _TrendSection extends StatelessWidget {
  final List<ExecutiveCockpitTrendPoint> points;

  const _TrendSection({required this.points});

  @override
  Widget build(BuildContext context) {
    if (points.isEmpty) {
      return const _InlineEmpty(text: '暂无趋势数据');
    }
    final totalCreated = points.fold<int>(
      0,
      (sum, item) => sum + item.createdCount,
    );
    final totalFinished = points.fold<int>(
      0,
      (sum, item) => sum + item.finishedCount,
    );

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: Row(
              children: [
                _LegendDot(color: _CockpitTheme.ink, label: '新增 $totalCreated'),
                const SizedBox(width: 16),
                _LegendDot(
                  color: _CockpitTheme.green,
                  label: '完成 $totalFinished',
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 168,
            child: CustomPaint(
              painter: _TrendPainter(points),
              child: const SizedBox.expand(),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusSection extends StatelessWidget {
  final List<ExecutiveCockpitStatusCount> items;

  const _StatusSection({required this.items});

  @override
  Widget build(BuildContext context) {
    final visible = items.where((item) => item.count > 0).toList();
    final total = visible.fold<int>(0, (sum, item) => sum + item.count);
    if (total <= 0) {
      return const _InlineEmpty(text: '暂无状态数据');
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: SizedBox(
              height: 8,
              child: Row(
                children: [
                  for (final item in visible)
                    Expanded(
                      flex: item.count,
                      child: Container(color: _toneColor(item.tone)),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 18,
            runSpacing: 10,
            children: visible.map((item) {
              final percent = (item.count / total * 100).round();
              return Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 6,
                    height: 6,
                    decoration: BoxDecoration(
                      color: _toneColor(item.tone),
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    item.label,
                    style: const TextStyle(
                      fontSize: 12,
                      color: _CockpitTheme.ink,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Text(
                    '${item.count} · $percent%',
                    style: const TextStyle(
                      fontSize: 12,
                      color: _CockpitTheme.dim,
                    ),
                  ),
                ],
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}

class _WorkloadSection extends StatelessWidget {
  final List<ExecutiveCockpitWorkload> items;

  const _WorkloadSection({required this.items});

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return const _InlineEmpty(text: '暂无负责人数据');
    final maxOpen = items.fold<int>(
      1,
      (maxValue, item) => math.max(maxValue, item.openCount),
    );

    return Column(
      children: [
        for (var i = 0; i < items.length; i++)
          _WorkloadRow(
            item: items[i],
            fraction: items[i].openCount / maxOpen,
            first: i == 0,
          ),
      ],
    );
  }
}

class _WorkloadRow extends StatelessWidget {
  final ExecutiveCockpitWorkload item;
  final double fraction;
  final bool first;

  const _WorkloadRow({
    required this.item,
    required this.fraction,
    required this.first,
  });

  @override
  Widget build(BuildContext context) {
    final risky = item.overdueCount > 0;
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 14, 24, 14),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: first ? Colors.transparent : _CockpitTheme.hairline,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  item.assigneeName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: _CockpitTheme.ink,
                  ),
                ),
              ),
              Text(
                '${item.openCount}',
                style: const TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: _CockpitTheme.ink,
                  letterSpacing: -0.3,
                ),
              ),
              if (risky) ...[
                const SizedBox(width: 6),
                Text(
                  '· 逾期 ${item.overdueCount}',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: _CockpitTheme.red,
                  ),
                ),
              ],
            ],
          ),
          const SizedBox(height: 10),
          Stack(
            children: [
              Container(
                height: 3,
                decoration: BoxDecoration(
                  color: _CockpitTheme.hairline,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              LayoutBuilder(
                builder: (context, constraints) => Container(
                  height: 3,
                  width: constraints.maxWidth * fraction.clamp(0.02, 1),
                  decoration: BoxDecoration(
                    color: risky ? _CockpitTheme.red : _CockpitTheme.ink,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _RiskSection extends StatelessWidget {
  final List<ExecutiveCockpitRisk> items;

  const _RiskSection({required this.items});

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const _InlineEmpty(text: '当前没有高风险需求 · 一切正常');
    }
    return Column(
      children: [
        for (var i = 0; i < items.length; i++)
          _RiskRow(item: items[i], first: i == 0),
      ],
    );
  }
}

class _RiskRow extends StatelessWidget {
  final ExecutiveCockpitRisk item;
  final bool first;

  const _RiskRow({required this.item, required this.first});

  @override
  Widget build(BuildContext context) {
    final due = item.expectedFinishDate == null
        ? '未设期望完成日'
        : '期望 ${_formatDate(item.expectedFinishDate!)}';
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 14, 24, 14),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: first ? Colors.transparent : _CockpitTheme.hairline,
          ),
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 4,
            height: 34,
            margin: const EdgeInsets.only(top: 2, right: 12),
            decoration: BoxDecoration(
              color: _CockpitTheme.red,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 14,
                    height: 1.35,
                    fontWeight: FontWeight.w600,
                    color: _CockpitTheme.ink,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${item.assigneeName} · ${item.statusLabel} · $due',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 12,
                    color: _CockpitTheme.dim,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Text(
            item.riskReason,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: _CockpitTheme.red,
              letterSpacing: 0.2,
            ),
          ),
        ],
      ),
    );
  }
}

class _OrdersysSection extends StatelessWidget {
  final List<ExecutiveCockpitRecentTask> items;

  const _OrdersysSection({required this.items});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (var i = 0; i < items.length; i++)
          _OrdersysRow(item: items[i], first: i == 0),
      ],
    );
  }
}

// 赔付月度 section —— calm / macOS·iOS 风：
//   • 去卡片化：无 shadow / 无 borderRadius / 无 harsh 分割线，靠 whitespace 分段
//   • 大数字 hero：金额独占一行，60pt 优雅字体
//   • 极简 chart：单色 + 极细网格线 + 轻标签
//   • 单一 accent：蓝灰 tone，只在 hover/highlight 上强调
class _PfSection extends StatelessWidget {
  final PfMonthlyOverview data;
  const _PfSection({required this.data});

  // Calm / iOS 调色板
  static const _accent = Color(0xFF0A84FF); // iOS system blue
  static const _tint = Color(0xFFE8ECF3);
  static const _ink = Color(0xFF111114);
  static const _softInk = Color(0xFF3F4046);
  static const _dim = Color(0xFF8A8A8E);
  static const _grid = Color(0xFFECECE8);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 30),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _label('赔付概览 · ${data.month}'),
          if (data.lastRefresh != null)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                '更新于 ${_formatMinute(data.lastRefresh!)}',
                style: const TextStyle(fontSize: 11, color: _dim),
              ),
            ),
          const SizedBox(height: 28),
          _hero(),
          const SizedBox(height: 40),
          _label('部门排行'),
          const SizedBox(height: 4),
          const Text(
            '平台承担金额 · 前 6 名',
            style: TextStyle(fontSize: 11, color: _dim, letterSpacing: 0.2),
          ),
          const SizedBox(height: 18),
          _deptRanks(),
          const SizedBox(height: 40),
          _label('日趋势'),
          const SizedBox(height: 4),
          const Text(
            '平台 / 门店 / 卖家',
            style: TextStyle(fontSize: 11, color: _dim, letterSpacing: 0.2),
          ),
          const SizedBox(height: 18),
          _trend(),
        ],
      ),
    );
  }

  static Widget _label(String text) => Text(
        text,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w700,
          color: _ink,
          letterSpacing: -0.2,
        ),
      );

  // Hero：大数字 60pt + 三档金额横排（无背景卡）
  Widget _hero() {
    final money = data.total.myMoney + data.total.outletsMoney + data.total.sellerMoney;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.baseline,
          textBaseline: TextBaseline.alphabetic,
          children: [
            Text(
              _formatMoney(money),
              style: const TextStyle(
                fontSize: 42,
                height: 1.05,
                fontWeight: FontWeight.w800,
                color: _ink,
                letterSpacing: -1.6,
              ),
            ),
            const SizedBox(width: 10),
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Text(
                '${data.total.cnt} 笔',
                style: const TextStyle(fontSize: 14, color: _dim),
              ),
            ),
            if (data.total.urgentCnt > 0) ...[
              const SizedBox(width: 10),
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: _tint,
                    borderRadius: BorderRadius.circular(3),
                  ),
                  child: Text(
                    '${data.total.urgentCnt} 加急',
                    style: const TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
                      color: _accent,
                    ),
                  ),
                ),
              ),
            ],
          ],
        ),
        const SizedBox(height: 24),
        // 三档小分栏，用极细分隔线
        Row(
          children: [
            Expanded(child: _split('平台承担', data.total.myMoney)),
            Container(width: 0.5, height: 34, color: _grid),
            Expanded(child: _split('门店承担', data.total.outletsMoney)),
            Container(width: 0.5, height: 34, color: _grid),
            Expanded(child: _split('卖家承担', data.total.sellerMoney)),
          ],
        ),
      ],
    );
  }

  Widget _split(String label, double v) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(fontSize: 11, color: _dim, letterSpacing: 0.2)),
        const SizedBox(height: 3),
        Text(_formatMoney(v),
            style: const TextStyle(
              fontSize: 16,
              height: 1.1,
              fontWeight: FontWeight.w700,
              color: _softInk,
            )),
      ],
    );
  }

  // 部门排行：**水平柱条**（比垂直柱状图更 iOS，因为部门名可以左边展全）
  Widget _deptRanks() {
    final top = data.deptRanking.take(6).toList();
    if (top.isEmpty) {
      return const SizedBox(
        height: 60,
        child: Center(
          child: Text('该区间暂无数据', style: TextStyle(color: _dim, fontSize: 12)),
        ),
      );
    }
    final maxV = top.map((d) => d.myMoney).reduce((a, b) => a > b ? a : b);
    return Column(
      children: [
        for (final d in top)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 7),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                SizedBox(
                  width: 78,
                  child: Text(
                    d.deptName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 13,
                      color: _softInk,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
                Expanded(
                  child: LayoutBuilder(
                    builder: (context, cs) {
                      final w = maxV == 0 ? 0.0 : (d.myMoney / maxV) * cs.maxWidth;
                      return Stack(
                        children: [
                          Container(
                            height: 6,
                            decoration: BoxDecoration(
                              color: _grid,
                              borderRadius: BorderRadius.circular(3),
                            ),
                          ),
                          Container(
                            height: 6,
                            width: w,
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                colors: [_accent, _accent.withValues(alpha: 0.75)],
                              ),
                              borderRadius: BorderRadius.circular(3),
                            ),
                          ),
                        ],
                      );
                    },
                  ),
                ),
                const SizedBox(width: 12),
                ConstrainedBox(
                  constraints: const BoxConstraints(minWidth: 66, maxWidth: 110),
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    alignment: Alignment.centerRight,
                    child: Text(
                    _formatMoney(d.myMoney),
                    maxLines: 1,
                    textAlign: TextAlign.right,
                    style: const TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                      color: _ink,
                      letterSpacing: 0.1,
                    ),
                  ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }

  // 日趋势：极简折线，无背景，无 dot，1.5px 细线，轻网格
  Widget _trend() {
    final points = data.trend;
    if (points.isEmpty) {
      return const SizedBox(
        height: 60,
        child: Center(
          child: Text('该区间暂无数据', style: TextStyle(color: _dim, fontSize: 12)),
        ),
      );
    }
    final maxY = points
        .map((p) => math.max(math.max(p.myMoney, p.outletsMoney), p.sellerMoney))
        .reduce((a, b) => a > b ? a : b);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            _legend('平台', _accent),
            const SizedBox(width: 14),
            _legend('门店', const Color(0xFF7B90A8)),
            const SizedBox(width: 14),
            _legend('卖家', const Color(0xFFA7B4C3)),
          ],
        ),
        const SizedBox(height: 14),
        SizedBox(
          height: 180,
          child: LineChart(
            LineChartData(
              minY: 0,
              maxY: (maxY == 0 ? 1 : maxY * 1.2),
              gridData: FlGridData(
                show: true,
                drawVerticalLine: false,
                horizontalInterval: (maxY == 0 ? 1 : maxY / 3),
                getDrawingHorizontalLine: (v) => FlLine(
                  color: _grid,
                  strokeWidth: 0.6,
                  dashArray: const [2, 3],
                ),
              ),
              borderData: FlBorderData(show: false),
              titlesData: FlTitlesData(
                leftTitles:
                    const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                topTitles:
                    const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                rightTitles:
                    const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                bottomTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    reservedSize: 22,
                    interval: math.max(1, (points.length / 5).floorToDouble()),
                    getTitlesWidget: (v, meta) {
                      final i = v.toInt();
                      if (i < 0 || i >= points.length) return const SizedBox();
                      final ymd = points[i].ymd;
                      final label = ymd.length >= 10 ? ymd.substring(5) : ymd;
                      return Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Text(
                          label,
                          style: const TextStyle(
                              fontSize: 9.5, color: _dim, letterSpacing: 0.1),
                        ),
                      );
                    },
                  ),
                ),
              ),
              lineTouchData: const LineTouchData(enabled: false),
              lineBarsData: [
                _softLine(points.map((p) => p.myMoney).toList(), _accent, 1.8),
                _softLine(points.map((p) => p.outletsMoney).toList(),
                    const Color(0xFF7B90A8), 1.2),
                _softLine(points.map((p) => p.sellerMoney).toList(),
                    const Color(0xFFA7B4C3), 1.2),
              ],
            ),
          ),
        ),
      ],
    );
  }

  static Widget _legend(String name, Color color) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 5),
        Text(name,
            style: const TextStyle(fontSize: 11, color: _dim, letterSpacing: 0.1)),
      ],
    );
  }

  static LineChartBarData _softLine(List<double> ys, Color color, double w) {
    return LineChartBarData(
      spots: [for (int i = 0; i < ys.length; i++) FlSpot(i.toDouble(), ys[i])],
      isCurved: true,
      curveSmoothness: 0.32,
      color: color,
      barWidth: w,
      dotData: const FlDotData(show: false),
      belowBarData: BarAreaData(show: false),
    );
  }

  static String _formatMoney(double v) {
    final abs = v.abs();
    if (abs >= 1e8) return '¥${(v / 1e8).toStringAsFixed(2)}亿';
    if (abs >= 1e4) {
      return '¥${(v / 1e4).toStringAsFixed(abs >= 1e6 ? 0 : 1)}万';
    }
    return '¥${v.toStringAsFixed(0)}';
  }
}

class _OrdersysRow extends StatelessWidget {
  final ExecutiveCockpitRecentTask item;
  final bool first;

  const _OrdersysRow({required this.item, required this.first});

  @override
  Widget build(BuildContext context) {
    final time = item.updateTime ?? item.taskTime;
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 12),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: first ? Colors.transparent : _CockpitTheme.hairline,
          ),
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 14,
                    height: 1.35,
                    fontWeight: FontWeight.w600,
                    color: _CockpitTheme.ink,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _joinText([
                    item.assigneeName,
                    item.operatorName,
                    time == null ? null : _formatMinute(time),
                  ]),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 12,
                    color: _CockpitTheme.dim,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Text(
            item.statusLabel,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: _CockpitTheme.accent,
              letterSpacing: 0.2,
            ),
          ),
        ],
      ),
    );
  }
}

class _LegendDot extends StatelessWidget {
  final Color color;
  final String label;

  const _LegendDot({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 6,
          height: 6,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: _CockpitTheme.dim,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }
}

class _InlineEmpty extends StatelessWidget {
  final String text;

  const _InlineEmpty({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
      child: Text(
        text,
        style: const TextStyle(fontSize: 13, color: _CockpitTheme.faint),
      ),
    );
  }
}

class _LoadingState extends StatelessWidget {
  const _LoadingState();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: SizedBox(
        width: 22,
        height: 22,
        child: CircularProgressIndicator(
          strokeWidth: 2,
          color: _CockpitTheme.ink,
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _ErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              '加载失败',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: _CockpitTheme.ink,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 13, color: _CockpitTheme.dim),
            ),
            const SizedBox(height: 18),
            OutlinedButton(
              onPressed: onRetry,
              style: OutlinedButton.styleFrom(
                foregroundColor: _CockpitTheme.ink,
                side: const BorderSide(color: _CockpitTheme.hairline),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(999),
                ),
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
              ),
              child: const Text(
                '重试',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TrendPainter extends CustomPainter {
  final List<ExecutiveCockpitTrendPoint> points;

  _TrendPainter(this.points);

  @override
  void paint(Canvas canvas, Size size) {
    final gridPaint = Paint()
      ..color = _CockpitTheme.hairline
      ..strokeWidth = 1;
    final createdPaint = Paint()
      ..color = _CockpitTheme.ink
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    final finishedPaint = Paint()
      ..color = _CockpitTheme.green
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    final fillPaint = Paint()
      ..color = _CockpitTheme.ink.withValues(alpha: 0.05)
      ..style = PaintingStyle.fill;

    const left = 12.0;
    const right = 12.0;
    const top = 14.0;
    const bottom = 24.0;
    final chartWidth = math.max(1.0, size.width - left - right);
    final chartHeight = math.max(1.0, size.height - top - bottom);
    final maxValue = points.fold<int>(
      1,
      (maxValue, point) =>
          math.max(maxValue, math.max(point.createdCount, point.finishedCount)),
    );

    for (var i = 0; i <= 3; i++) {
      final y = top + chartHeight * i / 3;
      canvas.drawLine(
        Offset(left, y),
        Offset(size.width - right, y),
        gridPaint,
      );
    }

    Path pathFor(int Function(ExecutiveCockpitTrendPoint) valueOf) {
      final path = Path();
      for (var i = 0; i < points.length; i++) {
        final x = points.length == 1
            ? left + chartWidth / 2
            : left + chartWidth * i / (points.length - 1);
        final y = top + chartHeight * (1 - valueOf(points[i]) / maxValue);
        if (i == 0) {
          path.moveTo(x, y);
        } else {
          path.lineTo(x, y);
        }
      }
      return path;
    }

    final createdPath = pathFor((point) => point.createdCount);
    final createdFill = Path.from(createdPath)
      ..lineTo(size.width - right, top + chartHeight)
      ..lineTo(left, top + chartHeight)
      ..close();
    canvas.drawPath(createdFill, fillPaint);
    canvas.drawPath(createdPath, createdPaint);
    canvas.drawPath(pathFor((point) => point.finishedCount), finishedPaint);

    final labels = _pickLabels(points);
    final textPainter = TextPainter(
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
    );
    for (final label in labels) {
      final index = label.key;
      final text = label.value;
      final x = points.length == 1
          ? left + chartWidth / 2
          : left + chartWidth * index / (points.length - 1);
      textPainter.text = TextSpan(
        text: text,
        style: const TextStyle(fontSize: 10, color: _CockpitTheme.faint),
      );
      textPainter.layout();
      textPainter.paint(
        canvas,
        Offset(
          (x - textPainter.width / 2)
              .clamp(0, size.width - textPainter.width)
              .toDouble(),
          size.height - 16,
        ),
      );
    }
  }

  @override
  bool shouldRepaint(covariant _TrendPainter oldDelegate) {
    return oldDelegate.points != points;
  }
}

List<MapEntry<int, String>> _pickLabels(
  List<ExecutiveCockpitTrendPoint> points,
) {
  if (points.isEmpty) return const [];
  final indexes = <int>{0, points.length ~/ 2, points.length - 1}.toList()
    ..sort();
  return indexes
      .map((index) => MapEntry(index, _monthDay(points[index].date)))
      .toList();
}

// -------------------- AGENT TAB --------------------

class AgentTab extends StatefulWidget {
  final ExecutiveCockpitOverview? overview;
  final int rangeDays;
  final Future<void> Function() onSnapshotRequested;
  // 输入框左边的收缩菜单 icon 回调；AiChatPage 传进来打开 bottom sheet 切 tab
  final VoidCallback? onOpenNav;
  // 上下文字符数变化时通知外部（AiChatPage 顶栏画进度条）
  final ValueChanged<int>? onContextCharsChanged;

  const AgentTab({
    super.key,
    required this.overview,
    required this.rangeDays,
    required this.onSnapshotRequested,
    this.onOpenNav,
    this.onContextCharsChanged,
  });

  @override
  State<AgentTab> createState() => AgentTabState();
}

class AgentTabState extends State<AgentTab> {
  static const _kConversationIdKey = 'cockpit_agent_conversation_id';

  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  final ScrollController _scrollController = ScrollController();
  final List<_ChatTurn> _turns = [];
  // 顶栏画上下文进度用：所有轮的用户+助手文本长度求和
  final ValueNotifier<int> contextCharsNotifier = ValueNotifier<int>(0);

  StreamSubscription<AgentStreamEvent>? _subscription;
  _ChatTurn? _activeAssistantTurn;
  String _conversationId = _newConversationId();
  bool _running = false;
  int _runSerial = 0;

  void _recomputeContextChars() {
    var total = 0;
    for (final t in _turns) {
      total += t.text.length;
      for (final a in t.activities) {
        if (a is _AnswerActivity) total += a.text.length;
        else if (a is _ReasoningActivity) total += a.text.length;
      }
    }
    contextCharsNotifier.value = total;
    widget.onContextCharsChanged?.call(total);
  }

  Timer? _contextTimer;

  @override
  void initState() {
    super.initState();
    _restoreConversationId();
    // 每 1s 重算一次上下文长度，AiChatPage 顶栏进度条读取
    // 增量精算改动面太大（setState 分布在 15+ 处），走定时聚合更省事
    _contextTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) _recomputeContextChars();
    });
  }

  @override
  void dispose() {
    _contextTimer?.cancel();
    contextCharsNotifier.dispose();
    _subscription?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _restoreConversationId() async {
    String? restored;
    try {
      final prefs = await SharedPreferences.getInstance();
      final saved = prefs.getString(_kConversationIdKey);
      if (saved != null && saved.trim().isNotEmpty && mounted) {
        restored = saved;
        setState(() => _conversationId = saved);
      } else {
        // First run — persist the generated one so history begins accumulating.
        await prefs.setString(_kConversationIdKey, _conversationId);
      }
    } catch (_) {
      // If prefs unavailable, keep the in-memory ID.
    }
    if (restored != null) {
      await _hydrateFromRemote(restored);
    }
  }

  Future<void> _persistConversationId(String id) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_kConversationIdKey, id);
    } catch (_) {}
  }

  List<String> get _suggestions {
    final overview = widget.overview;
    if (overview == null) {
      return const ['今天有什么风险需要关注', '按负责人看谁在办最多', '汇总近期需求进展'];
    }
    final list = <String>[
      '谁的在办最多，说明原因',
      '列出最紧急的 3 个风险需求',
      '近 ${widget.rangeDays} 天的完成节奏怎么样',
    ];
    if (overview.ordersysAvailable) {
      list.add('看看 ordersys 最近的工单动态');
    }
    return list;
  }

  /// 抽屉唤起：切换到已存在的会话，清空当前 turns，从后端拉历史消息回填。
  Future<void> switchToConversation(String conversationId) async {
    final id = conversationId.trim();
    if (id.isEmpty) return;
    if (_running) {
      await _stopGeneration(message: '已切换会话，上一轮查询已停止。');
    } else {
      await _subscription?.cancel();
    }
    if (!mounted) return;
    setState(() {
      _turns.clear();
      _conversationId = id;
    });
    await _persistConversationId(id);
    await _hydrateFromRemote(id);
  }

  /// 抽屉唤起：新建空会话。
  Future<void> startNewConversation() async {
    if (_running) {
      await _stopGeneration(message: '已新建会话，上一轮查询已停止。');
    } else {
      await _subscription?.cancel();
    }
    if (!mounted) return;
    final freshId = _newConversationId();
    setState(() {
      _turns.clear();
      _conversationId = freshId;
    });
    _persistConversationId(freshId);
  }

  Future<void> _hydrateFromRemote(String conversationId) async {
    try {
      final messages = await AgentConversationService.messages(
        conversationId,
        limit: 200,
      );
      if (!mounted || _conversationId != conversationId) return;
      final turns = <_ChatTurn>[];
      for (final msg in messages) {
        final content = msg.content.trim();
        if (content.isEmpty) continue;
        if (msg.role == 'user') {
          turns.add(_ChatTurn.user(content));
        } else if (msg.role == 'assistant') {
          final t = _ChatTurn.assistant();
          t.replaceText(content);
          turns.add(t);
        }
      }
      if (!mounted) return;
      setState(() {
        _turns
          ..clear()
          ..addAll(turns);
      });
      _scrollToBottom();
    } catch (_) {
      // 回填失败静默：不影响新的对话继续发送。
    }
  }

  void _reset() {
    if (_running) return;
    _subscription?.cancel();
    final freshId = _newConversationId();
    setState(() {
      _turns.clear();
      _conversationId = freshId;
    });
    _persistConversationId(freshId);
  }

  Future<void> _stopGeneration({String message = '已暂停，这次查询和工具调用都已停止。'}) async {
    if (!_running && _subscription == null) return;
    _runSerial++;
    final subscription = _subscription;
    _subscription = null;
    final activeTurn = _activeAssistantTurn;
    if (mounted) {
      setState(() {
        activeTurn?.markStopped(message);
        _activeAssistantTurn = null;
        _running = false;
      });
      _scrollToBottom();
    }
    try {
      await subscription?.cancel();
    } catch (_) {
      // Cancellation is best-effort; the UI has already detached from this run.
    }
  }

  void _send([String? preset]) {
    if (_running) {
      _stopGeneration();
      return;
    }
    final raw = (preset ?? _controller.text).trim();
    if (raw.isEmpty) {
      return;
    }
    _focusNode.unfocus();
    _controller.clear();

    final userTurn = _ChatTurn.user(raw);
    final assistantTurn = _ChatTurn.assistant();
    setState(() {
      _turns.add(userTurn);
      _turns.add(assistantTurn);
      _activeAssistantTurn = assistantTurn;
      _running = true;
    });
    _scrollToBottom();

    final messages = _turns
        .where((turn) => !turn.isError && turn.text.trim().isNotEmpty)
        .map((turn) => AgentChatMessage(role: turn.role, content: turn.text))
        .toList();

    final systemPrompt = _buildSystemPrompt();
    final runId = ++_runSerial;

    _subscription =
        AgentService.stream(
          messages: messages,
          conversationId: _conversationId,
          systemPrompt: systemPrompt,
          maxSteps: 24,
        ).listen(
          (event) {
            if (!_isCurrentRun(runId)) return;
            _handleEvent(assistantTurn, event);
          },
          onError: (error) {
            if (!_isCurrentRun(runId)) return;
            _runSerial++;
            setState(() {
              assistantTurn.markError(_cleanError(error));
              _activeAssistantTurn = null;
              _running = false;
              _subscription = null;
            });
          },
          onDone: () {
            if (!_isCurrentRun(runId)) return;
            setState(() {
              assistantTurn.finalize();
              _activeAssistantTurn = null;
              _running = false;
              _subscription = null;
            });
            _scrollToBottom();
            // Refresh underlying snapshot in background so metrics tab stays fresh.
            widget.onSnapshotRequested();
          },
          cancelOnError: true,
        );
  }

  bool _isCurrentRun(int runId) => mounted && _runSerial == runId;

  String _buildSystemPrompt() {
    final overview = widget.overview;
    final buffer = StringBuffer('你是嵌在“总裁驾驶舱”里的经营分析助手。回答简明、结构清晰，中文口吻自然。');
    buffer.write(' 除非用户明确要求，回答不超过 6 行。');
    if (overview != null) {
      final open = overview.metric('requirement_open')?.numericValue ?? 0;
      final overdue = overview.metric('requirement_overdue')?.numericValue ?? 0;
      buffer.write(' 当前视角：近 ${widget.rangeDays} 天；在办 $open 件；逾期 $overdue 件。');
    }
    return buffer.toString();
  }

  void _handleEvent(_ChatTurn turn, AgentStreamEvent event) {
    if (!mounted) return;
    setState(() {
      switch (event.type) {
        case 'text_delta':
          final delta = event.delta;
          if (delta != null) turn.appendText(delta);
          break;
        case 'text':
          // Full text messages are used only when we haven't seen streaming
          // deltas yet — otherwise deltas already carried the content.
          if (turn.activities.whereType<_AnswerActivity>().isEmpty) {
            final full = event.fullText;
            if (full != null) turn.replaceText(full);
          }
          break;
        case 'text_done':
          turn.finishText();
          break;
        case 'reasoning_delta':
        case 'thinking':
          final delta = event.delta;
          if (delta != null) turn.appendReasoning(delta);
          break;
        case 'reasoning':
        case 'reasoning_complete':
          turn.finishReasoning();
          break;
        case 'tool_call':
        case 'tool_call_start':
        case 'tool_call_item':
        case 'tool_use':
        case 'function_call':
          final name = event.toolName;
          if (name != null) {
            turn.startTool(
              id: event.toolCallId,
              name: name,
              argsPreview: event.argsPreview,
              humanLabel: event.humanLabel,
            );
          }
          break;
        case 'tool_call_delta':
          break;
        case 'tool_call_done':
        case 'tool_output':
        case 'tool_call_output_item':
        case 'tool_result':
        case 'function_call_output':
          final success = event.toolSuccess;
          if (success == false) {
            turn.failTool(
              id: event.toolCallId,
              name: event.toolName,
              error: event.errorMessage ?? event.outputPreview,
              humanLabel: event.humanLabel,
              durationMs: event.durationMs,
            );
          } else {
            turn.finishTool(
              id: event.toolCallId,
              name: event.toolName,
              outputPreview: event.outputPreview,
              humanLabel: event.humanLabel,
              durationMs: event.durationMs,
            );
          }
          break;
        case 'tool_error':
          turn.failTool(
            id: event.toolCallId,
            name: event.toolName,
            error: event.errorMessage,
            humanLabel: event.humanLabel,
            durationMs: event.durationMs,
          );
          break;
        case 'chart':
          // Backend emits this after `render_chart` tool_result — spec is the
          // full untruncated validated chart args.
          final raw = event.data['spec'];
          if (raw is Map) {
            turn.addChart(Map<String, dynamic>.from(raw));
          }
          break;
        case 'iteration_start':
          final iteration = event.iteration;
          if (iteration != null) turn.markIteration(iteration);
          break;
        case 'step_start':
        case 'step_end':
          break;
        case 'error':
          turn.markError(event.errorMessage ?? '模型异常');
          break;
        case 'run_done':
        case 'done':
          break;
        default:
          break;
      }
    });
    _scrollToBottom();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOutCubic,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final isEmpty = _turns.isEmpty;
    // 聊天区占满整个屏幕；pill 真正悬浮在最上层（不占垂直空间，无外层背景）
    // 聊天区通过 ListView 的 padding.bottom 给最后一条消息让出 pill 下方空间
    return Stack(
      children: [
        Positioned.fill(
          child: isEmpty ? _buildEmptyState() : _buildTranscript(),
        ),
        Positioned(
          left: 0,
          right: 0,
          bottom: 0,
          child: _ComposerBar(
            controller: _controller,
            focusNode: _focusNode,
            running: _running,
            canReset: !_running && _turns.isNotEmpty,
            onSend: _send,
            onStop: () {
              _stopGeneration();
            },
            onReset: _reset,
            onOpenNav: widget.onOpenNav,
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    final bottomSafe = MediaQuery.paddingOf(context).bottom;
    return ListView(
      padding: EdgeInsets.fromLTRB(24, 40, 24, 44 + 16 + bottomSafe + 12),
      children: [
        const Text(
          '问驾驶舱',
          style: TextStyle(
            fontSize: 30,
            height: 1.1,
            fontWeight: FontWeight.w800,
            letterSpacing: -0.6,
            color: _CockpitTheme.ink,
          ),
        ),
        const SizedBox(height: 10),
        const Text(
          '基于当前经营数据的自由问答。可以让它查需求、看工单、找风险、给出建议。',
          style: TextStyle(
            fontSize: 14,
            height: 1.55,
            color: _CockpitTheme.dim,
          ),
        ),
        const SizedBox(height: 28),
        const _SectionHeader(label: '试试这些问题'),
        const SizedBox(height: 14),
        for (final suggestion in _suggestions)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: _SuggestionTile(
              text: suggestion,
              onTap: () => _send(suggestion),
            ),
          ),
      ],
    );
  }

  Widget _buildTranscript() {
    // 底部预留 pill(44) + margin(16) + safeArea 的空间；让内容能滑到 pill 底下
    final bottomSafe = MediaQuery.paddingOf(context).bottom;
    return ListView.builder(
      controller: _scrollController,
      padding: EdgeInsets.fromLTRB(0, 20, 0, 44 + 16 + bottomSafe + 12),
      itemCount: _turns.length,
      itemBuilder: (context, index) {
        final turn = _turns[index];
        if (turn.role == 'user') {
          return _UserTurnView(text: turn.text);
        }
        return _AssistantTurnView(
          turn: turn,
          streaming: _running && index == _turns.length - 1,
        );
      },
    );
  }
}

class _ComposerBar extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool running;
  final bool canReset;
  final ValueChanged<String?> onSend;
  final VoidCallback onStop;
  final VoidCallback onReset;
  // 悬浮菜单收进输入框：左侧 icon 打开 tab 选择器
  final VoidCallback? onOpenNav;

  const _ComposerBar({
    required this.controller,
    required this.focusNode,
    required this.running,
    required this.canReset,
    required this.onSend,
    required this.onStop,
    required this.onReset,
    this.onOpenNav,
  });

  @override
  Widget build(BuildContext context) {
    // 悬浮 pill 风格：外无背景无 border，pill 白底 + 大圆角 + 微阴影，跟底部菜单同款视觉
    return Padding(
      padding: EdgeInsets.fromLTRB(
        16,
        6,
        16,
        MediaQuery.paddingOf(context).bottom + 10,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: Container(
              constraints: const BoxConstraints(minHeight: 44, maxHeight: 132),
              padding: EdgeInsets.only(
                left: (onOpenNav != null || canReset) ? 6 : 18,
                right: 4,
                top: 4,
                bottom: 4,
              ),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(32),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x14000000),
                    blurRadius: 24,
                    offset: Offset(0, 6),
                  ),
                  BoxShadow(
                    color: Color(0x08000000),
                    blurRadius: 3,
                    offset: Offset(0, 1),
                  ),
                ],
                border: Border.all(color: const Color(0xFFEEEEEE), width: 0.5),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  if (onOpenNav != null)
                    GestureDetector(
                      onTap: onOpenNav,
                      behavior: HitTestBehavior.opaque,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: Icon(
                          Icons.apps_rounded,
                          size: 22,
                          color: _CockpitTheme.dim,
                        ),
                      ),
                    ),
                  if (onOpenNav != null) const SizedBox(width: 6),
                  if (canReset)
                    GestureDetector(
                      onTap: onReset,
                      behavior: HitTestBehavior.opaque,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: Icon(
                          Icons.refresh_rounded,
                          size: 20,
                          color: _CockpitTheme.dim,
                        ),
                      ),
                    ),
                  if (canReset) const SizedBox(width: 6),
                  Expanded(
                    child: TextField(
                      controller: controller,
                      focusNode: focusNode,
                      enabled: !running,
                      minLines: 1,
                      maxLines: 4,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => running ? null : onSend(null),
                      cursorColor: _CockpitTheme.ink,
                      cursorWidth: 1.6,
                      style: const TextStyle(
                        fontSize: 15,
                        height: 1.35,
                        color: _CockpitTheme.ink,
                      ),
                      decoration: const InputDecoration(
                        // hint 单行不换行，超长直接省略
                        hintText: '问点什么…',
                        hintMaxLines: 1,
                        hintStyle: TextStyle(
                          fontSize: 14,
                          color: _CockpitTheme.faint,
                        ),
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        isDense: true,
                        filled: false,
                        contentPadding: EdgeInsets.symmetric(vertical: 8),
                      ),
                    ),
                  ),
                  _InlineSendButton(
                    running: running,
                    onTap: running ? onStop : () => onSend(null),
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

class _InlineSendButton extends StatelessWidget {
  final bool running;
  final VoidCallback? onTap;

  const _InlineSendButton({required this.running, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        width: 38,
        height: 38,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: running ? _CockpitTheme.red : _CockpitTheme.ink,
          shape: BoxShape.circle,
        ),
        child: running
            ? const Icon(Icons.stop_rounded, color: Colors.white, size: 20)
            : const Icon(
                Icons.arrow_upward_rounded,
                color: Colors.white,
                size: 20,
              ),
      ),
    );
  }
}

class _SuggestionTile extends StatelessWidget {
  final String text;
  final VoidCallback onTap;

  const _SuggestionTile({required this.text, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          decoration: BoxDecoration(
            color: _CockpitTheme.surface,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: _CockpitTheme.hairline),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  text,
                  style: const TextStyle(
                    fontSize: 14,
                    color: _CockpitTheme.ink,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              const Icon(
                Icons.north_east_rounded,
                size: 16,
                color: _CockpitTheme.faint,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _UserTurnView extends StatelessWidget {
  final String text;

  const _UserTurnView({required this.text});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerRight,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.78,
        ),
        margin: const EdgeInsets.fromLTRB(24, 6, 20, 14),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: _CockpitTheme.ink,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(18),
            topRight: Radius.circular(18),
            bottomLeft: Radius.circular(18),
            bottomRight: Radius.circular(4),
          ),
        ),
        child: SelectableText(
          text,
          style: const TextStyle(
            fontSize: 14,
            height: 1.45,
            color: Colors.white,
          ),
        ),
      ),
    );
  }
}

class _AssistantTurnView extends StatelessWidget {
  final _ChatTurn turn;
  final bool streaming;

  const _AssistantTurnView({required this.turn, required this.streaming});

  @override
  Widget build(BuildContext context) {
    final activities = turn.activities;
    final toolActivities = activities.whereType<_ToolActivity>().toList();
    final hasPendingTool = toolActivities.any(
      (activity) => !activity.completed,
    );
    final waitingForModel =
        streaming && !hasPendingTool && !turn.hasStreamingTail;
    final waitingText = activities.isEmpty ? '我先判断一下该怎么查。' : '查到的数据已收齐，正在整理回答。';
    // Render every activity inline in the order the server emitted it. The
    // previous "when tools > 2, collapse them all into _ToolActivityGroup"
    // was a workaround for verbose old tools; the new api_search + api_call
    // surface is compact enough that each call reads better as its own tile
    // interleaved with the thinking blocks that produced it.
    final children = <Widget>[
      for (var i = 0; i < activities.length; i++)
        _ActivityView(
          activity: activities[i],
          streaming: streaming && i == activities.length - 1,
          isError: turn.isError,
        ),
    ];

    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 6, 24, 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ...children,
          if (waitingForModel)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: _WaitingLine(text: waitingText),
            ),
          if (turn.isError && turn.text.isEmpty)
            _ErrorLine(text: turn.errorMessage ?? '小助理开小差了，可以重试。'),
        ],
      ),
    );
  }
}

class _ActivityView extends StatelessWidget {
  final _Activity activity;
  final bool streaming;
  final bool isError;

  const _ActivityView({
    required this.activity,
    required this.streaming,
    required this.isError,
  });

  @override
  Widget build(BuildContext context) {
    final act = activity;
    if (act is _ReasoningActivity) {
      return _ReasoningBlock(
        text: act.text,
        streaming: streaming && !act.completed,
      );
    }
    if (act is _ToolActivity) {
      return _ToolTile(activity: act);
    }
    if (act is _AnswerActivity) {
      return Padding(
        padding: const EdgeInsets.only(top: 6, bottom: 2),
        child: _MarkdownAnswer(
          text: act.text,
          streaming: streaming,
          isError: isError,
        ),
      );
    }
    if (act is _ChartActivity) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: _ChartCard(spec: act.spec),
      );
    }
    return const SizedBox.shrink();
  }
}

class _ToolTile extends StatefulWidget {
  final _ToolActivity activity;

  const _ToolTile({required this.activity});

  @override
  State<_ToolTile> createState() => _ToolTileState();
}

class _ToolTileState extends State<_ToolTile> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) {
    final act = widget.activity;
    final Color color;
    final Widget leading;

    if (act.cancelled) {
      color = _CockpitTheme.faint;
      leading = const _StatusPip(color: _CockpitTheme.faint);
    } else if (act.failed) {
      color = _CockpitTheme.red;
      leading = const _StatusPip(color: _CockpitTheme.red);
    } else if (act.completed) {
      color = _CockpitTheme.green;
      leading = const _StatusPip(color: _CockpitTheme.green);
    } else {
      color = _CockpitTheme.accent;
      leading = const _SpinnerDot(color: _CockpitTheme.accent);
    }

    final canExpand = _hasDetail(act);
    final label = _toolStatusLabel(act);
    final args = _prettyArgs(act.argsPreview);
    final durationText = act.durationMs == null
        ? null
        : _formatMillis(act.durationMs!);
    final labelText = Text(
      label,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: color),
    );

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Container(
        decoration: BoxDecoration(
          color: _CockpitTheme.hairline.withValues(alpha: 0.35),
          borderRadius: BorderRadius.circular(10),
        ),
        padding: const EdgeInsets.fromLTRB(12, 10, 10, 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: canExpand
                  ? () => setState(() => _expanded = !_expanded)
                  : null,
              child: Row(
                children: [
                  leading,
                  const SizedBox(width: 8),
                  Expanded(
                    child: act.completed
                        ? labelText
                        : _Shimmer(child: labelText),
                  ),
                  if (durationText != null)
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 6),
                      child: Text(
                        durationText,
                        style: const TextStyle(
                          fontSize: 11,
                          color: _CockpitTheme.faint,
                          fontFeatures: [FontFeature.tabularFigures()],
                        ),
                      ),
                    ),
                  if (canExpand)
                    Icon(
                      _expanded ? Icons.expand_less : Icons.expand_more,
                      size: 16,
                      color: _CockpitTheme.dim,
                    ),
                ],
              ),
            ),
            if (args != null) ...[
              const SizedBox(height: 4),
              Padding(
                padding: const EdgeInsets.only(left: 22),
                child: Text(
                  args,
                  maxLines: _expanded ? 6 : 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 12,
                    height: 1.4,
                    color: _CockpitTheme.dim,
                    fontFeatures: [FontFeature.tabularFigures()],
                  ),
                ),
              ),
            ],
            if (_expanded) ...[
              if (act.outputPreview != null &&
                  act.outputPreview!.isNotEmpty) ...[
                const SizedBox(height: 8),
                _detailHeader('结果'),
                const SizedBox(height: 4),
                _detailBody(act.outputPreview!),
              ],
              if ((act.failed || act.cancelled) &&
                  act.error != null &&
                  act.error!.isNotEmpty) ...[
                const SizedBox(height: 8),
                _detailHeader(act.cancelled ? '状态' : '错误'),
                const SizedBox(height: 4),
                _detailBody(
                  act.error!,
                  color: act.cancelled ? _CockpitTheme.dim : _CockpitTheme.red,
                ),
              ],
            ] else if ((act.failed || act.cancelled) && act.error != null) ...[
              const SizedBox(height: 4),
              Padding(
                padding: const EdgeInsets.only(left: 22),
                child: Text(
                  act.error!,
                  maxLines: 5,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 11.5,
                    height: 1.4,
                    color: act.cancelled
                        ? _CockpitTheme.dim
                        : _CockpitTheme.red,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _detailHeader(String label) => Padding(
    padding: const EdgeInsets.only(left: 22),
    child: Text(
      label,
      style: const TextStyle(
        fontSize: 10,
        fontWeight: FontWeight.w700,
        letterSpacing: 1.2,
        color: _CockpitTheme.faint,
      ),
    ),
  );

  Widget _detailBody(String text, {Color color = _CockpitTheme.ink}) => Padding(
    padding: const EdgeInsets.only(left: 22),
    child: SelectableText(
      text,
      style: TextStyle(
        fontSize: 11.5,
        height: 1.5,
        color: color,
        fontFamily: 'monospace',
      ),
    ),
  );

  bool _hasDetail(_ToolActivity act) {
    if (act.outputPreview != null && act.outputPreview!.isNotEmpty) return true;
    if ((act.failed || act.cancelled) && (act.error?.isNotEmpty ?? false)) {
      return true;
    }
    if ((act.argsPreview ?? '').length > 40) return true;
    return false;
  }
}

class _ToolActivityGroup extends StatefulWidget {
  final List<_ToolActivity> activities;
  final bool streaming;

  const _ToolActivityGroup({required this.activities, required this.streaming});

  @override
  State<_ToolActivityGroup> createState() => _ToolActivityGroupState();
}

class _ToolActivityGroupState extends State<_ToolActivityGroup> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) {
    final total = widget.activities.length;
    final pending = widget.activities.where((activity) {
      return !activity.completed;
    }).length;
    final failed = widget.activities.where((activity) {
      return activity.failed && !activity.cancelled;
    }).length;
    final cancelled = widget.activities.where((activity) {
      return activity.cancelled;
    }).length;
    final succeeded = widget.activities.where((activity) {
      return activity.completed && !activity.failed && !activity.cancelled;
    }).length;

    final active = widget.streaming || pending > 0;
    final Color color;
    final Widget leading;
    final String title;
    if (active) {
      color = _CockpitTheme.accent;
      leading = const _SpinnerDot(color: _CockpitTheme.accent);
      title = '正在跑 $total 个查询，先收起来';
    } else if (failed > 0) {
      color = _CockpitTheme.red;
      leading = const _StatusPip(color: _CockpitTheme.red);
      title = '$failed 个查询没查成，点开看原因';
    } else if (cancelled > 0) {
      color = _CockpitTheme.faint;
      leading = const _StatusPip(color: _CockpitTheme.faint);
      title = '已停止 $total 个查询';
    } else {
      color = _CockpitTheme.green;
      leading = const _StatusPip(color: _CockpitTheme.green);
      title = '$total 个查询跑完了，点开看明细';
    }

    final statusParts = <String>[
      if (succeeded > 0) '完成 $succeeded',
      if (pending > 0) '进行中 $pending',
      if (failed > 0) '失败 $failed',
      if (cancelled > 0) '暂停 $cancelled',
    ];
    final titleText = Text(
      title,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: color),
    );

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => setState(() => _expanded = !_expanded),
            child: Container(
              decoration: BoxDecoration(
                color: _CockpitTheme.hairline.withValues(alpha: 0.35),
                borderRadius: BorderRadius.circular(10),
              ),
              padding: const EdgeInsets.fromLTRB(12, 10, 10, 10),
              child: Row(
                children: [
                  leading,
                  const SizedBox(width: 8),
                  Expanded(
                    child: active ? _Shimmer(child: titleText) : titleText,
                  ),
                  if (statusParts.isNotEmpty) ...[
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        statusParts.join(' · '),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.right,
                        style: const TextStyle(
                          fontSize: 11,
                          color: _CockpitTheme.faint,
                          fontFeatures: [FontFeature.tabularFigures()],
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(width: 6),
                  Icon(
                    _expanded ? Icons.expand_less : Icons.expand_more,
                    size: 16,
                    color: _CockpitTheme.dim,
                  ),
                ],
              ),
            ),
          ),
          if (_expanded) ...[
            const SizedBox(height: 4),
            for (final activity in widget.activities)
              _ToolTile(activity: activity),
          ],
        ],
      ),
    );
  }
}

class _ReasoningBlock extends StatefulWidget {
  final String text;
  final bool streaming;

  const _ReasoningBlock({required this.text, required this.streaming});

  @override
  State<_ReasoningBlock> createState() => _ReasoningBlockState();
}

class _ReasoningBlockState extends State<_ReasoningBlock> {
  // Expanded ONLY while the block is actively streaming; collapse the moment
  // reasoning finishes (or if it was never streaming when the widget mounted,
  // start collapsed — that happens for every prior reasoning block once the
  // model moves on to a tool call or a new reasoning burst).
  late bool _expanded = widget.streaming;
  bool _autoCollapsed = false;

  @override
  void didUpdateWidget(covariant _ReasoningBlock oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!widget.streaming && oldWidget.streaming && !_autoCollapsed) {
      _autoCollapsed = true;
      _expanded = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final preview = widget.text.replaceAll('\n', ' ').trim();
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 4),
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
      decoration: BoxDecoration(
        color: _CockpitTheme.hairline.withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => setState(() => _expanded = !_expanded),
            child: Row(
              children: [
                Icon(
                  widget.streaming
                      ? Icons.psychology_alt_outlined
                      : Icons.psychology_outlined,
                  size: 14,
                  color: _CockpitTheme.dim,
                ),
                const SizedBox(width: 6),
                Text(
                  widget.streaming ? '小助理正在捋思路' : '思考过程',
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1.2,
                    color: _CockpitTheme.dim,
                  ),
                ),
                if (widget.streaming) ...[
                  const SizedBox(width: 6),
                  const _SpinnerDot(color: _CockpitTheme.dim),
                ],
                const Spacer(),
                Icon(
                  _expanded ? Icons.expand_less : Icons.expand_more,
                  size: 16,
                  color: _CockpitTheme.dim,
                ),
              ],
            ),
          ),
          if (_expanded) ...[
            const SizedBox(height: 6),
            Text(
              widget.text,
              style: const TextStyle(
                fontSize: 12.5,
                height: 1.55,
                color: _CockpitTheme.dim,
              ),
            ),
          ] else if (preview.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              preview,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 12, color: _CockpitTheme.faint),
            ),
          ],
        ],
      ),
    );
  }
}

class _MarkdownAnswer extends StatelessWidget {
  final String text;
  final bool streaming;
  final bool isError;

  const _MarkdownAnswer({
    required this.text,
    required this.streaming,
    required this.isError,
  });

  @override
  Widget build(BuildContext context) {
    // 高级质感 markdown 样式：
    //   • 字体：无衬线，字号 15.5，行高 1.65（阅读舒适）
    //   • 段落间距 12（呼吸感）
    //   • 标题字重 800/700，跟正文有明显层级
    //   • 代码：淡灰底 + 圆角，monospace，字号 13
    //   • 引用：左侧 3px accent 色柔和条带（不用 harsh 灰）
    //   • 分割线：极淡 hairline
    final ink = isError ? _CockpitTheme.red : _CockpitTheme.ink;
    // 字号 14.5（比之前 15.5 缩一档），行高 1.65 保留呼吸感
    final base = TextStyle(
      fontSize: 14.5,
      height: 1.65,
      color: ink,
      letterSpacing: 0.15,
    );
    const softShade = Color(0xFFF3F3F1);
    const softShadeTint = Color(0xFFF6F6F3);
    const accentSoft = Color(0xFF1D4ED8);
    final ss = MarkdownStyleSheet(
      p: base,
      pPadding: const EdgeInsets.only(bottom: 12),
      h1: base.copyWith(
        fontSize: 22,
        height: 1.35,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.4,
      ),
      h1Padding: const EdgeInsets.only(top: 18, bottom: 10),
      h2: base.copyWith(
        fontSize: 18,
        height: 1.4,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.2,
      ),
      h2Padding: const EdgeInsets.only(top: 20, bottom: 10),
      h3: base.copyWith(
        fontSize: 15.5,
        height: 1.4,
        fontWeight: FontWeight.w700,
      ),
      h3Padding: const EdgeInsets.only(top: 16, bottom: 8),
      h4: base.copyWith(fontSize: 14.5, fontWeight: FontWeight.w700),
      h4Padding: const EdgeInsets.only(top: 14, bottom: 8),
      listBullet: base,
      listBulletPadding: const EdgeInsets.only(right: 10),
      listIndent: 22,
      blockSpacing: 14,
      strong: base.copyWith(fontWeight: FontWeight.w700),
      em: base.copyWith(fontStyle: FontStyle.italic),
      a: base.copyWith(
        color: accentSoft,
        decoration: TextDecoration.underline,
        decorationColor: accentSoft.withValues(alpha: 0.35),
      ),
      code: TextStyle(
        fontFamily: 'monospace',
        fontSize: 12.5,
        height: 1.5,
        color: const Color(0xFFC11B37),
        backgroundColor: softShade,
      ),
      codeblockDecoration: BoxDecoration(
        color: softShade,
        borderRadius: BorderRadius.circular(12),
      ),
      codeblockPadding: const EdgeInsets.all(14),
      blockquoteDecoration: BoxDecoration(
        color: softShadeTint,
        borderRadius: BorderRadius.circular(10),
        border: const Border(
          left: BorderSide(color: accentSoft, width: 3),
        ),
      ),
      blockquotePadding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
      tableHead: base.copyWith(fontWeight: FontWeight.w800, fontSize: 12.5),
      tableBody: base.copyWith(fontSize: 12.5, height: 1.5),
      tableBorder: TableBorder(
        top: BorderSide(color: _CockpitTheme.hairline),
        bottom: BorderSide(color: _CockpitTheme.hairline),
        horizontalInside: BorderSide(color: _CockpitTheme.hairline.withValues(alpha: 0.55)),
      ),
      tableHeadAlign: TextAlign.left,
      tableCellsPadding: const EdgeInsets.symmetric(
        horizontal: 12,
        vertical: 10,
      ),
      tableColumnWidth: const IntrinsicColumnWidth(),
      tableCellsDecoration: const BoxDecoration(),
      horizontalRuleDecoration: const BoxDecoration(
        border: Border(
          top: BorderSide(color: _CockpitTheme.hairline, width: 0.8),
        ),
      ),
      textAlign: WrapAlignment.start,
    );

    final segments = _splitAnswerSegments(text, streaming: streaming);
    final children = <Widget>[];
    for (var i = 0; i < segments.length; i++) {
      final seg = segments[i];
      if (seg is _TextSegment && seg.text.isNotEmpty) {
        children.add(
          MarkdownBody(
            data: _normalizeMarkdown(seg.text),
            selectable: true,
            styleSheet: ss,
            shrinkWrap: true,
            softLineBreak: true,
            onTapLink: (text, href, _) async {
              // Agent 生成的 excel 下载链接、minio URL、外部页面都点这里
              // agent 会以 markdown link 语法 [下载 XX](https://...) 输出，用户点开浏览器下载
              if (href == null || href.trim().isEmpty) return;
              final uri = Uri.tryParse(href.trim());
              if (uri == null) return;
              await launchUrl(uri, mode: LaunchMode.externalApplication);
            },
          ),
        );
      } else if (seg is _ChartSegment) {
        if (_chartSpecIsEmpty(seg.json)) continue;
        children.add(
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: _ChartCard(spec: seg.json),
          ),
        );
      } else if (seg is _FileSegment) {
        children.add(
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: _FileDownloadCard(
              filename: seg.filename,
              url: seg.url,
            ),
          ),
        );
      }
    }
    if (streaming) {
      children.add(
        const Padding(
          padding: EdgeInsets.only(top: 2),
          child: _BlinkingCursor(),
        ),
      );
    }
    if (children.isEmpty) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: children,
    );
  }
}

// -------------------- answer segments (text + chart) --------------------

abstract class _AnswerSegment {
  const _AnswerSegment();
}

class _TextSegment extends _AnswerSegment {
  final String text;
  const _TextSegment(this.text);
}

class _ChartSegment extends _AnswerSegment {
  final Map<String, dynamic> json;
  const _ChartSegment(this.json);
}

class _FileSegment extends _AnswerSegment {
  final String filename;
  final String url;
  const _FileSegment(this.filename, this.url);
}

// 匹配 markdown 文件下载链接。3 种命中方式：
//   1. anchor 文本里带 .xlsx/.pdf 等后缀（例："[下载 xxx.xlsx](url)"）
//   2. URL 末尾是这些后缀（"[下载](https://.../f.xlsx)"）
//   3. URL query 里带 `filename=xxx.xlsx`（"[点击下载](https://.../download/uuid?filename=xxx.xlsx)"）
final RegExp _kFileLink = RegExp(
  r'\[([^\]]*?(?:\.(?:xlsx|xls|csv|pdf|docx|doc|zip|txt))[^\]]*)\]\((https?://[^\)\s]+)\)|'
  r'\[([^\]]*)\]\((https?://[^\)\s]+?\.(?:xlsx|xls|csv|pdf|docx|doc|zip|txt))(?:\?[^\)\s]*)?\)|'
  r'\[([^\]]*)\]\((https?://[^\)\s]*[?&]filename=[^&\)\s]+\.(?:xlsx|xls|csv|pdf|docx|doc|zip|txt)[^\)\s]*)\)',
  caseSensitive: false,
);
// 从 anchor 文本里抓文件名（去掉"下载 / 📦 / 空格"等修饰）
final RegExp _kFilenamePattern = RegExp(
  r'[\w一-龥_\-]+?\.(?:xlsx|xls|csv|pdf|docx|doc|zip|txt)',
  caseSensitive: false,
);

class _FileDownloadCard extends StatefulWidget {
  final String filename;
  final String url;
  const _FileDownloadCard({required this.filename, required this.url});

  @override
  State<_FileDownloadCard> createState() => _FileDownloadCardState();
}

class _FileDownloadCardState extends State<_FileDownloadCard> {
  double? _progress;

  Future<void> _openInApp() async {
    setState(() => _progress = 0);
    final res = await FileDownloadService.downloadAndOpen(
      url: widget.url,
      filename: widget.filename,
      onProgress: (p) {
        if (!mounted) return;
        setState(() => _progress = p);
      },
    );
    if (!mounted) return;
    setState(() => _progress = null);
    if (res.type != ResultType.done && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('打开失败: ${res.message}'),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final ext = widget.filename.contains('.')
        ? widget.filename.split('.').last.toLowerCase()
        : '';
    final (icon, color) = switch (ext) {
      'xlsx' || 'xls' || 'csv' => (Icons.grid_on_rounded, const Color(0xFF15A47A)),
      'pdf' => (Icons.picture_as_pdf_rounded, const Color(0xFFCE3B1F)),
      'docx' || 'doc' => (Icons.description_rounded, const Color(0xFF2B65D9)),
      'zip' => (Icons.folder_zip_rounded, const Color(0xFFB25E09)),
      _ => (Icons.insert_drive_file_rounded, const Color(0xFF6E6E76)),
    };
    final isBusy = _progress != null;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: isBusy ? null : _openInApp,
        onLongPress: () async {
          // 长按仍支持跳浏览器（比如需要复制链接给别人）
          final uri = Uri.tryParse(widget.url);
          if (uri == null) return;
          await launchUrl(uri, mode: LaunchMode.externalApplication);
        },
        child: Container(
          padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xFFEEEEEE)),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 42,
                    height: 42,
                    decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(icon, color: color, size: 22),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.filename,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF0E0E10),
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          isBusy ? '下载中…' : '点击下载并打开 · 长按跳浏览器',
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.black.withValues(alpha: 0.5),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    isBusy ? Icons.downloading_rounded : Icons.download_rounded,
                    color: color,
                    size: 22,
                  ),
                ],
              ),
              if (isBusy) ...[
                const SizedBox(height: 10),
                ClipRRect(
                  borderRadius: BorderRadius.circular(2),
                  child: LinearProgressIndicator(
                    value: _progress,
                    minHeight: 2.5,
                    backgroundColor: const Color(0xFFECECE8),
                    valueColor: AlwaysStoppedAnimation(color),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

// Match a fenced chart block. Tolerate 3+ backticks (or ~~~), whitespace after
// the language tag, and missing newlines at either boundary. The `?` on the
// inner capture makes the match non-greedy so multiple charts in one answer
// each get their own segment.
final RegExp _kChartFence = RegExp(
  r'(?:`{3,}|~{3,})\s*chart\s*\r?\n?([\s\S]*?)\r?\n?(?:`{3,}|~{3,})',
  multiLine: true,
);

// 在给定文本里再按文件链接切分，产出 [text, file, text, file, ...] 混合 segment
Iterable<_AnswerSegment> _splitFileSegments(String source) sync* {
  var lastEnd = 0;
  for (final m in _kFileLink.allMatches(source)) {
    if (m.start > lastEnd) yield _TextSegment(source.substring(lastEnd, m.start));
    // 3 组捕获分支：anchor 带后缀 / URL 带后缀 / URL query filename= 带后缀
    final anchor = m.group(1) ?? m.group(3) ?? m.group(5) ?? '';
    final url = m.group(2) ?? m.group(4) ?? m.group(6) ?? '';
    // 优先从 anchor 文字里抓文件名；否则从 URL query filename 抓；再否则从 URL 路径尾抓
    String filename = 'download';
    final fromAnchor = _kFilenamePattern.firstMatch(anchor)?.group(0);
    if (fromAnchor != null && fromAnchor.isNotEmpty) {
      filename = fromAnchor;
    } else {
      final uri = Uri.tryParse(url);
      final queryName = uri?.queryParameters['filename'];
      if (queryName != null && queryName.isNotEmpty) {
        filename = queryName;
      } else {
        final rawName = uri?.pathSegments.lastOrNull ?? '';
        if (rawName.contains('.')) {
          filename = Uri.decodeComponent(rawName);
        }
      }
    }
    yield _FileSegment(filename, url);
    lastEnd = m.end;
  }
  if (lastEnd < source.length) yield _TextSegment(source.substring(lastEnd));
}

List<_AnswerSegment> _splitAnswerSegments(
  String source, {
  required bool streaming,
}) {
  final segments = <_AnswerSegment>[];
  var lastEnd = 0;
  void addText(String s) {
    // 文本片段里再扫文件链接，把 [xxx.xlsx](url) 转成文件卡片
    for (final part in _splitFileSegments(s)) {
      if (part is _TextSegment && part.text.isEmpty) continue;
      segments.add(part);
    }
  }

  for (final match in _kChartFence.allMatches(source)) {
    if (match.start > lastEnd) {
      addText(source.substring(lastEnd, match.start));
    }
    final raw = (match.group(1) ?? '').trim();
    try {
      final decoded = json.decode(raw);
      if (decoded is Map<String, dynamic>) {
        segments.add(_ChartSegment(decoded));
      } else {
        addText(match.group(0) ?? '');
      }
    } catch (_) {
      addText(match.group(0) ?? '');
    }
    lastEnd = match.end;
  }
  if (lastEnd < source.length) {
    var tail = source.substring(lastEnd);
    // During streaming, if there's an unclosed ```chart, hide the raw JSON to
    // avoid flashing partial code — replace with a placeholder that reads
    // naturally.
    if (streaming) {
      final unclosed = tail.indexOf('```chart');
      if (unclosed >= 0) {
        final before = tail.substring(0, unclosed);
        addText(before);
        segments.add(const _TextSegment('_（正在生成图表…）_'));
        return segments;
      }
    }
    addText(tail);
  }
  return segments;
}

// -------------------- chart card --------------------

// Normalize model-emitted chart type names. LLMs freely swap synonyms.
String _normalizeChartType(String raw) {
  final t = raw.toLowerCase().trim();
  switch (t) {
    case 'pie':
    case 'donut':
    case 'doughnut':
    case 'circle':
    case 'ring':
      return 'pie';
    case 'bar':
    case 'column':
    case 'vbar':
    case 'vertical':
    case 'histogram':
    case 'ranking':
    case 'rank':
      return 'bar';
    case 'line':
    case 'curve':
    case 'trend':
    case 'timeseries':
    case 'time_series':
    case 'area':
      return 'line';
    case 'stat':
    case 'kpi':
    case 'number':
    case 'bignumber':
    case 'big_number':
    case 'metric':
      return 'stat';
    default:
      return t;
  }
}

// Return the datapoint list for pie/bar under any of the common key names.
List _readChartData(Map<String, dynamic> spec) {
  for (final key in const ['data', 'items', 'values', 'dataset', 'points']) {
    final v = spec[key];
    if (v is List && v.isNotEmpty) return v;
  }
  return const [];
}

// Return the list of series for a line chart, similarly forgiving.
List _readChartSeries(Map<String, dynamic> spec) {
  for (final key in const ['series', 'lines', 'datasets']) {
    final v = spec[key];
    if (v is List && v.isNotEmpty) return v;
  }
  return const [];
}

// Return true when a chart spec has no meaningful data to render — the LLM
// sometimes emits placeholder cards ("以下用不同图表拆解…" then empty). We swallow
// those silently rather than show an "无数据" filler card, which reads as broken.
bool _chartSpecIsEmpty(Map<String, dynamic> spec) {
  final rawType = (spec['type'] as String?) ?? '';
  final type = _normalizeChartType(rawType);
  switch (type) {
    case 'bar':
    case 'pie':
      return _readChartData(spec).isEmpty;
    case 'line':
      final series = _readChartSeries(spec);
      if (series.isEmpty) {
        // Some models put a single-series line under `data: [{x,y},...]`.
        final flat = _readChartData(spec);
        return flat.isEmpty;
      }
      for (final s in series) {
        if (s is Map) {
          final pts = s['points'] ?? s['data'] ?? s['values'];
          if (pts is List && pts.isNotEmpty) return false;
        }
      }
      return true;
    case 'stat':
      for (final k in const ['value', 'number', 'metric', 'kpi']) {
        final v = spec[k];
        if (v != null && v.toString().trim().isNotEmpty) return false;
      }
      return true;
    default:
      // Unknown type but has some data → still render (fall back to bar-ish).
      return _readChartData(spec).isEmpty && _readChartSeries(spec).isEmpty;
  }
}

class _ChartCard extends StatelessWidget {
  final Map<String, dynamic> spec;

  const _ChartCard({required this.spec});

  @override
  Widget build(BuildContext context) {
    if (_chartSpecIsEmpty(spec)) return const SizedBox.shrink();
    final rawType = (spec['type'] as String?) ?? '';
    final type = _normalizeChartType(rawType);
    final title = spec['title']?.toString();
    final Widget body;
    switch (type) {
      case 'bar':
        body = _BarChartView(spec: spec);
        break;
      case 'line':
        body = _LineChartView(spec: spec);
        break;
      case 'pie':
        body = _PieChartView(spec: spec);
        break;
      case 'stat':
        body = _StatCardView(spec: spec);
        break;
      default:
        // Unknown type but we detected data — fall back to bar so the boss
        // sees the numbers rather than silence.
        if (_readChartData(spec).isNotEmpty) {
          body = _BarChartView(spec: spec);
          break;
        }
        body = Padding(
          padding: const EdgeInsets.all(12),
          child: Text(
            '未识别的 chart 类型: $type',
            style: const TextStyle(color: _CockpitTheme.dim, fontSize: 12),
          ),
        );
    }
    return Container(
      decoration: BoxDecoration(
        color: _CockpitTheme.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: _CockpitTheme.hairline),
      ),
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title != null && title.trim().isNotEmpty) ...[
            Text(
              title,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.4,
                color: _CockpitTheme.dim,
              ),
            ),
            const SizedBox(height: 10),
          ],
          body,
        ],
      ),
    );
  }
}

Color _chartToneColor(String? tone) {
  switch (tone?.toLowerCase()) {
    case 'green':
      return _CockpitTheme.green;
    case 'red':
      return _CockpitTheme.red;
    case 'amber':
    case 'orange':
      return _CockpitTheme.amber;
    case 'blue':
    case 'indigo':
    case 'purple':
      return _CockpitTheme.accent;
    case 'gray':
    case 'slate':
      return _CockpitTheme.dim;
    default:
      return _CockpitTheme.ink;
  }
}

double _asDouble(dynamic value) {
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0;
  return 0;
}

// LLM emits chart datapoint labels under several key names — `label` (our
// canonical), `name` (echarts style), `key`, `x` (time series), `category`.
// Read whichever is present so we render whatever the model chose.
String _readLabel(Map item) {
  for (final k in const ['label', 'name', 'key', 'x', 'category', 'title']) {
    final v = item[k];
    if (v != null) {
      final s = v.toString().trim();
      if (s.isNotEmpty) return s;
    }
  }
  return '';
}

// Datapoint value across common conventions: `value` (our canonical), `y`,
// `count`, `total`, `amount`.
double _readValue(Map item) {
  for (final k in const ['value', 'y', 'count', 'total', 'amount']) {
    final v = item[k];
    if (v != null) return _asDouble(v);
  }
  return 0;
}

class _BarChartView extends StatelessWidget {
  final Map<String, dynamic> spec;

  const _BarChartView({required this.spec});

  @override
  Widget build(BuildContext context) {
    final rawData = _readChartData(spec);
    final data = rawData
        .whereType<Map>()
        .map((m) => Map<String, dynamic>.from(m))
        .toList();
    if (data.isEmpty) {
      return const Text(
        '（无数据）',
        style: TextStyle(color: _CockpitTheme.faint, fontSize: 12),
      );
    }
    // Show all bars; if they can't fit the phone width horizontally, let the
    // user swipe left/right. Stops the old top-8 truncation that silently
    // dropped data, and prevents fl_chart from cramming 20 bars into 320px.
    final display = data;
    final maxValue = display.fold<double>(
      0,
      (m, item) => math.max(m, _readValue(item)),
    );
    if (maxValue <= 0) {
      return const Text(
        '（数值全为 0）',
        style: TextStyle(color: _CockpitTheme.faint, fontSize: 12),
      );
    }
    final yMax = _niceCeil(maxValue);
    // Each bar (rod + spacing) needs ~44px to stay readable with a rotated
    // Chinese label underneath. Below that width labels start colliding.
    const double perBarWidth = 44;
    return LayoutBuilder(
      builder: (context, constraints) {
        final available = constraints.hasBoundedWidth
            ? constraints.maxWidth
            : 320.0;
        final desired = display.length * perBarWidth;
        final chartWidth = math.max(available, desired);
        final scrollable = desired > available;
        final child = SizedBox(
          width: chartWidth,
          height: 220,
          child: Padding(
            padding: const EdgeInsets.only(top: 8),
            child: BarChart(
          BarChartData(
            alignment: BarChartAlignment.spaceAround,
            maxY: yMax,
            minY: 0,
            groupsSpace: 12,
            barTouchData: BarTouchData(
              enabled: true,
              touchTooltipData: BarTouchTooltipData(
                tooltipPadding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 4,
                ),
                getTooltipColor: (_) => _CockpitTheme.ink,
                tooltipMargin: 6,
                getTooltipItem: (group, groupIdx, rod, rodIdx) {
                  final item = display[group.x];
                  return BarTooltipItem(
                    '${_readLabel(item)}\n${_formatNumber(rod.toY)}',
                    const TextStyle(
                      color: Colors.white,
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  );
                },
              ),
            ),
            gridData: FlGridData(
              show: true,
              drawVerticalLine: false,
              horizontalInterval: yMax / 4,
              getDrawingHorizontalLine: (_) => FlLine(
                color: _CockpitTheme.hairline,
                strokeWidth: 1,
                dashArray: const [4, 4],
              ),
            ),
            borderData: FlBorderData(show: false),
            titlesData: FlTitlesData(
              topTitles: const AxisTitles(
                sideTitles: SideTitles(showTitles: false),
              ),
              rightTitles: const AxisTitles(
                sideTitles: SideTitles(showTitles: false),
              ),
              leftTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 32,
                  interval: yMax / 4,
                  getTitlesWidget: (value, meta) {
                    if (value == 0) return const SizedBox.shrink();
                    return Text(
                      _formatCompact(value),
                      style: const TextStyle(
                        fontSize: 10,
                        color: _CockpitTheme.faint,
                        fontFeatures: [FontFeature.tabularFigures()],
                      ),
                    );
                  },
                ),
              ),
              bottomTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 44,
                  getTitlesWidget: (value, meta) {
                    final idx = value.toInt();
                    if (idx < 0 || idx >= display.length) {
                      return const SizedBox.shrink();
                    }
                    final label = _readLabel(display[idx]);
                    // Slightly rotate long labels so they don't overlap.
                    final rotate = display.length > 4 && label.length > 3;
                    final child = Text(
                      label.length > 5 ? '${label.substring(0, 5)}…' : label,
                      style: const TextStyle(
                        fontSize: 10.5,
                        color: _CockpitTheme.ink,
                        fontWeight: FontWeight.w600,
                      ),
                    );
                    return Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: rotate
                          ? RotatedBox(quarterTurns: 0, child: child)
                          : child,
                    );
                  },
                ),
              ),
            ),
            barGroups: [
              for (var i = 0; i < display.length; i++)
                BarChartGroupData(
                  x: i,
                  barRods: [
                    BarChartRodData(
                      toY: _readValue(display[i]).clamp(0, yMax),
                      width: display.length > 6 ? 14 : 22,
                      borderRadius: const BorderRadius.only(
                        topLeft: Radius.circular(6),
                        topRight: Radius.circular(6),
                      ),
                      gradient: LinearGradient(
                        begin: Alignment.bottomCenter,
                        end: Alignment.topCenter,
                        colors: [
                          _chartToneColor(display[i]['tone']?.toString()),
                          _chartToneColor(
                            display[i]['tone']?.toString(),
                          ).withValues(alpha: 0.7),
                        ],
                      ),
                      backDrawRodData: BackgroundBarChartRodData(
                        show: true,
                        toY: yMax,
                        color: _CockpitTheme.hairline.withValues(alpha: 0.35),
                      ),
                    ),
                  ],
                  showingTooltipIndicators: const [],
                ),
            ],
          ),
            ),
          ),
        );
        if (!scrollable) return child;
        return SizedBox(
          height: 220,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            physics: const BouncingScrollPhysics(),
            child: child,
          ),
        );
      },
    );
  }
}

// Round a number up to a "nice" scale-appropriate max value for the y-axis.
double _niceCeil(double value) {
  if (value <= 0) return 1;
  if (value <= 1) return 1;
  if (value <= 5) return 5;
  if (value <= 10) return 10;
  final magnitude = math
      .pow(10, value.floor().toString().length - 1)
      .toDouble();
  return (value / magnitude).ceil() * magnitude;
}

String _formatCompact(double v) {
  if (v.abs() >= 1000000) {
    return '${(v / 1000000).toStringAsFixed(1)}M';
  }
  if (v.abs() >= 1000) {
    return '${(v / 1000).toStringAsFixed(1)}k';
  }
  return _formatNumber(v);
}

String _formatNumber(double v) {
  if (v == v.roundToDouble()) return v.toStringAsFixed(0);
  return v.toStringAsFixed(1);
}

class _LineChartView extends StatelessWidget {
  final Map<String, dynamic> spec;

  const _LineChartView({required this.spec});

  @override
  Widget build(BuildContext context) {
    final seriesListRaw = _readChartSeries(spec);
    List<List<Map<String, dynamic>>> allPoints = [];
    if (seriesListRaw.isEmpty) {
      // Single-series shortcut: LLM may put points directly on data:[{x,y},…]
      final flat = _readChartData(spec)
          .whereType<Map>()
          .map((m) => Map<String, dynamic>.from(m))
          .toList();
      if (flat.isNotEmpty) allPoints = [flat];
    } else {
      for (final raw in seriesListRaw) {
        if (raw is! Map) continue;
        final s = Map<String, dynamic>.from(raw);
        final ptsRaw = s['points'] ?? s['data'] ?? s['values'];
        if (ptsRaw is! List) continue;
        allPoints.add(
          ptsRaw
              .whereType<Map>()
              .map((m) => Map<String, dynamic>.from(m))
              .toList(),
        );
      }
    }
    if (allPoints.isEmpty) {
      return const Text(
        '（无数据）',
        style: TextStyle(color: _CockpitTheme.faint, fontSize: 12),
      );
    }
    final seriesData = <List<FlSpot>>[];
    List<String> xLabels = const [];
    for (final points in allPoints) {
      final spots = <FlSpot>[];
      for (var i = 0; i < points.length; i++) {
        spots.add(FlSpot(i.toDouble(), _readValue(points[i])));
      }
      seriesData.add(spots);
      if (points.length > xLabels.length) {
        xLabels = points.map((p) => _readLabel(p)).toList();
      }
    }
    final palette = [
      _CockpitTheme.ink,
      _CockpitTheme.green,
      _CockpitTheme.accent,
    ];
    final maxPoints = allPoints.fold<int>(0, (m, p) => math.max(m, p.length));
    const double perPointWidth = 42;
    return LayoutBuilder(
      builder: (context, constraints) {
        final available = constraints.hasBoundedWidth
            ? constraints.maxWidth
            : 320.0;
        final desired = maxPoints * perPointWidth;
        final chartWidth = math.max(available, desired);
        final scrollable = desired > available;
        final chart = SizedBox(
          width: chartWidth,
          height: 180,
          child: LineChart(
        LineChartData(
          gridData: FlGridData(
            drawVerticalLine: false,
            horizontalInterval: null,
            getDrawingHorizontalLine: (_) =>
                FlLine(color: _CockpitTheme.hairline, strokeWidth: 1),
          ),
          titlesData: FlTitlesData(
            topTitles: const AxisTitles(
              sideTitles: SideTitles(showTitles: false),
            ),
            rightTitles: const AxisTitles(
              sideTitles: SideTitles(showTitles: false),
            ),
            leftTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 32,
                getTitlesWidget: (value, meta) => Text(
                  value.toStringAsFixed(0),
                  style: const TextStyle(
                    fontSize: 10,
                    color: _CockpitTheme.faint,
                  ),
                ),
              ),
            ),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 22,
                getTitlesWidget: (value, meta) {
                  final idx = value.toInt();
                  if (idx < 0 || idx >= xLabels.length) {
                    return const SizedBox.shrink();
                  }
                  // When we're scrollable there's room for every tick;
                  // when we're compact fall back to first/mid/last so
                  // labels don't collide.
                  if (!scrollable &&
                      xLabels.length > 4 &&
                      idx != 0 &&
                      idx != xLabels.length - 1 &&
                      idx != xLabels.length ~/ 2) {
                    return const SizedBox.shrink();
                  }
                  return Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      xLabels[idx],
                      style: const TextStyle(
                        fontSize: 10,
                        color: _CockpitTheme.faint,
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          borderData: FlBorderData(show: false),
          lineBarsData: [
            for (var i = 0; i < seriesData.length; i++)
              LineChartBarData(
                spots: seriesData[i],
                isCurved: true,
                curveSmoothness: 0.25,
                color: palette[i % palette.length],
                barWidth: 2,
                isStrokeCapRound: true,
                dotData: const FlDotData(show: false),
                belowBarData: BarAreaData(
                  show: i == 0,
                  color: palette[i % palette.length].withValues(alpha: 0.06),
                ),
              ),
          ],
        ),
          ),
        );
        if (!scrollable) return chart;
        return SizedBox(
          height: 180,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            physics: const BouncingScrollPhysics(),
            child: chart,
          ),
        );
      },
    );
  }
}

class _PieChartView extends StatelessWidget {
  final Map<String, dynamic> spec;

  const _PieChartView({required this.spec});

  @override
  Widget build(BuildContext context) {
    final data = _readChartData(spec)
        .whereType<Map>()
        .map((m) => Map<String, dynamic>.from(m))
        .toList();
    final total = data.fold<double>(
      0,
      (sum, item) => sum + _readValue(item),
    );
    if (data.isEmpty || total <= 0) {
      return const Text(
        '（无数据）',
        style: TextStyle(color: _CockpitTheme.faint, fontSize: 12),
      );
    }
    final palette = [
      _CockpitTheme.ink,
      _CockpitTheme.green,
      _CockpitTheme.accent,
      _CockpitTheme.amber,
      _CockpitTheme.red,
      _CockpitTheme.dim,
    ];
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        SizedBox(
          width: 120,
          height: 120,
          child: PieChart(
            PieChartData(
              sectionsSpace: 2,
              centerSpaceRadius: 32,
              startDegreeOffset: -90,
              sections: [
                for (var i = 0; i < data.length; i++)
                  PieChartSectionData(
                    value: _readValue(data[i]),
                    color: data[i]['tone'] != null
                        ? _chartToneColor(data[i]['tone']?.toString())
                        : palette[i % palette.length],
                    radius: 32,
                    showTitle: false,
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (var i = 0; i < data.length; i++)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 3),
                  child: Row(
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: data[i]['tone'] != null
                              ? _chartToneColor(data[i]['tone']?.toString())
                              : palette[i % palette.length],
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          _readLabel(data[i]),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 12,
                            color: _CockpitTheme.ink,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      Text(
                        '${((_readValue(data[i]) / total) * 100).toStringAsFixed(0)}%',
                        style: const TextStyle(
                          fontSize: 12,
                          color: _CockpitTheme.dim,
                          fontFeatures: [FontFeature.tabularFigures()],
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class _StatCardView extends StatelessWidget {
  final Map<String, dynamic> spec;

  const _StatCardView({required this.spec});

  @override
  Widget build(BuildContext context) {
    final valueRaw = spec['value'] ??
        spec['number'] ??
        spec['metric'] ??
        spec['kpi'];
    final value = valueRaw?.toString() ?? '--';
    final unit = spec['unit']?.toString();
    final delta = spec['delta']?.toString();
    final hint = spec['hint']?.toString();
    final tone = _chartToneColor(spec['tone']?.toString());
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  value,
                  style: TextStyle(
                    fontSize: 42,
                    height: 1,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -1,
                    color: tone,
                  ),
                ),
                if (unit != null && unit.isNotEmpty) ...[
                  const SizedBox(width: 4),
                  Text(
                    unit,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _CockpitTheme.dim,
                    ),
                  ),
                ],
              ],
            ),
            if (hint != null && hint.isNotEmpty) ...[
              const SizedBox(height: 4),
              Text(
                hint,
                style: const TextStyle(fontSize: 12, color: _CockpitTheme.dim),
              ),
            ],
          ],
        ),
        if (delta != null && delta.isNotEmpty) ...[
          const Spacer(),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
              color: tone.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              delta,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: tone,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _ErrorLine extends StatelessWidget {
  final String text;

  const _ErrorLine({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: SelectableText(
        text,
        style: const TextStyle(fontSize: 14, color: _CockpitTheme.red),
      ),
    );
  }
}

String? _prettyArgs(String? raw) {
  if (raw == null) return null;
  final trimmed = raw.trim();
  if (trimmed.isEmpty || trimmed == '{}' || trimmed == 'null') return null;
  return trimmed;
}

String _formatMillis(int millis) {
  if (millis < 1000) return '${millis}ms';
  return '${(millis / 1000).toStringAsFixed(1)}s';
}

// Salvage LLM markdown that violates the CommonMark blank-line rules. Real
// answers sometimes glue a heading onto a table row on the same line ("###核心
// 指标一览|指标|数值|") or forget the space after `###`. Slip in the missing
// newlines and spaces so flutter_markdown can actually parse them as heading
// + table.
String _normalizeMarkdown(String source) {
  var text = source;
  // Drop stray U+FFFD replacement chars (upstream token-boundary corruption).
  text = text.replaceAll('�', '');
  // `###核心` → `### 核心`
  text = text.replaceAllMapped(
    RegExp(r'^(#{1,6})([^\s#])', multiLine: true),
    (m) => '${m[1]} ${m[2]}',
  );
  // Heading glued to inline table on same line:
  // `### 核心指标一览|指标 |数值|` → newline before the pipe
  text = text.replaceAllMapped(
    RegExp(r'^(#{1,6}\s[^\n|]{1,80})\s*(\|)', multiLine: true),
    (m) => '${m[1]}\n\n${m[2]}',
  );
  // Blank line before a table (line starting with `|`) if the previous line
  // isn't already blank / another table row / a heading.
  final lines = text.split('\n');
  final out = <String>[];
  for (var i = 0; i < lines.length; i++) {
    final line = lines[i];
    final trimmed = line.trimLeft();
    if (trimmed.startsWith('|') && out.isNotEmpty) {
      final prev = out.last.trimLeft();
      final prevIsTable = prev.startsWith('|');
      final prevIsBlank = prev.isEmpty;
      if (!prevIsTable && !prevIsBlank) {
        out.add('');
      }
    }
    out.add(line);
    // Blank line AFTER a table when the next line looks like normal text.
    if (trimmed.startsWith('|') && i < lines.length - 1) {
      final nextTrimmed = lines[i + 1].trimLeft();
      final nextIsTable = nextTrimmed.startsWith('|');
      final nextIsBlank = nextTrimmed.isEmpty;
      if (!nextIsTable && !nextIsBlank) {
        out.add('');
      }
    }
  }
  return out.join('\n');
}

class _SpinnerDot extends StatelessWidget {
  final Color color;

  const _SpinnerDot({required this.color});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 12,
      height: 12,
      child: CircularProgressIndicator(strokeWidth: 1.6, color: color),
    );
  }
}

class _StatusPip extends StatelessWidget {
  final Color color;

  const _StatusPip({required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 9,
      height: 9,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
    );
  }
}

class _Shimmer extends StatefulWidget {
  final Widget child;

  const _Shimmer({required this.child});

  @override
  State<_Shimmer> createState() => _ShimmerState();
}

class _ShimmerState extends State<_Shimmer>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      child: widget.child,
      builder: (context, child) {
        final shift = _controller.value * 3 - 1.5;
        return ShaderMask(
          blendMode: BlendMode.srcATop,
          shaderCallback: (bounds) {
            return LinearGradient(
              begin: Alignment(shift - 1, 0),
              end: Alignment(shift + 1, 0),
              colors: [
                Colors.white.withValues(alpha: 0.16),
                Colors.white.withValues(alpha: 0.72),
                Colors.white.withValues(alpha: 0.16),
              ],
              stops: const [0.24, 0.5, 0.76],
            ).createShader(bounds);
          },
          child: child ?? const SizedBox.shrink(),
        );
      },
    );
  }
}

class _BlinkingCursor extends StatefulWidget {
  const _BlinkingCursor();

  @override
  State<_BlinkingCursor> createState() => _BlinkingCursorState();
}

class _BlinkingCursorState extends State<_BlinkingCursor>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 900),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final opacity = _controller.value < 0.5 ? 1.0 : 0.15;
        return Padding(
          padding: const EdgeInsets.only(left: 2, bottom: 4),
          child: Opacity(
            opacity: opacity,
            child: Container(width: 2, height: 16, color: _CockpitTheme.ink),
          ),
        );
      },
    );
  }
}

class _WaitingLine extends StatelessWidget {
  final String text;

  const _WaitingLine({required this.text});

  @override
  Widget build(BuildContext context) {
    final label = Text(
      text,
      style: const TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w700,
        color: _CockpitTheme.dim,
      ),
    );
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 9, 12, 9),
      decoration: BoxDecoration(
        color: _CockpitTheme.hairline.withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.auto_awesome_rounded,
            size: 14,
            color: _CockpitTheme.dim,
          ),
          const SizedBox(width: 8),
          Flexible(child: _Shimmer(child: label)),
          const SizedBox(width: 8),
          const _ThinkingDots(),
        ],
      ),
    );
  }
}

class _ThinkingDots extends StatefulWidget {
  const _ThinkingDots();

  @override
  State<_ThinkingDots> createState() => _ThinkingDotsState();
}

class _ThinkingDotsState extends State<_ThinkingDots>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1200),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return Row(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(3, (index) {
            final phase = (_controller.value * 3 - index).clamp(0.0, 1.0);
            final scale = 0.6 + 0.4 * math.sin(phase * math.pi);
            return Padding(
              padding: const EdgeInsets.only(right: 6),
              child: Transform.scale(
                scale: scale,
                child: Container(
                  width: 6,
                  height: 6,
                  decoration: const BoxDecoration(
                    color: _CockpitTheme.dim,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            );
          }),
        );
      },
    );
  }
}

// -------------------- MODELS --------------------

abstract class _Activity {
  const _Activity();
}

class _ReasoningActivity extends _Activity {
  String text = '';
  bool completed = false;
}

class _ToolActivity extends _Activity {
  final String? id;
  final String name;
  String? argsPreview;
  String? humanLabel;
  String? outputPreview;
  bool completed = false;
  bool failed = false;
  bool cancelled = false;
  String? error;
  int? durationMs;
  _ToolActivity({
    required this.id,
    required this.name,
    this.argsPreview,
    this.humanLabel,
  });
}

class _AnswerActivity extends _Activity {
  String text = '';
}

// Server emits a `chart` event after the `render_chart` tool_result with the
// full validated spec — treated as a first-class activity that gets rendered
// inline in the turn timeline as a chart card.
class _ChartActivity extends _Activity {
  final Map<String, dynamic> spec;
  _ChartActivity(this.spec);
}

class _ChatTurn {
  final String role;
  String text;
  bool isError;
  String? errorMessage;
  final List<_Activity> activities;

  _ChatTurn._({
    required this.role,
    required this.text,
    required this.isError,
    required this.errorMessage,
    required this.activities,
  });

  factory _ChatTurn.user(String text) => _ChatTurn._(
    role: 'user',
    text: text,
    isError: false,
    errorMessage: null,
    activities: const [],
  );

  factory _ChatTurn.assistant() => _ChatTurn._(
    role: 'assistant',
    text: '',
    isError: false,
    errorMessage: null,
    activities: [],
  );

  bool get hasStreamingTail {
    if (activities.isEmpty) return false;
    final last = activities.last;
    if (last is _AnswerActivity) return true;
    if (last is _ReasoningActivity) return !last.completed;
    return false;
  }

  _AnswerActivity get _answerTail {
    if (activities.isNotEmpty && activities.last is _AnswerActivity) {
      return activities.last as _AnswerActivity;
    }
    final answer = _AnswerActivity();
    activities.add(answer);
    return answer;
  }

  _ReasoningActivity get _reasoningTail {
    // If the last activity is already a reasoning block — whether streaming
    // or freshly completed — merge into it. Reopening a new card for every
    // burst of thinking between tool calls produces a wall of stacked
    // "思考过程" tiles. Reusing keeps the timeline compact and lets the
    // user still see the full thinking stream inside one collapsible card.
    if (activities.isNotEmpty && activities.last is _ReasoningActivity) {
      final r = activities.last as _ReasoningActivity;
      if (r.completed) {
        r.completed = false;
        if (r.text.isNotEmpty && !r.text.endsWith('\n')) r.text += '\n';
      }
      return r;
    }
    // Close a previous open reasoning if a non-reasoning event broke it up.
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) {
        item.completed = true;
      }
    }
    final r = _ReasoningActivity();
    activities.add(r);
    return r;
  }

  void appendText(String delta) {
    // Text arriving means any live reasoning segment is done.
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
    }
    _answerTail.text = _answerTail.text + delta;
    text = (activities.whereType<_AnswerActivity>().map(
      (a) => a.text,
    )).join('');
  }

  void replaceText(String value) {
    _answerTail.text = value;
    text = (activities.whereType<_AnswerActivity>().map(
      (a) => a.text,
    )).join('');
  }

  void finishText() {}

  void appendReasoning(String delta) {
    _reasoningTail.text = _reasoningTail.text + delta;
  }

  void finishReasoning() {
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
    }
  }

  void markIteration(int index) {}

  void addChart(Map<String, dynamic> spec) {
    // Close any live reasoning — model committed to rendering.
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
    }
    activities.add(_ChartActivity(spec));
  }

  void startTool({
    String? id,
    required String name,
    String? argsPreview,
    String? humanLabel,
  }) {
    // Close any live reasoning — model has decided to act.
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
    }
    activities.add(_ToolActivity(
      id: id,
      name: name,
      argsPreview: argsPreview,
      humanLabel: humanLabel,
    ));
  }

  void finishTool({
    String? id,
    String? name,
    String? outputPreview,
    String? humanLabel,
    int? durationMs,
  }) {
    final tool = _findTool(id: id, name: name);
    if (tool != null && !tool.completed) {
      tool.completed = true;
      tool.outputPreview = outputPreview ?? tool.outputPreview;
      tool.humanLabel = humanLabel ?? tool.humanLabel;
      tool.durationMs = durationMs ?? tool.durationMs;
    }
  }

  void failTool({
    String? id,
    String? name,
    String? error,
    String? humanLabel,
    int? durationMs,
  }) {
    final tool = _findTool(id: id, name: name);
    if (tool == null) return;
    tool.completed = true;
    tool.failed = true;
    tool.error = _cleanError(error ?? tool.error);
    tool.humanLabel = humanLabel ?? tool.humanLabel;
    tool.durationMs = durationMs ?? tool.durationMs;
  }

  _ToolActivity? _findTool({String? id, String? name}) {
    if (id != null) {
      for (final item in activities.reversed) {
        if (item is _ToolActivity && item.id == id) return item;
      }
    }
    if (name != null) {
      for (final item in activities.reversed) {
        if (item is _ToolActivity && !item.completed && item.name == name) {
          return item;
        }
      }
    }
    for (final item in activities.reversed) {
      if (item is _ToolActivity && !item.completed) return item;
    }
    return null;
  }

  void finalize() {
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
      if (item is _ToolActivity && !item.completed) {
        item.completed = true;
        item.failed = true;
        item.error = '这个工具没有返回结果，可以重试。';
      }
    }
    if (text.trim().isEmpty && !isError) {
      _answerTail.text = '小助理没有拿到可用结果，可以换个问法再试。';
      text = _answerTail.text;
    }
  }

  void markError(String message) {
    isError = true;
    final friendly = _cleanError(message);
    errorMessage = friendly;
    for (final item in activities) {
      if (item is _ToolActivity && !item.completed) {
        item.completed = true;
        item.failed = true;
        item.error = friendly;
      }
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
    }
    if (text.trim().isEmpty) {
      _answerTail.text = friendly;
      text = friendly;
    }
  }

  void markStopped(String message) {
    isError = false;
    errorMessage = null;
    for (final item in activities) {
      if (item is _ReasoningActivity && !item.completed) item.completed = true;
      if (item is _ToolActivity && !item.completed) {
        item.completed = true;
        item.cancelled = true;
        item.error = '已停止';
      }
    }
    if (text.trim().isEmpty) {
      _answerTail.text = message;
      text = message;
    } else if (!text.contains(message)) {
      appendText('\n\n$message');
    }
  }
}

const Map<String, String> _kToolLabels = {
  'get_request_context': '识别当前用户',
  'tool_search': '查找可用能力',
  'fetch_url': '阅读网页',
  'render_chart': '生成图表',
  'oa_read': '查询业务数据',
  'ordersys_read': '查询连图数据',
  'executive_cockpit_overview': '刷新驾驶舱',
  'executive_cockpit_chat': '生成经营总结',
};

const Map<String, String> _kOaPathLabels = {
  // 需求
  '/admin-api/business/work/requirement/page': '查询需求列表',
  '/admin-api/business/work/requirement/get': '查看需求详情',
  '/admin-api/business/work/requirement/children': '查询子需求',
  '/admin-api/business/work/requirement/overview': '汇总需求指标',
  '/admin-api/business/work/requirement/scope-options': '查询数据范围',
  '/admin-api/business/work/requirement/todo-approval-process-instance-ids':
      '查询待审批需求',
  '/admin-api/business/work/requirement/logs': '查看需求日志',
  '/admin-api/business/work/requirement/comment/list': '查看需求评论',
  '/admin-api/business/work/requirement/rate/list': '查看需求评分',
  // 待办 / 驾驶舱 / 日历
  '/admin-api/business/todo/page': '查询待办事项',
  '/admin-api/business/todo/get': '查看待办详情',
  '/admin-api/business/executive-cockpit/overview': '刷新经营指标',
  '/admin-api/business/work/calendar/events': '查询工作日历',
  // 系统只读
  '/admin-api/system/user/list-all-simple': '查询员工列表',
  '/admin-api/system/user/simple-list': '查询员工列表',
  '/admin-api/system/user/simple-list-by-tenants': '按公司查员工',
  '/admin-api/system/dept/list-all-simple': '查询部门列表',
  '/admin-api/system/dept/simple-list': '查询部门列表',
  '/admin-api/system/dict-data/list-all-simple': '查询字典',
  '/admin-api/system/dict-data/simple-list': '查询字典',
  // 员工档案
  '/admin-api/hr/employee/current': '查看我的档案',
  '/admin-api/hr/employee/get': '查看员工档案',
  '/admin-api/hr/employee/page': '查询花名册',
  '/admin-api/hr/employee/statistics': '员工总人数统计',
  '/admin-api/hr/employee/statistics-trend': '员工月度趋势',
  '/admin-api/hr/employee/overview': '员工总览',
  '/admin-api/hr/employee/workspace-overview': '员工工作台聚合',
  '/admin-api/hr/employee/performance/page': '查询绩效工作台',
  '/admin-api/hr/employee/performance/stats': '绩效统计',
  '/admin-api/hr/self-service/home': '查看自助首页',
  '/admin-api/hr/self-service/application/page': '查询我的申请',
  '/admin-api/hr/self-service/quick-action/list': '查询快捷入口',
  '/admin-api/hr/manager-self-service/home': '查看经理自助',
  '/admin-api/hr/job-level/options': '查询职级',
  '/admin-api/hr/job-level/list': '查询职级',
  '/admin-api/hr/sequence/tree': '查询职级序列',
  '/admin-api/hr/sequence/options': '查询职级序列',
  // 考勤
  '/admin-api/hr/attendance/my-today': '我今日打卡',
  '/admin-api/hr/attendance/my-month': '我月度考勤',
  '/admin-api/hr/attendance/my-page': '我的考勤记录',
  '/admin-api/hr/attendance/page': '查询全员打卡',
  '/admin-api/hr/attendance/summary': '考勤汇总',
  '/admin-api/hr/attendance/workbench': '考勤看板',
  '/admin-api/hr/attendance/daily-result/page': '查询每日考勤',
  '/admin-api/hr/attendance/daily-result/summary': '每日考勤汇总',
  '/admin-api/hr/attendance/exception/page': '查询考勤异常',
  '/admin-api/hr/attendance/exception/summary': '考勤异常汇总',
  '/admin-api/hr/attendance/correction/page': '查询补卡申请',
  '/admin-api/hr/attendance/overtime/page': '查询加班申请',
  '/admin-api/hr/employee/attendance-stat': '员工月度考勤',
  '/admin-api/hr/administrative/leave/page': '查询请假申请',
  '/admin-api/hr/administrative/leave/my-page': '查询我的请假',
  '/admin-api/hr/administrative/leave/get': '查看请假详情',
  '/admin-api/hr/leave/type/list': '查询假期类型',
  '/admin-api/hr/leave/balance/page': '查询假期余额',
  '/admin-api/hr/leave/balance/my': '我的假期余额',
  // 问卷
  '/admin-api/hr/questionnaire/page': '查询问卷模板',
  '/admin-api/hr/questionnaire/get': '查看问卷详情',
  '/admin-api/hr/questionnaire-publish/page': '查询问卷发布',
  '/admin-api/hr/questionnaire-publish/list': '查询问卷发布',
  '/admin-api/hr/questionnaire-publish/my-list': '我发起的问卷',
  '/admin-api/hr/questionnaire-publish/get': '查看发布详情',
  '/admin-api/hr/questionnaire-publish/batch-list': '查询发布批次',
  '/admin-api/hr/questionnaire-assignment/page': '查询问卷分配',
  '/admin-api/hr/questionnaire-assignment/my-page': '查询我要填的问卷',
  '/admin-api/hr/questionnaire-assignment/get': '查看问卷分配',
  '/admin-api/hr/questionnaire-result/list': '查询问卷结果',
  '/admin-api/hr/questionnaire-result/item-stats': '问卷题目统计',
  // 考试
  '/admin-api/hr/exam/page': '查询考试列表',
  '/admin-api/hr/exam/manage-page': '查询考试管理',
  '/admin-api/hr/exam/get': '查看考试详情',
  '/admin-api/hr/exam-publish/page': '查询考试发布',
  '/admin-api/hr/exam-publish/get': '查看考试发布',
  '/admin-api/hr/exam-attempt/my-list': '查询我的成绩',
  '/admin-api/hr/exam-result/list': '查询考试排名',
};

const Map<String, String> _kOrdersysPathLabels = {
  '/admin/ttask/list': '查询连图任务看板',
  '/workbench/todo/list': '查询连图待办事项',
  '/admin/assess/chart/list': '查询连图考核表',
  '/work/order/list': '查询我的工单',
  '/work/order/review/list': '查询审查分配工单',
  '/work/timeout/list': '查询超时工单',
  '/admin/sysProblem/list': '查询连图社区帖子',
  '/employees/community/tag/summary/list': '查询社区汇总',
  '/work/rest/list': '查询休息申请',
  '/exam/question/list': '查询试题管理',
};

String _toolLabel(String name, {String? argsPreview}) {
  if ((name == 'oa_read' || name == 'ordersys_read') &&
      argsPreview != null &&
      argsPreview.trim().isNotEmpty &&
      argsPreview.trim() != '{}') {
    try {
      final args = json.decode(argsPreview);
      if (args is Map) {
        final path = args['path']?.toString();
        if (path != null) {
          final labels = name == 'ordersys_read'
              ? _kOrdersysPathLabels
              : _kOaPathLabels;
          final label = labels[path.split('?').first];
          if (label != null) return label;
        }
      }
    } catch (_) {
      // fall through to default label
    }
  }
  return _kToolLabels[name] ?? name;
}

String _toolStatusLabel(_ToolActivity activity) {
  // Prefer the server-rendered humanLabel — it knows the endpoint's purpose
  // sentence and the exact args (e.g. "查询：售后【赔付/损失金额】月度统计"),
  // which the client cannot reconstruct on its own. Suffix by state so the
  // reader can distinguish "in flight" from "done" without watching the spinner.
  final human = activity.humanLabel?.trim();
  if (human != null && human.isNotEmpty) {
    if (activity.cancelled) return '$human · 已停止';
    if (activity.failed) return '$human · 出错，点开看原因';
    if (activity.completed) return '$human · 完成';
    return '$human 中…';
  }
  final label = _toolLabel(activity.name, argsPreview: activity.argsPreview);
  final target = _toolTargetName(label);
  if (activity.cancelled) return '$target 已停止';
  if (activity.failed) return '$target 没查成，点开看原因';
  if (activity.completed) return _toolDoneLabel(activity.name, target);
  return _toolRunningLabel(activity.name, label);
}

String _toolRunningLabel(String name, String label) {
  final target = _toolTargetName(label);
  switch (name) {
    case 'api_search':
      return '搜索接口中…';
    case 'api_call':
      return '查询数据中…';
    case 'render_chart':
      return '正在画图…';
    case 'fetch_url':
      return '读一下网页资料';
    case 'get_request_context':
      return '确认一下当前用户';
    // Legacy tool names retained so old sessions replayed from history render
    // without falling into the "正在处理这一步" generic fallback.
    case 'tool_search':
      return '先找一下该用哪个能力';
    case 'oa_read':
      return '去 OA 看一眼：$target';
    case 'ordersys_read':
      return '去连图看一眼：${_ordersysTargetName(target)}';
    case 'executive_cockpit_overview':
      return '刷新一下驾驶舱数据';
    case 'executive_cockpit_chat':
      return '整理一下经营总结';
    default:
      const fallbacks = ['正在处理这一步', '正在把线索串起来', '找一条合适的查询路线'];
      final index =
          name.codeUnits.fold<int>(0, (sum, code) => sum + code) %
          fallbacks.length;
      return fallbacks[index];
  }
}

String _toolDoneLabel(String name, String target) {
  switch (name) {
    case 'api_search':
      return '接口找到了';
    case 'api_call':
      return '数据拿到了';
    case 'render_chart':
      return '图表画好了';
    case 'fetch_url':
      return '网页资料读完了';
    case 'get_request_context':
      return '当前用户确认好了';
    case 'tool_search':
      return '能力路线找到了';
    case 'oa_read':
      return '$target 拿到了';
    case 'ordersys_read':
      return '连图${_ordersysTargetName(target)}拿到了';
    case 'executive_cockpit_overview':
      return '驾驶舱数据刷新好了';
    case 'executive_cockpit_chat':
      return '经营总结整理好了';
    default:
      return '$target 处理好了';
  }
}

String _toolTargetName(String label) {
  final target = label.replaceFirst(RegExp(r'^(查询|查看|刷新|生成|阅读)'), '');
  return target.trim().isEmpty ? label : target.trim();
}

String _ordersysTargetName(String target) {
  final text = target.replaceFirst(RegExp(r'^连图'), '').trim();
  return text.isEmpty ? target : text;
}

// -------------------- helpers --------------------

Color _toneColor(String tone) {
  switch (tone) {
    case 'green':
      return _CockpitTheme.green;
    case 'red':
      return _CockpitTheme.red;
    case 'amber':
    case 'orange':
      return _CockpitTheme.amber;
    case 'cyan':
    case 'indigo':
    case 'purple':
      return _CockpitTheme.accent;
    case 'slate':
    case 'gray':
      return _CockpitTheme.dim;
    default:
      return _CockpitTheme.ink;
  }
}

String _formatDate(DateTime value) {
  return '${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';
}

String _formatMinute(DateTime value) {
  return '${_formatDate(value)} ${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';
}

String _monthDay(String date) {
  final parts = date.split('-');
  if (parts.length >= 3) {
    return '${parts[1]}/${parts[2]}';
  }
  return date;
}

String _joinText(List<String?> values) {
  return values
      .map((value) => value?.trim() ?? '')
      .where((value) => value.isNotEmpty)
      .join(' · ');
}

String _newConversationId() {
  return 'cockpit-${DateTime.now().millisecondsSinceEpoch}';
}

String _cleanError(Object? error) {
  var message = (error?.toString() ?? '').trim();
  message = message.replaceFirst(RegExp(r'^Exception:\s*'), '');
  message = message.replaceFirst(RegExp(r'^ApiException:\s*'), '');
  message = message.replaceAll(RegExp(r', uri=.*$'), '').trim();
  if (message.isEmpty || message == 'null') {
    return '小助理开小差了，刚才没有拿到结果，可以重试。';
  }
  final lower = message.toLowerCase();
  if (lower.contains('401') ||
      lower.contains('unauthorized') ||
      message.contains('登录') ||
      message.contains('未登录')) {
    return '登录状态过期，请重新登录后再试。';
  }
  if (lower.contains('timeout') ||
      lower.contains('timed out') ||
      message.contains('超时')) {
    return '接口有点慢，小助理先停下来了。可以缩小范围后重试。';
  }
  if (lower.contains('clientexception') ||
      lower.contains('connection abort') ||
      lower.contains('connection reset') ||
      lower.contains('connection closed') ||
      lower.contains('socketexception') ||
      lower.contains('connection refused') ||
      lower.contains('network is unreachable')) {
    return '小助理开小差了，刚才连接断了，可以重试。';
  }
  if (RegExp(r'\b50[0-9]\b').hasMatch(lower) ||
      lower.contains('bad gateway') ||
      lower.contains('service unavailable') ||
      lower.contains('gateway timeout')) {
    return '网关或服务刚才不稳定，小助理没拿到结果，可以重试。';
  }
  if (lower.contains('format') || message.contains('格式')) {
    return '接口返回格式不太对，小助理没法继续解析，可以重试。';
  }
  return message;
}

// -------------------- CONVERSATION DRAWER --------------------

class ConversationDrawer extends StatefulWidget {
  final ValueChanged<String> onSelect;

  const ConversationDrawer({required this.onSelect});

  @override
  State<ConversationDrawer> createState() => ConversationDrawerState();
}

class ConversationDrawerState extends State<ConversationDrawer> {
  Future<List<AgentConversationSummary>>? _future;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<List<AgentConversationSummary>> _load() {
    return AgentConversationService.list(scene: 'cockpit', limit: 80);
  }

  Future<void> _refresh() async {
    setState(() => _future = _load());
    await _future;
  }

  Future<void> _rename(AgentConversationSummary item) async {
    final controller = TextEditingController(text: item.displayTitle);
    final newTitle = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('重命名会话'),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLength: 40,
          decoration: const InputDecoration(hintText: '输入新的标题'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(controller.text.trim()),
            child: const Text('保存'),
          ),
        ],
      ),
    );
    if (newTitle == null || newTitle.isEmpty) return;
    try {
      await AgentConversationService.rename(item.conversationId, newTitle);
      await _refresh();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('重命名失败：$e')));
      }
    }
  }

  Future<void> _delete(AgentConversationSummary item) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除会话'),
        content: Text('确认删除「${item.displayTitle}」？该操作不可恢复。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.redAccent),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await AgentConversationService.delete(item.conversationId);
      await _refresh();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('删除失败：$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Drawer(
      backgroundColor: _CockpitTheme.canvas,
      width: math.min(MediaQuery.sizeOf(context).width * 0.86, 360),
      child: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            const Divider(height: 1, color: _CockpitTheme.hairline),
            Expanded(child: _buildList()),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 14, 12, 14),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              '历史会话',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: _CockpitTheme.ink,
              ),
            ),
          ),
          IconButton(
            tooltip: '刷新',
            icon: const Icon(
              Icons.refresh_rounded,
              size: 20,
              color: _CockpitTheme.dim,
            ),
            onPressed: _refresh,
          ),
        ],
      ),
    );
  }

  Widget _buildList() {
    return FutureBuilder<List<AgentConversationSummary>>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(
            child: SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: _CockpitTheme.ink,
              ),
            ),
          );
        }
        if (snapshot.hasError) {
          return _buildError(snapshot.error);
        }
        final items = snapshot.data ?? const [];
        if (items.isEmpty) {
          return _buildEmpty();
        }
        return RefreshIndicator(
          onRefresh: _refresh,
          color: _CockpitTheme.ink,
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(vertical: 4),
            itemCount: items.length,
            separatorBuilder: (_, _) => const Divider(
              height: 1,
              indent: 20,
              endIndent: 20,
              color: _CockpitTheme.hairline,
            ),
            itemBuilder: (context, index) {
              final item = items[index];
              return _ConversationTile(
                item: item,
                onTap: () => widget.onSelect(item.conversationId),
                onRename: () => _rename(item),
                onDelete: () => _delete(item),
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildEmpty() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 60, 24, 24),
      children: [
        const Icon(Icons.forum_outlined, size: 40, color: _CockpitTheme.faint),
        const SizedBox(height: 12),
        const Text(
          '还没有历史会话',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: _CockpitTheme.ink,
          ),
        ),
        const SizedBox(height: 6),
        const Text(
          '开始对话后，历史会自动保存到这里。',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 13, color: _CockpitTheme.dim),
        ),
      ],
    );
  }

  Widget _buildError(Object? error) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 60, 24, 24),
      children: [
        const Icon(
          Icons.wifi_off_rounded,
          size: 40,
          color: _CockpitTheme.faint,
        ),
        const SizedBox(height: 12),
        Text(
          '加载失败：$error',
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 13, color: _CockpitTheme.dim),
        ),
        const SizedBox(height: 20),
        Center(
          child: OutlinedButton(
            onPressed: _refresh,
            style: OutlinedButton.styleFrom(
              foregroundColor: _CockpitTheme.ink,
              side: const BorderSide(color: _CockpitTheme.hairline),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(999),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            ),
            child: const Text('重试'),
          ),
        ),
      ],
    );
  }
}

class _ConversationTile extends StatelessWidget {
  final AgentConversationSummary item;
  final VoidCallback onTap;
  final VoidCallback onRename;
  final VoidCallback onDelete;

  const _ConversationTile({
    required this.item,
    required this.onTap,
    required this.onRename,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 8, 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    item.displayTitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: _CockpitTheme.ink,
                    ),
                  ),
                  if (item.lastMessage.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      item.lastMessage,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12,
                        color: _CockpitTheme.dim,
                        height: 1.4,
                      ),
                    ),
                  ],
                  const SizedBox(height: 6),
                  Text(
                    '${_formatRelative(item.updatedAt)} · ${item.messageCount} 条',

                    style: const TextStyle(
                      fontSize: 11,
                      color: _CockpitTheme.faint,
                    ),
                  ),
                ],
              ),
            ),
            PopupMenuButton<String>(
              tooltip: '更多',
              icon: const Icon(
                Icons.more_horiz_rounded,
                size: 20,
                color: _CockpitTheme.dim,
              ),
              onSelected: (v) {
                if (v == 'rename') onRename();
                if (v == 'delete') onDelete();
              },
              itemBuilder: (_) => const [
                PopupMenuItem(value: 'rename', child: Text('重命名')),
                PopupMenuItem(value: 'delete', child: Text('删除')),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatRelative(DateTime? time) {
    if (time == null) return '';
    final now = DateTime.now();
    final diff = now.difference(time);
    if (diff.inSeconds < 60) return '刚刚';
    if (diff.inMinutes < 60) return '${diff.inMinutes} 分钟前';
    if (diff.inHours < 24) return '${diff.inHours} 小时前';
    if (diff.inDays < 7) return '${diff.inDays} 天前';
    return '${time.year}-${_pad(time.month)}-${_pad(time.day)}';
  }

  String _pad(int n) => n < 10 ? '0$n' : '$n';
}
