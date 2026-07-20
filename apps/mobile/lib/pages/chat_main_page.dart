import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_html/flutter_html.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import 'contacts_page.dart';
import 'ai_chat_page.dart';
import 'bpm_approval_page.dart';
import 'executive_cockpit_page.dart';
import 'front_desk_reception_page.dart';
import 'requirement_page.dart';
import 'workbench_page.dart';
import 'settings_page.dart';
import 'debug_page.dart';
import '../config/app_config.dart';
import '../services/api_service.dart';
import '../services/bpm_service.dart';
import '../services/theme_service.dart';
import '../services/chat_service.dart';
import '../services/notification_service.dart';
import '../services/permissions_service.dart';
import '../widgets/immersive_wrapper.dart';
import '../widgets/kyx_design.dart';

class ChatMainPage extends StatefulWidget {
  const ChatMainPage({super.key});

  @override
  State<ChatMainPage> createState() => _ChatMainPageState();
}

class _ChatMainPageState extends State<ChatMainPage> {
  int _selectedIndex = 0;
  late ChatService _chatService;
  // 严格默认：未拿到权限前**不显示 "数据" 和 "对话" tab**。
  // 数据 → app:dashboard:view；对话 → app:chat:use。目前两者在同一层白名单里
  // （biz_boss / tenant_admin），但独立检查权限点，未来 OA 后台可分开分配。
  bool _canAccessDashboard = false;
  bool _canUseChat = false;
  bool _hasUserSelectedTab = false;

  @override
  void initState() {
    super.initState();
    _chatService = ChatService();
    if (AppConfig.enableIm) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _chatService.startChatService();
        NotificationService().requestPermissionIfNeeded();
      });
    }
    unawaited(_loadDashboardAccess());
    // 前台 30s 轮询：权限有变直接触发 _onPermsChange
    PermissionsService.addListener(_onPermsChange);
    PermissionsService.startForegroundPolling();
  }

  @override
  void dispose() {
    PermissionsService.removeListener(_onPermsChange);
    PermissionsService.stopForegroundPolling();
    _chatService.stopChatService();
    super.dispose();
  }

  void _onPermsChange(PermissionsChange change) {
    if (!mounted) return;
    final dashOk = PermissionsService.canAccessDashboard;
    final chatOk = PermissionsService.canUseChat;
    final needRebuild = _canAccessDashboard != dashOk || _canUseChat != chatOk;
    if (needRebuild) {
      setState(() {
        _canAccessDashboard = dashOk;
        _canUseChat = chatOk;
        // 权限被撤了当前 tab 消失了，回到首页避免白屏
        _selectedIndex = 0;
      });
    }
    // 只在**用户可见的 4 个 app:* 权限**变化时提示。
    // 附带角色的 hr:*/bpm:* 等内部权限变化对普通用户无意义 → 静默处理（不弹）。
    final visibleChanged =
        change.lost.any(_appPerms.contains) || change.gained.any(_appPerms.contains);
    if (!visibleChanged) return;
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('您的权限已变更，界面已自动刷新'),
        duration: Duration(seconds: 3),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  static const _appPerms = {
    'app:dashboard:view',
    'app:chat:use',
    'app:ops:view',
    'app:data-center:use',
  };

  @override
  Widget build(BuildContext context) {
    final themeService = Provider.of<ThemeService>(context);

    // 底部 5 tab：数据 / 对话（AI）/ 聊天（IM+通讯录 icon）/ 工作台 / 设置
    // 数据 tab 只显示指标；对话 tab 独立包 AgentTab；聊天 tab 右上角有通讯录跳转 icon
    // 对话 tab 的输入框左侧带 apps icon 拉起 tab picker bottom sheet；同时该 tab 上悬浮菜单隐藏
    // 数据 tab → app:dashboard:view；对话 tab → app:chat:use。
    // 两个权限点独立分配（默认同一层白名单，但 OA 后台可分开）。
    // 无 admin 用户只看到 聊天 / 工作台 / 设置 3 个 tab。
    final navItems = <BottomNavigationBarItem>[
      if (_canAccessDashboard)
        const BottomNavigationBarItem(
          icon: Icon(Icons.insights_rounded),
          activeIcon: Icon(Icons.insights_rounded),
          label: '数据',
        ),
      if (_canUseChat)
        const BottomNavigationBarItem(
          icon: Icon(Icons.smart_toy_rounded),
          activeIcon: Icon(Icons.smart_toy_rounded),
          label: '对话',
        ),
      const BottomNavigationBarItem(
        icon: Icon(Icons.forum_rounded),
        activeIcon: Icon(Icons.forum_rounded),
        label: '聊天',
      ),
      const BottomNavigationBarItem(
        icon: Icon(Icons.grid_view_rounded),
        activeIcon: Icon(Icons.grid_view_rounded),
        label: '工作台',
      ),
      const BottomNavigationBarItem(
        icon: Icon(Icons.tune_rounded),
        activeIcon: Icon(Icons.tune_rounded),
        label: '设置',
      ),
    ];

    void openTabPicker() {
      showModalBottomSheet<int>(
        context: context,
        backgroundColor: Colors.transparent,
        builder: (ctx) => _TabPickerSheet(
          items: navItems,
          currentIndex: _selectedIndex.clamp(0, navItems.length - 1),
          onTap: (i) {
            Navigator.of(ctx).pop();
            setState(() {
              _hasUserSelectedTab = true;
              _selectedIndex = i;
            });
          },
        ),
      );
    }

    final pages = <Widget>[
      if (_canAccessDashboard) const ExecutiveCockpitPage(),
      if (_canUseChat) AiChatPage(onOpenNav: openTabPicker),
      ChangeNotifierProvider.value(
        value: _chatService,
        child: const ChatListPage(),
      ),
      const WorkbenchPage(),
      const SettingsPage(),
    ];
    final currentIndex = _selectedIndex.clamp(0, pages.length - 1);

    return ImmersiveWrapper(
      themeService: themeService,
      child: PopScope(
        // 拦返回键：非首页 tab 时先切回首页（数据），而不是退出 App
        // canPop = false 阻断系统 pop；onPopInvoked 里做 tab 切换
        canPop: currentIndex == 0,
        onPopInvokedWithResult: (didPop, _) {
          if (didPop) return;
          if (currentIndex != 0) {
            setState(() {
              _hasUserSelectedTab = true;
              _selectedIndex = 0;
            });
          }
        },
        child: Scaffold(
        backgroundColor: KyXColors.bg,
        // 悬浮式底部导航：pill 形，跟内容有间距，iOS/现代设计感
        // 用 Stack 让主内容延伸到底、悬浮 pill 叠在上层
        body: Stack(
          children: [
            Positioned.fill(
              child: Padding(
                // AI 对话 tab 隐藏悬浮菜单 → 主内容占满，输入框内嵌 nav icon
                padding: EdgeInsets.only(
                  bottom: _isAiChatTab(currentIndex)
                      ? 0
                      : MediaQuery.of(context).padding.bottom + 74,
                ),
                // IndexedStack 保住每个 tab 的状态（滚动位置、SSE 会话、输入内容）
                // 对话 tab 上再包一层 GestureDetector：水平拖 → 回数据（home）tab
                child: IndexedStack(
                  index: currentIndex,
                  children: [
                    for (int i = 0; i < pages.length; i++)
                      if (_isAiChatTab(i))
                        GestureDetector(
                          behavior: HitTestBehavior.translucent,
                          onHorizontalDragEnd: (details) {
                            // 向右滑（返回 home / 数据 tab）
                            final v = details.primaryVelocity ?? 0;
                            if (v.abs() > 300 && _canAccessDashboard) {
                              setState(() {
                                _hasUserSelectedTab = true;
                                _selectedIndex = 0;
                              });
                            }
                          },
                          child: pages[i],
                        )
                      else
                        pages[i],
                  ],
                ),
              ),
            ),
            if (!_isAiChatTab(currentIndex))
              Positioned(
                left: 16,
                right: 16,
                bottom: MediaQuery.of(context).padding.bottom + 10,
                child: _FloatingNavPill(
                  items: navItems,
                  currentIndex: currentIndex,
                  onTap: (index) {
                    setState(() {
                      _hasUserSelectedTab = true;
                      _selectedIndex = index;
                    });
                  },
                ),
              ),
          ],
        ),
      ),
      ),
    );
  }

  // 对话 tab 的动态 index —— 前面可能有 0 或 1 个 数据 tab。
  // 无对话权限时 null（不匹配任何 index）。
  int? _aiChatTabIndex() {
    if (!_canUseChat) return null;
    return _canAccessDashboard ? 1 : 0;
  }
  bool _isAiChatTab(int index) => _aiChatTabIndex() == index;

  Future<void> _loadDashboardAccess() async {
    await PermissionsService.loadCacheAndScheduleRefresh();
    if (!mounted) return;
    final dashOk = PermissionsService.canAccessDashboard;
    final chatOk = PermissionsService.canUseChat;
    if (_canAccessDashboard == dashOk && _canUseChat == chatOk) return;
    setState(() {
      _canAccessDashboard = dashOk;
      _canUseChat = chatOk;
      // tab 集合变了 → 重置到首页避免索引错位
      if (!_hasUserSelectedTab) _selectedIndex = 0;
    });
  }
}

