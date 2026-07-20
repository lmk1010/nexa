import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../config/app_config.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/theme_service.dart';
import '../widgets/kyx_design.dart';
import 'chat_main_page.dart';

class TenantSelectionPage extends StatefulWidget {
  final AdminPreLoginResult preLoginResult;

  const TenantSelectionPage({super.key, required this.preLoginResult});

  @override
  State<TenantSelectionPage> createState() => _TenantSelectionPageState();
}

class _TenantSelectionPageState extends State<TenantSelectionPage> {
  bool _isLoading = false;
  int? _loadingTenantId;
  late final ThemeService _themeService;

  @override
  void initState() {
    super.initState();
    _themeService = Provider.of<ThemeService>(context, listen: false);
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom],
    );
  }

  @override
  void dispose() {
    _themeService.refreshSystemUiStyle();
    super.dispose();
  }

  SystemUiOverlayStyle _overlayStyle(bool isDarkMode) {
    return SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: isDarkMode ? Brightness.light : Brightness.dark,
      statusBarBrightness: isDarkMode ? Brightness.dark : Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarDividerColor: Colors.transparent,
      systemNavigationBarIconBrightness: isDarkMode
          ? Brightness.light
          : Brightness.dark,
    );
  }

  int get _availableCount => widget.preLoginResult.tenantList
      .where((tenant) => tenant.isAvailable)
      .length;

  @override
  Widget build(BuildContext context) {
    return Consumer<ThemeService>(
      builder: (context, themeService, child) {
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: _overlayStyle(themeService.isDarkMode),
          child: Scaffold(
            backgroundColor: const Color(0xFFF8FAFC),
            body: SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _buildBusinessTopBar(themeService),
                  _buildBusinessHero(),
                  _buildBusinessSectionHeader(),
                  Expanded(
                    child: Container(
                      color: KyXColors.surface,
                      child: ListView.builder(
                        physics: const BouncingScrollPhysics(),
                        itemCount: widget.preLoginResult.tenantList.length,
                        itemBuilder: (context, index) {
                          final tenant =
                              widget.preLoginResult.tenantList[index];
                          return _buildBusinessTenantRow(
                            tenant,
                            showDivider:
                                index !=
                                widget.preLoginResult.tenantList.length - 1,
                          );
                        },
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildBusinessTopBar(ThemeService themeService) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 8, 8, 10),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back_ios_new, size: 20),
            color: KyXColors.text,
            onPressed: () => Navigator.of(context).pop(),
            tooltip: '返回',
          ),
          Expanded(
            child: Text(
              AppConfig.appName,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: KyXColors.text,
                fontSize: 20,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          IconButton(
            icon: Icon(
              themeService.isDarkMode
                  ? Icons.light_mode_outlined
                  : Icons.dark_mode_outlined,
              size: 20,
            ),
            color: KyXColors.textSecondary,
            onPressed: () => setState(themeService.toggleTheme),
            tooltip: '切换主题',
          ),
        ],
      ),
    );
  }

  Widget _buildBusinessHero() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 22, 20, 22),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '选择工作空间',
            style: TextStyle(
              color: KyXColors.text,
              fontSize: 23,
              height: 1.2,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _availableCount > 1 ? '请选择本次进入的企业工作台' : '确认后进入企业工作台',
            style: KyXText.secondary,
          ),
        ],
      ),
    );
  }

  Widget _buildBusinessSectionHeader() {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 9),
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(top: BorderSide(color: KyXColors.lineSoft)),
      ),
      child: Text('工作空间', style: KyXText.section.copyWith(fontSize: 13)),
    );
  }

  Widget _buildBusinessTenantRow(
    TenantOption tenant, {
    required bool showDivider,
  }) {
    final isLoading = _isLoading && _loadingTenantId == tenant.tenantId;
    final available = tenant.isAvailable;
    final warningText = tenant.expired
        ? '租户已过期'
        : (!tenant.hasRole ? '当前账号暂无权限' : null);

    return Column(
      children: [
        InkWell(
          onTap: (!available || _isLoading)
              ? null
              : () => _selectTenant(tenant),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 11, 16, 11),
            child: Row(
              children: [
                _buildBusinessTenantAvatar(tenant),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              tenant.tenantName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                color: KyXColors.text,
                                fontSize: 15,
                                height: 1.2,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                          if (tenant.isDefault) ...[
                            const SizedBox(width: 8),
                            _buildBusinessPill('默认', KyXColors.primary),
                          ],
                        ],
                      ),
                      const SizedBox(height: 5),
                      Text(
                        warningText ?? '企业工作台',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: KyXText.secondary,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                SizedBox(
                  width: 56,
                  height: 30,
                  child: ElevatedButton(
                    onPressed: (!available || _isLoading)
                        ? null
                        : () => _selectTenant(tenant),
                    style:
                        kyxPrimaryButtonStyle(
                          color: available
                              ? KyXColors.primary
                              : KyXColors.slate,
                        ).copyWith(
                          minimumSize: const WidgetStatePropertyAll(
                            Size(56, 30),
                          ),
                          padding: const WidgetStatePropertyAll(
                            EdgeInsets.zero,
                          ),
                          textStyle: const WidgetStatePropertyAll(
                            TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          shape: const WidgetStatePropertyAll(
                            RoundedRectangleBorder(
                              borderRadius: BorderRadius.all(
                                Radius.circular(7),
                              ),
                            ),
                          ),
                        ),
                    child: isLoading
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                Colors.white,
                              ),
                            ),
                          )
                        : Text(available ? '进入' : '不可用'),
                  ),
                ),
              ],
            ),
          ),
        ),
        if (showDivider)
          const Divider(height: 1, indent: 72, color: KyXColors.lineSoft),
      ],
    );
  }

  Widget _buildBusinessTenantAvatar(TenantOption tenant) {
    final name = tenant.tenantName.trim();
    return KyXAvatar(
      text: name.isEmpty ? '企' : name,
      color: tenant.isDefault ? KyXColors.primary : KyXColors.cyan,
    );
  }

  Widget _buildBusinessPill(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }

  Future<void> _selectTenant(TenantOption tenant) async {
    final tenantId = tenant.tenantId;
    if (tenantId == null) return;

    setState(() {
      _isLoading = true;
      _loadingTenantId = tenantId;
    });

    final authService = Provider.of<AuthService>(context, listen: false);

    try {
      final loginCert = await ApiService.tenantLogin(
        preAuthToken: widget.preLoginResult.preAuthToken,
        tenantId: tenantId,
        deviceType: widget.preLoginResult.deviceType,
        deviceId: widget.preLoginResult.deviceId,
      );

      await authService.saveLoginCertificate(loginCert.toJson());

      if (!mounted) return;
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (context) => const ChatMainPage()),
        (route) => false,
      );
    } catch (e) {
      if (!mounted) return;
      final theme = Provider.of<ThemeService>(
        context,
        listen: false,
      ).currentTheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('进入租户失败: $e'), backgroundColor: theme['error']),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _loadingTenantId = null;
        });
      }
    }
  }
}
