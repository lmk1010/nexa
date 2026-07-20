import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/storage_service.dart';
import '../services/theme_service.dart';
import 'dart:developer' as developer;
import 'dart:async';
import 'email_login_page.dart';

class RegisterEmailPage extends StatefulWidget {
  final String? email;
  final String? invitationCode;
  
  const RegisterEmailPage({
    super.key,
    this.email,
    this.invitationCode,
  });

  @override
  State<RegisterEmailPage> createState() => _RegisterEmailPageState();
}

class _RegisterEmailPageState extends State<RegisterEmailPage>
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
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nicknameController = TextEditingController();
  final _verificationCodeController = TextEditingController();
  final _invitationCodeController = TextEditingController();
  
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
    
    // 如果传入了邮箱地址，则设置到控制器中
    if (widget.email != null) {
      _emailController.text = widget.email!;
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
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nicknameController.dispose();
    _verificationCodeController.dispose();
    _invitationCodeController.dispose();
    
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
    if (_emailController.text.isEmpty) {
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('请先输入邮箱地址'),
          backgroundColor: theme['error'],
        ),
      );
      return;
    }

    try {
      // 发送邮箱验证码
      await ApiService.sendVerificationCode(
        email: _emailController.text,
      );
      
      _startCountdown();
      
      final themeService = Provider.of<ThemeService>(context, listen: false);
      final theme = themeService.currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('验证码已发送到您的邮箱'),
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
                          _buildForm(theme),
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
          '邮箱注册',
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
          color: theme['primary']!.withOpacity(0.1),
          borderRadius: BorderRadius.circular(50),
        ),
        child: Icon(
          Icons.email_outlined,
          size: 50,
          color: theme['primary'],
        ),
      ),
    );
  }

  Widget _buildWelcomeText(Map<String, Color> theme) {
    return Column(
      children: [
        Text(
          '邮箱注册',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: theme['textPrimary'],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          '请输入您的邮箱地址完成注册',
          style: TextStyle(
            fontSize: 16,
            color: theme['textSecondary'],
          ),
        ),
      ],
    );
  }

  Widget _buildForm(Map<String, Color> theme) {
    return ScaleTransition(
      scale: _formAnimation,
      child: Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: theme['surface']!.withOpacity(0.9),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: theme['outline']!.withOpacity(0.2),
            width: 1,
          ),
          boxShadow: [
            BoxShadow(
              color: theme['shadow']!.withOpacity(0.1),
              blurRadius: 16,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              _buildEmailField(theme),
              const SizedBox(height: 16),
              _buildVerificationCodeField(theme),
              const SizedBox(height: 16),
              _buildPasswordField(theme),
              const SizedBox(height: 16),
              _buildConfirmPasswordField(theme),
              const SizedBox(height: 16),
              _buildNicknameField(theme),
              const SizedBox(height: 24),
              _buildRegisterButton(theme),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmailField(Map<String, Color> theme) {
    return TextFormField(
      controller: _emailController,
      keyboardType: TextInputType.emailAddress,
      decoration: InputDecoration(
        labelText: '邮箱地址',
        hintText: '请输入您的邮箱地址',
        prefixIcon: Icon(Icons.email_outlined, color: theme['primary']),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: theme['primary']!, width: 2),
        ),
      ),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '请输入邮箱地址';
        }
        if (!RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(value)) {
          return '请输入有效的邮箱地址';
        }
        return null;
      },
    );
  }

  Widget _buildVerificationCodeField(Map<String, Color> theme) {
    return Row(
      children: [
        Expanded(
          child: TextFormField(
            controller: _verificationCodeController,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              labelText: '验证码',
              hintText: '请输入验证码',
              prefixIcon: Icon(Icons.security, color: theme['primary']),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: theme['primary']!, width: 2),
              ),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return '请输入验证码';
              }
              return null;
            },
          ),
        ),
        const SizedBox(width: 12),
        SizedBox(
          width: 120,
          height: 56,
          child: ElevatedButton(
            onPressed: _isCountingDown ? null : _sendVerificationCode,
            style: ElevatedButton.styleFrom(
              backgroundColor: theme['primary'],
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: Text(
              _isCountingDown ? '${_countdown}s' : '发送验证码',
              style: const TextStyle(fontSize: 12),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPasswordField(Map<String, Color> theme) {
    return TextFormField(
      controller: _passwordController,
      obscureText: _obscurePassword,
      decoration: InputDecoration(
        labelText: '密码',
        hintText: '请输入密码',
        prefixIcon: Icon(Icons.lock_outline, color: theme['primary']),
        suffixIcon: IconButton(
          icon: Icon(
            _obscurePassword ? Icons.visibility_off : Icons.visibility,
            color: theme['textTertiary'],
          ),
          onPressed: () {
            setState(() {
              _obscurePassword = !_obscurePassword;
            });
          },
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: theme['primary']!, width: 2),
        ),
      ),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '请输入密码';
        }
        if (value.length < 6) {
          return '密码长度至少6位';
        }
        return null;
      },
    );
  }

  Widget _buildConfirmPasswordField(Map<String, Color> theme) {
    return TextFormField(
      controller: _confirmPasswordController,
      obscureText: _obscureConfirmPassword,
      decoration: InputDecoration(
        labelText: '确认密码',
        hintText: '请再次输入密码',
        prefixIcon: Icon(Icons.lock_outline, color: theme['primary']),
        suffixIcon: IconButton(
          icon: Icon(
            _obscureConfirmPassword ? Icons.visibility_off : Icons.visibility,
            color: theme['textTertiary'],
          ),
          onPressed: () {
            setState(() {
              _obscureConfirmPassword = !_obscureConfirmPassword;
            });
          },
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: theme['primary']!, width: 2),
        ),
      ),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '请确认密码';
        }
        if (value != _passwordController.text) {
          return '两次输入的密码不一致';
        }
        return null;
      },
    );
  }

  Widget _buildNicknameField(Map<String, Color> theme) {
    return TextFormField(
      controller: _nicknameController,
      decoration: InputDecoration(
        labelText: '昵称',
        hintText: '请输入您的昵称',
        prefixIcon: Icon(Icons.person_outline, color: theme['primary']),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: theme['primary']!, width: 2),
        ),
      ),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '请输入昵称';
        }
        return null;
      },
    );
  }

  Widget _buildRegisterButton(Map<String, Color> theme) {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: ElevatedButton(
        onPressed: _isLoading ? null : _handleRegister,
        style: ElevatedButton.styleFrom(
          backgroundColor: theme['primary'],
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          elevation: 4,
        ),
        child: _isLoading
            ? SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                ),
              )
            : const Text(
                '注册',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
      ),
    );
  }

  Future<void> _handleRegister() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      developer.log('开始邮箱注册流程', name: 'RegisterEmailPage');
      
      // 直接进行注册，邀请码验证和使用由后端处理
      final registerCert = await ApiService.registerWithEmail(
        email: _emailController.text,
        password: _passwordController.text,
        nickname: _nicknameController.text,
        verificationCode: _verificationCodeController.text,
        invitationCode: widget.invitationCode ?? '',
      );

      if (mounted) {
        developer.log('邮箱注册成功，用户ID: ${registerCert.userID}', name: 'RegisterEmailPage');
        
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('注册成功！用户ID: ${registerCert.userID}，请登录'),
            backgroundColor: theme['success'],
            duration: const Duration(seconds: 3),
          ),
        );

        // 延迟一下显示成功消息，然后返回登录页面
        await Future.delayed(const Duration(seconds: 2));
        
        if (mounted) {
          Navigator.of(context).popUntil((route) => route.isFirst);
        }
      }
    } catch (e) {
      developer.log('邮箱注册失败: $e', name: 'RegisterEmailPage');
      if (mounted) {
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        
        // 检查是否是用户已存在异常
        if (e.toString().contains('UserAlreadyExistsException') || 
            e.toString().contains('已注册')) {
          // 显示用户已存在弹窗
          _showUserExistsDialog(theme);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('注册失败: $e'),
              backgroundColor: theme['error'],
            ),
          );
        }
      }
    } finally {
      developer.log('邮箱注册流程结束', name: 'RegisterEmailPage');
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
                  '邮箱已注册',
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
                '该邮箱已经注册过了，请直接登录使用。',
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
                  color: theme['info']!.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: theme['info']!.withOpacity(0.3),
                    width: 1,
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.email_outlined,
                      color: theme['info'],
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        '邮箱：${_emailController.text}',
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
                // 跳转到邮箱登录页面
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(
                    builder: (context) => EmailLoginPage(),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: theme['info'],
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