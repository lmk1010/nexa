import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../services/api_service.dart';
import '../services/badge_read_service.dart';
import '../services/oa_chat_share.dart';
import '../services/requirement_service.dart';
import '../widgets/kyx_design.dart';
import '../widgets/oa_chat_share_sheet.dart';

class RequirementPage extends StatefulWidget {
  const RequirementPage({super.key});

  @override
  State<RequirementPage> createState() => _RequirementPageState();
}

class _RequirementPageState extends State<RequirementPage> {
  final TextEditingController _keywordController = TextEditingController();
  final FocusNode _searchFocus = FocusNode();
  _RequirementScope _scope = _RequirementScope.assigned;
  int? _status;
  int? _currentUserId;
  RequirementScopeOptions? _scopeOptions;
  RequirementOverview? _overview;
  List<RequirementItem> _items = const [];
  int _total = 0;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _keywordController.dispose();
    _searchFocus.dispose();
    super.dispose();
  }

  Future<void> _load({bool quiet = false}) async {
    if (!quiet) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }

    try {
      _currentUserId ??= await RequirementService.currentUserId();
      _scopeOptions ??= await RequirementService.getScopeOptions().catchError(
        (_) => const RequirementScopeOptions(
          currentTenantId: null,
          crossTenantEnabled: false,
          queryAllEnabled: false,
          selectableTenantIds: [],
        ),
      );
      final currentUserId = _currentUserId;
      final canQueryAll = _scopeOptions?.queryAllEnabled == true;
      final needsUserScope =
          !canQueryAll &&
          (_scope == _RequirementScope.all ||
              _scope == _RequirementScope.unread);
      final scopedUserId = needsUserScope ? currentUserId : null;
      String? processInstanceIds;
      var approvalScopeEmpty = false;

      if (_scope == _RequirementScope.approval) {
        final ids =
            await RequirementService.getTodoApprovalProcessInstanceIds();
        approvalScopeEmpty = ids.isEmpty;
        processInstanceIds = ids.join(',');
      }

      RequirementPagedResult<RequirementItem> page;
      if (approvalScopeEmpty) {
        page = const RequirementPagedResult(list: [], total: 0);
      } else {
        page = await RequirementService.getPage(
          pageNo: 1,
          pageSize: 50,
          keyword: _keywordController.text,
          status: _status,
          processInstanceIds: processInstanceIds,
          userId: scopedUserId,
          proposerUserId: _scope == _RequirementScope.mine
              ? currentUserId
              : null,
          assigneeUserId: _scope == _RequirementScope.assigned
              ? currentUserId
              : null,
          commentUnreadOnly: _scope == _RequirementScope.unread ? true : null,
        );
      }

      RequirementOverview? overview;
      try {
        overview = await RequirementService.getOverview(
          keyword: _keywordController.text,
          status: _status,
          processInstanceIds: processInstanceIds,
          userId: scopedUserId,
          proposerUserId: _scope == _RequirementScope.mine
              ? currentUserId
              : null,
          assigneeUserId: _scope == _RequirementScope.assigned
              ? currentUserId
              : null,
          commentUnreadOnly: _scope == _RequirementScope.unread ? true : null,
        );
      } catch (_) {
        overview = _overview;
      }

      if (!mounted) return;
      setState(() {
        _items = page.list;
        _total = page.total;
        _overview = overview;
        _loading = false;
        _error = null;
      });
      if (_scope == _RequirementScope.assigned) {
        await BadgeReadService.markSeen(
          'work_requirement_assigned',
          page.list.map((item) => item.id),
        );
      }
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = _cleanError(error);
      });
    }
  }

  Future<void> _openDetail(RequirementItem item) async {
    if (item.id == null) return;
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => RequirementDetailPage(requirementId: item.id!),
      ),
    );
    if (mounted) await _load(quiet: true);
  }

  Future<void> _createRequirement() async {
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => const RequirementCreatePage()),
    );
    if (created == true && mounted) {
      await _load(quiet: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '需求管理',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          IconButton(
            tooltip: '新建需求',
            onPressed: _createRequirement,
            icon: const Icon(Icons.add_circle_outline),
          ),
        ],
      ),
      body: Column(
        children: [
          _RequirementToolbar(
            keywordController: _keywordController,
            searchFocus: _searchFocus,
            scope: _scope,
            status: _status,
            overview: _overview,
            total: _total,
            onScopeChanged: (value) {
              setState(() => _scope = value);
              _load();
            },
            onStatusChanged: (value) {
              setState(() => _status = value);
              _load();
            },
            onSearch: () {
              _searchFocus.unfocus();
              _load();
            },
          ),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _ErrorState(message: _error!, onRetry: _load);
    }

    return RefreshIndicator(
      onRefresh: () => _load(quiet: true),
      child: _items.isEmpty
          ? ListView(
              children: const [
                SizedBox(height: 120),
                _EmptyState(text: '暂无需求'),
              ],
            )
          : ListView.separated(
              padding: const EdgeInsets.only(bottom: 24),
              itemCount: _items.length,
              separatorBuilder: (_, __) => const Divider(
                height: 1,
                indent: 16,
                color: KyXColors.lineSoft,
              ),
              itemBuilder: (context, index) {
                return _RequirementListRow(
                  item: _items[index],
                  onTap: () => _openDetail(_items[index]),
                );
              },
            ),
    );
  }
}

class RequirementDetailPage extends StatefulWidget {
  final int requirementId;

  const RequirementDetailPage({super.key, required this.requirementId});

  @override
  State<RequirementDetailPage> createState() => _RequirementDetailPageState();
}

class _RequirementDetailPageState extends State<RequirementDetailPage> {
  RequirementItem? _detail;
  List<RequirementLog> _logs = const [];
  List<RequirementComment> _comments = const [];
  RequirementApprovalDetail? _approval;
  final Map<String, RequirementAttachment> _attachments = {};
  final TextEditingController _commentController = TextEditingController();
  final FocusNode _commentFocusNode = FocusNode();
  final List<_PendingRequirementAttachment> _pendingCommentAttachments = [];
  int? _currentUserId;
  bool _loading = true;
  String? _error;
  String? _actionKey;
  bool _commentUploading = false;
  bool _commentSending = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _commentController.dispose();
    _commentFocusNode.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      _currentUserId ??= await RequirementService.currentUserId();
      final detail = await RequirementService.getDetail(widget.requirementId);
      final results = await Future.wait<dynamic>([
        RequirementService.getLogs(widget.requirementId),
        RequirementService.getComments(
          widget.requirementId,
        ).catchError((_) => <RequirementComment>[]),
        detail.processInstanceId == null
            ? Future<RequirementApprovalDetail?>.value()
            : RequirementService.getApprovalDetail(detail.processInstanceId!),
      ]);

      final logs = results[0] as List<RequirementLog>;
      final comments = results[1] as List<RequirementComment>;
      final approval = results[2] as RequirementApprovalDetail?;
      final rawAttachments = <String>{
        ...detail.attachmentUrls,
        for (final comment in comments) ...comment.attachmentUrls,
      };
      final resolved = await _resolveAttachments(rawAttachments);
      if (comments.isNotEmpty) {
        await RequirementService.readAllComments(
          widget.requirementId,
        ).catchError((_) {});
      }