class ChatListPage extends StatelessWidget {
  const ChatListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer2<ThemeService, ChatService>(
      builder: (context, themeService, chatService, child) {
        return Scaffold(
          backgroundColor: KyXColors.bg,
          appBar: AppBar(
            title: const Text('聊天', style: KyXText.title),
            backgroundColor: KyXColors.surface,
            elevation: 0,
            scrolledUnderElevation: 0,
            automaticallyImplyLeading: false,
            actions: [
              // 通讯录（合并入聊天页，从这里跳转过去）
              IconButton(
                icon: Icon(
                  Icons.contacts_rounded,
                  color: KyXColors.textSecondary,
                  size: 22,
                ),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const ContactsPage(),
                    ),
                  );
                },
                tooltip: '通讯录',
              ),
              // 调试按钮（仅开发模式）
              if (kDebugMode)
                IconButton(
                  icon: Icon(
                    Icons.bug_report,
                    color: KyXColors.textSecondary,
                    size: 20,
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const DebugPage(),
                      ),
                    );
                  },
                  tooltip: 'IM调试工具',
                ),
              // 连接状态指示器
              Container(
                margin: const EdgeInsets.only(right: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: _getStatusColor(chatService.connectionStatus),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 4),
                    Text(
                      _getStatusText(chatService.connectionStatus),
                      style: KyXText.caption,
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.search, color: KyXColors.text),
                onPressed: AppConfig.enableIm
                    ? () => _showStartConversationSheet(context, chatService)
                    : null,
                tooltip: '搜索联系人',
              ),
              IconButton(
                icon: const Icon(Icons.add, color: KyXColors.text),
                onPressed: AppConfig.enableIm
                    ? () => _showStartConversationSheet(context, chatService)
                    : null,
                tooltip: '新建对话',
              ),
            ],
          ),
          body: Column(
            children: [
              // 错误信息显示
              if (chatService.errorMessage != null)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 9,
                  ),
                  color: KyXColors.red.withValues(alpha: 0.08),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.error_outline,
                        color: KyXColors.red,
                        size: 16,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          chatService.errorMessage!,
                          style: const TextStyle(
                            color: KyXColors.red,
                            fontSize: 12,
                          ),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(
                          Icons.close,
                          color: KyXColors.red,
                          size: 16,
                        ),
                        onPressed: () => chatService.clearError(),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                      ),
                    ],
                  ),
                ),
              // 聊天列表
              Expanded(
                child: chatService.conversations.isEmpty
                    ? _buildEmptyState(context, themeService, chatService)
                    : ListView.separated(
                        itemCount: chatService.conversations.length,
                        separatorBuilder: (_, __) => const Divider(
                          height: 1,
                          indent: 64,
                          color: KyXColors.lineSoft,
                        ),
                        itemBuilder: (context, index) {
                          final conversation = chatService.conversations[index];
                          return _buildBusinessConversationItem(
                            context,
                            chatService,
                            conversation,
                          );
                        },
                      ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildEmptyState(
    BuildContext context,
    ThemeService themeService,
    ChatService chatService,
  ) {
    final connected =
        chatService.connectionStatus == ChatConnectionStatus.connected;
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 80, 16, 16),
      children: [
        const Icon(
          Icons.chat_bubble_outline,
          size: 36,
          color: KyXColors.textTertiary,
        ),
        const SizedBox(height: 12),
        Text(
          !AppConfig.enableIm
              ? '即时通讯暂未部署'
              : connected
              ? '暂无聊天记录'
              : '正在连接服务器',
          style: KyXText.title,
        ),
        const SizedBox(height: 6),
        Text(
          connected ? '从联系人列表或右上角新建对话开始沟通。' : '连接完成后会自动加载会话列表。',
          style: KyXText.secondary,
        ),
        const SizedBox(height: 18),
        if (AppConfig.enableIm && connected)
          Align(
            alignment: Alignment.centerLeft,
            child: ElevatedButton.icon(
              onPressed: () =>
                  _showStartConversationSheet(context, chatService),
              icon: const Icon(Icons.add_comment_outlined, size: 18),
              label: const Text('新建对话'),
              style: kyxPrimaryButtonStyle(),
            ),
          ),
        if (AppConfig.enableIm &&
            chatService.connectionStatus == ChatConnectionStatus.disconnected)
          Align(
            alignment: Alignment.centerLeft,
            child: ElevatedButton(
              onPressed: () => chatService.reconnect(),
              style: kyxPrimaryButtonStyle(),
              child: const Text('重新连接'),
            ),
          ),
      ],
    );
  }

  Widget _buildBusinessConversationItem(
    BuildContext context,
    ChatService chatService,
    ChatConversation conversation,
  ) {
    final hasUnread = conversation.unreadCount > 0;
    final title = conversation.name.trim().isNotEmpty
        ? conversation.name.trim()
        : conversation.id;
    return Material(
      color: KyXColors.surface,
      child: InkWell(
        onTap: () {
          chatService.markConversationAsRead(conversation.id);
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => ChangeNotifierProvider.value(
                value: chatService,
                child: ChatDetailPage(
                  conversationId: conversation.id,
                  contactName: title,
                  conversationType: conversation.type,
                ),
              ),
            ),
          );
        },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
          child: Row(
            children: [
              KyXAvatar(
                text: title,
                imageUrl: conversation.avatar,
                size: 40,
                color: conversation.type == 'group'
                    ? KyXColors.cyan
                    : KyXColors.primary,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: KyXColors.text,
                              fontSize: 15,
                              fontWeight: hasUnread
                                  ? FontWeight.w700
                                  : FontWeight.w600,
                            ),
                          ),
                        ),
                        if (conversation.type == 'group')
                          const Padding(
                            padding: EdgeInsets.only(left: 4),
                            child: Icon(
                              Icons.groups_outlined,
                              size: 15,
                              color: KyXColors.textTertiary,
                            ),
                          ),
                        if (conversation.isMuted)
                          const Padding(
                            padding: EdgeInsets.only(left: 4),
                            child: Icon(
                              Icons.notifications_off_outlined,
                              size: 15,
                              color: KyXColors.textTertiary,
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      conversation.lastMessage.isNotEmpty
                          ? conversation.lastMessage
                          : '暂无消息',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: hasUnread
                            ? KyXColors.textSecondary
                            : KyXColors.textTertiary,
                        fontSize: 13,
                        fontWeight: hasUnread
                            ? FontWeight.w600
                            : FontWeight.w400,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    _formatTime(conversation.lastMessageTime),
                    style: KyXText.caption,
                  ),
                  if (hasUnread) ...[
                    const SizedBox(height: 7),
                    Container(
                      constraints: const BoxConstraints(minWidth: 18),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 5,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: KyXColors.red,
                        borderRadius: BorderRadius.circular(9),
                      ),
                      child: Text(
                        conversation.unreadCount > 99
                            ? '99+'
                            : conversation.unreadCount.toString(),
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showStartConversationSheet(
    BuildContext context,
    ChatService chatService,
  ) async {
    final target = await showModalBottomSheet<_ConversationTarget>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => const _StartConversationSheet(),
    );
    if (target == null || !context.mounted) return;

    try {
      await chatService.openConversation(
        conversationId: target.imUserId,
        name: target.name,
      );
      if (!context.mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => ChangeNotifierProvider.value(
            value: chatService,
            child: ChatDetailPage(
              conversationId: target.imUserId,
              contactName: target.name,
              conversationType: 'single',
            ),
          ),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('无法打开聊天：$e')));
    }
  }

  // ignore: unused_element
  Widget _buildConversationItem(
    BuildContext context,
    ThemeService themeService,
    ChatService chatService,
    ChatConversation conversation,
  ) {
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: themeService.primaryColor,
        child: conversation.avatar.isNotEmpty
            ? ClipOval(
                child: Image.network(
                  conversation.avatar,
                  width: 40,
                  height: 40,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) {
                    return Text(
                      conversation.name.isNotEmpty ? conversation.name[0] : '?',
                      style: const TextStyle(color: Colors.white),
                    );
                  },
                ),
              )
            : Text(
                conversation.name.isNotEmpty ? conversation.name[0] : '?',
                style: const TextStyle(color: Colors.white),
              ),
      ),
      title: Row(
        children: [
          Expanded(
            child: Text(
              conversation.name,
              style: TextStyle(
                color: themeService.textPrimaryColor,
                fontWeight: conversation.unreadCount > 0
                    ? FontWeight.bold
                    : FontWeight.normal,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          if (conversation.type == 'group')
            Icon(Icons.group, size: 16, color: themeService.textSecondaryColor),
          if (conversation.isMuted) ...[
            const SizedBox(width: 4),
            Icon(
              Icons.notifications_off_outlined,
              size: 16,
              color: themeService.textSecondaryColor,
            ),
          ],
        ],
      ),
      subtitle: Text(
        conversation.lastMessage.isNotEmpty ? conversation.lastMessage : '暂无消息',
        style: TextStyle(
          color: themeService.textSecondaryColor,
          fontWeight: conversation.unreadCount > 0
              ? FontWeight.w500
              : FontWeight.normal,
        ),
        overflow: TextOverflow.ellipsis,
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            _formatTime(conversation.lastMessageTime),
            style: TextStyle(
              color: themeService.textSecondaryColor,
              fontSize: 12,
            ),
          ),
          if (conversation.unreadCount > 0) ...[
            const SizedBox(height: 4),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: Colors.red,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text(
                conversation.unreadCount > 99
                    ? '99+'
                    : conversation.unreadCount.toString(),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ],
      ),
      onTap: () {
        // 标记为已读
        chatService.markConversationAsRead(conversation.id);

        // 导航到聊天详情页
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => ChangeNotifierProvider.value(
              value: chatService,
              child: ChatDetailPage(
                conversationId: conversation.id,
                contactName: conversation.name,
                conversationType: conversation.type,
              ),
            ),
          ),
        );
      },
    );
  }

  Color _getStatusColor(ChatConnectionStatus status) {
    switch (status) {
      case ChatConnectionStatus.connected:
        return Colors.green;
      case ChatConnectionStatus.connecting:
        return Colors.orange;
      case ChatConnectionStatus.reconnecting:
        return Colors.orange;
      case ChatConnectionStatus.disconnected:
        return Colors.red;
      case ChatConnectionStatus.error:
        return Colors.red;
    }
  }

  String _getStatusText(ChatConnectionStatus status) {
    switch (status) {
      case ChatConnectionStatus.connected:
        return '已连接';
      case ChatConnectionStatus.connecting:
        return '连接中';
      case ChatConnectionStatus.reconnecting:
        return '重连中';
      case ChatConnectionStatus.disconnected:
        return '未连接';
      case ChatConnectionStatus.error:
        return '连接错误';
    }
  }

  String _formatTime(DateTime time) {
    final now = DateTime.now();
    final difference = now.difference(time);

    if (difference.inDays > 0) {
      return '${difference.inDays}天前';
    } else if (difference.inHours > 0) {
      return '${difference.inHours}小时前';
    } else if (difference.inMinutes > 0) {
      return '${difference.inMinutes}分钟前';
    } else {
      return '刚刚';
    }
  }

  // ignore: unused_element
  Widget _buildChatItem(BuildContext context, int index) {
    // 模拟聊天数据
    final chatNames = [
      '技术交流群',
      '产品讨论组',
      '张经理',
      '李总监',
      '王工程师',
      '市场部群',
      '人事部通知',
      '财务部群',
      '客服团队',
      '开发团队',
      '设计团队',
      '测试团队',
      '运营团队',
      '销售团队',
      '法务部',
      '行政部',
      '采购部',
      '质量部',
      '安全部',
      'IT支持',
    ];

    final lastMessages = [
      '好的，我马上处理',
      '这个功能什么时候上线？',
      '请查看一下这个方案',
      '明天开会讨论一下',
      '代码已经提交了',
      '新版本发布通知',
      '请及时更新个人信息',
      '本月财务报表已上传',
      '客户反馈已处理',
      '新功能开发进度',
      '设计稿已更新',
      '测试用例编写完成',
      '活动策划方案',
      '销售数据统计',
      '合同审核通过',
      '办公用品采购',
      '供应商评估报告',
      '质量检查报告',
      '安全检查完成',
      '系统维护通知',
    ];

    final times = [
      '14:30',
      '13:45',
      '12:20',
      '11:15',
      '10:30',
      '09:45',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
      '昨天',
    ];

    final unreadCounts = [
      3,
      0,
      1,
      0,
      5,
      2,
      0,
      0,
      1,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
    ];

    return Container(
      color: Colors.white,
      child: ListTile(
        leading: CircleAvatar(
          radius: 25,
          backgroundColor: Colors.blue[100],
          child: Text(
            chatNames[index].substring(0, 1),
            style: TextStyle(
              color: Colors.blue[700],
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        title: Row(
          children: [
            Expanded(
              child: Text(
                chatNames[index],
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            Text(
              times[index],
              style: TextStyle(fontSize: 12, color: Colors.grey[600]),
            ),
          ],
        ),
        subtitle: Row(
          children: [
            Expanded(
              child: Text(
                lastMessages[index],
                style: TextStyle(fontSize: 14, color: Colors.grey[600]),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            if (unreadCounts[index] > 0)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  unreadCounts[index].toString(),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
          ],
        ),
        onTap: () {
          // TODO: 跳转到聊天详情页面
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => ChatDetailPage(
                conversationId: 'conv_$index',
                contactName: chatNames[index],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _ConversationTarget {
  final String imUserId;
  final String name;

  const _ConversationTarget({required this.imUserId, required this.name});
}

class _StartConversationSheet extends StatefulWidget {
  const _StartConversationSheet();

  @override
  State<_StartConversationSheet> createState() =>
      _StartConversationSheetState();
}

class _StartConversationSheetState extends State<_StartConversationSheet> {
  final TextEditingController _searchController = TextEditingController();
  List<TencentImContact> _contacts = [];
  bool _isLoading = true;
  String? _errorMessage;
  String? _openingImUserId;
  String _keyword = '';

  @override
  void initState() {
    super.initState();
    _loadContacts();
  }

  Future<void> _loadContacts() async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });
      final contacts = await ApiService.getTencentImContacts();
      if (!mounted) return;
      setState(() {
        _contacts = contacts;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e.toString();
        _isLoading = false;
      });
    }
  }

  List<TencentImContact> get _filteredContacts {
    final keyword = _keyword.trim().toLowerCase();
    if (keyword.isEmpty) {
      return _contacts;
    }
    return _contacts.where((contact) {
      final values = [
        contact.displayName,
        contact.oaUserId?.toString() ?? '',
        contact.oaUsername,
        contact.ordersysUsername,
        contact.imUserId,
      ];
      return values.any((value) => value.toLowerCase().contains(keyword));
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final themeService = Provider.of<ThemeService>(context);
    final contacts = _filteredContacts;

    return SafeArea(
      child: FractionallySizedBox(
        heightFactor: 0.78,
        child: Container(
          decoration: BoxDecoration(
            color: KyXColors.surface,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
          ),
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 10, 8, 6),
                child: Row(
                  children: [
                    Expanded(child: Text('选择联系人', style: KyXText.title)),
                    IconButton(
                      icon: const Icon(Icons.close, size: 20),
                      onPressed: () => Navigator.pop(context),
                      tooltip: '关闭',
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                child: TextField(
                  controller: _searchController,
                  style: KyXText.body,
                  decoration: kyxInputDecoration(
                    prefixIcon: const Icon(
                      Icons.search,
                      size: 20,
                      color: KyXColors.textSecondary,
                    ),
                    hintText: '搜索姓名、OA ID、ordersys 或 IM ID',
                  ),
                  onChanged: (value) {
                    setState(() {
                      _keyword = value;
                    });
                  },
                ),
              ),
              Expanded(child: _buildBody(themeService, contacts)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBody(
    ThemeService themeService,
    List<TencentImContact> contacts,
  ) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 36, color: KyXColors.textTertiary),
            const SizedBox(height: 12),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(
                _errorMessage!,
                textAlign: TextAlign.center,
                style: KyXText.secondary,
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: _loadContacts,
              style: kyxPrimaryButtonStyle(),
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    if (contacts.isEmpty) {
      return Center(
        child: Text(
          _contacts.isEmpty ? '暂无可聊天联系人' : '没有匹配的联系人',
          style: KyXText.secondary,
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadContacts,
      child: ListView.separated(
        itemCount: contacts.length,
        separatorBuilder: (_, __) =>
            const Divider(height: 1, indent: 64, color: KyXColors.lineSoft),
        itemBuilder: (context, index) {
          final contact = contacts[index];
          return _buildContactItem(themeService, contact);
        },
      ),
    );
  }

  Widget _buildContactItem(
    ThemeService themeService,
    TencentImContact contact,
  ) {
    final displayName = contact.displayName;
    final avatarText = displayName.isNotEmpty
        ? displayName[0].toUpperCase()
        : '?';
    final isOpening = _openingImUserId == contact.imUserId;

    return Material(
      color: KyXColors.surface,
      child: InkWell(
        onTap: isOpening ? null : () => _openContact(contact),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
          child: Row(
            children: [
              KyXAvatar(text: avatarText, color: KyXColors.primary),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      displayName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: KyXText.bodyStrong,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      contact.imUserId,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: KyXText.secondary,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              isOpening
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(
                      Icons.chat_bubble_outline,
                      color: KyXColors.primary,
                      size: 20,
                    ),
            ],
          ),
        ),
      ),
    );
  }

  void _openContact(TencentImContact contact) {
    setState(() {
      _openingImUserId = contact.imUserId;
    });
    Navigator.pop(
      context,
      _ConversationTarget(
        imUserId: contact.imUserId,
        name: contact.displayName,
      ),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }
}

class ChatDetailPage extends StatefulWidget {
  final String conversationId;
  final String contactName;
  final String conversationType;

  const ChatDetailPage({
    super.key,
    required this.conversationId,
    required this.contactName,
    this.conversationType = 'single',
  });

  @override
  State<ChatDetailPage> createState() => _ChatDetailPageState();
}

class _ChatDetailPageState extends State<ChatDetailPage> {
  static final Map<String, _ConversationScrollState> _conversationScrollStates =
      {};

  static const List<String> _emojiValues = [
    '😀',
    '😄',
    '😊',
    '😂',
    '😍',
    '😎',
    '🥳',
    '😢',
    '😡',
    '👍',
    '👏',
    '🙏',
    '💪',
    '👌',
    '🤝',
    '❤️',
    '🔥',
    '🎉',
    '✅',
    '⭐',
  ];

  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final Set<String> _openingFileMessageIds = {};
  final Set<String> _loadingSoundMessageIds = {};
  final AudioPlayer _voicePlayer = AudioPlayer();
  StreamSubscription<void>? _voiceCompleteSubscription;
  late ChatService _chatService;
  bool _isLoadingHistory = true;
  bool _isRestoringInitialScroll = true;
  bool _isSendingAttachment = false;
  bool _showEmojiPanel = false;
  bool _hasInputText = false;
  bool _isRequestingOlderHistory = false;
  String? _playingSoundMessageKey;
  String? _historyError;

  String get _scrollStateKey =>
      '${widget.conversationType}:${widget.conversationId}';

  @override
  void initState() {
    super.initState();
    _chatService = Provider.of<ChatService>(context, listen: false);
    _messageController.addListener(_handleMessageTextChanged);
    _scrollController.addListener(_handleHistoryScroll);
    _voicePlayer.setReleaseMode(ReleaseMode.stop);
    _voiceCompleteSubscription = _voicePlayer.onPlayerComplete.listen((_) {
      if (!mounted) return;
      setState(() {
        _playingSoundMessageKey = null;
      });
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadInitialHistory();
    });
  }

  void _handleMessageTextChanged() {
    final hasInputText = _messageController.text.trim().isNotEmpty;
    if (hasInputText == _hasInputText || !mounted) return;
    setState(() {
      _hasInputText = hasInputText;
    });
  }

  void _handleHistoryScroll() {
    if (_isLoadingHistory ||
        _isRequestingOlderHistory ||
        !_scrollController.hasClients) {
      return;
    }
    _rememberScrollPosition();
    if (_scrollController.position.pixels <= 80) {
      _loadOlderHistory();
    }
  }

  Future<void> _loadInitialHistory() async {
    if (widget.conversationId.isEmpty) {
      if (mounted) {
        setState(() {
          _isLoadingHistory = false;
          _isRestoringInitialScroll = false;
        });
      }
      return;
    }

    setState(() {
      _isLoadingHistory = true;
      _isRestoringInitialScroll = true;
      _historyError = null;
    });

    try {
      await _chatService.openConversation(
        conversationId: widget.conversationId,
        name: widget.contactName,
        type: widget.conversationType,
      );
      await _chatService.loadHistoryMessages(widget.conversationId);
    } catch (e) {
      _historyError = e.toString();
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingHistory = false;
        });
        _restoreInitialScrollPosition();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer2<ThemeService, ChatService>(
      builder: (context, themeService, chatService, child) {
        final messages = chatService.getMessagesForConversation(
          widget.conversationId,
        );

        return Scaffold(
          backgroundColor: KyXColors.bg,
          appBar: AppBar(
            titleSpacing: 0,
            title: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.contactName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.title,
                ),
                Text(
                  widget.conversationType == 'group' ? '群聊' : '单聊',
                  style: KyXText.caption,
                ),
              ],
            ),
            backgroundColor: KyXColors.surface,
            elevation: 0,
            scrolledUnderElevation: 0,
            iconTheme: const IconThemeData(color: KyXColors.text),
            actions: [
              IconButton(
                icon: const Icon(Icons.more_horiz, size: 22),
                onPressed: () => _openConversationSettings(chatService),
                tooltip: '会话设置',
              ),
            ],
          ),
          body: Column(
            children: [
              // 连接状态提示
              if (chatService.connectionStatus !=
                  ChatConnectionStatus.connected)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 7,
                  ),
                  color: KyXColors.amber.withValues(alpha: 0.1),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(
                        Icons.warning_amber_outlined,
                        color: KyXColors.amber,
                        size: 15,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        chatService.connectionStatus ==
                                ChatConnectionStatus.connecting
                            ? '正在连接...'
                            : '连接已断开，消息可能无法发送',
                        style: TextStyle(color: KyXColors.amber, fontSize: 12),
                      ),
                    ],
                  ),
                ),
              if (_isLoadingHistory)
                const LinearProgressIndicator(minHeight: 2),
              if (_historyError != null)
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 7,
                  ),
                  color: KyXColors.red.withValues(alpha: 0.08),
                  child: Text(
                    '聊天记录加载失败：$_historyError',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: KyXColors.red, fontSize: 12),
                  ),
                ),
              // 消息列表
              Expanded(
                child: _isLoadingHistory
                    ? const Center(child: CircularProgressIndicator())
                    : messages.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(
                              Icons.chat_bubble_outline,
                              size: 34,
                              color: KyXColors.textTertiary,
                            ),
                            const SizedBox(height: 16),
                            const Text(
                              '暂无消息\n开始聊天吧！',
                              textAlign: TextAlign.center,
                              style: KyXText.secondary,
                            ),
                          ],
                        ),
                      )
                    : Stack(
                        children: [
                          Opacity(
                            opacity: _isRestoringInitialScroll ? 0 : 1,
                            child: ListView.builder(
                              controller: _scrollController,
                              padding: const EdgeInsets.fromLTRB(
                                12,
                                12,
                                12,
                                14,
                              ),
                              itemCount:
                                  messages.length +
                                  (_shouldShowHistoryHeader(
                                        chatService,
                                        messages,
                                      )
                                      ? 1
                                      : 0),
                              itemBuilder: (context, index) {
                                final showHistoryHeader =
                                    _shouldShowHistoryHeader(
                                      chatService,
                                      messages,
                                    );
                                if (showHistoryHeader && index == 0) {
                                  return _buildHistoryHeader(chatService);
                                }
                                final message =
                                    messages[showHistoryHeader
                                        ? index - 1
                                        : index];
                                return _buildMessageBubble(
                                  themeService,
                                  message,
                                );
                              },
                            ),
                          ),
                          if (_isRestoringInitialScroll)
                            const Center(child: CircularProgressIndicator()),
                        ],
                      ),
              ),
              // 消息输入框
              _buildMessageInput(themeService, chatService),
            ],
          ),
        );
      },
    );
  }

  bool _shouldShowHistoryHeader(
    ChatService chatService,
    List<ChatMessage> messages,
  ) {
    if (messages.isEmpty) return false;
    return chatService.isLoadingMoreHistory(widget.conversationId) ||
        !chatService.hasMoreHistory(widget.conversationId);
  }

  Widget _buildHistoryHeader(ChatService chatService) {
    final isLoading = chatService.isLoadingMoreHistory(widget.conversationId);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Center(
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isLoading) ...[
              const SizedBox(
                width: 13,
                height: 13,
                child: CircularProgressIndicator(strokeWidth: 1.6),
              ),
              const SizedBox(width: 8),
            ],
            Text(isLoading ? '正在加载更早消息' : '没有更多聊天记录', style: KyXText.caption),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageBubble(ThemeService themeService, ChatMessage message) {
    final isOaShareMessage = _oaShareData(message) != null;
    final usePrimaryBubble =
        message.isMe && message.messageType == 'text' && !isOaShareMessage;
    final bubbleColor = usePrimaryBubble
        ? KyXColors.primary
        : KyXColors.surface;
    final textColor = usePrimaryBubble ? Colors.white : KyXColors.text;
    final isMediaMessage = message.messageType == 'image';
    final senderName = _senderDisplayName(message);
    final senderAvatar = _metadataText(message, 'senderAvatar');
    final isGroupPeerMessage =
        !message.isMe && widget.conversationType == 'group';
    final screenWidth = MediaQuery.of(context).size.width;
    final bubbleMaxWidth = screenWidth * (isOaShareMessage ? 0.78 : 0.68);
    const avatarSize = 34.0;

    final messageBody = Column(
      crossAxisAlignment: message.isMe
          ? CrossAxisAlignment.end
          : CrossAxisAlignment.start,
      children: [
        if (isGroupPeerMessage) ...[
          Padding(
            padding: const EdgeInsets.only(left: 2, bottom: 5),
            child: Text(
              senderName,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: KyXColors.textSecondary,
                fontSize: 12,
                height: 1.15,
              ),
            ),
          ),
        ],
        Container(
          constraints: BoxConstraints(maxWidth: bubbleMaxWidth),
          padding: isOaShareMessage
              ? EdgeInsets.zero
              : isMediaMessage
              ? const EdgeInsets.all(5)
              : const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: bubbleColor,
            borderRadius: BorderRadius.only(
              topLeft: const Radius.circular(10),
              topRight: const Radius.circular(10),
              bottomLeft: Radius.circular(message.isMe ? 10 : 4),
              bottomRight: Radius.circular(message.isMe ? 4 : 10),
            ),
            border: usePrimaryBubble
                ? null
                : Border.all(color: KyXColors.lineSoft),
          ),
          child: _buildMessageContent(themeService, message, textColor),
        ),
        const SizedBox(height: 4),
        Text(
          _formatMessageTime(message.timestamp),
          style: KyXText.caption.copyWith(fontSize: 11),
        ),
      ],
    );

    if (message.isMe) {
      return Padding(
        padding: const EdgeInsets.fromLTRB(56, 0, 0, 12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.end,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Flexible(child: messageBody),
            const SizedBox(width: 8),
            _buildMessageAvatar(
              name: '我',
              userId: message.senderId,
              avatar: senderAvatar,
              size: avatarSize,
              color: KyXColors.green,
              sourceLabel: '当前账号',
              canStartConversation: false,
            ),
          ],
        ),
      );
    }

    final senderUserId = message.senderId.trim().isNotEmpty
        ? message.senderId
        : widget.conversationType == 'single'
        ? widget.conversationId
        : '';

    return Padding(
      padding: const EdgeInsets.fromLTRB(0, 0, 44, 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildMessageAvatar(
            name: senderName,
            userId: senderUserId,
            avatar: senderAvatar,
            size: avatarSize,
            color: widget.conversationType == 'group'
                ? KyXColors.cyan
                : KyXColors.primary,
            sourceLabel: widget.conversationType == 'group' ? '群成员' : '企业成员',
          ),
          const SizedBox(width: 8),
          Flexible(child: messageBody),
        ],
      ),
    );
  }

  Widget _buildMessageAvatar({
    required String name,
    required String userId,
    required String avatar,
    required double size,
    required Color color,
    required String sourceLabel,
    bool canStartConversation = true,
  }) {
    return SizedBox(
      width: size,
      height: size,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: () => _openUserProfile(
            name: name,
            userId: userId,
            avatar: avatar,
            sourceLabel: sourceLabel,
            canStartConversation: canStartConversation,
          ),
          child: KyXAvatar(
            text: name,
            imageUrl: avatar,
            size: size,
            color: color,
          ),
        ),
      ),
    );
  }

  void _openUserProfile({
    required String name,
    required String userId,
    required String avatar,
    required String sourceLabel,
    bool canStartConversation = true,
  }) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ChangeNotifierProvider.value(
          value: _chatService,
          child: _ImUserProfilePage(
            userId: userId,
            name: name,
            avatar: avatar,
            sourceLabel: sourceLabel,
            currentConversationId: widget.conversationId,
            currentConversationType: widget.conversationType,
            canStartConversation: canStartConversation,
          ),
        ),
      ),
    );
  }

  Widget _buildMessageContent(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    switch (message.messageType) {
      case 'html':
        return _buildHtmlMessage(themeService, message, textColor);
      case 'custom':
        return _buildCustomMessage(themeService, message, textColor);
      case 'image':
        return _buildImageMessage(themeService, message);
      case 'video':
        return _buildVideoMessage(themeService, message, textColor);
      case 'file':
        return _buildFileMessage(themeService, message, textColor);
      case 'sound':
        return _buildSoundMessage(themeService, message, textColor);
      case 'location':
        return _buildLocationMessage(themeService, message, textColor);
      case 'merger':
        return _buildMergerMessage(themeService, message, textColor);
      case 'face':
        return Text(
          message.content,
          style: TextStyle(color: textColor, fontSize: 20),
        );
      case 'unknown':
        return _buildMessageInfoRow(
          icon: Icons.help_outline,
          title: '暂不支持的消息',
          subtitle: message.content,
          themeService: themeService,
          textColor: textColor,
        );
      case 'text':
      default:
        return Text(
          message.content,
          style: TextStyle(color: textColor, fontSize: 15, height: 1.35),
        );
    }
  }

  Widget _buildHtmlMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final html = _metadataText(message, 'html');
    if (html.isEmpty) {
      return Text(
        message.content,
        style: TextStyle(color: textColor, fontSize: 15, height: 1.35),
      );
    }

    return Html(
      data: html,
      shrinkWrap: true,
      onLinkTap: (url, attributes, element) {
        if (url == null || url.isEmpty) return;
        Clipboard.setData(ClipboardData(text: url));
        if (!mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('链接已复制')));
      },
      style: {
        'body': Style(
          margin: Margins.zero,
          padding: HtmlPaddings.zero,
          color: textColor,
          fontSize: FontSize(15),
          lineHeight: LineHeight(1.35),
        ),
        'p': Style(margin: Margins.only(bottom: 6)),
        'div': Style(margin: Margins.only(bottom: 4)),
        'img': Style(width: Width(220)),
        'a': Style(color: themeService.primaryColor),
      },
    );
  }

  Widget _buildCustomMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final oaShareData = _oaShareData(message);
    if (oaShareData != null) {
      return _buildOaShareMessage(themeService, oaShareData);
    }

    final title = _metadataText(message, 'title');
    final messageHtml = _metadataText(message, 'messageHtml');
    final extraHtml = _metadataText(message, 'extraHtml');
    if (title.isEmpty && messageHtml.isEmpty && extraHtml.isEmpty) {
      return Text(
        message.content,
        style: TextStyle(color: textColor, fontSize: 15, height: 1.35),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (title.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Text(
              title,
              style: TextStyle(
                color: textColor,
                fontSize: 15,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        if (messageHtml.isNotEmpty)
          _buildInlineHtml(themeService, messageHtml, textColor),
        if (extraHtml.isNotEmpty) ...[
          const SizedBox(height: 6),
          _buildInlineHtml(themeService, extraHtml, textColor),
        ],
        if (messageHtml.isEmpty &&
            extraHtml.isEmpty &&
            message.content.isNotEmpty)
          Text(
            message.content,
            style: TextStyle(color: textColor, fontSize: 15, height: 1.35),
          ),
      ],
    );
  }

  Widget _buildOaShareMessage(
    ThemeService themeService,
    Map<String, dynamic> data,
  ) {
    final module = _jsonText(data, 'module', fallback: 'OA');
    final title = _jsonText(data, 'content', fallback: '业务通知');
    final status = _jsonText(data, 'status');
    final summary = _jsonText(data, 'summary');
    final fields = _jsonFieldList(data['fields']);
    final canOpen = _canOpenOaShare(data);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: canOpen ? () => _openOaShare(data) : null,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 11, 12, 10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Container(
                    width: 32,
                    height: 32,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: themeService.primaryColor.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Icon(
                      _oaShareIcon(data),
                      color: themeService.primaryColor,
                      size: 18,
                    ),
                  ),
                  const SizedBox(width: 9),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '$module通知',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: KyXColors.textSecondary,
                            fontSize: 12,
                            height: 1.2,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          title,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: KyXColors.text,
                            fontSize: 15,
                            height: 1.28,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              if (status.isNotEmpty) ...[
                const SizedBox(height: 9),
                _OaShareStatusPill(text: status),
              ],
              if (fields.isNotEmpty) ...[
                const SizedBox(height: 9),
                ...fields.take(5).map((field) {
                  return _OaShareFieldLine(label: field.$1, value: field.$2);
                }),
              ],
              if (summary.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  summary,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: KyXText.secondary.copyWith(height: 1.35),
                ),
              ],
              if (canOpen) ...[
                const SizedBox(height: 9),
                const Divider(height: 1, color: KyXColors.lineSoft),
                const SizedBox(height: 8),
                Row(
                  children: const [
                    Expanded(
                      child: Text(
                        '查看详情',
                        style: TextStyle(
                          color: KyXColors.primary,
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    Icon(
                      Icons.chevron_right,
                      color: KyXColors.primary,
                      size: 18,
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInlineHtml(
    ThemeService themeService,
    String html,
    Color textColor,
  ) {
    return Html(
      data: html,
      shrinkWrap: true,
      onLinkTap: (url, attributes, element) {
        if (url == null || url.isEmpty) return;
        Clipboard.setData(ClipboardData(text: url));
        if (!mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('链接已复制')));
      },
      style: {
        'body': Style(
          margin: Margins.zero,
          padding: HtmlPaddings.zero,
          color: textColor,
          fontSize: FontSize(15),
          lineHeight: LineHeight(1.35),
        ),
        'p': Style(margin: Margins.only(bottom: 4)),
        'div': Style(margin: Margins.only(bottom: 4)),
        'img': Style(width: Width(220)),
        'a': Style(color: themeService.primaryColor),
      },
    );
  }

  Widget _buildImageMessage(ThemeService themeService, ChatMessage message) {
    final thumbnailUrl = _metadataText(message, 'thumbnailUrl');
    final mediaUrl = _metadataText(message, 'mediaUrl');
    final imageUrl = thumbnailUrl.isNotEmpty ? thumbnailUrl : mediaUrl;
    final isNetworkImage = _isNetworkUrl(imageUrl);
    final isLocalImage = _isLocalFilePath(imageUrl);
    if (!isNetworkImage && !isLocalImage) {
      return _buildMessageInfoRow(
        icon: Icons.image_outlined,
        title: '图片消息',
        subtitle: imageUrl.isNotEmpty ? imageUrl : '图片地址为空',
        themeService: themeService,
        textColor: themeService.textPrimaryColor,
      );
    }

    return GestureDetector(
      onTap: () => _showImagePreview(mediaUrl.isNotEmpty ? mediaUrl : imageUrl),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(10),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 220, maxHeight: 260),
          child: isNetworkImage
              ? Image.network(
                  imageUrl,
                  fit: BoxFit.cover,
                  loadingBuilder: (context, child, loadingProgress) {
                    if (loadingProgress == null) return child;
                    return SizedBox(
                      width: 160,
                      height: 120,
                      child: Center(
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          value: loadingProgress.expectedTotalBytes == null
                              ? null
                              : loadingProgress.cumulativeBytesLoaded /
                                    loadingProgress.expectedTotalBytes!,
                        ),
                      ),
                    );
                  },
                  errorBuilder: (context, error, stackTrace) {
                    return _buildBrokenImage(themeService);
                  },
                )
              : Image.file(
                  File(_localFilePath(imageUrl)),
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) {
                    return _buildBrokenImage(themeService);
                  },
                ),
        ),
      ),
    );
  }

  Widget _buildBrokenImage(ThemeService themeService) {
    return SizedBox(
      width: 180,
      height: 120,
      child: Center(
        child: Icon(
          Icons.broken_image_outlined,
          color: themeService.textSecondaryColor,
        ),
      ),
    );
  }

  Widget _buildVideoMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final thumbnailUrl = _metadataText(message, 'thumbnailUrl');
    final duration = _metadataInt(message, 'duration');
    final hasThumbnail = _isNetworkUrl(thumbnailUrl);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 220,
          height: 132,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (hasThumbnail)
                  Image.network(
                    thumbnailUrl,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) {
                      return _buildMediaPlaceholder(
                        themeService,
                        Icons.videocam_outlined,
                      );
                    },
                  )
                else
                  _buildMediaPlaceholder(themeService, Icons.videocam_outlined),
                Container(color: Colors.black.withValues(alpha: 0.22)),
                const Center(
                  child: Icon(
                    Icons.play_circle_fill,
                    color: Colors.white,
                    size: 44,
                  ),
                ),
              ],
            ),
          ),
        ),
        if (duration > 0) ...[
          const SizedBox(height: 6),
          Text(
            _formatDuration(duration),
            style: TextStyle(color: textColor, fontSize: 13),
          ),
        ],
      ],
    );
  }

  Widget _buildFileMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final fileName = _metadataText(message, 'fileName');
    final fileSize = _metadataInt(message, 'fileSize');
    final fileKey = _fileMessageKey(message);
    final isOpening = _openingFileMessageIds.contains(fileKey);
    final subtitleParts = <String>[
      if (fileSize > 0) _formatFileSize(fileSize),
      isOpening ? '正在准备文件' : '点击下载或打开',
    ];

    return InkWell(
      borderRadius: BorderRadius.circular(8),
      onTap: isOpening ? null : () => _openFileMessage(message),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Flexible(
              child: _buildMessageInfoRow(
                icon: Icons.insert_drive_file_outlined,
                title: fileName.isNotEmpty ? fileName : '文件消息',
                subtitle: subtitleParts.join(' · '),
                themeService: themeService,
                textColor: textColor,
              ),
            ),
            const SizedBox(width: 10),
            isOpening
                ? SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 1.8,
                      color: textColor,
                    ),
                  )
                : Icon(Icons.download_outlined, size: 18, color: textColor),
          ],
        ),
      ),
    );
  }

  Widget _buildSoundMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final duration = _metadataInt(message, 'duration');
    final soundKey = _soundMessageKey(message);
    final isPlaying = _playingSoundMessageKey == soundKey;
    final isLoading = _loadingSoundMessageIds.contains(soundKey);
    final safeDuration = duration.clamp(1, 60).toDouble();
    final width = (108.0 + safeDuration * 1.7).clamp(118.0, 210.0).toDouble();

    return InkWell(
      borderRadius: BorderRadius.circular(8),
      onTap: isLoading ? null : () => _toggleSoundMessage(message),
      child: SizedBox(
        width: width,
        height: 34,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 22,
              height: 22,
              child: isLoading
                  ? CircularProgressIndicator(
                      strokeWidth: 1.8,
                      color: textColor,
                    )
                  : Icon(
                      isPlaying
                          ? Icons.stop_circle_outlined
                          : Icons.play_circle_outline,
                      color: textColor,
                      size: 22,
                    ),
            ),
            const SizedBox(width: 8),
            Expanded(child: _buildVoiceWaveform(textColor, isPlaying)),
            const SizedBox(width: 8),
            Text(
              duration > 0 ? _formatDuration(duration) : '--',
              style: TextStyle(
                color: textColor.withValues(alpha: 0.86),
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildVoiceWaveform(Color color, bool active) {
    const heights = [8.0, 15.0, 11.0, 18.0, 10.0, 14.0, 7.0];
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        for (var i = 0; i < heights.length; i++)
          AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            width: 3,
            height: active && i.isOdd ? heights[i] + 3 : heights[i],
            decoration: BoxDecoration(
              color: color.withValues(alpha: active ? 0.82 : 0.42),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
      ],
    );
  }

  Widget _buildLocationMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final description = _metadataText(message, 'description');
    return _buildMessageInfoRow(
      icon: Icons.place_outlined,
      title: '位置消息',
      subtitle: description.isNotEmpty ? description : message.content,
      themeService: themeService,
      textColor: textColor,
    );
  }

  Widget _buildMergerMessage(
    ThemeService themeService,
    ChatMessage message,
    Color textColor,
  ) {
    final title = _metadataText(message, 'title');
    final abstracts = message.metadata['abstractList'];
    final lines = abstracts is List
        ? abstracts
              .map((item) => item.toString())
              .where((item) => item.isNotEmpty)
              .toList()
        : const <String>[];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildMessageInfoRow(
          icon: Icons.forum_outlined,
          title: title.isNotEmpty ? title : '聊天记录',
          subtitle: lines.isNotEmpty ? lines.first : message.content,
          themeService: themeService,
          textColor: textColor,
        ),
        for (final line in lines.skip(1).take(3)) ...[
          const SizedBox(height: 4),
          Text(
            line,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              color: themeService.textSecondaryColor,
              fontSize: 12,
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildMessageInfoRow({
    required IconData icon,
    required String title,
    required String subtitle,
    required ThemeService themeService,
    required Color textColor,
  }) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: themeService.primaryColor.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(icon, color: themeService.primaryColor, size: 20),
        ),
        const SizedBox(width: 10),
        Flexible(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: textColor,
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (subtitle.isNotEmpty) ...[
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: themeService.textSecondaryColor,
                    fontSize: 12,
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMediaPlaceholder(ThemeService themeService, IconData icon) {
    return Container(
      color: themeService.backgroundColor,
      child: Center(
        child: Icon(icon, color: themeService.textSecondaryColor, size: 38),
      ),
    );
  }

  void _showImagePreview(String url) {
    if (!_isNetworkUrl(url) && !_isLocalFilePath(url)) return;
    showDialog<void>(
      context: context,
      barrierColor: Colors.black.withValues(alpha: 0.88),
      builder: (context) {
        return GestureDetector(
          onTap: () => Navigator.pop(context),
          child: InteractiveViewer(
            child: Center(
              child: _isNetworkUrl(url)
                  ? Image.network(
                      url,
                      fit: BoxFit.contain,
                      errorBuilder: (context, error, stackTrace) {
                        return const Icon(
                          Icons.broken_image_outlined,
                          color: Colors.white,
                          size: 48,
                        );
                      },
                    )
                  : Image.file(
                      File(_localFilePath(url)),
                      fit: BoxFit.contain,
                      errorBuilder: (context, error, stackTrace) {
                        return const Icon(
                          Icons.broken_image_outlined,
                          color: Colors.white,
                          size: 48,
                        );
                      },
                    ),
            ),
          ),
        );
      },
    );
  }

  Future<void> _openFileMessage(ChatMessage message) async {
    final fileKey = _fileMessageKey(message);
    if (_openingFileMessageIds.contains(fileKey)) return;

    setState(() {
      _openingFileMessageIds.add(fileKey);
    });

    try {
      var launchUri = _fileLaunchUri(
        _firstNonEmpty([
          _metadataText(message, 'mediaUrl'),
          _metadataText(message, 'url'),
          _metadataText(message, 'localUrl'),
          _metadataText(message, 'path'),
        ]),
      );

      if (launchUri == null && message.id.trim().isNotEmpty) {
        final onlineUrl = await _chatService.getFileMessageOnlineUrl(
          message.id,
        );
        launchUri = _fileLaunchUri(onlineUrl);
      }

      if (launchUri == null) {
        _showFileOpenMessage('文件地址暂不可用', isError: true);
        return;
      }

      final opened = await launchUrl(
        launchUri,
        mode: LaunchMode.externalApplication,
      );
      if (!opened) {
        _showFileOpenMessage('无法打开文件，请稍后重试', isError: true);
      }
    } catch (e) {
      _showFileOpenMessage('无法打开文件，请稍后重试', isError: true);
    } finally {
      if (mounted) {
        setState(() {
          _openingFileMessageIds.remove(fileKey);
        });
      }
    }
  }

  Uri? _fileLaunchUri(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) return null;

    final uri = Uri.tryParse(trimmed);
    if (uri != null && (uri.scheme == 'http' || uri.scheme == 'https')) {
      return uri;
    }
    if (uri != null && uri.scheme == 'file') {
      return File(uri.toFilePath()).existsSync() ? uri : null;
    }
    if (uri != null && uri.scheme == 'content') {
      return uri;
    }
    if (uri != null && uri.scheme.isNotEmpty) {
      return uri;
    }

    try {
      final file = File(trimmed);
      return file.existsSync() ? Uri.file(file.path) : null;
    } catch (_) {
      return null;
    }
  }

  void _showFileOpenMessage(String message, {bool isError = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Colors.red : null,
      ),
    );
  }

  Future<void> _toggleSoundMessage(ChatMessage message) async {
    final soundKey = _soundMessageKey(message);
    if (_playingSoundMessageKey == soundKey) {
      await _voicePlayer.stop();
      if (!mounted) return;
      setState(() {
        _playingSoundMessageKey = null;
      });
      return;
    }
    if (_loadingSoundMessageIds.contains(soundKey)) return;

    setState(() {
      _loadingSoundMessageIds.add(soundKey);
    });

    try {
      final source = await _resolveSoundSource(message);
      if (source == null) {
        _showFileOpenMessage('语音地址暂不可用', isError: true);
        return;
      }

      await _voicePlayer.stop();
      await _voicePlayer.play(source);
      if (!mounted) return;
      setState(() {
        _playingSoundMessageKey = soundKey;
      });
    } catch (_) {
      _showFileOpenMessage('语音播放失败，请稍后重试', isError: true);
    } finally {
      if (mounted) {
        setState(() {
          _loadingSoundMessageIds.remove(soundKey);
        });
      }
    }
  }

  Future<Source?> _resolveSoundSource(ChatMessage message) async {
    final directSource = _audioSourceFromPath(
      _firstNonEmpty([
        _metadataText(message, 'mediaUrl'),
        _metadataText(message, 'url'),
        _metadataText(message, 'localUrl'),
        _metadataText(message, 'path'),
      ]),
    );
    if (directSource != null) return directSource;

    if (message.id.trim().isEmpty) return null;
    final onlineUrl = await _chatService.getSoundMessageOnlineUrl(message.id);
    return _audioSourceFromPath(onlineUrl);
  }

  Source? _audioSourceFromPath(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) return null;

    final uri = Uri.tryParse(trimmed);
    if (uri != null && (uri.scheme == 'http' || uri.scheme == 'https')) {
      return UrlSource(trimmed);
    }
    if (uri != null && uri.scheme == 'file') {
      final filePath = uri.toFilePath();
      return File(filePath).existsSync() ? DeviceFileSource(filePath) : null;
    }
    if (uri != null && uri.scheme.isNotEmpty) return null;

    try {
      return File(trimmed).existsSync() ? DeviceFileSource(trimmed) : null;
    } catch (_) {
      return null;
    }
  }

  String _fileMessageKey(ChatMessage message) {
    return _firstNonEmpty([
      message.id,
      '${message.senderId}_${message.timestamp.millisecondsSinceEpoch}_${_metadataText(message, 'fileName')}',
    ]);
  }

  String _soundMessageKey(ChatMessage message) {
    return _firstNonEmpty([
      message.id,
      '${message.senderId}_${message.timestamp.millisecondsSinceEpoch}_sound',
    ]);
  }

  String _metadataText(ChatMessage message, String key) {
    final value = message.metadata[key];
    return value?.toString().trim() ?? '';
  }

  int _metadataInt(ChatMessage message, String key) {
    final value = message.metadata[key];
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value) ?? 0;
    return 0;
  }

  // ignore: unused_element
  bool _isNetworkUrl(String value) {
    final uri = Uri.tryParse(value);
    return uri != null && (uri.scheme == 'http' || uri.scheme == 'https');
  }

  bool _isLocalFilePath(String value) {
    if (value.trim().isEmpty) return false;
    final uri = Uri.tryParse(value);
    if (uri != null && uri.scheme == 'file') {
      return File(uri.toFilePath()).existsSync();
    }
    if (uri != null && uri.scheme.isNotEmpty) return false;
    try {
      return File(value).existsSync();
    } catch (_) {
      return false;
    }
  }

  String _localFilePath(String value) {
    final uri = Uri.tryParse(value);
    if (uri != null && uri.scheme == 'file') {
      return uri.toFilePath();
    }
    return value;
  }

  String _senderDisplayName(ChatMessage message) {
    final senderName = _firstNonEmpty([
      message.metadata['senderName'],
      message.metadata['nickName'],
      message.metadata['nickname'],
      message.metadata['friendRemark'],
      message.metadata['remark'],
    ]);
    if (senderName.isNotEmpty) return senderName;
    if (message.isMe) return '我';
    if (widget.conversationType == 'single' && widget.contactName.isNotEmpty) {
      return widget.contactName;
    }
    return '企业成员';
  }

  String _firstNonEmpty(List<dynamic> values) {
    for (final value in values) {
      final text = value?.toString().trim();
      if (text != null && text.isNotEmpty) {
        return text;
      }
    }
    return '';
  }

  String _formatFileSize(int bytes) {
    if (bytes <= 0) return '';
    const units = ['B', 'KB', 'MB', 'GB'];
    var size = bytes.toDouble();
    var unitIndex = 0;
    while (size >= 1024 && unitIndex < units.length - 1) {
      size = size / 1024;
      unitIndex++;
    }
    final fixed = size >= 10 || unitIndex == 0 ? 0 : 1;
    return '${size.toStringAsFixed(fixed)} ${units[unitIndex]}';
  }

  String _formatDuration(int seconds) {
    final minutes = seconds ~/ 60;
    final remainder = seconds % 60;
    if (minutes <= 0) return '${remainder}s';
    return '$minutes:${remainder.toString().padLeft(2, '0')}';
  }

  Widget _buildMessageInput(
    ThemeService themeService,
    ChatService chatService,
  ) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(10, 7, 10, 7),
        decoration: const BoxDecoration(
          color: KyXColors.surface,
          border: Border(top: BorderSide(color: KyXColors.line)),
        ),
        child: Column(
          children: [
            if (_showEmojiPanel) _buildEmojiPanel(themeService, chatService),
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                _buildInputActionButton(
                  tooltip: '发送附件',
                  onPressed: _isSendingAttachment
                      ? null
                      : () => _showAttachmentSheet(chatService),
                  child: _isSendingAttachment
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 1.8),
                        )
                      : const Icon(Icons.add_outlined, size: 22),
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Container(
                    constraints: const BoxConstraints(minHeight: 40),
                    decoration: BoxDecoration(
                      color: KyXColors.bg,
                      borderRadius: BorderRadius.circular(9),
                      border: Border.all(color: KyXColors.lineSoft),
                    ),
                    child: TextField(
                      controller: _messageController,
                      style: KyXText.body,
                      textAlignVertical: TextAlignVertical.center,
                      keyboardType: TextInputType.multiline,
                      decoration: const InputDecoration(
                        hintText: '输入消息',
                        hintStyle: KyXText.secondary,
                        isDense: true,
                        border: InputBorder.none,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 9,
                        ),
                      ),
                      minLines: 1,
                      maxLines: 4,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _sendMessage(chatService),
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                _buildInputActionButton(
                  tooltip: '表情',
                  onPressed: () {
                    FocusScope.of(context).unfocus();
                    setState(() {
                      _showEmojiPanel = !_showEmojiPanel;
                    });
                  },
                  child: Icon(
                    _showEmojiPanel
                        ? Icons.keyboard_outlined
                        : Icons.emoji_emotions_outlined,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 6),
                _buildInputActionButton(
                  tooltip: '发送',
                  backgroundColor: _hasInputText
                      ? KyXColors.primary
                      : Colors.transparent,
                  foregroundColor: _hasInputText
                      ? Colors.white
                      : KyXColors.textSecondary,
                  onPressed: (_isSendingAttachment || !_hasInputText)
                      ? null
                      : () => _sendMessage(chatService),
                  child: const Icon(Icons.arrow_upward, size: 20),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInputActionButton({
    required Widget child,
    required VoidCallback? onPressed,
    required String tooltip,
    Color backgroundColor = Colors.transparent,
    Color foregroundColor = KyXColors.textSecondary,
  }) {
    final enabled = onPressed != null;
    final button = SizedBox(
      width: 40,
      height: 40,
      child: Material(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(9),
        child: InkWell(
          borderRadius: BorderRadius.circular(9),
          onTap: onPressed,
          child: Opacity(
            opacity: enabled ? 1 : 0.62,
            child: IconTheme(
              data: IconThemeData(color: foregroundColor),
              child: Center(child: child),
            ),
          ),
        ),
      ),
    );

    return Tooltip(message: tooltip, child: button);
  }

  Widget _buildEmojiPanel(ThemeService themeService, ChatService chatService) {
    return Container(
      constraints: const BoxConstraints(maxHeight: 148),
      margin: const EdgeInsets.only(bottom: 8),
      child: GridView.builder(
        shrinkWrap: true,
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 8,
          mainAxisSpacing: 6,
          crossAxisSpacing: 6,
        ),
        itemCount: _emojiValues.length,
        itemBuilder: (context, index) {
          final emoji = _emojiValues[index];
          return InkWell(
            borderRadius: BorderRadius.circular(8),
            onTap: () => _sendFaceMessage(chatService, index, emoji),
            child: Container(
              decoration: BoxDecoration(
                color: KyXColors.bg,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Center(
                child: Text(emoji, style: const TextStyle(fontSize: 24)),
              ),
            ),
          );
        },
      ),
    );
  }

  Future<void> _showAttachmentSheet(ChatService chatService) async {
    setState(() {
      _showEmojiPanel = false;
    });

    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(12)),
      ),
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ListTile(
                  minLeadingWidth: 24,
                  leading: const Icon(
                    Icons.image_outlined,
                    color: KyXColors.primary,
                    size: 22,
                  ),
                  title: const Text('发送图片', style: KyXText.bodyStrong),
                  onTap: () {
                    Navigator.pop(context);
                    _pickAndSendImage(chatService);
                  },
                ),
                ListTile(
                  minLeadingWidth: 24,
                  leading: const Icon(
                    Icons.insert_drive_file_outlined,
                    color: KyXColors.primary,
                    size: 22,
                  ),
                  title: const Text('发送文件', style: KyXText.bodyStrong),
                  onTap: () {
                    Navigator.pop(context);
                    _pickAndSendFile(chatService);
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _pickAndSendImage(ChatService chatService) async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.image,
      allowMultiple: false,
      withData: false,
    );
    final file = result?.files.isNotEmpty == true ? result!.files.first : null;
    final path = file?.path;
    if (path == null || path.isEmpty) return;

    await _sendAttachment(
      action: () => chatService.sendImageMessage(
        conversationId: widget.conversationId,
        imagePath: path,
        imageName: file?.name ?? '',
      ),
      failurePrefix: '图片发送失败',
    );
  }

  Future<void> _pickAndSendFile(ChatService chatService) async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.any,
      allowMultiple: false,
      withData: false,
    );
    final file = result?.files.isNotEmpty == true ? result!.files.first : null;
    final path = file?.path;
    if (path == null || path.isEmpty) return;

    await _sendAttachment(
      action: () => chatService.sendFileMessage(
        conversationId: widget.conversationId,
        filePath: path,
        fileName: file?.name ?? path.split(Platform.pathSeparator).last,
      ),
      failurePrefix: '文件发送失败',
    );
  }

  Future<void> _sendAttachment({
    required Future<void> Function() action,
    required String failurePrefix,
  }) async {
    setState(() {
      _isSendingAttachment = true;
    });
    try {
      await action();
      _scrollToBottom();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('$failurePrefix: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSendingAttachment = false;
        });
      }
    }
  }

  Future<void> _sendFaceMessage(
    ChatService chatService,
    int index,
    String data,
  ) async {
    try {
      await chatService.sendFaceMessage(
        conversationId: widget.conversationId,
        index: index,
        data: data,
      );
      _scrollToBottom();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('表情发送失败: $e'), backgroundColor: Colors.red),
      );
    }
  }

  void _openConversationSettings(ChatService chatService) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ChangeNotifierProvider.value(
          value: chatService,
          child: _ConversationSettingsPage(
            conversationId: widget.conversationId,
            contactName: widget.contactName,
            conversationType: widget.conversationType,
          ),
        ),
      ),
    );
  }

  Future<void> _loadOlderHistory() async {
    if (_isRequestingOlderHistory ||
        _isLoadingHistory ||
        !_chatService.hasMoreHistory(widget.conversationId)) {
      return;
    }

    final oldMaxScrollExtent = _scrollController.hasClients
        ? _scrollController.position.maxScrollExtent
        : 0.0;
    final oldOffset = _scrollController.hasClients
        ? _scrollController.position.pixels
        : 0.0;

    setState(() {
      _isRequestingOlderHistory = true;
    });

    try {
      final addedCount = await _chatService.loadMoreHistoryMessages(
        widget.conversationId,
      );
      if (!mounted || addedCount <= 0) return;

      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || !_scrollController.hasClients) return;
        final newMaxScrollExtent = _scrollController.position.maxScrollExtent;
        final delta = newMaxScrollExtent - oldMaxScrollExtent;
        final target = (oldOffset + delta).clamp(
          _scrollController.position.minScrollExtent,
          _scrollController.position.maxScrollExtent,
        );
        _scrollController.jumpTo(target);
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('加载更早消息失败: $e'), backgroundColor: Colors.red),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isRequestingOlderHistory = false;
        });
      }
    }
  }

  void _sendMessage(ChatService chatService) async {
    final text = _messageController.text.trim();
    if (text.isNotEmpty) {
      _messageController.clear();

      try {
        await chatService.sendMessage(widget.conversationId, text);

        _scrollToBottom();
      } catch (e) {
        // 错误处理已在ChatService中完成
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('发送失败: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _scrollToBottom({bool animated = true, VoidCallback? onSettled}) {
    void scrollOnce({required bool animate}) {
      if (!mounted || !_scrollController.hasClients) return;
      final position = _scrollController.position;
      final target = position.maxScrollExtent;
      if (target <= position.minScrollExtent) return;

      if (animate) {
        _scrollController.animateTo(
          target,
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
        );
      } else {
        _scrollController.jumpTo(target);
      }
      _rememberScrollPosition();
    }

    WidgetsBinding.instance.addPostFrameCallback((_) {
      scrollOnce(animate: animated);
      Future.delayed(const Duration(milliseconds: 120), () {
        scrollOnce(animate: false);
        onSettled?.call();
      });
    });
  }

  void _restoreInitialScrollPosition() {
    final state = _conversationScrollStates[_scrollStateKey];
    if (state == null || state.isAtBottom) {
      _scrollToBottom(
        animated: false,
        onSettled: () {
          if (!mounted) return;
          setState(() => _isRestoringInitialScroll = false);
        },
      );
      return;
    }
    _scrollToOffset(
      state.offset,
      onSettled: () {
        if (!mounted) return;
        setState(() => _isRestoringInitialScroll = false);
      },
    );
  }

  void _scrollToOffset(double offset, {VoidCallback? onSettled}) {
    void scrollOnce() {
      if (!mounted || !_scrollController.hasClients) return;
      final position = _scrollController.position;
      final target = offset.clamp(
        position.minScrollExtent,
        position.maxScrollExtent,
      );
      _scrollController.jumpTo(target);
      _rememberScrollPosition();
    }

    WidgetsBinding.instance.addPostFrameCallback((_) {
      scrollOnce();
      Future.delayed(const Duration(milliseconds: 120), () {
        scrollOnce();
        onSettled?.call();
      });
    });
  }

  void _rememberScrollPosition() {
    if (!_scrollController.hasClients) return;
    final position = _scrollController.position;
    _conversationScrollStates[_scrollStateKey] = _ConversationScrollState(
      offset: position.pixels,
      isAtBottom: position.maxScrollExtent - position.pixels <= 80,
    );
  }

  Map<String, dynamic>? _oaShareData(ChatMessage message) {
    final candidates = [message.metadata['json'], message.metadata['rawData']];
    for (final candidate in candidates) {
      final map = _jsonMap(candidate);
      if (map != null && _jsonText(map, 'type') == 'oa_share') {
        return map;
      }
    }
    return null;
  }

  Map<String, dynamic>? _jsonMap(dynamic value) {
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    if (value is String && value.trim().isNotEmpty) {
      try {
        final decoded = jsonDecode(value);
        if (decoded is Map<String, dynamic>) return decoded;
        if (decoded is Map) return Map<String, dynamic>.from(decoded);
      } catch (_) {
        return null;
      }
    }
    return null;
  }

  String _jsonText(
    Map<String, dynamic> data,
    String key, {
    String fallback = '',
  }) {
    final value = data[key]?.toString().trim();
    if (value == null || value.isEmpty || value == 'null') return fallback;
    return value;
  }

  List<(String, String)> _jsonFieldList(dynamic value) {
    if (value is! List) return const [];
    return value
        .map(_jsonMap)
        .whereType<Map<String, dynamic>>()
        .map((item) {
          final label = _jsonText(item, 'label');
          final fieldValue = _jsonText(item, 'value');
          return (label, fieldValue);
        })
        .where((item) => item.$1.isNotEmpty && item.$2.isNotEmpty)
        .toList();
  }

  IconData _oaShareIcon(Map<String, dynamic> data) {
    return switch (_jsonText(data, 'objectType')) {
      'requirement' => Icons.assignment_outlined,
      'bpm' => Icons.approval_outlined,
      'reception' => Icons.record_voice_over_outlined,
      _ => Icons.notifications_active_outlined,
    };
  }

  bool _canOpenOaShare(Map<String, dynamic> data) {
    final objectType = _jsonText(data, 'objectType');
    final objectId = _jsonText(data, 'objectId');
    final extra = _jsonMap(data['extraData']) ?? const <String, dynamic>{};
    return switch (objectType) {
      'requirement' => int.tryParse(objectId) != null,
      'reception' => int.tryParse(objectId) != null,
      'bpm' => _jsonText(
        extra,
        'processInstanceId',
        fallback: objectId,
      ).isNotEmpty,
      _ => false,
    };
  }

  void _openOaShare(Map<String, dynamic> data) {
    final objectType = _jsonText(data, 'objectType');
    final objectId = _jsonText(data, 'objectId');
    final title = _jsonText(data, 'content', fallback: '业务详情');

    if (objectType == 'requirement') {
      final requirementId = int.tryParse(objectId);
      if (requirementId == null) {
        _showFileOpenMessage('暂时无法打开详情', isError: true);
        return;
      }
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => RequirementDetailPage(requirementId: requirementId),
        ),
      );
      return;
    }

    if (objectType == 'reception') {
      final recordId = int.tryParse(objectId);
      if (recordId == null) {
        _showFileOpenMessage('暂时无法打开详情', isError: true);
        return;
      }
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) =>
              ReceptionRecordDetailPage(recordId: recordId, title: title),
        ),
      );
      return;
    }

    if (objectType == 'bpm') {
      final extra = _jsonMap(data['extraData']) ?? const <String, dynamic>{};
      final processInstanceId = _jsonText(
        extra,
        'processInstanceId',
        fallback: objectId,
      );
      if (processInstanceId.isEmpty) {
        _showFileOpenMessage('暂时无法打开详情', isError: true);
        return;
      }
      final taskId = _jsonText(extra, 'taskId');
      final task = BpmTaskItem(
        id: taskId,
        name: title,
        taskDefinitionKey: null,
        parentTaskId: null,
        createTime: null,
        endTime: null,
        status: null,
        reason: null,
        processInstanceId: processInstanceId,
        processInstance: BpmProcessInstance(
          id: processInstanceId,
          name: title,
          createTime: null,
          startTime: null,
          endTime: null,
          status: null,
          businessKey: null,
          category: null,
          startUser: null,
          summary: const [],
          formVariables: const {},
        ),
        ownerUser: null,
        assigneeUser: null,
        reasonRequire: false,
        signEnable: false,
        nodeType: null,
        children: const [],
        buttonsSetting: const {},
        formVariables: const {},
      );
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => BpmApprovalDetailPage(task: task, readOnly: true),
        ),
      );
      return;
    }

    _showFileOpenMessage('暂时无法打开详情', isError: true);
  }

  String _formatMessageTime(DateTime time) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final messageDate = DateTime(time.year, time.month, time.day);

    if (messageDate == today) {
      // 今天的消息只显示时间
      return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
    } else {
      // 其他日期显示日期和时间
      return '${time.month}/${time.day} ${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
    }
  }

  @override
  void dispose() {
    _rememberScrollPosition();
    _messageController.removeListener(_handleMessageTextChanged);
    _scrollController.removeListener(_handleHistoryScroll);
    _voiceCompleteSubscription?.cancel();
    _voicePlayer.dispose();
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }
}

