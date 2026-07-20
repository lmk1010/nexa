import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/hr_service.dart';

const MethodChannel _nativeIntents = MethodChannel('kyx/native_intents');

class HrSelfServicePage extends StatefulWidget {
  const HrSelfServicePage({super.key});

  @override
  State<HrSelfServicePage> createState() => _HrSelfServicePageState();
}

class _HrSelfServicePageState extends State<HrSelfServicePage> {
  late Future<_HrDashboardData> _future;

  @override
  void initState() {
    super.initState();
    _future = _loadData();
  }

  Future<_HrDashboardData> _loadData() async {
    final home = await HrService.getSelfServiceHome();
    final results = await Future.wait<dynamic>([
      _safePage<HrApplicationItem>(
        () => HrService.getApplications(pageSize: 5),
      ),
      _safeList<HrLeaveBalance>(
        () => HrService.getMyLeaveBalances(year: DateTime.now().year),
      ),
      _safePage<HrPayslip>(() => HrService.getMyPayslips(pageSize: 3)),
      _safePage<HrQuestionnaireAssignment>(
        () => HrService.getMyQuestionnaires(pageSize: 3),
      ),
      _safePage<HrExamPublish>(() => HrService.getMyExams(pageSize: 3)),
      _safePage<HrTodoTask>(() => HrService.getMyTodos(pageSize: 5)),
    ]);
    final applications = results[0] as HrPage<HrApplicationItem>;
    final balances = results[1] as List<HrLeaveBalance>;
    final payslips = results[2] as HrPage<HrPayslip>;
    final questionnaires = results[3] as HrPage<HrQuestionnaireAssignment>;
    final exams = results[4] as HrPage<HrExamPublish>;
    final todos = results[5] as HrPage<HrTodoTask>;

    return _HrDashboardData(
      home: home,
      applications: applications.list,
      balances: balances,
      payslips: payslips.list,
      questionnaires: questionnaires.list,
      exams: exams.list,
      todos: todos.list,
    );
  }

  Future<void> _refresh() async {
    setState(() {
      _future = _loadData();
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        title: const Text('员工自助'),
        titleTextStyle: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w800,
          color: _text,
        ),
        centerTitle: false,
        backgroundColor: _bg,
        foregroundColor: _text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: FutureBuilder<_HrDashboardData>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _ErrorState(
              message: _cleanError(snapshot.error),
              onRetry: _refresh,
            );
          }

          final data = snapshot.data!;
          return RefreshIndicator(
            onRefresh: _refresh,
            child: ListView(
              padding: const EdgeInsets.only(bottom: 28),
              children: [
                _ProfileHeader(home: data.home),
                const SizedBox(height: 10),
                _TodayAttendancePanel(
                  attendance: data.home.todayAttendance,
                  onOpenDingTalk: _openDingTalkAttendance,
                  onRefresh: () => _refresh(),
                ),
                const SizedBox(height: 14),
                _SectionTitle(
                  title: '高频入口',
                  trailing: '${_dashboardOpenCount(data)} 项待处理',
                ),
                _ActionGrid(actions: _buildActions(context)),
                const SizedBox(height: 16),
                _SectionTitle(
                  title: '假期余额',
                  trailing: '${DateTime.now().year}',
                ),
                _LeaveBalanceStrip(balances: data.balances),
                const SizedBox(height: 16),
                _PreviewSection<HrApplicationItem>(
                  title: '最近申请',
                  emptyText: '暂无申请记录',
                  items: data.applications,
                  onMore: () => _push(const HrApplicationsPage()),
                  itemBuilder: (item) => _CompactRow(
                    icon: Icons.receipt_long,
                    title: item.title ?? _businessTypeName(item.businessType),
                    subtitle: _joinText([
                      item.summary,
                      _dateRange(item.startTime, item.endTime),
                    ]),
                    tag: item.statusText ?? '-',
                  ),
                ),
                const SizedBox(height: 16),
                _PreviewSection<HrPayslip>(
                  title: '工资条',
                  emptyText: '暂无工资条',
                  items: data.payslips,
                  onMore: () => _push(const HrPayslipPage()),
                  itemBuilder: (item) => _CompactRow(
                    icon: Icons.payments_outlined,
                    title: item.payrollMonth ?? '未命名月份',
                    subtitle: '实发 ${_money(item.netSalary)}',
                    tag: _payslipStatus(item.status),
                  ),
                ),
                const SizedBox(height: 16),
                _PreviewSection<HrTodoTask>(
                  title: '人事待办',
                  emptyText: '暂无待办',
                  items: data.todos,
                  onMore: () => _push(const HrTodoPage()),
                  itemBuilder: (item) => _CompactRow(
                    icon: Icons.task_alt_outlined,
                    title: item.title,
                    subtitle: _joinText([
                      item.content,
                      item.dueTime == null
                          ? null
                          : '截止 ${_shortDate(item.dueTime)}',
                    ]),
                    tag: _priorityName(item.priority),
                  ),
                ),
                const SizedBox(height: 16),
                _LearningPreview(
                  questionnaires: data.questionnaires,
                  exams: data.exams,
                  onMore: () => _push(const HrLearningPage()),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  List<_MobileAction> _buildActions(BuildContext context) {
    return [
      _MobileAction(
        '考勤记录',
        Icons.access_time,
        _blue,
        () => _push(const HrAttendancePage()),
      ),
      _MobileAction(
        '请假申请',
        Icons.event_available,
        _green,
        () => _push(const HrLeaveApplyPage()),
      ),
      _MobileAction(
        '我的申请',
        Icons.fact_check_outlined,
        _orange,
        () => _push(const HrApplicationsPage()),
      ),
      _MobileAction(
        '工资条',
        Icons.payments_outlined,
        _purple,
        () => _push(const HrPayslipPage()),
      ),
      _MobileAction(
        '问卷互评',
        Icons.ballot_outlined,
        _cyan,
        () => _push(const HrLearningPage(initialTab: 0)),
      ),
      _MobileAction(
        '考试学习',
        Icons.school_outlined,
        _indigo,
        () => _push(const HrLearningPage(initialTab: 1)),
      ),
      _MobileAction(
        '人事待办',
        Icons.task_alt_outlined,
        _red,
        () => _push(const HrTodoPage()),
      ),
      _MobileAction('员工档案', Icons.badge_outlined, _slate, _showProfileTip),
    ];
  }

  Future<void> _openDingTalkAttendance() async {
    try {
      final opened =
          await _nativeIntents.invokeMethod<bool>('openDingTalkAttendance') ??
          false;
      if (!mounted) return;
      if (!opened) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('未检测到钉钉，请打开钉钉完成考勤打卡')));
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('无法拉起钉钉，请手动打开钉钉打卡')));
    }
  }

  void _showProfileTip() {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('员工档案编辑涉及人事审批，移动端先展示资料完整度')));
  }

  Future<void> _push(Widget page) async {
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => page));
    if (mounted) {
      setState(() {
        _future = _loadData();
      });
    }
  }

  int _dashboardOpenCount(_HrDashboardData data) {
    final summary = data.home.todoSummary;
    if (summary != null) return summary.totalOpen;
    return data.todos.length + data.questionnaires.length + data.exams.length;
  }
}

class HrApplicationsPage extends StatefulWidget {
  const HrApplicationsPage({super.key});

  @override
  State<HrApplicationsPage> createState() => _HrApplicationsPageState();
}

class _HrApplicationsPageState extends State<HrApplicationsPage> {
  late Future<HrPage<HrApplicationItem>> _future;

  @override
  void initState() {
    super.initState();
    _future = HrService.getApplications(pageSize: 30);
  }

