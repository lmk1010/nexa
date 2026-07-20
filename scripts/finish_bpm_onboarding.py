from pathlib import Path
import json
import urllib.request
import subprocess
import os
import time

root = Path("E:/code/nexa")
os.chdir(root)

# dart page
(root / "apps/mobile/lib/pages/tenant_onboarding_page.dart").write_text(
    r"""import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';

/// Minimal nexa tenant onboarding: register company or accept invite.
class TenantOnboardingPage extends StatefulWidget {
  const TenantOnboardingPage({super.key});

  @override
  State<TenantOnboardingPage> createState() => _TenantOnboardingPageState();
}

class _TenantOnboardingPageState extends State<TenantOnboardingPage>
    with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 2, vsync: this);
  final _company = TextEditingController();
  final _admin = TextEditingController();
  final _password = TextEditingController(text: 'pass123');
  final _invite = TextEditingController();
  final _joinUser = TextEditingController();
  final _joinPass = TextEditingController(text: 'pass123');
  String _msg = '';
  bool _loading = false;

  @override
  void dispose() {
    _tabs.dispose();
    _company.dispose();
    _admin.dispose();
    _password.dispose();
    _invite.dispose();
    _joinUser.dispose();
    _joinPass.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    setState(() {
      _loading = true;
      _msg = '';
    });
    try {
      final res = await http.post(
        Uri.parse('${AppConfig.baseUrl}/v1/iam/tenants/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'company': _company.text.trim(),
          'adminUsername': _admin.text.trim(),
          'password': _password.text,
        }),
      );
      final body = jsonDecode(res.body);
      setState(() => _msg = res.statusCode == 200
          ? '注册成功 tenant=${body['data']?['tenant']?['id']}'
          : '失败: ${body['msg'] ?? res.body}');
    } catch (e) {
      setState(() => _msg = '错误: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _accept() async {
    setState(() {
      _loading = true;
      _msg = '';
    });
    try {
      final res = await http.post(
        Uri.parse('${AppConfig.baseUrl}/v1/iam/invites/accept'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'code': _invite.text.trim(),
          'username': _joinUser.text.trim(),
          'password': _joinPass.text,
        }),
      );
      final body = jsonDecode(res.body);
      setState(() => _msg = res.statusCode == 200
          ? '加入成功 user=${body['data']?['user']?['username']}'
          : '失败: ${body['msg'] ?? res.body}');
    } catch (e) {
      setState(() => _msg = '错误: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('开通 Nexa 企业'),
        bottom: TabBar(
          controller: _tabs,
          tabs: const [Tab(text: '注册企业'), Tab(text: '邀请码加入')],
        ),
      ),
      body: TabBarView(
        controller: _tabs,
        children: [
          ListView(
            padding: const EdgeInsets.all(16),
            children: [
              TextField(controller: _company, decoration: const InputDecoration(labelText: '企业名称')),
              TextField(controller: _admin, decoration: const InputDecoration(labelText: '管理员账号')),
              TextField(controller: _password, decoration: const InputDecoration(labelText: '密码'), obscureText: true),
              const SizedBox(height: 16),
              FilledButton(onPressed: _loading ? null : _register, child: const Text('注册并开通')),
              if (_msg.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 12), child: Text(_msg)),
            ],
          ),
          ListView(
            padding: const EdgeInsets.all(16),
            children: [
              TextField(controller: _invite, decoration: const InputDecoration(labelText: '邀请码')),
              TextField(controller: _joinUser, decoration: const InputDecoration(labelText: '用户名')),
              TextField(controller: _joinPass, decoration: const InputDecoration(labelText: '密码'), obscureText: true),
              const SizedBox(height: 16),
              FilledButton(onPressed: _loading ? null : _accept, child: const Text('加入企业')),
              if (_msg.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 12), child: Text(_msg)),
            ],
          ),
        ],
      ),
    );
  }
}
""",
    encoding="utf-8",
)

# docs
status = Path("docs/STATUS.md")
st = status.read_text(encoding="utf-8")
st = st.replace(
    "2. 审批/待办状态机与 IM 增强",
    "2. ~~审批状态机~~ ✅ history/cancel/reopen；todo complete（IM 仍可增强）",
)
st = st.replace(
    "6. App **开通向导 UI**",
    "6. ~~App 开通向导 UI~~ ✅ tenant_onboarding_page.dart scaffold",
)
if "history/cancel/reopen" not in st:
    st = st.rstrip() + "\n- BPM history/cancel/reopen + todo complete + onboarding page\n"
status.write_text(st, encoding="utf-8")

goal = Path("docs/GOAL.md")
gt = goal.read_text(encoding="utf-8")
if "cancel/reopen" not in gt:
    goal.write_text(
        gt.rstrip()
        + "\n| 2026-07-20 | BPM 状态机 history/cancel/reopen；todo complete；开通向导页；密码 x bypass 需环境变量 |\n",
        encoding="utf-8",
    )

cl = Path("CHANGELOG.md")
ct = cl.read_text(encoding="utf-8")
if "state machine" not in ct:
    ct = ct.replace(
        "### Added\n",
        "### Added\n\n"
        "- BPM task state machine with history (approve/reject/cancel/reopen)\n"
        "- Business todo complete API\n"
        "- Mobile tenant onboarding page scaffold\n"
        "- IAM smoke password bypass gated by env\n",
    )
    cl.write_text(ct, encoding="utf-8")

api = Path("docs/api/README.md")
at = api.read_text(encoding="utf-8")
if "tasks/cancel" not in at:
    at += (
        "\n## BPM 状态机\n\n"
        "| Method | Path |\n|--------|------|\n"
        "| POST | `/v1/bpm/tasks/start` |\n"
        "| POST | `/v1/bpm/tasks/approve` |\n"
        "| POST | `/v1/bpm/tasks/cancel` |\n"
        "| POST | `/v1/bpm/tasks/reopen` |\n"
        "| GET | `/v1/bpm/tasks/get?id=` |\n"
        "| POST | `/v1/business/todos/complete` |\n"
    )
    api.write_text(at, encoding="utf-8")

print("files written")
