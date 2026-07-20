import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/api_service.dart';
import '../services/badge_read_service.dart';
import '../services/bpm_service.dart';
import '../services/hotel_operations_service.dart';
import '../services/hr_service.dart';
import '../services/permissions_service.dart';
import '../services/requirement_service.dart';
import '../widgets/kyx_design.dart';
import 'bpm_approval_page.dart';
import 'data_center_page.dart';
import 'executive_cockpit_page.dart';
import 'hotel_front_desk_page.dart';
import 'hotel_manager_dashboard_page.dart';
import 'hr_self_service_page.dart';
import 'ops_monitor_page.dart';
import 'requirement_page.dart';

class WorkbenchPage extends StatefulWidget {
  const WorkbenchPage({super.key});

  @override
  State<WorkbenchPage> createState() => _WorkbenchPageState();
}

class _WorkbenchPageState extends State<WorkbenchPage> {
  int _approvalBadgeCount = 0;
  int _requirementBadgeCount = 0;
  int _hrTotalBadgeCount = 0;
  int _hrTodoBadgeCount = 0;
  int _learningBadgeCount = 0;
  int _questionnaireBadgeCount = 0;
  int _examBadgeCount = 0;
  int _applicationBadgeCount = 0;
  int _payslipBadgeCount = 0;
  int _hotelBadgeCount = 0;
  HotelPermission _hotelPermission = HotelPermission.empty;
  bool _initialLoaded = false;
  bool _loadingBadges = false;
  // 权限点从 PermissionsService（登录后 /me/permissions 拉取 + SharedPreferences 持久化）。
  // 3 个入口 sync 读，避免时序 bug 越权。分两组权限：
  //   Dashboard (总裁驾驶舱) —— 老板 + 租户管理员
  //   Ops (运维监控)       —— 租户管理员 + 技术部（老板不给）
  //   数据中心             —— 运维组 + 单独权限点
  bool _canAccessDashboard = false;
  bool _canAccessOps = false;
  bool _canUseDataCenter = false;
  bool _permsLoaded = false;

  @override
  void initState() {
    super.initState();
    unawaited(_loadCachedBadges());
    _loadBadges(silent: true);
    unawaited(_loadPermissions());
    PermissionsService.addListener(_onPermsChange);
  }

  @override
  void dispose() {
    PermissionsService.removeListener(_onPermsChange);
    super.dispose();
  }

  Future<void> _loadPermissions() async {
    await PermissionsService.loadCacheAndScheduleRefresh();
    if (!mounted) return;
    _applyPermsToState();
  }

  void _onPermsChange(PermissionsChange change) {
    if (!mounted) return;
    _applyPermsToState();
    // 提示由 chat_main_page 统一弹 —— 避免同一次撤权弹两个 snackbar
  }

  void _applyPermsToState() {
    setState(() {
      _canAccessDashboard = PermissionsService.canAccessDashboard;
      _canAccessOps = PermissionsService.canAccessOps;
      _canUseDataCenter = PermissionsService.canUseDataCenter;
      _permsLoaded = true;
    });
  }

  Future<String> _cacheKey() async {
    final cert = await ApiService.getFreshLoginCertificate();
    final tenantId = (cert?['tenantId'] ?? '').toString();
    final userId = (cert?['userId'] ?? cert?['userID'] ?? '').toString();
    return 'workbench_badges_v2:$tenantId:$userId';
  }

