import 'package:flutter/material.dart';
import 'services/api_service.dart';

class TestApiPage extends StatefulWidget {
  const TestApiPage({super.key});

  @override
  State<TestApiPage> createState() => _TestApiPageState();
}

class _TestApiPageState extends State<TestApiPage> {
  String _result = '';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('API测试'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ElevatedButton(
              onPressed: _testLogin,
              child: const Text('测试登录'),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _testRegister,
              child: const Text('测试注册'),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _testSendSmsCode,
              child: const Text('测试发送验证码'),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _testResetPassword,
              child: const Text('测试重置密码'),
            ),
            const SizedBox(height: 32),
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SingleChildScrollView(
                  child: Text(
                    _result,
                    style: const TextStyle(fontFamily: 'monospace'),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _addResult(String message) {
    setState(() {
      _result += '${DateTime.now()}: $message\n';
    });
  }

  Future<void> _testLogin() async {
    try {
      _addResult('开始测试登录...');
      final result = await ApiService.login(
        phoneNumber: '18905526118',
        password: '123456',
      );
      _addResult('登录成功: ${result.userID}');
      _addResult('AccessToken: ${result.accessToken}');
    } catch (e) {
      _addResult('登录失败: $e');
    }
  }

  Future<void> _testRegister() async {
    try {
      _addResult('开始测试注册...');
      final result = await ApiService.register(
        nickname: '测试用户',
        password: '123456',
        phoneNumber: '18905526119',
        verificationCode: '666666',
      );
      _addResult('注册成功: ${result.userID}');
      _addResult('AccessToken: ${result.accessToken}');
    } catch (e) {
      _addResult('注册失败: $e');
    }
  }

  Future<void> _testSendSmsCode() async {
    try {
      _addResult('开始测试发送验证码...');
      await ApiService.sendVerificationCode(
        phoneNumber: '18905526118',
      );
      _addResult('验证码发送成功');
    } catch (e) {
      _addResult('验证码发送失败: $e');
    }
  }

  Future<void> _testResetPassword() async {
    try {
      _addResult('开始测试重置密码...');
      await ApiService.resetPassword(
        mobile: '18905526118',
        code: '666666',
        password: '123456',
      );
      _addResult('密码重置成功');
    } catch (e) {
      _addResult('密码重置失败: $e');
    }
  }
} 