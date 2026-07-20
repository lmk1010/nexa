import 'package:flutter/material.dart';

import '../services/bpm_service.dart';
import '../services/oa_chat_share.dart';
import '../widgets/kyx_design.dart';
import '../widgets/oa_chat_share_sheet.dart';

const int _bpmButtonApprove = 1;
const int _bpmButtonReject = 2;
const int _bpmButtonTransfer = 3;
const int _bpmButtonDelegate = 4;
const int _bpmButtonAddSign = 5;
const int _bpmButtonReturn = 6;
const int _bpmButtonCopy = 7;

class BpmApprovalPage extends StatefulWidget {
  const BpmApprovalPage({super.key});

  @override
  State<BpmApprovalPage> createState() => _BpmApprovalPageState();
}

class _BpmApprovalPageState extends State<BpmApprovalPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  final TextEditingController _keywordController = TextEditingController();
  final FocusNode _searchFocus = FocusNode();

  int _activeTab = 0;
  List<BpmTaskItem> _todoItems = const [];
  List<BpmTaskItem> _doneItems = const [];
  int _todoTotal = 0;
  int _doneTotal = 0;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this)
      ..addListener(_handleTabChanged);
    _load();
  }

  @override
  void dispose() {
    _tabController
      ..removeListener(_handleTabChanged)
      ..dispose();
    _keywordController.dispose();
    _searchFocus.dispose();
    super.dispose();
  }

  void _handleTabChanged() {
    if (_tabController.indexIsChanging) return;
    if (_activeTab == _tabController.index) return;
    setState(() => _activeTab = _tabController.index);
    _load();
  }

  Future<void> _load({bool quiet = false}) async {
    if (!quiet) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }

    try {
      final keyword = _keywordController.text.trim();
      final page = _activeTab == 0
          ? await BpmService.getTodoTasks(
              pageNo: 1,
              pageSize: 50,
              keyword: keyword,
            )
          : await BpmService.getDoneTasks(
              pageNo: 1,
              pageSize: 50,
              keyword: keyword,
            );
      if (!mounted) return;
      setState(() {
        if (_activeTab == 0) {
          _todoItems = page.list;
          _todoTotal = page.total;
        } else {
          _doneItems = page.list;
          _doneTotal = page.total;
        }
        _loading = false;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = _cleanError(error);
      });
    }
  }

  Future<void> _openDetail(BpmTaskItem task) async {
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) =>
            BpmApprovalDetailPage(task: task, readOnly: _activeTab == 1),
      ),
    );
    if (changed == true && mounted) {
      await _load(quiet: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = _activeTab == 0 ? _todoItems : _doneItems;
    final total = _activeTab == 0 ? _todoTotal : _doneTotal;
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '审批中心',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(42),
          child: Container(
            alignment: Alignment.centerLeft,
            decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: KyXColors.line)),
            ),
            child: TabBar(
              controller: _tabController,
              labelColor: KyXColors.primary,
              unselectedLabelColor: KyXColors.textSecondary,
              indicatorColor: KyXColors.primary,
              indicatorSize: TabBarIndicatorSize.label,
              labelStyle: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w800,
              ),
              unselectedLabelStyle: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
              ),
              tabs: const [
                Tab(text: '待办'),
                Tab(text: '已办'),
              ],
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          _BpmSearchBar(
            keywordController: _keywordController,
            searchFocus: _searchFocus,
            total: total,
            tabName: _activeTab == 0 ? '待处理' : '已处理',
            onSearch: () {
              _searchFocus.unfocus();
              _load();
            },
          ),
          Expanded(child: _buildBody(items)),
        ],
      ),
    );
  }

  Widget _buildBody(List<BpmTaskItem> items) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _BpmErrorState(message: _error!, onRetry: _load);
    }

    return RefreshIndicator(
      onRefresh: () => _load(quiet: true),
      child: items.isEmpty
          ? ListView(
              children: [
                const SizedBox(height: 120),
                _BpmEmptyState(text: _activeTab == 0 ? '暂无待办审批' : '暂无已办记录'),
              ],
            )
          : ListView.separated(
              padding: const EdgeInsets.only(bottom: 24),
              itemCount: items.length,
              separatorBuilder: (_, __) => const Divider(
                height: 1,
                indent: 16,
                color: KyXColors.lineSoft,
              ),
              itemBuilder: (context, index) {
                return _BpmTaskRow(
                  task: items[index],
                  done: _activeTab == 1,
                  onTap: () => _openDetail(items[index]),
                );
              },
            ),
    );
  }
}

class BpmApprovalDetailPage extends StatefulWidget {
  final BpmTaskItem task;
  final bool readOnly;

  const BpmApprovalDetailPage({
    super.key,
    required this.task,
    required this.readOnly,
  });

  @override
  State<BpmApprovalDetailPage> createState() => _BpmApprovalDetailPageState();
}

