import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../services/ai_reception_service.dart';
import '../services/api_service.dart';
import '../services/chat_service.dart';
import '../services/hotel_operations_service.dart';
import '../widgets/kyx_design.dart';
import 'front_desk_reception_page.dart';
import 'hotel_manager_dashboard_page.dart';

class HotelFrontDeskPage extends StatefulWidget {
  const HotelFrontDeskPage({super.key});

  @override
  State<HotelFrontDeskPage> createState() => _HotelFrontDeskPageState();
}

class _HotelFrontDeskPageState extends State<HotelFrontDeskPage> {
  String _store = HotelOperationsService.stores.first;
  HotelPermission _permission = HotelPermission.empty;
  _OrderFilter _orderFilter = _OrderFilter.all;
  late Future<_HotelFrontDeskSnapshot> _future;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<_HotelFrontDeskSnapshot> _load() async {
    final permission = await HotelOperationsService.getPermission();
    _permission = permission;
    if (!permission.canUseFrontDesk || permission.stores.isEmpty) {
      return const _HotelFrontDeskSnapshot();
    }
    final stores = permission.stores;
    final nextStore = stores.contains(_store) ? _store : stores.first;
    _store = nextStore;
    final orders = await HotelOperationsService.getWorkOrders(store: _store);
    final talks = await HotelOperationsService.getIntercomMessages();
    return _HotelFrontDeskSnapshot(orders: orders, talks: talks);
  }

  Future<void> _refresh() async {
    setState(() => _future = _load());
    await _future;
  }

