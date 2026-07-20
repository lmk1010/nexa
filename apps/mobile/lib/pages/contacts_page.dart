import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/api_service.dart';
import '../services/bpm_service.dart';
import 'add_friend_page.dart';
import 'login_page.dart';
import '../services/chat_service.dart';
import '../widgets/kyx_design.dart';

class ContactsPage extends StatefulWidget {
  const ContactsPage({super.key});

  @override
  State<ContactsPage> createState() => _ContactsPageState();
}

class _ContactsPageState extends State<ContactsPage> {
  final TextEditingController _searchController = TextEditingController();
  List<TencentImContact> _contacts = [];
  bool _isLoading = true;
  String? _errorMessage;
  bool _isSearching = false;
  String _keyword = '';

  @override
  void initState() {
    super.initState();
    _loadFriends();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
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

  Future<void> _loadFriends() async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });

      final contacts = await ApiService.getTencentImContacts();

      if (mounted) {
        setState(() {
          _contacts = contacts;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString();
          _isLoading = false;
        });

        // 如果是认证失败，提示用户重新登录
        if (e.toString().contains('认证失败') || e.toString().contains('用户未登录')) {
          _showLoginDialog();
        }
      }
    }
  }

  void _showLoginDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('需要重新登录'),
        content: const Text('您的登录状态已过期，请重新登录后再试。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (context) => const LoginPage()),
                (route) => false,
              );
            },
            child: const Text('去登录'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: _isSearching
            ? TextField(
                controller: _searchController,
                autofocus: true,
                style: KyXText.body,
                decoration: const InputDecoration(
                  hintText: '搜索联系人',
                  border: InputBorder.none,
                ),
                onChanged: (value) {
                  setState(() {
                    _keyword = value;
                  });
                },
              )
            : const Text(
                '我的好友',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: KyXColors.text,
                ),
              ),
        backgroundColor: KyXColors.surface,
        elevation: 0,
        scrolledUnderElevation: 0,
        systemOverlayStyle: const SystemUiOverlayStyle(
          statusBarColor: Colors.white,
          statusBarIconBrightness: Brightness.dark,
          statusBarBrightness: Brightness.light,
        ),
        actions: [
          IconButton(
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => const EnterpriseDirectoryPage(),
              ),
            ),
            icon: const Icon(Icons.business_outlined, size: 21),
            tooltip: '企业通讯录',
          ),
          IconButton(
            icon: Icon(_isSearching ? Icons.close : Icons.search),
            onPressed: () {
              setState(() {
                _isSearching = !_isSearching;
                if (!_isSearching) {
                  _searchController.clear();
                  _keyword = '';
                }
              });
            },
          ),
          IconButton(
            icon: const Icon(Icons.person_add),
            onPressed: () async {
              await Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const AddFriendPage()),
              );
              if (mounted) {
                _loadFriends();
              }
            },
          ),
        ],
      ),
      body: RefreshIndicator(onRefresh: _loadFriends, child: _buildBody()),
    );
  }

  Widget _buildBody() {
    final contacts = _filteredContacts;

    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 36,
              color: KyXColors.textTertiary,
            ),
            const SizedBox(height: 16),
            Text(
              _errorMessage!,
              style: KyXText.secondary,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadFriends,
              style: kyxPrimaryButtonStyle(),
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    if (contacts.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.people_outline,
              size: 36,
              color: KyXColors.textTertiary,
            ),
            const SizedBox(height: 16),
            Text(
              _contacts.isEmpty ? '暂无可聊天联系人' : '没有匹配的联系人',
              style: KyXText.secondary,
            ),
            const SizedBox(height: 16),
            if (_contacts.isEmpty)
              ElevatedButton.icon(
                onPressed: _loadFriends,
                icon: const Icon(Icons.refresh, size: 18),
                label: const Text('刷新'),
                style: kyxPrimaryButtonStyle(),
              ),
          ],
        ),
      );
    }

    return ListView.builder(
      itemCount: contacts.length + 1,
      itemBuilder: (context, index) {
        if (index == 0) {
          return _buildSectionHeader('好友列表 (${contacts.length})');
        }

        final contact = contacts[index - 1];
        return _buildFriendItem(contact);
      },
    );
  }

  Widget _buildSectionHeader(String title) {
    return KyXSectionLabel(title);
  }

  Widget _buildFriendItem(TencentImContact contact) {
    final displayName = contact.displayName;
    final avatarText = displayName.isNotEmpty
        ? displayName.substring(0, 1).toUpperCase()
        : '?';

    return Container(
      color: KyXColors.surface,
      child: Column(
        children: [
          KyXListRow(
            leading: KyXAvatar(text: avatarText, color: KyXColors.primary),
            title: displayName,
            subtitle: _friendSubtitle(contact, displayName),
            trailing: IconButton(
              icon: const Icon(
                Icons.message_outlined,
                color: KyXColors.primary,
                size: 20,
              ),
              constraints: const BoxConstraints.tightFor(width: 36, height: 36),
              padding: EdgeInsets.zero,
              onPressed: () => _startChat(contact, displayName),
              tooltip: '发消息',
            ),
            onTap: () => _startChat(contact, displayName),
          ),
        ],
      ),
    );
  }

  Future<void> _startChat(TencentImContact contact, String displayName) async {
    try {
      final chatService = ChatService();
      await chatService.openConversation(
        conversationId: contact.imUserId,
        name: displayName,
      );
      if (!mounted) return;
      Navigator.pushNamed(
        context,
        '/chat',
        arguments: {'userID': contact.imUserId, 'nickname': displayName},
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('无法打开聊天：$e')));
    }
  }

  String _friendSubtitle(TencentImContact contact, String displayName) {
    final values =
        [contact.oaUsername, contact.ordersysUsername, contact.remark]
            .whereType<String>()
            .map((value) => value.trim())
            .where(
              (value) =>
                  value.isNotEmpty &&
                  value != displayName &&
                  !value.toLowerCase().startsWith('ordersys prod export'),
            )
            .toList();
    return values.isEmpty ? '可发起会话' : values.take(2).join(' / ');
  }
}