  Future<void> _refresh() async {
    setState(() {
      _future = HrService.getApplications(pageSize: 30);
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return _ListScaffold<HrApplicationItem>(
      title: '我的申请',
      future: _future,
      onRefresh: _refresh,
      emptyText: '暂无申请记录',
      itemBuilder: (item) => _ListTileShell(
        child: _CompactRow(
          icon: Icons.fact_check_outlined,
          title: item.title ?? _businessTypeName(item.businessType),
          subtitle: _joinText([
            item.summary,
            item.applyTime == null ? null : '申请 ${_shortDate(item.applyTime)}',
          ]),
          tag: item.statusText ?? '-',
        ),
      ),
    );
  }
}

class HrLeaveApplyPage extends StatefulWidget {
  const HrLeaveApplyPage({super.key});

  @override
  State<HrLeaveApplyPage> createState() => _HrLeaveApplyPageState();
}

class _HrLeaveApplyPageState extends State<HrLeaveApplyPage> {
  final _formKey = GlobalKey<FormState>();
  final _emergencyPhoneController = TextEditingController();
  final _handoverController = TextEditingController();
  final _remarkController = TextEditingController();

  List<HrLeaveBalance> _balances = const [];
  String _leaveCategory = 'leave';
  String _leaveType = 'annual';
  DateTime _startTime = _roundToHalfHour(
    DateTime.now().add(const Duration(hours: 1)),
  );
  DateTime _endTime = _roundToHalfHour(
    DateTime.now().add(const Duration(hours: 9)),
  );
  bool _loading = true;
  bool _submitting = false;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _loadBalances();
  }

  @override
  void dispose() {
    _emergencyPhoneController.dispose();
    _handoverController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  Future<void> _loadBalances() async {
    try {
      final balances = await HrService.getMyLeaveBalances(
        year: DateTime.now().year,
      );
      if (!mounted) return;
      setState(() {
        _balances = balances;
        final options = _buildLeaveOptions(balances);
        if (options.isNotEmpty) {
          _leaveType = options.first.code;
        }
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loadError = _cleanError(error);
        _loading = false;
      });
    }
  }

  Future<void> _submit() async {
    final form = _formKey.currentState;
    if (form == null || !form.validate()) return;

    final duration = _calculateDuration(_startTime, _endTime, _leaveType);
    if (duration <= 0) {
      _showMessage('结束时间必须晚于开始时间');
      return;
    }
    final balance = _balances
        .where((item) => item.leaveTypeCode == _leaveType)
        .firstOrNull;
    if (balance != null &&
        balance.remainAmount > 0 &&
        balance.remainAmount < duration) {
      _showMessage(
        '${balance.leaveTypeName}余额不足，剩余 ${_formatAmount(balance.remainAmount)}',
      );
      return;
    }

    setState(() => _submitting = true);
    try {
      await HrService.createLeave(
        HrLeaveCreateRequest(
          leaveCategory: _leaveCategory,
          leaveType: _leaveType,
          startTime: _startTime,
          endTime: _endTime,
          duration: duration,
          emergencyPhone: _emergencyPhoneController.text.trim(),
          workHandover: _handoverController.text.trim(),
          remark: _remarkController.text.trim(),
        ),
      );
      if (!mounted) return;
      _showMessage('请假申请已提交');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      _showMessage(_cleanError(error));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final duration = _calculateDuration(_startTime, _endTime, _leaveType);
    final leaveOptions = _buildLeaveOptions(_balances);

    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        title: const Text('请假申请'),
        backgroundColor: Colors.white,
        foregroundColor: _text,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 96),
                children: [
                  if (_loadError != null) _InlineWarning(text: _loadError!),
                  _LeaveBalanceStrip(balances: _balances),
                  const SizedBox(height: 14),
                  _FormSurface(
                    children: [
                      DropdownButtonFormField<String>(
                        value: _leaveCategory,
                        decoration: const InputDecoration(labelText: '申请类型'),
                        items: const [
                          DropdownMenuItem(value: 'leave', child: Text('请假')),
                          DropdownMenuItem(
                            value: 'compensatory',
                            child: Text('调休'),
                          ),
                        ],
                        onChanged: (value) =>
                            setState(() => _leaveCategory = value ?? 'leave'),
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String>(
                        value:
                            leaveOptions.any((item) => item.code == _leaveType)
                            ? _leaveType
                            : leaveOptions.first.code,
                        decoration: const InputDecoration(labelText: '假期类型'),
                        items: leaveOptions
                            .map(
                              (item) => DropdownMenuItem(
                                value: item.code,
                                child: Text(item.name),
                              ),
                            )
                            .toList(),
                        onChanged: (value) =>
                            setState(() => _leaveType = value ?? _leaveType),
                      ),
                      const SizedBox(height: 12),
                      _DateTimeField(
                        label: '开始时间',
                        value: _startTime,
                        onChanged: (value) =>
                            setState(() => _startTime = value),
                      ),
                      const SizedBox(height: 12),
                      _DateTimeField(
                        label: '结束时间',
                        value: _endTime,
                        onChanged: (value) => setState(() => _endTime = value),
                      ),
                      const SizedBox(height: 12),
                      _InfoLine(
                        label: '系统计算时长',
                        value:
                            '${_formatAmount(duration)}${_leaveUnit(_leaveType) == 'day' ? '天' : '小时'}',
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  _FormSurface(
                    children: [
                      TextFormField(
                        controller: _emergencyPhoneController,
                        keyboardType: TextInputType.phone,
                        decoration: const InputDecoration(labelText: '应急电话'),
                        validator: (value) =>
                            (value == null || value.trim().isEmpty)
                            ? '请填写应急电话'
                            : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _handoverController,
                        minLines: 2,
                        maxLines: 4,
                        decoration: const InputDecoration(labelText: '工作交接'),
                        validator: (value) =>
                            (value == null || value.trim().isEmpty)
                            ? '请填写工作交接'
                            : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _remarkController,
                        minLines: 2,
                        maxLines: 4,
                        decoration: const InputDecoration(labelText: '申请说明'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 14),
          child: FilledButton(
            onPressed: _submitting ? null : _submit,
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(46),
              backgroundColor: _blue,
            ),
            child: Text(_submitting ? '提交中...' : '提交审批'),
          ),
        ),
      ),
    );
  }

  void _showMessage(String text) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }
}

class HrPayslipPage extends StatefulWidget {
  const HrPayslipPage({super.key});

  @override
  State<HrPayslipPage> createState() => _HrPayslipPageState();
}

class _HrPayslipPageState extends State<HrPayslipPage> {
  late Future<HrPage<HrPayslip>> _future;

  @override
  void initState() {
    super.initState();
    _future = HrService.getMyPayslips(pageSize: 30);
  }

  Future<void> _refresh() async {
    setState(() {
      _future = HrService.getMyPayslips(pageSize: 30);
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return _ListScaffold<HrPayslip>(
      title: '工资条',
      future: _future,
      onRefresh: _refresh,
      emptyText: '暂无工资条',
      itemBuilder: (item) => _ListTileShell(
        onTap: () => _showPayslip(item),
        child: _CompactRow(
          icon: Icons.payments_outlined,
          title: item.payrollMonth ?? '未命名月份',
          subtitle:
              '基本 ${_money(item.baseSalary)}  扣款 ${_money(item.deduction + item.attendanceDeduction)}',
          tag: _payslipStatus(item.status),
          value: _money(item.netSalary),
        ),
      ),
    );
  }

  void _showPayslip(HrPayslip item) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${item.payrollMonth ?? ''} 工资条',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 14),
                _InfoLine(
                  label: '实发工资',
                  value: _money(item.netSalary),
                  highlighted: true,
                ),
                _InfoLine(label: '基本工资', value: _money(item.baseSalary)),
                _InfoLine(label: '加班工资', value: _money(item.overtimePay)),
                _InfoLine(label: '奖金', value: _money(item.bonus)),
                _InfoLine(label: '津贴', value: _money(item.allowance)),
                _InfoLine(
                  label: '考勤扣款',
                  value: _money(item.attendanceDeduction),
                ),
                _InfoLine(label: '其他扣款', value: _money(item.deduction)),
                _InfoLine(label: '社保', value: _money(item.socialInsurance)),
                _InfoLine(label: '公积金', value: _money(item.housingFund)),
                _InfoLine(label: '个税', value: _money(item.tax)),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: item.id == null
                            ? null
                            : () => _issuePayslip(item.id!),
                        child: const Text('反馈异常'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: FilledButton(
                        onPressed: item.id == null
                            ? null
                            : () => _confirmPayslip(item.id!),
                        style: FilledButton.styleFrom(backgroundColor: _blue),
                        child: const Text('确认无误'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _confirmPayslip(int id) async {
    Navigator.of(context).pop();
    try {
      await HrService.confirmMyPayslip(id);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('工资条已确认')));
      await _refresh();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_cleanError(error))));
    }
  }

  Future<void> _issuePayslip(int id) async {
    final remark = await showDialog<String>(
      context: context,
      builder: (context) {
        final controller = TextEditingController();
        return AlertDialog(
          title: const Text('反馈工资条异常'),
          content: TextField(
            controller: controller,
            minLines: 3,
            maxLines: 5,
            decoration: const InputDecoration(hintText: '请说明异常项和期望核对的信息'),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () =>
                  Navigator.of(context).pop(controller.text.trim()),
              child: const Text('提交'),
            ),
          ],
        );
      },
    );
    if (remark == null || remark.isEmpty) return;

    try {
      await HrService.issueMyPayslip(id: id, issueRemark: remark);
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('异常已提交 HR 核对')));
      await _refresh();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_cleanError(error))));
    }
  }
}