class _ConversationScrollState {
  final double offset;
  final bool isAtBottom;

  const _ConversationScrollState({
    required this.offset,
    required this.isAtBottom,
  });
}

class _OaShareStatusPill extends StatelessWidget {
  final String text;

  const _OaShareStatusPill({required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: KyXColors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        text,
        style: const TextStyle(
          color: KyXColors.primary,
          fontSize: 11,
          height: 1.15,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _OaShareFieldLine extends StatelessWidget {
  final String label;
  final String value;

  const _OaShareFieldLine({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 64,
            child: Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: KyXText.caption,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              value,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: KyXText.secondary.copyWith(color: KyXColors.text),
            ),
          ),
        ],
      ),
    );
  }
}

class _ImUserProfilePage extends StatelessWidget {
  final String userId;
  final String name;
  final String avatar;
  final String sourceLabel;
  final String currentConversationId;
  final String currentConversationType;
  final String roleText;
  final String statusText;
  final bool canStartConversation;

  const _ImUserProfilePage({
    required this.userId,
    required this.name,
    required this.avatar,
    required this.sourceLabel,
    required this.currentConversationId,
    required this.currentConversationType,
    this.roleText = '',
    this.statusText = '',
    this.canStartConversation = true,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<ChatService>(
      builder: (context, chatService, child) {
        final displayName = _displayName;
        final normalizedUserId = userId.trim();
        final currentImUserId = chatService.currentImUserId?.trim() ?? '';
        final isSelf =
            normalizedUserId.isNotEmpty && normalizedUserId == currentImUserId;
        final isCurrentSingleConversation =
            currentConversationType == 'single' &&
            normalizedUserId.isNotEmpty &&
            currentConversationId.trim() == normalizedUserId;
        final canMessage =
            canStartConversation && normalizedUserId.isNotEmpty && !isSelf;
        final actionText = isCurrentSingleConversation ? '回到会话' : '发消息';
        final hasAction = canMessage || isCurrentSingleConversation;

        return Scaffold(
          backgroundColor: KyXColors.bg,
          appBar: AppBar(
            title: const Text('成员资料', style: KyXText.title),
            backgroundColor: KyXColors.surface,
            elevation: 0,
            scrolledUnderElevation: 0,
            iconTheme: const IconThemeData(color: KyXColors.text),
          ),
          body: ListView(
            padding: const EdgeInsets.only(bottom: 24),
            children: [
              _buildHeader(displayName),
              const SizedBox(height: 14),
              _buildInfoSection(),
            ],
          ),
          bottomNavigationBar: hasAction
              ? SafeArea(
                  top: false,
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
                    decoration: const BoxDecoration(
                      color: KyXColors.surface,
                      border: Border(top: BorderSide(color: KyXColors.line)),
                    ),
                    child: ElevatedButton(
                      style: kyxPrimaryButtonStyle(),
                      onPressed: () => _openConversation(context, chatService),
                      child: Text(actionText),
                    ),
                  ),
                )
              : null,
        );
      },
    );
  }