      if (!mounted) return;
      setState(() {
        _detail = detail;
        _logs = logs;
        _comments = comments;
        _approval = approval;
        _attachments
          ..clear()
          ..addAll(resolved);
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

  Future<Map<String, RequirementAttachment>> _resolveAttachments(
    Iterable<String> values,
  ) async {
    final result = <String, RequirementAttachment>{};
    for (final raw in values.where((item) => item.trim().isNotEmpty)) {
      result[raw] = await RequirementService.resolveAttachment(raw);
    }
    return result;
  }

  List<_DetailAction> _buildActions() {
    final item = _detail;
    if (item == null || item.id == null) return const [];
    final id = item.id!;
    final actions = <_DetailAction>[];
    final canFlow = item.canOperateByApproval;
    final isOwner = item.isProposer(_currentUserId);
    final isDeveloper = item.isDeveloper(_currentUserId);
    final approvalTask = _approval?.todoTask;
    final hasPendingApproval =
        item.approvalStatus == 1 &&
        approvalTask != null &&
        approvalTask.canHandle(_currentUserId);

    if (hasPendingApproval) {
      actions.add(
        _DetailAction(
          key: 'bpmApprove',
          label: '审批通过',
          icon: Icons.verified_outlined,
          primary: true,
          handler: () => _approveTask(approvalTask),
        ),
      );
      actions.add(
        _DetailAction(
          key: 'bpmReject',
          label: '审批驳回',
          icon: Icons.undo_outlined,
          danger: true,
          handler: () => _rejectApprovalTask(approvalTask),
        ),
      );
    }

    if ([0, 1, 2, 3, 4].contains(item.status) &&
        (item.approvalStatus == null ||
            item.approvalStatus == 3 ||
            item.approvalStatus == 4)) {
      actions.add(
        _DetailAction(
          key: 'submitApproval',
          label: item.approvalStatus == null ? '提交审批' : '重新提审',
          icon: Icons.send_outlined,
          primary: true,
          handler: () => _runAction(
            'submitApproval',
            () => RequirementService.submitApproval(id),
            successText: '审批流程已发起',
          ),
        ),
      );
    }

    if (canFlow && [0, 1, 2].contains(item.status)) {
      actions.add(
        _DetailAction(
          key: 'assign',
          label: item.assigneeUserId == null ? '分派' : '转派',
          icon: Icons.person_add_alt_outlined,
          handler: () => _assign(item),
        ),
      );
    }

    if (canFlow && item.status == 1 && (isDeveloper || isOwner)) {
      actions.add(
        _DetailAction(
          key: 'startDev',
          label: '开始开发',
          icon: Icons.play_arrow_outlined,
          primary: true,
          handler: () => _runAction(
            'startDev',
            () => RequirementService.startDev(id),
            successText: '已开始开发',
          ),
        ),
      );
    }

    if (canFlow && item.status == 2 && (isDeveloper || isOwner)) {
      actions.add(
        _DetailAction(
          key: 'submitTest',
          label: '提交验收',
          icon: Icons.upload_outlined,
          primary: true,
          handler: () => _runAction(
            'submitTest',
            () => RequirementService.submitTest(id),
            successText: '已提交验收',
          ),
        ),
      );
    }

    if (canFlow && item.status == 3) {
      actions.add(
        _DetailAction(
          key: 'testPass',
          label: '测试通过',
          icon: Icons.check_circle_outline,
          primary: true,
          handler: () => _runAction(
            'testPass',
            () => RequirementService.testPass(id),
            successText: '已进入验收',
          ),
        ),
      );
      actions.add(
        _DetailAction(
          key: 'testReject',
          label: '测试退回',
          icon: Icons.reply_outlined,
          danger: true,
          handler: () => _remarkAction(
            title: '测试退回',
            hintText: '说明需要调整的问题',
            actionKey: 'testReject',
            onSubmit: (remark) => RequirementService.testReject(id, remark),
            successText: '已退回开发',
          ),
        ),
      );
    }

    if (canFlow && item.status == 4) {
      actions.add(
        _DetailAction(
          key: 'acceptPass',
          label: '验收通过',
          icon: Icons.done_all_outlined,
          primary: true,
          handler: () => _remarkAction(
            title: '验收通过',
            hintText: '填写验收说明',
            actionKey: 'acceptPass',
            onSubmit: (remark) =>
                RequirementService.acceptPass(id, remark: remark),
            successText: '验收已通过',
          ),
        ),
      );
      actions.add(
        _DetailAction(
          key: 'acceptReject',
          label: '验收退回',
          icon: Icons.rotate_left_outlined,
          danger: true,
          handler: () => _remarkAction(
            title: '验收退回',
            hintText: '说明退回原因',
            actionKey: 'acceptReject',
            onSubmit: (remark) => RequirementService.acceptReject(id, remark),
            successText: '已退回开发',
          ),
        ),
      );
    }

    if (canFlow && [1, 2].contains(item.status) && (isDeveloper || isOwner)) {
      actions.add(
        _DetailAction(
          key: 'devReject',
          label: '拒绝/提疑',
          icon: Icons.help_outline,
          danger: true,
          handler: () => _remarkAction(
            title: '拒绝/提疑',
            hintText: '写清原因或疑问',
            actionKey: 'devReject',
            onSubmit: (remark) => RequirementService.devReject(id, remark),
            successText: '已退回待分派',
          ),
        ),
      );
    }

    if ([0, 1, 2, 3, 4].contains(item.status)) {
      actions.add(
        _DetailAction(
          key: 'suspend',
          label: '挂起',
          icon: Icons.pause_circle_outline,
          handler: () => _confirmAction(
            title: '挂起需求',
            content: '确认挂起《${item.title}》吗？',
            actionKey: 'suspend',
            onConfirm: () => RequirementService.suspend(id),
            successText: '已挂起',
          ),
        ),
      );
      actions.add(
        _DetailAction(
          key: 'cancel',
          label: '取消',
          icon: Icons.cancel_outlined,
          danger: true,
          handler: () => _confirmAction(
            title: '取消需求',
            content: '确认取消《${item.title}》吗？',
            actionKey: 'cancel',
            onConfirm: () => RequirementService.cancel(id),
            successText: '已取消',
          ),
        ),
      );
    }

    if ([5, 6, 7].contains(item.status)) {
      actions.add(
        _DetailAction(
          key: 'reopen',
          label: '重开',
          icon: Icons.refresh_outlined,
          handler: () => _confirmAction(
            title: '重新打开需求',
            content: '确认重新打开《${item.title}》吗？',
            actionKey: 'reopen',
            onConfirm: () => RequirementService.reopen(id),
            successText: '已重新打开',
          ),
        ),
      );
    }

    return actions;
  }

  Future<void> _assign(RequirementItem item) async {
    final user = await _pickRequirementUser(context);
    if (user == null || user.id == null || item.id == null) return;
    await _runAction(
      'assign',
      () => RequirementService.assignRequirement(
        id: item.id!,
        assigneeUserId: user.id!,
        assigneeTenantId: user.tenantId,
        assigneeName: user.displayName,
        transfer: item.assigneeUserId != null,
      ),
      successText: '已分派给 ${user.displayName}',
    );
  }

  Future<void> _approveTask(RequirementApprovalTask task) async {
    final reason = await _inputText(
      title: '审批通过',
      hintText: task.reasonRequire ? '请输入审批意见' : '审批意见（选填）',
      required: task.reasonRequire,
    );
    if (reason == null) return;
    await _runAction(
      'bpmApprove',
      () => RequirementService.approveTask(taskId: task.id, reason: reason),
      successText: '审批已通过',
    );
  }

  Future<void> _rejectApprovalTask(RequirementApprovalTask task) async {
    final reason = await _inputText(
      title: '审批驳回',
      hintText: '请输入审批意见',
      required: true,
    );
    if (reason == null) return;
    await _runAction(
      'bpmReject',
      () => RequirementService.rejectTask(taskId: task.id, reason: reason),
      successText: '审批已驳回',
    );
  }

  Future<void> _remarkAction({
    required String title,
    required String hintText,
    required String actionKey,
    required Future<void> Function(String remark) onSubmit,
    required String successText,
  }) async {
    final remark = await _inputText(
      title: title,
      hintText: hintText,
      required: true,
    );
    if (remark == null) return;
    await _runAction(
      actionKey,
      () => onSubmit(remark),
      successText: successText,
    );
  }

  Future<void> _confirmAction({
    required String title,
    required String content,
    required String actionKey,
    required Future<void> Function() onConfirm,
    required String successText,
  }) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          ElevatedButton(
            style: kyxPrimaryButtonStyle(),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('确认'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    await _runAction(actionKey, onConfirm, successText: successText);
  }

  Future<void> _runAction(
    String actionKey,
    Future<void> Function() handler, {
    required String successText,
  }) async {
    setState(() => _actionKey = actionKey);
    try {
      await handler();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(successText)));
      await _load();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_cleanError(error))));
    } finally {
      if (mounted) setState(() => _actionKey = null);
    }
  }

  Future<void> _shareToChat() async {
    final detail = _detail;
    if (detail == null) return;
    final sent = await showOaChatShareSheet(
      context,
      _buildRequirementSharePayload(detail),
    );
    if (!mounted || sent != true) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('已发送到会话')));
  }

  OaChatSharePayload _buildRequirementSharePayload(RequirementItem item) {
    final status = _statusMeta[item.status]?.label ?? '未知状态';
    final priority = _priorityMeta[item.priority]?.label ?? '中';
    return OaChatSharePayload(
      module: '需求',
      objectType: 'requirement',
      objectId: item.id?.toString(),
      title: item.title,
      status: status,
      fields: [
        OaChatShareField(label: '优先级', value: priority),
        OaChatShareField(label: '提出人', value: item.proposerName ?? '-'),
        OaChatShareField(label: '目标部门', value: item.targetDept ?? '-'),
        OaChatShareField(label: '负责人', value: item.assigneeName ?? '待分派'),
        OaChatShareField(
          label: '期望完成',
          value: _formatDate(item.expectedFinishDate),
        ),
      ],
      summary: _shareSummary(item.description),
    );
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
                        setModalState(() => errorText = '请填写内容');
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

  Future<void> _openAttachment(RequirementAttachment attachment) async {
    if (attachment.isImage) {
      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => _ImagePreviewPage(attachment: attachment),
        ),
      );
      return;
    }

    final uri = Uri.tryParse(attachment.url);
    if (uri == null || uri.toString().isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('附件地址无效')));
      return;
    }
    final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!opened && mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('无法打开附件')));
    }
  }

  void _focusCommentInput() {
    _commentFocusNode.requestFocus();
  }

  Future<void> _pickCommentImages() =>
      _pickCommentAttachments(imagesOnly: true);

  Future<void> _pickCommentFiles() =>
      _pickCommentAttachments(imagesOnly: false);

  Future<void> _pickCommentAttachments({required bool imagesOnly}) async {
    if (_commentUploading || _commentSending) return;

    final result = await FilePicker.platform.pickFiles(
      allowMultiple: true,
      type: imagesOnly ? FileType.image : FileType.any,
      withData: false,
    );
    if (result == null || result.files.isEmpty) return;
    if (!mounted) return;

    final messenger = ScaffoldMessenger.of(context);
    setState(() => _commentUploading = true);
    final additions = <_PendingRequirementAttachment>[];
    try {
      for (final file in result.files) {
        final path = file.path;
        if (path == null || path.isEmpty) continue;
        final fileId = await RequirementService.uploadFile(
          path: path,
          fileName: file.name,
        );
        additions.add(
          _PendingRequirementAttachment(
            id: fileId,
            name: file.name,
            path: path,
            isImage: imagesOnly || _isImageFileName(file.name),
          ),
        );
      }
      if (additions.isNotEmpty) {
        messenger.showSnackBar(
          SnackBar(content: Text('已添加 ${additions.length} 个附件')),
        );
      }
    } catch (error) {
      messenger.showSnackBar(SnackBar(content: Text(_cleanError(error))));
    } finally {
      if (mounted) {
        setState(() {
          _pendingCommentAttachments.addAll(additions);
          _commentUploading = false;
        });
      }
    }
  }

  void _removePendingCommentAttachment(int index) {
    if (index < 0 || index >= _pendingCommentAttachments.length) return;
    setState(() => _pendingCommentAttachments.removeAt(index));
  }

  Future<void> _sendComment() async {
    final detail = _detail;
    if (detail?.id == null || _commentUploading || _commentSending) return;

    final messenger = ScaffoldMessenger.of(context);
    final content = _commentController.text.trim();
    final attachmentIds = _pendingCommentAttachments
        .map((item) => item.id)
        .toList();
    if (content.isEmpty && attachmentIds.isEmpty) {
      messenger.showSnackBar(const SnackBar(content: Text('请输入评论或添加附件')));
      _focusCommentInput();
      return;
    }

    setState(() => _commentSending = true);
    try {
      await RequirementService.createComment(
        requirementId: detail!.id!,
        content: content,
        attachmentUrls: attachmentIds,
      );
      if (!mounted) return;
      _commentController.clear();
      _commentFocusNode.unfocus();
      setState(() => _pendingCommentAttachments.clear());
      messenger.showSnackBar(const SnackBar(content: Text('评论已发送')));
      await _load();
    } catch (error) {
      messenger.showSnackBar(SnackBar(content: Text(_cleanError(error))));
    } finally {
      if (mounted) setState(() => _commentSending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final actions = _buildActions();
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '需求详情',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          IconButton(
            tooltip: '发送到会话',
            onPressed: detail == null ? null : _shareToChat,
            icon: const Icon(Icons.ios_share_outlined),
          ),
          IconButton(
            tooltip: '写评论',
            onPressed: detail == null ? null : _focusCommentInput,
            icon: const Icon(Icons.mode_comment_outlined),
          ),
        ],
      ),
      bottomNavigationBar: detail == null
          ? null
          : _RequirementDetailBottomBar(
              actions: actions,
              loadingKey: _actionKey,
              commentController: _commentController,
              commentFocusNode: _commentFocusNode,
              pendingAttachments: _pendingCommentAttachments,
              uploading: _commentUploading,
              sending: _commentSending,
              onPickImage: _pickCommentImages,
              onPickFile: _pickCommentFiles,
              onRemoveAttachment: _removePendingCommentAttachment,
              onSend: _sendComment,
            ),
      body: _buildDetailBody(detail),
    );
  }

  Widget _buildDetailBody(RequirementItem? detail) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _ErrorState(message: _error!, onRetry: _load);
    }
    if (detail == null) {
      return const _EmptyState(text: '需求不存在');
    }

    final allAttachments = detail.attachmentUrls
        .map((raw) => _attachments[raw])
        .whereType<RequirementAttachment>()
        .toList();

    final bottomPadding =
        132 +
        (_pendingCommentAttachments.isEmpty ? 0 : 76) +
        (_buildActions().isEmpty ? 0 : 48);

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: EdgeInsets.only(bottom: bottomPadding.toDouble()),
        children: [
          _RequirementDetailHeader(item: detail),
          const KyXSectionLabel('关键信息'),
          KyXListSection(
            children: [
              _InfoRow(label: '提出人', value: detail.proposerName ?? '-'),
              _InfoRow(label: '提出部门', value: detail.proposerDept ?? '-'),
              _InfoRow(label: '目标部门', value: detail.targetDept ?? '-'),
              _InfoRow(label: '负责人', value: detail.assigneeName ?? '待分派'),
              _InfoRow(
                label: '协作人',
                value: detail.collaboratorNames ?? _developerNames(detail),
              ),
              _InfoRow(
                label: '期望完成',
                value: _formatDate(detail.expectedFinishDate),
              ),
              _InfoRow(
                label: '创建时间',
                value: _formatDateTime(detail.createTime),
                showDivider: false,
              ),
            ],
          ),
          if (detail.lastRejectReason != null) ...[
            const KyXSectionLabel('最近退回'),
            KyXListSection(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(detail.lastRejectReason!, style: KyXText.body),
              ),
            ),
          ],
          if (allAttachments.isNotEmpty) ...[
            const KyXSectionLabel('附件'),
            KyXListSection(
              child: _AttachmentPanel(
                attachments: allAttachments,
                onAttachmentTap: _openAttachment,
              ),
            ),
          ],
          if ((_approval?.activityNodes ?? const []).isNotEmpty) ...[
            const KyXSectionLabel('审批进度'),
            KyXListSection(
              child: _ApprovalTimeline(nodes: _approval!.activityNodes),
            ),
          ],
          const KyXSectionLabel('处理日志'),
          KyXListSection(
            child: _logs.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(20),
                    child: _EmptyState(text: '暂无处理日志'),
                  )
                : Column(
                    children: _logs
                        .asMap()
                        .entries
                        .map(
                          (entry) => _LogRow(
                            log: entry.value,
                            showDivider: entry.key != _logs.length - 1,
                          ),
                        )
                        .toList(),
                  ),
          ),
          KyXSectionLabel('评论 (${_comments.length})'),
          KyXListSection(
            child: _comments.isEmpty
                ? Padding(
                    padding: const EdgeInsets.fromLTRB(16, 14, 16, 18),
                    child: Row(
                      children: [
                        const Expanded(
                          child: Text('暂无评论', style: KyXText.secondary),
                        ),
                        TextButton.icon(
                          onPressed: _focusCommentInput,
                          icon: const Icon(Icons.add_comment_outlined),
                          label: const Text('写评论'),
                        ),
                      ],
                    ),
                  )
                : Column(
                    children: _comments
                        .asMap()
                        .entries
                        .map(
                          (entry) => _CommentRow(
                            comment: entry.value,
                            attachments: entry.value.attachmentUrls
                                .map((raw) => _attachments[raw])
                                .whereType<RequirementAttachment>()
                                .toList(),
                            showDivider: entry.key != _comments.length - 1,
                            onAttachmentTap: _openAttachment,
                          ),
                        )
                        .toList(),
                  ),
          ),
        ],
      ),
    );
  }
}

