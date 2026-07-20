// 运维管理页 — 第一屏 Tab 是 canal 同步监控
// 只在 workbench_page 里把入口置于 admin (userId==1111) 可见就够；这里再做二次保护
// 拒 403 就明确提示，避免暴露内部结构。
import 'dart:async';

import 'package:flutter/material.dart';

import '../services/ops_service.dart';

class OpsMonitorPage extends StatefulWidget {
  const OpsMonitorPage({super.key});

  @override
  State<OpsMonitorPage> createState() => _OpsMonitorPageState();
}

class _OpsMonitorPageState extends State<OpsMonitorPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tab = TabController(length: 4, vsync: this);

  @override
  void dispose() {
    _tab.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F7FB),
      appBar: AppBar(
        title: const Text('运维管理'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        bottom: TabBar(
          controller: _tab,
          labelColor: const Color(0xFF2E5FF2),
          unselectedLabelColor: Colors.black54,
          indicatorColor: const Color(0xFF2E5FF2),
          isScrollable: true,
          tabs: const [
            Tab(text: '概览'),
            Tab(text: '表统计'),
            Tab(text: 'DDL 变更'),
            Tab(text: '审计分析'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tab,
        children: const [
          _OverviewTab(),
          _SyncStatsTab(),
          _DdlAuditTab(),
          _AuditTab(),
        ],
      ),
    );
  }
}

// ────────────────────────────────────────────────────────────────
// 概览 tab —— 实时压力大数字 + 组件存活 + 源库位点 + 24h 错误
// 每 3s poll /ops/canal-rates（后端算 rate），每 6s poll /ops/canal-status（存活）
// ────────────────────────────────────────────────────────────────
class _OverviewTab extends StatefulWidget {
  const _OverviewTab();
  @override
  State<_OverviewTab> createState() => _OverviewTabState();
}

class _OverviewTabState extends State<_OverviewTab> {
  CanalStatus? _status;
  CanalRates? _rates;
  String? _errorStatus;
  String? _errorRates;
  bool _loading = false;
  Timer? _tRates;
  Timer? _tStatus;

  @override
  void initState() {
    super.initState();
    _fetchAll();
    _tRates = Timer.periodic(const Duration(seconds: 3), (_) => _fetchRates());
    _tStatus = Timer.periodic(const Duration(seconds: 6), (_) => _fetchStatus());
  }

  @override
  void dispose() {
    _tRates?.cancel();
    _tStatus?.cancel();
    super.dispose();
  }

  Future<void> _fetchAll() async {
    setState(() => _loading = true);
    await Future.wait([_fetchRates(), _fetchStatus()]);
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _fetchRates() async {
    if (!mounted) return;
    try {
      final r = await OpsService.getCanalRates();
      if (!mounted) return;
      setState(() { _rates = r; _errorRates = null; });
    } catch (e) {
      if (!mounted) return;
      setState(() => _errorRates = e.toString());
    }
  }

  Future<void> _fetchStatus() async {
    if (!mounted) return;
    try {
      final s = await OpsService.getCanalStatus();
      if (!mounted) return;
      setState(() { _status = s; _errorStatus = null; });
    } catch (e) {
      if (!mounted) return;
      setState(() => _errorStatus = e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _fetchAll,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 24),
        children: [
          _RatesHeadlineCard(rates: _rates, loading: _loading),
          const SizedBox(height: 12),
          _PressureCard(rates: _rates),
          const SizedBox(height: 12),
          _SqlTypeBreakdownCard(rates: _rates),
          const SizedBox(height: 12),
          _StatusRow(status: _status, loading: _loading),
          const SizedBox(height: 12),
          _MasterPositionCard(master: _status?.cdbMaster),
          const SizedBox(height: 12),
          _ErrorsCard(errors: _status?.recentErrors ?? const []),
          if (_errorStatus != null || _errorRates != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                '拉取失败: ${_errorRates ?? _errorStatus}',
                style: const TextStyle(color: Color(0xFFC62828)),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

// 顶部实时 TPS 大数字看板
class _RatesHeadlineCard extends StatelessWidget {
  final CanalRates? rates;
  final bool loading;
  const _RatesHeadlineCard({required this.rates, required this.loading});

  @override
  Widget build(BuildContext context) {
    final r = rates;
    final agg = r?.aggregate;
    final ok = r?.available == true;
    final firstPoll = ok && r?.elapsedSec == null;
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: const [BoxShadow(color: Color(0x0F000000), blurRadius: 8, offset: Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('当前压力', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
              const SizedBox(width: 8),
              _statusChip(r),
              const Spacer(),
              if (loading)
                const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2)),
            ],
          ),
          const SizedBox(height: 10),
          if (!ok)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                r?.reason ?? '正在采集…',
                style: const TextStyle(color: Colors.black45, fontSize: 12),
              ),
            )
          else if (firstPoll)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 8),
              child: Text('首次快照已采集，3 秒后出速率…', style: TextStyle(color: Colors.black45, fontSize: 12)),
            )
          else ...[
            Row(
              children: [
                Expanded(
                  child: _BigNum(
                    label: '总 TPS',
                    value: agg!.total.toStringAsFixed(1),
                    unit: '/s',
                    color: const Color(0xFF2E5FF2),
                  ),
                ),
                Expanded(
                  child: _BigNum(
                    label: '批/秒',
                    value: agg.batches.toStringAsFixed(1),
                    unit: '/s',
                    color: const Color(0xFF6B7280),
                  ),
                ),
                Expanded(
                  child: _BigNum(
                    label: '错误 TPS',
                    value: agg.err.toStringAsFixed(1),
                    unit: '/s',
                    color: agg.err > 0 ? const Color(0xFFC62828) : const Color(0xFF16A34A),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: _MiniTps(label: 'INS', value: agg.ins, color: const Color(0xFF16A34A))),
                Expanded(child: _MiniTps(label: 'UPD', value: agg.upd, color: const Color(0xFF2E5FF2))),
                Expanded(child: _MiniTps(label: 'DEL', value: agg.del, color: const Color(0xFFB25E09))),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _statusChip(CanalRates? r) {
    final up = r?.connected == true;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: up ? const Color(0xFFECFDF5) : const Color(0xFFFEF2F2),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        up ? '已连接' : '未连接',
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: up ? const Color(0xFF15A47A) : const Color(0xFFC62828),
        ),
      ),
    );
  }
}

class _BigNum extends StatelessWidget {
  final String label;
  final String value;
  final String? unit;
  final Color color;
  const _BigNum({required this.label, required this.value, this.unit, required this.color});
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 11, color: Color(0xFF6B7280))),
        const SizedBox(height: 3),
        RichText(
          text: TextSpan(
            style: DefaultTextStyle.of(context).style,
            children: [
              TextSpan(
                text: value,
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800, color: color),
              ),
              if (unit != null)
                TextSpan(
                  text: unit,
                  style: const TextStyle(fontSize: 12, color: Color(0xFF6B7280)),
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class _MiniTps extends StatelessWidget {
  final String label;
  final double value;
  final Color color;
  const _MiniTps({required this.label, required this.value, required this.color});
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(width: 6, height: 22, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2))),
        const SizedBox(width: 6),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontSize: 10.5, color: Color(0xFF6B7280))),
            Text('${value.toStringAsFixed(1)}/s',
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700)),
          ],
        ),
      ],
    );
  }
}