class EnterpriseDirectoryPage extends StatefulWidget {
  const EnterpriseDirectoryPage({super.key});

  @override
  State<EnterpriseDirectoryPage> createState() =>
      _EnterpriseDirectoryPageState();
}

class _EnterpriseDirectoryPageState extends State<EnterpriseDirectoryPage> {
  final TextEditingController _searchController = TextEditingController();
  late Future<_EnterpriseDirectoryData> _future;
  String _keyword = '';

  @override
  void initState() {
    super.initState();
    _future = _loadDirectory();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<_EnterpriseDirectoryData> _loadDirectory() async {
    final contactsFuture = ApiService.getTencentImContacts(limit: 1000);
    final usersFuture = BpmService.getSimpleUsers();
    List<BpmDeptNode> depts = const [];
    var deptTreeAvailable = true;

    try {
      depts = await BpmService.getDeptTreeWithEmployeeCount();
    } catch (_) {
      deptTreeAvailable = false;
    }

    final contacts = await contactsFuture;
    final users = await usersFuture;
    return _EnterpriseDirectoryData.build(
      users: users,
      contacts: contacts,
      depts: depts,
      deptTreeAvailable: deptTreeAvailable,
    );
  }

  Future<void> _refresh() async {
    setState(() {
      _future = _loadDirectory();
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '企业通讯录',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: KyXColors.text,
          ),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: FutureBuilder<_EnterpriseDirectoryData>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _DirectoryErrorState(
              message: _cleanError(snapshot.error),
              onRetry: _refresh,
            );
          }

          final data = snapshot.data!;
          final groups = data.filteredGroups(_keyword);
          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.only(bottom: 28),
              children: [
                _DirectorySearchBar(
                  controller: _searchController,
                  onChanged: (value) => setState(() => _keyword = value),
                  onClear: () {
                    _searchController.clear();
                    setState(() => _keyword = '');
                  },
                ),
                _DirectorySummary(data: data),
                if (!data.deptTreeAvailable)
                  const Padding(
                    padding: EdgeInsets.fromLTRB(16, 8, 16, 0),
                    child: Text(
                      '当前账号暂时无法读取完整部门树，已按用户部门分组展示。',
                      style: KyXText.caption,
                    ),
                  ),
                const KyXSectionLabel('组织架构'),
                if (groups.isEmpty)
                  const _DirectoryEmptyState(text: '没有匹配的成员')
                else
                  KyXListSection(
                    child: Column(
                      children: groups
                          .asMap()
                          .entries
                          .map(
                            (entry) => _DeptGroupView(
                              group: entry.value,
                              depth: 0,
                              initiallyExpanded: entry.key < 3,
                              onStartChat: _startChat,
                            ),
                          )
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

  Future<void> _startChat(_EnterpriseContactEntry entry) async {
    final contact = entry.contact;
    if (contact == null || contact.imUserId.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('该成员暂未开通 IM，不能发起会话')));
      return;
    }

    try {
      final chatService = ChatService();
      await chatService.openConversation(
        conversationId: contact.imUserId,
        name: entry.displayName,
      );
      if (!mounted) return;
      Navigator.pushNamed(
        context,
        '/chat',
        arguments: {'userID': contact.imUserId, 'nickname': entry.displayName},
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('无法打开聊天：$e')));
    }
  }
}

class _DirectorySearchBar extends StatelessWidget {
  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final VoidCallback onClear;

  const _DirectorySearchBar({
    required this.controller,
    required this.onChanged,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
      child: TextField(
        controller: controller,
        onChanged: onChanged,
        style: KyXText.body,
        decoration: InputDecoration(
          hintText: '搜索姓名、账号、部门',
          hintStyle: KyXText.secondary,
          prefixIcon: const Icon(Icons.search, size: 20),
          suffixIcon: controller.text.isEmpty
              ? null
              : IconButton(
                  onPressed: onClear,
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
}

class _DirectorySummary extends StatelessWidget {
  final _EnterpriseDirectoryData data;

  const _DirectorySummary({required this.data});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 11, 16, 12),
      child: Row(
        children: [
          _SummaryItem(label: '成员', value: data.totalUsers.toString()),
          _SummaryDivider(),
          _SummaryItem(label: '可聊天', value: data.chatEnabledUsers.toString()),
          _SummaryDivider(),
          _SummaryItem(label: '部门', value: data.departmentCount.toString()),
        ],
      ),
    );
  }
}

class _SummaryItem extends StatelessWidget {
  final String label;
  final String value;

  const _SummaryItem({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(value, style: KyXText.title),
          const SizedBox(height: 2),
          Text(label, style: KyXText.caption),
        ],
      ),
    );
  }
}

class _SummaryDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(width: 1, height: 28, color: KyXColors.lineSoft);
  }
}