class RequirementCreatePage extends StatefulWidget {
  const RequirementCreatePage({super.key});

  @override
  State<RequirementCreatePage> createState() => _RequirementCreatePageState();
}

class _RequirementCreatePageState extends State<RequirementCreatePage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  final TextEditingController _proposerNameController = TextEditingController();
  final TextEditingController _proposerDeptController = TextEditingController();
  final TextEditingController _targetDeptController = TextEditingController();
  final List<String> _attachmentIds = [];
  final List<String> _attachmentNames = [];
  int _priority = 2;
  DateTime? _expectedFinishDate;
  RequirementUser? _assignee;
  bool _saving = false;
  bool _uploading = false;

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _proposerNameController.dispose();
    _proposerDeptController.dispose();
    _targetDeptController.dispose();
    super.dispose();
  }

  Future<void> _pickExpectedDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _expectedFinishDate ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 5),
    );
    if (picked == null) return;
    setState(() => _expectedFinishDate = picked);
  }

  Future<void> _pickAssignee() async {
    final user = await _pickRequirementUser(context);
    if (user == null) return;
    setState(() => _assignee = user);
  }

  Future<void> _pickFiles() async {
    final result = await FilePicker.platform.pickFiles(
      allowMultiple: true,
      withData: false,
    );
    if (result == null || result.files.isEmpty) return;
    setState(() => _uploading = true);
    try {
      for (final file in result.files) {
        final path = file.path;
        if (path == null || path.isEmpty) continue;
        final fileId = await RequirementService.uploadFile(
          path: path,
          fileName: file.name,
        );
        _attachmentIds.add(fileId);
        _attachmentNames.add(file.name);
      }
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('附件已上传')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(_cleanError(error))));
      }
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      await RequirementService.createRequirement(
        RequirementSaveRequest(
          title: _titleController.text.trim(),
          description: _descriptionController.text.trim(),
          priority: _priority,
          expectedFinishDate: _expectedFinishDate,
          assigneeUserId: _assignee?.id,
          assigneeTenantId: _assignee?.tenantId,
          assigneeName: _assignee?.displayName,
          proposerName: _proposerNameController.text.trim(),
          proposerDept: _proposerDeptController.text.trim(),
          targetDept: _targetDeptController.text.trim(),
          attachmentUrls: _attachmentIds,
        ),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('需求已创建')));
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_cleanError(error))));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '新建需求',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      bottomNavigationBar: SafeArea(
        child: Container(
          decoration: const BoxDecoration(
            color: KyXColors.surface,
            border: Border(top: BorderSide(color: KyXColors.line)),
          ),
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
          child: ElevatedButton(
            style: kyxPrimaryButtonStyle(),
            onPressed: _saving || _uploading ? null : _submit,
            child: Text(_saving ? '提交中' : '提交需求'),
          ),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.only(bottom: 92),
          children: [
            const KyXSectionLabel('基础信息'),
            KyXListSection(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: Column(
                  children: [
                    TextFormField(
                      controller: _titleController,
                      decoration: kyxInputDecoration(hintText: '需求标题'),
                      validator: (value) =>
                          (value ?? '').trim().isEmpty ? '请填写需求标题' : null,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _descriptionController,
                      minLines: 5,
                      maxLines: 8,
                      decoration: kyxInputDecoration(hintText: '需求描述'),
                      validator: (value) =>
                          (value ?? '').trim().isEmpty ? '请填写需求描述' : null,
                    ),
                    const SizedBox(height: 12),
                    _PrioritySelector(
                      value: _priority,
                      onChanged: (value) => setState(() => _priority = value),
                    ),
                  ],
                ),
              ),
            ),
            const KyXSectionLabel('人员与计划'),
            KyXListSection(
              children: [
                KyXListRow(
                  title: '负责人',
                  subtitle: _assignee == null
                      ? '可稍后分派'
                      : _assignee!.displayName,
                  trailing: const Icon(
                    Icons.chevron_right,
                    color: KyXColors.textTertiary,
                  ),
                  onTap: _pickAssignee,
                ),
                KyXListRow(
                  title: '期望完成',
                  subtitle: _expectedFinishDate == null
                      ? '未设置'
                      : _formatDate(_expectedFinishDate),
                  trailing: const Icon(
                    Icons.chevron_right,
                    color: KyXColors.textTertiary,
                  ),
                  onTap: _pickExpectedDate,
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                  child: TextFormField(
                    controller: _proposerNameController,
                    decoration: kyxInputDecoration(hintText: '提出人（选填）'),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                  child: TextFormField(
                    controller: _proposerDeptController,
                    decoration: kyxInputDecoration(hintText: '提出部门（选填）'),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 16),
                  child: TextFormField(
                    controller: _targetDeptController,
                    decoration: kyxInputDecoration(hintText: '目标部门（选填）'),
                  ),
                ),
              ],
            ),
            const KyXSectionLabel('附件'),
            KyXListSection(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (_attachmentNames.isEmpty)
                      const Text('暂无附件', style: KyXText.secondary)
                    else
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: _attachmentNames
                            .map(
                              (name) => Chip(
                                label: Text(
                                  name,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                visualDensity: VisualDensity.compact,
                              ),
                            )
                            .toList(),
                      ),
                    const SizedBox(height: 10),
                    OutlinedButton.icon(
                      onPressed: _saving || _uploading ? null : _pickFiles,
                      icon: _uploading
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.attach_file),
                      label: Text(_uploading ? '上传中' : '添加附件'),
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
}

class _RequirementToolbar extends StatelessWidget {
  final TextEditingController keywordController;
  final FocusNode searchFocus;
  final _RequirementScope scope;
  final int? status;
  final RequirementOverview? overview;
  final int total;
  final ValueChanged<_RequirementScope> onScopeChanged;
  final ValueChanged<int?> onStatusChanged;
  final VoidCallback onSearch;

  const _RequirementToolbar({
    required this.keywordController,
    required this.searchFocus,
    required this.scope,
    required this.status,
    required this.overview,
    required this.total,
    required this.onScopeChanged,
    required this.onStatusChanged,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 9, 16, 11),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: keywordController,
            focusNode: searchFocus,
            textInputAction: TextInputAction.search,
            onSubmitted: (_) => onSearch(),
            decoration: kyxInputDecoration(
              hintText: '搜索标题、描述、编号',
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
          const SizedBox(height: 10),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _RequirementScope.values
                  .map(
                    (item) => Padding(
                      padding: const EdgeInsets.only(right: 7),
                      child: _FilterChip(
                        label: item.label,
                        selected: scope == item,
                        onTap: () => onScopeChanged(item),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
          const SizedBox(height: 9),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _FilterChip(
                  label: '全部状态',
                  selected: status == null,
                  onTap: () => onStatusChanged(null),
                ),
                const SizedBox(width: 7),
                for (final entry in _statusMeta.entries)
                  Padding(
                    padding: const EdgeInsets.only(right: 7),
                    child: _FilterChip(
                      label: entry.value.label,
                      selected: status == entry.key,
                      color: entry.value.color,
                      onTap: () => onStatusChanged(entry.key),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              _MetricText(label: '当前', value: total),
              const SizedBox(width: 16),
              _MetricText(label: '待办', value: overview?.myTodoCount ?? 0),
              const SizedBox(width: 16),
              _MetricText(label: '未读', value: overview?.unreadCount ?? 0),
              const SizedBox(width: 16),
              _MetricText(label: '逾期', value: overview?.overdueCount ?? 0),
            ],
          ),
        ],
      ),
    );
  }
}

class _RequirementListRow extends StatelessWidget {
  final RequirementItem item;
  final VoidCallback onTap;

  const _RequirementListRow({required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final status =
        _statusMeta[item.status] ?? _StatusMeta('未知', KyXColors.slate);
    final priority =
        _priorityMeta[item.priority] ?? _StatusMeta('中', KyXColors.primary);
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
                      item.title,
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
                  if (item.commentUnreadCount > 0) ...[
                    const SizedBox(width: 8),
                    _UnreadBadge(count: item.commentUnreadCount),
                  ],
                ],
              ),
              const SizedBox(height: 7),
              Text(
                _cleanSummary(item.description),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: KyXText.secondary,
              ),
              const SizedBox(height: 9),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  _Tag(label: status.label, color: status.color),
                  _Tag(label: priority.label, color: priority.color),
                  if (item.approvalStatus != null)
                    _Tag(
                      label: _approvalStatusLabel(item.approvalStatus),
                      color: _approvalStatusColor(item.approvalStatus),
                    ),
                  _InlineMeta(
                    icon: Icons.person_outline,
                    text: item.assigneeName ?? '待分派',
                  ),
                  _InlineMeta(
                    icon: Icons.schedule,
                    text: _formatDate(item.expectedFinishDate),
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

class _RequirementDetailHeader extends StatelessWidget {
  final RequirementItem item;

  const _RequirementDetailHeader({required this.item});

  @override
  Widget build(BuildContext context) {
    final status =
        _statusMeta[item.status] ?? _StatusMeta('未知', KyXColors.slate);
    final priority =
        _priorityMeta[item.priority] ?? _StatusMeta('中', KyXColors.primary);
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            item.title,
            style: KyXText.pageTitle.copyWith(fontSize: 18, height: 1.25),
          ),
          const SizedBox(height: 9),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              _Tag(label: status.label, color: status.color),
              _Tag(label: priority.label, color: priority.color),
              if (item.approvalStatus != null)
                _Tag(
                  label: _approvalStatusLabel(item.approvalStatus),
                  color: _approvalStatusColor(item.approvalStatus),
                ),
              if (item.commentCount > 0)
                _Tag(label: '${item.commentCount} 条评论', color: KyXColors.cyan),
            ],
          ),
          if (item.description.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(
              item.description.trim(),
              style: KyXText.body.copyWith(fontSize: 14, height: 1.45),
            ),
          ],
        ],
      ),
    );
  }
}