// binlog 延迟 + 累计吞吐 + 重连次数 + 平均批大小
class _PressureCard extends StatelessWidget {
  final CanalRates? rates;
  const _PressureCard({required this.rates});

  @override
  Widget build(BuildContext context) {
    final r = rates;
    final p = r?.pressure;
    final c = r?.cumulative;
    final lagWarn = (p?.lagBytes ?? 0) > 100 * 1024 * 1024; // >100MB
    return _Card(
      title: '压力指标',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _KV(
            'binlog 延迟',
            p?.lagWarn ?? p?.lagHuman ?? '—',
            valueColor: lagWarn ? const Color(0xFFC62828) : null,
          ),
          _KV('源库位点', p?.sourceBinlogFile != null
              ? '${p!.sourceBinlogFile}:${p.sourceBinlogPos}'
              : '—', mono: true),
          _KV('CDC 位点', p?.binlogFile != null
              ? '${p!.binlogFile}:${p.binlogPos}'
              : '—', mono: true),
          const Divider(height: 20),
          _KV('累计已同步', c != null ? _fmtNum(c.total) : '—'),
          _KV('累计批数', c != null ? _fmtNum(c.batches) : '—'),
          _KV('平均批大小', p?.avgBatchSize != null ? '${p!.avgBatchSize!.toStringAsFixed(1)} 行' : '—'),
          _KV('累计错误率',
              p != null ? '${p.errorRatePct.toStringAsFixed(2)}%' : '—',
              valueColor: (p?.errorRatePct ?? 0) > 0.1
                  ? const Color(0xFFC62828)
                  : const Color(0xFF16A34A)),
          _KV(
            '重连次数',
            p != null ? '${p.reconnectsTotal}' : '—',
            valueColor: (p?.reconnectsTotal ?? 0) > 0
                ? const Color(0xFFB25E09)
                : null,
          ),
          if (p?.startedAt != null)
            _KV('进程启动', _fmtTime(p!.startedAt!)),
        ],
      ),
    );
  }
}

// SQL 类型分布 —— 累计维度的 INS/UPD/DEL 占比
class _SqlTypeBreakdownCard extends StatelessWidget {
  final CanalRates? rates;
  const _SqlTypeBreakdownCard({required this.rates});

