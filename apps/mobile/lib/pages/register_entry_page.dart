import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../config/app_config.dart';
import '../services/theme_service.dart';
import '../widgets/kyx_design.dart';
import 'login_page.dart';
import 'company_selection_page.dart';

class RegisterEntryPage extends StatefulWidget {
  const RegisterEntryPage({super.key});

  @override
  State<RegisterEntryPage> createState() => _RegisterEntryPageState();
}

class _RegisterEntryPageState extends State<RegisterEntryPage>
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

  void _navigateToPage(Widget page) {
    Navigator.of(context).push(MaterialPageRoute(builder: (context) => page));
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;
    final bottomPadding = MediaQuery.of(context).padding.bottom;

    return Consumer<ThemeService>(
      builder: (context, themeService, child) {
        return Scaffold(
          backgroundColor: KyXColors.bg,
          body: SafeArea(
            child: FadeTransition(
              opacity: _fadeAnimation,
              child: SlideTransition(
                position: _slideAnimation,
                child: Padding(
                  padding: EdgeInsets.fromLTRB(20, 12, 20, bottomPadding + 12),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(minHeight: screenHeight - 64),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _buildTopBrand(),
                        const SizedBox(height: 58),
                        _buildLogo(),
                        const SizedBox(height: 18),
                        _buildWelcomeText(),
                        const SizedBox(height: 32),
                        _buildActionButtons(),
                        const Spacer(),
                        _buildFooterText(),
                      ],
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

  Widget _buildTopBrand() {
    return Row(
      children: [
        Text(AppConfig.appName, style: KyXText.title),
        const Spacer(),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: KyXColors.surface,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: KyXColors.line),
          ),
          child: const Text('PROD', style: KyXText.caption),
        ),
      ],
    );
  }

  Widget _buildLogo() {
    return Align(
      alignment: Alignment.centerLeft,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(10),
        child: Image.asset(
          'assets/images/app_logo.png',
          width: 56,
          height: 56,
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => Container(
            width: 56,
            height: 56,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: KyXColors.primary,
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(
              Icons.business_center_outlined,
              color: Colors.white,
              size: 28,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildWelcomeText() {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('企业移动工作台', style: KyXText.pageTitle),
        SizedBox(height: 8),
        Text('统一处理审批、人事、通讯录和内部 IM', style: KyXText.secondary),
      ],
    );
  }

  Widget _buildActionButtons() {
    return ScaleTransition(
      scale: _buttonAnimation,
      child: Column(
        children: [
          SizedBox(
            height: 44,
            child: ElevatedButton.icon(
              onPressed: () => _navigateToPage(const LoginPage()),
              icon: const Icon(Icons.login, size: 18),
              label: const Text('登录'),
              style: kyxPrimaryButtonStyle(),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 42,
            child: OutlinedButton.icon(
              onPressed: () => _navigateToPage(
                const CompanySelectionPage(isLoginMode: false),
              ),
              icon: const Icon(Icons.person_add_alt_outlined, size: 18),
              label: const Text('注册企业账号'),
              style: OutlinedButton.styleFrom(
                foregroundColor: KyXColors.text,
                side: const BorderSide(color: KyXColors.line),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                textStyle: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildButton({
    required String text,
    required VoidCallback onPressed,
    required Color backgroundColor,
    required Color textColor,
    Color? borderColor,
    required IconData icon,
  }) {
    return Container(
      width: double.infinity,
      height: 56,
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(28),
        border: borderColor != null
            ? Border.all(color: borderColor, width: 2)
            : null,
        boxShadow: backgroundColor != Colors.transparent
            ? [
                BoxShadow(
                  color: backgroundColor.withOpacity(0.3),
                  blurRadius: 8,
                  offset: const Offset(0, 4),
                ),
              ]
            : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(28),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: textColor, size: 20),
              const SizedBox(width: 8),
              Text(
                text,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: textColor,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFooterText() {
    return const Text(
      '继续使用即表示同意我们的服务条款和隐私政策',
      style: KyXText.caption,
      textAlign: TextAlign.center,
    );
  }
}