class _DetailActionBar extends StatelessWidget {
  final List<_DetailAction> actions;
  final String? loadingKey;
  final bool safeArea;

  const _DetailActionBar({
    required this.actions,
    required this.loadingKey,
    this.safeArea = true,
  });

  @override
  Widget build(BuildContext context) {
    final content = Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(top: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(12, 7, 12, 9),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: actions.map((action) {
            final loading = loadingKey == action.key;
            final color = action.danger ? KyXColors.red : KyXColors.primary;
            final child = Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (loading)
                  const SizedBox(
                    width: 15,
                    height: 15,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                else
                  Icon(action.icon, size: 17),
                const SizedBox(width: 5),
                Text(action.label),
              ],
            );
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: action.primary
                  ? ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        minimumSize: const Size(0, 34),
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        visualDensity: VisualDensity.compact,
                        elevation: 0,
                        shadowColor: Colors.transparent,
                        backgroundColor: color,
                        foregroundColor: Colors.white,
                        textStyle: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(7),
                        ),
                      ),
                      onPressed: loadingKey == null ? action.handler : null,
                      child: child,
                    )
                  : OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size(0, 34),
                        padding: const EdgeInsets.symmetric(horizontal: 11),
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        visualDensity: VisualDensity.compact,
                        foregroundColor: color,
                        side: BorderSide(color: color.withValues(alpha: 0.38)),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(7),
                        ),
                        textStyle: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      onPressed: loadingKey == null ? action.handler : null,
                      child: child,
                    ),
            );
          }).toList(),
        ),
      ),
    );
    return safeArea ? SafeArea(child: content) : content;
  }
}

