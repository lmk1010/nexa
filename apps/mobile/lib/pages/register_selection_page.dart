import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/theme_service.dart';
import 'register_account_page.dart';
import 'register_phone_page.dart';
import 'register_email_page.dart';

class RegisterSelectionPage extends StatefulWidget {
  final String? invitationCode;
  final Map<String, dynamic>? companyInfo;

  const RegisterSelectionPage({
    super.key,
    this.invitationCode,
    this.companyInfo,
  });

  @override
  State<RegisterSelectionPage> createState() => _RegisterSelectionPageState();
}

class _RegisterSelectionPageState extends State<RegisterSelectionPage>
    with TickerProviderStateMixin {
  late AnimationController _fadeController;
  late AnimationController _slideController;
  late AnimationController _buttonController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _buttonAnimation;

  @override
  void initState() {
    super.initState();

    // 设置沉浸式状态栏
    final themeService = Provider.of<ThemeService>(context, listen: false);
    SystemChrome.setSystemUIOverlayStyle(themeService.systemUiStyle);

    // 设置沉浸式显示（状态栏可见但透明）
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);

    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _slideController = AnimationController(
      duration: const Duration(milliseconds: 600),
      vsync: this,
    );
    _buttonController = AnimationController(
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

    _buttonAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _buttonController, curve: Curves.elasticOut),
    );

    _fadeController.forward();
    _slideController.forward();
    _buttonController.forward();
  }

  @override
  void dispose() {
    _fadeController.dispose();
    _slideController.dispose();
    _buttonController.dispose();

    // 恢复系统UI设置
    final themeService = Provider.of<ThemeService>(context, listen: false);
    SystemChrome.setSystemUIOverlayStyle(themeService.restoreSystemUiStyle);

    super.dispose();
  }

  void _navigateToRegisterPage(Widget page) {
    Navigator.of(context).push(MaterialPageRoute(builder: (context) => page));
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
                    padding: EdgeInsets.symmetric(
                      horizontal: screenWidth * 0.06,
                    ),
                    child: ConstrainedBox(
                      constraints: BoxConstraints(
                        minHeight: screenHeight - topPadding - bottomPadding,
                      ),
                      child: IntrinsicHeight(
                        child: Column(
                          children: [
                            SizedBox(height: screenHeight * 0.04),
                            _buildHeader(theme),
                            SizedBox(height: screenHeight * 0.025),
                            _buildLogo(theme),
                            SizedBox(height: screenHeight * 0.025),
                            _buildWelcomeText(theme),
                            if (widget.companyInfo != null) ...[
                              SizedBox(height: screenHeight * 0.02),
                              _buildCompanyInfo(theme),
                            ],
                            SizedBox(height: screenHeight * 0.05),
                            _buildRegisterOptions(theme),
                            const Spacer(),
                            _buildBackToLoginButton(theme),
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
          '选择注册方式',
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
      child: Icon(Icons.person_add_rounded, size: 40, color: theme['primary']),
    );
  }

  Widget _buildWelcomeText(Map<String, Color> theme) {
    return Column(
      children: [
        Text(
          '欢迎加入我们',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: theme['textPrimary'],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          '请选择您喜欢的注册方式',
          style: TextStyle(fontSize: 16, color: theme['textSecondary']),
        ),
      ],
    );
  }

  Widget _buildCompanyInfo(Map<String, Color> theme) {
    if (widget.companyInfo == null) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme['success']!.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: theme['success']!.withOpacity(0.3), width: 1),
      ),
      child: Row(
        children: [
          Icon(Icons.business, color: theme['success'], size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '已选择公司',
                  style: TextStyle(fontSize: 12, color: theme['textSecondary']),
                ),
                const SizedBox(height: 2),
                Text(
                  widget.companyInfo!['tenantName'] ?? '未知公司',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: theme['textPrimary'],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRegisterOptions(Map<String, Color> theme) {
    return ScaleTransition(
      scale: _buttonAnimation,
      child: Column(
        children: [
          _buildRegisterOption(
            icon: Icons.account_circle_outlined,
            title: '账号注册',
            subtitle: '使用用户名和密码注册',
            color: theme['primary']!,
            onTap: () => _navigateToRegisterPage(
              RegisterAccountPage(invitationCode: widget.invitationCode),
            ),
            theme: theme,
          ),
          const SizedBox(height: 16),
          _buildRegisterOption(
            icon: Icons.phone_android_outlined,
            title: '手机注册',
            subtitle: '使用手机号快速注册',
            color: theme['success']!,
            onTap: () => _navigateToRegisterPage(
              RegisterPhonePage(invitationCode: widget.invitationCode),
            ),
            theme: theme,
          ),
          const SizedBox(height: 16),
          _buildRegisterOption(
            icon: Icons.email_outlined,
            title: '邮箱注册',
            subtitle: '使用邮箱地址注册',
            color: theme['info']!,
            onTap: () => _navigateToRegisterPage(
              RegisterEmailPage(invitationCode: widget.invitationCode),
            ),
            theme: theme,
          ),
        ],
      ),
    );
  }

  Widget _buildRegisterOption({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required VoidCallback onTap,
    required Map<String, Color> theme,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: theme['surface']!.withOpacity(0.9),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: theme['outline']!.withOpacity(0.2),
            width: 1,
          ),
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
            Container(
              width: 50,
              height: 50,
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: color, size: 24),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: theme['textPrimary'],
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 14,
                      color: theme['textSecondary'],
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              Icons.arrow_forward_ios,
              color: theme['textTertiary'],
              size: 16,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBackToLoginButton(Map<String, Color> theme) {
    return TextButton(
      onPressed: () => Navigator.of(context).pop(),
      style: TextButton.styleFrom(
        foregroundColor: theme['primary'],
        padding: const EdgeInsets.symmetric(horizontal: 0, vertical: 6),
      ),
      child: Text(
        '返回登录',
        style: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: theme['primary'],
        ),
      ),
    );
  }
}
