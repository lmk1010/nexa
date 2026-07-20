import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../config/app_config.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/storage_service.dart';
import '../services/theme_service.dart';
import '../widgets/kyx_design.dart';
import 'chat_main_page.dart';
import 'phone_login_page.dart';
import 'email_login_page.dart';
import 'tenant_selection_page.dart';

/// 登录类型枚举
/// 定义支持的登录方式
enum LoginType {
  phone, // 手机号登录
  email, // 邮箱登录
  account, // 账号登录
}

/// 通用登录页面
/// 支持多种登录方式：手机号、邮箱、用户名
/// 提供智能输入检测和动态UI适配
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

/// 登录页面状态管理类
/// 管理表单状态、动画效果和登录逻辑
class _LoginPageState extends State<LoginPage> with TickerProviderStateMixin {
  /// 表单验证键
  final _formKey = GlobalKey<FormState>();

  /// 账号输入控制器
  final _accountController = TextEditingController();

  /// 密码输入控制器
  final _passwordController = TextEditingController();

  final StorageService _storageService = StorageService();

  /// 加载状态标志
  bool _isLoading = false;

  bool _isDingTalkLoading = false;
  String _dingTalkStatusText = '';
  int _dingTalkLoginAttempt = 0;

  /// 密码可见性标志
  bool _obscurePassword = true;

  bool _rememberPassword = true;
  List<LoginFormCredential> _credentialHistory = const [];

  // 动画控制器
  late AnimationController _fadeController; // 淡入动画控制器
  late AnimationController _slideController; // 滑动动画控制器

  // 动画对象
  late Animation<double> _fadeAnimation; // 淡入动画
  late Animation<Offset> _slideAnimation; // 滑动动画