class HrLearningPage extends StatelessWidget {
  final int initialTab;

  const HrLearningPage({super.key, this.initialTab = 0});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      initialIndex: initialTab,
      child: Scaffold(
        backgroundColor: _bg,
        appBar: AppBar(
          title: const Text('学习与互评'),
          titleTextStyle: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: _text,
          ),
          centerTitle: false,
          backgroundColor: _bg,
          foregroundColor: _text,
          elevation: 0,
          scrolledUnderElevation: 0,
          bottom: const TabBar(
            labelColor: _blue,
            unselectedLabelColor: _muted,
            indicatorColor: _blue,
            indicatorSize: TabBarIndicatorSize.label,
            dividerColor: Colors.transparent,
            tabs: [
              Tab(text: '问卷互评'),
              Tab(text: '考试'),
            ],
          ),
        ),
        body: const TabBarView(children: [_QuestionnaireList(), _ExamList()]),
      ),
    );
  }
}

class HrQuestionnaireAnswerPage extends StatefulWidget {
  final HrQuestionnaireAssignment assignment;

  const HrQuestionnaireAnswerPage({super.key, required this.assignment});

  @override
  State<HrQuestionnaireAnswerPage> createState() =>
      _HrQuestionnaireAnswerPageState();
}

class _HrQuestionnaireAnswerPageState extends State<HrQuestionnaireAnswerPage> {
  late Future<HrQuestionnaire> _future;
  final Map<int, _QuestionAnswer> _answers = {};
  Object? _initializedKey;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _future = _loadQuestionnaire();
  }

  Future<HrQuestionnaire> _loadQuestionnaire() async {
    final questionnaireId = widget.assignment.questionnaireId;
    if (questionnaireId == null || questionnaireId <= 0) {
      throw Exception('问卷任务缺少问卷信息');
    }
    return HrService.getAccessibleQuestionnaire(
      questionnaireId: questionnaireId,
      assignmentId: widget.assignment.id,
      publishId: widget.assignment.publishId,
    );
  }

  void _ensureAnswers(HrQuestionnaire questionnaire) {
    final key = '${questionnaire.id}-${questionnaire.items.length}';
    if (_initializedKey == key) return;
    _answers.clear();
    for (final item in questionnaire.items) {
      final itemId = item.id;
      if (itemId != null) _answers[itemId] = _QuestionAnswer();
    }
    _initializedKey = key;
  }

  Future<void> _submit(HrQuestionnaire questionnaire) async {
    final assignmentId = widget.assignment.id;
    final questionnaireId =
        widget.assignment.questionnaireId ?? questionnaire.id;
    if (assignmentId == null || questionnaireId == null) {
      _showMessage('问卷任务信息不完整，无法提交');
      return;
    }

    final unanswered = questionnaire.items.where((item) {
      final itemId = item.id;
      if (!item.required || itemId == null) return false;
      return !_isAnswered(item, _answers[itemId]);
    }).length;
    if (unanswered > 0) {
      _showMessage('还有 $unanswered 道必填题未完成');
      return;
    }

    final payload = questionnaire.items.where((item) => item.id != null).map((
      item,
    ) {
      final answer = _answers[item.id!] ?? _QuestionAnswer();
      return HrQuestionnaireAnswerItem(
        itemId: item.id!,
        answerText: answer.text.trim().isEmpty ? null : answer.text.trim(),
        answerScore: answer.score,
        answerJson: _answerJson(item, answer),
      );
    }).toList();

    setState(() => _saving = true);
    try {
      await HrService.submitQuestionnaireAnswers(
        HrQuestionnaireAnswerSubmitRequest(
          assignmentId: assignmentId,
          questionnaireId: questionnaireId,
          answers: payload,
        ),
      );
      if (!mounted) return;
      _showMessage('问卷已提交');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      _showMessage(_cleanError(error));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        title: const Text('填写问卷'),
        titleTextStyle: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w800,
          color: _text,
        ),
        centerTitle: false,
        backgroundColor: _bg,
        foregroundColor: _text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: FutureBuilder<HrQuestionnaire>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _ErrorState(
              message: _cleanError(snapshot.error),
              onRetry: () async {
                setState(() {
                  _initializedKey = null;
                  _future = _loadQuestionnaire();
                });
                await _future;
              },
            );
          }

          final questionnaire = snapshot.data!;
          _ensureAnswers(questionnaire);
          final answered = _answeredCount(questionnaire);
          final total = questionnaire.items.length;

          return Column(
            children: [
              Container(
                width: double.infinity,
                color: Colors.white,
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      questionnaire.name,
                      style: const TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.w800,
                        color: _text,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Expanded(
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(999),
                            child: LinearProgressIndicator(
                              minHeight: 7,
                              value: total == 0 ? 0 : answered / total,
                              backgroundColor: _border,
                              valueColor: const AlwaysStoppedAnimation<Color>(
                                _blue,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Text(
                          '$answered/$total',
                          style: const TextStyle(fontSize: 12, color: _muted),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Expanded(
                child: questionnaire.items.isEmpty
                    ? const _EmptyState(text: '暂无问卷题目')
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(0, 8, 0, 24),
                        itemBuilder: (context, index) {
                          final item = questionnaire.items[index];
                          return _QuestionItemCard(
                            index: index,
                            item: item,
                            answer: item.id == null
                                ? _QuestionAnswer()
                                : _answers[item.id!]!,
                            multiScoreMode: questionnaire.multiScoreMode,
                            onChanged: () => setState(() {}),
                          );
                        },
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemCount: questionnaire.items.length,
                      ),
              ),
              SafeArea(
                top: false,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 14),
                  child: FilledButton.icon(
                    onPressed: _saving ? null : () => _submit(questionnaire),
                    icon: const Icon(Icons.send_outlined, size: 18),
                    label: Text(_saving ? '提交中...' : '提交问卷'),
                    style: FilledButton.styleFrom(
                      minimumSize: const Size.fromHeight(46),
                      backgroundColor: _blue,
                    ),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  int _answeredCount(HrQuestionnaire questionnaire) {
    return questionnaire.items
        .where((item) => _isAnswered(item, _answers[item.id]))
        .length;
  }

  void _showMessage(String text) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
  }
}

class HrTodoPage extends StatefulWidget {
  const HrTodoPage({super.key});

  @override
  State<HrTodoPage> createState() => _HrTodoPageState();
}

class _HrTodoPageState extends State<HrTodoPage> {
  late Future<HrPage<HrTodoTask>> _future;

  @override
  void initState() {
    super.initState();
    _future = HrService.getMyTodos(pageSize: 30);
  }

  Future<void> _refresh() async {
    setState(() {
      _future = HrService.getMyTodos(pageSize: 30);
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return _ListScaffold<HrTodoTask>(
      title: '人事待办',
      future: _future,
      onRefresh: _refresh,
      emptyText: '暂无待办',
      itemBuilder: (item) => _ListTileShell(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: _CompactRow(
                icon: Icons.task_alt_outlined,
                title: item.title,
                subtitle: _joinText([
                  item.content,
                  item.dueTime == null
                      ? null
                      : '截止 ${_shortDate(item.dueTime)}',
                ]),
                tag: _priorityName(item.priority),
              ),
            ),
            const SizedBox(width: 8),
            TextButton(
              onPressed: item.id == null ? null : () => _complete(item.id!),
              child: const Text('完成'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _complete(int id) async {
    try {
      await HrService.completeTodo(id: id);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('待办已完成')));
      await _refresh();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_cleanError(error))));
    }
  }
}

class HrAttendancePage extends StatefulWidget {
  const HrAttendancePage({super.key});

  @override
  State<HrAttendancePage> createState() => _HrAttendancePageState();
}

class _HrAttendancePageState extends State<HrAttendancePage> {
  late Future<HrPage<HrAttendanceRecord>> _future;

  @override
  void initState() {
    super.initState();
    _future = HrService.getMyAttendancePage(pageSize: 30);
  }

  Future<void> _refresh() async {
    setState(() {
      _future = HrService.getMyAttendancePage(pageSize: 30);
    });
    await _future;
  }

  Future<void> _openDingTalkAttendance() async {
    try {
      final opened =
          await _nativeIntents.invokeMethod<bool>('openDingTalkAttendance') ??
          false;
      if (!mounted) return;
      if (!opened) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('未检测到钉钉，请打开钉钉完成考勤打卡')));
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('无法拉起钉钉，请手动打开钉钉打卡')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return _ListScaffold<HrAttendanceRecord>(
      title: '考勤记录',
      future: _future,
      onRefresh: _refresh,
      emptyText: '暂无考勤记录',
      header: Container(
        color: Colors.white,
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
        child: Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                onPressed: _openDingTalkAttendance,
                icon: const Icon(Icons.open_in_new, size: 18),
                label: const Text('打开钉钉打卡'),
                style: FilledButton.styleFrom(
                  backgroundColor: _blue,
                  minimumSize: const Size.fromHeight(42),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: _refresh,
                icon: const Icon(Icons.refresh, size: 18),
                label: const Text('刷新记录'),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(42),
                  side: const BorderSide(color: _line),
                ),
              ),
            ),
          ],
        ),
      ),
      itemBuilder: (item) =>
          _ListTileShell(child: _AttendanceRecordRow(item: item)),
    );
  }
}

class _QuestionnaireList extends StatefulWidget {
  const _QuestionnaireList();

  @override
  State<_QuestionnaireList> createState() => _QuestionnaireListState();
}

class _QuestionnaireListState extends State<_QuestionnaireList> {
  late Future<HrPage<HrQuestionnaireAssignment>> _future;

  @override
  void initState() {
    super.initState();
    _future = HrService.getMyQuestionnaires(pageSize: 30);
  }

  Future<void> _refresh() async {
    setState(() {
      _future = HrService.getMyQuestionnaires(pageSize: 30);
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return _PageListBody<HrQuestionnaireAssignment>(
      future: _future,
      onRefresh: _refresh,
      emptyText: '暂无问卷任务',
      itemBuilder: (item) => _ListTileShell(
        onTap: item.status == 1
            ? null
            : () async {
                await Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => HrQuestionnaireAnswerPage(assignment: item),
                  ),
                );
                if (mounted) await _refresh();
              },
        child: _CompactRow(
          icon: Icons.ballot_outlined,
          title: item.batchLabel ?? '问卷互评任务',
          subtitle: _joinText([
            item.targetName == null ? null : '评价对象 ${item.targetName}',
            item.role,
          ]),
          tag: _questionnaireStatus(item.status),
        ),
      ),
    );
  }
}

class _ExamList extends StatefulWidget {
  const _ExamList();

  @override
  State<_ExamList> createState() => _ExamListState();
}

class _ExamListState extends State<_ExamList> {
  late Future<HrPage<HrExamPublish>> _future;

  @override
  void initState() {
    super.initState();
    _future = HrService.getMyExams(pageSize: 30);
  }

  Future<void> _refresh() async {
    setState(() {
      _future = HrService.getMyExams(pageSize: 30);
    });
    await _future;
  }

  @override
  Widget build(BuildContext context) {
    return _PageListBody<HrExamPublish>(
      future: _future,
      onRefresh: _refresh,
      emptyText: '暂无考试任务',
      itemBuilder: (item) => _ListTileShell(
        onTap: () => _showExamDetail(context, item),
        child: _CompactRow(
          icon: Icons.school_outlined,
          title: item.examName ?? '考试任务',
          subtitle: _joinText([
            item.batchLabel,
            item.endAt == null ? null : '截止 ${_shortDate(item.endAt)}',
            item.durationMin == null ? null : '${item.durationMin} 分钟',
          ]),
          tag: _examStatus(item.status),
        ),
      ),
    );
  }
}

class _QuestionItemCard extends StatelessWidget {
  final int index;
  final HrQuestionnaireItem item;
  final _QuestionAnswer answer;
  final String multiScoreMode;
  final VoidCallback onChanged;

  const _QuestionItemCard({
    required this.index,
    required this.item,
    required this.answer,
    required this.multiScoreMode,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 28,
                height: 28,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: _bg,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  '${index + 1}',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                    color: _blue,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  item.title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    color: _text,
                    height: 1.35,
                  ),
                ),
              ),
              if (item.required) const _Badge(text: '必填', color: _red),
            ],
          ),
          const SizedBox(height: 14),
          _buildAnswerControl(),
        ],
      ),
    );
  }

  Widget _buildAnswerControl() {
    final type = item.itemType.toLowerCase();
    if (type == 'single') {
      return Column(
        children: item.options
            .where((option) => option.optionText.isNotEmpty)
            .map(
              (option) => RadioListTile<String>(
                dense: true,
                visualDensity: VisualDensity.compact,
                contentPadding: EdgeInsets.zero,
                value: option.optionText,
                groupValue: answer.single,
                activeColor: _blue,
                title: Text(
                  option.optionText,
                  style: const TextStyle(
                    fontSize: 14,
                    color: _text,
                    height: 1.25,
                  ),
                ),
                onChanged: (value) {
                  answer.single = value;
                  answer.score = option.optionScore;
                  onChanged();
                },
              ),
            )
            .toList(),
      );
    }

    if (type == 'multi') {
      return Column(
        children: item.options
            .where((option) => option.optionText.isNotEmpty)
            .map(
              (option) => CheckboxListTile(
                dense: true,
                visualDensity: VisualDensity.compact,
                contentPadding: EdgeInsets.zero,
                value: answer.multi.contains(option.optionText),
                activeColor: _blue,
                controlAffinity: ListTileControlAffinity.leading,
                title: Text(
                  option.optionText,
                  style: const TextStyle(
                    fontSize: 14,
                    color: _text,
                    height: 1.25,
                  ),
                ),
                onChanged: (checked) {
                  if (checked == true) {
                    answer.multi.add(option.optionText);
                  } else {
                    answer.multi.remove(option.optionText);
                  }
                  answer.score = _calculateQuestionMultiScore(
                    item,
                    answer.multi.toList(),
                    multiScoreMode,
                  );
                  onChanged();
                },
              ),
            )
            .toList(),
      );
    }

    if (type == 'score' || type == 'score_text') {
      final maxScore = item.maxScore <= 0 ? 10.0 : item.maxScore;
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Slider(
                  value: (answer.score ?? 0).clamp(0, maxScore).toDouble(),
                  min: 0,
                  max: maxScore,
                  divisions: maxScore.round().clamp(1, 100).toInt(),
                  label: _formatAmount(answer.score ?? 0),
                  activeColor: _blue,
                  onChanged: (value) {
                    answer.score = value.roundToDouble();
                    onChanged();
                  },
                ),
              ),
              SizedBox(
                width: 48,
                child: Text(
                  _formatAmount(answer.score ?? 0),
                  textAlign: TextAlign.right,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    color: _text,
                  ),
                ),
              ),
            ],
          ),
          if (type == 'score_text')
            TextFormField(
              initialValue: answer.text,
              minLines: 2,
              maxLines: 4,
              decoration: _fieldDecoration('原因说明'),
              onChanged: (value) {
                answer.text = value;
                onChanged();
              },
            ),
        ],
      );
    }

    return TextFormField(
      initialValue: answer.text,
      minLines: type == 'blank' ? 1 : 3,
      maxLines: type == 'blank' ? 2 : 6,
      decoration: _fieldDecoration(type == 'blank' ? '填空答案' : '回答内容'),
      onChanged: (value) {
        answer.text = value;
        onChanged();
      },
    );
  }
}

