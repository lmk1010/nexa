import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:provider/provider.dart';
import '../config/app_config.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/theme_service.dart';
import 'register_phone_page.dart';
import 'company_selection_page.dart';
import 'tenant_selection_page.dart';
import 'dart:developer' as developer;
import 'dart:async';
import 'register_email_page.dart';

class EmailLoginPage extends StatefulWidget {
  const EmailLoginPage({super.key});

  @override
  State<EmailLoginPage> createState() => _EmailLoginPageState();
}

class _EmailLoginPageState extends State<EmailLoginPage>
    with TickerProviderStateMixin {
  late AnimationController _fadeController;
  late AnimationController _slideController;
  late AnimationController _formController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _formAnimation;

  late ThemeService _themeService;

  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _verificationCodeController = TextEditingController();

  bool _isLoading = false;
  bool _isCountingDown = false;
  int _countdown = 60;
  bool _isPasswordLogin = true; // true为密码登录，false为验证码登录

  @override
  void initState() {
    super.initState();

    _themeService = Provider.of<ThemeService>(context, listen: false);

    // 设置沉浸式状态栏
    SystemChrome.setSystemUIOverlayStyle(_themeService.systemUiStyle);

    // 设置系统UI模式为状态栏和导航栏都可见
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom],
    );

    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _slideController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    );
    _formController = AnimationController(
      duration: const Duration(milliseconds: 400),
      vsync: this,
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _fadeController, curve: Curves.easeInOut),
    );

    _slideAnimation =
        Tween<Offset>(begin: const Offset(0, 0.3), end: Offset.zero).animate(
          CurvedAnimation(parent: _slideController, curve: Curves.easeOutCubic),
        );

    _formAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _formController, curve: Curves.elasticOut),
    );

    _fadeController.forward();
    _slideController.forward();
    _formController.forward();
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _verificationCodeController.dispose();

    _fadeController.dispose();
    _slideController.dispose();
    _formController.dispose();

    // 恢复系统UI设置
    SystemChrome.setSystemUIOverlayStyle(_themeService.restoreSystemUiStyle);

    super.dispose();
  }

  void _startCountdown() {
    setState(() {
      _isCountingDown = true;
      _countdown = 60;
    });

    Timer.periodic(const Duration(seconds: 1), (timer) {
      if (mounted) {
        setState(() {
          _countdown--;
        });

        if (_countdown <= 0) {
          setState(() {
            _isCountingDown = false;
          });
          timer.cancel();
        }
      } else {
        timer.cancel();
      }
    });
  }

  Future<void> _sendVerificationCode() async {
    if (_emailController.text.isEmpty) {
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('请先输入邮箱'),
          backgroundColor: theme['error'],
        ),
      );
      return;
    }

    if (!RegExp(
      r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$',
    ).hasMatch(_emailController.text)) {
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('请输入正确的邮箱地址'),
          backgroundColor: theme['error'],
        ),
      );
      return;
    }

    try {
      // 调用API发送验证码
      await ApiService.sendVerificationCode(email: _emailController.text);

      _startCountdown();

      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('验证码已发送，固定验证码：666666'),
          backgroundColor: theme['success'],
        ),
      );
    } catch (e) {
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('发送失败: $e'), backgroundColor: theme['error']),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;
    final screenWidth = MediaQuery.of(context).size.width;
    final topPadding = MediaQuery.of(context).padding.top;
    final bottomPadding = MediaQuery.of(context).padding.bottom;

    return Consumer<ThemeService>(
      builder: (context, themeService, child) {
        final theme = themeService.currentTheme;

        return Scaffold(
          resizeToAvoidBottomInset: true,
          body: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  theme['gradientStart']!,
                  theme['gradientCenter']!,
                  theme['gradientEnd']!,
                ],
              ),
            ),
            child: SafeArea(
              child: FadeTransition(
                opacity: _fadeAnimation,
                child: SlideTransition(
                  position: _slideAnimation,
                  child: Column(
                    children: [
                      Expanded(
                        child: SingleChildScrollView(
                          padding: EdgeInsets.only(
                            bottom:
                                MediaQuery.of(context).viewInsets.bottom + 20,
                          ),
                          child: ConstrainedBox(
                            constraints: BoxConstraints(
                              minHeight:
                                  screenHeight -
                                  topPadding -
                                  bottomPadding -
                                  60,
                            ),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 24,
                              ),
                              child: Column(
                                children: [
                                  // 返回按钮和标题
                                  Padding(
                                    padding: const EdgeInsets.only(
                                      top: 20,
                                      bottom: 40,
                                    ),
                                    child: Row(
                                      children: [
                                        GestureDetector(
                                          onTap: () => Navigator.pop(context),
                                          child: Container(
                                            padding: const EdgeInsets.all(8),
                                            decoration: BoxDecoration(
                                              color: theme['surface']!
                                                  .withOpacity(0.9),
                                              borderRadius:
                                                  BorderRadius.circular(8),
                                            ),
                                            child: Icon(
                                              Icons.arrow_back_ios,
                                              size: 20,
                                              color: theme['textPrimary'],
                                            ),
                                          ),
                                        ),
                                        const SizedBox(width: 16),
                                        Expanded(
                                          child: Text(
                                            '邮箱登录',
                                            style: TextStyle(
                                              fontSize: 24,
                                              fontWeight: FontWeight.bold,
                                              color: theme['textPrimary'],
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),

                                  // 登录方式切换
                                  _buildLoginTypeSwitch(theme),
                                  const SizedBox(height: 24),

                                  // 表单
                                  ScaleTransition(
                                    scale: _formAnimation,
                                    child: Form(
                                      key: _formKey,
                                      child: Column(
                                        children: [
                                          // 邮箱输入框
                                          _buildEmailField(theme),
                                          const SizedBox(height: 20),

                                          // 密码或验证码输入框
                                          _isPasswordLogin
                                              ? _buildPasswordField(theme)
                                              : _buildVerificationCodeField(
                                                  theme,
                                                ),
                                          const SizedBox(height: 32),

                                          // 登录按钮
                                          _buildLoginButton(theme),
                                          const SizedBox(height: 24),

                                          // 其他登录方式
                                          _buildOtherLoginOptions(theme),
                                        ],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildLoginTypeSwitch(Map<String, Color> theme) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: theme['surface']!.withOpacity(0.9),
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: GestureDetector(
              onTap: () {
                setState(() {
                  _isPasswordLogin = true;
                });
              },
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 12),
                decoration: BoxDecoration(
                  color: _isPasswordLogin
                      ? theme['primary']!.withOpacity(0.1)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  '密码登录',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _isPasswordLogin
                        ? theme['primary']
                        : theme['textSecondary'],
                  ),
                ),
              ),
            ),
          ),
          Expanded(
            child: GestureDetector(
              onTap: () {
                setState(() {
                  _isPasswordLogin = false;
                });
              },
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 12),
                decoration: BoxDecoration(
                  color: !_isPasswordLogin
                      ? theme['info']!.withOpacity(0.1)
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  '验证码登录',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: !_isPasswordLogin
                        ? theme['info']
                        : theme['textSecondary'],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmailField(Map<String, Color> theme) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: TextFormField(
        controller: _emailController,
        keyboardType: TextInputType.emailAddress,
        decoration: InputDecoration(
          labelText: '邮箱地址',
          hintText: '请输入您的邮箱地址',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['info']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.email_outlined, size: 18, color: theme['info']),
          ),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: theme['info']!, width: 1.5),
          ),
          filled: true,
          fillColor: theme['surface']!.withOpacity(0.9),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 16,
          ),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请输入邮箱地址';
          }
          if (!RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(value)) {
            return '请输入正确的邮箱地址';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildPasswordField(Map<String, Color> theme) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: TextFormField(
        controller: _passwordController,
        obscureText: true,
        decoration: InputDecoration(
          labelText: '密码',
          hintText: '请输入您的密码',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.lock_outline, size: 18, color: theme['primary']),
          ),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: theme['primary']!, width: 1.5),
          ),
          filled: true,
          fillColor: theme['surface']!.withOpacity(0.9),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 16,
          ),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请输入密码';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildVerificationCodeField(Map<String, Color> theme) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: TextFormField(
        controller: _verificationCodeController,
        keyboardType: TextInputType.number,
        decoration: InputDecoration(
          labelText: '验证码',
          hintText: '请输入验证码',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['info']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(
              Icons.security_outlined,
              size: 18,
              color: theme['info'],
            ),
          ),
          suffixIcon: GestureDetector(
            onTap: _isCountingDown ? null : _sendVerificationCode,
            child: Container(
              margin: const EdgeInsets.all(8),
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: _isCountingDown
                    ? theme['textTertiary']!.withOpacity(0.1)
                    : theme['info']!.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                _isCountingDown ? '${_countdown}s' : '发送',
                style: TextStyle(
                  fontSize: 12,
                  color: _isCountingDown
                      ? theme['textTertiary']
                      : theme['info'],
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: theme['info']!, width: 1.5),
          ),
          filled: true,
          fillColor: theme['surface']!.withOpacity(0.9),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 16,
          ),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请输入验证码';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildLoginButton(Map<String, Color> theme) {
    return Container(
      width: double.infinity,
      height: 48,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: LinearGradient(
          colors: [theme['info']!, theme['info']!.withOpacity(0.8)],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        boxShadow: [
          BoxShadow(
            color: theme['info']!.withOpacity(0.3),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: ElevatedButton(
        onPressed: _isLoading ? null : _login,
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.transparent,
          foregroundColor: theme['onPrimary'],
          elevation: 0,
          shadowColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
        child: _isLoading
            ? SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(
                    theme['onPrimary']!,
                  ),
                ),
              )
            : const Text(
                '登录',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
      ),
    );
  }

  Widget _buildOtherLoginOptions(Map<String, Color> theme) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: Divider(color: theme['textTertiary']!.withOpacity(0.3)),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(
                '其他登录方式',
                style: TextStyle(fontSize: 14, color: theme['textTertiary']),
              ),
            ),
            Expanded(
              child: Divider(color: theme['textTertiary']!.withOpacity(0.3)),
            ),
          ],
        ),
        const SizedBox(height: 20),

        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            _buildLoginOption(
              icon: Icons.person_outline,
              label: '账号登录',
              color: theme['primary']!,
              onTap: () => Navigator.pop(context),
            ),
            _buildLoginOption(
              icon: Icons.phone_outlined,
              label: '手机登录',
              color: theme['success']!,
              onTap: () => Navigator.pop(context),
            ),
            _buildLoginOption(
              icon: Icons.wechat,
              label: '微信登录',
              color: const Color(0xFF07C160),
              onTap: () {
                // 微信登录逻辑
              },
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildLoginOption({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Container(
            width: 50,
            height: 50,
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color, size: 24),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              color: color,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  void _login() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      if (AppConfig.isProduction) {
        if (!_isPasswordLogin) {
          throw ApiException('生产环境暂不支持邮箱验证码登录');
        }
        final preLoginResult = await ApiService.preLogin(
          email: _emailController.text,
          password: _passwordController.text,
        );
        if (mounted) {
          Navigator.pushReplacement(
            context,
            MaterialPageRoute(
              builder: (context) =>
                  TenantSelectionPage(preLoginResult: preLoginResult),
            ),
          );
        }
        return;
      }

      LoginCertificate loginCert;

      if (_isPasswordLogin) {
        loginCert = await ApiService.login(
          email: _emailController.text,
          password: _passwordController.text,
        );
      } else {
        loginCert = await ApiService.login(
          email: _emailController.text,
          verificationCode: _verificationCodeController.text,
        );
      }

      if (mounted) {
        final authService = Provider.of<AuthService>(context, listen: false);
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final messenger = ScaffoldMessenger.of(context);

        // 保存登录凭证
        await authService.saveLoginCertificate(loginCert.toJson());
        if (!mounted) return;

        if (kDebugMode) {
          print('邮箱登录成功，用户ID: ${loginCert.userID}');
          print('登录凭证已保存到本地存储');
        }

        final theme = themeService.currentTheme;
        messenger.showSnackBar(
          SnackBar(
            content: Text('登录成功！用户ID: ${loginCert.userID}'),
            backgroundColor: theme['success'],
            duration: const Duration(seconds: 2),
          ),
        );

        // 延迟一下显示成功消息，然后跳转到聊天主界面
        await Future.delayed(const Duration(seconds: 1));

        // 跳转到公司选择页面
        if (mounted) {
          Navigator.pushReplacement(
            context,
            MaterialPageRoute(
              builder: (context) =>
                  const CompanySelectionPage(isLoginMode: true),
            ),
          );
        }
      }
    } catch (e) {
      developer.log('邮箱登录失败: $e', name: 'EmailLoginPage');

      if (mounted) {
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;

        // 检查是否是用户未注册异常
        if (e.toString().contains('UserNotRegisteredException') ||
            e.toString().contains('用户未注册')) {
          // 显示用户未注册提示
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: const Text('用户未注册，正在跳转到注册页面...'),
              backgroundColor: theme['warning'],
              duration: const Duration(seconds: 2),
            ),
          );

          // 延迟一下显示提示，然后跳转到注册页面
          await Future.delayed(const Duration(seconds: 1));

          if (mounted) {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) =>
                    RegisterEmailPage(email: _emailController.text),
              ),
            );
          }
        } else {
          // 其他错误显示普通错误提示
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('登录失败: $e'),
              backgroundColor: theme['error'],
            ),
          );
        }
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _showRegisterDialog() {
    final themeService = Provider.of<ThemeService>(context, listen: false);
    final theme = themeService.currentTheme;

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: Text(
            '账号未注册',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: theme['textPrimary'],
            ),
          ),
          content: Text(
            '该邮箱尚未注册，是否前往注册页面设置密码？',
            style: TextStyle(fontSize: 16, color: theme['textSecondary']),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text(
                '取消',
                style: TextStyle(color: theme['textSecondary'], fontSize: 16),
              ),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                // 跳转到注册页面
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => RegisterPhonePage()),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: theme['primary'],
                foregroundColor: theme['onPrimary'],
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text(
                '去注册',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
              ),
            ),
          ],
        );
      },
    );
  }
}