class _DeptGroupView extends StatelessWidget {
  final _EnterpriseDeptGroup group;
  final int depth;
  final bool initiallyExpanded;
  final ValueChanged<_EnterpriseContactEntry> onStartChat;

  const _DeptGroupView({
    required this.group,
    required this.depth,
    required this.initiallyExpanded,
    required this.onStartChat,
  });

  @override
  Widget build(BuildContext context) {
    final leftPadding = 16.0 + depth * 14;
    return Theme(
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        initiallyExpanded: initiallyExpanded,
        tilePadding: EdgeInsets.only(left: leftPadding, right: 12),
        childrenPadding: EdgeInsets.zero,
        leading: Icon(
          depth == 0 ? Icons.account_tree_outlined : Icons.apartment_outlined,
          size: 20,
          color: KyXColors.primary,
        ),
        title: Text(
          group.name,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: KyXText.bodyStrong,
        ),
        subtitle: Text('${group.totalCount} 人', style: KyXText.caption),
        children: [
          ...group.users.asMap().entries.map(
            (entry) => _DirectoryUserRow(
              entry: entry.value,
              depth: depth + 1,
              showDivider:
                  entry.key != group.users.length - 1 ||
                  group.children.isNotEmpty,
              onTap: () => onStartChat(entry.value),
            ),
          ),
          ...group.children.asMap().entries.map(
            (entry) => _DeptGroupView(
              group: entry.value,
              depth: depth + 1,
              initiallyExpanded: depth < 1,
              onStartChat: onStartChat,
            ),
          ),
        ],
      ),
    );
  }
}

class _DirectoryUserRow extends StatelessWidget {
  final _EnterpriseContactEntry entry;
  final int depth;
  final bool showDivider;
  final VoidCallback onTap;

  const _DirectoryUserRow({
    required this.entry,
    required this.depth,
    required this.showDivider,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return KyXListRow(
      padding: EdgeInsets.fromLTRB(16.0 + depth * 14, 10, 12, 10),
      leading: KyXAvatar(
        text: entry.displayName,
        imageUrl: entry.user?.avatar,
        color: entry.canChat ? KyXColors.primary : KyXColors.slate,
      ),
      title: entry.displayName,
      subtitle: entry.subtitle,
      trailing: entry.canChat
          ? const Icon(
              Icons.message_outlined,
              size: 19,
              color: KyXColors.primary,
            )
          : const Text('未开通', style: KyXText.caption),
      showDivider: showDivider,
      onTap: onTap,
    );
  }
}

class _DirectoryEmptyState extends StatelessWidget {
  final String text;