  @override
  void initState() {
    super.initState();

    // 设置系统状态栏样式，保留状态栏显示
    final themeService = Provider.of<ThemeService>(context, listen: false);
    SystemChrome.setSystemUIOverlayStyle(
      themeService.isDarkMode
          ? const SystemUiOverlayStyle(
              statusBarColor: Colors.transparent,
              statusBarIconBrightness: Brightness.light,
              statusBarBrightness: Brightness.dark,
              systemNavigationBarColor: Colors.transparent,
              systemNavigationBarIconBrightness: Brightness.light,
            )
          : const SystemUiOverlayStyle(
              statusBarColor: Colors.transparent,
              statusBarIconBrightness: Brightness.dark,
              statusBarBrightness: Brightness.light,
              systemNavigationBarColor: Colors.transparent,
              systemNavigationBarIconBrightness: Brightness.dark,
            ),
    );

    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom],
    );

    // 初始化动画控制器
    _initAnimationControllers();

    // 启动动画
    _fadeController.forward();
    _slideController.forward();
    _loadSavedLoginCredentials();
  }

  /// 初始化动画控制器
  /// 设置动画持续时间和曲线
  void _initAnimationControllers() {
    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _slideController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _fadeController, curve: Curves.easeInOut),
    );

    _slideAnimation =
        Tween<Offset>(begin: const Offset(0, 0.3), end: Offset.zero).animate(
          CurvedAnimation(parent: _slideController, curve: Curves.easeOutCubic),
        );
  }

  Future<void> _loadSavedLoginCredentials() async {
    final history = await _storageService.getLoginFormCredentialHistory();
    if (!mounted) return;

    setState(() {
      _credentialHistory = history;
      if (history.isNotEmpty &&
          _accountController.text.trim().isEmpty &&
          _passwordController.text.isEmpty) {
        _accountController.text = history.first.account;
        _passwordController.text = history.first.password;
      }
    });
  }

  @override
  void dispose() {
    _dingTalkLoginAttempt++;
    _accountController.dispose();
    _passwordController.dispose();

    _fadeController.dispose();
    _slideController.dispose();

    // 恢复系统UI设置
    final themeService = Provider.of<ThemeService>(context, listen: false);
    themeService.refreshSystemUiStyle();

    super.dispose();
  }

  /// 检测输入类型
  /// [input] 用户输入的文本
  /// 根据输入内容智能判断登录类型
  LoginType _detectLoginType(String input) {
    if (input.isEmpty) return LoginType.account;

    // 检测手机号（11位数字，以1开头）
    if (RegExp(r'^1[3-9]\d{9}$').hasMatch(input)) {
      return LoginType.phone;
    }

    // 默认为 OA 用户名；邮箱不作为独立登录入口
    return LoginType.account;
  }

  /// 获取输入框标签文本
  /// 根据当前检测的登录类型返回对应的标签
  String _getInputLabel() {
    final loginType = _detectLoginType(_accountController.text);
    switch (loginType) {
      case LoginType.phone:
        return '手机号';
      case LoginType.email:
        return '账号';
      case LoginType.account:
        return '账号或手机号';
    }
  }

  /// 获取输入框提示文本
  /// 根据当前检测的登录类型返回对应的提示
  String _getInputHint() {
    final loginType = _detectLoginType(_accountController.text);
    switch (loginType) {
      case LoginType.phone:
        return '请输入手机号';
      case LoginType.email:
        return '请输入用户名或手机号';
      case LoginType.account:
        return '请输入用户名或手机号';
    }
  }

  /// 获取输入框图标
  /// 根据当前检测的登录类型返回对应的图标
  IconData _getInputIcon() {
    final loginType = _detectLoginType(_accountController.text);
    switch (loginType) {
      case LoginType.phone:
        return Icons.phone_outlined;
      case LoginType.email:
        return Icons.person_outline;
      case LoginType.account:
        return Icons.person_outline;
    }
  }

  /// 获取输入框主题色
  /// [theme] 当前主题配色
  /// 根据当前检测的登录类型返回对应的主题色
  Color _getInputColor(Map<String, Color> theme) {
    final loginType = _detectLoginType(_accountController.text);
    switch (loginType) {
      case LoginType.phone:
        return theme['success']!;
      case LoginType.email:
        return theme['info']!;
      case LoginType.account:
        return theme['primary']!;
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;
    final topPadding = MediaQuery.of(context).padding.top;
    final bottomPadding = MediaQuery.of(context).padding.bottom;
    return Consumer<ThemeService>(
      builder: (context, themeService, child) {
        const overlayStyle = SystemUiOverlayStyle(
          statusBarColor: KyXColors.bg,
          statusBarIconBrightness: Brightness.dark,
          statusBarBrightness: Brightness.light,
          systemNavigationBarColor: KyXColors.bg,
          systemNavigationBarIconBrightness: Brightness.dark,
        );

        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: overlayStyle,
          child: Scaffold(
            backgroundColor: KyXColors.bg,
            resizeToAvoidBottomInset: true,
            body: SafeArea(
              child: FadeTransition(
                opacity: _fadeAnimation,
                child: SlideTransition(
                  position: _slideAnimation,
                  child: SingleChildScrollView(
                    padding: EdgeInsets.fromLTRB(
                      18,
                      8,
                      18,
                      MediaQuery.of(context).viewInsets.bottom + 16,
                    ),
                    child: ConstrainedBox(
                      constraints: BoxConstraints(
                        minHeight:
                            screenHeight - topPadding - bottomPadding - 32,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          _buildBusinessHeader(),
                          const SizedBox(height: 28),
                          _buildBusinessLogo(),
                          const SizedBox(height: 14),
                          _buildBusinessWelcomeText(),
                          const SizedBox(height: 26),
                          _buildBusinessLoginForm(),
                          const SizedBox(height: 34),
                          _buildBusinessFooter(),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildHeader(Map<String, Color> theme) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        const SizedBox(width: 40), // 占位，保持居中
        Text(
          AppConfig.appName,
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: theme['textPrimary'],
            letterSpacing: 1,
          ),
        ),
        // 主题切换按钮
        GestureDetector(
          onTap: () {
            final themeService = Provider.of<ThemeService>(
              context,
              listen: false,
            );
            themeService.toggleTheme();
          },
          child: Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: theme['surface']!.withOpacity(0.2),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: theme['outline']!.withOpacity(0.3),
                width: 1,
              ),
            ),
            child: Consumer<ThemeService>(
              builder: (context, themeService, child) {
                return Icon(
                  themeService.isDarkMode ? Icons.light_mode : Icons.dark_mode,
                  color: theme['textPrimary'],
                  size: 20,
                );
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildLogo(Map<String, Color> theme) {
    return Container(
      width: 72,
      height: 72,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [theme['logoGradientStart']!, theme['logoGradientEnd']!],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.only(top: 2),
        child: Icon(
          Icons.chat_bubble_outline,
          color: theme['onPrimary'],
          size: 36,
        ),
      ),
    );
  }

  Widget _buildWelcomeText(Map<String, Color> theme) {
    return Column(
      children: [
        Text(
          '欢迎回来',
          style: TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.w700,
            color: theme['textPrimary'],
            letterSpacing: 0.6,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          '新一代工作平台',
          style: TextStyle(
            fontSize: 15,
            color: theme['textSecondary'],
            fontWeight: FontWeight.w400,
            letterSpacing: 0.3,
          ),
        ),
      ],
    );
  }

  Widget _buildLoginForm(Map<String, Color> theme) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SizedBox(height: 28),
          _buildInputFields(theme),
          const SizedBox(height: 28),
          _buildLoginButton(theme),
          const SizedBox(height: 24),
          _buildLoginOptions(theme),
        ],
      ),
    );
  }

  Widget _buildInputFields(Map<String, Color> theme) {
    return Column(
      children: [
        _buildAccountField(theme),
        const SizedBox(height: 20),
        _buildPasswordField(theme),
      ],
    );
  }

  Widget _buildAccountField(Map<String, Color> theme) {
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
        controller: _accountController,
        onChanged: (value) {
          // 当输入内容改变时，触发UI更新以显示正确的图标和颜色
          setState(() {});
        },
        decoration: InputDecoration(
          labelText: _getInputLabel(),
          hintText: _getInputHint(),
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: _getInputColor(theme).withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(
              _getInputIcon(),
              size: 18,
              color: _getInputColor(theme),
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
            borderSide: BorderSide(color: _getInputColor(theme), width: 1.5),
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
            return '请输入${_getInputLabel()}';
          }

          final loginType = _detectLoginType(value);
          switch (loginType) {
            case LoginType.phone:
              if (!RegExp(r'^1[3-9]\d{9}$').hasMatch(value)) {
                return '请输入正确的手机号';
              }
              break;
            case LoginType.email:
              if (!RegExp(
                r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$',
              ).hasMatch(value)) {
                return '请输入正确的邮箱地址';
              }
              break;
            case LoginType.account:
              // 普通账户不需要特殊验证
              break;
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
        obscureText: _obscurePassword,
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
          suffixIcon: GestureDetector(
            onTap: () {
              setState(() {
                _obscurePassword = !_obscurePassword;
              });
            },
            child: Container(
              margin: const EdgeInsets.all(8),
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: theme['primary']!.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(
                _obscurePassword ? Icons.visibility_off : Icons.visibility,
                size: 18,
                color: theme['primary'],
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

  Widget _buildLoginButton(Map<String, Color> theme) {
    return Container(
      width: double.infinity,
      height: 48,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: LinearGradient(
          colors: [theme['primary']!, theme['primaryLight']!],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        boxShadow: [
          BoxShadow(
            color: theme['primary']!.withOpacity(0.3),
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

  Widget _buildLoginOptions(Map<String, Color> theme) {
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
              icon: Icons.phone_outlined,
              label: '手机登录',
              color: theme['success']!,
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const PhoneLoginPage(),
                  ),
                );
              },
            ),
            _buildLoginOption(
              icon: Icons.email_outlined,
              label: '邮箱登录',
              color: theme['info']!,
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const EmailLoginPage(),
                  ),
                );
              },
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

  Widget _buildBusinessHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(AppConfig.appName, style: KyXText.title),
        Consumer<ThemeService>(
          builder: (context, themeService, child) {
            return IconButton(
              onPressed: themeService.toggleTheme,
              icon: Icon(
                themeService.isDarkMode
                    ? Icons.light_mode_outlined
                    : Icons.dark_mode_outlined,
              ),
              color: KyXColors.textSecondary,
              iconSize: 20,
              constraints: const BoxConstraints.tightFor(width: 36, height: 36),
              padding: EdgeInsets.zero,
              tooltip: '切换主题',
              style: IconButton.styleFrom(
                backgroundColor: KyXColors.surface,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                  side: const BorderSide(color: KyXColors.line),
                ),
              ),
            );
          },
        ),
      ],
    );
  }

  Widget _buildBusinessLogo() {
    return Align(
      alignment: Alignment.centerLeft,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(9),
        child: Image.asset(
          'assets/images/app_logo.png',
          width: 48,
          height: 48,
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => Container(
            width: 48,
            height: 48,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: KyXColors.primary,
              borderRadius: BorderRadius.circular(9),
            ),
            child: const Icon(
              Icons.business_center_outlined,
              color: Colors.white,
              size: 24,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBusinessWelcomeText() {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('欢迎回来', style: KyXText.pageTitle),
        SizedBox(height: 8),
        Text('使用 OA 账号登录，进入移动工作台和企业 IM', style: KyXText.secondary),
      ],
    );
  }

  Widget _buildBusinessLoginForm() {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildBusinessAccountField(),
          const SizedBox(height: 12),
          _buildBusinessPasswordField(),
          const SizedBox(height: 9),
          _buildRememberPasswordRow(),
          const SizedBox(height: 16),
          _buildBusinessLoginButton(),
          const SizedBox(height: 12),
          _buildBusinessLoginOptions(),
        ],
      ),
    );
  }

  Widget _buildBusinessFieldLabel(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 7),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: KyXColors.text,
        ),
      ),
    );
  }

  Widget _buildBusinessAccountField() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildBusinessFieldLabel(_getInputLabel()),
        TextFormField(
          controller: _accountController,
          onChanged: (_) => setState(() {}),
          decoration: kyxInputDecoration(
            hintText: _getInputHint(),
            prefixIcon: Icon(
              _getInputIcon(),
              size: 20,
              color: KyXColors.textSecondary,
            ),
            suffixIcon: _credentialHistory.isEmpty
                ? null
                : IconButton(
                    onPressed: _showCredentialHistorySheet,
                    icon: const Icon(Icons.expand_more_outlined, size: 20),
                    color: KyXColors.textSecondary,
                    tooltip: '历史账号',
                  ),
          ),
          style: KyXText.body,
          validator: (value) {
            if (value == null || value.isEmpty) {
              return '请输入${_getInputLabel()}';
            }

            final loginType = _detectLoginType(value);
            switch (loginType) {
              case LoginType.phone:
                if (!RegExp(r'^1[3-9]\d{9}$').hasMatch(value)) {
                  return '请输入正确的手机号';
                }
                break;
              case LoginType.email:
                if (!RegExp(
                  r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$',
                ).hasMatch(value)) {
                  return '请输入正确的邮箱地址';
                }
                break;
              case LoginType.account:
                break;
            }
            return null;
          },
        ),
      ],
    );
  }

  Widget _buildRememberPasswordRow() {
    return Row(
      children: [
        Expanded(
          child: InkWell(
            onTap: () {
              setState(() {
                _rememberPassword = !_rememberPassword;
              });
            },
            borderRadius: BorderRadius.circular(6),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                children: [
                  Icon(
                    _rememberPassword
                        ? Icons.check_circle
                        : Icons.radio_button_unchecked,
                    size: 17,
                    color: _rememberPassword
                        ? KyXColors.primary
                        : KyXColors.textTertiary,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _rememberPassword ? '已记住账号密码' : '记住账号密码',
                    style: KyXText.secondary.copyWith(
                      color: _rememberPassword
                          ? KyXColors.textSecondary
                          : KyXColors.textTertiary,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        if (_credentialHistory.isNotEmpty) ...[
          const SizedBox(width: 8),
          InkWell(
            onTap: _showCredentialHistorySheet,
            borderRadius: BorderRadius.circular(6),
            child: const Padding(
              padding: EdgeInsets.symmetric(horizontal: 2, vertical: 4),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.history_outlined,
                    size: 16,
                    color: KyXColors.primary,
                  ),
                  SizedBox(width: 4),
                  Text(
                    '历史账号',
                    style: TextStyle(
                      color: KyXColors.primary,
                      fontSize: 13,
                      height: 1.3,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildBusinessPasswordField() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildBusinessFieldLabel('密码'),
        TextFormField(
          controller: _passwordController,
          obscureText: _obscurePassword,
          decoration: kyxInputDecoration(
            hintText: '请输入密码',
            prefixIcon: const Icon(
              Icons.lock_outline,
              size: 20,
              color: KyXColors.textSecondary,
            ),
            suffixIcon: IconButton(
              onPressed: () {
                setState(() {
                  _obscurePassword = !_obscurePassword;
                });
              },
              icon: Icon(
                _obscurePassword
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                size: 20,
              ),
              color: KyXColors.textSecondary,
              tooltip: _obscurePassword ? '显示密码' : '隐藏密码',
            ),
          ),
          style: KyXText.body,
          validator: (value) {
            if (value == null || value.isEmpty) {
              return '请输入密码';
            }
            return null;
          },
        ),
      ],
    );
  }

  Future<void> _showCredentialHistorySheet() async {
    if (_credentialHistory.isEmpty) return;

    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: KyXColors.surface,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (sheetContext) {
        return SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(0, 0, 0, 8),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Padding(
                  padding: EdgeInsets.fromLTRB(20, 0, 20, 12),
                  child: Text('选择历史账号', style: KyXText.title),
                ),
                const Divider(height: 1, color: KyXColors.lineSoft),
                Flexible(
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: _credentialHistory.length,
                    separatorBuilder: (_, __) => const Divider(
                      height: 1,
                      indent: 72,
                      color: KyXColors.lineSoft,
                    ),
                    itemBuilder: (context, index) {
                      final credential = _credentialHistory[index];
                      return _buildCredentialHistoryRow(
                        sheetContext,
                        credential,
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildCredentialHistoryRow(
    BuildContext sheetContext,
    LoginFormCredential credential,
  ) {
    final account = credential.account;
    return InkWell(
      onTap: () {
        Navigator.of(sheetContext).pop();
        _fillLoginCredential(credential);
      },
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 8, 12),
        child: Row(
          children: [
            KyXAvatar(text: account, size: 36, color: KyXColors.primary),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    account,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: KyXText.bodyStrong,
                  ),
                  const SizedBox(height: 3),
                  const Text('已保存密码', style: KyXText.caption),
                ],
              ),
            ),
            IconButton(
              onPressed: () async {
                await _removeLoginCredential(sheetContext, account);
              },
              icon: const Icon(Icons.close, size: 18),
              color: KyXColors.textTertiary,
              tooltip: '删除记录',
            ),
          ],
        ),
      ),
    );
  }

  void _fillLoginCredential(LoginFormCredential credential) {
    setState(() {
      _accountController.text = credential.account;
      _passwordController.text = credential.password;
      _rememberPassword = true;
    });
  }

  Future<void> _removeLoginCredential(
    BuildContext sheetContext,
    String account,
  ) async {
    final sheetNavigator = Navigator.of(sheetContext);
    await _storageService.removeLoginFormCredential(account);
    final history = await _storageService.getLoginFormCredentialHistory();
    if (!mounted) return;

    setState(() {
      _credentialHistory = history;
      if (_accountController.text.trim() == account) {
        _accountController.clear();
        _passwordController.clear();
      }
    });

    if (history.isEmpty && sheetNavigator.canPop()) {
      sheetNavigator.pop();
    }
  }

  Widget _buildBusinessLoginButton() {
    return SizedBox(
      height: 42,
      child: ElevatedButton(
        onPressed: _isLoading ? null : _login,
        style: kyxPrimaryButtonStyle(),
        child: _isLoading
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                ),
              )
            : const Text('登录'),
      ),
    );
  }

  Widget _buildBusinessLoginOptions() {
    return Column(
      children: [
        OutlinedButton.icon(
          onPressed: (_isLoading || _isDingTalkLoading)
              ? null
              : _startDingTalkLogin,
          icon: _isDingTalkLoading
              ? const SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.business_outlined, size: 17),
          label: const Text('钉钉登录'),
          style: _businessSecondaryLoginButtonStyle(),
        ),
        if (_dingTalkStatusText.isNotEmpty) ...[
          const SizedBox(height: 10),
          Text(
            _dingTalkStatusText,
            textAlign: TextAlign.center,
            style: KyXText.caption,
          ),
        ],
      ],
    );
  }

  ButtonStyle _businessSecondaryLoginButtonStyle() {
    return OutlinedButton.styleFrom(
      minimumSize: const Size.fromHeight(38),
      foregroundColor: KyXColors.text,
      side: const BorderSide(color: KyXColors.line),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      padding: const EdgeInsets.symmetric(horizontal: 8),
      textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
    );
  }

  Widget _buildBusinessFooter() {
    return const Padding(
      padding: EdgeInsets.only(top: 24, bottom: 4),
      child: Text(
        '账号由企业管理员统一开通，登录即表示同意企业账号和数据安全规范',
        textAlign: TextAlign.center,
        style: KyXText.caption,
      ),
    );
  }

  Future<void> _startDingTalkLogin() async {
    if (_isLoading || _isDingTalkLoading) return;

    final confirmed = await _showDingTalkLoginNotice();
    if (!mounted || !confirmed) return;

    final attempt = ++_dingTalkLoginAttempt;
    setState(() {
      _isDingTalkLoading = true;
      _dingTalkStatusText = '正在打开钉钉授权，授权后请手动切回 App';
    });

    try {
      final handoffId = _buildSocialHandoffId();
      final redirectUri = _buildSocialRedirectUri(
        ApiService.dingtalkSocialType,
        handoffId,
      );
      final authorizeUrl = await ApiService.socialAuthRedirect(
        type: ApiService.dingtalkSocialType,
        redirectUri: redirectUri,
      );
      if (!mounted || attempt != _dingTalkLoginAttempt) return;

      final dingtalkUri = Uri.parse(
        'dingtalk://dingtalkclient/page/link?url=${Uri.encodeComponent(authorizeUrl)}',
      );
      final opened = await launchUrl(
        dingtalkUri,
        mode: LaunchMode.externalApplication,
      );
      if (!opened) {
        throw ApiException('未检测到钉钉，请先安装钉钉后重试');
      }

      if (!mounted || attempt != _dingTalkLoginAttempt) return;
      setState(() {
        _dingTalkStatusText = '已打开钉钉。授权成功后请手动切回 OA App，系统会自动继续登录';
      });
      await _pollDingTalkHandoff(handoffId, attempt);
    } catch (e) {
      if (!mounted || attempt != _dingTalkLoginAttempt) return;
      setState(() {
        _isDingTalkLoading = false;
        _dingTalkStatusText = '';
      });
      _showLoginError(_resolveLoginErrorText(e));
    }
  }

  Future<bool> _showDingTalkLoginNotice() async {
    final result = await showModalBottomSheet<bool>(
      context: context,
      backgroundColor: KyXColors.surface,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (sheetContext) {
        return SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 18),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text('钉钉登录提示', style: KyXText.title),
                const SizedBox(height: 12),
                _buildDingTalkNoticeRow(
                  icon: Icons.open_in_new_outlined,
                  text: '点击后会打开钉钉授权页面。',
                ),
                const SizedBox(height: 8),
                _buildDingTalkNoticeRow(
                  icon: Icons.keyboard_return_outlined,
                  text: '在钉钉里授权成功后，手动切回 OA App，就会自动继续登录。',
                ),
                const SizedBox(height: 18),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => Navigator.pop(sheetContext, false),
                        style: _businessSecondaryLoginButtonStyle(),
                        child: const Text('取消'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.pop(sheetContext, true),
                        style: kyxPrimaryButtonStyle(),
                        child: const Text('去钉钉授权'),
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
    return result == true;
  }

  Widget _buildDingTalkNoticeRow({
    required IconData icon,
    required String text,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 1),
          child: Icon(icon, size: 17, color: KyXColors.primary),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: KyXText.secondary.copyWith(color: KyXColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Future<void> _pollDingTalkHandoff(String handoffId, int attempt) async {
    const interval = Duration(seconds: 2);
    const timeout = Duration(minutes: 2);
    final startedAt = DateTime.now();

    while (mounted && attempt == _dingTalkLoginAttempt) {
      if (DateTime.now().difference(startedAt) > timeout) {
        throw TimeoutException('钉钉授权超时，请重新尝试');
      }

      final status = await ApiService.getSocialBrowserHandoffStatus(handoffId);
      if (!mounted || attempt != _dingTalkLoginAttempt) return;

      if (status.isSuccess) {
        if (status.preAuthToken.isEmpty || status.tenantList.isEmpty) {
          throw ApiException('钉钉授权成功，但未返回可用租户');
        }
        setState(() {
          _isDingTalkLoading = false;
          _dingTalkStatusText = '';
        });
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (context) =>
                TenantSelectionPage(preLoginResult: status.toPreLoginResult()),
          ),
        );
        return;
      }

      if (status.isFailed) {
        throw ApiException(status.message ?? '钉钉授权失败，请重新尝试');
      }

      setState(() {
        _dingTalkStatusText = '等待钉钉授权完成。授权成功后请切回 OA App';
      });
      await Future.delayed(interval);
    }
  }

  String _buildSocialHandoffId() {
    final random = Random.secure().nextInt(0x7fffffff).toRadixString(16);
    return '${DateTime.now().microsecondsSinceEpoch}$random';
  }

  String _buildSocialRedirectUri(int type, String handoffId) {
    final appReturnUrl = Uri(
      scheme: 'kyxoa',
      host: 'auth',
      path: '/social-login',
      queryParameters: {'handoffId': handoffId},
    ).toString();
    final query = Uri(
      queryParameters: {
        'type': type.toString(),
        'handoffId': handoffId,
        'appReturnUrl': appReturnUrl,
      },
    ).query;
    return '${AppConfig.socialLoginOrigin}/auth/social-login?${Uri.encodeComponent(query)}';
  }

  String _resolveLoginErrorText(Object error) {
    final text = error.toString().replaceFirst('Exception: ', '');
    return text.startsWith('ApiException: ')
        ? text.replaceFirst('ApiException: ', '')
        : text;
  }

  void _showLoginError(String message) {
    final themeService = Provider.of<ThemeService>(context, listen: false);
    final theme = themeService.currentTheme;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: theme['error']),
    );
  }

  Future<void> _saveCurrentLoginFormIfNeeded() async {
    if (!_rememberPassword) return;

    await _storageService.saveLoginFormCredential(
      account: _accountController.text,
      password: _passwordController.text,
    );
    final history = await _storageService.getLoginFormCredentialHistory();
    if (!mounted) return;

    setState(() {
      _credentialHistory = history;
    });
  }

  void _login() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final input = _accountController.text;
      final loginType = _detectLoginType(input);

      if (AppConfig.isProduction) {
        AdminPreLoginResult preLoginResult;
        switch (loginType) {
          case LoginType.phone:
            preLoginResult = await ApiService.preLogin(
              phoneNumber: input,
              password: _passwordController.text,
            );
            break;
          case LoginType.email:
            preLoginResult = await ApiService.preLogin(
              email: input,
              password: _passwordController.text,
            );
            break;
          case LoginType.account:
            preLoginResult = await ApiService.preLogin(
              account: input,
              password: _passwordController.text,
            );
            break;
        }

        if (mounted) {
          await _saveCurrentLoginFormIfNeeded();
          if (!mounted) return;

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

      switch (loginType) {
        case LoginType.phone:
          loginCert = await ApiService.login(
            phoneNumber: input,
            areaCode: '+86',
            password: _passwordController.text,
          );
          break;
        case LoginType.email:
          loginCert = await ApiService.login(
            email: input,
            password: _passwordController.text,
          );
          break;
        case LoginType.account:
          loginCert = await ApiService.login(
            account: input,
            password: _passwordController.text,
          );
          break;
      }

      if (mounted) {
        await _saveCurrentLoginFormIfNeeded();
        if (!mounted) return;

        await Provider.of<AuthService>(
          context,
          listen: false,
        ).saveLoginCertificate(loginCert.toJson());
        if (!mounted) return;

        if (kDebugMode) {
          print('登录成功，用户ID: ${loginCert.userID}');
          print('租户ID: ${loginCert.tenantId}');
          print('租户名称: ${loginCert.tenantName}');
          print('登录凭证已保存到本地存储');
        }

        Navigator.pushReplacement(
          context,
          MaterialPageRoute(builder: (context) => const ChatMainPage()),
        );
        return;
      }
    } catch (e) {
      if (mounted) {
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('登录失败: $e'), backgroundColor: theme['error']),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }
}