class _QuestionAnswer {
  String? single;
  final Set<String> multi = <String>{};
  double? score;
  String text = '';
}

class _HrDashboardData {
  final HrSelfServiceHome home;
  final List<HrApplicationItem> applications;
  final List<HrLeaveBalance> balances;
  final List<HrPayslip> payslips;
  final List<HrQuestionnaireAssignment> questionnaires;
  final List<HrExamPublish> exams;
  final List<HrTodoTask> todos;

  const _HrDashboardData({
    required this.home,
    required this.applications,
    required this.balances,
    required this.payslips,
    required this.questionnaires,
    required this.exams,
    required this.todos,
  });
}

class _MobileAction {
  final String title;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  const _MobileAction(this.title, this.icon, this.color, this.onTap);
}

class _ActionGrid extends StatelessWidget {
  final List<_MobileAction> actions;

  const _ActionGrid({required this.actions});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(8, 8, 8, 12),
      child: GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 4,
          mainAxisSpacing: 4,
          crossAxisSpacing: 2,
          childAspectRatio: 0.9,
        ),
        itemCount: actions.length,
        itemBuilder: (context, index) {
          final action = actions[index];
          return InkWell(
            onTap: action.onTap,
            borderRadius: BorderRadius.circular(8),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: action.color.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(action.icon, color: action.color, size: 21),
                  ),
                  const SizedBox(height: 7),
                  Text(
                    action.title,
                    maxLines: 2,
                    textAlign: TextAlign.center,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: _text,
                      height: 1.15,
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

class _ProfileHeader extends StatelessWidget {
  final HrSelfServiceHome home;

  const _ProfileHeader({required this.home});

  @override
  Widget build(BuildContext context) {
    final profile = home.profile;
    final employment = home.employment;
    final health = home.profileHealth;

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 23,
                backgroundColor: _blue.withValues(alpha: 0.12),
                child: Text(
                  (profile?.name.isNotEmpty ?? false)
                      ? profile!.name.substring(0, 1)
                      : '员',
                  style: const TextStyle(
                    color: _blue,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      profile?.name ?? '未匹配员工档案',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: _text,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      _joinText([
                        employment?.jobTitle,
                        employment?.workStatusText,
                        employment?.employeeNo == null
                            ? null
                            : '工号 ${employment!.employeeNo}',
                      ]),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 13, color: _muted),
                    ),
                  ],
                ),
              ),
              _Badge(
                text: home.hasProfile ? '已建档' : '未建档',
                color: home.hasProfile ? _green : _orange,
              ),
            ],
          ),
          const SizedBox(height: 16),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              minHeight: 7,
              value: ((health?.completeness ?? 0).clamp(0, 100)) / 100,
              backgroundColor: _border,
              valueColor: AlwaysStoppedAnimation<Color>(
                (health?.completeness ?? 0) >= 80 ? _green : _orange,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '资料完整度 ${health?.completeness ?? 0}%',
            style: const TextStyle(fontSize: 12, color: _muted, height: 1.2),
          ),
        ],
      ),
    );
  }
}

