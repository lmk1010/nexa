import 'dart:async';

import 'package:flutter/material.dart';

import '../services/api_service.dart';
import '../services/chat_service.dart';
import '../services/oa_chat_share.dart';
import 'kyx_design.dart';

Future<bool?> showOaChatShareSheet(
  BuildContext context,
  OaChatSharePayload payload,
) {
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: KyXColors.surface,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
    ),
    builder: (_) => OaChatShareSheet(payload: payload),
  );
}

class OaChatShareSheet extends StatefulWidget {
  final OaChatSharePayload payload;

  const OaChatShareSheet({super.key, required this.payload});

  @override
  State<OaChatShareSheet> createState() => _OaChatShareSheetState();
}

class _OaChatShareSheetState extends State<OaChatShareSheet> {
  final TextEditingController _searchController = TextEditingController();
  final ChatService _chatService = ChatService();

  List<TencentImContact> _contacts = const [];
  bool _loadingContacts = true;
  String? _contactsError;
  String? _sendError;
  String? _sendingKey;
  String _keyword = '';
  int _activeTab = 0;

  @override
  void initState() {
    super.initState();
    _chatService.addListener(_handleChatChanged);
    unawaited(_bootstrap());
  }

  @override
  void dispose() {
    _chatService.removeListener(_handleChatChanged);
    _searchController.dispose();
    super.dispose();
  }

  void _handleChatChanged() {
    if (!mounted) return;
    setState(() {});
  }

  Future<void> _bootstrap() async {
    unawaited(_chatService.startChatService());
    try {
      final contacts = await ApiService.getTencentImContacts(limit: 1000);
      if (!mounted) return;
      setState(() {
        _contacts = contacts;
        _loadingContacts = false;
        _contactsError = null;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loadingContacts = false;
        _contactsError = _cleanError(error);
      });
    }
  }

  List<ChatConversation> get _filteredConversations {
    final keyword = _keyword.trim().toLowerCase();
    final conversations = _chatService.conversations
        .where((item) => item.id.trim().isNotEmpty)
        .toList();
    if (keyword.isEmpty) return conversations;
    return conversations.where((item) {
      return [
        item.name,
        item.id,
        item.lastMessage,
        item.type == 'group' ? '群聊' : '单聊',
      ].any((value) => value.toLowerCase().contains(keyword));
    }).toList();
  }

