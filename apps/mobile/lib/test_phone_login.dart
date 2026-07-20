import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import '../services/api_service.dart';
import 'dart:developer' as developer;

class TestPhoneLogin extends StatefulWidget {
  const TestPhoneLogin({super.key});

  @override
  State<TestPhoneLogin> createState() => _TestPhoneLoginState();
}

class _TestPhoneLoginState extends State<TestPhoneLogin> {
  String _result = '';
  bool _isLoading = false;

  void _addResult(String message) {
    setState(() {
      _result += '${DateTime.now()}: $message\n';
    });
  }

  Future<void> _testPasswordLogin() async {
    try {
      setState(() {
        _isLoading = true;
      });
      
      _addResult('开始测试手机号密码登录...');
      final result = await ApiService.login(
        phoneNumber: '18905526118',
        password: '123456',
      );
      _addResult('密码登录成功: ${result.userID}');
      _addResult('AccessToken: ${result.accessToken}');
    } catch (e) {
      _addResult('密码登录失败: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _testVerificationCodeLogin() async {
    try {
      setState(() {
        _isLoading = true;
      });
      
      _addResult('开始测试手机号验证码登录...');
      _addResult('注意：验证码登录时不传递password字段');
      final result = await ApiService.login(
        phoneNumber: '18905526118',
        verificationCode: '666666',
      );
      _addResult('验证码登录成功: ${result.userID}');
      _addResult('AccessToken: ${result.accessToken}');
    } catch (e) {
      _addResult('验证码登录失败: $e');
      _addResult('错误详情: ${e.toString()}');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _testCheckUserExists() async {
    try {
      setState(() {
        _isLoading = true;
      });
      
      _addResult('开始测试检查用户是否存在...');
      final exists = await ApiService.checkUserExists(
        phoneNumber: '18905526118',
      );
      _addResult('用户存在: $exists');
    } catch (e) {
      _addResult('检查用户失败: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _testSendVerificationCode() async {
    try {
      setState(() {
        _isLoading = true;
      });
      
      _addResult('开始测试发送验证码...');
      await ApiService.sendVerificationCode(
        phoneNumber: '18905526118',
      );
      _addResult('验证码发送成功');
    } catch (e) {
      _addResult('验证码发送失败: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('手机登录测试'),
        backgroundColor: Colors.blue,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _testPasswordLogin,
                    child: const Text('测试密码登录'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _testVerificationCodeLogin,
                    child: const Text('测试验证码登录'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _testCheckUserExists,
                    child: const Text('检查用户是否存在'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _testSendVerificationCode,
                    child: const Text('发送验证码'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              ),
            const SizedBox(height: 16),
            Expanded(
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SingleChildScrollView(
                  child: Text(
                    _result.isEmpty ? '测试结果将显示在这里...' : _result,
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
} 