  @override
  Widget build(BuildContext context) {
    final c = rates?.cumulative;
    final total = c != null ? (c.ins + c.upd + c.del) : 0;
    if (total == 0) {
      return _Card(title: 'SQL 类型分布', child: const _EmptyText('暂无数据'));
    }
    final insPct = c!.ins / total;
    final updPct = c.upd / total;
    final delPct = c.del / total;
    return _Card(
      title: 'SQL 类型分布 (累计)',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: SizedBox(
              height: 12,
              child: Row(
                children: [
                  Expanded(flex: (insPct * 10000).toInt().clamp(0, 100000), child: Container(color: const Color(0xFF16A34A))),
                  Expanded(flex: (updPct * 10000).toInt().clamp(0, 100000), child: Container(color: const Color(0xFF2E5FF2))),
                  Expanded(flex: (delPct * 10000).toInt().clamp(0, 100000), child: Container(color: const Color(0xFFB25E09))),
                ],
              ),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(child: _legend('INS', c.ins, insPct, const Color(0xFF16A34A))),
              Expanded(child: _legend('UPD', c.upd, updPct, const Color(0xFF2E5FF2))),
              Expanded(child: _legend('DEL', c.del, delPct, const Color(0xFFB25E09))),
            ],
          ),
        ],
      ),
    );
  }

  Widget _legend(String label, int cnt, double pct, Color color) {
    return Row(
      children: [
        Container(width: 10, height: 10, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2))),
        const SizedBox(width: 6),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(fontSize: 11, color: Color(0xFF6B7280))),
              Text('${(pct * 100).toStringAsFixed(1)}%  · ${_fmtNum(cnt)}',
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
            ],
          ),
        ),
      ],
    );
  }
}

// ────────────────────────────────────────────────────────────────
// DDL 变更 tab —— 读 warehouse._canal_ddl_applied
// ────────────────────────────────────────────────────────────────
class _DdlAuditTab extends StatefulWidget {
  const _DdlAuditTab();
  @override
  State<_DdlAuditTab> createState() => _DdlAuditTabState();
}

class _DdlAuditTabState extends State<_DdlAuditTab> {
  DdlAuditPage? _page;
  List<DdlAuditItem> _items = const [];
  String? _error;
  bool _loading = false;
  int _pageNo = 1;
  int _windowHours = 168;
  String? _statusFilter;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    setState(() { _loading = true; _pageNo = 1; });
    try {
      final p = await OpsService.getDdlAudit(
        pageNo: 1,
        pageSize: 20,
        hours: _windowHours,
        status: _statusFilter,
      );
      if (!mounted) return;
      setState(() {
        _page = p;
        _items = p.list;
        _error = null;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() { _error = e.toString(); _loading = false; });
    }
  }