class _RequirementDetailBottomBar extends StatelessWidget {
  final List<_DetailAction> actions;
  final String? loadingKey;
  final TextEditingController commentController;
  final FocusNode commentFocusNode;
  final List<_PendingRequirementAttachment> pendingAttachments;
  final bool uploading;
  final bool sending;
  final VoidCallback onPickImage;
  final VoidCallback onPickFile;
  final ValueChanged<int> onRemoveAttachment;
  final VoidCallback onSend;

  const _RequirementDetailBottomBar({
    required this.actions,
    required this.loadingKey,
    required this.commentController,
    required this.commentFocusNode,
    required this.pendingAttachments,
    required this.uploading,
    required this.sending,
    required this.onPickImage,
    required this.onPickFile,
    required this.onRemoveAttachment,
    required this.onSend,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (actions.isNotEmpty)
            _DetailActionBar(
              actions: actions,
              loadingKey: loadingKey,
              safeArea: false,
            ),
          _RequirementCommentComposer(
            controller: commentController,
            focusNode: commentFocusNode,
            pendingAttachments: pendingAttachments,
            uploading: uploading,
            sending: sending,
            showTopBorder: actions.isEmpty,
            onPickImage: onPickImage,
            onPickFile: onPickFile,
            onRemoveAttachment: onRemoveAttachment,
            onSend: onSend,
          ),
        ],
      ),
    );
  }
}