  List<TencentImContact> get _filteredContacts {
    final keyword = _keyword.trim().toLowerCase();
    if (keyword.isEmpty) return _contacts;
    return _contacts.where((item) {
      return [
        item.displayName,
        item.oaUsername,
        item.ordersysUsername,
        item.imUserId,
        item.remark ?? '',
      ].any((value) => value.toLowerCase().contains(keyword));
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return FractionallySizedBox(
      heightFactor: 0.88,
      child: Column(
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
          _buildHeader(context),
          _buildPreview(),
          _buildSearch(),
          _buildTabs(),
          if (_sendError != null) _buildError(_sendError!),
          Expanded(child: _buildTargetList()),
        ],
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 10, 8, 10),
      child: Row(
        children: [
          const Expanded(child: Text('发送到会话', style: KyXText.title)),
          IconButton(
            onPressed: () => Navigator.of(context).pop(false),
            icon: const Icon(Icons.close, size: 20),
            tooltip: '关闭',
          ),
        ],
      ),
    );
  }

  Widget _buildPreview() {
    return Container(
      width: double.infinity,
      color: KyXColors.bg,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 34,
            height: 34,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: KyXColors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(
              Icons.notifications_active_outlined,
              color: KyXColors.primary,
              size: 18,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(widget.payload.notificationTitle, style: KyXText.caption),
                const SizedBox(height: 3),
                Text(
                  widget.payload.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.bodyStrong,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSearch() {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
      child: TextField(
        controller: _searchController,
        onChanged: (value) => setState(() => _keyword = value),
        style: KyXText.body,
        decoration: InputDecoration(
          hintText: '搜索会话、姓名、账号',
          hintStyle: KyXText.secondary,
          prefixIcon: const Icon(Icons.search, size: 20),
          suffixIcon: _keyword.trim().isEmpty
              ? null
              : IconButton(
                  onPressed: () {
                    _searchController.clear();
                    setState(() => _keyword = '');
                  },
                  icon: const Icon(Icons.close, size: 18),
                  tooltip: '清空',
                ),
          filled: true,
          fillColor: KyXColors.bg,
          contentPadding: const EdgeInsets.symmetric(vertical: 0),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: BorderSide.none,
          ),
        ),
      ),
    );
  }

  Widget _buildTabs() {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Row(
        children: [
          Expanded(
            child: _ShareTabButton(
              label: '最近会话',
              active: _activeTab == 0,
              onTap: () => setState(() => _activeTab = 0),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: _ShareTabButton(
              label: '企业联系人',
              active: _activeTab == 1,
              onTap: () => setState(() => _activeTab = 1),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError(String text) {
    return Container(
      width: double.infinity,
      color: KyXColors.red.withValues(alpha: 0.08),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Text(
        text,
        style: const TextStyle(color: KyXColors.red, fontSize: 12),
      ),
    );
  }

  Widget _buildTargetList() {
    if (_activeTab == 0) return _buildConversationList();
    return _buildContactList();
  }

  Widget _buildConversationList() {
    final conversations = _filteredConversations;
    if (conversations.isEmpty) {
      return _ShareEmptyState(
        icon: Icons.chat_bubble_outline,
        text: _keyword.trim().isEmpty ? '暂无最近会话，可切到企业联系人发送' : '没有匹配的会话',
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.only(bottom: 18),
      itemCount: conversations.length,
      itemBuilder: (context, index) {
        final conversation = conversations[index];
        final key = 'conversation:${conversation.id}';
        return _ShareTargetRow(
          title: conversation.name.trim().isEmpty
              ? conversation.id
              : conversation.name,
          subtitle: _conversationSubtitle(conversation),
          avatarText: conversation.name.trim().isEmpty
              ? conversation.id
              : conversation.name,
          icon: conversation.type == 'group'
              ? Icons.groups_outlined
              : Icons.person_outline,
          loading: _sendingKey == key,
          onTap: _sendingKey == null
              ? () => _sendToConversation(conversation, key)
              : null,
          showDivider: index != conversations.length - 1,
        );
      },
    );
  }

  Widget _buildContactList() {
    if (_loadingContacts) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_contactsError != null) {
      return _ShareRetryState(message: _contactsError!, onRetry: _bootstrap);
    }

    final contacts = _filteredContacts;
    if (contacts.isEmpty) {
      return _ShareEmptyState(
        icon: Icons.people_outline,
        text: _keyword.trim().isEmpty ? '暂无可发送联系人' : '没有匹配的联系人',
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.only(bottom: 18),
      itemCount: contacts.length,
      itemBuilder: (context, index) {
        final contact = contacts[index];
        final key = 'contact:${contact.imUserId}';
        return _ShareTargetRow(
          title: contact.displayName,
          subtitle: _contactSubtitle(contact),
          avatarText: contact.displayName,
          icon: Icons.person_outline,
          loading: _sendingKey == key,
          onTap: _sendingKey == null
              ? () => _sendToContact(contact, key)
              : null,
          showDivider: index != contacts.length - 1,
        );
      },
    );
  }

  Future<void> _sendToConversation(
    ChatConversation conversation,
    String key,
  ) async {
    await _send(
      key: key,
      conversationId: conversation.id,
      name: conversation.name,
      avatar: conversation.avatar,
      type: conversation.type,
    );
  }

  Future<void> _sendToContact(TencentImContact contact, String key) async {
    await _send(
      key: key,
      conversationId: contact.imUserId,
      name: contact.displayName,
      type: 'single',
    );
  }

  Future<void> _send({
    required String key,
    required String conversationId,
    required String name,
    String avatar = '',
    String type = 'single',
  }) async {
    final confirmed = await _confirmSend(name: name, type: type);
    if (confirmed != true || !mounted) return;

    setState(() {
      _sendingKey = key;
      _sendError = null;
    });

    try {
      await _chatService.openConversation(
        conversationId: conversationId,
        name: name,
        avatar: avatar,
        type: type,
      );
      try {
        await _chatService.sendCustomMessage(
          conversationId: conversationId,
          data: widget.payload.customData,
          description: widget.payload.previewText,
          extension: 'json',
          fallbackContent: widget.payload.previewText,
          fallbackMetadata: {'json': widget.payload.customData},
        );
      } catch (_) {
        await _chatService.sendMessage(
          conversationId,
          widget.payload.plainText,
        );
      }
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _sendError = '发送失败：${_cleanError(error)}';
        _sendingKey = null;
      });
    }
  }

  Future<bool> _confirmSend({
    required String name,
    required String type,
  }) async {
    final targetName = _targetDisplayName(name: name, type: type);
    final shareTitle = _shortText(widget.payload.title, maxLength: 34);
    final confirmed = await showDialog<bool>(
      context: context,
      barrierDismissible: true,
      builder: (dialogContext) {
        return AlertDialog(
          backgroundColor: KyXColors.surface,
          surfaceTintColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          titlePadding: const EdgeInsets.fromLTRB(20, 18, 20, 0),
          contentPadding: const EdgeInsets.fromLTRB(20, 12, 20, 4),
          actionsPadding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
          title: const Text('确认发送', style: KyXText.title),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('将内容发送给「$targetName」', style: KyXText.body),
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: KyXColors.bg,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  shareTitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.bodyStrong,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              style: kyxTextButtonStyle(color: KyXColors.textSecondary),
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('取消'),
            ),
            ElevatedButton(
              style: kyxPrimaryButtonStyle().copyWith(
                minimumSize: const WidgetStatePropertyAll(Size(72, 38)),
                padding: const WidgetStatePropertyAll(
                  EdgeInsets.symmetric(horizontal: 18),
                ),
              ),
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('发送'),
            ),
          ],
        );
      },
    );
    return confirmed == true;
  }

  String _targetDisplayName({required String name, required String type}) {
    final cleanName = name.trim();
    if (cleanName.isNotEmpty) return _shortText(cleanName, maxLength: 18);
    return type == 'group' ? '该群聊' : '该会话';
  }

  String _shortText(String text, {required int maxLength}) {
    final value = text.trim();
    if (value.runes.length <= maxLength) return value;
    return '${String.fromCharCodes(value.runes.take(maxLength - 1))}…';
  }

  String _conversationSubtitle(ChatConversation conversation) {
    final parts = <String>[
      conversation.type == 'group' ? '群聊' : '单聊',
      if (conversation.lastMessage.trim().isNotEmpty)
        conversation.lastMessage.trim(),
    ];
    return parts.join(' / ');
  }

  String _contactSubtitle(TencentImContact contact) {
    final parts = [contact.oaUsername, contact.ordersysUsername, contact.remark]
        .whereType<String>()
        .map((item) => item.trim())
        .where(
          (item) =>
              item.isNotEmpty &&
              item != contact.displayName &&
              !item.toLowerCase().startsWith('ordersys prod export'),
        )
        .take(2)
        .toList();
    return parts.isEmpty ? '企业成员' : parts.join(' / ');
  }

  String _cleanError(Object error) {
    final text = error.toString();
    return text
        .replaceFirst('Exception: ', '')
        .replaceFirst('ApiException: ', '')
        .trim();
  }
}

class _ShareTabButton extends StatelessWidget {
  final String label;
  final bool active;
  final VoidCallback onTap;

  const _ShareTabButton({
    required this.label,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: active ? KyXColors.primary : KyXColors.bg,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: SizedBox(
          height: 36,
          child: Center(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: active ? Colors.white : KyXColors.textSecondary,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ShareTargetRow extends StatelessWidget {
  final String title;
  final String subtitle;
  final String avatarText;
  final IconData icon;
  final bool loading;
  final VoidCallback? onTap;
  final bool showDivider;

  const _ShareTargetRow({
    required this.title,
    required this.subtitle,
    required this.avatarText,
    required this.icon,
    required this.loading,
    required this.onTap,
    required this.showDivider,
  });

  @override
  Widget build(BuildContext context) {
    return KyXListRow(
      leading: KyXAvatar(text: avatarText, size: 38, color: KyXColors.primary),
      title: title,
      subtitle: subtitle,
      showDivider: showDivider,
      onTap: onTap,
      trailing: loading
          ? const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Icon(icon, size: 20, color: KyXColors.primary),
    );
  }
}

class _ShareEmptyState extends StatelessWidget {
  final IconData icon;
  final String text;

  const _ShareEmptyState({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 34, color: KyXColors.textTertiary),
            const SizedBox(height: 12),
            Text(text, textAlign: TextAlign.center, style: KyXText.secondary),
          ],
        ),
      ),
    );
  }
}

class _ShareRetryState extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _ShareRetryState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: KyXColors.red, size: 34),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: KyXText.secondary,
            ),
            const SizedBox(height: 14),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 18),
              label: const Text('重试'),
            ),
          ],
        ),
      ),
    );
  }
}