  Future<void> _openDashboard() async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const HotelManagerDashboardPage()),
    );
    if (mounted) await _refresh();
  }

  Future<void> _openReceptionRecord() async {
    await Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => const FrontDeskReceptionPage()));
    if (mounted) await _refresh();
  }

  Future<void> _openTempIntercom() async {
    final sent = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => _TempIntercomSheet(store: _store),
    );
    if (sent == true && mounted) await _refresh();
  }

  Future<void> _createOrder({String? presetTitle, String? presetType}) async {
    final created = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => _WorkOrderSheet(
        store: _store,
        presetTitle: presetTitle,
        presetType: presetType,
      ),
    );
    if (created == true && mounted) await _refresh();
  }

  Future<String?> _askFinishRemark(HotelWorkOrder order) async {
    final controller = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('完成工单'),
        content: TextField(
          controller: controller,
          autofocus: true,
          minLines: 2,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: '处理结果（选填）',
            hintText: '例如：已送毛巾，客人确认满意',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('确认完成'),
          ),
        ],
      ),
    );
    controller.dispose();
    return result;
  }

  Future<void> _updateOrder(
    HotelWorkOrder order,
    HotelWorkOrderStatus status, {
    String? remark,
  }) async {
    String? finalRemark = remark;
    if (status == HotelWorkOrderStatus.done && finalRemark == null) {
      finalRemark = await _askFinishRemark(order);
      if (finalRemark == null) return;
    }
    try {
      await HotelOperationsService.updateOrderStatus(
        order.id,
        status,
        remark: finalRemark,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('已更新为${status.label}')));
      await _refresh();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('更新失败：${_cleanError(error)}')));
    }
  }

  Future<void> _openOrderDetail(
    HotelWorkOrder order,
    List<HotelIntercomMessage> messages,
  ) async {
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: KyXColors.surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => _OrderDetailSheet(
        order: order,
        messages: messages,
        onDoing: () => _updateOrder(order, HotelWorkOrderStatus.doing),
        onDone: () => _updateOrder(order, HotelWorkOrderStatus.done),
      ),
    );
    if (mounted) await _refresh();
  }

  Future<void> _showOrderManageActions(HotelWorkOrder order) async {
    if (!_permission.canManageWorkOrder) return;
    final action = await showModalBottomSheet<String>(
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
            Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: KyXColors.line,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 8),
            ListTile(
              leading: const Icon(
                Icons.edit_outlined,
                color: KyXColors.primary,
              ),
              title: const Text('编辑工单'),
              onTap: () => Navigator.pop(context, 'edit'),
            ),
            if (_permission.canDeleteWorkOrder)
              ListTile(
                leading: const Icon(Icons.delete_outline, color: KyXColors.red),
                title: const Text('删除工单'),
                onTap: () => Navigator.pop(context, 'delete'),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (action == 'edit') await _editOrder(order);
    if (action == 'delete') await _deleteOrder(order);
  }

  Future<void> _editOrder(HotelWorkOrder order) async {
    final updated = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => _EditWorkOrderSheet(order: order),
    );
    if (updated == true && mounted) await _refresh();
  }

  Future<void> _deleteOrder(HotelWorkOrder order) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除工单'),
        content: Text('确定删除“${order.title}”？删除后工单列表不再显示。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: KyXColors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await HotelOperationsService.deleteWorkOrder(order.id);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('工单已删除')));
      await _refresh();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('删除失败：${_cleanError(error)}')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '酒店前台',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          if (_permission.canViewDashboard)
            IconButton(
              tooltip: '驾驶舱',
              onPressed: _openDashboard,
              icon: const Icon(Icons.analytics_outlined),
            ),
        ],
      ),
      body: FutureBuilder<_HotelFrontDeskSnapshot>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting &&
              !snapshot.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _NoHotelPermissionView(message: '加载失败：${snapshot.error}');
          }
          if (!_permission.canUseFrontDesk) {
            return const _NoHotelPermissionView(message: '暂无酒店前台权限');
          }
          if (_permission.stores.isEmpty) {
            return const _NoHotelPermissionView(message: '未识别到所属门店，请联系管理员维护部门');
          }
          final data = snapshot.data ?? const _HotelFrontDeskSnapshot();
          final storeOrders = data.orders
              .where((item) => item.store == _store)
              .toList();
          final pendingCount = storeOrders
              .where((item) => item.status == HotelWorkOrderStatus.pending)
              .length;
          final doingCount = storeOrders
              .where((item) => item.status == HotelWorkOrderStatus.doing)
              .length;
          final doneCount = storeOrders
              .where((item) => item.status == HotelWorkOrderStatus.done)
              .length;
          final talks = data.talks
              .where((item) => item.store == _store)
              .toList();
          final visibleOrders = storeOrders.where((order) {
            return switch (_orderFilter) {
              _OrderFilter.all => true,
              _OrderFilter.pending =>
                order.status == HotelWorkOrderStatus.pending,
              _OrderFilter.doing => order.status == HotelWorkOrderStatus.doing,
              _OrderFilter.done => order.status == HotelWorkOrderStatus.done,
            };
          }).toList();

          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.only(bottom: 28),
              children: [
                _Header(
                  store: _store,
                  storeOptions: _permission.stores,
                  pendingCount: pendingCount,
                  doingCount: doingCount,
                  doneCount: doneCount,
                  onStoreChanged: (value) => setState(() {
                    _store = value;
                    _future = _load();
                  }),
                ),
                const KyXSectionLabel('操作'),
                KyXListSection(
                  children: [
                    KyXListRow(
                      leading: const _PlainIcon(Icons.add_task_outlined),
                      title: '创建工单',
                      subtitle: '语音、手动或选择接待录音生成工单',
                      trailing: const Icon(
                        Icons.chevron_right,
                        color: KyXColors.textTertiary,
                      ),
                      onTap: () => _createOrder(),
                    ),
                    KyXListRow(
                      leading: const _PlainIcon(Icons.graphic_eq_outlined),
                      title: '前台接待录音',
                      subtitle: '客户对话录音、转写和分析',
                      trailing: const Icon(
                        Icons.chevron_right,
                        color: KyXColors.textTertiary,
                      ),
                      onTap: _openReceptionRecord,
                    ),
                    KyXListRow(
                      leading: const _PlainIcon(Icons.campaign_outlined),
                      title: '临时对讲通知',
                      subtitle: '只通知不生成工单',
                      trailing: const Icon(
                        Icons.chevron_right,
                        color: KyXColors.textTertiary,
                      ),
                      onTap: _openTempIntercom,
                      showDivider: false,
                    ),
                  ],
                ),
                const KyXSectionLabel('工单'),
                Container(
                  color: KyXColors.surface,
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
                  child: _FilterBar(
                    value: _orderFilter,
                    onChanged: (value) => setState(() => _orderFilter = value),
                  ),
                ),
                visibleOrders.isEmpty
                    ? const KyXListSection(child: _EmptyRow(text: '暂无工单'))
                    : KyXListSection(
                        children: _withDividers(
                          visibleOrders.map((order) {
                            final linkedMessages = talks
                                .where((item) => item.linkedOrderId == order.id)
                                .toList();
                            return InkWell(
                              onTap: () =>
                                  _openOrderDetail(order, linkedMessages),
                              onLongPress: _permission.canManageWorkOrder
                                  ? () => _showOrderManageActions(order)
                                  : null,
                              child: _OrderRow(
                                order: order,
                                onDoing: () => _updateOrder(
                                  order,
                                  HotelWorkOrderStatus.doing,
                                ),
                                onDone: () => _updateOrder(
                                  order,
                                  HotelWorkOrderStatus.done,
                                ),
                              ),
                            );
                          }).toList(),
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

class _HotelFrontDeskSnapshot {
  final List<HotelWorkOrder> orders;
  final List<HotelIntercomMessage> talks;

  const _HotelFrontDeskSnapshot({
    this.orders = const [],
    this.talks = const [],
  });
}

class _NoHotelPermissionView extends StatelessWidget {
  final String message;

  const _NoHotelPermissionView({required this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.lock_outline,
              size: 40,
              color: KyXColors.textTertiary,
            ),
            const SizedBox(height: 12),
            Text(message, style: KyXText.body, textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  final String store;
  final List<String> storeOptions;
  final int pendingCount;
  final int doingCount;
  final int doneCount;
  final ValueChanged<String> onStoreChanged;

  const _Header({
    required this.store,
    this.storeOptions = HotelOperationsService.stores,
    required this.pendingCount,
    required this.doingCount,
    required this.doneCount,
    required this.onStoreChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
      child: Row(
        children: [
          const Icon(
            Icons.assignment_outlined,
            color: KyXColors.primary,
            size: 24,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(child: Text(store, style: KyXText.title)),
                    _StorePicker(
                      value: store,
                      options: storeOptions,
                      onChanged: onStoreChanged,
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  '待确认 $pendingCount · 已收到 $doingCount · 已完成 $doneCount',
                  style: KyXText.secondary,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StorePicker extends StatelessWidget {
  final String value;
  final List<String> options;
  final ValueChanged<String> onChanged;

  const _StorePicker({
    required this.value,
    this.options = HotelOperationsService.stores,
    required this.onChanged,
  });

  Future<void> _pick(BuildContext context) async {
    final next = await showModalBottomSheet<String>(
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
            for (final item in options)
              ListTile(
                title: Text(item, style: KyXText.bodyStrong),
                trailing: item == value
                    ? const Icon(Icons.check, color: KyXColors.primary)
                    : null,
                onTap: () => Navigator.pop(context, item),
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (next != null) onChanged(next);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => _pick(context),
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
        decoration: BoxDecoration(
          color: KyXColors.bg,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: KyXColors.line),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              value,
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
    );
  }
}

class _PlainIcon extends StatelessWidget {
  final IconData icon;
  final Color? color;
  const _PlainIcon(this.icon, {this.color});

  @override
  Widget build(BuildContext context) {
    return Icon(icon, size: 22, color: color ?? KyXColors.primary);
  }
}

class _OrderRow extends StatelessWidget {
  final HotelWorkOrder order;
  final VoidCallback onDoing;
  final VoidCallback onDone;

  const _OrderRow({
    required this.order,
    required this.onDoing,
    required this.onDone,
  });

  @override
  Widget build(BuildContext context) {
    final isRisk = order.priority == '紧急' || order.customerEmotion == '生气';
    final statusColor = switch (order.status) {
      HotelWorkOrderStatus.pending => KyXColors.amber,
      HotelWorkOrderStatus.doing => KyXColors.primary,
      HotelWorkOrderStatus.done => KyXColors.green,
    };
    final meta = [
      if (order.roomNo.isNotEmpty) order.roomNo,
      order.type,
      order.customerEmotion,
      order.assigneeName,
    ].join(' · ');

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 11, 12, 11),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Icon(
            isRisk ? Icons.error_outline : Icons.assignment_outlined,
            size: 22,
            color: isRisk ? KyXColors.red : statusColor,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  order.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.bodyStrong,
                ),
                const SizedBox(height: 3),
                Text(
                  meta,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.secondary,
                ),
                if (order.content.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    order.content,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: KyXText.caption,
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 8),
          _StatusTag(status: order.status),
          if (order.status != HotelWorkOrderStatus.done)
            const SizedBox(width: 6),
          if (order.status == HotelWorkOrderStatus.pending)
            _SmallTextButton(text: '已收到', onTap: onDoing),
          if (order.status != HotelWorkOrderStatus.done)
            _SmallTextButton(text: '完成', onTap: onDone, strong: true),
        ],
      ),
    );
  }
}

class _StatusTag extends StatelessWidget {
  final HotelWorkOrderStatus status;

  const _StatusTag({required this.status});

  @override
  Widget build(BuildContext context) {
    final (bg, fg, border, icon) = switch (status) {
      HotelWorkOrderStatus.pending => (
        const Color(0xFFFFF7E6),
        KyXColors.amber,
        const Color(0xFFFFD591),
        Icons.schedule_outlined,
      ),
      HotelWorkOrderStatus.doing => (
        const Color(0xFFEFF6FF),
        KyXColors.primary,
        const Color(0xFFBFDBFE),
        Icons.downloading_outlined,
      ),
      HotelWorkOrderStatus.done => (
        const Color(0xFFEAF8F0),
        KyXColors.green,
        const Color(0xFF9EDDB8),
        Icons.check_circle,
      ),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: border),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: fg),
          const SizedBox(width: 3),
          Text(
            status.label,
            style: KyXText.caption.copyWith(
              color: fg,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _SmallTextButton extends StatelessWidget {
  final String text;
  final VoidCallback onTap;
  final bool strong;

  const _SmallTextButton({
    required this.text,
    required this.onTap,
    this.strong = false,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(4),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
        child: Text(
          text,
          style: KyXText.secondary.copyWith(
            color: strong ? KyXColors.primary : KyXColors.textSecondary,
            fontWeight: strong ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
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

enum _OrderFilter { all, pending, doing, done }

extension _OrderFilterText on _OrderFilter {
  String get label {
    switch (this) {
      case _OrderFilter.all:
        return '全部';
      case _OrderFilter.pending:
        return '待确认';
      case _OrderFilter.doing:
        return '已收到';
      case _OrderFilter.done:
        return '已完成';
    }
  }
}

class _HotelWorkOrderListPage extends StatefulWidget {
  final String store;
  final List<HotelWorkOrder> orders;
  final Future<void> Function(
    HotelWorkOrder order,
    HotelWorkOrderStatus status, {
    String? remark,
  })
  onUpdate;

  const _HotelWorkOrderListPage({
    required this.store,
    required this.orders,
    required this.onUpdate,
  });

  @override
  State<_HotelWorkOrderListPage> createState() =>
      _HotelWorkOrderListPageState();
}

class _HotelWorkOrderListPageState extends State<_HotelWorkOrderListPage> {
  static const int _pageSize = 20;

  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  _OrderFilter _filter = _OrderFilter.all;
  List<HotelWorkOrder> _orders = const [];
  int _pageNo = 1;
  bool _loading = false;
  bool _loadingMore = false;
  bool _hasMore = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _orders = widget.orders;
    _scrollController.addListener(_onScroll);
    unawaited(_reload());
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  HotelWorkOrderStatus? get _statusFilter => switch (_filter) {
    _OrderFilter.all => null,
    _OrderFilter.pending => HotelWorkOrderStatus.pending,
    _OrderFilter.doing => HotelWorkOrderStatus.doing,
    _OrderFilter.done => HotelWorkOrderStatus.done,
  };

  void _onScroll() {
    if (!_hasMore || _loading || _loadingMore) return;
    if (!_scrollController.hasClients) return;
    final position = _scrollController.position;
    if (position.pixels >= position.maxScrollExtent - 180) {
      unawaited(_loadMore());
    }
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _error = null;
      _pageNo = 1;
      _hasMore = true;
    });
    try {
      final latest = await HotelOperationsService.getWorkOrders(
        store: widget.store,
        pageNo: 1,
        pageSize: _pageSize,
        keyword: _searchController.text,
        status: _statusFilter,
      );
      if (!mounted) return;
      setState(() {
        _orders = latest;
        _hasMore = latest.length >= _pageSize;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.toString();
        _loading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    setState(() => _loadingMore = true);
    try {
      final nextPage = _pageNo + 1;
      final more = await HotelOperationsService.getWorkOrders(
        store: widget.store,
        pageNo: nextPage,
        pageSize: _pageSize,
        keyword: _searchController.text,
        status: _statusFilter,
      );
      if (!mounted) return;
      setState(() {
        _pageNo = nextPage;
        _orders = [..._orders, ...more];
        _hasMore = more.length >= _pageSize;
        _loadingMore = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _loadingMore = false);
    }
  }

  Future<void> _update(
    HotelWorkOrder order,
    HotelWorkOrderStatus status, {
    String? remark,
  }) async {
    await widget.onUpdate(order, status, remark: remark);
    await _reload();
  }

  Future<void> _openDetail(HotelWorkOrder order) async {
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: KyXColors.surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => _OrderDetailSheet(
        order: order,
        onDoing: () => _update(order, HotelWorkOrderStatus.doing),
        onDone: () => _update(order, HotelWorkOrderStatus.done),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: Text(
          '${widget.store}工单',
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: RefreshIndicator(
        onRefresh: _reload,
        child: ListView(
          controller: _scrollController,
          padding: const EdgeInsets.only(bottom: 28),
          children: [
            Container(
              color: KyXColors.surface,
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
              child: Column(
                children: [
                  _SearchBox(
                    controller: _searchController,
                    hintText: '搜索房号、类型、内容、处理人',
                    onSubmitted: (_) => _reload(),
                    onClear: () {
                      _searchController.clear();
                      _reload();
                    },
                  ),
                  const SizedBox(height: 10),
                  _FilterBar(
                    value: _filter,
                    onChanged: (value) {
                      setState(() => _filter = value);
                      _reload();
                    },
                  ),
                ],
              ),
            ),
            const KyXSectionLabel('工单'),
            if (_loading && _orders.isEmpty)
              const KyXListSection(child: _EmptyRow(text: '加载中...'))
            else if (_error != null && _orders.isEmpty)
              KyXListSection(child: _EmptyRow(text: '加载失败，请下拉刷新'))
            else if (_orders.isEmpty)
              const KyXListSection(child: _EmptyRow(text: '暂无工单'))
            else
              KyXListSection(
                children: _withDividers(
                  _orders
                      .map(
                        (order) => InkWell(
                          onTap: () => _openDetail(order),
                          child: _OrderRow(
                            order: order,
                            onDoing: () =>
                                _update(order, HotelWorkOrderStatus.doing),
                            onDone: () =>
                                _update(order, HotelWorkOrderStatus.done),
                          ),
                        ),
                      )
                      .toList(),
                ),
              ),
            if (_loadingMore)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 14),
                child: Center(
                  child: SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                ),
              )
            else if (!_hasMore && _orders.isNotEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 14),
                child: Center(child: Text('没有更多了', style: KyXText.caption)),
              ),
          ],
        ),
      ),
    );
  }
}

class _SearchBox extends StatelessWidget {
  final TextEditingController controller;
  final String hintText;
  final ValueChanged<String> onSubmitted;
  final VoidCallback onClear;

  const _SearchBox({
    required this.controller,
    required this.hintText,
    required this.onSubmitted,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 40,
      child: TextField(
        controller: controller,
        textInputAction: TextInputAction.search,
        onSubmitted: onSubmitted,
        decoration: kyxInputDecoration(
          hintText: hintText,
          prefixIcon: const Icon(Icons.search, size: 20),
          suffixIcon: ValueListenableBuilder<TextEditingValue>(
            valueListenable: controller,
            builder: (_, value, __) => value.text.trim().isEmpty
                ? const SizedBox.shrink()
                : IconButton(
                    icon: const Icon(Icons.close, size: 18),
                    onPressed: onClear,
                  ),
          ),
        ).copyWith(contentPadding: const EdgeInsets.symmetric(horizontal: 12)),
      ),
    );
  }
}

class _FilterBar extends StatelessWidget {
  final _OrderFilter value;
  final ValueChanged<_OrderFilter> onChanged;

  const _FilterBar({required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 38,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: KyXColors.bg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: KyXColors.line),
      ),
      child: Row(
        children: _OrderFilter.values.map((item) {
          final selected = item == value;
          return Expanded(
            child: InkWell(
              onTap: () => onChanged(item),
              borderRadius: BorderRadius.circular(6),
              child: Container(
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: selected ? KyXColors.surface : Colors.transparent,
                  borderRadius: BorderRadius.circular(6),
                  boxShadow: selected
                      ? const [
                          BoxShadow(color: Color(0x10000000), blurRadius: 3),
                        ]
                      : null,
                ),
                child: Text(
                  item.label,
                  style: KyXText.secondary.copyWith(
                    color: selected
                        ? KyXColors.primary
                        : KyXColors.textSecondary,
                    fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}


class HotelWorkOrderDetailPage extends StatefulWidget {
  final int orderId;

  const HotelWorkOrderDetailPage({super.key, required this.orderId});

  @override
  State<HotelWorkOrderDetailPage> createState() => _HotelWorkOrderDetailPageState();
}

class _HotelWorkOrderDetailPageState extends State<HotelWorkOrderDetailPage> {
  late Future<HotelWorkOrder> _future;
  bool _updating = false;

  @override
  void initState() {
    super.initState();
    _future = HotelOperationsService.getWorkOrder(widget.orderId);
  }

  Future<void> _reload() async {
    setState(() {
      _future = HotelOperationsService.getWorkOrder(widget.orderId);
    });
    await _future;
  }

  Future<String?> _askFinishRemark() async {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('完成工单'),
        content: TextField(
          controller: controller,
          autofocus: true,
          minLines: 2,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: '处理结果（选填）',
            hintText: '例如：已送水，客人确认收到',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('确认完成'),
          ),
        ],
      ),
    );
  }

  Future<void> _updateStatus(
    HotelWorkOrder order,
    HotelWorkOrderStatus status,
  ) async {
    if (_updating) return;
    String? remark;
    if (status == HotelWorkOrderStatus.done) {
      remark = await _askFinishRemark();
      if (remark == null) return;
    }
    setState(() => _updating = true);
    try {
      await HotelOperationsService.updateOrderStatus(
        order.id,
        status,
        remark: remark,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('已更新为${status.label}')));
      await _reload();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('更新失败：${_cleanError(error)}')));
    } finally {
      if (mounted) setState(() => _updating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('酒店工单详情')),
      body: FutureBuilder<HotelWorkOrder>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _RouteErrorState(
              message: _cleanError(snapshot.error),
              onRetry: _reload,
            );
          }
          final order = snapshot.data!;
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
              children: [
                Text(order.title, style: KyXText.title),
                const SizedBox(height: 12),
                KyXListSection(
                  children: [
                    _DetailLine(label: '状态', value: order.status.label),
                    _DetailLine(
                      label: '门店',
                      value: order.store.isEmpty ? '-' : order.store,
                    ),
                    _DetailLine(
                      label: '房号',
                      value: order.roomNo.isEmpty ? '-' : order.roomNo,
                    ),
                    _DetailLine(label: '类型', value: order.type),
                    _DetailLine(label: '优先级', value: order.priority),
                    _DetailLine(label: '客户情绪', value: order.customerEmotion),
                    _DetailLine(label: '处理人', value: order.assigneeName),
                    _DetailLine(label: '来源', value: order.source),
                    _DetailLine(
                      label: '创建时间',
                      value: _formatFullTime(order.createTime),
                    ),
                    if (order.acceptedTime != null)
                      _DetailLine(
                        label: '确认时间',
                        value: _formatFullTime(order.acceptedTime!),
                      ),
                    if (order.acceptedUserName.isNotEmpty)
                      _DetailLine(label: '确认人', value: order.acceptedUserName),
                    if (order.finishTime != null)
                      _DetailLine(
                        label: '完成时间',
                        value: _formatFullTime(order.finishTime!),
                      ),
                    if (order.finishUserName.isNotEmpty)
                      _DetailLine(label: '完成人', value: order.finishUserName),
                  ],
                ),
                const KyXSectionLabel(
                  '处理进度',
                  padding: EdgeInsets.fromLTRB(0, 16, 0, 8),
                ),
                KyXListSection(child: _OrderTimeline(order: order, messages: const [])),
                if (order.logs.isNotEmpty) ...[
                  const KyXSectionLabel(
                    '处理记录',
                    padding: EdgeInsets.fromLTRB(0, 16, 0, 8),
                  ),
                  KyXListSection(
                    children: _withDividers(
                      order.logs.map((log) => _OrderLogRow(log: log)).toList(),
                    ),
                  ),
                ],
                if (order.content.isNotEmpty) ...[
                  const KyXSectionLabel(
                    '内容',
                    padding: EdgeInsets.fromLTRB(0, 16, 0, 8),
                  ),
                  KyXListSection(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Text(order.content, style: KyXText.body),
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                if (order.status != HotelWorkOrderStatus.done)
                  Row(
                    children: [
                      if (order.status == HotelWorkOrderStatus.pending) ...[
                        Expanded(
                          child: OutlinedButton(
                            onPressed: _updating
                                ? null
                                : () => _updateStatus(
                                      order,
                                      HotelWorkOrderStatus.doing,
                                    ),
                            child: const Text('已收到'),
                          ),
                        ),
                        const SizedBox(width: 10),
                      ],
                      Expanded(
                        child: FilledButton(
                          onPressed: _updating
                              ? null
                              : () => _updateStatus(
                                    order,
                                    HotelWorkOrderStatus.done,
                                  ),
                          child: Text(_updating ? '处理中...' : '完成'),
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _RouteErrorState extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _RouteErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: KyXColors.red, size: 42),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center, style: KyXText.body),
            const SizedBox(height: 16),
            OutlinedButton(onPressed: onRetry, child: const Text('重试')),
          ],
        ),
      ),
    );
  }
}

class _OrderDetailSheet extends StatelessWidget {
  final HotelWorkOrder order;
  final List<HotelIntercomMessage> messages;
  final VoidCallback onDoing;
  final VoidCallback onDone;

  const _OrderDetailSheet({
    required this.order,
    this.messages = const [],
    required this.onDoing,
    required this.onDone,
  });

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.of(context).padding.bottom;
    final maxHeight = MediaQuery.of(context).size.height * 0.86;
    return SizedBox(
      height: maxHeight,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 12, 16, bottom + 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: KyXColors.line,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              order.title,
              style: KyXText.title,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 12),
            Expanded(
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    KyXListSection(
                      children: [
                        _DetailLine(label: '状态', value: order.status.label),
                        _DetailLine(
                          label: '房号',
                          value: order.roomNo.isEmpty ? '-' : order.roomNo,
                        ),
                        _DetailLine(label: '类型', value: order.type),
                        _DetailLine(label: '优先级', value: order.priority),
                        _DetailLine(
                          label: '客户情绪',
                          value: order.customerEmotion,
                        ),
                        _DetailLine(label: '处理人', value: order.assigneeName),
                        _DetailLine(label: '来源', value: order.source),
                        if (order.sourceRecordId != null)
                          _DetailLine(
                            label: '来源录音',
                            value:
                                '#${order.sourceRecordId} ${order.sourceRecordTitle}',
                          ),
                        _DetailLine(
                          label: '创建时间',
                          value: _formatFullTime(order.createTime),
                        ),
                        if (order.acceptedTime != null)
                          _DetailLine(
                            label: '确认时间',
                            value: _formatFullTime(order.acceptedTime!),
                          ),
                        if (order.acceptedUserName.isNotEmpty)
                          _DetailLine(
                            label: '确认人',
                            value: order.acceptedUserName,
                          ),
                        if (order.finishTime != null)
                          _DetailLine(
                            label: '完成时间',
                            value: _formatFullTime(order.finishTime!),
                          ),
                        if (order.finishUserName.isNotEmpty)
                          _DetailLine(
                            label: '完成人',
                            value: order.finishUserName,
                          ),
                      ],
                    ),
                    const KyXSectionLabel(
                      '处理进度',
                      padding: EdgeInsets.fromLTRB(0, 16, 0, 8),
                    ),
                    KyXListSection(
                      child: _OrderTimeline(order: order, messages: messages),
                    ),
                    if (order.logs.isNotEmpty) ...[
                      const KyXSectionLabel(
                        '处理记录',
                        padding: EdgeInsets.fromLTRB(0, 16, 0, 8),
                      ),
                      KyXListSection(
                        children: _withDividers(
                          order.logs
                              .map((log) => _OrderLogRow(log: log))
                              .toList(),
                        ),
                      ),
                    ],
                    if (order.content.isNotEmpty) ...[
                      const KyXSectionLabel(
                        '内容',
                        padding: EdgeInsets.fromLTRB(0, 16, 0, 8),
                      ),
                      KyXListSection(
                        child: Padding(
                          padding: const EdgeInsets.all(14),
                          child: Text(order.content, style: KyXText.body),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                if (order.status == HotelWorkOrderStatus.pending)
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        onDoing();
                        Navigator.pop(context);
                      },
                      child: const Text('已收到'),
                    ),
                  ),
                if (order.status == HotelWorkOrderStatus.pending)
                  const SizedBox(width: 10),
                if (order.status != HotelWorkOrderStatus.done)
                  Expanded(
                    child: FilledButton(
                      onPressed: () {
                        onDone();
                        Navigator.pop(context);
                      },
                      child: const Text('完成'),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _OrderLogRow extends StatelessWidget {
  final HotelWorkOrderLog log;

  const _OrderLogRow({required this.log});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  log.operatorName.isEmpty ? '系统记录' : log.operatorName,
                  style: KyXText.bodyStrong,
                ),
              ),
              if (log.createTime != null)
                Text(_formatTime(log.createTime!), style: KyXText.caption),
            ],
          ),
          if (log.content.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(log.content, style: KyXText.secondary),
          ],
        ],
      ),
    );
  }
}

class _OrderTimeline extends StatelessWidget {
  final HotelWorkOrder order;
  final List<HotelIntercomMessage> messages;

  const _OrderTimeline({required this.order, required this.messages});

  @override
  Widget build(BuildContext context) {
    final steps = <_TimelineStep>[
      _TimelineStep(
        title: '工单已创建',
        time: order.createTime,
        detail: '${order.source} · 已派给${order.assigneeName}',
        done: true,
      ),
      _TimelineStep(
        title: '已通知${order.assigneeName}',
        time: messages.isEmpty ? order.createTime : messages.last.createTime,
        detail: messages.isEmpty ? '系统自动通知' : messages.last.content,
        done: true,
      ),
      _TimelineStep(
        title: '${order.assigneeName}已收到',
        time: order.acceptedTime,
        detail: order.acceptedTime == null
            ? '等待确认'
            : '确认人：${order.acceptedUserName.isEmpty ? order.assigneeName : order.acceptedUserName}',
        done: order.acceptedTime != null,
      ),
      _TimelineStep(
        title: '工单已完成',
        time: order.finishTime,
        detail: order.finishTime == null
            ? '等待完成'
            : '完成人：${order.finishUserName.isEmpty ? order.assigneeName : order.finishUserName}',
        done: order.finishTime != null,
      ),
    ];
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Column(
        children: steps.asMap().entries.map((entry) {
          final step = entry.value;
          final last = entry.key == steps.length - 1;
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 20,
                child: Column(
                  children: [
                    Icon(
                      step.done
                          ? Icons.check_circle
                          : Icons.radio_button_unchecked,
                      size: 18,
                      color: step.done
                          ? KyXColors.green
                          : KyXColors.textTertiary,
                    ),
                    if (!last)
                      Container(width: 1, height: 38, color: KyXColors.line),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(step.title, style: KyXText.bodyStrong),
                          ),
                          if (step.time != null)
                            Text(
                              _formatTime(step.time!),
                              style: KyXText.caption,
                            ),
                        ],
                      ),
                      const SizedBox(height: 3),
                      Text(
                        step.detail,
                        style: KyXText.secondary,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ),
            ],
          );
        }).toList(),
      ),
    );
  }
}

class _TimelineStep {
  final String title;
  final DateTime? time;
  final String detail;
  final bool done;

  const _TimelineStep({
    required this.title,
    required this.time,
    required this.detail,
    required this.done,
  });
}

class _DetailLine extends StatelessWidget {
  final String label;
  final String value;

  const _DetailLine({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 11, 16, 11),
      child: Row(
        children: [
          SizedBox(width: 74, child: Text(label, style: KyXText.secondary)),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: KyXText.bodyStrong,
            ),
          ),
        ],
      ),
    );
  }
}

class _VoiceInputPanel extends StatelessWidget {
  final TextEditingController controller;
  final bool isRecording;
  final bool isRecognizing;
  final int seconds;
  final String? errorText;
  final VoidCallback onRecordTap;
  final VoidCallback onClear;

  const _VoiceInputPanel({
    required this.controller,
    required this.isRecording,
    required this.isRecognizing,
    required this.seconds,
    required this.errorText,
    required this.onRecordTap,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    final status = isRecognizing
        ? (errorText ?? '识别中')
        : isRecording
        ? '录音中 ${seconds <= 0 ? 1 : seconds}s'
        : controller.text.trim().isEmpty
        ? '点击录音'
        : '已识别，可编辑';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: isRecognizing ? null : onRecordTap,
                icon: Icon(
                  isRecording
                      ? Icons.stop_circle_outlined
                      : Icons.mic_none_outlined,
                ),
                label: Text(isRecording ? '停止录音' : '开始说话'),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(44),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Text(status, style: KyXText.secondary),
          ],
        ),
        const SizedBox(height: 10),
        TextField(
          controller: controller,
          minLines: 5,
          maxLines: 8,
          decoration: kyxInputDecoration(
            hintText: '录音识别后会显示在这里，可手动修正',
            suffixIcon: controller.text.trim().isEmpty
                ? null
                : IconButton(icon: const Icon(Icons.close), onPressed: onClear),
          ),
        ),
        if (errorText != null && errorText!.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(
            errorText!,
            style: KyXText.caption.copyWith(color: KyXColors.red),
          ),
        ],
      ],
    );
  }
}

bool _isReceptionProcessing(int? status) => status == 0 || status == 1;

enum _WorkOrderInputMode { manual, voice, record }

extension _WorkOrderInputModeText on _WorkOrderInputMode {
  String get label {
    switch (this) {
      case _WorkOrderInputMode.manual:
        return '手动录入';
      case _WorkOrderInputMode.voice:
        return '语音录入';
      case _WorkOrderInputMode.record:
        return '录音发送';
    }
  }

  IconData get icon {
    switch (this) {
      case _WorkOrderInputMode.manual:
        return Icons.edit_outlined;
      case _WorkOrderInputMode.voice:
        return Icons.mic_none_outlined;
      case _WorkOrderInputMode.record:
        return Icons.graphic_eq_outlined;
    }
  }
}

class _InputModeSwitch extends StatelessWidget {
  final _WorkOrderInputMode mode;
  final ValueChanged<_WorkOrderInputMode> onChanged;

  const _InputModeSwitch({required this.mode, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 40,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: KyXColors.bg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: KyXColors.line),
      ),
      child: Row(
        children: _WorkOrderInputMode.values.map((item) {
          return _ModeItem(
            label: item.label,
            icon: item.icon,
            selected: mode == item,
            onTap: () => onChanged(item),
          );
        }).toList(),
      ),
    );
  }
}

class _ModeItem extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  const _ModeItem({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(6),
        child: Container(
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: selected ? KyXColors.surface : Colors.transparent,
            borderRadius: BorderRadius.circular(6),
            boxShadow: selected
                ? const [BoxShadow(color: Color(0x12000000), blurRadius: 3)]
                : null,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 17,
                color: selected ? KyXColors.primary : KyXColors.textSecondary,
              ),
              const SizedBox(width: 5),
              Text(
                label,
                style: KyXText.secondary.copyWith(
                  color: selected ? KyXColors.primary : KyXColors.textSecondary,
                  fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AssigneePicker extends StatelessWidget {
  final Future<List<TencentImContact>> future;
  final TencentImContact? selected;
  final ValueChanged<TencentImContact> onChanged;

  const _AssigneePicker({
    required this.future,
    required this.selected,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<TencentImContact>>(
      future: future,
      builder: (context, snapshot) {
        final contacts = snapshot.data ?? const <TencentImContact>[];
        final subtitle = selected == null
            ? (snapshot.connectionState == ConnectionState.waiting
                  ? '加载处理中'
                  : '未分配，点击选择')
            : '${selected!.displayName}${selected!.imUserId.isEmpty ? '' : ' · ${selected!.imUserId}'}';
        return KyXListSection(
          child: KyXListRow(
            leading: const _PlainIcon(Icons.person_search_outlined),
            title: '处理人',
            subtitle: subtitle,
            trailing: const Icon(
              Icons.chevron_right,
              color: KyXColors.textTertiary,
            ),
            showDivider: false,
            onTap: () {
              if (snapshot.connectionState == ConnectionState.waiting) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(const SnackBar(content: Text('人员加载中，请稍候')));
                return;
              }
              if (contacts.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('未获取到本店人员，请检查通讯录')),
                );
                return;
              }
              _showAssigneeSelector(context, contacts);
            },
          ),
        );
      },
    );
  }

  Future<void> _showAssigneeSelector(
    BuildContext context,
    List<TencentImContact> contacts,
  ) async {
    final chosen = await _showContactPicker(
      context,
      contacts,
      selected,
      title: '选择处理人',
      emptyText: '暂无本店人员',
    );
    if (chosen != null) onChanged(chosen);
  }
}

class _ReceptionRecordPanel extends StatelessWidget {
  final Future<List<AiReceptionRecord>> future;
  final AiReceptionRecord? selected;
  final VoidCallback onRefresh;
  final ValueChanged<AiReceptionRecord> onSelected;
  final ValueChanged<AiReceptionRecord> onDelete;

  const _ReceptionRecordPanel({
    required this.future,
    required this.selected,
    required this.onRefresh,
    required this.onSelected,
    required this.onDelete,
  });

  Future<void> _showRecordActions(
    BuildContext context,
    AiReceptionRecord record,
  ) async {
    final action = await showModalBottomSheet<String>(
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
            Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: KyXColors.line,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 8),
            ListTile(
              leading: const Icon(
                Icons.check_circle_outline,
                color: KyXColors.primary,
              ),
              title: const Text('选择这条录音'),
              onTap: () => Navigator.pop(context, 'select'),
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline, color: KyXColors.red),
              title: const Text('删除录音'),
              onTap: () => Navigator.pop(context, 'delete'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (action == 'select') onSelected(record);
    if (action == 'delete') onDelete(record);
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<AiReceptionRecord>>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const KyXListSection(child: _EmptyRow(text: '录音加载中'));
        }
        final records = snapshot.data ?? const <AiReceptionRecord>[];
        if (records.isEmpty) {
          return KyXListSection(
            children: [
              const _EmptyRow(text: '暂无接待录音'),
              KyXListRow(
                leading: const _PlainIcon(Icons.refresh_outlined),
                title: '刷新录音',
                subtitle: '录音分析完成后可直接选择发送',
                showDivider: false,
                onTap: onRefresh,
              ),
            ],
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.36,
              child: KyXListSection(
                child: ListView.separated(
                  itemCount: records.length,
                  separatorBuilder: (_, __) => const Divider(
                    height: 1,
                    indent: 56,
                    color: KyXColors.lineSoft,
                  ),
                  itemBuilder: (context, index) {
                    final record = records[index];
                    final isSelected = selected?.id == record.id;
                    return KyXListRow(
                      onLongPress: () => _showRecordActions(context, record),
                      leading: _PlainIcon(
                        isSelected
                            ? Icons.check_circle
                            : Icons.graphic_eq_outlined,
                        color: isSelected
                            ? KyXColors.primary
                            : KyXColors.textSecondary,
                      ),
                      title: _receptionRecordTitle(record),
                      subtitle: _receptionRecordSubtitle(record),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (isSelected)
                            const Text('已选', style: KyXText.caption),
                          if (isSelected) const SizedBox(width: 6),
                          IconButton(
                            onPressed: () =>
                                _showRecordActions(context, record),
                            icon: const Icon(Icons.more_horiz, size: 20),
                            color: KyXColors.textTertiary,
                            tooltip: '更多操作',
                            visualDensity: VisualDensity.compact,
                            constraints: const BoxConstraints(
                              minWidth: 32,
                              minHeight: 32,
                            ),
                            padding: EdgeInsets.zero,
                          ),
                        ],
                      ),
                      showDivider: false,
                      onTap: () => onSelected(record),
                    );
                  },
                ),
              ),
            ),
            if (selected != null) ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                constraints: const BoxConstraints(maxHeight: 120),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: KyXColors.surface,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: KyXColors.line),
                ),
                child: SingleChildScrollView(
                  child: Text(
                    _receptionRecordPreview(selected!),
                    style: KyXText.secondary,
                  ),
                ),
              ),
            ],
          ],
        );
      },
    );
  }
}

