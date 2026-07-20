import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 主题模式枚举
/// 定义应用支持的主题模式类型
enum ThemeMode {
  light, // 浅色主题
  dark, // 深色主题
  system, // 跟随系统主题
}

/// 主题管理服务
/// 负责管理应用的主题切换、颜色配置和系统UI样式
/// 使用单例模式确保全局主题状态一致性
class ThemeService extends ChangeNotifier {
  // 单例模式实现
  static final ThemeService _instance = ThemeService._internal();
  factory ThemeService() => _instance;
  ThemeService._internal();

  /// 当前主题模式
  ThemeMode _currentThemeMode = ThemeMode.system;

  /// 是否为深色模式
  bool _isDarkMode = false;

  /// 获取当前主题模式
  ThemeMode get currentThemeMode => _currentThemeMode;

  /// 获取当前是否为深色模式
  bool get isDarkMode => _isDarkMode;

  /// 获取当前主题的颜色配置
  Map<String, Color> get _currentColors => _isDarkMode ? darkTheme : lightTheme;

  /// 主色调
  Color get primaryColor => _currentColors['primary']!;

  /// 浅色主色调
  Color get primaryLightColor => _currentColors['primaryLight']!;

  /// 深色主色调
  Color get primaryDarkColor => _currentColors['primaryDark']!;

  /// 次要色调
  Color get secondaryColor => _currentColors['secondary']!;

  /// 背景色
  Color get backgroundColor => _currentColors['background']!;

  /// 表面色
  Color get surfaceColor => _currentColors['surface']!;

  /// 表面变体色
  Color get surfaceVariantColor => _currentColors['surfaceVariant']!;

  /// 主色调上的文字色
  Color get onPrimaryColor => _currentColors['onPrimary']!;

  /// 背景上的文字色（主要文字色）
  Color get textPrimaryColor => _currentColors['onBackground']!;

  /// 表面上的文字色
  Color get onSurfaceColor => _currentColors['onSurface']!;

  /// 表面变体上的文字色（次要文字色）
  Color get textSecondaryColor => _currentColors['onSurfaceVariant']!;

  /// 轮廓色
  Color get outlineColor => _currentColors['outline']!;

  /// 轮廓变体色
  Color get outlineVariantColor => _currentColors['outlineVariant']!;

  /// 阴影色
  Color get shadowColor => _currentColors['shadow']!;

  /// 浅色主题配色方案
  /// 定义浅色模式下的所有颜色配置
  static const lightTheme = {
    'primary': Color(0xFF2563EB), // 主色调
    'primaryLight': Color(0xFF3B82F6), // 浅色主色调
    'primaryDark': Color(0xFF1D4ED8), // 深色主色调
    'secondary': Color(0xFF0891B2), // 次要色调
    'background': Color(0xFFF6F8FB), // 背景色
    'surface': Color(0xFFFFFFFF), // 表面色
    'surfaceVariant': Color(0xFFF1F5F9), // 表面变体色
    'onPrimary': Color(0xFFFFFFFF), // 主色调上的文字色
    'onBackground': Color(0xFF111827), // 背景上的文字色
    'onSurface': Color(0xFF111827), // 表面上的文字色
    'onSurfaceVariant': Color(0xFF6B7280), // 表面变体上的文字色
    'outline': Color(0xFFE5E7EB), // 轮廓色
    'outlineVariant': Color(0xFFD1D5DB), // 轮廓变体色
    'shadow': Color(0x00000000), // 阴影色
    'gradientStart': Color(0xFFF6F8FB), // 渐变起始色
    'gradientCenter': Color(0xFFF6F8FB), // 渐变中间色
    'gradientEnd': Color(0xFFFFFFFF), // 渐变结束色
    'logoGradientStart': Color(0xFF2563EB), // Logo渐变起始色
    'logoGradientEnd': Color(0xFF1D4ED8), // Logo渐变结束色
    'textPrimary': Color(0xFF111827), // 主要文字色
    'textSecondary': Color(0xFF6B7280), // 次要文字色
    'textTertiary': Color(0xFF9CA3AF), // 第三级文字色
    'success': Color(0xFF059669), // 成功色
    'error': Color(0xFFDC2626), // 错误色
    'warning': Color(0xFFD97706), // 警告色
    'info': Color(0xFF2563EB), // 信息色
  };