class _BpmApprovalDetailPageState extends State<BpmApprovalDetailPage> {
  BpmApprovalDetail? _detail;
  List<BpmUser>? _userCache;
  bool _loading = true;
  String? _error;
  String? _actionKey;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final processInstanceId = widget.task.processInstanceId.trim();
      if (processInstanceId.isEmpty) {
        throw Exception('流程实例缺失，无法加载审批详情');
      }
      final detail = await BpmService.getApprovalDetail(
        processInstanceId: processInstanceId,
        taskId: widget.task.id,
      );
      if (!mounted) return;
      setState(() {
        _detail = detail;
        _loading = false;
        _error = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = _cleanError(error);
      });
    }
  }

  Future<void> _shareToChat() async {
    final sent = await showOaChatShareSheet(context, _buildBpmSharePayload());
    if (!mounted || sent != true) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('已发送到会话')));
  }

  OaChatSharePayload _buildBpmSharePayload() {
    final detail = _detail;
    final task = detail?.todoTask ?? widget.task;
    final process = detail?.processInstance ?? widget.task.processInstance;
    final summary = process?.summary.isNotEmpty == true
        ? process!.summary
        : widget.task.summary;
    final variableSummary = summary.isEmpty
        ? _formVariableSummary(
            process?.formVariables.isNotEmpty == true
                ? process!.formVariables
                : task.formVariables,
          )
        : const <BpmSummaryItem>[];
    final displaySummary = (summary.isNotEmpty ? summary : variableSummary)
        .map(
          (item) => BpmSummaryItem(
            key: _fieldLabel(item.key),
            value: _displayValue(item.value),
          ),
        )
        .where((item) => item.value.trim().isNotEmpty && item.value != '-')
        .take(4)
        .toList();

    return OaChatSharePayload(
      module: '审批',
      objectType: 'bpm',
      objectId: task.processInstanceId,
      title: task.processName,
      status: _processStatusLabel(
        detail?.status ?? process?.status ?? task.status,
      ),
      extraData: {
        'taskId': task.id,
        'processInstanceId': task.processInstanceId,
      },
      fields: [
        OaChatShareField(label: '当前节点', value: task.name),
        OaChatShareField(label: '发起人', value: task.starterName),
        OaChatShareField(
          label: '创建时间',
          value: _formatDateTime(process?.startTime ?? task.createTime),
        ),
        for (final item in displaySummary)
          OaChatShareField(label: item.key, value: item.value),
      ],
      summary: _summaryText(displaySummary),
    );
  }

  List<_BpmDetailAction> _buildActions() {
    if (widget.readOnly) return const [];
    final task = _detail?.todoTask ?? widget.task;
    if (!task.canOperate) return const [];

    final actions = <_BpmDetailAction>[];
    if (task.isButtonEnabled(_bpmButtonCopy)) {
      actions.add(
        _BpmDetailAction(
          key: 'copy',
          label: task.buttonLabel(_bpmButtonCopy, '抄送'),
          icon: Icons.ios_share_outlined,
          handler: () => _copy(task),
        ),
      );
    }
    if (task.isButtonEnabled(_bpmButtonTransfer)) {
      actions.add(
        _BpmDetailAction(
          key: 'transfer',
          label: task.buttonLabel(_bpmButtonTransfer, '转办'),
          icon: Icons.swap_horiz,
          handler: () => _transfer(task),
        ),
      );
    }
    if (task.isButtonEnabled(_bpmButtonDelegate)) {
      actions.add(
        _BpmDetailAction(
          key: 'delegate',
          label: task.buttonLabel(_bpmButtonDelegate, '委派'),
          icon: Icons.person_add_alt_1_outlined,
          handler: () => _delegate(task),
        ),
      );
    }
    if (task.isButtonEnabled(_bpmButtonAddSign)) {
      actions.add(
        _BpmDetailAction(
          key: 'addSign',
          label: task.buttonLabel(_bpmButtonAddSign, '加签'),
          icon: Icons.playlist_add,
          handler: () => _addSign(task),
        ),
      );
    }
    if (task.children.isNotEmpty) {
      actions.add(
        _BpmDetailAction(
          key: 'deleteSign',
          label: '减签',
          icon: Icons.playlist_remove,
          handler: () => _deleteSign(task),
        ),
      );
    }
    if (task.isButtonEnabled(_bpmButtonReturn)) {
      actions.add(
        _BpmDetailAction(
          key: 'return',
          label: task.buttonLabel(_bpmButtonReturn, '退回'),
          icon: Icons.keyboard_return,
          danger: true,
          handler: () => _returnTask(task),
        ),
      );
    }
    if (task.isButtonEnabled(_bpmButtonReject)) {
      actions.add(
        _BpmDetailAction(
          key: 'reject',
          label: task.buttonLabel(_bpmButtonReject, '拒绝'),
          icon: Icons.close,
          danger: true,
          handler: () => _reject(task),
        ),
      );
    }
    if (task.isButtonEnabled(_bpmButtonApprove)) {
      actions.add(
        _BpmDetailAction(
          key: 'approve',
          label: task.buttonLabel(_bpmButtonApprove, '通过'),
          icon: Icons.check,
          primary: true,
          handler: () => _approve(task),
        ),
      );
    }
    return actions;
  }

  Future<void> _approve(BpmTaskItem task) async {
    final reason = await _inputText(
      title: '审批通过',
      hintText: task.reasonRequire ? '请输入审批意见' : '审批意见（选填）',
      required: task.reasonRequire,
    );
    if (reason == null) return;
    await _runAction(
      'approve',
      () => BpmService.approveTask(taskId: task.id, reason: reason),
      successText: '审批已通过',
    );
  }

  Future<void> _reject(BpmTaskItem task) async {
    final reason = await _inputText(
      title: '审批驳回',
      hintText: '请输入驳回原因',
      required: true,
    );
    if (reason == null) return;
    await _runAction(
      'reject',
      () => BpmService.rejectTask(taskId: task.id, reason: reason),
      successText: '审批已驳回',
    );
  }

  Future<void> _copy(BpmTaskItem task) async {
    final result = await _showUserActionSheet(
      title: '抄送知会',
      userLabel: '选择知会人',
      confirmText: task.buttonLabel(_bpmButtonCopy, '抄送'),
      multi: true,
      reasonHint: '补充说明（选填）',
      reasonRequired: false,
    );
    if (result == null) return;
    await _runAction(
      'copy',
      () => BpmService.copyTask(
        taskId: task.id,
        copyUserIds: result.userIds,
        reason: result.reason,
      ),
      successText: '已抄送给 ${result.userIds.length} 人',
      closeOnSuccess: false,
    );
  }

  Future<void> _transfer(BpmTaskItem task) async {
    final result = await _showUserActionSheet(
      title: '转办审批',
      userLabel: '新审批人',
      confirmText: task.buttonLabel(_bpmButtonTransfer, '转办'),
      multi: false,
      reasonHint: '请输入转办原因',
      reasonRequired: true,
    );
    if (result == null) return;
    await _runAction(
      'transfer',
      () => BpmService.transferTask(
        taskId: task.id,
        assigneeUserId: result.userIds.first,
        reason: result.reason,
      ),
      successText: '审批已转办',
    );
  }

  Future<void> _delegate(BpmTaskItem task) async {
    final result = await _showUserActionSheet(
      title: '委派审批',
      userLabel: '接收人',
      confirmText: task.buttonLabel(_bpmButtonDelegate, '委派'),
      multi: false,
      reasonHint: '请输入委派原因',
      reasonRequired: true,
    );
    if (result == null) return;
    await _runAction(
      'delegate',
      () => BpmService.delegateTask(
        taskId: task.id,
        delegateUserId: result.userIds.first,
        reason: result.reason,
      ),
      successText: '审批已委派',
    );
  }

  Future<void> _addSign(BpmTaskItem task) async {
    final result = await _showUserActionSheet(
      title: '加签审批',
      userLabel: '加签处理人',
      confirmText: task.buttonLabel(_bpmButtonAddSign, '加签'),
      multi: true,
      reasonHint: '请输入加签原因',
      reasonRequired: true,
      showSignType: true,
    );
    if (result == null) return;
    await _runAction(
      'addSign',
      () => BpmService.createSignTask(
        taskId: task.id,
        userIds: result.userIds,
        type: result.signType,
        reason: result.reason,
      ),
      successText: result.signType == 'before' ? '已发起前加签' : '已发起后加签',
      closeOnSuccess: false,
      reloadAfterSuccess: true,
    );
  }

  Future<void> _returnTask(BpmTaskItem task) async {
    final nodes = await _loadReturnTasks(task);
    if (nodes == null) return;
    if (nodes.isEmpty) {
      _showSnack('暂无可退回节点');
      return;
    }
    if (!mounted) return;
    final result = await showModalBottomSheet<_BpmReturnActionResult>(
      context: context,
      isScrollControlled: true,
      backgroundColor: KyXColors.surface,
      builder: (_) => _BpmReturnActionSheet(
        title: task.buttonLabel(_bpmButtonReturn, '退回'),
        nodes: nodes,
      ),
    );
    if (result == null) return;
    await _runAction(
      'return',
      () => BpmService.returnTask(
        taskId: task.id,
        targetTaskDefinitionKey: result.taskDefinitionKey,
        reason: result.reason,
      ),
      successText: '审批已退回',
    );
  }

  Future<void> _deleteSign(BpmTaskItem task) async {
    final children = await _loadSignChildren(task);
    if (children == null) return;
    if (children.isEmpty) {
      _showSnack('暂无可减签人员');
      return;
    }
    if (!mounted) return;
    final result = await showModalBottomSheet<_BpmDeleteSignActionResult>(
      context: context,
      isScrollControlled: true,
      backgroundColor: KyXColors.surface,
      builder: (_) => _BpmDeleteSignActionSheet(tasks: children),
    );
    if (result == null) return;
    await _runAction(
      'deleteSign',
      () => BpmService.deleteSignTask(
        taskId: result.taskId,
        reason: result.reason,
      ),
      successText: '减签已提交',
      closeOnSuccess: false,
      reloadAfterSuccess: true,
    );
  }

  Future<List<BpmReturnTask>?> _loadReturnTasks(BpmTaskItem task) async {
    setState(() => _actionKey = 'return');
    try {
      return await BpmService.getReturnTasks(task.id);
    } catch (error) {
      if (mounted) _showSnack(_cleanError(error));
      return null;
    } finally {
      if (mounted) setState(() => _actionKey = null);
    }
  }

  Future<List<BpmTaskItem>?> _loadSignChildren(BpmTaskItem task) async {
    final localChildren = _flattenSignChildren(task.children);
    if (localChildren.isNotEmpty) return localChildren;

    setState(() => _actionKey = 'deleteSign');
    try {
      return _flattenSignChildren(await BpmService.getChildrenTasks(task.id));
    } catch (error) {
      if (mounted) _showSnack(_cleanError(error));
      return null;
    } finally {
      if (mounted) setState(() => _actionKey = null);
    }
  }

  Future<_BpmUserActionResult?> _showUserActionSheet({
    required String title,
    required String userLabel,
    required String confirmText,
    required bool multi,
    required String reasonHint,
    required bool reasonRequired,
    bool showSignType = false,
  }) async {
    final users = await _loadUsers();
    if (!mounted) return null;
    return showModalBottomSheet<_BpmUserActionResult>(
      context: context,
      isScrollControlled: true,
      backgroundColor: KyXColors.surface,
      builder: (_) => _BpmUserActionSheet(
        title: title,
        userLabel: userLabel,
        confirmText: confirmText,
        users: users,
        multi: multi,
        reasonHint: reasonHint,
        reasonRequired: reasonRequired,
        showSignType: showSignType,
      ),
    );
  }

  Future<List<BpmUser>> _loadUsers() async {
    final cached = _userCache;
    if (cached != null) return cached;

    setState(() => _actionKey = 'users');
    try {
      final users = await BpmService.getSimpleUsers();
      if (mounted) {
        setState(() => _userCache = users);
      }
      return users;
    } catch (error) {
      if (mounted) _showSnack(_cleanError(error));
      return const [];
    } finally {
      if (mounted) setState(() => _actionKey = null);
    }
  }

  Future<void> _runAction(
    String actionKey,
    Future<void> Function() handler, {
    required String successText,
    bool closeOnSuccess = true,
    bool reloadAfterSuccess = false,
  }) async {
    setState(() => _actionKey = actionKey);
    try {
      await handler();
      if (!mounted) return;
      _showSnack(successText);
      if (closeOnSuccess) {
        Navigator.of(context).pop(true);
      } else if (reloadAfterSuccess) {
        await _load();
      }
    } catch (error) {
      if (!mounted) return;
      _showSnack(_cleanError(error));
    } finally {
      if (mounted) setState(() => _actionKey = null);
    }
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<String?> _inputText({
    required String title,
    required String hintText,
    required bool required,
  }) async {
    final controller = TextEditingController();
    try {
      return showDialog<String>(
        context: context,
        builder: (context) {
          String? errorText;
          return StatefulBuilder(
            builder: (context, setModalState) {
              return AlertDialog(
                title: Text(title),
                content: TextField(
                  controller: controller,
                  maxLines: 4,
                  decoration: kyxInputDecoration(
                    hintText: hintText,
                  ).copyWith(errorText: errorText),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('取消'),
                  ),
                  ElevatedButton(
                    style: kyxPrimaryButtonStyle(),
                    onPressed: () {
                      final value = controller.text.trim();
                      if (required && value.isEmpty) {
                        setModalState(() => errorText = '请填写审批意见');
                        return;
                      }
                      Navigator.of(context).pop(value);
                    },
                    child: const Text('确认'),
                  ),
                ],
              );
            },
          );
        },
      );
    } finally {
      controller.dispose();
    }
  }

  @override
  Widget build(BuildContext context) {
    final actions = _buildActions();
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '审批详情',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          IconButton(
            tooltip: '发送到会话',
            onPressed: _loading ? null : _shareToChat,
            icon: const Icon(Icons.ios_share_outlined),
          ),
        ],
      ),
      bottomNavigationBar: actions.isEmpty
          ? null
          : _BpmDetailActionBar(actions: actions, loadingKey: _actionKey),
      body: _buildDetailBody(actions.isEmpty),
    );
  }

  Widget _buildDetailBody(bool noActions) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _BpmErrorState(message: _error!, onRetry: _load);
    }

    final detail = _detail;
    final process = detail?.processInstance ?? widget.task.processInstance;
    final task = detail?.todoTask ?? widget.task;
    final summary = process?.summary.isNotEmpty == true
        ? process!.summary
        : widget.task.summary;
    final variableSummary = summary.isEmpty
        ? _formVariableSummary(
            process?.formVariables.isNotEmpty == true
                ? process!.formVariables
                : task.formVariables,
          )
        : const <BpmSummaryItem>[];
    final displaySummary = (summary.isNotEmpty ? summary : variableSummary)
        .map(
          (item) => BpmSummaryItem(
            key: _fieldLabel(item.key),
            value: _displayValue(item.value),
          ),
        )
        .where((item) => item.value.trim().isNotEmpty && item.value != '-')
        .toList();
    final nodes = detail?.activityNodes ?? const <BpmApprovalNode>[];

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: EdgeInsets.only(bottom: noActions ? 28 : 78),
        children: [
          _BpmDetailHeader(
            task: task,
            process: process,
            status: detail?.status,
          ),
          if (displaySummary.isNotEmpty) ...[
            const KyXSectionLabel('表单摘要'),
            KyXListSection(
              children: displaySummary.asMap().entries.map((entry) {
                return _BpmInfoRow(
                  label: entry.value.key,
                  value: entry.value.value,
                  showDivider: entry.key != displaySummary.length - 1,
                );
              }).toList(),
            ),
          ],
          if ((widget.task.reason ?? '').trim().isNotEmpty) ...[
            const KyXSectionLabel('审批意见'),
            KyXListSection(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(widget.task.reason!.trim(), style: KyXText.body),
              ),
            ),
          ],
          const KyXSectionLabel('审批轨迹'),
          KyXListSection(
            child: nodes.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(20),
                    child: _BpmEmptyState(text: '暂无审批轨迹'),
                  )
                : _BpmTimeline(nodes: nodes),
          ),
        ],
      ),
    );
  }
}