  Widget _buildHeader(String displayName) {
    final subtitle = _joinNonEmpty([sourceLabel, roleText, statusText]);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 22, 16, 22),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      child: Column(
        children: [
          KyXAvatar(
            text: displayName,
            imageUrl: avatar,
            size: 64,
            color: KyXColors.primary,
          ),
          const SizedBox(height: 12),
          Text(
            displayName,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: KyXColors.text,
              fontSize: 20,
              height: 1.2,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (subtitle.isNotEmpty) ...[
            const SizedBox(height: 5),
            Text(
              subtitle,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: KyXText.secondary,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildInfoSection() {
    final rows = [
      _ProfileInfoRow(label: '类型', value: sourceLabel),
      if (roleText.trim().isNotEmpty)
        _ProfileInfoRow(label: '身份', value: roleText),
      if (statusText.trim().isNotEmpty)
        _ProfileInfoRow(label: '状态', value: statusText),
    ];

    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: Column(children: rows),
    );
  }

  Future<void> _openConversation(
    BuildContext context,
    ChatService chatService,
  ) async {
    final normalizedUserId = userId.trim();
    if (normalizedUserId.isEmpty) return;

    if (currentConversationType == 'single' &&
        currentConversationId.trim() == normalizedUserId) {
      Navigator.pop(context);
      return;
    }

    await chatService.openConversation(
      conversationId: normalizedUserId,
      name: _displayName,
      avatar: avatar,
      type: 'single',
    );
    if (!context.mounted) return;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => ChangeNotifierProvider.value(
          value: chatService,
          child: ChatDetailPage(
            conversationId: normalizedUserId,
            contactName: _displayName,
            conversationType: 'single',
          ),
        ),
      ),
    );
  }

  String get _displayName {
    final trimmedName = name.trim();
    if (trimmedName.isNotEmpty) return trimmedName;
    return '企业成员';
  }

  String _joinNonEmpty(List<String> values) {
    return values
        .map((value) => value.trim())
        .where((value) => value.isNotEmpty)
        .join(' · ');
  }
}

class _ProfileInfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _ProfileInfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
      child: Row(
        children: [
          Text(label, style: KyXText.secondary),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.right,
              style: KyXText.bodyStrong,
            ),
          ),
        ],
      ),
    );
  }
}