String _receptionRecordTitle(AiReceptionRecord record) {
  final summary = record.summaryText?.trim();
  if (summary != null && summary.isNotEmpty) {
    return summary.length > 28 ? '${summary.substring(0, 28)}…' : summary;
  }
  final todo = record.todoTexts.isNotEmpty ? record.todoTexts.first.trim() : '';
  if (todo.isNotEmpty) {
    return todo.length > 28 ? '${todo.substring(0, 28)}…' : todo;
  }
  final text = record.transcriptText?.trim();
  if (text != null && text.isNotEmpty) {
    return text.length > 28 ? '${text.substring(0, 28)}…' : text;
  }
  return '接待录音 #${record.id ?? '-'}';
}

String _receptionRecordSubtitle(AiReceptionRecord record) {
  final parts = <String>[];
  if (record.createTime?.trim().isNotEmpty == true) {
    parts.add(record.createTime!.trim());
  }
  if (record.duration > 0) parts.add('${record.duration}s');
  if (record.customerEmotion?.trim().isNotEmpty == true) {
    parts.add('情绪 ${record.customerEmotion!.trim()}');
  }
  return parts.isEmpty ? '点击发送成工单' : parts.join(' · ');
}

String _receptionRecordPreview(AiReceptionRecord record) {
  final lines = <String>[];
  final summary = record.summaryText?.trim();
  if (summary != null && summary.isNotEmpty) lines.add(summary);
  for (final turn in record.dialogueTurns.take(3)) {
    lines.add('${turn.speakerLabel}：${turn.text}');
  }
  final text = record.transcriptText?.trim();
  if (lines.isEmpty && text != null && text.isNotEmpty) lines.add(text);
  return lines.join('\n');
}

