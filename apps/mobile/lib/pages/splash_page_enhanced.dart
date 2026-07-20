import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:animated_text_kit/animated_text_kit.dart';
import 'package:lottie/lottie.dart';
import 'package:provider/provider.dart';
import 'dart:async';
import 'dart:math';
import 'login_page.dart';
import 'chat_main_page.dart';
import 'debug_page.dart';
import '../config/app_config.dart';
import '../services/theme_service.dart';
import '../services/auth_service.dart';
import '../services/app_update_service.dart';
import '../widgets/immersive_wrapper.dart';
import '../widgets/kyx_design.dart';

/// 增强版启动页面
/// 提供丰富的动画效果和用户体验
/// 包含Logo动画、粒子效果、进度条和自动导航功能
class SplashPageEnhanced extends StatefulWidget {
  const SplashPageEnhanced({super.key});

  @override
  State<SplashPageEnhanced> createState() => _SplashPageEnhancedState();
}

/// 启动页面状态管理类
/// 管理多个动画控制器和页面生命周期
class _SplashPageEnhancedState extends State<SplashPageEnhanced>
    with TickerProviderStateMixin {
  // 动画控制器
  late AnimationController _logoController; // Logo缩放动画控制器
  late AnimationController _fadeController; // 淡入动画控制器
  late AnimationController _scaleController; // 缩放动画控制器
  late AnimationController _particleController; // 粒子动画控制器

  // 动画对象
  late Animation<double> _logoAnimation; // Logo缩放动画
  late Animation<double> _fadeAnimation; // 淡入动画
  late Animation<double> _scaleAnimation; // 缩放动画

  /// 进度条进度值 (0.0 - 1.0)
  double _progress = 0.0;

  /// 进度条定时器
  Timer? _progressTimer;

  /// 粒子列表
  final List<Particle> _particles = [];

  /// 随机数生成器
  final Random _random = Random();

  final AppUpdateService _appUpdateService = AppUpdateService();
  late SystemUiOverlayStyle _restoreSystemUiStyle;
  bool _isNavigating = false;

  @override
  void initState() {
    super.initState();

    // 设置沉浸式状态栏
    final themeService = Provider.of<ThemeService>(context, listen: false);
    _restoreSystemUiStyle = themeService.restoreSystemUiStyle;
    SystemChrome.setSystemUIOverlayStyle(themeService.systemUiStyle);

    // 设置系统UI模式为状态栏和导航栏都可见
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.edgeToEdge,
      overlays: [SystemUiOverlay.top, SystemUiOverlay.bottom],
    );

    // 初始化粒子效果
    _initParticles();

    // 初始化动画控制器
    _initAnimationControllers();

    // 启动动画序列
    _startAnimationSequence();
  }

  /// 初始化粒子效果
  /// 创建随机分布的粒子对象
  void _initParticles() {
    final themeService = Provider.of<ThemeService>(context, listen: false);
    final theme = themeService.currentTheme;
    for (int i = 0; i < 30; i++) {
      _particles.add(
        Particle(
          x: _random.nextDouble() * 500,
          y: _random.nextDouble() * 1000,
          size: _random.nextDouble() * 6 + 1,
          speed: _random.nextDouble() * 3 + 0.5,
          color: theme['primary']!.withValues(
            alpha: _random.nextDouble() * 0.6 + 0.1,
          ),
        ),
      );
    }
  }

  /// 初始化动画控制器
  /// 设置各个动画控制器的持续时间和动画曲线
  void _initAnimationControllers() {
    // Logo动画控制器 - 弹性效果
    _logoController = AnimationController(
      duration: const Duration(milliseconds: 2000),
      vsync: this,
    );

    // 淡入动画控制器 - 平滑过渡
    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 1200),
      vsync: this,
    );

    // 缩放动画控制器 - 弹跳效果
    _scaleController = AnimationController(
      duration: const Duration(milliseconds: 1000),
      vsync: this,
    );

    // 粒子动画控制器 - 循环动画
    _particleController = AnimationController(
      duration: const Duration(milliseconds: 3000),
      vsync: this,
    );

    // 设置动画曲线
    _logoAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _logoController, curve: Curves.elasticOut),
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _fadeController, curve: Curves.easeInOut),
    );

    _scaleAnimation = Tween<double>(begin: 0.3, end: 1.0).animate(
      CurvedAnimation(parent: _scaleController, curve: Curves.bounceOut),
    );
  }

  /// 启动动画序列
  /// 按顺序执行各个动画效果
  void _startAnimationSequence() async {
    // 启动粒子动画（循环播放）
    _particleController.repeat();

    // 启动Logo动画
    _logoController.forward();

    // 延迟启动淡入动画
    await Future.delayed(const Duration(milliseconds: 800));
    _fadeController.forward();

    // 延迟启动缩放动画
    await Future.delayed(const Duration(milliseconds: 500));
    _scaleController.forward();

    // 启动进度条动画
    _startProgressAnimation();
  }

  /// 启动进度条动画
  /// 模拟加载进度并最终触发页面导航
  void _startProgressAnimation() {
    _progressTimer = Timer.periodic(const Duration(milliseconds: 40), (timer) {
      setState(() {
        _progress += 0.015;
        if (_progress >= 1.0) {
          _progress = 1.0;
          timer.cancel();
          _navigateToLogin();
        }
      });
    });
  }

  /// 导航到登录页面
  /// 检查用户登录状态并导航到相应页面
  void _navigateToLogin() async {
    if (_isNavigating) return;
    _isNavigating = true;

    final canContinue = await _handleAppUpdateIfNeeded();
    if (!canContinue) {
      _isNavigating = false;
      return;
    }
    if (!mounted) return;

    // 使用AuthService检查登录状态
    final authService = Provider.of<AuthService>(context, listen: false);
    await authService.initialize();

    if (kDebugMode) {
      print('启动时检查登录状态: ${authService.isLoggedIn}');
      if (authService.isLoggedIn) {
        print('已登录用户ID: ${authService.getUserId()}');
      }
    }

    Widget targetPage;
    if (authService.isLoggedIn) {
      // 如果已登录，直接跳转到聊天主界面
      targetPage = const ChatMainPage();
    } else {
      // 如果未登录，直接进入账号登录页
      targetPage = const LoginPage();
    }

    if (mounted) {
      Navigator.of(context).pushReplacement(
        PageRouteBuilder(
          pageBuilder: (context, animation, secondaryAnimation) => targetPage,
          transitionsBuilder: (context, animation, secondaryAnimation, child) {
            return FadeTransition(opacity: animation, child: child);
          },
          transitionDuration: const Duration(milliseconds: 220),
        ),
      );
    }
  }

  Future<bool> _handleAppUpdateIfNeeded() async {
    final updateInfo = await _appUpdateService.checkForUpdate();
    if (!mounted || updateInfo == null) return true;
    return await _showUpdateDialog(updateInfo);
  }

  Future<bool> _showUpdateDialog(AppUpdateInfo info) async {
    final savedSnapshot = await _appUpdateService.getSavedDownloadSnapshot(
      info,
    );
    if (!mounted) return true;

    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: !info.forceUpdate,
      builder: (context) {
        var downloading = false;
        var progress = savedSnapshot?.progress ?? 0.0;
        var receivedBytes = savedSnapshot?.receivedBytes ?? 0;
        var totalBytes = savedSnapshot?.totalBytes ?? info.fileSize ?? 0;
        String? statusText = savedSnapshot?.isActive == true
            ? '后台下载中，返回后会继续'
            : savedSnapshot?.isSuccessful == true
            ? '安装包已下载'
            : null;
        var statusIsError = false;
        var autoStarted = false;

        return StatefulBuilder(
          builder: (context, setDialogState) {
            Future<void> startUpdate() async {
              if (downloading) return;
              setDialogState(() {
                downloading = true;
                if (progress <= 0 || progress >= 1) {
                  progress = savedSnapshot?.isSuccessful == true ? 1 : 0;
                }
                receivedBytes = savedSnapshot?.receivedBytes ?? 0;
                totalBytes =
                    savedSnapshot?.totalBytes ?? info.fileSize ?? totalBytes;
                statusText = '后台下载中，切到其它页面后会继续';
                statusIsError = false;
              });

              try {
                final status = await _appUpdateService.downloadAndInstall(
                  info,
                  onProgress: (value) {
                    if (!context.mounted) return;
                    setDialogState(() {
                      progress = value;
                    });
                  },
                  onSnapshot: (snapshot) {
                    if (!context.mounted) return;
                    setDialogState(() {
                      progress = snapshot.progress;
                      receivedBytes = snapshot.receivedBytes;
                      totalBytes = snapshot.totalBytes > 0
                          ? snapshot.totalBytes
                          : totalBytes;
                    });
                  },
                  onStatus: (value) {
                    if (!context.mounted) return;
                    setDialogState(() {
                      statusText = value;
                      statusIsError = false;
                    });
                  },
                );
                if (!context.mounted) return;
                if (status == 'permission_required') {
                  setDialogState(() {
                    downloading = false;
                    statusText = '请允许安装权限后再次更新';
                    statusIsError = true;
                  });
                  return;
                }
                Navigator.of(context).pop(!info.forceUpdate);
              } catch (e) {
                if (!context.mounted) return;
                setDialogState(() {
                  downloading = false;
                  statusText = e is AppUpdateException ? e.message : '更新失败';
                  statusIsError = true;
                });
              }
            }

            if (!autoStarted && savedSnapshot?.isActive == true) {
              autoStarted = true;
              WidgetsBinding.instance.addPostFrameCallback((_) {
                if (context.mounted) {
                  startUpdate();
                }
              });
            }

            final notes = info.releaseNotes?.trim();
            final progressPercent = ((progress * 100).clamp(0, 100)).round();
            final byteText = _formatUpdateBytes(receivedBytes, totalBytes);
            final progressText = progress <= 0
                ? '后台下载中${byteText.isNotEmpty ? ' · $byteText' : ''}'
                : '已下载 $progressPercent%${byteText.isNotEmpty ? ' · $byteText' : ''}';
            return Dialog(
              insetPadding: const EdgeInsets.symmetric(horizontal: 28),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              child: Container(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
                decoration: BoxDecoration(
                  color: KyXColors.surface,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 38,
                          height: 38,
                          decoration: BoxDecoration(
                            color: KyXColors.primary.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(9),
                          ),
                          child: const Icon(
                            Icons.system_update_alt,
                            color: KyXColors.primary,
                            size: 21,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('发现新版本', style: KyXText.title),
                              const SizedBox(height: 3),
                              Text(
                                'v${info.latestVersionName}',
                                style: KyXText.secondary,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    if (notes != null && notes.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      Text(
                        notes,
                        maxLines: 5,
                        overflow: TextOverflow.ellipsis,
                        style: KyXText.body,
                      ),
                    ],
                    if (downloading) ...[
                      const SizedBox(height: 18),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(2),
                        child: LinearProgressIndicator(
                          minHeight: 4,
                          value: progress <= 0 ? null : progress,
                          color: KyXColors.primary,
                          backgroundColor: KyXColors.lineSoft,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(progressText, style: KyXText.caption),
                    ],
                    if (statusText != null) ...[
                      const SizedBox(height: 14),
                      Text(
                        statusText!,
                        style: KyXText.secondary.copyWith(
                          color: statusIsError
                              ? KyXColors.red
                              : KyXColors.textSecondary,
                        ),
                      ),
                    ],
                    const SizedBox(height: 18),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        if (!info.forceUpdate && !downloading)
                          TextButton(
                            onPressed: () => Navigator.of(context).pop(true),
                            child: const Text('稍后'),
                          ),
                        const SizedBox(width: 8),
                        FilledButton(
                          onPressed: downloading ? null : startUpdate,
                          style: FilledButton.styleFrom(
                            backgroundColor: KyXColors.primary,
                            foregroundColor: Colors.white,
                            minimumSize: const Size(92, 40),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ),
                          child: Text(
                            savedSnapshot?.isSuccessful == true
                                ? '安装'
                                : progress > 0
                                ? '继续更新'
                                : info.forceUpdate
                                ? '立即更新'
                                : '更新',
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
      },
    );
    return result ?? !info.forceUpdate;
  }

  String _formatUpdateBytes(int receivedBytes, int totalBytes) {
    if (totalBytes <= 0) return '';
    final received = receivedBytes.clamp(0, totalBytes).toDouble();
    final total = totalBytes.toDouble();
    return '${_formatByteSize(received)} / ${_formatByteSize(total)}';
  }

  String _formatByteSize(double bytes) {
    const units = ['B', 'KB', 'MB', 'GB'];
    var value = bytes;
    var unitIndex = 0;
    while (value >= 1024 && unitIndex < units.length - 1) {
      value = value / 1024;
      unitIndex++;
    }
    final digits = unitIndex == 0 || value >= 10 ? 0 : 1;
    return '${value.toStringAsFixed(digits)} ${units[unitIndex]}';
  }

  @override
  void dispose() {
    // 释放动画控制器资源
    _logoController.dispose();
    _fadeController.dispose();
    _scaleController.dispose();
    _particleController.dispose();
    _progressTimer?.cancel();

    // 恢复系统UI设置。dispose 时 context 可能已失效，不能再通过 Provider 取服务。
    SystemChrome.setSystemUIOverlayStyle(_restoreSystemUiStyle);

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;
    final bottomPadding = MediaQuery.of(context).padding.bottom;

    return Consumer<ThemeService>(
      builder: (context, themeService, child) {
        final theme = themeService.currentTheme;

        return ImmersiveWrapper(
          themeService: themeService,
          child: Scaffold(
            backgroundColor: KyXColors.bg,
            body: Stack(
              children: [
                Positioned.fill(child: Container(color: KyXColors.bg)),

                // 调试入口按钮（仅开发模式）
                if (kDebugMode)
                  Positioned(
                    top: topPadding + 20,
                    right: 20,
                    child: AnimatedBuilder(
                      animation: _fadeAnimation,
                      builder: (context, child) {
                        return Opacity(
                          opacity: _fadeAnimation.value * 0.7,
                          child: GestureDetector(
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => const DebugPage(),
                                ),
                              );
                            },
                            child: Container(
                              width: 50,
                              height: 50,
                              decoration: BoxDecoration(
                                color: KyXColors.surface,
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(color: KyXColors.line),
                              ),
                              child: Icon(
                                Icons.bug_report,
                                color: theme['textSecondary'],
                                size: 20,
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),

                // 主要内容
                Padding(
                  padding: EdgeInsets.only(bottom: bottomPadding),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      // Logo区域
                      Expanded(
                        flex: 3,
                        child: Center(
                          child: AnimatedBuilder(
                            animation: _logoAnimation,
                            builder: (context, child) {
                              return Transform.scale(
                                scale: _logoAnimation.value,
                                child: Container(
                                  width: 72,
                                  height: 72,
                                  clipBehavior: Clip.antiAlias,
                                  decoration: BoxDecoration(
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Image.asset(
                                    'assets/images/app_logo.png',
                                    fit: BoxFit.cover,
                                    errorBuilder:
                                        (context, error, stackTrace) =>
                                            Container(
                                              color: KyXColors.primary,
                                              child: const Icon(
                                                Icons.business_center,
                                                size: 34,
                                                color: Colors.white,
                                              ),
                                            ),
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                      ),

                      // 应用名称
                      Expanded(
                        flex: 1,
                        child: AnimatedBuilder(
                          animation: _fadeAnimation,
                          builder: (context, child) {
                            return Opacity(
                              opacity: _fadeAnimation.value,
                              child: Column(
                                children: [
                                  Text(
                                    AppConfig.appName,
                                    style: TextStyle(
                                      fontSize: 24,
                                      fontWeight: FontWeight.w800,
                                      color: theme['textPrimary'],
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  AnimatedTextKit(
                                    animatedTexts: [
                                      TypewriterAnimatedText(
                                        '新一代工作平台',
                                        textStyle: TextStyle(
                                          fontSize: 13,
                                          color: theme['textSecondary'],
                                          fontWeight: FontWeight.w500,
                                        ),
                                        speed: const Duration(milliseconds: 80),
                                      ),
                                    ],
                                    totalRepeatCount: 1,
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                      ),

                      // 进度条区域
                      Expanded(
                        flex: 1,
                        child: AnimatedBuilder(
                          animation: _scaleAnimation,
                          builder: (context, child) {
                            return Transform.scale(
                              scale: _scaleAnimation.value,
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 50,
                                ),
                                child: Column(
                                  children: [
                                    // Lottie动画
                                    SizedBox(
                                      width: 32,
                                      height: 32,
                                      child: Lottie.asset(
                                        'assets/animations/loading.json',
                                        fit: BoxFit.contain,
                                      ),
                                    ),
                                    const SizedBox(height: 16),

                                    // 进度条
                                    Container(
                                      height: 3,
                                      decoration: BoxDecoration(
                                        borderRadius: BorderRadius.circular(2),
                                        color: KyXColors.line,
                                      ),
                                      child: FractionallySizedBox(
                                        alignment: Alignment.centerLeft,
                                        widthFactor: _progress,
                                        child: Container(
                                          decoration: BoxDecoration(
                                            borderRadius: BorderRadius.circular(
                                              2,
                                            ),
                                            color: KyXColors.primary,
                                          ),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: 10),
                                    Text(
                                      '${(_progress * 100).toInt()}%',
                                      style: TextStyle(
                                        color: theme['textSecondary'],
                                        fontSize: 12,
                                        fontWeight: FontWeight.w500,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      ),

                      SizedBox(height: bottomPadding + 20),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// 粒子类
/// 定义粒子的基本属性和行为
class Particle {
  /// 粒子X坐标
  double x;

  /// 粒子Y坐标
  double y;

  /// 粒子大小
  double size;

  /// 粒子移动速度
  double speed;

  /// 粒子颜色
  Color color;

  /// 粒子角度（用于旋转效果）
  double angle = 0;

  Particle({
    required this.x,
    required this.y,
    required this.size,
    required this.speed,
    required this.color,
  });

  /// 更新粒子位置
  /// 实现粒子的移动和循环效果
  void update() {
    angle += speed * 0.02;
    y -= speed;
    if (y < -10) {
      y = 1010; // 根据屏幕高度调整
    }
  }
}

/// 粒子绘制器
/// 负责在画布上绘制粒子效果
class ParticlePainter extends CustomPainter {
  /// 粒子列表
  final List<Particle> particles;

  /// 动画值（用于控制透明度）
  final double animationValue;

  ParticlePainter(this.particles, this.animationValue);

  @override
  void paint(Canvas canvas, Size size) {
    for (var particle in particles) {
      particle.update();

      // 确保粒子在屏幕范围内
      if (particle.x >= 0 &&
          particle.x <= size.width &&
          particle.y >= 0 &&
          particle.y <= size.height) {
        final paint = Paint()
          ..color = particle.color.withValues(alpha: animationValue)
          ..style = PaintingStyle.fill;

        canvas.drawCircle(Offset(particle.x, particle.y), particle.size, paint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