class _RequirementCommentComposer extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final List<_PendingRequirementAttachment> pendingAttachments;
  final bool uploading;
  final bool sending;
  final bool showTopBorder;
  final VoidCallback onPickImage;
  final VoidCallback onPickFile;
  final ValueChanged<int> onRemoveAttachment;
  final VoidCallback onSend;

  const _RequirementCommentComposer({
    required this.controller,
    required this.focusNode,
    required this.pendingAttachments,
    required this.uploading,
    required this.sending,
    required this.showTopBorder,
    required this.onPickImage,
    required this.onPickFile,
    required this.onRemoveAttachment,
    required this.onSend,
  });

  @override
  Widget build(BuildContext context) {
    final disabled = uploading || sending;
    return Container(
      decoration: BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(
            color: showTopBorder ? KyXColors.line : KyXColors.lineSoft,
          ),
        ),
      ),
      padding: EdgeInsets.fromLTRB(
        10,
        pendingAttachments.isEmpty ? 7 : 8,
        10,
        8,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (pendingAttachments.isNotEmpty)
            _PendingAttachmentStrip(
              attachments: pendingAttachments,
              onRemove: onRemoveAttachment,
            ),
          if (uploading) ...[
            const SizedBox(height: 6),
            const Row(
              children: [
                SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
                SizedBox(width: 8),
                Text('附件上传中', style: KyXText.caption),
              ],
            ),
          ],
          SizedBox(height: pendingAttachments.isEmpty && !uploading ? 0 : 7),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              _ComposerIconButton(
                tooltip: '图片',
                icon: Icons.image_outlined,
                onPressed: disabled ? null : onPickImage,
              ),
              _ComposerIconButton(
                tooltip: '文件',
                icon: Icons.attach_file,
                onPressed: disabled ? null : onPickFile,
              ),
              const SizedBox(width: 6),
              Expanded(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 96),
                  child: TextField(
                    controller: controller,
                    focusNode: focusNode,
                    enabled: !sending,
                    minLines: 1,
                    maxLines: 4,
                    textInputAction: TextInputAction.newline,
                    decoration: InputDecoration(
                      hintText: '写评论',
                      isDense: true,
                      filled: true,
                      fillColor: KyXColors.bg,
                      hintStyle: KyXText.secondary,
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 9,
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: const BorderSide(color: KyXColors.line),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: const BorderSide(
                          color: KyXColors.primary,
                          width: 1.1,
                        ),
                      ),
                      disabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: const BorderSide(color: KyXColors.line),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 7),
              SizedBox(
                width: 38,
                height: 36,
                child: IconButton(
                  tooltip: '发送',
                  onPressed: disabled ? null : onSend,
                  padding: EdgeInsets.zero,
                  icon: sending
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.send_rounded, size: 20),
                  color: KyXColors.primary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ComposerIconButton extends StatelessWidget {
  final String tooltip;
  final IconData icon;
  final VoidCallback? onPressed;

  const _ComposerIconButton({
    required this.tooltip,
    required this.icon,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 34,
      height: 36,
      child: IconButton(
        tooltip: tooltip,
        onPressed: onPressed,
        padding: EdgeInsets.zero,
        icon: Icon(icon, size: 20),
        color: KyXColors.textSecondary,
        disabledColor: KyXColors.textTertiary,
      ),
    );
  }
}

class _PendingAttachmentStrip extends StatelessWidget {
  final List<_PendingRequirementAttachment> attachments;
  final ValueChanged<int> onRemove;

  const _PendingAttachmentStrip({
    required this.attachments,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 60,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: attachments.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final item = attachments[index];
          return item.isImage
              ? _PendingImageAttachment(
                  attachment: item,
                  onRemove: () => onRemove(index),
                )
              : _PendingFileAttachment(
                  attachment: item,
                  onRemove: () => onRemove(index),
                );
        },
      ),
    );
  }
}

class _PendingImageAttachment extends StatelessWidget {
  final _PendingRequirementAttachment attachment;
  final VoidCallback onRemove;

  const _PendingImageAttachment({
    required this.attachment,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(7),
          child: Image.file(
            File(attachment.path),
            width: 58,
            height: 58,
            fit: BoxFit.cover,
            errorBuilder: (_, __, ___) => _AttachmentThumbFallback(
              icon: Icons.image_not_supported_outlined,
              label: attachment.name,
            ),
          ),
        ),
        Positioned(
          top: -7,
          right: -7,
          child: _AttachmentRemoveButton(onPressed: onRemove),
        ),
      ],
    );
  }
}

class _PendingFileAttachment extends StatelessWidget {
  final _PendingRequirementAttachment attachment;
  final VoidCallback onRemove;

  const _PendingFileAttachment({
    required this.attachment,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Container(
          width: 154,
          height: 58,
          padding: const EdgeInsets.fromLTRB(9, 7, 10, 7),
          decoration: BoxDecoration(
            color: KyXColors.bg,
            borderRadius: BorderRadius.circular(7),
            border: Border.all(color: KyXColors.line),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.insert_drive_file_outlined,
                color: KyXColors.primary,
                size: 20,
              ),
              const SizedBox(width: 7),
              Expanded(
                child: Text(
                  attachment.name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.caption.copyWith(color: KyXColors.text),
                ),
              ),
            ],
          ),
        ),
        Positioned(
          top: -7,
          right: -7,
          child: _AttachmentRemoveButton(onPressed: onRemove),
        ),
      ],
    );
  }
}

class _AttachmentRemoveButton extends StatelessWidget {
  final VoidCallback onPressed;

  const _AttachmentRemoveButton({required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 22,
      height: 22,
      child: IconButton(
        onPressed: onPressed,
        padding: EdgeInsets.zero,
        style: IconButton.styleFrom(
          backgroundColor: KyXColors.text.withValues(alpha: 0.72),
          foregroundColor: Colors.white,
        ),
        icon: const Icon(Icons.close, size: 14),
      ),
    );
  }
}

class _ApprovalTimeline extends StatelessWidget {
  final List<RequirementApprovalNode> nodes;

  const _ApprovalTimeline({required this.nodes});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: nodes.asMap().entries.map((entry) {
        final node = entry.value;
        final isLast = entry.key == nodes.length - 1;
        final color = _bpmStatusColor(node.status);
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
                    Container(width: 1, height: 42, color: KyXColors.line),
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
                            _bpmStatusLabel(node.status),
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

class _LogRow extends StatelessWidget {
  final RequirementLog log;
  final bool showDivider;

  const _LogRow({required this.log, required this.showDivider});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(
                Icons.history,
                size: 18,
                color: KyXColors.textTertiary,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_logTitle(log), style: KyXText.bodyStrong),
                    if ((log.remark ?? '').trim().isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(log.remark!.trim(), style: KyXText.secondary),
                    ],
                    const SizedBox(height: 5),
                    Text(
                      '${log.operatorName ?? '-'} · ${_formatDateTime(log.createTime)}',
                      style: KyXText.caption,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        if (showDivider)
          const Divider(height: 1, indent: 44, color: KyXColors.lineSoft),
      ],
    );
  }
}

class _CommentRow extends StatelessWidget {
  final RequirementComment comment;
  final List<RequirementAttachment> attachments;
  final bool showDivider;
  final ValueChanged<RequirementAttachment> onAttachmentTap;

  const _CommentRow({
    required this.comment,
    required this.attachments,
    required this.showDivider,
    required this.onAttachmentTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 13, 16, 13),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              KyXAvatar(
                text: comment.fromUserName ?? '?',
                size: 34,
                color: KyXColors.indigo,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            comment.fromUserName ?? '未知用户',
                            style: KyXText.bodyStrong,
                          ),
                        ),
                        Text(
                          _formatDateTime(comment.createTime),
                          style: KyXText.caption,
                        ),
                      ],
                    ),
                    if (comment.content.trim().isNotEmpty) ...[
                      const SizedBox(height: 5),
                      Text(comment.content.trim(), style: KyXText.body),
                    ],
                    if (attachments.isNotEmpty) ...[
                      const SizedBox(height: 9),
                      _CommentAttachmentGroup(
                        attachments: attachments,
                        onAttachmentTap: onAttachmentTap,
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
        if (showDivider)
          const Divider(height: 1, indent: 60, color: KyXColors.lineSoft),
      ],
    );
  }
}

class _CommentAttachmentGroup extends StatelessWidget {
  final List<RequirementAttachment> attachments;
  final ValueChanged<RequirementAttachment> onAttachmentTap;

  const _CommentAttachmentGroup({
    required this.attachments,
    required this.onAttachmentTap,
  });

  @override
  Widget build(BuildContext context) {
    final images = attachments.where((item) => item.isImage).toList();
    final files = attachments.where((item) => !item.isImage).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (images.isNotEmpty)
          _AttachmentImageGrid(
            attachments: images,
            compact: true,
            onAttachmentTap: onAttachmentTap,
          ),
        if (files.isNotEmpty) ...[
          if (images.isNotEmpty) const SizedBox(height: 8),
          Column(
            children: files
                .map(
                  (item) => _CompactFilePill(
                    attachment: item,
                    onTap: () => onAttachmentTap(item),
                  ),
                )
                .toList(),
          ),
        ],
      ],
    );
  }
}

class _AttachmentPanel extends StatelessWidget {
  final List<RequirementAttachment> attachments;
  final ValueChanged<RequirementAttachment> onAttachmentTap;

  const _AttachmentPanel({
    required this.attachments,
    required this.onAttachmentTap,
  });