class _TempIntercomSheet extends StatefulWidget {
  final String store;

  const _TempIntercomSheet({required this.store});

  @override
  State<_TempIntercomSheet> createState() => _TempIntercomSheetState();
}

class _TempIntercomSheetState extends State<_TempIntercomSheet> {
  final AudioRecorder _recorder = AudioRecorder();
  late Future<List<TencentImContact>> _contactsFuture;
  TencentImContact? _selected;
  bool _recording = false;
  bool _sending = false;
  int _seconds = 0;
  DateTime? _startedAt;
  Timer? _timer;
  String? _path;
  String? _error;

  @override
  void initState() {
    super.initState();
    _contactsFuture = _loadContacts();
  }

  @override
  void dispose() {
    _timer?.cancel();
    if (_recording) unawaited(_recorder.cancel());
    _recorder.dispose();
    super.dispose();
  }

  Future<List<TencentImContact>> _loadContacts() async {
    return _loadHotelContactsForStore(widget.store, requireIm: true);
  }

  Future<void> _toggle() async {
    if (_sending) return;
    if (_recording) {
      await _stopAndSend();
    } else {
      await _start();
    }
  }

  Future<void> _start() async {
    final target = _selected;
    if (target == null) {
      setState(() => _error = '请选择对讲接收人');
      return;
    }
    try {
      final ok = await _recorder.hasPermission();
      if (!ok) {
        setState(() => _error = '麦克风权限未开启');
        return;
      }
      final dir = await getTemporaryDirectory();
      final path =
          '${dir.path}${Platform.pathSeparator}hotel_intercom_${DateTime.now().millisecondsSinceEpoch}.m4a';
      await _recorder.start(
        const RecordConfig(
          encoder: AudioEncoder.aacLc,
          bitRate: 64000,
          sampleRate: 16000,
          numChannels: 1,
          echoCancel: true,
          noiseSuppress: true,
        ),
        path: path,
      );
      _timer?.cancel();
      _startedAt = DateTime.now();
      _timer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (!mounted || !_recording) return;
        final started = _startedAt;
        if (started == null) return;
        setState(
          () => _seconds = DateTime.now()
              .difference(started)
              .inSeconds
              .clamp(1, 999),
        );
      });
      setState(() {
        _path = path;
        _seconds = 0;
        _recording = true;
        _error = null;
      });
    } catch (_) {
      setState(() => _error = '对讲录音启动失败');
    }
  }

  Future<void> _stopAndSend() async {
    final target = _selected;
    if (target == null) return;
    setState(() => _sending = true);
    try {
      final duration = _currentSeconds();
      final stoppedPath = await _recorder.stop();
      _timer?.cancel();
      final path = stoppedPath ?? _path;
      setState(() => _recording = false);
      if (path == null || path.isEmpty || !File(path).existsSync()) {
        setState(() {
          _sending = false;
          _error = '对讲录音文件不可用';
        });
        return;
      }
      await ChatService().sendSoundMessage(
        conversationId: target.imUserId,
        soundPath: path,
        duration: duration <= 0 ? 1 : duration,
      );
      await HotelOperationsService.sendIntercom(
        store: widget.store,
        content:
            '对讲语音 ${duration <= 0 ? 1 : duration}s，已发送给${target.displayName}',
        targetRole: target.displayName,
      );
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _sending = false;
        _recording = false;
        _error = '对讲发送失败，请确认聊天已连接';
      });
    }
  }

  int _currentSeconds() {
    final started = _startedAt;
    if (started == null) return _seconds <= 0 ? 1 : _seconds;
    final value = DateTime.now().difference(started).inSeconds;
    return value <= 0 ? 1 : value;
  }

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.of(context).padding.bottom;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 12, 16, bottom + 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: KyXColors.line,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),
          const Text('临时对讲通知', style: KyXText.title),
          const SizedBox(height: 12),
          FutureBuilder<List<TencentImContact>>(
            future: _contactsFuture,
            builder: (context, snapshot) {
              final contacts = snapshot.data ?? const <TencentImContact>[];
              return KyXListSection(
                child: KyXListRow(
                  leading: const _PlainIcon(Icons.person_outline),
                  title: '接收人',
                  subtitle: _selected == null
                      ? '选择本店人员'
                      : _selected!.displayName,
                  trailing: const Icon(
                    Icons.chevron_right,
                    color: KyXColors.textTertiary,
                  ),
                  showDivider: false,
                  onTap: _recording
                      ? null
                      : () async {
                          if (snapshot.connectionState ==
                              ConnectionState.waiting) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('人员加载中，请稍候')),
                            );
                            return;
                          }
                          if (contacts.isEmpty) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('未获取到本店人员，请检查通讯录')),
                            );
                            return;
                          }
                          final chosen = await _showContactPicker(
                            context,
                            contacts,
                            _selected,
                            title: '选择接收人',
                            emptyText: '暂无本店可对讲人员',
                          );
                          if (chosen != null) {
                            setState(() => _selected = chosen);
                          }
                        },
                ),
              );
            },
          ),
          const SizedBox(height: 14),
          FilledButton.icon(
            onPressed: _sending ? null : _toggle,
            icon: Icon(
              _recording
                  ? Icons.stop_circle_outlined
                  : Icons.keyboard_voice_outlined,
            ),
            label: Text(
              _sending
                  ? '发送中'
                  : (_recording ? '松开/点击发送 ${_currentSeconds()}s' : '按下开始对讲'),
            ),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(48),
              backgroundColor: _recording ? KyXColors.red : KyXColors.primary,
            ),
          ),
          if (_error != null && _error!.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              _error!,
              style: KyXText.caption.copyWith(color: KyXColors.red),
            ),
          ],
        ],
      ),
    );
  }
}