  Future<void> _loadCachedBadges() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(await _cacheKey());
      if (raw == null || raw.trim().isEmpty) return;
      final map = json.decode(raw);
      if (map is! Map) return;
      if (!mounted) return;
      setState(() {
        _approvalBadgeCount = _intValue(map['approvalBadgeCount']);
        _requirementBadgeCount = _intValue(map['requirementBadgeCount']);
        _hrTotalBadgeCount = _intValue(map['hrTotalBadgeCount']);
        _hrTodoBadgeCount = _intValue(map['hrTodoBadgeCount']);
        _learningBadgeCount = _intValue(map['learningBadgeCount']);
        _questionnaireBadgeCount = _intValue(map['questionnaireBadgeCount']);
        _examBadgeCount = _intValue(map['examBadgeCount']);
        _applicationBadgeCount = _intValue(map['applicationBadgeCount']);
        _payslipBadgeCount = _intValue(map['payslipBadgeCount']);
        _hotelBadgeCount = _intValue(map['hotelBadgeCount']);
        _hotelPermission = HotelPermission.fromJson(
          Map<String, dynamic>.from(map['hotelPermission'] ?? const {}),
        );
        _initialLoaded = true;
      });
    } catch (_) {
      // ignore local cache errors
    }
  }

  Future<void> _saveCachedBadges() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(
        await _cacheKey(),
        json.encode({
          'approvalBadgeCount': _approvalBadgeCount,
          'requirementBadgeCount': _requirementBadgeCount,
          'hrTotalBadgeCount': _hrTotalBadgeCount,
          'hrTodoBadgeCount': _hrTodoBadgeCount,
          'learningBadgeCount': _learningBadgeCount,
          'questionnaireBadgeCount': _questionnaireBadgeCount,
          'examBadgeCount': _examBadgeCount,
          'applicationBadgeCount': _applicationBadgeCount,
          'payslipBadgeCount': _payslipBadgeCount,
          'hotelBadgeCount': _hotelBadgeCount,
          'hotelPermission': _hotelPermission.toJson(),
        }),
      );
    } catch (_) {
      // ignore cache write errors
    }
  }

  Future<void> _loadBadges({bool silent = false}) async {
    if (_loadingBadges) return;
    _loadingBadges = true;
    try {
      final results = await Future.wait<dynamic>([
        _loadApprovalBadge(),
        _loadRequirementBadge(),
        _loadHrBadges(),
        _loadPayslipBadge(),
        _loadHotelBadge(),
        _loadHotelPermission(),
      ]);
      if (!mounted) return;
      final hr = results[2] as _HrBadgeSnapshot;
      setState(() {
        _approvalBadgeCount = results[0] as int;
        _requirementBadgeCount = results[1] as int;
        _hrTotalBadgeCount = hr.total;
        _hrTodoBadgeCount = hr.todo;
        _learningBadgeCount = hr.learning;
        _questionnaireBadgeCount = hr.questionnaire;
        _examBadgeCount = hr.exam;
        _applicationBadgeCount = hr.application;
        _payslipBadgeCount = results[3] as int;
        _hotelBadgeCount = results[4] as int;
        _hotelPermission = results[5] as HotelPermission;
        _initialLoaded = true;
      });
      await _saveCachedBadges();
    } finally {
      _loadingBadges = false;
      if (mounted && !_initialLoaded && !silent) {
        setState(() => _initialLoaded = true);
      }
    }
  }

  Future<int> _loadApprovalBadge() async {
    try {
      final page = await BpmService.getTodoTasks(pageNo: 1, pageSize: 1);
      return page.total;
    } catch (_) {
      return _approvalBadgeCount;
    }
  }

  Future<int> _loadRequirementBadge() async {
    try {
      final currentUserId = await RequirementService.currentUserId();
      if (currentUserId == null) return _requirementBadgeCount;
      final page = await RequirementService.getPage(
        pageNo: 1,
        pageSize: 200,
        assigneeUserId: currentUserId,
      );
      return BadgeReadService.unseenCount(
        'work_requirement_assigned',
        page.list.map((item) => item.id),
      );
    } catch (_) {
      return _requirementBadgeCount;
    }
  }

  Future<_HrBadgeSnapshot> _loadHrBadges() async {
    try {
      final home = await HrService.getSelfServiceHome();
      final summary = home.todoSummary;
      if (summary == null) return const _HrBadgeSnapshot();
      final questionnaire = await _loadQuestionnaireBadge(
        fallback: summary.pendingQuestionnaireCount,
      );
      final exam = await _loadExamBadge(
        fallback: summary.availableExamCount + summary.inProgressExamCount,
      );
      final todo = summary.openTodoCount + summary.pendingLifecycleTaskCount;
      final application = summary.runningLeaveCount + summary.runningTripCount;
      return _HrBadgeSnapshot(
        total: todo + questionnaire + exam + application,
        todo: todo,
        learning: questionnaire + exam,
        questionnaire: questionnaire,
        exam: exam,
        application: application,
      );
    } catch (_) {
      return _HrBadgeSnapshot(
        total: _hrTotalBadgeCount,
        todo: _hrTodoBadgeCount,
        learning: _learningBadgeCount,
        questionnaire: _questionnaireBadgeCount,
        exam: _examBadgeCount,
        application: _applicationBadgeCount,
      );
    }
  }

  Future<int> _loadQuestionnaireBadge({required int fallback}) async {
    try {
      final page = await HrService.getMyQuestionnaires(
        pageNo: 1,
        pageSize: 200,
        status: 0,
      );
      return BadgeReadService.unseenCount(
        'hr_questionnaire',
        page.list.map((item) => item.id),
      );
    } catch (_) {
      return fallback;
    }
  }

  Future<int> _loadExamBadge({required int fallback}) async {
    try {
      final page = await HrService.getMyExams(
        pageNo: 1,
        pageSize: 200,
        status: 1,
      );
      final now = DateTime.now();
      final ids = page.list
          .where((item) {
            final start = _parseDateTime(item.startAt);
            final end = _parseDateTime(item.endAt);
            if (start != null && now.isBefore(start)) return false;
            if (end != null && now.isAfter(end)) return false;
            return true;
          })
          .map((item) => item.id);
      return BadgeReadService.unseenCount('hr_exam', ids);
    } catch (_) {
      return fallback;
    }
  }

  DateTime? _parseDateTime(String? value) {
    final text = value?.trim();
    if (text == null || text.isEmpty) return null;
    return DateTime.tryParse(text.replaceFirst(' ', 'T'));
  }

  Future<int> _loadPayslipBadge() async {
    try {
      final page = await HrService.getMyPayslips(pageNo: 1, pageSize: 50);
      return page.list.where((item) => item.status == 'PUBLISHED').length;
    } catch (_) {
      return _payslipBadgeCount;
    }
  }

  Future<HotelPermission> _loadHotelPermission() async {
    try {
      return await HotelOperationsService.getPermission();
    } catch (_) {
      return _hotelPermission;
    }
  }

  Future<int> _loadHotelBadge() async {
    try {
      return await HotelOperationsService.getBadge();
    } catch (_) {
      return _hotelBadgeCount;
    }
  }

  Future<void> _openPage(Widget page) async {
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => page));
    if (mounted) await _loadBadges();
  }

  @override
  Widget build(BuildContext context) {
    final quickActions = [
      _WorkbenchAction(
        '审批中心',
        Icons.approval_outlined,
        _green,
        const BpmApprovalPage(),
        badgeCount: _approvalBadgeCount,
      ),
      _WorkbenchAction(
        '需求管理',
        Icons.assignment_outlined,
        _blue,
        const RequirementPage(),
        badgeCount: _requirementBadgeCount,
      ),
      _WorkbenchAction(
        '总裁驾驶舱',
        Icons.query_stats,
        _indigo,
        const ExecutiveCockpitPage(),
      ),
      _WorkbenchAction(
        '数据中心',
        Icons.folder_zip_outlined,
        _blue,
        const DataCenterPage(),
      ),
      _WorkbenchAction(
        '员工自助',
        Icons.badge_outlined,
        _blue,
        const HrSelfServicePage(),
        badgeCount: _hrTotalBadgeCount,
      ),
      _WorkbenchAction(
        '酒店前台',
        Icons.local_hotel_outlined,
        _cyan,
        const HotelFrontDeskPage(),
        badgeCount: _hotelBadgeCount,
      ),
      _WorkbenchAction(
        '酒店驾驶舱',
        Icons.dashboard_customize_outlined,
        _indigo,
        const HotelManagerDashboardPage(),
        badgeCount: _hotelBadgeCount,
      ),
      _WorkbenchAction(
        '请假申请',
        Icons.event_available,
        _green,
        const HrLeaveApplyPage(),
        badgeCount: _applicationBadgeCount,
      ),
      _WorkbenchAction(
        '我的申请',
        Icons.fact_check_outlined,
        _orange,
        const HrApplicationsPage(),
        badgeCount: _applicationBadgeCount,
      ),
      _WorkbenchAction(
        '工资条',
        Icons.payments_outlined,
        _purple,
        const HrPayslipPage(),
        badgeCount: _payslipBadgeCount,
      ),
      _WorkbenchAction(
        '问卷互评',
        Icons.ballot_outlined,
        _cyan,
        const HrLearningPage(initialTab: 0),
        badgeCount: _questionnaireBadgeCount,
      ),
      _WorkbenchAction(
        '考试学习',
        Icons.school_outlined,
        _indigo,
        const HrLearningPage(initialTab: 1),
        badgeCount: _examBadgeCount,
      ),
      _WorkbenchAction(
        '人事待办',
        Icons.task_alt_outlined,
        _red,
        const HrTodoPage(),
        badgeCount: _hrTodoBadgeCount,
      ),
      _WorkbenchAction(
        '考勤记录',
        Icons.access_time,
        _slate,
        const HrAttendancePage(),
        badgeCount: _applicationBadgeCount,
      ),
      // 运维监控 — 仅管理员可见（下面 visibleQuickActions.where 里过滤）
      _WorkbenchAction(
        '运维监控',
        Icons.monitor_heart_outlined,
        _slate,
        const OpsMonitorPage(),
      ),
    ];

    // 权限过滤：3 个入口分 2 组（老板看得到驾驶舱但看不到运维/数据中心）
    // 未加载完成时严格判定为无权限，避免时序上闪现
    final visibleQuickActions = quickActions.where((action) {
      if (action.name == '酒店前台') return _hotelPermission.canUseFrontDesk;
      if (action.name == '酒店驾驶舱') return _hotelPermission.canViewDashboard;
      if (action.name == '总裁驾驶舱') return _permsLoaded && _canAccessDashboard;
      if (action.name == '运维监控') return _permsLoaded && _canAccessOps;
      if (action.name == '数据中心') return _permsLoaded && _canUseDataCenter;
      return true;
    }).toList();

    final apps = [
      _WorkbenchAction(
        '审批中心',
        Icons.approval_outlined,
        _green,
        const BpmApprovalPage(),
        badgeCount: _approvalBadgeCount,
      ),
      _WorkbenchAction(
        '需求管理',
        Icons.assignment_outlined,
        _blue,
        const RequirementPage(),
        badgeCount: _requirementBadgeCount,
      ),
      _WorkbenchAction(
        '总裁驾驶舱',
        Icons.query_stats,
        _indigo,
        const ExecutiveCockpitPage(),
      ),
      _WorkbenchAction(
        '数据中心',
        Icons.folder_zip_outlined,
        _blue,
        const DataCenterPage(),
      ),
      _WorkbenchAction(
        'HR 员工服务',
        Icons.groups_outlined,
        _blue,
        const HrSelfServicePage(),
        badgeCount: _hrTotalBadgeCount,
      ),
      _WorkbenchAction(
        '学习与互评',
        Icons.school_outlined,
        _indigo,
        const HrLearningPage(),
        badgeCount: _learningBadgeCount,
      ),
      _WorkbenchAction(
        '人事待办',
        Icons.task_alt_outlined,
        _red,
        const HrTodoPage(),
        badgeCount: _hrTodoBadgeCount,
      ),
      _WorkbenchAction(
        '考勤与请假',
        Icons.calendar_month_outlined,
        _green,
        const HrAttendancePage(),
        badgeCount: _applicationBadgeCount,
      ),
      _WorkbenchAction(
        '酒店前台',
        Icons.local_hotel_outlined,
        _cyan,
        const HotelFrontDeskPage(),
        badgeCount: _hotelBadgeCount,
      ),
      _WorkbenchAction(
        '酒店驾驶舱',
        Icons.dashboard_customize_outlined,
        _indigo,
        const HotelManagerDashboardPage(),
        badgeCount: _hotelBadgeCount,
      ),
    ];

    // apps 列表跟 quickActions 走同一套权限门
    final visibleApps = apps.where((action) {
      if (action.name == '酒店前台') return _hotelPermission.canUseFrontDesk;
      if (action.name == '酒店驾驶舱') return _hotelPermission.canViewDashboard;
      if (action.name == '总裁驾驶舱') return _permsLoaded && _canAccessDashboard;
      if (action.name == '数据中心') return _permsLoaded && _canUseDataCenter;
      return true;
    }).toList();

    if (!_initialLoaded) {
      return const _WorkbenchLoadingScaffold();
    }

    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '工作台',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: RefreshIndicator(
        onRefresh: () => _loadBadges(silent: false),
        child: ListView(
          padding: const EdgeInsets.only(bottom: 28),
          children: [
            const _WorkbenchHeader(),
            const KyXSectionLabel('工作高频'),
            _ActionGrid(actions: visibleQuickActions, onOpen: _openPage),
            const KyXSectionLabel('工作应用'),
            KyXListSection(
              children: visibleApps
                  .asMap()
                  .entries
                  .map(
                    (entry) => _AppRow(
                      action: entry.value,
                      showDivider: entry.key != visibleApps.length - 1,
                      onOpen: _openPage,
                    ),
                  )
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }
}

int _intValue(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '') ?? 0;
}

class _WorkbenchLoadingScaffold extends StatelessWidget {
  const _WorkbenchLoadingScaffold();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '工作台',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: const Center(child: CircularProgressIndicator()),
    );
  }
}