class _BpmSearchBar extends StatelessWidget {
  final TextEditingController keywordController;
  final FocusNode searchFocus;
  final int total;
  final String tabName;
  final VoidCallback onSearch;

  const _BpmSearchBar({
    required this.keywordController,
    required this.searchFocus,
    required this.total,
    required this.tabName,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: keywordController,
              focusNode: searchFocus,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => onSearch(),
              decoration: kyxInputDecoration(
                hintText: '搜索流程或任务',
                prefixIcon: const Icon(
                  Icons.search,
                  color: KyXColors.textTertiary,
                  size: 20,
                ),
                suffixIcon: IconButton(
                  tooltip: '搜索',
                  onPressed: onSearch,
                  icon: const Icon(Icons.arrow_forward, size: 19),
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                total.toString(),
                style: const TextStyle(
                  color: KyXColors.text,
                  fontSize: 17,
                  height: 1.1,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 2),
              Text(tabName, style: KyXText.caption),
            ],
          ),
        ],
      ),
    );
  }
}

class _BpmTaskRow extends StatelessWidget {
  final BpmTaskItem task;
  final bool done;
  final VoidCallback onTap;

  const _BpmTaskRow({
    required this.task,
    required this.done,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final summaryText = _summaryText(task.summary);
    final status = done
        ? _StatusMeta(
            _taskStatusLabel(task.status),
            _taskStatusColor(task.status),
          )
        : const _StatusMeta('待处理', KyXColors.primary);
    return Material(
      color: KyXColors.surface,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      task.processName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 15,
                        height: 1.32,
                        color: KyXColors.text,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  _BpmTag(label: status.label, color: status.color),
                ],
              ),
              if (summaryText.isNotEmpty) ...[
                const SizedBox(height: 7),
                Text(
                  summaryText,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.secondary,
                ),
              ],
              const SizedBox(height: 9),
              Wrap(
                spacing: 10,
                runSpacing: 6,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  _BpmInlineMeta(
                    icon: Icons.person_outline,
                    text: task.starterName,
                  ),
                  _BpmInlineMeta(
                    icon: Icons.task_alt_outlined,
                    text: task.name,
                  ),
                  _BpmInlineMeta(
                    icon: done ? Icons.done_all_outlined : Icons.schedule,
                    text: _formatShortDateTime(
                      done ? task.endTime : task.createTime,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BpmDetailHeader extends StatelessWidget {
  final BpmTaskItem task;
  final BpmProcessInstance? process;
  final int? status;

  const _BpmDetailHeader({
    required this.task,
    required this.process,
    required this.status,
  });

  @override
  Widget build(BuildContext context) {
    final currentStatus = status ?? process?.status ?? task.status;
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            process?.name ?? task.processName,
            style: KyXText.pageTitle.copyWith(fontSize: 18, height: 1.25),
          ),
          const SizedBox(height: 9),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              _BpmTag(
                label: _processStatusLabel(currentStatus),
                color: _processStatusColor(currentStatus),
              ),
              _BpmTag(label: task.name, color: KyXColors.slate),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 12,
            runSpacing: 7,
            children: [
              _BpmInlineMeta(
                icon: Icons.person_outline,
                text:
                    '发起人 ${process?.startUser?.displayName ?? task.starterName}',
              ),
              _BpmInlineMeta(
                icon: Icons.access_time,
                text: _formatDateTime(process?.createTime ?? task.createTime),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _BpmTimeline extends StatelessWidget {
  final List<BpmApprovalNode> nodes;

  const _BpmTimeline({required this.nodes});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: nodes.asMap().entries.map((entry) {
        final node = entry.value;
        final isLast = entry.key == nodes.length - 1;
        final color = _taskStatusColor(node.status);
        final taskText = _nodeTaskText(node);
        return Padding(
          padding: EdgeInsets.fromLTRB(16, entry.key == 0 ? 14 : 0, 16, 0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Column(
                children: [
                  Container(
                    width: 10,
                    height: 10,
                    decoration: BoxDecoration(
                      color: color,
                      shape: BoxShape.circle,
                    ),
                  ),
                  if (!isLast)
                    Container(width: 1, height: 54, color: KyXColors.line),
                ],
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(node.name, style: KyXText.bodyStrong),
                          ),
                          Text(
                            _taskStatusLabel(node.status),
                            style: TextStyle(
                              fontSize: 12,
                              color: color,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 3),
                      Text(
                        _formatDateTime(node.endTime ?? node.startTime),
                        style: KyXText.caption,
                      ),
                      if (taskText.isNotEmpty) ...[
                        const SizedBox(height: 5),
                        Text(taskText, style: KyXText.secondary),
                      ],
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}

class _BpmInfoRow extends StatelessWidget {
  final String label;
  final String value;
  final bool showDivider;

  const _BpmInfoRow({
    required this.label,
    required this.value,
    this.showDivider = true,
  });

  @override
  Widget build(BuildContext context) {
    return KyXListRow(
      title: label,
      trailing: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 220),
        child: Text(
          value.isEmpty ? '-' : value,
          textAlign: TextAlign.right,
          maxLines: 3,
          overflow: TextOverflow.ellipsis,
          style: KyXText.secondary,
        ),
      ),
      showDivider: showDivider,
    );
  }
}

class _BpmDetailActionBar extends StatelessWidget {
  final List<_BpmDetailAction> actions;
  final String? loadingKey;

  const _BpmDetailActionBar({required this.actions, required this.loadingKey});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        decoration: const BoxDecoration(
          color: KyXColors.surface,
          border: Border(top: BorderSide(color: KyXColors.line)),
        ),
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          reverse: true,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: actions.map((action) {
              final loading = loadingKey == action.key;
              final disabled = loadingKey != null;
              final color = action.danger ? KyXColors.red : KyXColors.primary;
              final child = Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (loading)
                    SizedBox(
                      width: 15,
                      height: 15,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: action.primary ? Colors.white : color,
                      ),
                    )
                  else
                    Icon(action.icon, size: 17),
                  const SizedBox(width: 5),
                  Text(action.label, maxLines: 1),
                ],
              );
              return Padding(
                padding: const EdgeInsets.only(left: 8),
                child: action.primary
                    ? ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          minimumSize: const Size(0, 34),
                          padding: const EdgeInsets.symmetric(horizontal: 14),
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          visualDensity: VisualDensity.compact,
                          elevation: 0,
                          shadowColor: Colors.transparent,
                          backgroundColor: color,
                          foregroundColor: Colors.white,
                          disabledBackgroundColor: KyXColors.line,
                          disabledForegroundColor: KyXColors.textTertiary,
                          textStyle: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(7),
                          ),
                        ),
                        onPressed: disabled ? null : action.handler,
                        child: child,
                      )
                    : OutlinedButton(
                        style: OutlinedButton.styleFrom(
                          minimumSize: const Size(0, 34),
                          padding: const EdgeInsets.symmetric(horizontal: 13),
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          visualDensity: VisualDensity.compact,
                          foregroundColor: color,
                          disabledForegroundColor: KyXColors.textTertiary,
                          side: BorderSide(
                            color: color.withValues(alpha: 0.38),
                          ),
                          textStyle: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(7),
                          ),
                        ),
                        onPressed: disabled ? null : action.handler,
                        child: child,
                      ),
              );
            }).toList(),
          ),
        ),
      ),
    );
  }
}