class _TodayAttendancePanel extends StatelessWidget {
  final HrTodayAttendance? attendance;
  final VoidCallback onOpenDingTalk;
  final VoidCallback onRefresh;

  const _TodayAttendancePanel({
    required this.attendance,
    required this.onOpenDingTalk,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '今日考勤',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: _text,
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _MetricCell(
                  label: '上班',
                  value: _attendanceTime(attendance?.clockInTime),
                  status: attendance?.clockInStatus,
                ),
              ),
              Expanded(
                child: _MetricCell(
                  label: '下班',
                  value: _attendanceTime(attendance?.clockOutTime),
                  status: attendance?.clockOutStatus,
                ),
              ),
              Expanded(
                child: _MetricCell(
                  label: '本月打卡',
                  value: '${attendance?.monthClockDays ?? 0}天',
                  status: attendance?.onLeaveToday == true ? '今日请假' : null,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: onOpenDingTalk,
                  icon: const Icon(Icons.open_in_new, size: 18),
                  label: const Text('钉钉打卡'),
                  style: FilledButton.styleFrom(
                    backgroundColor: _blue,
                    minimumSize: const Size.fromHeight(42),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: onRefresh,
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('刷新'),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(42),
                    side: const BorderSide(color: _line),
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

class _MetricCell extends StatelessWidget {
  final String label;
  final String value;
  final String? status;

  const _MetricCell({required this.label, required this.value, this.status});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: _muted)),
        const SizedBox(height: 4),
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w800,
            color: _text,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          status ?? '正常',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            fontSize: 11,
            color: status == null ? _muted : _orange,
          ),
        ),
      ],
    );
  }
}

class _LearningPreview extends StatelessWidget {
  final List<HrQuestionnaireAssignment> questionnaires;
  final List<HrExamPublish> exams;
  final VoidCallback onMore;

  const _LearningPreview({
    required this.questionnaires,
    required this.exams,
    required this.onMore,
  });

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[
      ...questionnaires
          .take(2)
          .map(
            (item) => _CompactRow(
              icon: Icons.ballot_outlined,
              title: item.batchLabel ?? '问卷互评任务',
              subtitle: item.targetName == null
                  ? '问卷任务'
                  : '评价对象 ${item.targetName}',
              tag: _questionnaireStatus(item.status),
            ),
          ),
      ...exams
          .take(2)
          .map(
            (item) => _CompactRow(
              icon: Icons.school_outlined,
              title: item.examName ?? '考试任务',
              subtitle: item.endAt == null
                  ? '考试任务'
                  : '截止 ${_shortDate(item.endAt)}',
              tag: _examStatus(item.status),
            ),
          ),
    ];

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  '学习与互评',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    color: _text,
                  ),
                ),
              ),
              TextButton(onPressed: onMore, child: const Text('查看全部')),
            ],
          ),
          if (questionnaires.isEmpty && exams.isEmpty)
            const _EmptyMini(text: '暂无问卷或考试任务')
          else
            ...rows.asMap().entries.map((entry) {
              final isLast = entry.key == rows.length - 1;
              return Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: entry.value,
                  ),
                  if (!isLast)
                    const Divider(height: 1, indent: 48, color: _line),
                ],
              );
            }),
        ],
      ),
    );
  }
}