  const _DirectoryEmptyState({required this.text});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 220,
      child: Center(child: Text(text, style: KyXText.secondary)),
    );
  }
}

class _DirectoryErrorState extends StatelessWidget {
  final String message;
  final Future<void> Function() onRetry;

  const _DirectoryErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline,
              size: 36,
              color: KyXColors.textTertiary,
            ),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: KyXText.secondary,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: onRetry,
              style: kyxPrimaryButtonStyle(),
              child: const Text('重试'),
            ),
          ],
        ),
      ),
    );
  }
}

class _EnterpriseDirectoryData {
  final List<_EnterpriseDeptGroup> groups;
  final int totalUsers;
  final int chatEnabledUsers;
  final int departmentCount;
  final bool deptTreeAvailable;

  const _EnterpriseDirectoryData({
    required this.groups,
    required this.totalUsers,
    required this.chatEnabledUsers,
    required this.departmentCount,
    required this.deptTreeAvailable,
  });

  factory _EnterpriseDirectoryData.build({
    required List<BpmUser> users,
    required List<TencentImContact> contacts,
    required List<BpmDeptNode> depts,
    required bool deptTreeAvailable,
  }) {
    final contactsByUserId = <int, TencentImContact>{};
    final contactsByUsername = <String, TencentImContact>{};
    for (final contact in contacts) {
      if (contact.oaUserId != null) {
        contactsByUserId[contact.oaUserId!] = contact;
      }
      final username = contact.oaUsername.trim().toLowerCase();
      if (username.isNotEmpty) contactsByUsername[username] = contact;
    }

    final entries = <_EnterpriseContactEntry>[];
    final usedContactKeys = <String>{};
    for (final user in users) {
      final contact =
          (user.id == null ? null : contactsByUserId[user.id!]) ??
          contactsByUsername[user.username.trim().toLowerCase()];
      if (contact != null) usedContactKeys.add(_contactKey(contact));
      entries.add(_EnterpriseContactEntry(user: user, contact: contact));
    }

    for (final contact in contacts) {
      if (usedContactKeys.contains(_contactKey(contact))) continue;
      entries.add(_EnterpriseContactEntry(user: null, contact: contact));
    }

    final groups = depts.isEmpty
        ? _buildGroupsByDeptName(entries)
        : _buildGroupsByDeptTree(entries, depts);
    return _EnterpriseDirectoryData(
      groups: groups,
      totalUsers: entries.length,
      chatEnabledUsers: entries.where((entry) => entry.canChat).length,
      departmentCount: _countGroups(groups),
      deptTreeAvailable: deptTreeAvailable && depts.isNotEmpty,
    );
  }

  List<_EnterpriseDeptGroup> filteredGroups(String keyword) {
    final query = keyword.trim().toLowerCase();
    if (query.isEmpty) return groups;
    return groups
        .map((group) => group.filtered(query))
        .whereType<_EnterpriseDeptGroup>()
        .toList();
  }
}

class _EnterpriseDeptGroup {
  final int? id;
  final String name;
  final List<_EnterpriseContactEntry> users;
  final List<_EnterpriseDeptGroup> children;

  const _EnterpriseDeptGroup({
    required this.id,
    required this.name,
    required this.users,
    required this.children,
  });

  int get totalCount {
    return users.length +
        children.fold<int>(0, (total, child) => total + child.totalCount);
  }

  _EnterpriseDeptGroup? filtered(String query) {
    if (name.toLowerCase().contains(query)) return this;
    final nextUsers = users.where((entry) => entry.matches(query)).toList();
    final nextChildren = children
        .map((child) => child.filtered(query))
        .whereType<_EnterpriseDeptGroup>()
        .toList();
    if (nextUsers.isEmpty && nextChildren.isEmpty) return null;
    return _EnterpriseDeptGroup(
      id: id,
      name: name,
      users: nextUsers,
      children: nextChildren,
    );
  }
}

class _EnterpriseContactEntry {
  final BpmUser? user;
  final TencentImContact? contact;

  const _EnterpriseContactEntry({required this.user, required this.contact});

  bool get canChat => contact?.imUserId.trim().isNotEmpty == true;

  int? get deptId => user?.deptId;

  String get deptName {
    final name = user?.deptName?.trim();
    return name == null || name.isEmpty ? '未分配部门' : name;
  }

  String get displayName {
    final userName = user?.displayName.trim();
    if (userName != null && userName.isNotEmpty) return userName;
    return contact?.displayName ?? '未知用户';
  }

