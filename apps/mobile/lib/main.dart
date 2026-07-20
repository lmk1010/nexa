import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'config/app_config.dart';
import 'services/theme_service.dart';
import 'services/auth_service.dart';
import 'services/app_lifecycle_service.dart';
import 'services/chat_service.dart';
import 'services/notification_service.dart';
import 'pages/splash_page_enhanced.dart';
import 'pages/debug_page.dart';
import 'pages/chat_main_page.dart';
import 'pages/hotel_front_desk_page.dart';
import 'widgets/kyx_design.dart';

/// 应用程序入口点
/// 初始化Flutter应用并设置全局状态管理
Future<void> main() async {
  // 确保Flutter绑定初始化
  WidgetsFlutterBinding.ensureInitialized();
  await NotificationService().initialize();

  // 设置系统UI覆盖样式 - 一体化状态栏
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.white, // 状态栏背景色与应用保持一致
      statusBarIconBrightness: Brightness.dark,
      statusBarBrightness: Brightness.light,
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );

  // 设置首选方向为竖屏
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  runApp(const MyApp());
}

/// 主应用程序组件
/// 负责设置全局状态管理、主题配置和路由管理
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      // 配置全局状态管理提供者
      providers: [
        // 主题服务 - 管理应用的主题切换和UI样式
        ChangeNotifierProvider(create: (context) => ThemeService()),
        // 认证服务 - 管理用户登录状态和认证信息
        ChangeNotifierProvider(create: (context) => AuthService()),
      ],
      child: Consumer<ThemeService>(
        builder: (context, themeService, child) {
          return AppLifecycleService(
            themeService: themeService,
            child: MaterialApp(
              navigatorKey: NotificationService.navigatorKey,
              title: AppConfig.appName,
              // 开发模式配置
              debugShowCheckedModeBanner: false,
              showPerformanceOverlay: false,
              showSemanticsDebugger: false,
              checkerboardRasterCacheImages: false,
              checkerboardOffscreenLayers: false,
              // 路由配置
              routes: {
                '/debug': (context) => const DebugPage(),
                '/chat': (context) {
                  final args = ModalRoute.of(context)?.settings.arguments;
                  final data = args is Map ? args : const {};
                  final conversationId =
                      data['conversationId']?.toString() ??
                      data['userID']?.toString() ??
                      '';
                  final contactName =
                      data['contactName']?.toString() ??
                      data['nickname']?.toString() ??
                      conversationId;
                  return ChangeNotifierProvider.value(
                    value: ChatService(),
                    child: ChatDetailPage(
                      conversationId: conversationId,
                      contactName: contactName,
                      conversationType:
                          data['conversationType']?.toString() ?? 'single',
                    ),
                  );
                },
              },
              onGenerateRoute: (settings) {
                final uri = Uri.tryParse(settings.name ?? '');
                if (uri != null &&
                    uri.pathSegments.length == 3 &&
                    uri.pathSegments[0] == 'hotel' &&
                    uri.pathSegments[1] == 'work-order') {
                  final orderId = int.tryParse(uri.pathSegments[2]);
                  if (orderId != null) {
                    return MaterialPageRoute<void>(
                      settings: settings,
                      builder: (_) => HotelWorkOrderDetailPage(orderId: orderId),
                    );
                  }
                }
                return null;
              },
              // 应用主题配置
              theme: ThemeData(
                colorScheme: ColorScheme.fromSeed(
                  seedColor: KyXColors.primary,
                  primary: KyXColors.primary,
                  surface: KyXColors.surface,
                ),
                useMaterial3: true,
                scaffoldBackgroundColor: KyXColors.bg,
                fontFamily: 'PingFang SC',
                // 配置AppBar主题以支持一体化状态栏
                appBarTheme: const AppBarTheme(
                  backgroundColor: KyXColors.surface,
                  foregroundColor: KyXColors.text,
                  elevation: 0,
                  scrolledUnderElevation: 0,
                  centerTitle: false,
                  titleTextStyle: KyXText.title,
                  systemOverlayStyle: SystemUiOverlayStyle(
                    statusBarColor: Colors.white,
                    statusBarIconBrightness: Brightness.dark,
                    statusBarBrightness: Brightness.light,
                  ),
                ),
                bottomNavigationBarTheme: const BottomNavigationBarThemeData(
                  backgroundColor: KyXColors.surface,
                  selectedItemColor: KyXColors.primary,
                  unselectedItemColor: KyXColors.textTertiary,
                  selectedLabelStyle: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                  ),
                  unselectedLabelStyle: TextStyle(fontSize: 11),
                  type: BottomNavigationBarType.fixed,
                  elevation: 0,
                ),
                elevatedButtonTheme: ElevatedButtonThemeData(
                  style: kyxPrimaryButtonStyle(),
                ),
                textButtonTheme: TextButtonThemeData(
                  style: kyxTextButtonStyle(),
                ),
                inputDecorationTheme: InputDecorationTheme(
                  isDense: true,
                  filled: true,
                  fillColor: KyXColors.surface,
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 12,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: const BorderSide(color: KyXColors.line),
                  ),
                ),
              ),
              // 设置启动页面
              home: const SplashPageEnhanced(),
            ),
          );
        },
      ),
    );
  }
}