class _LeaveBalanceStrip extends StatelessWidget {
  final List<HrLeaveBalance> balances;

  const _LeaveBalanceStrip({required this.balances});

  @override
  Widget build(BuildContext context) {
    if (balances.isEmpty) {
      return Container(
        color: Colors.white,
        padding: const EdgeInsets.fromLTRB(16, 18, 16, 18),
        child: const _EmptyMini(text: '暂无假期余额'),
      );
    }

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 10),
      child: Column(
        children: balances.asMap().entries.map((entry) {
          final item = entry.value;
          final isLast = entry.key == balances.length - 1;
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 10),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        item.leaveTypeName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: _text,
                        ),
                      ),
                    ),
                    Text(
                      '剩余 ${_formatAmount(item.remainAmount)}',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                        color: _text,
                      ),
                    ),
                    const SizedBox(width: 10),
                    SizedBox(
                      width: 58,
                      child: Text(
                        '已用 ${_formatAmount(item.usedAmount)}',
                        textAlign: TextAlign.right,
                        style: const TextStyle(fontSize: 12, color: _muted),
                      ),
                    ),
                  ],
                ),
              ),
              if (!isLast) const Divider(height: 1, color: _line),
            ],
          );
        }).toList(),
      ),
    );
  }
}

class _PreviewSection<T> extends StatelessWidget {
  final String title;
  final String emptyText;
  final List<T> items;
  final Widget Function(T item) itemBuilder;
  final VoidCallback onMore;

  const _PreviewSection({
    required this.title,
    required this.emptyText,
    required this.items,
    required this.itemBuilder,
    required this.onMore,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    color: _text,
                  ),
                ),
              ),
              TextButton(onPressed: onMore, child: const Text('查看全部')),
            ],
          ),
          if (items.isEmpty)
            _EmptyMini(text: emptyText)
          else
            ...items.asMap().entries.map((entry) {
              final isLast = entry.key == items.length - 1;
              return Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: itemBuilder(entry.value),
                  ),
                  if (!isLast)
                    const Divider(height: 1, indent: 48, color: _line),
                ],
              );
            }),
        ],
      ),
    );
  }
}

class _ListScaffold<T> extends StatelessWidget {
  final String title;
  final Future<HrPage<T>> future;
  final Future<void> Function() onRefresh;
  final String emptyText;
  final Widget Function(T item) itemBuilder;
  final Widget? header;

  const _ListScaffold({
    required this.title,
    required this.future,
    required this.onRefresh,
    required this.emptyText,
    required this.itemBuilder,
    this.header,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        title: Text(title),
        titleTextStyle: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.w800,
          color: _text,
        ),
        centerTitle: false,
        backgroundColor: _bg,
        foregroundColor: _text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: Column(
        children: [
          if (header != null) header!,
          Expanded(
            child: _PageListBody<T>(
              future: future,
              onRefresh: onRefresh,
              emptyText: emptyText,
              itemBuilder: itemBuilder,
            ),
          ),
        ],
      ),
    );
  }
}

class _PageListBody<T> extends StatelessWidget {
  final Future<HrPage<T>> future;
  final Future<void> Function() onRefresh;
  final String emptyText;
  final Widget Function(T item) itemBuilder;

  const _PageListBody({
    required this.future,
    required this.onRefresh,
    required this.emptyText,
    required this.itemBuilder,
  });

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<HrPage<T>>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _ErrorState(
            message: _cleanError(snapshot.error),
            onRetry: onRefresh,
          );
        }
        final items = snapshot.data?.list ?? <T>[];
        return RefreshIndicator(
          onRefresh: onRefresh,
          child: items.isEmpty
              ? ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    const SizedBox(height: 80),
                    _EmptyState(text: emptyText),
                  ],
                )
              : ListView.separated(
                  padding: const EdgeInsets.fromLTRB(0, 10, 0, 28),
                  itemBuilder: (context, index) => itemBuilder(items[index]),
                  separatorBuilder: (_, __) => Container(
                    color: Colors.white,
                    child: const Divider(height: 1, indent: 64, color: _line),
                  ),
                  itemCount: items.length,
                ),
        );
      },
    );
  }
}

class _ListTileShell extends StatelessWidget {
  final Widget child;
  final VoidCallback? onTap;

  const _ListTileShell({required this.child, this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        color: Colors.white,
        padding: const EdgeInsets.fromLTRB(16, 13, 16, 13),
        child: child,
      ),
    );
  }
}

class _AttendanceRecordRow extends StatelessWidget {
  final HrAttendanceRecord item;

  const _AttendanceRecordRow({required this.item});

  @override
  Widget build(BuildContext context) {
    final typeName = _clockTypeName(item.clockType);
    final time = _attendanceTime(item.clockTime);
    final day = _attendanceDay(item.attendanceDate, item.clockTime);
    final daySub = _attendanceDaySub(item.attendanceDate, item.clockTime);
    final statusText = _attendanceStatusText(item.clockStatus);
    final sourceText = _attendanceSourceText(item.sourceType);
    final location = item.locationName?.trim();
    final subtitle = _joinText([
      location == null || location.isEmpty ? null : location,
      sourceText,
    ]);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        SizedBox(
          width: 54,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                day,
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w800,
                  color: _text,
                ),
              ),
              const SizedBox(height: 3),
              Text(daySub, style: const TextStyle(fontSize: 11, color: _muted)),
            ],
          ),
        ),
        Container(width: 1, height: 38, color: _line),
        const SizedBox(width: 12),
        Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            color: (item.clockType == 'OUT' ? _slate : _blue).withValues(
              alpha: 0.1,
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            item.clockType == 'OUT' ? Icons.logout : Icons.login,
            color: item.clockType == 'OUT' ? _slate : _blue,
            size: 19,
          ),
        ),
        const SizedBox(width: 11),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      typeName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                        color: _text,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    time,
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                      color: _text,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 5),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      subtitle.isEmpty ? '暂无地点信息' : subtitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 12, color: _muted),
                    ),
                  ),
                  const SizedBox(width: 8),
                  _Badge(
                    text: statusText,
                    color: _attendanceStatusColor(statusText),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CompactRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final String tag;
  final String? value;

  const _CompactRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.tag,
    this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: _iconTint(icon),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: _iconColor(icon), size: 20),
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
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: _text,
                ),
              ),
              if (subtitle.isNotEmpty) ...[
                const SizedBox(height: 3),
                Text(
                  subtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 12,
                    color: _muted,
                    height: 1.35,
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(width: 8),
        Column(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            if (value != null)
              Text(
                value!,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w800,
                  color: _text,
                ),
              ),
            _Badge(text: tag, color: _statusColor(tag)),
          ],
        ),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final String? trailing;

  const _SectionTitle({required this.title, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 9),
      child: Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w800,
                color: _text,
              ),
            ),
          ),
          if (trailing != null)
            Text(
              trailing!,
              style: const TextStyle(fontSize: 12, color: _muted),
            ),
        ],
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  final String text;
  final Color color;

  const _Badge({required this.text, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        text,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          color: color,
        ),
      ),
    );
  }
}

class _InfoLine extends StatelessWidget {
  final String label;
  final String value;
  final bool highlighted;