  Future<void> _loadMore() async {
    if (_page == null || !_page!.hasMore || _loading) return;
    setState(() => _loading = true);
    try {
      final next = _pageNo + 1;
      final p = await OpsService.getDdlAudit(
        pageNo: next,
        pageSize: 20,
        hours: _windowHours,
        status: _statusFilter,
      );
      if (!mounted) return;
      setState(() {
        _page = p;
        _items = [..._items, ...p.list];
        _pageNo = next;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 24),
        children: [
          _distributionCard(),
          const SizedBox(height: 12),
          _filterCard(),
          const SizedBox(height: 12),
          if (_page?.note != null) ...[
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFFBEB),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(_page!.note!, style: const TextStyle(color: Color(0xFF92400E), fontSize: 12)),
            ),
            const SizedBox(height: 12),
          ],
          if (_items.isEmpty && !_loading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 40),
              child: Center(
                child: Text('时间窗内无 DDL 变更',
                    style: TextStyle(color: Colors.black38)),
              ),
            ),
          for (final item in _items) ...[
            _DdlItemCard(item: item),
            const SizedBox(height: 8),
          ],
          if (_page?.hasMore == true) ...[
            const SizedBox(height: 8),
            Center(
              child: TextButton(
                onPressed: _loading ? null : _loadMore,
                child: _loading
                    ? const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2))
                    : Text('加载更多 (${_items.length}/${_page!.total})'),
              ),
            ),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text('拉取失败: $_error', style: const TextStyle(color: Color(0xFFC62828))),
            ),
          ],
        ],
      ),
    );
  }

  Widget _distributionCard() {
    final d = _page?.distribution;
    return _Card(
      title: '${_windowHours < 168 ? "${_windowHours}h" : "近 ${(_windowHours / 24).round()}d"} 变更概况',
      child: Row(
        children: [
          Expanded(child: _distCell('applied', d?.applied ?? 0, const Color(0xFF16A34A))),
          Expanded(child: _distCell('skipped', d?.skipped ?? 0, const Color(0xFF6B7280))),
          Expanded(child: _distCell('pending', d?.pending ?? 0, const Color(0xFFB25E09))),
          Expanded(child: _distCell('failed', d?.failed ?? 0, const Color(0xFFC62828))),
        ],
      ),
    );
  }

  Widget _distCell(String label, int cnt, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 11, color: Color(0xFF6B7280))),
        const SizedBox(height: 3),
        Text('$cnt', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: color)),
      ],
    );
  }

  Widget _filterCard() {
    return _Card(
      title: '过滤',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('时间窗口', style: TextStyle(fontSize: 11.5, color: Color(0xFF6B7280))),
          const SizedBox(height: 6),
          Wrap(
            spacing: 6,
            children: [
              for (final h in const [24, 72, 168, 720])
                ChoiceChip(
                  label: Text(h < 24 ? '${h}h' : '${(h / 24).round()}d'),
                  selected: _windowHours == h,
                  onSelected: (v) {
                    if (v) {
                      setState(() => _windowHours = h);
                      _reload();
                    }
                  },
                ),
            ],
          ),
          const SizedBox(height: 10),
          const Text('状态', style: TextStyle(fontSize: 11.5, color: Color(0xFF6B7280))),
          const SizedBox(height: 6),
          Wrap(
            spacing: 6,
            children: [
              for (final s in const [null, 'applied', 'skipped', 'pending', 'failed'])
                ChoiceChip(
                  label: Text(s ?? '全部'),
                  selected: _statusFilter == s,
                  onSelected: (v) {
                    if (v) {
                      setState(() => _statusFilter = s);
                      _reload();
                    }
                  },
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _DdlItemCard extends StatefulWidget {
  final DdlAuditItem item;
  const _DdlItemCard({required this.item});
  @override
  State<_DdlItemCard> createState() => _DdlItemCardState();
}

class _DdlItemCardState extends State<_DdlItemCard> {
  bool _expanded = false;
  @override
  Widget build(BuildContext context) {
    final it = widget.item;
    final st = _statusStyle(it.status);
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: () => setState(() => _expanded = !_expanded),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(10),
            border: it.status == 'failed'
                ? Border.all(color: const Color(0xFFFECACA), width: 1)
                : null,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                    decoration: BoxDecoration(
                      color: st.$1,
                      borderRadius: BorderRadius.circular(3),
                    ),
                    child: Text(
                      it.status.toUpperCase(),
                      style: TextStyle(fontSize: 10, fontWeight: FontWeight.w800, color: st.$2),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '${it.srcSchema}.${it.srcTable} → ${it.targetTable}',
                      style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, fontFamily: 'monospace'),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  if (it.ts != null)
                    Text(_fmtTime(it.ts!), style: const TextStyle(fontSize: 11, color: Colors.black45)),
                ],
              ),
              if (it.errMsg != null && it.errMsg!.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text('err: ${it.errMsg}', style: const TextStyle(fontSize: 11.5, color: Color(0xFFC62828))),
              ],
              if (_expanded) ...[
                const SizedBox(height: 10),
                if (it.binlogPos != null)
                  _KV('binlog', it.binlogPos!, mono: true),
                _KV('source', it.sourceStmt ?? '—', mono: true),
                if (it.appliedStmt != null && it.appliedStmt!.isNotEmpty)
                  _KV('applied', it.appliedStmt!, mono: true),
              ],
            ],
          ),
        ),
      ),
    );
  }

  (Color, Color) _statusStyle(String s) {
    switch (s) {
      case 'applied':
        return (const Color(0xFFECFDF5), const Color(0xFF15A47A));
      case 'skipped':
        return (const Color(0xFFF3F4F6), const Color(0xFF6B7280));
      case 'pending':
        return (const Color(0xFFFFFBEB), const Color(0xFFB25E09));
      case 'failed':
        return (const Color(0xFFFEF2F2), const Color(0xFFC62828));
      default:
        return (const Color(0xFFF3F4F6), const Color(0xFF6B7280));
    }
  }
}

// ────────────────────────────────────────────────────────────────
// 同步统计 tab —— 每张表：行数 / 磁盘 MB / 24h 流量 / 错误率
// ────────────────────────────────────────────────────────────────
class _SyncStatsTab extends StatefulWidget {
  const _SyncStatsTab();
  @override
  State<_SyncStatsTab> createState() => _SyncStatsTabState();
}