class _BpmDetailAction {
  final String key;
  final String label;
  final IconData icon;
  final bool primary;
  final bool danger;
  final Future<void> Function() handler;

  const _BpmDetailAction({
    required this.key,
    required this.label,
    required this.icon,
    required this.handler,
    this.primary = false,
    this.danger = false,
  });
}

class _BpmUserActionResult {
  final List<int> userIds;
  final String reason;
  final String signType;

  const _BpmUserActionResult({
    required this.userIds,
    required this.reason,
    required this.signType,
  });
}

class _BpmReturnActionResult {
  final String taskDefinitionKey;
  final String reason;

  const _BpmReturnActionResult({
    required this.taskDefinitionKey,
    required this.reason,
  });
}

class _BpmDeleteSignActionResult {
  final String taskId;
  final String reason;

  const _BpmDeleteSignActionResult({
    required this.taskId,
    required this.reason,
  });
}

class _BpmUserActionSheet extends StatefulWidget {
  final String title;
  final String userLabel;
  final String confirmText;
  final List<BpmUser> users;
  final bool multi;
  final String reasonHint;
  final bool reasonRequired;
  final bool showSignType;

  const _BpmUserActionSheet({
    required this.title,
    required this.userLabel,
    required this.confirmText,
    required this.users,
    required this.multi,
    required this.reasonHint,
    required this.reasonRequired,
    required this.showSignType,
  });

