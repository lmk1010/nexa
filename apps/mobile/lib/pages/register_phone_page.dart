import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/theme_service.dart';
import 'dart:developer' as developer;
import 'dart:async';
import 'chat_main_page.dart';
import 'phone_login_page.dart';

class RegisterPhonePage extends StatefulWidget {
  final String? phoneNumber;
  final String? areaCode;
  final String? invitationCode;
  
  const RegisterPhonePage({
    super.key,
    this.phoneNumber,
    this.areaCode,
    this.invitationCode,
  });

  @override
  State<RegisterPhonePage> createState() => _RegisterPhonePageState();
}

class _RegisterPhonePageState extends State<RegisterPhonePage>
    with TickerProviderStateMixin {
  late AnimationController _fadeController;
  late AnimationController _slideController;
  late AnimationController _formController;
  late AnimationController _imageController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _formAnimation;
  late Animation<double> _imageAnimation;
  
  late ThemeService _themeService; // 保存ThemeService引用
  
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _areaCodeController = TextEditingController(text: '+86');
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nicknameController = TextEditingController();
  final _verificationCodeController = TextEditingController();
  
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  bool _isCountingDown = false;
  int _countdown = 60;

  @override
  void initState() {
    super.initState();
    
    // 保存ThemeService引用
    _themeService = Provider.of<ThemeService>(context, listen: false);
    
    // 设置沉浸式状态栏
    SystemChrome.setSystemUIOverlayStyle(_themeService.systemUiStyle);
    
    // 设置沉浸式显示（状态栏可见但透明）
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.immersiveSticky,
    );
    
    // 如果传入了手机号和区号，则设置到控制器中
    if (widget.phoneNumber != null) {
      _phoneController.text = widget.phoneNumber!;
    }
    if (widget.areaCode != null) {
      _areaCodeController.text = widget.areaCode!;
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
    _imageController = AnimationController(
      duration: const Duration(milliseconds: 1000),
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
    
    _imageAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _imageController,
      curve: Curves.easeInOut,
    ));
    
    _fadeController.forward();
    _slideController.forward();
    _formController.forward();
    _imageController.forward();
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _areaCodeController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nicknameController.dispose();
    _verificationCodeController.dispose();
    
    _fadeController.dispose();
    _slideController.dispose();
    _formController.dispose();
    _imageController.dispose();
    
    // 恢复系统UI设置 - 使用保存的引用
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
    if (_phoneController.text.isEmpty) {
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('请先输入手机号'),
          backgroundColor: theme['error'],
        ),
      );
      return;
    }

    try {
      // 发送手机验证码
      await ApiService.sendVerificationCode(
        phoneNumber: _phoneController.text,
      );
      
      _startCountdown();
      
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('验证码已发送'),
          backgroundColor: theme['success'],
        ),
      );
    } catch (e) {
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('发送失败: $e'),
          backgroundColor: theme['error'],
        ),
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
          '手机注册',
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
    return FadeTransition(
      opacity: _imageAnimation,
      child: Container(
        width: 100,
        height: 100,
        decoration: BoxDecoration(
          color: theme['success']!.withOpacity(0.1),
          borderRadius: BorderRadius.circular(25),
          boxShadow: [
            BoxShadow(
              color: theme['success']!.withOpacity(0.2),
              blurRadius: 20,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // 背景装饰
            Positioned(
              top: 10,
              right: 10,
              child: Container(
                width: 20,
                height: 20,
                decoration: BoxDecoration(
                  color: theme['success']!.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ),
            Positioned(
              bottom: 15,
              left: 15,
              child: Container(
                width: 15,
                height: 15,
                decoration: BoxDecoration(
                  color: theme['success']!.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(7.5),
                ),
              ),
            ),
            // 主图标
            Icon(
              Icons.phone_android_rounded,
              size: 50,
              color: theme['success'],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildWelcomeText(Map<String, Color> theme) {
    return Column(
      children: [
        Text(
          '手机号注册',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: theme['textPrimary'],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          '使用手机号快速完成注册',
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
          _buildPhoneField(theme),
          const SizedBox(height: 20),
          _buildVerificationCodeField(theme),
          const SizedBox(height: 20),
          _buildPasswordField(theme),
          const SizedBox(height: 20),
          _buildConfirmPasswordField(theme),
          const SizedBox(height: 20),
          _buildNicknameField(theme),
          const SizedBox(height: 32),
          _buildRegisterButton(theme),
        ],
      ),
    );
  }

  Widget _buildPhoneField(Map<String, Color> theme) {
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
        controller: _phoneController,
        keyboardType: TextInputType.phone,
        decoration: InputDecoration(
          labelText: '手机号',
          hintText: '请输入手机号',
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: theme['success']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.phone_outlined, size: 18, color: theme['success']),
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
            borderSide: BorderSide(color: theme['success']!, width: 1.5),
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
            return '请输入手机号';
          }
          // 简单的手机号验证（11位数字）
          if (!RegExp(r'^1[3-9]\d{9}$').hasMatch(value)) {
            return '请输入正确的手机号';
          }
          return null;
        },
      ),
    );
  }

  Widget _buildVerificationCodeField(Map<String, Color> theme) {
    return Row(
      children: [
        Expanded(
          child: Container(
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
              decoration: InputDecoration(
                labelText: '验证码',
                hintText: '请输入验证码',
                prefixIcon: Container(
                  margin: const EdgeInsets.all(8),
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: theme['success']!.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Icon(Icons.verified, size: 18, color: theme['success']),
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
                  borderSide: BorderSide(color: theme['success']!, width: 1.5),
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
                  return '请输入验证码';
                }
                return null;
              },
            ),
          ),
        ),
        const SizedBox(width: 12),
        Container(
          height: 52,
          child: ElevatedButton(
            onPressed: _isCountingDown ? null : _sendVerificationCode,
            style: ElevatedButton.styleFrom(
              backgroundColor: _isCountingDown ? theme['textTertiary'] : theme['success'],
              foregroundColor: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: Text(
              _isCountingDown ? '${_countdown}s' : '发送',
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
      ],
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
              color: theme['success']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.lock_outline, size: 18, color: theme['success']),
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
            borderSide: BorderSide(color: theme['success']!, width: 1.5),
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
              color: theme['success']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.lock_outline, size: 18, color: theme['success']),
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
            borderSide: BorderSide(color: theme['success']!, width: 1.5),
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
              color: theme['success']!.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(Icons.person_outline, size: 18, color: theme['success']),
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
            borderSide: BorderSide(color: theme['success']!, width: 1.5),
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
            theme['success']!,
            theme['success']!.withOpacity(0.8),
          ],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        boxShadow: [
          BoxShadow(
            color: theme['success']!.withOpacity(0.3),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: ElevatedButton(
        onPressed: _isLoading ? null : _register,
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.transparent,
          foregroundColor: Colors.white,
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
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
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
    developer.log('开始手机注册流程', name: 'RegisterPhonePage');
    
    if (!_formKey.currentState!.validate()) {
      developer.log('表单验证失败', name: 'RegisterPhonePage');
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final phoneNumber = _phoneController.text;
      final areaCode = _areaCodeController.text;
      final nickname = _nicknameController.text.isNotEmpty 
          ? _nicknameController.text 
          : '用户 ${DateTime.now().millisecondsSinceEpoch}';
      
      developer.log('手机注册 - 手机号: $phoneNumber, 区号: $areaCode', name: 'RegisterPhonePage');
      
      // 直接进行注册，邀请码验证和使用由后端处理
      final registerCert = await ApiService.registerWithVerification(
        nickname: nickname,
        password: _passwordController.text,
        phoneNumber: phoneNumber,
        areaCode: areaCode,
        verificationCode: _verificationCodeController.text,
        invitationCode: widget.invitationCode,
      );

              if (mounted) {
          developer.log('手机注册成功，用户ID: ${registerCert.userID}', name: 'RegisterPhonePage');
          
          // 使用AuthService保存登录凭证并更新状态
          final authService = Provider.of<AuthService>(context, listen: false);
          await authService.saveLoginCertificate(registerCert.toMap());
          
          // 显示成功消息
          final themeService = Provider.of<ThemeService>(context, listen: false);
          final theme = themeService.currentTheme;
          
          // 使用全局的ScaffoldMessenger来避免widget销毁问题
          final scaffoldMessenger = ScaffoldMessenger.of(context);
          scaffoldMessenger.showSnackBar(
            SnackBar(
              content: Text('注册成功！正在自动登录...'),
              backgroundColor: theme['success'],
              duration: const Duration(seconds: 2),
            ),
          );

          // 延迟一下显示成功消息，然后跳转到主页面
          await Future.delayed(const Duration(seconds: 2));
          
          if (mounted) {
            // 直接跳转到主页面，而不是返回登录页面
            Navigator.of(context).pushAndRemoveUntil(
              MaterialPageRoute(
                builder: (context) => const ChatMainPage(),
              ),
              (route) => false, // 清除所有路由
            );
          }
        }
    } catch (e) {
      developer.log('手机注册失败: $e', name: 'RegisterPhonePage');
      if (mounted) {
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        
        // 检查是否是用户已存在异常
        if (e.toString().contains('UserAlreadyExistsException') || 
            e.toString().contains('已注册')) {
          // 显示用户已存在弹窗
          _showUserExistsDialog(theme);
        } else {
          // 使用全局的ScaffoldMessenger来避免widget销毁问题
          final scaffoldMessenger = ScaffoldMessenger.of(context);
          scaffoldMessenger.showSnackBar(
            SnackBar(
              content: Text('注册失败: $e'),
              backgroundColor: theme['error'],
            ),
          );
        }
      }
    } finally {
      developer.log('手机注册流程结束', name: 'RegisterPhonePage');
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
                  '手机号已注册',
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
                '该手机号已经注册过了，请直接登录使用。',
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
                  color: theme['success']!.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: theme['success']!.withOpacity(0.3),
                    width: 1,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.phone_android_outlined,
                      color: theme['success'],
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        '手机号：${_phoneController.text}',
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
                // 跳转到手机登录页面
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(
                    builder: (context) => PhoneLoginPage(),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: theme['success'],
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