class _SyncStatsTabState extends State<_SyncStatsTab> {
  List<TableStatus> _tables = const [];
  String? _error;
  bool _loading = false;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _fetch();
    _timer = Timer.periodic(const Duration(seconds: 10), (_) => _fetch());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _fetch() async {
    if (!mounted) return;
    // 只在首次（_tables 空）显示 loading —— 每 10s 轮询不该闪转圈让用户觉得卡
    final firstLoad = _tables.isEmpty;
    if (firstLoad) setState(() => _loading = true);
    try {
      final s = await OpsService.getCanalStatus();
      if (!mounted) return;
      setState(() { _tables = s.tables; _error = null; _loading = false; });
    } catch (e) {
      if (!mounted) return;
      setState(() { _error = e.toString(); _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    // 汇总条：总表数、总行数、总 MB、24h 总流量
    final totalRows = _tables.fold<int>(0, (a, t) => a + (t.rows ?? 0));
    final totalMb = _tables.fold<double>(0, (a, t) => a + (t.sizeMb ?? 0));
    final totalOps = _tables.fold<int>(0, (a, t) => a + t.total24h);
    final totalErr = _tables.fold<int>(0, (a, t) => a + t.err24h);
    return RefreshIndicator(
      onRefresh: _fetch,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 24),
        children: [
          _SummaryStripCard(
            tableCount: _tables.length,
            totalRows: totalRows,
            totalMb: totalMb,
            totalOps24h: totalOps,
            errCount: totalErr,
            loading: _loading,
          ),
          const SizedBox(height: 12),
          for (final t in _tables) ...[
            _TableStatCard(t: t),
            const SizedBox(height: 10),
          ],
          if (_tables.isEmpty && !_loading)
            const Padding(
              padding: EdgeInsets.all(24),
              child: Center(child: Text('暂无同步表', style: TextStyle(color: Colors.black38))),
            ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text('拉取失败: $_error',
                  style: const TextStyle(color: Color(0xFFC62828))),
            ),
          ],
        ],
      ),
    );
  }
}

// ─── 汇总条 ────────────────────
class _SummaryStripCard extends StatelessWidget {
  final int tableCount;
  final int totalRows;
  final double totalMb;
  final int totalOps24h;
  final int errCount;
  final bool loading;
  const _SummaryStripCard({
    required this.tableCount,
    required this.totalRows,
    required this.totalMb,
    required this.totalOps24h,
    required this.errCount,
    required this.loading,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: const [BoxShadow(color: Color(0x0F000000), blurRadius: 8, offset: Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('同步汇总', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              if (loading)
                const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2)),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _SumCell(label: '表数', value: '$tableCount'),
              _SumCell(label: '总行数', value: _fmtNum(totalRows)),
              _SumCell(label: '磁盘', value: '${totalMb.toStringAsFixed(1)}MB'),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _SumCell(label: '24h 流量', value: _fmtNum(totalOps24h)),
              _SumCell(label: '24h 错误', value: '$errCount',
                tone: errCount > 0 ? const Color(0xFFC62828) : null),
              _SumCell(label: '总错误率',
                value: totalOps24h > 0
                  ? '${(errCount / totalOps24h * 100).toStringAsFixed(2)}%'
                  : '0%',
                tone: errCount > 0 ? const Color(0xFFC62828) : const Color(0xFF16A34A)),
            ],
          ),
        ],
      ),
    );
  }
}

class _SumCell extends StatelessWidget {
  final String label;
  final String value;
  final Color? tone;
  const _SumCell({required this.label, required this.value, this.tone});
  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(fontSize: 11, color: Colors.black45)),
          const SizedBox(height: 3),
          Text(value, style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: tone ?? Colors.black87)),
        ],
      ),
    );
  }
}

// ─── 单表卡片 ────────────────
class _TableStatCard extends StatelessWidget {
  final TableStatus t;
  const _TableStatCard({required this.t});

  @override
  Widget build(BuildContext context) {
    final hasErr = t.err24h > 0;
    final hot = t.total24h > 0;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: const [BoxShadow(color: Color(0x0A000000), blurRadius: 6, offset: Offset(0, 1))],
        border: hasErr ? Border.all(color: const Color(0xFFFECACA), width: 1) : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 8, height: 8,
                decoration: BoxDecoration(
                  color: hot ? const Color(0xFF16A34A) : const Color(0xFF9CA3AF),
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(t.name, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, fontFamily: 'monospace')),
              ),
              if (t.lastApplyAt != null)
                Text(_fmtTime(t.lastApplyAt!), style: const TextStyle(fontSize: 11, color: Colors.black45)),
            ],
          ),
          if (t.error != null) ...[
            const SizedBox(height: 8),
            Text('查询失败: ${t.error}', style: const TextStyle(color: Color(0xFFC62828), fontSize: 12)),
          ] else ...[
            const SizedBox(height: 10),
            Row(
              children: [
                _StatBox(label: '行数', value: _fmtNum(t.rows ?? 0)),
                _StatBox(label: '磁盘', value: t.sizeMb != null ? '${t.sizeMb!.toStringAsFixed(1)}MB' : '-'),
                _StatBox(label: '24h 流量', value: _fmtNum(t.total24h)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                _StatBox(label: 'INS', value: '${t.ins24h}'),
                _StatBox(label: 'UPD', value: '${t.upd24h}'),
                _StatBox(label: 'DEL', value: '${t.del24h}'),
                _StatBox(
                  label: '错误率',
                  value: '${t.errorRate.toStringAsFixed(2)}%',
                  tone: hasErr ? const Color(0xFFC62828) : const Color(0xFF16A34A),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _StatBox extends StatelessWidget {
  final String label;
  final String value;
  final Color? tone;
  const _StatBox({required this.label, required this.value, this.tone});
  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(fontSize: 11, color: Colors.black45)),
          const SizedBox(height: 3),
          Text(value, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: tone ?? Colors.black87)),
        ],
      ),
    );
  }
}