  String get subtitle {
    final values = [deptName, user?.username, user?.tenantName]
        .whereType<String>()
        .map((value) => value.trim())
        .where((value) => value.isNotEmpty && value != displayName)
        .toList();
    return values.isEmpty ? (canChat ? '可发起会话' : '未开通 IM') : values.join(' / ');
  }

  bool matches(String query) {
    final haystack = [
      displayName,
      deptName,
      user?.username,
      user?.mobile,
      user?.email,
      user?.tenantName,
      contact?.oaUsername,
      contact?.ordersysUsername,
      contact?.remark,
    ].whereType<String>().join(' ').toLowerCase();
    return haystack.contains(query);
  }
}

List<_EnterpriseDeptGroup> _buildGroupsByDeptName(
  List<_EnterpriseContactEntry> entries,
) {
  final grouped = <String, List<_EnterpriseContactEntry>>{};
  for (final entry in entries) {
    grouped.putIfAbsent(entry.deptName, () => []).add(entry);
  }
  final names = grouped.keys.toList()..sort();
  return names
      .map(
        (name) => _EnterpriseDeptGroup(
          id: null,
          name: name,
          users: _sortEntries(grouped[name] ?? const []),
          children: const [],
        ),
      )
      .toList();
}

List<_EnterpriseDeptGroup> _buildGroupsByDeptTree(
  List<_EnterpriseContactEntry> entries,
  List<BpmDeptNode> depts,
) {
  final usersByDept = <int, List<_EnterpriseContactEntry>>{};
  final unassigned = <_EnterpriseContactEntry>[];
  for (final entry in entries) {
    final deptId = entry.deptId;
    if (deptId == null) {
      unassigned.add(entry);
      continue;
    }
    usersByDept.putIfAbsent(deptId, () => []).add(entry);
  }

  final childrenByParent = <int?, List<BpmDeptNode>>{};
  for (final dept in depts) {
    childrenByParent.putIfAbsent(dept.parentId, () => []).add(dept);
  }
  for (final children in childrenByParent.values) {
    children.sort((a, b) => a.name.compareTo(b.name));
  }

  final deptIds = depts
      .where((dept) => dept.id != null)
      .map((dept) => dept.id)
      .toSet();
  final roots =
      depts
          .where(
            (dept) =>
                dept.parentId == null ||
                dept.parentId == 0 ||
                !deptIds.contains(dept.parentId),
          )
          .toList()
        ..sort((a, b) => a.name.compareTo(b.name));
  final result = roots
      .map((dept) => _buildDeptGroup(dept, childrenByParent, usersByDept))
      .where((group) => group.totalCount > 0)
      .toList();

  for (final entry in entries) {
    final deptId = entry.deptId;
    if (deptId != null && !deptIds.contains(deptId)) {
      unassigned.add(entry);
    }
  }

  if (unassigned.isNotEmpty) {
    result.add(
      _EnterpriseDeptGroup(
        id: null,
        name: '未分配部门',
        users: _sortEntries(unassigned),
        children: const [],
      ),
    );
  }
  return result;
}

_EnterpriseDeptGroup _buildDeptGroup(
  BpmDeptNode dept,
  Map<int?, List<BpmDeptNode>> childrenByParent,
  Map<int, List<_EnterpriseContactEntry>> usersByDept,
) {
  final id = dept.id;
  final children =
      (id == null ? const <BpmDeptNode>[] : childrenByParent[id] ?? const [])
          .map((child) => _buildDeptGroup(child, childrenByParent, usersByDept))
          .where((group) => group.totalCount > 0)
          .toList();
  return _EnterpriseDeptGroup(
    id: id,
    name: dept.name,
    users: _sortEntries(id == null ? const [] : usersByDept[id] ?? const []),
    children: children,
  );
}

List<_EnterpriseContactEntry> _sortEntries(
  List<_EnterpriseContactEntry> entries,
) {
  final result = entries.toList();
  result.sort((a, b) => a.displayName.compareTo(b.displayName));
  return result;
}

int _countGroups(List<_EnterpriseDeptGroup> groups) {
  return groups.fold<int>(
    0,
    (total, group) => total + 1 + _countGroups(group.children),
  );
}

String _contactKey(TencentImContact contact) {
  final id = contact.id?.toString();
  if (id != null && id.isNotEmpty) return 'id:$id';
  return 'im:${contact.imUserId}';
}

String _cleanError(Object? error) {
  return (error?.toString() ?? '请求失败')
      .replaceFirst('Exception: ', '')
      .replaceFirst('ApiException: ', '');
}