  const _InfoLine({
    required this.label,
    required this.value,
    this.highlighted = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: const TextStyle(fontSize: 13, color: _muted),
            ),
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: highlighted ? 18 : 14,
              fontWeight: highlighted ? FontWeight.w800 : FontWeight.w700,
              color: highlighted ? _blue : _text,
            ),
          ),
        ],
      ),
    );
  }
}

class _DateTimeField extends StatelessWidget {
  final String label;
  final DateTime value;
  final ValueChanged<DateTime> onChanged;

  const _DateTimeField({
    required this.label,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () async {
        final date = await showDatePicker(
          context: context,
          initialDate: value,
          firstDate: DateTime.now().subtract(const Duration(days: 1)),
          lastDate: DateTime.now().add(const Duration(days: 365)),
        );
        if (date == null || !context.mounted) return;
        final time = await showTimePicker(
          context: context,
          initialTime: TimeOfDay.fromDateTime(value),
          minuteLabelText: '分钟',
          hourLabelText: '小时',
        );
        if (time == null) return;
        onChanged(
          DateTime(date.year, date.month, date.day, time.hour, time.minute),
        );
      },
      borderRadius: BorderRadius.circular(8),
      child: InputDecorator(
        decoration: InputDecoration(labelText: label),
        child: Row(
          children: [
            Expanded(child: Text(_formatDisplayDateTime(value))),
            const Icon(Icons.expand_more, color: _muted),
          ],
        ),
      ),
    );
  }
}

class _FormSurface extends StatelessWidget {
  final List<Widget> children;

  const _FormSurface({required this.children});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: _surfaceDecoration(),
      padding: const EdgeInsets.all(14),
      child: Column(children: children),
    );
  }
}

class _InlineWarning extends StatelessWidget {
  final String text;

  const _InlineWarning({required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _orange.withValues(alpha: 0.1),
        border: Border.all(color: _orange.withValues(alpha: 0.25)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(text, style: const TextStyle(color: _orange, fontSize: 13)),
    );
  }
}

class _EmptyMini extends StatelessWidget {
  final String text;

  const _EmptyMini({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 18),
      child: Center(
        child: Text(text, style: const TextStyle(fontSize: 13, color: _muted)),
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
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: _border),
              borderRadius: BorderRadius.circular(999),
            ),
            child: const Icon(Icons.inbox_outlined, color: _muted),
          ),
          const SizedBox(height: 12),
          Text(text, style: const TextStyle(color: _muted)),
        ],
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
            const Icon(Icons.error_outline, color: _red, size: 40),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _text, height: 1.4),
            ),
            const SizedBox(height: 14),
            OutlinedButton(onPressed: onRetry, child: const Text('重试')),
          ],
        ),
      ),
    );
  }
}

class _LeaveOption {
  final String code;
  final String name;

  const _LeaveOption(this.code, this.name);
}

Future<HrPage<T>> _safePage<T>(Future<HrPage<T>> Function() load) async {
  try {
    return await load();
  } catch (_) {
    return HrPage<T>(list: <T>[], total: 0);
  }
}

Future<List<T>> _safeList<T>(Future<List<T>> Function() load) async {
  try {
    return await load();
  } catch (_) {
    return <T>[];
  }
}

List<_LeaveOption> _buildLeaveOptions(List<HrLeaveBalance> balances) {
  if (balances.isNotEmpty) {
    final options = balances
        .map((item) => _LeaveOption(item.leaveTypeCode, item.leaveTypeName))
        .where((item) => item.code.isNotEmpty)
        .toList();
    if (options.isNotEmpty) return options;
  }
  return const [
    _LeaveOption('annual', '年假'),
    _LeaveOption('personal', '事假'),
    _LeaveOption('sick', '病假'),
    _LeaveOption('rest', '休息'),
    _LeaveOption('marriage', '婚假'),
    _LeaveOption('maternity', '产假'),
  ];
}

double _calculateDuration(DateTime start, DateTime end, String leaveType) {
  final minutes = end.difference(start).inMinutes;
  if (minutes <= 0) return 0;
  if (_leaveUnit(leaveType) == 'day') {
    return minutes / (24 * 60);
  }
  return minutes / 60;
}

String _leaveUnit(String leaveType) {
  return leaveType == 'marriage' || leaveType == 'maternity' ? 'day' : 'hour';
}

DateTime _roundToHalfHour(DateTime value) {
  final minute = value.minute < 30 ? 30 : 0;
  final hour = value.minute < 30 ? value.hour : value.hour + 1;
  return DateTime(value.year, value.month, value.day, hour, minute);
}

void _showExamDetail(BuildContext context, HrExamPublish item) {
  showModalBottomSheet(
    context: context,
    backgroundColor: Colors.white,
    showDragHandle: true,
    isScrollControlled: true,
    builder: (context) {
      return SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      item.examName ?? '考试任务',
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: _text,
                        height: 1.25,
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  _Badge(
                    text: _examStatus(item.status),
                    color: _statusColor(_examStatus(item.status)),
                  ),
                ],
              ),
              if ((item.batchLabel ?? '').isNotEmpty) ...[
                const SizedBox(height: 6),
                Text(
                  item.batchLabel!,
                  style: const TextStyle(fontSize: 13, color: _muted),
                ),
              ],
              const SizedBox(height: 18),
              _InfoLine(label: '批次', value: item.batchLabel ?? '-'),
              _InfoLine(label: '开始时间', value: _shortDate(item.startAt)),
              _InfoLine(label: '截止时间', value: _shortDate(item.endAt)),
              _InfoLine(
                label: '考试时长',
                value: item.durationMin == null
                    ? '-'
                    : '${item.durationMin} 分钟',
              ),
              _InfoLine(
                label: '最多次数',
                value: item.maxAttempts == null ? '-' : '${item.maxAttempts} 次',
              ),
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.of(context).pop(),
                  style: FilledButton.styleFrom(
                    backgroundColor: _blue,
                    minimumSize: const Size.fromHeight(44),
                  ),
                  child: const Text('关闭'),
                ),
              ),
            ],
          ),
        ),
      );
    },
  );
}

bool _isAnswered(HrQuestionnaireItem item, _QuestionAnswer? answer) {
  if (answer == null) {
    return false;
  }
  final type = item.itemType.toLowerCase();
  if (type == 'single') {
    return answer.single != null && answer.single!.isNotEmpty;
  }
  if (type == 'multi') {
    return answer.multi.isNotEmpty;
  }
  if (type == 'score') {
    return answer.score != null;
  }
  if (type == 'score_text') {
    return answer.score != null && answer.text.trim().isNotEmpty;
  }
  return answer.text.trim().isNotEmpty;
}

String? _answerJson(HrQuestionnaireItem item, _QuestionAnswer answer) {
  final type = item.itemType.toLowerCase();
  if (type == 'single') return jsonEncode(answer.single ?? '');
  if (type == 'multi') return jsonEncode(answer.multi.toList());
  return null;
}

double? _calculateQuestionMultiScore(
  HrQuestionnaireItem item,
  List<String> values,
  String mode,
) {
  if (mode == 'none' || values.isEmpty) return null;
  final scores = item.options
      .where((option) => values.contains(option.optionText))
      .map((option) => option.optionScore)
      .whereType<double>()
      .toList();
  if (scores.isEmpty) return null;
  if (mode == 'max') return scores.reduce((a, b) => a > b ? a : b);
  if (mode == 'sum') return scores.reduce((a, b) => a + b);
  final total = scores.reduce((a, b) => a + b);
  return (total / scores.length).roundToDouble();
}

BoxDecoration _surfaceDecoration() {
  return BoxDecoration(color: Colors.white);
}

InputDecoration _fieldDecoration(String label) {
  return InputDecoration(
    labelText: label,
    filled: true,
    fillColor: _bg,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: BorderSide.none,
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: BorderSide.none,
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: _blue),
    ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
  );
}