Future<TencentImContact?> _showContactPicker(
  BuildContext context,
  List<TencentImContact> contacts,
  TencentImContact? selected, {
  String title = '选择接收人',
  String emptyText = '暂无本店人员',
}) {
  final searchController = TextEditingController();
  return showModalBottomSheet<TencentImContact>(
    context: context,
    isScrollControlled: true,
    backgroundColor: KyXColors.surface,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
    ),
    builder: (context) {
      final bottom = MediaQuery.of(context).padding.bottom;
      final height = MediaQuery.of(context).size.height * 0.78;
      var keyword = '';
      return StatefulBuilder(
        builder: (context, setSheetState) {
          final filtered = contacts
              .where((item) {
                final key = keyword.trim().toLowerCase();
                if (key.isEmpty) return true;
                return item.searchableText.contains(key) ||
                    _contactSubTitle(item).toLowerCase().contains(key);
              })
              .toList(growable: false);
          return SizedBox(
            height: height,
            child: Padding(
              padding: EdgeInsets.fromLTRB(16, 12, 16, bottom + 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 36,
                      height: 4,
                      decoration: BoxDecoration(
                        color: KyXColors.line,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(title, style: KyXText.title),
                  const SizedBox(height: 12),
                  TextField(
                    controller: searchController,
                    decoration: kyxInputDecoration(
                      hintText: '搜索姓名、部门',
                      prefixIcon: const Icon(Icons.search, size: 20),
                      suffixIcon: keyword.isEmpty
                          ? null
                          : IconButton(
                              icon: const Icon(Icons.close),
                              onPressed: () {
                                searchController.clear();
                                setSheetState(() => keyword = '');
                              },
                            ),
                    ),
                    onChanged: (value) => setSheetState(() => keyword = value),
                  ),
                  const SizedBox(height: 12),
                  Expanded(
                    child: filtered.isEmpty
                        ? KyXListSection(child: _EmptyRow(text: emptyText))
                        : KyXListSection(
                            child: ListView.separated(
                              primary: false,
                              physics: const AlwaysScrollableScrollPhysics(),
                              itemCount: filtered.length,
                              separatorBuilder: (_, __) => const Divider(
                                height: 1,
                                indent: 56,
                                color: KyXColors.lineSoft,
                              ),
                              itemBuilder: (context, index) {
                                final item = filtered[index];
                                final checked =
                                    selected?.oaUserId == item.oaUserId &&
                                    selected?.imUserId == item.imUserId;
                                return KyXListRow(
                                  leading: _PlainIcon(
                                    checked
                                        ? Icons.check_circle
                                        : Icons.person_outline,
                                    color: checked
                                        ? KyXColors.primary
                                        : KyXColors.textSecondary,
                                  ),
                                  title: item.displayName,
                                  subtitle: _contactSubTitle(item),
                                  showDivider: false,
                                  onLongPress: () =>
                                      _showContactDeptDetail(context, item),
                                  onTap: () => Navigator.of(context).pop(item),
                                );
                              },
                            ),
                          ),
                  ),
                ],
              ),
            ),
          );
        },
      );
    },
  ).whenComplete(searchController.dispose);
}