  @override
  State<_BpmUserActionSheet> createState() => _BpmUserActionSheetState();
}

class _BpmUserActionSheetState extends State<_BpmUserActionSheet> {
  final TextEditingController _searchController = TextEditingController();
  final TextEditingController _reasonController = TextEditingController();
  final Set<int> _selectedIds = <int>{};
  String _signType = 'before';
  String? _error;

  @override
  void initState() {
    super.initState();
    _searchController.addListener(_refresh);
  }

  @override
  void dispose() {
    _searchController
      ..removeListener(_refresh)
      ..dispose();
    _reasonController.dispose();
    super.dispose();
  }

  void _refresh() {
    if (mounted) setState(() {});
  }

  List<BpmUser> get _filteredUsers {
    final keyword = _searchController.text.trim().toLowerCase();
    if (keyword.isEmpty) return widget.users;
    return widget.users.where((user) {
      final text = [
        user.displayName,
        user.username,
        user.subtitle,
        user.id?.toString(),
      ].whereType<String>().join(' ').toLowerCase();
      return text.contains(keyword);
    }).toList();
  }

  void _toggle(BpmUser user) {
    final id = user.id;
    if (id == null) return;
    setState(() {
      _error = null;
      if (widget.multi) {
        _selectedIds.contains(id)
            ? _selectedIds.remove(id)
            : _selectedIds.add(id);
      } else {
        _selectedIds
          ..clear()
          ..add(id);
      }
    });
  }

