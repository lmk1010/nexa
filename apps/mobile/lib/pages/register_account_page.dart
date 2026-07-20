import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/storage_service.dart';
import '../services/theme_service.dart';
import 'chat_main_page.dart';
import 'dart:developer' as developer;
import 'login_page.dart';

class RegisterAccountPage extends StatefulWidget {
  final String? invitationCode;
  
  const RegisterAccountPage({
    super.key,
    this.invitationCode,
  });

  @override
  State<RegisterAccountPage> createState() => _RegisterAccountPageState();
}

class _RegisterAccountPageState extends State<RegisterAccountPage>
    with TickerProviderStateMixin {
  late AnimationController _fadeController;
  late AnimationController _slideController;
  late AnimationController _formController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _formAnimation;
  
  late ThemeService _themeService; // 保存ThemeService引用
  ScaffoldMessengerState? _scaffoldMessenger; // 保存ScaffoldMessenger引用
  
  final _formKey = GlobalKey<FormState>();
  final _accountController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _invitationCodeController = TextEditingController();
  final _nicknameController = TextEditingController();
  
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;

  @override
  void initState() {
    super.initState();
    
    // 保存ThemeService引用
    _themeService = Provider.of<ThemeService>(context, listen: false);
    
    // 设置沉浸式状态栏
    SystemChrome.setSystemUIOverlayStyle(_themeService.systemUiStyle);
    
    // 设置系统UI模式为状态栏和导航栏都可见
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom]
    );
    
    // 如果有邀请码，预填充到输入框
    if (widget.invitationCode != null && widget.invitationCode!.isNotEmpty) {
      _invitationCodeController.text = widget.invitationCode!;
    }
    
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
    
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _fadeController,
      curve: Curves.easeInOut,
    ));
    
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.3),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _slideController,
      curve: Curves.easeOutCubic,
    ));
    
    _formAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _formController,
      curve: Curves.elasticOut,
    ));
    
    _fadeController.forward();
    _slideController.forward();
    _formController.forward();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // 保存ScaffoldMessenger引用
    _scaffoldMessenger = ScaffoldMessenger.of(context);
  }

  @override
  void dispose() {
    _accountController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _invitationCodeController.dispose();
    _nicknameController.dispose();
    
    _fadeController.dispose();
    _slideController.dispose();
    _formController.dispose();
    
    // 恢复系统UI设置 - 使用保存的引用
    SystemChrome.setSystemUIOverlayStyle(_themeService.restoreSystemUiStyle);
    
    super.dispose();
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
                  child: SingleChildScrollView(
                    padding: EdgeInsets.only(
                      bottom: MediaQuery.of(context).viewInsets.bottom + 20,
                    ),
                    child: Padding(
                      padding: EdgeInsets.symmetric(
                        horizontal: screenWidth * 0.06,
                      ),
                      child: Column(
                        children: [
                          SizedBox(height: screenHeight * 0.05),
                          _buildHeader(theme),
                          SizedBox(height: screenHeight * 0.03),
                          _buildLogo(theme),
                          SizedBox(height: screenHeight * 0.03),
                          _buildWelcomeText(theme),
                          SizedBox(height: screenHeight * 0.04),
                          ScaleTransition(
                            scale: _formAnimation,
                            child: _buildRegisterForm(theme),
                          ),
                          SizedBox(height: 20),
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
      children: [
        IconButton(
          onPressed: () => Navigator.of(context).pop(),
          icon: Icon(
            Icons.arrow_back_ios_new,
            color: theme['textPrimary'],
            size: 24,
          ),
        ),
        const Spacer(),
        Text(
          '账号注册',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: theme['textPrimary'],
          ),
        ),
        const Spacer(),
        const SizedBox(width: 48), // 平衡布局
      ],
    );
  }

  Widget _buildLogo(Map<String, Color> theme) {
    return Container(
      width: 80,
      height: 80,
      decoration: BoxDecoration(
        color: theme['primary']!.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Icon(
        Icons.account_circle_outlined,
        size: 40,
        color: theme['primary'],
      ),
    );
  }

  Widget _buildWelcomeText(Map<String, Color> theme) {
    return Column(
      children: [
        Text(
          '创建账号',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: theme['textPrimary'],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          '请填写以下信息完成注册',
          style: TextStyle(
            fontSize: 16,
            color: theme['textSecondary'],
          ),
        ),
      ],
    );
  }

  Widget _buildRegisterForm(Map<String, Color> theme) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildAccountField(theme),
          const SizedBox(height: 20),
          _buildPasswordField(theme),
          const SizedBox(height: 20),
          _buildConfirmPasswordField(theme),
          const SizedBox(height: 20),
          _buildInvitationCodeField(theme),
          const SizedBox(height: 20),
          _buildNicknameField(theme),
          const SizedBox(height: 32),
          _buildRegisterButton(theme),
        ],
      ),
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
        decoration: InputDecoration(
          labelText: '用户ID',
          hintText: '请输入用户ID',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.person_outline, size: 18, color: theme['primary']),
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
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请输入用户ID';
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
          hintText: '请输入密码',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.lock_outline, size: 18, color: theme['primary']),
          ),
          suffixIcon: IconButton(
            icon: Icon(
              _obscurePassword ? Icons.visibility_off : Icons.visibility,
              size: 18,
              color: theme['textSecondary'],
            ),
            onPressed: () {
              setState(() {
                _obscurePassword = !_obscurePassword;
              });
            },
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
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请输入密码';
          }
          if (value.length < 6) {
            return '密码长度不能少于6位';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildConfirmPasswordField(Map<String, Color> theme) {
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
        controller: _confirmPasswordController,
        obscureText: _obscureConfirmPassword,
        decoration: InputDecoration(
          labelText: '确认密码',
          hintText: '请再次输入密码',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.lock_outline, size: 18, color: theme['primary']),
          ),
          suffixIcon: IconButton(
            icon: Icon(
              _obscureConfirmPassword ? Icons.visibility_off : Icons.visibility,
              size: 18,
              color: theme['textSecondary'],
            ),
            onPressed: () {
              setState(() {
                _obscureConfirmPassword = !_obscureConfirmPassword;
              });
            },
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
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请再次输入密码';
          }
          if (value != _passwordController.text) {
            return '两次输入的密码不一致';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildInvitationCodeField(Map<String, Color> theme) {
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
        controller: _invitationCodeController,
        enabled: widget.invitationCode == null || widget.invitationCode!.isEmpty,
        decoration: InputDecoration(
          labelText: '邀请码',
          hintText: widget.invitationCode != null && widget.invitationCode!.isNotEmpty 
              ? '已预填充邀请码' 
              : '请输入邀请码（必填）',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.code_outlined, size: 18, color: theme['primary']),
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
          fillColor: widget.invitationCode != null && widget.invitationCode!.isNotEmpty
              ? theme['success']!.withOpacity(0.1)
              : theme['surface']!.withOpacity(0.9),
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
        validator: (value) {
          if (value == null || value.isEmpty) {
            return '请输入邀请码';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildNicknameField(Map<String, Color> theme) {
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
        controller: _nicknameController,
        decoration: InputDecoration(
          labelText: '昵称',
          hintText: '请输入您的昵称 (可选)',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.person_outline, size: 18, color: theme['primary']),
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
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          labelStyle: TextStyle(fontSize: 15, color: theme['textSecondary']),
          hintStyle: TextStyle(fontSize: 15, color: theme['textTertiary']),
        ),
        style: TextStyle(fontSize: 15, color: theme['textPrimary']),
      ),
    );
  }

  Widget _buildRegisterButton(Map<String, Color> theme) {
    return Container(
      width: double.infinity,
      height: 48,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: LinearGradient(
          colors: [
            theme['primary']!,
            theme['primaryLight']!,
          ],
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
        onPressed: _isLoading ? null : _register,
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
                  valueColor: AlwaysStoppedAnimation<Color>(theme['onPrimary']!),
                ),
              )
            : const Text(
                '注册',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
      ),
    );
  }

  void _register() async {
    developer.log('开始账号注册流程', name: 'RegisterAccountPage');
    
    if (!_formKey.currentState!.validate()) {
      developer.log('表单验证失败', name: 'RegisterAccountPage');
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final userID = _accountController.text;
      final nickname = _nicknameController.text.isNotEmpty 
          ? _nicknameController.text 
          : '用户 ${DateTime.now().millisecondsSinceEpoch}';
      final invitationCode = _invitationCodeController.text;
      
      developer.log('账号注册 - 用户ID: $userID, 昵称: $nickname, 邀请码: $invitationCode', name: 'RegisterAccountPage');
      
      // 直接进行注册，邀请码验证和使用由后端处理
      final registerCert = await ApiService.registerAccount(
        userID: userID,
        nickname: nickname,
        password: _passwordController.text,
        invitationCode: invitationCode,
      );

      if (mounted) {
        developer.log('账号注册成功，用户ID: ${registerCert.userID}', name: 'RegisterAccountPage');
        developer.log('租户ID: ${registerCert.tenantId}', name: 'RegisterAccountPage');
        developer.log('租户名称: ${registerCert.tenantName}', name: 'RegisterAccountPage');
        
        // 保存登录凭证
        await StorageService().saveLoginCertificate(registerCert.toJson());
        
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        
        // 显示注册成功信息，包含租户信息
        String successMessage = '注册成功！';
        if (registerCert.tenantName != null && registerCert.tenantName!.isNotEmpty) {
          successMessage += ' 当前公司：${registerCert.tenantName}';
        }
        
        if (_scaffoldMessenger != null) {
          _scaffoldMessenger!.showSnackBar(
            SnackBar(
              content: Text(successMessage),
              backgroundColor: theme['success'],
              duration: const Duration(seconds: 3),
            ),
          );
        }

        // 注册成功后直接跳转到聊天主页面
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (context) => const ChatMainPage(),
          ),
        );
      }
    } catch (e) {
      developer.log('账号注册失败: $e', name: 'RegisterAccountPage');
      if (mounted) {
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        
        // 检查是否是用户已存在异常
        if (e.toString().contains('UserAlreadyExistsException') || 
            e.toString().contains('已注册')) {
          // 显示用户已存在弹窗
          _showUserExistsDialog(theme);
        } else if (_scaffoldMessenger != null) {
          _scaffoldMessenger!.showSnackBar(
            SnackBar(
              content: Text('注册失败: $e'),
              backgroundColor: theme['error'],
            ),
          );
        }
      }
    } finally {
      developer.log('账号注册流程结束', name: 'RegisterAccountPage');
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  // 显示用户已存在弹窗
  void _showUserExistsDialog(Map<String, Color> theme) {
    showDialog(
      context: context,
      barrierDismissible: false, // 不允许点击外部关闭
      builder: (BuildContext context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: theme['warning']!.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(
                  Icons.info_outline,
                  color: theme['warning'],
                  size: 24,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  '账号已注册',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: theme['textPrimary'],
                  ),
                ),
              ),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '该账号已经注册过了，请直接登录使用。',
                style: TextStyle(
                  fontSize: 16,
                  color: theme['textSecondary'],
                  height: 1.4,
                ),
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: theme['primary']!.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: theme['primary']!.withOpacity(0.3),
                    width: 1,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.account_circle_outlined,
                      color: theme['primary'],
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        '账号：${_accountController.text}',
                        style: TextStyle(
                          fontSize: 14,
                          color: theme['textSecondary'],
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text(
                '继续注册',
                style: TextStyle(
                  color: theme['textSecondary'],
                  fontSize: 16,
                ),
              ),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                // 跳转到登录页面
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(
                    builder: (context) => LoginPage(),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: theme['primary'],
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                elevation: 0,
              ),
              child: const Text(
                '去登录',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}