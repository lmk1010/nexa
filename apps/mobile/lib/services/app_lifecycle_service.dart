import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'theme_service.dart';

/// 应用生命周期管理服务
/// 负责监听应用生命周期状态变化并执行相应的处理
/// 主要用于处理应用从后台恢复时的UI状态重置
class AppLifecycleService extends StatefulWidget {
  /// 子组件
  final Widget child;
  
  /// 主题服务实例
  final ThemeService themeService;

  const AppLifecycleService({
    super.key,
    required this.child,
    required this.themeService,
  });

  @override
  State<AppLifecycleService> createState() => _AppLifecycleServiceState();
}

/// 应用生命周期管理服务的状态类
/// 实现WidgetsBindingObserver接口来监听应用生命周期变化
class _AppLifecycleServiceState extends State<AppLifecycleService>
    with WidgetsBindingObserver {
  
  @override
  void initState() {
    super.initState();
    // 注册生命周期观察者
    WidgetsBinding.instance.addObserver(this);
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
    
    // 当应用从后台恢复时，重新设置沉浸式状态栏
    if (state == AppLifecycleState.resumed) {
      // 延迟一点时间确保UI已经完全恢复
      Future.delayed(const Duration(milliseconds: 100), () {
        if (mounted) {
          widget.themeService.refreshSystemUiStyle();
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return widget.child;
  }
} 