Color _iconColor(IconData icon) {
  if (icon == Icons.ballot_outlined) {
    return _cyan;
  }
  if (icon == Icons.school_outlined) {
    return _indigo;
  }
  if (icon == Icons.payments_outlined) {
    return _purple;
  }
  if (icon == Icons.task_alt_outlined) {
    return _red;
  }
  if (icon == Icons.access_time ||
      icon == Icons.login ||
      icon == Icons.logout) {
    return _blue;
  }
  if (icon == Icons.event_available) {
    return _green;
  }
  if (icon == Icons.fact_check_outlined || icon == Icons.receipt_long) {
    return _orange;
  }
  return _slate;
}

Color _iconTint(IconData icon) {
  return _iconColor(icon).withValues(alpha: 0.1);
}

Color _statusColor(String text) {
  if (text.contains('通过') || text.contains('完成') || text.contains('确认')) {
    return _green;
  }
  if (text.contains('拒') || text.contains('异常') || text.contains('超')) {
    return _red;
  }
  if (text.contains('审批') || text.contains('进行') || text.contains('高')) {
    return _orange;
  }
  return _slate;
}

String _joinText(List<String?> values) {
  return values
      .where((value) => value != null && value.trim().isNotEmpty)
      .map((value) => value!.trim())
      .join(' · ');
}

String _dateRange(String? start, String? end) {
  if ((start == null || start.isEmpty) && (end == null || end.isEmpty)) {
    return '';
  }
  return '${_shortDate(start)} 至 ${_shortDate(end)}';
}

String _shortDate(String? value) {
  if (value == null || value.isEmpty) return '-';
  final normalized = value.replaceFirst('T', ' ');
  return normalized.length > 16 ? normalized.substring(0, 16) : normalized;
}

String _attendanceTime(String? value) {
  final raw = value?.trim();
  if (raw == null || raw.isEmpty) return '-';
  var normalized = raw.replaceFirst('T', ' ');
  if (normalized.contains(' ')) {
    normalized = normalized.split(' ').last;
  }
  normalized = normalized.split('.').first;
  if (!normalized.contains(':')) return '-';
  return normalized.length >= 5 ? normalized.substring(0, 5) : normalized;
}

String _attendanceDay(String? date, String? fallbackTime) {
  final datePart =
      _attendanceDatePart(date) ?? _attendanceDatePart(fallbackTime);
  if (datePart == null) return '-';
  return datePart.length >= 10 ? datePart.substring(5, 10) : datePart;
}

String _attendanceDaySub(String? date, String? fallbackTime) {
  final datePart =
      _attendanceDatePart(date) ?? _attendanceDatePart(fallbackTime);
  if (datePart == null) return '未记录';
  final parsed = DateTime.tryParse(datePart);
  if (parsed == null) return datePart;
  const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  if (parsed.year == DateTime.now().year) {
    return weekdays[parsed.weekday - 1];
  }
  return parsed.year.toString();
}

String? _attendanceDatePart(String? value) {
  final raw = value?.trim();
  if (raw == null || raw.isEmpty) return null;
  final normalized = raw.replaceFirst('T', ' ').replaceAll('/', '-');
  if (normalized.length >= 10 && normalized.substring(0, 10).contains('-')) {
    return normalized.substring(0, 10);
  }
  return null;
}

String _attendanceStatusText(String? status) {
  final raw = status?.trim();
  if (raw == null || raw.isEmpty) return '未标记';
  final upper = raw.toUpperCase();
  switch (upper) {
    case 'NORMAL':
    case 'SUCCESS':
    case 'OK':
    case 'CHECK_NORMAL':
      return '正常';
    case 'LATE':
    case 'CHECK_LATE':
      return '迟到';
    case 'EARLY':
    case 'EARLY_LEAVE':
    case 'CHECK_EARLY':
      return '早退';
    case 'ABSENT':
    case 'MISSING':
    case 'NO_RECORD':
      return '缺卡';
    case 'OUTSIDE':
    case 'FIELD':
      return '外勤';
    default:
      return raw;
  }
}

Color _attendanceStatusColor(String text) {
  if (text.contains('正常') || text.contains('外勤')) return _green;
  if (text.contains('迟') || text.contains('早')) return _orange;
  if (text.contains('缺') || text.contains('异常') || text.contains('失败')) {
    return _red;
  }
  return _slate;
}

String? _attendanceSourceText(String? source) {
  final raw = source?.trim();
  if (raw == null || raw.isEmpty) return null;
  final upper = raw.toUpperCase();
  if (upper.contains('DING')) return '钉钉';
  if (upper == 'OA' || upper == 'APP' || upper == 'MOBILE') return '移动端';
  if (upper == 'MANUAL') return '人工补录';
  if (upper == 'AUTO') return '系统记录';
  return raw;
}

String _formatDisplayDateTime(DateTime value) {
  String two(int number) => number.toString().padLeft(2, '0');
  return '${value.year}-${two(value.month)}-${two(value.day)} ${two(value.hour)}:${two(value.minute)}';
}

String _formatAmount(double value) {
  if (value == value.roundToDouble()) return value.toInt().toString();
  return value.toStringAsFixed(1);
}

String _money(double value) {
  return '¥${value.toStringAsFixed(2)}';
}

String _businessTypeName(String type) {
  switch (type) {
    case 'LEAVE':
    case 'leave':
      return '请假申请';
    case 'TRIP':
    case 'trip':
      return '出差申请';
    case 'OVERTIME':
    case 'overtime':
      return '加班申请';
    case 'CORRECTION':
    case 'correction':
      return '补卡申请';
    default:
      return type.isEmpty ? '人事申请' : type;
  }
}

String _payslipStatus(String? status) {
  switch (status) {
    case 'CONFIRMED':
      return '已确认';
    case 'ISSUE':
      return '有异常';
    case 'RESOLVED':
      return '已处理';
    case 'PUBLISHED':
      return '待确认';
    case 'DRAFT':
      return '草稿';
    default:
      return status ?? '-';
  }
}

String _priorityName(String? priority) {
  switch (priority) {
    case 'HIGH':
      return '高优先级';
    case 'LOW':
      return '低优先级';
    case 'MEDIUM':
      return '中优先级';
    default:
      return priority ?? '-';
  }
}

String _questionnaireStatus(int? status) {
  switch (status) {
    case 0:
      return '待填写';
    case 1:
      return '已提交';
    default:
      return status == null ? '-' : '状态$status';
  }
}

String _examStatus(int? status) {
  switch (status) {
    case 0:
      return '草稿';
    case 1:
      return '进行中';
    case 2:
      return '已暂停';
    case 3:
      return '已结束';
    default:
      return status == null ? '-' : '状态$status';
  }
}

String _clockTypeName(String? type) {
  return type == 'OUT' ? '下班打卡' : '上班打卡';
}

String _cleanError(Object? error) {
  final text = error?.toString() ?? '请求失败';
  return text
      .replaceFirst('Exception: ', '')
      .replaceFirst('ApiException: ', '');
}

const Color _bg = Color(0xFFF6F8FB);
const Color _text = Color(0xFF172033);
const Color _muted = Color(0xFF6B7280);
const Color _border = Color(0xFFE5E7EB);
const Color _line = Color(0xFFEFF2F6);
const Color _blue = Color(0xFF2563EB);
const Color _green = Color(0xFF059669);
const Color _orange = Color(0xFFD97706);
const Color _purple = Color(0xFF7C3AED);
const Color _cyan = Color(0xFF0891B2);
const Color _indigo = Color(0xFF4F46E5);
const Color _red = Color(0xFFDC2626);
const Color _slate = Color(0xFF475569);

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