  @override
  Widget build(BuildContext context) {
    final images = attachments.where((item) => item.isImage).toList();
    final files = attachments.where((item) => !item.isImage).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (images.isNotEmpty)
          _AttachmentImageGrid(
            attachments: images,
            onAttachmentTap: onAttachmentTap,
          ),
        if (images.isNotEmpty && files.isNotEmpty)
          const Divider(height: 1, color: KyXColors.lineSoft),
        if (files.isNotEmpty)
          Column(
            children: files
                .asMap()
                .entries
                .map(
                  (entry) => _AttachmentRow(
                    attachment: entry.value,
                    showDivider: entry.key != files.length - 1,
                    onTap: () => onAttachmentTap(entry.value),
                  ),
                )
                .toList(),
          ),
      ],
    );
  }
}

class _AttachmentImageGrid extends StatelessWidget {
  final List<RequirementAttachment> attachments;
  final bool compact;
  final ValueChanged<RequirementAttachment> onAttachmentTap;

  const _AttachmentImageGrid({
    required this.attachments,
    required this.onAttachmentTap,
    this.compact = false,
  });

  @override
  Widget build(BuildContext context) {
    final columns = compact ? 3 : 4;
    final spacing = compact ? 7.0 : 8.0;
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      padding: compact ? EdgeInsets.zero : const EdgeInsets.all(16),
      itemCount: attachments.length,
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: columns,
        crossAxisSpacing: spacing,
        mainAxisSpacing: spacing,
      ),
      itemBuilder: (context, index) {
        final attachment = attachments[index];
        return InkWell(
          onTap: () => onAttachmentTap(attachment),
          borderRadius: BorderRadius.circular(7),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(7),
            child: Image.network(
              attachment.url,
              fit: BoxFit.cover,
              errorBuilder: (_, __, ___) => _AttachmentThumbFallback(
                icon: Icons.broken_image_outlined,
                label: attachment.name,
              ),
            ),
          ),
        );
      },
    );
  }
}

class _CompactFilePill extends StatelessWidget {
  final RequirementAttachment attachment;
  final VoidCallback onTap;

  const _CompactFilePill({required this.attachment, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(7),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(9, 7, 9, 7),
          decoration: BoxDecoration(
            color: KyXColors.bg,
            borderRadius: BorderRadius.circular(7),
            border: Border.all(color: KyXColors.line),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.insert_drive_file_outlined,
                size: 18,
                color: KyXColors.primary,
              ),
              const SizedBox(width: 7),
              Expanded(
                child: Text(
                  attachment.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.caption.copyWith(color: KyXColors.text),
                ),
              ),
              const SizedBox(width: 6),
              const Icon(
                Icons.open_in_new,
                size: 15,
                color: KyXColors.textTertiary,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AttachmentThumbFallback extends StatelessWidget {
  final IconData icon;
  final String label;

  const _AttachmentThumbFallback({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: KyXColors.bg,
      alignment: Alignment.center,
      padding: const EdgeInsets.all(6),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 20, color: KyXColors.textTertiary),
          const SizedBox(height: 4),
          Text(
            label,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 10,
              height: 1.1,
              color: KyXColors.textTertiary,
            ),
          ),
        ],
      ),
    );
  }
}

class _AttachmentRow extends StatelessWidget {
  final RequirementAttachment attachment;
  final bool showDivider;
  final VoidCallback onTap;

  const _AttachmentRow({
    required this.attachment,
    required this.showDivider,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return KyXListRow(
      leading: Icon(
        attachment.isImage ? Icons.image_outlined : Icons.description_outlined,
        color: KyXColors.primary,
      ),
      title: attachment.name,
      subtitle: attachment.isImage ? '图片附件' : '文件附件',
      trailing: const Icon(Icons.open_in_new, color: KyXColors.textTertiary),
      onTap: onTap,
      showDivider: showDivider,
    );
  }
}

class _ImagePreviewPage extends StatelessWidget {
  final RequirementAttachment attachment;

  const _ImagePreviewPage({required this.attachment});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        elevation: 0,
        title: Text(
          attachment.name,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
        actions: [
          IconButton(
            tooltip: '外部打开',
            onPressed: () async {
              final uri = Uri.tryParse(attachment.url);
              if (uri == null) return;
              await launchUrl(uri, mode: LaunchMode.externalApplication);
            },
            icon: const Icon(Icons.open_in_new),
          ),
        ],
      ),
      body: Center(
        child: InteractiveViewer(
          minScale: 0.8,
          maxScale: 4,
          child: Image.network(
            attachment.url,
            fit: BoxFit.contain,
            loadingBuilder: (context, child, progress) {
              if (progress == null) return child;
              return const SizedBox(
                width: 28,
                height: 28,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              );
            },
            errorBuilder: (_, __, ___) => const Text(
              '图片加载失败',
              style: TextStyle(color: Colors.white70, fontSize: 14),
            ),
          ),
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;
  final bool showDivider;

  const _InfoRow({
    required this.label,
    required this.value,
    this.showDivider = true,
  });

  @override
  Widget build(BuildContext context) {
    return KyXListRow(
      title: label,
      trailing: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 210),
        child: Text(
          value.isEmpty ? '-' : value,
          textAlign: TextAlign.right,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: KyXText.secondary,
        ),
      ),
      showDivider: showDivider,
    );
  }
}

class _PrioritySelector extends StatelessWidget {
  final int value;
  final ValueChanged<int> onChanged;

  const _PrioritySelector({required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: _priorityMeta.entries.map((entry) {
        final selected = value == entry.key;
        return Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 8),
            child: InkWell(
              onTap: () => onChanged(entry.key),
              borderRadius: BorderRadius.circular(8),
              child: Container(
                height: 40,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: selected
                      ? entry.value.color.withValues(alpha: 0.12)
                      : KyXColors.bg,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: selected ? entry.value.color : KyXColors.line,
                  ),
                ),
                child: Text(
                  entry.value.label,
                  style: TextStyle(
                    color: selected ? entry.value.color : KyXColors.text,
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _RequirementUserPicker extends StatefulWidget {
  const _RequirementUserPicker();

  @override
  State<_RequirementUserPicker> createState() => _RequirementUserPickerState();
}

class _RequirementUserPickerState extends State<_RequirementUserPicker> {
  final TextEditingController _controller = TextEditingController();
  List<RequirementUser> _users = const [];
  List<RequirementUser> _filtered = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadUsers();
    _controller.addListener(_filterUsers);
  }

  @override
  void dispose() {
    _controller
      ..removeListener(_filterUsers)
      ..dispose();
    super.dispose();
  }

  Future<void> _loadUsers() async {
    try {
      final scope = await RequirementService.getScopeOptions().catchError(
        (_) => const RequirementScopeOptions(
          currentTenantId: null,
          crossTenantEnabled: false,
          queryAllEnabled: false,
          selectableTenantIds: [],
        ),
      );
      final users = await RequirementService.getUsers(
        tenantIds: scope.tenantIdsParam,
      );
      if (!mounted) return;
      setState(() {
        _users = users;
        _filtered = users;
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

  void _filterUsers() {
    final keyword = _controller.text.trim().toLowerCase();
    setState(() {
      _filtered = keyword.isEmpty
          ? _users
          : _users.where((item) => item.matches(keyword)).toList();
    });
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.72,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 8, 8),
              child: Row(
                children: [
                  const Expanded(
                    child: Text(
                      '选择负责人',
                      style: TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.w800,
                        color: KyXColors.text,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
              child: TextField(
                controller: _controller,
                decoration: kyxInputDecoration(
                  hintText: '搜索姓名、账号、手机号',
                  prefixIcon: const Icon(
                    Icons.search,
                    size: 20,
                    color: KyXColors.textTertiary,
                  ),
                ),
              ),
            ),
            Expanded(child: _buildUserList()),
          ],
        ),
      ),
    );
  }

  Widget _buildUserList() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _ErrorState(message: _error!, onRetry: _loadUsers);
    }
    if (_filtered.isEmpty) return const _EmptyState(text: '没有匹配的用户');
    return ListView.separated(
      itemCount: _filtered.length,
      separatorBuilder: (_, __) =>
          const Divider(height: 1, indent: 62, color: KyXColors.lineSoft),
      itemBuilder: (context, index) {
        final user = _filtered[index];
        return KyXListRow(
          leading: KyXAvatar(
            text: user.displayName,
            imageUrl: user.avatar,
            color: KyXColors.primary,
          ),
          title: user.displayName,
          subtitle: _joinText([user.deptName, user.tenantName, user.mobile]),
          trailing: const Icon(
            Icons.chevron_right,
            color: KyXColors.textTertiary,
          ),
          onTap: () => Navigator.of(context).pop(user),
          showDivider: false,
        );
      },
    );
  }
}

Future<RequirementUser?> _pickRequirementUser(BuildContext context) {
  return showModalBottomSheet<RequirementUser>(
    context: context,
    isScrollControlled: true,
    backgroundColor: KyXColors.surface,
    builder: (_) => const _RequirementUserPicker(),
  );
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final Color color;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
    this.color = KyXColors.primary,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        height: 30,
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 10),
        decoration: BoxDecoration(
          color: selected ? color.withValues(alpha: 0.11) : KyXColors.bg,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: selected ? color : KyXColors.line),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            color: selected ? color : KyXColors.textSecondary,
          ),
        ),
      ),
    );
  }
}