  void _submit() {
    final reason = _reasonController.text.trim();
    if (_selectedIds.isEmpty) {
      setState(() => _error = '请选择${widget.userLabel}');
      return;
    }
    if (widget.reasonRequired && reason.isEmpty) {
      setState(() => _error = '请填写处理意见');
      return;
    }
    Navigator.of(context).pop(
      _BpmUserActionResult(
        userIds: _selectedIds.toList(),
        reason: reason,
        signType: _signType,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final users = _filteredUsers;
    final height = MediaQuery.of(context).size.height * 0.86;
    return SafeArea(
      child: AnimatedPadding(
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: SizedBox(
          height: height,
          child: Column(
            children: [
              _BpmSheetHeader(title: widget.title),
              if (widget.showSignType)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 2, 16, 10),
                  child: _BpmSignTypeSelector(
                    value: _signType,
                    onChanged: (value) => setState(() => _signType = value),
                  ),
                ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
                child: TextField(
                  controller: _searchController,
                  textInputAction: TextInputAction.search,
                  decoration: kyxInputDecoration(
                    hintText: '搜索姓名、账号或部门',
                    prefixIcon: const Icon(
                      Icons.search,
                      color: KyXColors.textTertiary,
                      size: 20,
                    ),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(widget.userLabel, style: KyXText.bodyStrong),
                    ),
                    Text('已选 ${_selectedIds.length}', style: KyXText.caption),
                  ],
                ),
              ),
              Expanded(
                child: users.isEmpty
                    ? const _BpmEmptyState(text: '暂无匹配人员')
                    : ListView.separated(
                        keyboardDismissBehavior:
                            ScrollViewKeyboardDismissBehavior.onDrag,
                        itemCount: users.length,
                        separatorBuilder: (_, __) => const Divider(
                          height: 1,
                          indent: 64,
                          color: KyXColors.lineSoft,
                        ),
                        itemBuilder: (context, index) {
                          final user = users[index];
                          final selected =
                              user.id != null && _selectedIds.contains(user.id);
                          return _BpmSelectableUserRow(
                            user: user,
                            selected: selected,
                            onTap: () => _toggle(user),
                          );
                        },
                      ),
              ),
              _BpmSheetSubmitArea(
                reasonController: _reasonController,
                hintText: widget.reasonHint,
                errorText: _error,
                confirmText: widget.confirmText,
                onSubmit: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BpmReturnActionSheet extends StatefulWidget {
  final String title;
  final List<BpmReturnTask> nodes;

  const _BpmReturnActionSheet({required this.title, required this.nodes});

  @override
  State<_BpmReturnActionSheet> createState() => _BpmReturnActionSheetState();
}

class _BpmReturnActionSheetState extends State<_BpmReturnActionSheet> {
  final TextEditingController _reasonController = TextEditingController();
  String? _selectedKey;
  String? _error;

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  void _submit() {
    final key = _selectedKey;
    final reason = _reasonController.text.trim();
    if (key == null || key.isEmpty) {
      setState(() => _error = '请选择退回节点');
      return;
    }
    if (reason.isEmpty) {
      setState(() => _error = '请填写退回理由');
      return;
    }
    Navigator.of(
      context,
    ).pop(_BpmReturnActionResult(taskDefinitionKey: key, reason: reason));
  }

  @override
  Widget build(BuildContext context) {
    final height = MediaQuery.of(context).size.height * 0.72;
    return SafeArea(
      child: AnimatedPadding(
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: SizedBox(
          height: height,
          child: Column(
            children: [
              _BpmSheetHeader(title: widget.title),
              const Padding(
                padding: EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text('退回节点', style: KyXText.bodyStrong),
                ),
              ),
              Expanded(
                child: ListView.separated(
                  itemCount: widget.nodes.length,
                  separatorBuilder: (_, __) => const Divider(
                    height: 1,
                    indent: 16,
                    color: KyXColors.lineSoft,
                  ),
                  itemBuilder: (context, index) {
                    final node = widget.nodes[index];
                    final selected = _selectedKey == node.taskDefinitionKey;
                    return ListTile(
                      title: Text(node.name, style: KyXText.bodyStrong),
                      subtitle: Text('退回到该审批节点', style: KyXText.caption),
                      trailing: Icon(
                        selected
                            ? Icons.check_circle
                            : Icons.radio_button_unchecked,
                        color: selected
                            ? KyXColors.primary
                            : KyXColors.textTertiary,
                      ),
                      onTap: () => setState(() {
                        _error = null;
                        _selectedKey = node.taskDefinitionKey;
                      }),
                    );
                  },
                ),
              ),
              _BpmSheetSubmitArea(
                reasonController: _reasonController,
                hintText: '请输入退回理由',
                errorText: _error,
                confirmText: widget.title,
                onSubmit: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BpmDeleteSignActionSheet extends StatefulWidget {
  final List<BpmTaskItem> tasks;

  const _BpmDeleteSignActionSheet({required this.tasks});

  @override
  State<_BpmDeleteSignActionSheet> createState() =>
      _BpmDeleteSignActionSheetState();
}

class _BpmDeleteSignActionSheetState extends State<_BpmDeleteSignActionSheet> {
  final TextEditingController _reasonController = TextEditingController();
  String? _selectedTaskId;
  String? _error;

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  void _submit() {
    final taskId = _selectedTaskId;
    final reason = _reasonController.text.trim();
    if (taskId == null || taskId.isEmpty) {
      setState(() => _error = '请选择减签人员');
      return;
    }
    if (reason.isEmpty) {
      setState(() => _error = '请填写减签原因');
      return;
    }
    Navigator.of(
      context,
    ).pop(_BpmDeleteSignActionResult(taskId: taskId, reason: reason));
  }

  @override
  Widget build(BuildContext context) {
    final height = MediaQuery.of(context).size.height * 0.72;
    return SafeArea(
      child: AnimatedPadding(
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: SizedBox(
          height: height,
          child: Column(
            children: [
              const _BpmSheetHeader(title: '减签'),
              const Padding(
                padding: EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text('减签人员', style: KyXText.bodyStrong),
                ),
              ),
              Expanded(
                child: ListView.separated(
                  itemCount: widget.tasks.length,
                  separatorBuilder: (_, __) => const Divider(
                    height: 1,
                    indent: 64,
                    color: KyXColors.lineSoft,
                  ),
                  itemBuilder: (context, index) {
                    final task = widget.tasks[index];
                    final user = task.assigneeUser ?? task.ownerUser;
                    final selected = _selectedTaskId == task.id;
                    return ListTile(
                      leading: KyXAvatar(
                        text: user?.displayName ?? task.name,
                        imageUrl: user?.avatar,
                        size: 36,
                        color: KyXColors.indigo,
                      ),
                      title: Text(
                        _signTaskLabel(task),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: KyXText.bodyStrong,
                      ),
                      subtitle: Text(
                        user?.subtitle ?? task.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: KyXText.caption,
                      ),
                      trailing: Icon(
                        selected
                            ? Icons.check_circle
                            : Icons.radio_button_unchecked,
                        color: selected
                            ? KyXColors.primary
                            : KyXColors.textTertiary,
                      ),
                      onTap: () => setState(() {
                        _error = null;
                        _selectedTaskId = task.id;
                      }),
                    );
                  },
                ),
              ),
              _BpmSheetSubmitArea(
                reasonController: _reasonController,
                hintText: '请输入减签原因',
                errorText: _error,
                confirmText: '减签',
                onSubmit: _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BpmSheetHeader extends StatelessWidget {
  final String title;

  const _BpmSheetHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const SizedBox(height: 8),
        Container(
          width: 36,
          height: 4,
          decoration: BoxDecoration(
            color: KyXColors.line,
            borderRadius: BorderRadius.circular(99),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 8, 10),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: KyXText.pageTitle.copyWith(fontSize: 18),
                ),
              ),
              IconButton(
                tooltip: '关闭',
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close, size: 20),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _BpmSignTypeSelector extends StatelessWidget {
  final String value;
  final ValueChanged<String> onChanged;

  const _BpmSignTypeSelector({required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 38,
      decoration: BoxDecoration(
        color: KyXColors.bg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: KyXColors.line),
      ),
      child: Row(
        children: [
          _BpmSegmentOption(
            label: '前加签',
            selected: value == 'before',
            onTap: () => onChanged('before'),
          ),
          _BpmSegmentOption(
            label: '后加签',
            selected: value == 'after',
            onTap: () => onChanged('after'),
          ),
        ],
      ),
    );
  }
}

class _BpmSegmentOption extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _BpmSegmentOption({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(7),
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 140),
          alignment: Alignment.center,
          margin: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: selected ? KyXColors.surface : Colors.transparent,
            borderRadius: BorderRadius.circular(6),
            boxShadow: selected
                ? [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.06),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    ),
                  ]
                : null,
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 13,
              color: selected ? KyXColors.primary : KyXColors.textSecondary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ),
    );
  }
}

class _BpmSelectableUserRow extends StatelessWidget {
  final BpmUser user;
  final bool selected;
  final VoidCallback onTap;

  const _BpmSelectableUserRow({
    required this.user,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      leading: KyXAvatar(
        text: user.displayName,
        imageUrl: user.avatar,
        size: 36,
        color: KyXColors.primary,
      ),
      title: Text(
        user.displayName,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: KyXText.bodyStrong,
      ),
      subtitle: Text(
        user.subtitle,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: KyXText.caption,
      ),
      trailing: Icon(
        selected ? Icons.check_circle : Icons.radio_button_unchecked,
        color: selected ? KyXColors.primary : KyXColors.textTertiary,
      ),
    );
  }
}

class _BpmSheetSubmitArea extends StatelessWidget {
  final TextEditingController reasonController;
  final String hintText;
  final String? errorText;
  final String confirmText;
  final VoidCallback onSubmit;

  const _BpmSheetSubmitArea({
    required this.reasonController,
    required this.hintText,
    required this.errorText,
    required this.confirmText,
    required this.onSubmit,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(top: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: reasonController,
            minLines: 2,
            maxLines: 3,
            decoration: kyxInputDecoration(hintText: hintText),
          ),
          if (errorText != null) ...[
            const SizedBox(height: 7),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                errorText!,
                style: const TextStyle(
                  color: KyXColors.red,
                  fontSize: 12,
                  height: 1.25,
                ),
              ),
            ),
          ],
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              style: kyxPrimaryButtonStyle(),
              onPressed: onSubmit,
              child: Text(confirmText),
            ),
          ),
        ],
      ),
    );
  }
}

class _BpmTag extends StatelessWidget {
  final String label;
  final Color color;

  const _BpmTag({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(5),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          height: 1.05,
          color: color,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _BpmInlineMeta extends StatelessWidget {
  final IconData icon;
  final String text;

  const _BpmInlineMeta({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: KyXColors.textTertiary),
        const SizedBox(width: 3),
        Text(text, style: KyXText.caption),
      ],
    );
  }
}

class _BpmEmptyState extends StatelessWidget {
  final String text;

  const _BpmEmptyState({required this.text});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(text, style: KyXText.secondary, textAlign: TextAlign.center),
    );
  }
}

class _BpmErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _BpmErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              message,
              style: KyXText.secondary,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            TextButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('重试'),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusMeta {
  final String label;
  final Color color;

  const _StatusMeta(this.label, this.color);
}

const Map<String, String> _bpmFieldLabels = {
  'name': '名称',
  'title': '标题',
  'subject': '主题',
  'description': '说明',
  'content': '内容',
  'reason': '原因',
  'remark': '备注',
  'status': '状态',
  'type': '类型',
  'category': '分类',
  'priority': '优先级',
  'level': '级别',
  'createtime': '创建时间',
  'updatetime': '更新时间',
  'submittime': '提交时间',
  'applytime': '申请时间',
  'starttime': '开始时间',
  'endtime': '结束时间',
  'startdate': '开始日期',
  'enddate': '结束日期',
  'date': '日期',
  'day': '天数',
  'days': '天数',
  'duration': '时长',
  'durationinmillis': '持续时长',
  'amount': '金额',
  'totalamount': '总金额',
  'price': '单价',
  'quantity': '数量',
  'count': '数量',
  'username': '账号',
  'nickname': '姓名',
  'realname': '姓名',
  'deptname': '部门',
  'department': '部门',
  'mobile': '手机号',
  'phone': '手机号',
  'email': '邮箱',
  'startuser': '发起人',
  'startusername': '发起人',
  'applicant': '申请人',
  'applicantname': '申请人',
  'proposer': '提出人',
  'proposername': '提出人',
  'assignee': '负责人',
  'assigneename': '负责人',
  'owner': '负责人',
  'project': '项目',
  'projectname': '项目名称',
  'requirement': '需求',
  'requirementname': '需求名称',
  'expectedfinishdate': '期望完成日期',
  'expectedfinishtime': '期望完成时间',
  'expectedenddate': '期望结束日期',
  'expectedendtime': '期望结束时间',
  'leavetype': '请假类型',
  'address': '地址',
  'location': '地点',
  'contact': '联系人',
  'contactname': '联系人',
  'attachment': '附件',
  'attachments': '附件',
};

const Map<String, String> _bpmFieldWordLabels = {
  'apply': '申请',
  'applicant': '申请人',
  'assignee': '负责人',
  'attachment': '附件',
  'category': '分类',
  'content': '内容',
  'count': '数量',
  'create': '创建',
  'date': '日期',
  'day': '天数',
  'dept': '部门',
  'description': '说明',
  'duration': '时长',
  'email': '邮箱',
  'end': '结束',
  'expected': '期望',
  'finish': '完成',
  'level': '级别',
  'mobile': '手机号',
  'name': '名称',
  'owner': '负责人',
  'phone': '手机号',
  'priority': '优先级',
  'project': '项目',
  'proposer': '提出人',
  'quantity': '数量',
  'reason': '原因',
  'remark': '备注',
  'requirement': '需求',
  'start': '开始',
  'status': '状态',
  'submit': '提交',
  'time': '时间',
  'title': '标题',
  'total': '合计',
  'type': '类型',
  'update': '更新',
  'user': '用户',
};

List<BpmSummaryItem> _formVariableSummary(Map<String, dynamic> variables) {
  return variables.entries
      .where((entry) => !_isSystemField(entry.key))
      .map(
        (entry) => BpmSummaryItem(
          key: _fieldLabel(entry.key),
          value: _displayValue(entry.value),
        ),
      )
      .where((item) => item.value.trim().isNotEmpty)
      .toList();
}

bool _isSystemField(String key) {
  final lower = key.replaceAll(RegExp(r'[\s_\-.]'), '').toLowerCase();
  return lower.isEmpty ||
      lower == 'id' ||
      lower.endsWith('id') ||
      lower.startsWith('_') ||
      lower.contains('process') ||
      lower.contains('tenant') ||
      lower.contains('taskdefinition') ||
      lower.contains('businesskey') ||
      lower.contains('formconf') ||
      lower.contains('formfields') ||
      lower.contains('variables') ||
      lower == 'deleted' ||
      lower == 'revision' ||
      lower == 'createuser' ||
      lower == 'updateuser';
}

String _fieldLabel(String key) {
  final trimmed = key.trim();
  if (trimmed.isEmpty) return '字段';
  if (RegExp(r'[\u4e00-\u9fff]').hasMatch(trimmed)) return trimmed;

  final normalized = trimmed.replaceAll(RegExp(r'[\s_\-.]'), '').toLowerCase();
  final direct = _bpmFieldLabels[normalized];
  if (direct != null) return direct;

  final words = trimmed
      .replaceAll('_', ' ')
      .replaceAll('-', ' ')
      .replaceAll('.', ' ')
      .replaceAllMapped(RegExp(r'([a-z])([A-Z])'), (m) => '${m[1]} ${m[2]}')
      .trim()
      .split(RegExp(r'\s+'))
      .where((word) => word.isNotEmpty)
      .toList();
  if (words.isEmpty) return '字段';
  return words
      .map((word) => _bpmFieldWordLabels[word.toLowerCase()] ?? word)
      .join('');
}

String _displayValue(dynamic value) {
  if (value == null) return '-';
  if (value is DateTime) return _formatDateTime(value);
  if (value is bool) return value ? '是' : '否';
  if (value is List) {
    final text = value
        .map(_displayValue)
        .where((item) => item.trim().isNotEmpty && item != '-')
        .join('、');
    return text.isEmpty ? '-' : text;
  }
  if (value is Map) {
    final map = Map<dynamic, dynamic>.from(value);
    final preferred =
        map['nickname'] ??
        map['name'] ??
        map['label'] ??
        map['title'] ??
        map['value'];
    if (preferred != null && preferred is! Map && preferred is! List) {
      return _displayValue(preferred);
    }
    final text = value.entries
        .where((entry) => !_isSystemField(entry.key.toString()))
        .map(
          (entry) =>
              '${_fieldLabel(entry.key.toString())}: ${_displayValue(entry.value)}',
        )
        .where((item) => !item.endsWith(': -'))
        .join('，');
    return text.isEmpty ? '-' : text;
  }
  final text = value.toString().trim();
  if (text.isEmpty || text == 'null') return '-';
  if (text.toLowerCase() == 'true') return '是';
  if (text.toLowerCase() == 'false') return '否';
  final normalizedDate = text.contains(' ') && !text.contains('T')
      ? text.replaceFirst(' ', 'T')
      : text;
  if (RegExp(r'^\d{4}-\d{1,2}-\d{1,2}').hasMatch(text)) {
    final date = DateTime.tryParse(normalizedDate);
    if (date != null) return _formatDateTime(date);
  }
  return text;
}

String _summaryText(List<BpmSummaryItem> summary) {
  return summary
      .take(3)
      .map((item) => '${_fieldLabel(item.key)}: ${_displayValue(item.value)}')
      .join(' | ')
      .trim();
}

String _nodeTaskText(BpmApprovalNode node) {
  final handlers = node.tasks
      .map((task) {
        final reason = task.reason?.trim();
        final status = _taskStatusLabel(task.status);
        if (reason == null || reason.isEmpty) {
          return '${task.handlerName} · $status';
        }
        return '${task.handlerName} · $status · $reason';
      })
      .where((item) => item.trim().isNotEmpty && item != '-')
      .toList();
  if (handlers.isNotEmpty) return handlers.join('；');
  final candidates = node.candidateUsers
      .map((item) => item.displayName)
      .toList();
  return candidates.isEmpty ? '' : '候选人 ${candidates.join('、')}';
}

List<BpmTaskItem> _flattenSignChildren(List<BpmTaskItem> tasks) {
  final result = <BpmTaskItem>[];
  for (final task in tasks) {
    if (task.id.trim().isNotEmpty) result.add(task);
    result.addAll(_flattenSignChildren(task.children));
  }
  return result;
}

String _signTaskLabel(BpmTaskItem task) {
  final user = task.assigneeUser ?? task.ownerUser;
  final name = user?.displayName.trim();
  if (name != null && name.isNotEmpty) return name;
  return task.name.trim().isEmpty ? '加签人员' : task.name.trim();
}

String _taskStatusLabel(int? status) {
  switch (status) {
    case -1:
      return '未开始';
    case 0:
      return '待审批';
    case 1:
      return '审批中';
    case 2:
      return '已通过';
    case 3:
      return '已拒绝';
    case 4:
      return '已取消';
    case 5:
      return '已退回';
    case 7:
      return '通过中';
    default:
      return '审批中';
  }
}

Color _taskStatusColor(int? status) {
  switch (status) {
    case 2:
      return KyXColors.green;
    case 3:
    case 5:
      return KyXColors.red;
    case 4:
      return KyXColors.slate;
    case -1:
    case 0:
      return KyXColors.amber;
    default:
      return KyXColors.primary;
  }
}

String _processStatusLabel(int? status) {
  switch (status) {
    case -1:
      return '未开始';
    case 1:
      return '审批中';
    case 2:
      return '审批通过';
    case 3:
      return '审批拒绝';
    case 4:
      return '已取消';
    default:
      return '审批中';
  }
}

Color _processStatusColor(int? status) {
  switch (status) {
    case 2:
      return KyXColors.green;
    case 3:
      return KyXColors.red;
    case 4:
      return KyXColors.slate;
    case -1:
      return KyXColors.amber;
    default:
      return KyXColors.primary;
  }
}

String _formatDateTime(DateTime? value) {
  if (value == null) return '-';
  String two(int input) => input.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)} '
      '${two(value.hour)}:${two(value.minute)}';
}

String _formatShortDateTime(DateTime? value) {
  if (value == null) return '-';
  String two(int input) => input.toString().padLeft(2, '0');
  return '${two(value.month)}-${two(value.day)} ${two(value.hour)}:${two(value.minute)}';
}

String _cleanError(Object error) {
  final text = error.toString().trim();
  if (text.startsWith('Exception: ')) return text.substring(11);
  return text.isEmpty ? '请求失败，请稍后重试' : text;
}
