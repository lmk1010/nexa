import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/theme_service.dart';

/// 一体化包装器组件
/// 用于包装页面内容，提供一体化状态栏体验
/// 监听应用生命周期变化，确保状态栏样式的一致性
class ImmersiveWrapper extends StatefulWidget {
  /// 子组件
  final Widget child;
  
  /// 主题服务实例
  final ThemeService themeService;

  const ImmersiveWrapper({
    super.key,
    required this.child,
    required this.themeService,
  });

  @override
  State<ImmersiveWrapper> createState() => _ImmersiveWrapperState();
}

/// 一体化包装器状态管理类
/// 实现WidgetsBindingObserver接口来监听应用生命周期
class _ImmersiveWrapperState extends State<ImmersiveWrapper>
    with WidgetsBindingObserver {
  
  @override
  void initState() {
    super.initState();
    // 注册生命周期观察者
    WidgetsBinding.instance.addObserver(this);
    
    // 初始化时设置一体化状态栏
    _setUnifiedMode();
  }

  @override
  void dispose() {
    // 移除生命周期观察者
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// 应用生命周期状态变化回调
  /// [state] 当前的应用生命周期状态
  /// 当应用从后台恢复时，重新设置沉浸式状态栏
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    
    // 当应用从后台恢复时，重新设置一体化状态栏
    if (state == AppLifecycleState.resumed) {
      // 延迟一点时间确保UI已经完全恢复
      Future.delayed(const Duration(milliseconds: 200), () {
        if (mounted) {
          _setUnifiedMode();
        }
      });
    }
  }

  /// 设置一体化模式
  /// 配置系统UI为一体化显示，状态栏与应用背景色保持一致
  void _setUnifiedMode() {
    // 直接设置系统UI样式，不使用沉浸式模式
    SystemChrome.setSystemUIOverlayStyle(widget.themeService.systemUiStyle);
  }

  @override
  Widget build(BuildContext context) {
    return widget.child;
  }
}