  /// 深色主题配色方案
  /// 定义深色模式下的所有颜色配置
  static const darkTheme = {
    'primary': Color(0xFF4FACFE), // 主色调
    'primaryLight': Color(0xFF00F2FE), // 浅色主色调
    'primaryDark': Color(0xFF667eea), // 深色主色调
    'secondary': Color(0xFF00F2FE), // 次要色调
    'background': Color(0xFF0F0F23), // 背景色
    'surface': Color(0xFF1A1A2E), // 表面色
    'surfaceVariant': Color(0xFF16213E), // 表面变体色
    'onPrimary': Color(0xFFFFFFFF), // 主色调上的文字色
    'onBackground': Color(0xFFFFFFFF), // 背景上的文字色
    'onSurface': Color(0xFFFFFFFF), // 表面上的文字色
    'onSurfaceVariant': Color(0xFFB3B3B3), // 表面变体上的文字色
    'outline': Color(0xFF2D3748), // 轮廓色
    'outlineVariant': Color(0xFF4A5568), // 轮廓变体色
    'shadow': Color(0xFF000000), // 阴影色
    'gradientStart': Color(0xFF1a1a2e), // 渐变起始色
    'gradientCenter': Color(0xFF16213e), // 渐变中间色
    'gradientEnd': Color(0xFF0f3460), // 渐变结束色
    'logoGradientStart': Color(0xFF4FACFE), // Logo渐变起始色
    'logoGradientEnd': Color(0xFF00F2FE), // Logo渐变结束色
    'textPrimary': Color(0xFFFFFFFF), // 主要文字色
    'textSecondary': Color(0xFFB3B3B3), // 次要文字色
    'textTertiary': Color(0xFF808080), // 第三级文字色
    'success': Color(0xFF10B981), // 成功色
    'error': Color(0xFFEF4444), // 错误色
    'warning': Color(0xFFF59E0B), // 警告色
    'info': Color(0xFF4FACFE), // 信息色
  };

  /// 获取当前主题的配色方案
  /// 根据当前是否为深色模式返回对应的颜色配置
  Map<String, Color> get currentTheme => _isDarkMode ? darkTheme : lightTheme;

  /// 初始化主题服务
  /// 从本地存储读取保存的主题模式并应用
  Future<void> initialize() async {
    final prefs = await SharedPreferences.getInstance();
    final savedThemeMode = prefs.getString('theme_mode');

    if (savedThemeMode != null) {
      _currentThemeMode = ThemeMode.values.firstWhere(
        (e) => e.toString() == savedThemeMode,
        orElse: () => ThemeMode.system,
      );
    }

    _updateTheme();
  }

  /// 设置主题模式
  /// [mode] 要设置的主题模式
  /// 保存设置到本地存储并通知监听者
  Future<void> setThemeMode(ThemeMode mode) async {
    if (_currentThemeMode != mode) {
      _currentThemeMode = mode;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('theme_mode', mode.toString());
      _updateTheme();
      notifyListeners();
    }
  }

  /// 切换明暗模式
  /// 在浅色和深色模式之间切换
  void toggleTheme() {
    _isDarkMode = !_isDarkMode;
    notifyListeners();
  }

  /// 更新主题状态
  /// 根据当前主题模式设置深色模式状态
  void _updateTheme() {
    switch (_currentThemeMode) {
      case ThemeMode.light:
        _isDarkMode = false;
        break;
      case ThemeMode.dark:
        _isDarkMode = true;
        break;
      case ThemeMode.system:
        // 业务类移动端默认保持浅色，避免系统深色导致页面风格不一致。
        _isDarkMode = false;
        break;
    }
  }

  /// 获取系统UI样式
  /// 根据当前主题模式返回对应的系统UI配置
  SystemUiOverlayStyle get systemUiStyle {
    return _isDarkMode
        ? const SystemUiOverlayStyle(
            statusBarColor: Color(0xFF1E293B), // 深色模式使用深色背景
            statusBarIconBrightness: Brightness.light,
            statusBarBrightness: Brightness.dark,
            systemNavigationBarColor: Color(0xFF1E293B),
            systemNavigationBarIconBrightness: Brightness.light,
          )
        : const SystemUiOverlayStyle(
            statusBarColor: Colors.white, // 浅色模式使用白色背景
            statusBarIconBrightness: Brightness.dark,
            statusBarBrightness: Brightness.light,
            systemNavigationBarColor: Colors.white,
            systemNavigationBarIconBrightness: Brightness.dark,
          );
  }

  /// 获取恢复的系统UI样式
  /// 用于应用从后台恢复时的系统UI配置
  SystemUiOverlayStyle get restoreSystemUiStyle {
    return _isDarkMode
        ? const SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: Brightness.dark,
            statusBarBrightness: Brightness.light,
            systemNavigationBarColor: Colors.transparent,
            systemNavigationBarIconBrightness: Brightness.dark,
          )
        : const SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: Brightness.light,
            statusBarBrightness: Brightness.dark,
            systemNavigationBarColor: Colors.transparent,
            systemNavigationBarIconBrightness: Brightness.light,
          );
  }

  /// 重新设置系统UI样式
  /// 用于应用从后台恢复时重新应用一体化状态栏
  void refreshSystemUiStyle() {
    SystemUiOverlayStyle style = _isDarkMode
        ? const SystemUiOverlayStyle(
            statusBarColor: Color(0xFF1E293B), // 深色模式使用深色背景
            statusBarIconBrightness: Brightness.light,
            statusBarBrightness: Brightness.dark,
            systemNavigationBarColor: Color(0xFF1E293B),
            systemNavigationBarIconBrightness: Brightness.light,
          )
        : const SystemUiOverlayStyle(
            statusBarColor: Colors.white, // 浅色模式使用白色背景
            statusBarIconBrightness: Brightness.dark,
            statusBarBrightness: Brightness.light,
            systemNavigationBarColor: Colors.white,
            systemNavigationBarIconBrightness: Brightness.dark,
          );

    // 直接设置样式，不使用沉浸式模式
    SystemChrome.setSystemUIOverlayStyle(style);
  }
}