class _HrBadgeSnapshot {
  final int total;
  final int todo;
  final int learning;
  final int questionnaire;
  final int exam;
  final int application;

  const _HrBadgeSnapshot({
    this.total = 0,
    this.todo = 0,
    this.learning = 0,
    this.questionnaire = 0,
    this.exam = 0,
    this.application = 0,
  });
}

class _WorkbenchHeader extends StatelessWidget {
  const _WorkbenchHeader();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 15),
      child: const Row(
        children: [
          Icon(
            Icons.business_center_outlined,
            color: KyXColors.primary,
            size: 24,
          ),
          SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '移动 OA',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: KyXColors.text,
                  ),
                ),
                SizedBox(height: 4),
                Text(
                  '需求、审批、人事服务集中处理',
                  style: TextStyle(
                    fontSize: 13,
                    color: KyXColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionGrid extends StatelessWidget {
  final List<_WorkbenchAction> actions;
  final ValueChanged<Widget> onOpen;

  const _ActionGrid({required this.actions, required this.onOpen});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      padding: const EdgeInsets.fromLTRB(8, 8, 8, 10),
      child: GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 4,
          mainAxisSpacing: 4,
          childAspectRatio: 0.92,
        ),
        itemCount: actions.length,
        itemBuilder: (context, index) {
          final action = actions[index];
          return InkWell(
            onTap: () => onOpen(action.page),
            borderRadius: BorderRadius.circular(8),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  _ActionIcon(action: action, size: 34, iconSize: 20),
                  const SizedBox(height: 7),
                  Text(
                    action.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: KyXColors.text,
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _AppRow extends StatelessWidget {
  final _WorkbenchAction action;
  final bool showDivider;
  final ValueChanged<Widget> onOpen;

  const _AppRow({
    required this.action,
    required this.showDivider,
    required this.onOpen,
  });

  @override
  Widget build(BuildContext context) {
    return KyXListRow(
      leading: _ActionIcon(action: action, size: 38, iconSize: 20),
      title: action.name,
      trailing: const Icon(Icons.chevron_right, color: KyXColors.textTertiary),
      onTap: () => onOpen(action.page),
      showDivider: showDivider,
    );
  }
}

class _WorkbenchAction {
  final String name;
  final IconData icon;
  final Color color;
  final Widget page;
  final int badgeCount;

  const _WorkbenchAction(
    this.name,
    this.icon,
    this.color,
    this.page, {
    this.badgeCount = 0,
  });
}

class _ActionIcon extends StatelessWidget {
  final _WorkbenchAction action;
  final double size;
  final double iconSize;

  const _ActionIcon({
    required this.action,
    required this.size,
    required this.iconSize,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size + 10,
      height: size + 8,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned(
            left: 0,
            bottom: 0,
            child: Container(
              width: size,
              height: size,
              decoration: BoxDecoration(
                color: action.color.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(action.icon, color: action.color, size: iconSize),
            ),
          ),
          if (action.badgeCount > 0)
            Positioned(
              right: 0,
              top: 0,
              child: Container(
                constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                padding: const EdgeInsets.symmetric(horizontal: 4),
                decoration: BoxDecoration(
                  color: KyXColors.red,
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: KyXColors.surface, width: 1),
                ),
                alignment: Alignment.center,
                child: Text(
                  action.badgeCount > 99 ? '99+' : '${action.badgeCount}',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 9,
                    fontWeight: FontWeight.w800,
                    height: 1,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

const Color _blue = Color(0xFF2563EB);
const Color _green = Color(0xFF059669);
const Color _orange = Color(0xFFD97706);
const Color _purple = Color(0xFF7C3AED);
const Color _cyan = Color(0xFF0891B2);
const Color _indigo = Color(0xFF4F46E5);
const Color _red = Color(0xFFDC2626);
const Color _slate = Color(0xFF475569);