void _showContactDeptDetail(BuildContext context, TencentImContact item) {
  showModalBottomSheet<void>(
    context: context,
    backgroundColor: KyXColors.surface,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
    ),
    builder: (context) {
      final bottom = MediaQuery.of(context).padding.bottom;
      return Padding(
        padding: EdgeInsets.fromLTRB(16, 12, 16, bottom + 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: KyXColors.line,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(item.displayName, style: KyXText.title),
            const SizedBox(height: 12),
            KyXListSection(
              children: [
                _DetailLine(
                  label: '部门',
                  value: item.deptName?.trim().isNotEmpty == true
                      ? item.deptName!.trim()
                      : '-',
                ),
                _DetailLine(
                  label: 'OA用户',
                  value: item.oaUserId?.toString() ?? '-',
                ),
                _DetailLine(
                  label: 'IM',
                  value: item.imUserId.trim().isEmpty ? '未开通IM' : item.imUserId,
                ),
              ],
            ),
          ],
        ),
      );
    },
  );
}

String _contactSubTitle(TencentImContact item) {
  final parts = <String>[];
  if (item.deptName?.trim().isNotEmpty == true) {
    parts.add(item.deptName!.trim());
  }
  if (item.imUserId.trim().isEmpty) {
    parts.add('未开通IM');
  }
  if (parts.isEmpty) parts.add(item.imUserId);
  return parts.join(' · ');
}

Future<List<TencentImContact>> _loadHotelContactsForStore(
  String store, {
  bool requireIm = false,
}) async {
  final results = await Future.wait([
    ApiService.getSystemSimpleUsers(),
    ApiService.getSystemSimpleDepts(),
    ApiService.getTencentImContacts(limit: 1000),
  ]);
  final users = results[0] as List<SystemSimpleUser>;
  final depts = results[1] as List<SystemSimpleDept>;
  final imContacts = results[2] as List<TencentImContact>;
  final deptById = <int, SystemSimpleDept>{
    for (final dept in depts)
      if (dept.id != null) dept.id!: dept,
  };
  final storeDeptIds = _findStoreDeptIds(store, depts);
  if (storeDeptIds.isEmpty) return const [];
  final imByOaUserId = <int, TencentImContact>{
    for (final item in imContacts)
      if (item.oaUserId != null) item.oaUserId!: item,
  };
  final list = <TencentImContact>[];
  for (final user in users) {
    final userId = user.id;
    final deptId = user.deptId;
    if (userId == null || deptId == null) continue;
    if (!_isDeptInStore(deptId, storeDeptIds, deptById)) continue;
    final im = imByOaUserId[userId];
    final imUserId = im?.imUserId ?? '';
    if (requireIm && imUserId.trim().isEmpty) continue;
    final deptPath = _deptPathText(deptId, deptById);
    list.add(
      TencentImContact(
        id: im?.id,
        oaUserId: userId,
        tenantId: user.tenantId ?? im?.tenantId,
        oaUsername: user.nickname.trim().isNotEmpty
            ? user.nickname.trim()
            : user.username,
        ordersysUsername: im?.ordersysUsername ?? user.username,
        imUserId: imUserId,
        remark: im?.remark,
        deptId: deptId,
        deptName: deptPath.isNotEmpty ? deptPath : user.deptName,
        storeName: store,
      ),
    );
  }
  list.sort((a, b) {
    final ad = a.deptName ?? '';
    final bd = b.deptName ?? '';
    final cmp = ad.compareTo(bd);
    if (cmp != 0) return cmp;
    return a.displayName.compareTo(b.displayName);
  });
  return list;
}

Set<int> _findStoreDeptIds(String store, List<SystemSimpleDept> depts) {
  final aliases = _storeAliases(store);
  return depts
      .where((dept) {
        final name = dept.name.trim().toLowerCase();
        return aliases.any((alias) => alias.isNotEmpty && name.contains(alias));
      })
      .map((dept) => dept.id)
      .whereType<int>()
      .toSet();
}

Set<String> _storeAliases(String storeName) {
  final store = storeName.replaceAll('店', '').trim().toLowerCase();
  final aliases = <String>{storeName.toLowerCase(), store};
  if (storeName.contains('万达')) aliases.addAll(['万达', 'wanda']);
  if (storeName.contains('聚云')) aliases.addAll(['聚云', 'juyun']);
  if (storeName.contains('高新')) aliases.addAll(['高新', 'gaoxin']);
  return aliases.where((item) => item.isNotEmpty).toSet();
}

bool _isDeptInStore(
  int deptId,
  Set<int> storeDeptIds,
  Map<int, SystemSimpleDept> deptById,
) {
  var current = deptId;
  final visited = <int>{};
  while (current > 0 && visited.add(current)) {
    if (storeDeptIds.contains(current)) return true;
    final parent = deptById[current]?.parentId;
    if (parent == null || parent == current) break;
    current = parent;
  }
  return false;
}

String _deptPathText(int deptId, Map<int, SystemSimpleDept> deptById) {
  final names = <String>[];
  var current = deptId;
  final visited = <int>{};
  while (current > 0 && visited.add(current)) {
    final dept = deptById[current];
    if (dept == null) break;
    if (dept.name.trim().isNotEmpty) names.add(dept.name.trim());
    final parent = dept.parentId;
    if (parent == null || parent == current) break;
    current = parent;
  }
  return names.reversed.take(4).join(' / ');
}

class _EditWorkOrderSheet extends StatefulWidget {
  final HotelWorkOrder order;

  const _EditWorkOrderSheet({required this.order});

  @override
  State<_EditWorkOrderSheet> createState() => _EditWorkOrderSheetState();
}

