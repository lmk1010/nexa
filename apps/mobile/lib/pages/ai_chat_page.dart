// AI 对话页 —— 独立底部 tab
// 复用 executive_cockpit_page.dart 里的 AgentTab（含 SSE、工具卡片、图表等所有已有能力）
// 底部悬浮 pill 菜单收进输入框：apps icon 打开 bottom sheet 切 tab
// 顶栏画上下文进度条：当前 chars / 40k 阈值
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'executive_cockpit_page.dart' show AgentTab, AgentTabState, ConversationDrawer;
import 'file_history_page.dart';

class AiChatPage extends StatefulWidget {
  final VoidCallback? onOpenNav;
  const AiChatPage({super.key, this.onOpenNav});

  @override
  State<AiChatPage> createState() => _AiChatPageState();
}

class _AiChatPageState extends State<AiChatPage> {
  final GlobalKey<AgentTabState> agentKey = GlobalKey<AgentTabState>();
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  static const _canvas = Color(0xFFF7F7F5);
  static const _ink = Color(0xFF0E0E10);
  static const _dim = Color(0xFF6E6E76);
  // 40k chars 是后端 AGENT_CONTEXT_MAX_CHARS，70% 触发压缩 → 28k 是黄区
  static const _contextMax = 40000;
  static const _contextWarn = 28000;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: _canvas,
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(56),
        child: AppBar(
          backgroundColor: _canvas,
          foregroundColor: _ink,
          elevation: 0,
          scrolledUnderElevation: 0,
          titleSpacing: 20,
          automaticallyImplyLeading: false,
          systemOverlayStyle: SystemUiOverlayStyle.dark,
          title: Row(
            children: [
              const Text(
                '对话',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.2,
                  color: _ink,
                ),
              ),
              const SizedBox(width: 16),
              // 上下文进度：细条 + 数字
              Expanded(child: _ContextProgress(agentKey: agentKey)),
            ],
          ),
          actions: [
            IconButton(
              tooltip: '新建会话',
              icon: const Icon(Icons.add_comment_rounded, color: _ink, size: 20),
              onPressed: () {
                agentKey.currentState?.startNewConversation();
              },
            ),
            IconButton(
              tooltip: '历史会话',
              icon: const Icon(Icons.history_rounded, color: _ink, size: 20),
              onPressed: () => _scaffoldKey.currentState?.openEndDrawer(),
            ),
            IconButton(
              tooltip: '导出文件',
              icon: const Icon(Icons.folder_open_rounded, color: _ink, size: 20),
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => const FileHistoryPage()),
                );
              },
            ),
            const SizedBox(width: 4),
          ],
        ),
      ),
      endDrawer: ConversationDrawer(
        onSelect: (id) {
          Navigator.of(context).pop();
          agentKey.currentState?.switchToConversation(id);
        },
      ),
      body: AgentTab(
        key: agentKey,
        overview: null,
        rangeDays: 30,
        onSnapshotRequested: () async {},
        onOpenNav: widget.onOpenNav,
      ),
    );
  }
}

// 上下文进度条：极简 iOS 风，2px 细条 + 8pt 灰字
class _ContextProgress extends StatelessWidget {
  final GlobalKey<AgentTabState> agentKey;
  const _ContextProgress({required this.agentKey});

  @override
  Widget build(BuildContext context) {
    final s = agentKey.currentState;
    if (s == null) {
      // AgentTabState 还没挂载，占位
      return const SizedBox.shrink();
    }
    return ValueListenableBuilder<int>(
      valueListenable: s.contextCharsNotifier,
      builder: (_, chars, __) {
        final pct = (chars / _AiChatPageState._contextMax).clamp(0.0, 1.0);
        final warn = chars >= _AiChatPageState._contextWarn;
        final color = warn
            ? const Color(0xFFB25E09)
            : const Color(0xFF6E6E76);
        return Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                const Expanded(child: SizedBox()),
                Text(
                  '上下文 ${(pct * 100).toStringAsFixed(0)}%',
                  style: TextStyle(
                    fontSize: 11,
                    color: color,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.2,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            ClipRRect(
              borderRadius: BorderRadius.circular(2),
              child: LinearProgressIndicator(
                value: pct,
                minHeight: 2.5,
                backgroundColor: const Color(0xFFECECE8),
                valueColor: AlwaysStoppedAnimation(color),
              ),
            ),
          ],
        );
      },
    );
  }
}