class _ConversationSettingsPage extends StatefulWidget {
  final String conversationId;
  final String contactName;
  final String conversationType;

  const _ConversationSettingsPage({
    required this.conversationId,
    required this.contactName,
    required this.conversationType,
  });

  @override
  State<_ConversationSettingsPage> createState() =>
      _ConversationSettingsPageState();
}

class _ConversationSettingsPageState extends State<_ConversationSettingsPage> {
  ChatGroupInfo? _groupInfo;
  bool _isLoadingGroupInfo = false;
  bool _isUpdatingMute = false;
  String? _groupError;

  bool get _isGroup => widget.conversationType == 'group';

  @override
  void initState() {
    super.initState();
    if (_isGroup) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _loadGroupInfo();
      });
    }
  }

  Future<void> _loadGroupInfo() async {
    final chatService = Provider.of<ChatService>(context, listen: false);
    setState(() {
      _isLoadingGroupInfo = true;
      _groupError = null;
    });

    try {
      final groupInfo = await chatService.loadGroupInfo(widget.conversationId);
      if (!mounted) return;
      setState(() {
        _groupInfo = groupInfo;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _groupError = e.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingGroupInfo = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer2<ThemeService, ChatService>(
      builder: (context, themeService, chatService, child) {
        final conversation = chatService.getConversation(widget.conversationId);
        final title = _displayName(conversation);
        final avatar = _groupInfo?.avatar ?? conversation?.avatar ?? '';
        final isMuted = conversation?.isMuted ?? _groupInfo?.isMuted ?? false;

        return Scaffold(
          backgroundColor: KyXColors.bg,
          appBar: AppBar(
            title: Text(_isGroup ? '群聊设置' : '会话设置', style: KyXText.title),
            backgroundColor: KyXColors.surface,
            elevation: 0,
            scrolledUnderElevation: 0,
            iconTheme: const IconThemeData(color: KyXColors.text),
          ),
          body: ListView(
            padding: const EdgeInsets.only(bottom: 24),
            children: [
              _buildHeader(themeService, chatService, title, avatar),
              const SizedBox(height: 16),
              _buildMuteSection(themeService, chatService, isMuted),
              const SizedBox(height: 16),
              if (_isGroup)
                _buildGroupSection(themeService, chatService)
              else
                _buildSingleSection(themeService, chatService),
            ],
          ),
        );
      },
    );
  }

  Widget _buildHeader(
    ThemeService themeService,
    ChatService chatService,
    String title,
    String avatar,
  ) {
    final avatarWidget = KyXAvatar(
      text: _initial(title),
      imageUrl: avatar,
      size: 44,
      color: _isGroup ? KyXColors.cyan : KyXColors.primary,
    );

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      child: Row(
        children: [
          _isGroup
              ? avatarWidget
              : InkWell(
                  borderRadius: BorderRadius.circular(8),
                  onTap: () => _openMemberProfile(
                    chatService,
                    name: title,
                    userId: widget.conversationId,
                    avatar: avatar,
                    sourceLabel: '企业成员',
                  ),
                  child: avatarWidget,
                ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: KyXColors.text,
                    fontSize: 17,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _isGroup ? _groupHeaderSubtitle : '企业成员',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: KyXColors.textSecondary,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMuteSection(
    ThemeService themeService,
    ChatService chatService,
    bool isMuted,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: KyXColors.surface,
        border: const Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: SwitchListTile(
        value: isMuted,
        onChanged: _isUpdatingMute
            ? null
            : (value) => _setMuted(chatService, value),
        title: Text('消息免打扰', style: KyXText.bodyStrong),
        subtitle: Text('开启后仍会接收消息，但不弹本地通知', style: KyXText.secondary),
        secondary: _isUpdatingMute
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : Icon(
                Icons.notifications_off_outlined,
                color: KyXColors.textSecondary,
              ),
      ),
    );
  }

  Widget _buildSingleSection(
    ThemeService themeService,
    ChatService chatService,
  ) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.person_outline, color: KyXColors.textSecondary),
          const SizedBox(width: 12),
          Expanded(child: Text('单聊会话', style: KyXText.bodyStrong)),
        ],
      ),
    );
  }

  Widget _buildGroupSection(
    ThemeService themeService,
    ChatService chatService,
  ) {
    if (_isLoadingGroupInfo) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (_groupError != null) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: KyXColors.surface,
          border: Border.all(color: KyXColors.line),
        ),
        child: Column(
          children: [
            Icon(Icons.error_outline, color: KyXColors.textTertiary, size: 36),
            const SizedBox(height: 8),
            Text(
              _groupError!,
              textAlign: TextAlign.center,
              style: KyXText.secondary,
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: _loadGroupInfo,
              child: const Text('重新加载'),
            ),
          ],
        ),
      );
    }

    final groupInfo = _groupInfo;
    if (groupInfo == null) {
      return const SizedBox.shrink();
    }

    return Column(
      children: [
        _buildGroupStats(themeService, groupInfo),
        const SizedBox(height: 16),
        _buildGroupTextSection(
          themeService,
          title: '群公告',
          content: groupInfo.notification.isNotEmpty
              ? groupInfo.notification
              : '暂无群公告',
        ),
        const SizedBox(height: 16),
        _buildGroupTextSection(
          themeService,
          title: '群简介',
          content: groupInfo.introduction.isNotEmpty
              ? groupInfo.introduction
              : '暂无群简介',
        ),
        const SizedBox(height: 16),
        _buildMemberSection(themeService, chatService, groupInfo.members),
      ],
    );
  }

  Widget _buildGroupStats(ThemeService themeService, ChatGroupInfo groupInfo) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: Column(
        children: [
          _buildInfoRow(
            themeService,
            icon: Icons.groups_outlined,
            label: '成员',
            value: '${groupInfo.memberCount} 人',
          ),
          const SizedBox(height: 12),
          _buildInfoRow(
            themeService,
            icon: Icons.verified_user_outlined,
            label: '群主',
            value: groupInfo.owner.isNotEmpty ? groupInfo.owner : '未知',
          ),
          const SizedBox(height: 12),
          _buildInfoRow(
            themeService,
            icon: Icons.block_outlined,
            label: '全员禁言',
            value: groupInfo.isAllMuted ? '已开启' : '未开启',
          ),
        ],
      ),
    );
  }

  Widget _buildGroupTextSection(
    ThemeService themeService, {
    required String title,
    required String content,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: TextStyle(
              color: KyXColors.text,
              fontSize: 15,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            content,
            style: TextStyle(
              color: KyXColors.textSecondary,
              fontSize: 14,
              height: 1.35,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMemberSection(
    ThemeService themeService,
    ChatService chatService,
    List<ChatGroupMember> members,
  ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '群成员',
            style: TextStyle(
              color: KyXColors.text,
              fontSize: 15,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          if (members.isEmpty)
            Text('暂无成员信息', style: KyXText.secondary)
          else
            Wrap(
              spacing: 14,
              runSpacing: 14,
              children: members.map((member) {
                final roleText = _memberRoleText(member.role);
                final statusText = member.isOnline ? '在线' : '离线';
                return SizedBox(
                  width: 64,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(8),
                    onTap: () => _openMemberProfile(
                      chatService,
                      name: member.displayName,
                      userId: member.userId,
                      avatar: member.avatar,
                      sourceLabel: '群成员',
                      roleText: roleText,
                      statusText: statusText,
                    ),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 2),
                      child: Column(
                        children: [
                          KyXAvatar(
                            text: _initial(member.displayName),
                            imageUrl: member.avatar,
                            size: 38,
                            color: KyXColors.primary,
                          ),
                          const SizedBox(height: 6),
                          Text(
                            member.displayName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: KyXColors.text,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(
    ThemeService themeService, {
    required IconData icon,
    required String label,
    required String value,
  }) {
    return Row(
      children: [
        Icon(icon, color: KyXColors.textSecondary, size: 20),
        const SizedBox(width: 10),
        Text(label, style: KyXText.secondary),
        const Spacer(),
        Flexible(
          child: Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.right,
            style: KyXText.bodyStrong,
          ),
        ),
      ],
    );
  }

  void _openMemberProfile(
    ChatService chatService, {
    required String name,
    required String userId,
    required String avatar,
    required String sourceLabel,
    String roleText = '',
    String statusText = '',
  }) {
    final normalizedUserId = userId.trim();
    final currentImUserId = chatService.currentImUserId?.trim() ?? '';
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ChangeNotifierProvider.value(
          value: chatService,
          child: _ImUserProfilePage(
            userId: normalizedUserId,
            name: name,
            avatar: avatar,
            sourceLabel: sourceLabel,
            roleText: roleText,
            statusText: statusText,
            currentConversationId: widget.conversationId,
            currentConversationType: widget.conversationType,
            canStartConversation:
                normalizedUserId.isNotEmpty &&
                normalizedUserId != currentImUserId,
          ),
        ),
      ),
    );
  }

  String get _groupHeaderSubtitle {
    final memberCount = _groupInfo?.memberCount ?? 0;
    if (memberCount > 0) return '$memberCount 位成员';
    return '群聊资料';
  }

  String _memberRoleText(int role) {
    switch (role) {
      case 400:
        return '群主';
      case 300:
        return '管理员';
      default:
        return '成员';
    }
  }

  Future<void> _setMuted(ChatService chatService, bool value) async {
    setState(() {
      _isUpdatingMute = true;
    });
    try {
      await chatService.setConversationMute(
        widget.conversationId,
        isMuted: value,
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('设置失败: $e'), backgroundColor: Colors.red),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isUpdatingMute = false;
        });
      }
    }
  }

  String _displayName(ChatConversation? conversation) {
    final groupName = _groupInfo?.name.trim() ?? '';
    if (groupName.isNotEmpty) return groupName;
    final conversationName = conversation?.name.trim() ?? '';
    if (conversationName.isNotEmpty) return conversationName;
    final widgetName = widget.contactName.trim();
    return widgetName.isNotEmpty ? widgetName : widget.conversationId;
  }

  String _initial(String text) {
    final value = text.trim();
    return value.isNotEmpty ? value.substring(0, 1) : '?';
  }

  // ignore: unused_element
  bool _isNetworkUrl(String value) {
    final uri = Uri.tryParse(value);
    return uri != null && (uri.scheme == 'http' || uri.scheme == 'https');
  }
}

// AI 对话 tab 的 tab picker bottom sheet
// AppBar 顶部圆角 + 5 个 tab 图标横向排列，点选后 pop 回上层切换
class _TabPickerSheet extends StatelessWidget {
  final List<BottomNavigationBarItem> items;
  final int currentIndex;
  final ValueChanged<int> onTap;

  const _TabPickerSheet({
    required this.items,
    required this.currentIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        margin: const EdgeInsets.fromLTRB(12, 0, 12, 12),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          boxShadow: const [
            BoxShadow(
              color: Color(0x1A000000),
              blurRadius: 30,
              offset: Offset(0, 6),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 36,
              height: 4,
              margin: const EdgeInsets.only(bottom: 8),
              decoration: BoxDecoration(
                color: const Color(0xFFECECE8),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                for (int i = 0; i < items.length; i++)
                  Expanded(
                    child: InkWell(
                      onTap: () => onTap(i),
                      borderRadius: BorderRadius.circular(16),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 180),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        margin: const EdgeInsets.symmetric(horizontal: 3, vertical: 4),
                        decoration: BoxDecoration(
                          color: i == currentIndex
                              ? const Color(0xFFF3F3F1)
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Column(
                          children: [
                            IconTheme(
                              data: IconThemeData(
                                color: i == currentIndex
                                    ? const Color(0xFF0E0E10)
                                    : const Color(0xFF98989F),
                                size: 24,
                              ),
                              child: items[i].icon,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              items[i].label ?? '',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: i == currentIndex
                                    ? FontWeight.w700
                                    : FontWeight.w500,
                                color: i == currentIndex
                                    ? const Color(0xFF0E0E10)
                                    : const Color(0xFF98989F),
                              ),
                            ),
                          ],
                        ),
                      ),
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

// 悬浮式底部导航条 —— pill 形，白底大圆角 + 微阴影
// 参考 iOS 16+/现代 fintech 应用（阿里/字节内部 app 的浮层导航）
class _FloatingNavPill extends StatelessWidget {
  final List<BottomNavigationBarItem> items;
  final int currentIndex;
  final ValueChanged<int> onTap;

  const _FloatingNavPill({
    required this.items,
    required this.currentIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 64,
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
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            for (int i = 0; i < items.length; i++)
              Expanded(
                child: _FloatingNavCell(
                  item: items[i],
                  active: i == currentIndex,
                  onTap: () => onTap(i),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _FloatingNavCell extends StatelessWidget {
  final BottomNavigationBarItem item;
  final bool active;
  final VoidCallback onTap;

  const _FloatingNavCell({
    required this.item,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    const activeColor = Color(0xFF0E0E10);
    const inactiveColor = Color(0xFF98989F);
    final color = active ? activeColor : inactiveColor;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(24),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOutCubic,
          margin: const EdgeInsets.symmetric(vertical: 6, horizontal: 3),
          decoration: BoxDecoration(
            color: active ? const Color(0xFFF3F3F1) : Colors.transparent,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              IconTheme(
                data: IconThemeData(color: color, size: 22),
                child: active
                    ? (item.activeIcon ?? item.icon)
                    : item.icon,
              ),
              const SizedBox(height: 3),
              Text(
                item.label ?? '',
                style: TextStyle(
                  fontSize: 10.5,
                  fontWeight: active ? FontWeight.w700 : FontWeight.w500,
                  color: color,
                  letterSpacing: 0.1,
                ),
                overflow: TextOverflow.ellipsis,
                maxLines: 1,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