class _EditWorkOrderSheetState extends State<_EditWorkOrderSheet> {
  late final TextEditingController _roomController;
  late final TextEditingController _titleController;
  late final TextEditingController _contentController;
  late String _type;
  late String _priority;
  late String _emotion;
  late Future<List<TencentImContact>> _contactsFuture;
  TencentImContact? _selectedAssignee;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final order = widget.order;
    _roomController = TextEditingController(text: order.roomNo);
    _titleController = TextEditingController(text: order.title);
    _contentController = TextEditingController(text: order.content);
    _type = HotelOperationsService.issueTypes.contains(order.type)
        ? order.type
        : HotelOperationsService.issueTypes.first;
    _priority = ['普通', '优先', '紧急'].contains(order.priority)
        ? order.priority
        : '普通';
    _emotion = ['平静', '着急', '不满', '生气'].contains(order.customerEmotion)
        ? order.customerEmotion
        : '平静';
    _contactsFuture = _loadContacts();
  }

  Future<List<TencentImContact>> _loadContacts() async {
    final contacts = await _loadHotelContactsForStore(widget.order.store);
    TencentImContact? matched;
    for (final item in contacts) {
      if (widget.order.assigneeUserId != null &&
          item.oaUserId == widget.order.assigneeUserId) {
        matched = item;
        break;
      }
      if (widget.order.assigneeImUserId.trim().isNotEmpty &&
          item.imUserId == widget.order.assigneeImUserId) {
        matched = item;
        break;
      }
      if (widget.order.assigneeName.trim().isNotEmpty &&
          item.displayName == widget.order.assigneeName) {
        matched = item;
        break;
      }
    }
    if (mounted) {
      setState(() => _selectedAssignee = matched ?? contacts.firstOrNull);
    } else {
      _selectedAssignee = matched ?? contacts.firstOrNull;
    }
    return contacts;
  }

  @override
  void dispose() {
    _roomController.dispose();
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_saving) return;
    final assignee = _selectedAssignee;
    if (assignee == null || assignee.oaUserId == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请选择本店处理人')));
      return;
    }
    setState(() => _saving = true);
    try {
      await HotelOperationsService.updateWorkOrder(
        id: widget.order.id,
        store: widget.order.store,
        roomNo: _roomController.text,
        title: _titleController.text,
        type: _type,
        priority: _priority,
        content: _contentController.text,
        source: widget.order.source,
        sourceRecordId: widget.order.sourceRecordId,
        sourceRecordTitle: widget.order.sourceRecordTitle,
        customerEmotion: _emotion,
        assigneeName: assignee.displayName,
        assigneeUserId: assignee.oaUserId!,
        assigneeImUserId: assignee.imUserId,
      ).timeout(const Duration(seconds: 15));
      if (mounted) Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('保存失败：${_cleanError(error)}')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.of(context).viewInsets.bottom;
    final maxHeight = MediaQuery.of(context).size.height * 0.88;
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(bottom: bottom),
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: maxHeight),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Center(
                  child: Container(
                    width: 36,
                    height: 4,
                    decoration: BoxDecoration(
                      color: KyXColors.line,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                const Text('编辑工单', style: KyXText.title),
                const SizedBox(height: 14),
                _AssigneePicker(
                  future: _contactsFuture,
                  selected: _selectedAssignee,
                  onChanged: (value) =>
                      setState(() => _selectedAssignee = value),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _roomController,
                  decoration: kyxInputDecoration(hintText: '房号'),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _titleController,
                  decoration: kyxInputDecoration(hintText: '标题'),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  value: _type,
                  decoration: kyxInputDecoration(hintText: '问题类型'),
                  items: HotelOperationsService.issueTypes
                      .map(
                        (item) =>
                            DropdownMenuItem(value: item, child: Text(item)),
                      )
                      .toList(),
                  onChanged: (value) => setState(() => _type = value ?? _type),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        value: _priority,
                        decoration: kyxInputDecoration(hintText: '优先级'),
                        items: const ['普通', '优先', '紧急']
                            .map(
                              (item) => DropdownMenuItem(
                                value: item,
                                child: Text(item),
                              ),
                            )
                            .toList(),
                        onChanged: (value) =>
                            setState(() => _priority = value ?? _priority),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        value: _emotion,
                        decoration: kyxInputDecoration(hintText: '客户情绪'),
                        items: const ['平静', '着急', '不满', '生气']
                            .map(
                              (item) => DropdownMenuItem(
                                value: item,
                                child: Text(item),
                              ),
                            )
                            .toList(),
                        onChanged: (value) =>
                            setState(() => _emotion = value ?? _emotion),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _contentController,
                  minLines: 3,
                  maxLines: 6,
                  decoration: kyxInputDecoration(hintText: '处理内容'),
                ),
                const SizedBox(height: 18),
                FilledButton(
                  onPressed: _saving ? null : _save,
                  style: FilledButton.styleFrom(
                    minimumSize: const Size.fromHeight(46),
                  ),
                  child: Text(_saving ? '保存中' : '保存修改'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _WorkOrderSheet extends StatefulWidget {
  final String store;
  final String? presetTitle;
  final String? presetType;

  const _WorkOrderSheet({
    required this.store,
    this.presetTitle,
    this.presetType,
  });

  @override
  State<_WorkOrderSheet> createState() => _WorkOrderSheetState();
}

class _WorkOrderSheetState extends State<_WorkOrderSheet> {
  static const int _recordingSampleRate = 16000;
  static const int _recordingChannels = 1;
  static const int _recordingBitsPerSample = 16;
  static const int _wavHeaderLength = 44;

  late final TextEditingController _titleController;
  late final TextEditingController _contentController;
  late final TextEditingController _roomController;
  late String _type;
  String _priority = '普通';
  String _emotion = '平静';
  final AudioRecorder _recorder = AudioRecorder();
  Timer? _recordTimer;
  StreamSubscription<Uint8List>? _recordingStreamSub;
  RandomAccessFile? _recordingFile;
  Future<void> _recordingWriteFuture = Future.value();
  int _recordingPcmBytes = 0;
  DateTime? _recordStartedAt;
  String? _recordedPath;
  String? _activeRecordPath;
  String? _voiceError;
  _WorkOrderInputMode _inputMode = _WorkOrderInputMode.manual;
  late Future<List<AiReceptionRecord>> _recordsFuture;
  late Future<List<TencentImContact>> _contactsFuture;
  AiReceptionRecord? _selectedRecord;
  TencentImContact? _selectedAssignee;
  bool _recording = false;
  bool _recognizing = false;
  int _recordSeconds = 0;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _type = widget.presetType ?? HotelOperationsService.issueTypes.first;
    _titleController = TextEditingController(text: widget.presetTitle ?? '');
    _contentController = TextEditingController();
    _roomController = TextEditingController();
    _recordsFuture = _loadReceptionRecords();
    _contactsFuture = _loadContacts();
  }

  @override
  void dispose() {
    _recordTimer?.cancel();
    unawaited(_recordingStreamSub?.cancel());
    unawaited(_recordingFile?.close());
    if (_recording) {
      _recorder.cancel();
    }
    _recorder.dispose();
    _deleteVoiceFile();
    _titleController.dispose();
    _contentController.dispose();
    _roomController.dispose();
    super.dispose();
  }

  Future<void> _toggleVoiceRecord() async {
    if (_recording) {
      await _stopVoiceRecord();
    } else {
      await _startVoiceRecord();
    }
  }

  Future<void> _startVoiceRecord() async {
    if (_saving || _recognizing) return;
    await _deleteVoiceFile();
    if (!mounted) return;
    setState(() {
      _voiceError = null;
      _recordSeconds = 0;
      _recordedPath = null;
    });
    try {
      final hasPermission = await _recorder.hasPermission();
      if (!hasPermission) {
        if (mounted) setState(() => _voiceError = '麦克风权限未开启');
        return;
      }
      final dir = await getTemporaryDirectory();
      final path =
          '${dir.path}${Platform.pathSeparator}hotel_order_${DateTime.now().millisecondsSinceEpoch}.wav';
      _activeRecordPath = path;
      _recordingPcmBytes = 0;
      _recordingWriteFuture = Future.value();
      _recordingFile = await File(path).open(mode: FileMode.write);
      await _recordingFile!.writeFrom(_buildWavHeader(0));
      final stream = await _recorder.startStream(
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: _recordingSampleRate,
          numChannels: _recordingChannels,
          echoCancel: true,
          noiseSuppress: true,
        ),
      );
      _recordingStreamSub = stream.listen(
        _handleVoiceAudioChunk,
        onError: (_) {
          if (mounted && _recording) setState(() => _voiceError = '录音流异常，请重试');
        },
      );
      _recordStartedAt = DateTime.now();
      _recordTimer?.cancel();
      _recordTimer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (!mounted || !_recording) return;
        final startedAt = _recordStartedAt;
        if (startedAt == null) return;
        setState(
          () => _recordSeconds = DateTime.now()
              .difference(startedAt)
              .inSeconds
              .clamp(1, 999),
        );
      });
      if (mounted) setState(() => _recording = true);
    } catch (_) {
      if (mounted) setState(() => _voiceError = '录音启动失败');
    }
  }

  Future<void> _stopVoiceRecord() async {
    if (!_recording) return;
    setState(() => _voiceError = null);
    try {
      final duration = _currentVoiceSeconds();
      await _recorder.stop();
      await _recordingStreamSub?.cancel();
      _recordingStreamSub = null;
      await _recordingWriteFuture;
      final file = _recordingFile;
      _recordingFile = null;
      if (file != null) {
        await file.setPosition(0);
        await file.writeFrom(_buildWavHeader(_recordingPcmBytes));
        await file.close();
      }
      _recordTimer?.cancel();
      final path = _activeRecordPath;
      if (path == null ||
          path.isEmpty ||
          !File(path).existsSync() ||
          File(path).lengthSync() <= _wavHeaderLength) {
        if (mounted) {
          setState(() {
            _recording = false;
            _voiceError = '录音文件不可用';
          });
        }
        return;
      }
      if (mounted) {
        setState(() {
          _recording = false;
          _recordedPath = path;
          _recordSeconds = duration <= 0 ? 1 : duration;
        });
      }
      await _recognizeVoice(path, duration <= 0 ? 1 : duration);
    } catch (_) {
      if (mounted) {
        setState(() {
          _recording = false;
          _voiceError = '录音停止失败';
        });
      }
    }
  }

  void _handleVoiceAudioChunk(Uint8List chunk) {
    final file = _recordingFile;
    if (file == null || chunk.isEmpty) return;
    final data = Uint8List.fromList(chunk);
    _recordingPcmBytes += data.lengthInBytes;
    _recordingWriteFuture = _recordingWriteFuture
        .then((_) async {
          await file.writeFrom(data);
        })
        .catchError((_) {});
  }

  Uint8List _buildWavHeader(int dataLength) {
    final header = Uint8List(_wavHeaderLength);
    final data = ByteData.view(header.buffer);
    void writeAscii(int offset, String value) {
      for (var i = 0; i < value.length; i++) {
        header[offset + i] = value.codeUnitAt(i);
      }
    }

    final byteRate =
        _recordingSampleRate *
        _recordingChannels *
        _recordingBitsPerSample ~/
        8;
    final blockAlign = _recordingChannels * _recordingBitsPerSample ~/ 8;
    writeAscii(0, 'RIFF');
    data.setUint32(4, 36 + dataLength, Endian.little);
    writeAscii(8, 'WAVE');
    writeAscii(12, 'fmt ');
    data.setUint32(16, 16, Endian.little);
    data.setUint16(20, 1, Endian.little);
    data.setUint16(22, _recordingChannels, Endian.little);
    data.setUint32(24, _recordingSampleRate, Endian.little);
    data.setUint32(28, byteRate, Endian.little);
    data.setUint16(32, blockAlign, Endian.little);
    data.setUint16(34, _recordingBitsPerSample, Endian.little);
    writeAscii(36, 'data');
    data.setUint32(40, dataLength, Endian.little);
    return header;
  }

  Future<void> _recognizeVoice(String path, int duration) async {
    if (_recognizing) return;
    setState(() {
      _recognizing = true;
      _voiceError = null;
    });
    try {
      final id = await AiReceptionService.uploadRecording(
        path: path,
        duration: duration,
      );
      AiReceptionRecord? record;
      for (var i = 0; i < 20; i++) {
        if (i > 0) await Future.delayed(const Duration(seconds: 2));
        record = await AiReceptionService.getRecord(id);
        final text = record.transcriptText?.trim();
        if (text != null && text.isNotEmpty) break;
        if (!_isReceptionProcessing(record.status)) break;
        if (mounted) {
          setState(() => _voiceError = '识别中，已等待 ${i * 2}s');
        }
      }
      final text = record?.transcriptText?.trim();
      if (text == null || text.isEmpty) {
        if (mounted) {
          final stillProcessing = _isReceptionProcessing(record?.status);
          setState(
            () => _voiceError = stillProcessing
                ? '识别还在处理中，请稍后重试或先手动输入'
                : (record?.errorMessage ?? '未识别到文字，请重录或手动输入'),
          );
        }
        return;
      }
      final analysis = HotelOperationsService.analyzeFrontDeskSpeech(text);
      if (!mounted) return;
      setState(() {
        _contentController.text = text;
        _roomController.text = analysis.roomNo;
        _titleController.text = analysis.title;
        _type = analysis.type;
        _priority = analysis.priority;
        _emotion = analysis.customerEmotion;
      });
    } catch (error) {
      if (mounted) setState(() => _voiceError = '语音识别失败');
    } finally {
      if (mounted) setState(() => _recognizing = false);
    }
  }

  int _currentVoiceSeconds() {
    final startedAt = _recordStartedAt;
    if (startedAt == null) return _recordSeconds <= 0 ? 1 : _recordSeconds;
    final seconds = DateTime.now().difference(startedAt).inSeconds;
    return seconds <= 0 ? 1 : seconds;
  }

  Future<void> _deleteVoiceFile() async {
    await _recordingStreamSub?.cancel();
    _recordingStreamSub = null;
    await _recordingFile?.close();
    _recordingFile = null;
    _recordingPcmBytes = 0;
    final paths = {_recordedPath, _activeRecordPath};
    for (final path in paths) {
      if (path == null || path.isEmpty) continue;
      try {
        final file = File(path);
        if (file.existsSync()) await file.delete();
      } catch (_) {}
    }
    _recordedPath = null;
    _activeRecordPath = null;
  }

  Future<List<AiReceptionRecord>> _loadReceptionRecords() async {
    final records = await AiReceptionService.getMyRecords();
    records.sort((a, b) => (b.id ?? 0).compareTo(a.id ?? 0));
    return records.take(30).toList(growable: false);
  }

  Future<List<TencentImContact>> _loadContacts() async {
    final list = await _loadHotelContactsForStore(widget.store);
    if (mounted && _selectedAssignee == null && list.isNotEmpty) {
      setState(() => _selectedAssignee = list.first);
    }
    return list;
  }

  void _useReceptionRecord(AiReceptionRecord record) {
    final content = _buildRecordOrderContent(record);
    final analysis = HotelOperationsService.analyzeFrontDeskSpeech(content);
    setState(() {
      _selectedRecord = record;
      _contentController.text = content;
      _roomController.text = analysis.roomNo;
      _titleController.text = analysis.title;
      _type = analysis.type;
      _priority = analysis.priority;
      _emotion =
          _normalizeEmotion(record.customerEmotion) ?? analysis.customerEmotion;
    });
  }

  Future<void> _deleteReceptionRecord(AiReceptionRecord record) async {
    final id = record.id;
    if (id == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除录音'),
        content: Text('确定删除“${_recordTitle(record)}”？删除后不能再用它创建工单。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: KyXColors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await AiReceptionService.deleteRecord(id);
      if (!mounted) return;
      setState(() {
        if (_selectedRecord?.id == id) {
          _selectedRecord = null;
          _contentController.clear();
          _titleController.clear();
          _roomController.clear();
          _type = widget.presetType ?? HotelOperationsService.issueTypes.first;
          _priority = '普通';
          _emotion = '平静';
        }
        _recordsFuture = _loadReceptionRecords();
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('录音已删除')));
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('删除失败：${_cleanError(error)}')));
    }
  }

  String _recordTitle(AiReceptionRecord record) {
    final summary = record.summaryText?.trim();
    if (summary != null && summary.isNotEmpty) {
      return summary.length > 24 ? '${summary.substring(0, 24)}…' : summary;
    }
    final todo = record.todoTexts.isNotEmpty
        ? record.todoTexts.first.trim()
        : '';
    if (todo.isNotEmpty) {
      return todo.length > 24 ? '${todo.substring(0, 24)}…' : todo;
    }
    final text = record.transcriptText?.trim();
    if (text != null && text.isNotEmpty) {
      return text.length > 24 ? '${text.substring(0, 24)}…' : text;
    }
    return '接待录音 #${record.id ?? '-'}';
  }

  String _buildRecordOrderContent(AiReceptionRecord record) {
    final lines = <String>[];
    final summary = record.summaryText?.trim();
    if (summary != null && summary.isNotEmpty) lines.add('摘要：$summary');
    if (record.customerEmotion?.trim().isNotEmpty == true) {
      lines.add('客户情绪：${record.customerEmotion!.trim()}');
    }
    if (record.todoTexts.isNotEmpty) {
      lines.add('待办：${record.todoTexts.join('；')}');
    }
    if (record.customerNeedTexts.isNotEmpty) {
      lines.add('客户需求：${record.customerNeedTexts.join('；')}');
    }
    final turns = record.dialogueTurns;
    if (turns.isNotEmpty) {
      lines.add('对话记录：');
      for (final turn in turns.take(12)) {
        lines.add('${turn.speakerLabel}：${turn.text}');
      }
    }
    final transcript = record.transcriptText?.trim();
    if (transcript != null && transcript.isNotEmpty) {
      lines.add('转写原文：$transcript');
    }
    if (lines.isEmpty) lines.add('接待录音 #${record.id ?? '-'}');
    return lines.join('\n');
  }

  String? _normalizeEmotion(String? raw) {
    final text = raw?.trim();
    if (text == null || text.isEmpty) return null;
    if (text.contains('怒') || text.contains('生气')) return '生气';
    if (text.contains('急')) return '着急';
    if (text.contains('不满') || text.contains('抱怨')) return '不满';
    return '平静';
  }

  Future<void> _sendWorkOrderIm(
    HotelWorkOrder order,
    TencentImContact assignee,
  ) async {
    final imUserId = assignee.imUserId.trim();
    if (imUserId.isEmpty) return;
    final room = order.roomNo.isEmpty ? '' : '$order.roomNo房 ';
    final text = [
      '【酒店工单】$room${order.title}',
      '类型：${order.type}｜优先级：${order.priority}',
      '情绪：${order.customerEmotion}',
      order.content,
      '请确认收到，处理完成后点完成。',
    ].where((item) => item.trim().isNotEmpty).join('\n');
    try {
      await ChatService()
          .sendMessage(imUserId, text)
          .timeout(const Duration(seconds: 6));
    } catch (_) {
      // IM 通知失败不能阻塞服务端工单和 OA 待办。
    }
  }

  Future<void> _save() async {
    if (_saving || _recording || _recognizing) return;
    final assignee = _selectedAssignee;
    if (assignee == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请选择实际处理人')));
      return;
    }
    if (_inputMode == _WorkOrderInputMode.voice &&
        _contentController.text.trim().isEmpty) {
      setState(() => _voiceError = '请先录音识别，或输入识别文字');
      return;
    }
    if (_inputMode == _WorkOrderInputMode.record && _selectedRecord == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请选择一条接待录音')));
      return;
    }
    setState(() {
      _saving = true;
      _voiceError = null;
    });
    try {
      if (_inputMode == _WorkOrderInputMode.voice) {
        final text = _contentController.text.trim();
        final analysis = HotelOperationsService.analyzeFrontDeskSpeech(text);
        final createdOrder = await HotelOperationsService.createWorkOrder(
          store: widget.store,
          title: analysis.title,
          type: analysis.type,
          priority: analysis.priority,
          content: analysis.actionText,
          roomNo: analysis.roomNo,
          customerEmotion: analysis.customerEmotion,
          assigneeName: assignee.displayName,
          assigneeUserId: assignee.oaUserId,
          assigneeImUserId: assignee.imUserId,
          source: '语音录入',
        ).timeout(const Duration(seconds: 15));
        await _sendWorkOrderIm(createdOrder, assignee);
      } else if (_inputMode == _WorkOrderInputMode.record) {
        final record = _selectedRecord!;
        final content = _contentController.text.trim().isEmpty
            ? _buildRecordOrderContent(record)
            : _contentController.text.trim();
        final createdOrder = await HotelOperationsService.createWorkOrder(
          store: widget.store,
          title: _titleController.text.trim().isEmpty
              ? _recordTitle(record)
              : _titleController.text.trim(),
          type: _type,
          priority: _priority,
          content: content,
          roomNo: _roomController.text,
          customerEmotion: _emotion,
          assigneeName: assignee.displayName,
          assigneeUserId: assignee.oaUserId,
          assigneeImUserId: assignee.imUserId,
          source: '接待录音',
          sourceRecordId: record.id,
          sourceRecordTitle: _recordTitle(record),
        ).timeout(const Duration(seconds: 15));
        await _sendWorkOrderIm(createdOrder, assignee);
      } else {
        final createdOrder = await HotelOperationsService.createWorkOrder(
          store: widget.store,
          title: _titleController.text,
          type: _type,
          priority: _priority,
          content: _contentController.text,
          roomNo: _roomController.text,
          customerEmotion: _emotion,
          assigneeName: assignee.displayName,
          assigneeUserId: assignee.oaUserId,
          assigneeImUserId: assignee.imUserId,
          source: '手动创建',
        ).timeout(const Duration(seconds: 15));
        await _sendWorkOrderIm(createdOrder, assignee);
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _voiceError = _inputMode == _WorkOrderInputMode.voice
            ? '创建失败，请重试或切换手动录入'
            : null;
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('工单创建失败，请重试')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottom =
        MediaQuery.of(context).viewInsets.bottom +
        MediaQuery.of(context).padding.bottom;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 12, 16, bottom + 16),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: KyXColors.line,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text('创建工单', style: KyXText.title),
            const SizedBox(height: 14),
            _InputModeSwitch(
              mode: _inputMode,
              onChanged: (value) => setState(() => _inputMode = value),
            ),
            const SizedBox(height: 14),
            _AssigneePicker(
              future: _contactsFuture,
              selected: _selectedAssignee,
              onChanged: (value) => setState(() => _selectedAssignee = value),
            ),
            const SizedBox(height: 14),
            if (_inputMode == _WorkOrderInputMode.voice)
              _VoiceInputPanel(
                controller: _contentController,
                isRecording: _recording,
                isRecognizing: _recognizing,
                seconds: _recordSeconds,
                errorText: _voiceError,
                onRecordTap: _toggleVoiceRecord,
                onClear: () => setState(() {
                  _contentController.clear();
                  _voiceError = null;
                }),
              )
            else if (_inputMode == _WorkOrderInputMode.record)
              _ReceptionRecordPanel(
                future: _recordsFuture,
                selected: _selectedRecord,
                onRefresh: () => setState(() {
                  _recordsFuture = _loadReceptionRecords();
                }),
                onSelected: _useReceptionRecord,
                onDelete: _deleteReceptionRecord,
              )
            else ...[
              TextField(
                controller: _roomController,
                decoration: kyxInputDecoration(hintText: '房号'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _titleController,
                decoration: kyxInputDecoration(hintText: '标题'),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                value: _type,
                decoration: kyxInputDecoration(hintText: '问题类型'),
                items: HotelOperationsService.issueTypes
                    .map(
                      (item) =>
                          DropdownMenuItem(value: item, child: Text(item)),
                    )
                    .toList(),
                onChanged: (value) => setState(() => _type = value ?? _type),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      value: _priority,
                      decoration: kyxInputDecoration(hintText: '优先级'),
                      items: const ['普通', '优先', '紧急']
                          .map(
                            (item) => DropdownMenuItem(
                              value: item,
                              child: Text(item),
                            ),
                          )
                          .toList(),
                      onChanged: (value) =>
                          setState(() => _priority = value ?? _priority),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      value: _emotion,
                      decoration: kyxInputDecoration(hintText: '客户情绪'),
                      items: const ['平静', '着急', '不满', '生气']
                          .map(
                            (item) => DropdownMenuItem(
                              value: item,
                              child: Text(item),
                            ),
                          )
                          .toList(),
                      onChanged: (value) =>
                          setState(() => _emotion = value ?? _emotion),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _contentController,
                minLines: 3,
                maxLines: 5,
                decoration: kyxInputDecoration(hintText: '处理内容'),
              ),
            ],
            const SizedBox(height: 18),
            FilledButton(
              onPressed: (_saving || _recording || _recognizing) ? null : _save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(46),
              ),
              child: Text(_saving ? '提交中' : '创建并通知'),
            ),
          ],
        ),
      ),
    );
  }
}

List<Widget> _withDividers(List<Widget> rows) {
  final result = <Widget>[];
  for (var i = 0; i < rows.length; i++) {
    result.add(rows[i]);
    if (i != rows.length - 1) {
      result.add(
        const Divider(height: 1, indent: 56, color: KyXColors.lineSoft),
      );
    }
  }
  return result;
}

String _formatFullTime(DateTime time) {
  String two(int v) => v.toString().padLeft(2, '0');
  return '${time.year}-${two(time.month)}-${two(time.day)} ${two(time.hour)}:${two(time.minute)}';
}

String _formatTime(DateTime time) {
  String two(int v) => v.toString().padLeft(2, '0');
  return '${two(time.month)}-${two(time.day)} ${two(time.hour)}:${two(time.minute)}';
}

String _cleanError(Object? error) {
  if (error == null) return '未知错误';
  final text = error.toString().trim();
  return text
      .replaceFirst(RegExp(r'^Exception:\s*'), '')
      .replaceFirst(RegExp(r'^ApiException:\s*'), '');
}