String _fmtNum(int n) {
  if (n >= 1000000000) return '${(n / 1000000000).toStringAsFixed(2)}B';
  if (n >= 1000000) return '${(n / 1000000).toStringAsFixed(2)}M';
  if (n >= 1000) return '${(n / 1000).toStringAsFixed(1)}K';
  return '$n';
}

class _AuditTab extends StatefulWidget {
  const _AuditTab();

  @override
  State<_AuditTab> createState() => _AuditTabState();
}

class _AuditTabState extends State<_AuditTab> {
  AuditAnalytics? _data;
  String? _error;
  bool _loading = false;
  int _windowHours = 24;

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    if (!mounted) return;
    setState(() => _loading = true);
    try {
      final d = await OpsService.getAuditAnalytics(hours: _windowHours);
      if (!mounted) return;
      setState(() {
        _data = d;
        _error = null;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _fetch,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 24),
        children: [
          _windowSelector(),
          const SizedBox(height: 12),
          if (_data != null) _totalsCard(_data!.totals),
          const SizedBox(height: 12),
          if (_data != null) _topPathsCard(_data!.topPaths),
          const SizedBox(height: 12),
          if (_data != null) _slowestCard(_data!.slowest),
          const SizedBox(height: 12),
          if (_data != null) _errorsCard(_data!.errorBreakdown),
          const SizedBox(height: 12),
          if (_data != null) _topUsersCard(_data!.topUsers),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text('拉取失败: $_error',
                  style: const TextStyle(color: Color(0xFFC62828))),
            ),
          ],
        ],
      ),
    );
  }

  Widget _windowSelector() {
    return _AuditCard(
      title: '时间窗口',
      trailing: _loading
          ? const SizedBox(
              width: 14,
              height: 14,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : null,
      child: Wrap(
        spacing: 8,
        children: [
          for (final h in const [1, 6, 24, 72, 168])
            ChoiceChip(
              label: Text(h < 24 ? '${h}h' : '${(h / 24).round()}d'),
              selected: _windowHours == h,
              onSelected: (v) {
                if (v) {
                  setState(() => _windowHours = h);
                  _fetch();
                }
              },
            ),
        ],
      ),
    );
  }

  Widget _totalsCard(AuditTotals t) {
    final rate = (t.successRate * 100).toStringAsFixed(1);
    return _AuditCard(
      title: '总体 (${_windowHours}h)',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: _kpi('调用总数', '${t.total}', const Color(0xFF0E0E10))),
              Expanded(
                child: _kpi(
                  '成功率',
                  '$rate%',
                  t.successRate > 0.95
                      ? const Color(0xFF15A47A)
                      : t.successRate > 0.8
                          ? const Color(0xFFB25E09)
                          : const Color(0xFFC62828),
                ),
              ),
              Expanded(child: _kpi('错误', '${t.err}', const Color(0xFFC62828))),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: _kpi('平均 ms', '${t.avgMs}', const Color(0xFF6B7280))),
              Expanded(
                child: _kpi('P95 ms', '${t.p95Ms}',
                    t.p95Ms > 3000 ? const Color(0xFFB25E09) : const Color(0xFF6B7280)),
              ),
              Expanded(child: _kpi('最慢 ms', '${t.maxMs}', const Color(0xFF6B7280))),
            ],
          ),
        ],
      ),
    );
  }

  Widget _kpi(String label, String value, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 11, color: Color(0xFF6B7280))),
        const SizedBox(height: 3),
        Text(value,
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: color)),
      ],
    );
  }

  Widget _topPathsCard(List<AuditPath> paths) {
    return _AuditCard(
      title: '高频接口 (top 15)',
      child: paths.isEmpty
          ? const _EmptyText('近 ${''} 无数据')
          : Column(
              children: [
                for (final p in paths)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 5),
                    child: Row(
                      children: [
                        Expanded(
                          flex: 5,
                          child: Text(
                            p.path,
                            style: const TextStyle(
                                fontSize: 12, fontFamily: 'monospace'),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        SizedBox(
                          width: 45,
                          child: Text('${p.cnt}',
                              textAlign: TextAlign.right,
                              style: const TextStyle(
                                  fontSize: 12, fontWeight: FontWeight.w700)),
                        ),
                        SizedBox(
                          width: 60,
                          child: Text('${p.avgMs}ms',
                              textAlign: TextAlign.right,
                              style: TextStyle(
                                fontSize: 11,
                                color: p.avgMs > 3000
                                    ? const Color(0xFFB25E09)
                                    : Colors.black54,
                              )),
                        ),
                        SizedBox(
                          width: 50,
                          child: Text(p.errCnt > 0 ? '⚠${p.errCnt}' : '',
                              textAlign: TextAlign.right,
                              style: const TextStyle(
                                  fontSize: 11, color: Color(0xFFC62828))),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }

  Widget _slowestCard(List<AuditSlow> rows) {
    return _AuditCard(
      title: '最慢 10 次',
      child: rows.isEmpty
          ? const _EmptyText('无数据')
          : Column(
              children: [
                for (final r in rows)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 5),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(r.path,
                                  style: const TextStyle(
                                      fontSize: 12, fontFamily: 'monospace'),
                                  overflow: TextOverflow.ellipsis),
                              Text(
                                '${r.at ?? ''}${r.code == null ? '' : '  ${r.code}'}${r.ok ? '' : ' ✕'}',
                                style: const TextStyle(
                                    fontSize: 10.5, color: Color(0xFF6B7280)),
                              ),
                            ],
                          ),
                        ),
                        Text('${r.durationMs}ms',
                            style: const TextStyle(
                                fontSize: 13, fontWeight: FontWeight.w800)),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }

  Widget _errorsCard(List<AuditError> errors) {
    return _AuditCard(
      title: '错误码分布',
      child: errors.isEmpty
          ? const _EmptyText('近期无错误 ✓')
          : Column(
              children: [
                for (final e in errors)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(e.code,
                              style: const TextStyle(
                                  fontSize: 12, fontFamily: 'monospace')),
                        ),
                        Text('${e.cnt}',
                            style: const TextStyle(
                                fontSize: 13, fontWeight: FontWeight.w800)),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }

  Widget _topUsersCard(List<AuditUser> users) {
    return _AuditCard(
      title: '活跃用户 (top 8)',
      child: users.isEmpty
          ? const _EmptyText('无数据')
          : Column(
              children: [
                for (final u in users)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Expanded(child: Text(u.username)),
                        Text('${u.cnt}',
                            style: const TextStyle(
                                fontSize: 13, fontWeight: FontWeight.w800)),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }
}

class _AuditCard extends StatelessWidget {
  final String title;
  final Widget child;
  final Widget? trailing;
  const _AuditCard({required this.title, required this.child, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: const [
          BoxShadow(
              color: Color(0x0A000000), blurRadius: 6, offset: Offset(0, 2)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(title,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w700)),
              const Spacer(),
              if (trailing != null) trailing!,
            ],
          ),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}

class _EmptyText extends StatelessWidget {
  final String text;
  const _EmptyText(this.text);
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child:
          Text(text, style: const TextStyle(color: Colors.black38, fontSize: 12)),
    );
  }
}


class _StatusRow extends StatelessWidget {
  final CanalStatus? status;
  final bool loading;
  const _StatusRow({required this.status, required this.loading});

  @override
  Widget build(BuildContext context) {
    final s = status;
    return _Card(
      title: '组件状态',
      trailing: loading
          ? const SizedBox(
              width: 14,
              height: 14,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Text(
              s?.generatedAt != null ? _fmtTime(s!.generatedAt!) : '—',
              style: const TextStyle(fontSize: 12, color: Colors.black54),
            ),
      child: Column(
        children: [
          _StatusLine(
            label: 'nexa-cdc',
            up: (s?.canalServer.up ?? false) && (s?.consumer.up ?? false),
            detail: (s?.canalServer.up ?? false) && (s?.consumer.up ?? false)
                ? '心跳 ${s?.consumer.idleSeconds ?? '?'} 秒前 · ${s?.canalServer.host ?? ''}'
                : s?.consumer.lastHeartbeatAt != null
                    ? '最近心跳 ${_fmtTime(s!.consumer.lastHeartbeatAt!)}'
                    : '未连接',
          ),
          _StatusLine(
            label: 'warehouse',
            up: s?.warehouse.up ?? false,
            detail: s?.warehouse.host ?? '',
          ),
        ],
      ),
    );
  }
}

class _StatusLine extends StatelessWidget {
  final String label;
  final bool up;
  final String detail;
  const _StatusLine(
      {required this.label, required this.up, required this.detail});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(
              color: up ? const Color(0xFF34C759) : const Color(0xFFFF3B30),
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Text(label,
              style: const TextStyle(
                  fontSize: 14, fontWeight: FontWeight.w600)),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              detail,
              style: const TextStyle(fontSize: 12, color: Colors.black54),
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.right,
            ),
          ),
        ],
      ),
    );
  }
}

class _MasterPositionCard extends StatelessWidget {
  final MasterPosition? master;
  const _MasterPositionCard({required this.master});

  @override
  Widget build(BuildContext context) {
    final m = master;
    if (m == null) {
      return _Card(
        title: '源库位点',
        child: const Text('未获取', style: TextStyle(color: Colors.black54)),
      );
    }
    if (m.error != null) {
      return _Card(
        title: '源库位点',
        child: Text('查询失败: ${m.error}',
            style: const TextStyle(color: Color(0xFFC62828))),
      );
    }
    return _Card(
      title: '源库位点',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _KV('binlog 文件', m.file ?? '-'),
          _KV('位置', m.position?.toString() ?? '-'),
          if (m.gtidSet != null && m.gtidSet!.isNotEmpty)
            _KV('GTID', m.gtidSet!, mono: true),
        ],
      ),
    );
  }
}

// 分页错误卡片 —— 24h 内动态分页，加载更多；解决旧版 Row 溢出的问题
class _ErrorsCard extends StatefulWidget {
  // errors 参数是"首屏预取"的错误列表（跟 canal-status 一起来的）
  // 用户点击"加载更多"后走 /ops/canal-errors 分页接口
  final List<CanalError> errors;
  const _ErrorsCard({required this.errors});

  @override
  State<_ErrorsCard> createState() => _ErrorsCardState();
}

class _ErrorsCardState extends State<_ErrorsCard> {
  late List<CanalError> _items = List.of(widget.errors);
  int _pageNo = 1;
  static const int _pageSize = 10;
  bool _loading = false;
  bool _hasMore = true;
  int _total = 0;

  @override
  void didUpdateWidget(covariant _ErrorsCard old) {
    super.didUpdateWidget(old);
    // canal-status 5s 刷一次，首屏那批换了就重置
    if (_pageNo == 1 && old.errors != widget.errors) {
      setState(() {
        _items = List.of(widget.errors);
      });
    }
  }

  Future<void> _loadMore() async {
    if (_loading) return;
    setState(() => _loading = true);
    try {
      final next = _pageNo + 1;
      final page = await OpsService.getCanalErrorsPage(pageNo: next, pageSize: _pageSize);
      if (!mounted) return;
      setState(() {
        _items = [..._items, ...page.list];
        _pageNo = next;
        _hasMore = page.hasMore;
        _total = page.total;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return _Card(
      title: _total > 0 ? '24h 错误 · ${_items.length}/$_total' : '24h 错误',
      child: _items.isEmpty
          ? const Padding(
              padding: EdgeInsets.symmetric(vertical: 6),
              child: Text('近 24h 无错误 ✓',
                  style: TextStyle(color: Color(0xFF2E7D32))),
            )
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                for (final e in _items) _ErrorItem(e: e),
                if (_hasMore || _loading) ...[
                  const SizedBox(height: 8),
                  Center(
                    child: TextButton(
                      onPressed: _loading ? null : _loadMore,
                      child: _loading
                          ? const SizedBox(
                              width: 14,
                              height: 14,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('加载更多'),
                    ),
                  ),
                ],
              ],
            ),
    );
  }
}

// 单条错误 —— 时间戳独立成行，避免 Row 溢出；长 id / errMsg 允许换行
class _ErrorItem extends StatelessWidget {
  final CanalError e;
  const _ErrorItem({required this.e});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 时间戳单独一行（放右上，不再和 title 抢位置）
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  '${e.tableName ?? '?'} · ${e.eventType ?? '?'}',
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                e.ts != null ? _fmtTime(e.ts!) : '',
                style: const TextStyle(fontSize: 11, color: Colors.black54),
              ),
            ],
          ),
          if ((e.rowId ?? '').isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                'id: ${e.rowId}',
                style: const TextStyle(fontSize: 11, color: Colors.black54),
                softWrap: true,
              ),
            ),
          const SizedBox(height: 3),
          Text(
            e.errMsg ?? '',
            style: const TextStyle(fontSize: 11.5, color: Color(0xFFC62828)),
            softWrap: true,
          ),
        ],
      ),
    );
  }
}