class _Tag extends StatelessWidget {
  final String label;
  final Color color;

  const _Tag({required this.label, required this.color});

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

class _InlineMeta extends StatelessWidget {
  final IconData icon;
  final String text;

  const _InlineMeta({required this.icon, required this.text});

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

class _MetricText extends StatelessWidget {
  final String label;
  final int value;

  const _MetricText({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return RichText(
      text: TextSpan(
        style: KyXText.caption,
        children: [
          TextSpan(text: '$label '),
          TextSpan(
            text: value.toString(),
            style: const TextStyle(
              color: KyXColors.text,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _UnreadBadge extends StatelessWidget {
  final int count;

  const _UnreadBadge({required this.count});

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minWidth: 20),
      height: 20,
      alignment: Alignment.center,
      padding: const EdgeInsets.symmetric(horizontal: 6),
      decoration: BoxDecoration(
        color: KyXColors.red,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Text(
        count > 99 ? '99+' : count.toString(),
        style: const TextStyle(
          color: Colors.white,
          fontSize: 11,
          fontWeight: FontWeight.w800,
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
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: KyXColors.red, size: 34),
            const SizedBox(height: 10),
            Text(
              message,
              textAlign: TextAlign.center,
              style: KyXText.secondary,
            ),
            const SizedBox(height: 14),
            OutlinedButton(onPressed: onRetry, child: const Text('重试')),
          ],
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final String text;

  const _EmptyState({required this.text});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 18),
        child: Text(text, style: KyXText.secondary),
      ),
    );
  }
}

enum _RequirementScope {
  all('全部'),
  mine('我提出'),
  assigned('分配给我'),
  approval('待审批'),
  unread('未读');

  final String label;
  const _RequirementScope(this.label);
}

class _StatusMeta {
  final String label;
  final Color color;

  const _StatusMeta(this.label, this.color);
}

class _DetailAction {
  final String key;
  final String label;
  final IconData icon;
  final bool primary;
  final bool danger;
  final Future<void> Function() handler;

  const _DetailAction({
    required this.key,
    required this.label,
    required this.icon,
    required this.handler,
    this.primary = false,
    this.danger = false,
  });
}

class _PendingRequirementAttachment {
  final String id;
  final String name;
  final String path;
  final bool isImage;

  const _PendingRequirementAttachment({
    required this.id,
    required this.name,
    required this.path,
    required this.isImage,
  });
}

const Map<int, _StatusMeta> _statusMeta = {
  0: _StatusMeta('待分派', KyXColors.slate),
  1: _StatusMeta('待开发', KyXColors.cyan),
  2: _StatusMeta('开发中', KyXColors.primary),
  3: _StatusMeta('测试中', KyXColors.indigo),
  4: _StatusMeta('待验收', KyXColors.amber),
  5: _StatusMeta('已完成', KyXColors.green),
  6: _StatusMeta('已取消', KyXColors.red),
  7: _StatusMeta('已挂起', KyXColors.amber),
};

const Map<int, _StatusMeta> _priorityMeta = {
  1: _StatusMeta('低', KyXColors.slate),
  2: _StatusMeta('中', KyXColors.primary),
  3: _StatusMeta('高', KyXColors.amber),
  4: _StatusMeta('紧急', KyXColors.red),
};

String _approvalStatusLabel(int? status) {
  return switch (status) {
    1 => '审批中',
    2 => '审批通过',
    3 => '审批拒绝',
    4 => '已撤销',
    _ => '未审批',
  };
}

Color _approvalStatusColor(int? status) {
  return switch (status) {
    1 => KyXColors.primary,
    2 => KyXColors.green,
    3 => KyXColors.red,
    4 => KyXColors.slate,
    _ => KyXColors.textTertiary,
  };
}

String _bpmStatusLabel(int? status) {
  return switch (status) {
    -1 => '未开始',
    0 => '待审批',
    1 => '审批中',
    2 => '已通过',
    3 => '已驳回',
    4 => '已取消',
    5 => '已退回',
    7 => '通过中',
    _ => '未知',
  };
}

Color _bpmStatusColor(int? status) {
  return switch (status) {
    0 || 1 => KyXColors.primary,
    2 || 7 => KyXColors.green,
    3 || 5 => KyXColors.red,
    4 => KyXColors.slate,
    _ => KyXColors.textTertiary,
  };
}

String _logTitle(RequirementLog log) {
  final from = _statusMeta[log.fromStatus]?.label;
  final to = _statusMeta[log.toStatus]?.label;
  if (from != null && to != null && from != to) return '$from -> $to';
  if (to != null) return to;
  if (log.actionType.isNotEmpty) return log.actionType;
  return '处理记录';
}

String _developerNames(RequirementItem item) {
  final names = item.developerMembers
      .map((member) => member.userName)
      .whereType<String>()
      .where((name) => name.trim().isNotEmpty)
      .toList();
  if (names.isEmpty) return '-';
  return names.join('、');
}

String _cleanSummary(String value) {
  final text = value.replaceAll(RegExp(r'\s+'), ' ').trim();
  return text.isEmpty ? '暂无描述' : text;
}

String _shareSummary(String value) {
  final text = value.replaceAll(RegExp(r'\s+'), ' ').trim();
  if (text.isEmpty) return '';
  return text.length <= 80 ? text : '${text.substring(0, 80)}...';
}

String _formatDate(DateTime? value) {
  if (value == null) return '-';
  String two(int input) => input.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)}';
}

String _formatDateTime(DateTime? value) {
  if (value == null) return '-';
  String two(int input) => input.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)} '
      '${two(value.hour)}:${two(value.minute)}';
}

String _joinText(List<String?> values) {
  return values
      .whereType<String>()
      .map((item) => item.trim())
      .where((item) => item.isNotEmpty)
      .join(' · ');
}

bool _isImageFileName(String value) {
  final lower = value.toLowerCase();
  return lower.endsWith('.png') ||
      lower.endsWith('.jpg') ||
      lower.endsWith('.jpeg') ||
      lower.endsWith('.gif') ||
      lower.endsWith('.webp') ||
      lower.endsWith('.bmp');
}

String _cleanError(Object? error) {
  final message = error is ApiException
      ? error.message
      : error.toString().replaceFirst(RegExp(r'^Exception:\s*'), '');
  if (message.contains('No permission to access this requirement')) {
    return '你没有该需求的访问权限';
  }
  if (message.contains('没有该操作权限')) {
    return '你没有该操作权限';
  }
  if (message.contains('系统异常')) {
    return '服务暂时异常，请稍后重试';
  }
  return message;
}
