import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/theme_service.dart';
import 'register_selection_page.dart';
import 'chat_main_page.dart';
import 'dart:developer' as developer;
import 'login_page.dart'; // Added import for LoginPage

class CompanySelectionPage extends StatefulWidget {
  final bool isLoginMode; // true为登录后选择公司，false为注册时选择公司
  
  const CompanySelectionPage({
    super.key,
    this.isLoginMode = false,
  });

  @override
  State<CompanySelectionPage> createState() => _CompanySelectionPageState();
}

class _CompanySelectionPageState extends State<CompanySelectionPage>
    with TickerProviderStateMixin {
  late AnimationController _fadeController;
  late AnimationController _slideController;
  late AnimationController _formController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _formAnimation;
  
  late ThemeService _themeService;
  
  final _formKey = GlobalKey<FormState>();
  final _invitationCodeController = TextEditingController();
  
  bool _isLoading = false;
  bool _isValidating = false;
  Map<String, dynamic>? _companyInfo;

  @override
  void initState() {
    super.initState();
    
    _themeService = Provider.of<ThemeService>(context, listen: false);
    
    SystemChrome.setSystemUIOverlayStyle(_themeService.systemUiStyle);
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom]
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
  void dispose() {
    _invitationCodeController.dispose();
    _fadeController.dispose();
    _slideController.dispose();
    _formController.dispose();
    SystemChrome.setSystemUIOverlayStyle(_themeService.restoreSystemUiStyle);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;
    final screenWidth = MediaQuery.of(context).size.width;
    
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
                    child: ConstrainedBox(
                      constraints: BoxConstraints(
                        minHeight: screenHeight - MediaQuery.of(context).padding.top - MediaQuery.of(context).padding.bottom,
                      ),
                      child: IntrinsicHeight(
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
                                child: _buildCompanySelectionForm(theme),
                              ),
                              const Spacer(),
                              SizedBox(height: 20),
                            ],
                          ),
                        ),
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
        if (!widget.isLoginMode) // 注册模式下显示返回按钮
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: Icon(Icons.arrow_back_ios, color: theme['textPrimary']),
          ),
        Expanded(
          child: Text(
            widget.isLoginMode ? '选择公司' : '选择公司',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: theme['textPrimary'],
            ),
            textAlign: TextAlign.center,
          ),
        ),
        if (!widget.isLoginMode) // 注册模式下显示占位
          const SizedBox(width: 48),
      ],
    );
  }

  Widget _buildLogo(Map<String, Color> theme) {
    return Container(
      width: 100,
      height: 100,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            theme['primary']!.withOpacity(0.1),
            theme['primary']!.withOpacity(0.2),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(50),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.2),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Icon(
        Icons.business,
        size: 50,
        color: theme['primary'],
      ),
    );
  }

  Widget _buildWelcomeText(Map<String, Color> theme) {
    return Column(
      children: [
        Text(
          widget.isLoginMode ? '欢迎回来' : '欢迎加入',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: theme['textPrimary'],
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          widget.isLoginMode 
              ? '请选择您要进入的公司'
              : '请输入邀请码选择您的公司',
          style: TextStyle(
            fontSize: 16,
            color: theme['textSecondary'],
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          decoration: BoxDecoration(
            color: theme['primary']!.withOpacity(0.1),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: theme['primary']!.withOpacity(0.3),
              width: 1,
            ),
          ),
          child: Text(
            widget.isLoginMode 
                ? '登录后需要选择公司才能进入系统'
                : '注册前需要先选择公司',
            style: TextStyle(
              fontSize: 12,
              color: theme['primary'],
              fontWeight: FontWeight.w500,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      ],
    );
  }

  Widget _buildCompanySelectionForm(Map<String, Color> theme) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildInvitationCodeField(theme),
          const SizedBox(height: 24),
          _buildValidateButton(theme),
          if (_companyInfo != null) ...[
            const SizedBox(height: 24),
            _buildCompanyInfo(theme),
            const SizedBox(height: 24),
            _buildContinueButton(theme),
          ],
        ],
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
        decoration: InputDecoration(
          labelText: '邀请码',
          hintText: widget.isLoginMode ? '请输入公司邀请码' : '请输入公司邀请码',
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
          fillColor: theme['surface']!.withOpacity(0.9),
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

  Widget _buildValidateButton(Map<String, Color> theme) {
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
        onPressed: _isValidating ? null : _validateInvitationCode,
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.transparent,
          foregroundColor: theme['onPrimary'],
          elevation: 0,
          shadowColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
        child: _isValidating
            ? SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(theme['onPrimary']!),
                ),
              )
            : Text(
                widget.isLoginMode ? '进入公司' : '验证邀请码',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
      ),
    );
  }

  Widget _buildCompanyInfo(Map<String, Color> theme) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme['surface']!.withOpacity(0.9),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: theme['success']!.withOpacity(0.3),
          width: 2,
        ),
        boxShadow: [
          BoxShadow(
            color: theme['shadow']!.withOpacity(0.1),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: theme['success']!.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(Icons.check_circle, color: theme['success'], size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  '邀请码验证成功',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: theme['success'],
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: theme['primary']!.withOpacity(0.05),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: theme['primary']!.withOpacity(0.1),
                width: 1,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.business, color: theme['primary'], size: 18),
                    const SizedBox(width: 8),
                    Text(
                      '公司信息',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: theme['primary'],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Text(
                      '公司名称：',
                      style: TextStyle(
                        fontSize: 14,
                        color: theme['textSecondary'],
                      ),
                    ),
                    Expanded(
                      child: Text(
                        _companyInfo!['tenantName'],
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: theme['textPrimary'],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Text(
                      '邀请码状态：',
                      style: TextStyle(
                        fontSize: 14,
                        color: theme['textSecondary'],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: _companyInfo!['status'] == 0 
                            ? theme['success']!.withOpacity(0.1)
                            : theme['error']!.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        _getInviteCodeStatusText(_companyInfo!['status']),
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          color: _companyInfo!['status'] == 0 
                              ? theme['success']
                              : theme['error'],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Text(
                      '租户状态：',
                      style: TextStyle(
                        fontSize: 14,
                        color: theme['textSecondary'],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: _companyInfo!['tenantStatus'] == 0 
                            ? theme['success']!.withOpacity(0.1)
                            : theme['error']!.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        _companyInfo!['tenantStatus'] == 0 ? '正常' : '禁用',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          color: _companyInfo!['tenantStatus'] == 0 
                              ? theme['success']
                              : theme['error'],
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContinueButton(Map<String, Color> theme) {
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
        onPressed: _isLoading ? null : _continueToNext,
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
            : Text(
                widget.isLoginMode ? '进入聊天' : '继续注册',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
      ),
    );
  }

  Future<void> _validateInvitationCode() async {
    developer.log('开始验证邀请码', name: 'CompanySelectionPage');
    
    if (!_formKey.currentState!.validate()) {
      developer.log('表单验证失败', name: 'CompanySelectionPage');
      return;
    }

    setState(() {
      _isValidating = true;
    });

    try {
      final invitationCode = _invitationCodeController.text;
      developer.log('验证邀请码: $invitationCode', name: 'CompanySelectionPage');
      
      final companyInfo = await ApiService.validateInvitationCode(invitationCode);

      if (mounted) {
        // 检查邀请码状态
        final inviteCodeStatus = companyInfo['status'] as int?;
        if (inviteCodeStatus != null && inviteCodeStatus != 0) {
          // 邀请码被禁用、过期或已用完
          final themeService = Provider.of<ThemeService>(context, listen: false);
          final theme = themeService.currentTheme;
          String errorMessage;
          bool shouldShowDialog = false;
          
          switch (inviteCodeStatus) {
            case 1:
              errorMessage = '邀请码已被禁用';
              break;
            case 2:
              errorMessage = '邀请码已过期';
              shouldShowDialog = true;
              break;
            case 3:
              errorMessage = '邀请码已用完';
              break;
            default:
              errorMessage = '邀请码状态异常';
          }
          
          if (shouldShowDialog) {
            // 显示弹窗
            await showDialog<void>(
              context: context,
              barrierDismissible: false,
              builder: (BuildContext context) {
                return AlertDialog(
                  title: Text(
                    '邀请码已过期',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: theme['text'],
                    ),
                  ),
                  content: Text(
                    '您的邀请码已过期，请联系管理员重新索要邀请码。',
                    style: TextStyle(
                      fontSize: 16,
                      color: theme['textSecondary'],
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                        // 返回登录界面
                        Navigator.pushReplacement(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const LoginPage(),
                          ),
                        );
                      },
                      child: Text(
                        '确认',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: theme['primary'],
                        ),
                      ),
                    ),
                  ],
                );
              },
            );
          } else {
            // 显示SnackBar
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(errorMessage),
                backgroundColor: theme['error'],
                duration: const Duration(seconds: 3),
              ),
            );
          }
          return;
        }

        // 检查租户状态
        final tenantStatus = companyInfo['tenantStatus'] as int?;
        if (tenantStatus != null && tenantStatus == 1) {
          // 租户被禁用，显示错误信息
          final themeService = Provider.of<ThemeService>(context, listen: false);
          final theme = themeService.currentTheme;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('该公司的邀请码已被禁用，请联系管理员'),
              backgroundColor: theme['error'],
              duration: const Duration(seconds: 3),
            ),
          );
          return;
        }

        setState(() {
          _companyInfo = companyInfo;
        });
        
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('邀请码验证成功！'),
            backgroundColor: theme['success'],
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      developer.log('邀请码验证失败: $e', name: 'CompanySelectionPage');
      if (mounted) {
        final themeService = Provider.of<ThemeService>(context, listen: false);
        final theme = themeService.currentTheme;
        
        // 检查是否是邀请码过期错误
        if (e.toString().contains('过期') || e.toString().contains('expired')) {
          // 显示弹窗
          await showDialog<void>(
            context: context,
            barrierDismissible: false,
            builder: (BuildContext context) {
              return AlertDialog(
                title: Text(
                  '邀请码已过期',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: theme['text'],
                  ),
                ),
                content: Text(
                  '您的邀请码已过期，请联系管理员重新索要邀请码。',
                  style: TextStyle(
                    fontSize: 16,
                    color: theme['textSecondary'],
                  ),
                ),
                actions: [
                  TextButton(
                    onPressed: () {
                      Navigator.of(context).pop();
                      // 返回登录界面
                      Navigator.pushReplacement(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const LoginPage(),
                        ),
                      );
                    },
                    child: Text(
                      '确认',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: theme['primary'],
                      ),
                    ),
                  ),
                ],
              );
            },
          );
        } else {
          // 其他错误显示SnackBar
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('邀请码验证失败: $e'),
              backgroundColor: theme['error'],
            ),
          );
        }
      }
    } finally {
      developer.log('邀请码验证流程结束', name: 'CompanySelectionPage');
      if (mounted) {
        setState(() {
          _isValidating = false;
        });
      }
    }
  }

  String _getInviteCodeStatusText(int status) {
    switch (status) {
      case 0:
        return '启用';
      case 1:
        return '禁用';
      case 2:
        return '已过期';
      case 3:
        return '已用完';
      default:
        return '未知';
    }
  }

  void _continueToNext() {
    if (widget.isLoginMode) {
      // 登录模式：直接进入聊天主界面
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => const ChatMainPage(),
        ),
      );
    } else {
      // 注册模式：跳转到注册选择页面
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => RegisterSelectionPage(
            invitationCode: _invitationCodeController.text,
            companyInfo: _companyInfo,
          ),
        ),
      );
    }
  }
}