// ── UI helpers ────────────────────────────────────────────

class _Card extends StatelessWidget {
  final String title;
  final Widget child;
  final Widget? trailing;
  const _Card({required this.title, required this.child, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        boxShadow: const [
          BoxShadow(
              color: Color(0x0A000000), blurRadius: 6, offset: Offset(0, 2)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(title,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w700)),
              const Spacer(),
              if (trailing != null) trailing!,
            ],
          ),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}

class _KV extends StatelessWidget {
  final String k;
  final String v;
  final bool mono;
  final Color? valueColor;
  const _KV(this.k, this.v, {this.mono = false, this.valueColor});
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(k,
                style: const TextStyle(
                    fontSize: 12, color: Colors.black54)),
          ),
          Expanded(
            child: Text(
              v,
              style: TextStyle(
                fontSize: mono ? 11 : 13,
                fontFamily: mono ? 'monospace' : null,
                color: valueColor,
                fontWeight: valueColor != null ? FontWeight.w600 : null,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

String _fmtTime(DateTime t) {
  final l = t.toLocal();
  return '${l.month.toString().padLeft(2, '0')}-${l.day.toString().padLeft(2, '0')} '
      '${l.hour.toString().padLeft(2, '0')}:${l.minute.toString().padLeft(2, '0')}:${l.second.toString().padLeft(2, '0')}';
}
