import 'package:flutter/foundation.dart';

/// 应用配置类
/// 集中管理应用的所有配置参数
/// 包括环境配置、API配置、超时设置等
class AppConfig {
  static const String appName = 'Nexa企业助手';

  /// 环境配置
  static const String _appEnv = String.fromEnvironment(
    'APP_ENV',
    defaultValue: '',
  );

  static const String _baseUrlOverride = String.fromEnvironment(
    'BASE_URL',
    defaultValue: '',
  );

  static const String _enableImOverride = String.fromEnvironment(
    'ENABLE_IM',
    defaultValue: '',
  );

  static const String _deviceTypeOverride = String.fromEnvironment(
    'DEVICE_TYPE',
    defaultValue: '',
  );

  static const String _socialLoginOriginOverride = String.fromEnvironment(
    'SOCIAL_LOGIN_ORIGIN',
    defaultValue: '',
  );

  static const String _appChannelOverride = String.fromEnvironment(
    'APP_CHANNEL',
    defaultValue: '',
  );

  /// 是否为生产环境
  static bool get isProduction => _appEnv == 'prod' || kReleaseMode;

  /// 是否为开发环境
  static bool get isDevelopment => !isProduction;

  /// API配置
  /// 开发环境默认指向本地 nexa-gateway :48080（可用 --dart-define=BASE_URL= 覆盖）
  static const String devBaseUrl = 'http://127.0.0.1:48080';

  /// 生产环境API基础地址（部署 nexa gateway 后替换）
  static const String prodBaseUrl = 'http://127.0.0.1:48080';

  /// 获取当前环境的baseUrl
  /// 根据编译模式自动选择对应的API地址
  static String get baseUrl {
    if (_baseUrlOverride.isNotEmpty) return _baseUrlOverride;
    return isProduction ? prodBaseUrl : devBaseUrl;
  }

  static String get socialLoginOrigin {
    if (_socialLoginOriginOverride.isNotEmpty) {
      return _socialLoginOriginOverride.replaceAll(RegExp(r'/+$'), '');
    }
    return baseUrl.replaceAll(RegExp(r'/+$'), '');
  }

  static bool get enableIm {
    if (_enableImOverride.isNotEmpty) {
      final normalized = _enableImOverride.toLowerCase();
      return normalized == 'true' || normalized == '1';
    }
    return true;
  }

  static String get deviceTypeOverride =>
      _deviceTypeOverride.trim().toUpperCase();

  static String get appChannel {
    if (_appChannelOverride.trim().isNotEmpty) {
      return _appChannelOverride.trim().toLowerCase();
    }
    return isProduction ? 'prod' : 'dev';
  }

  /// 客户端信息
  /// 客户端类型标识
  static const String clientType = 'mobile';

  /// 客户端版本号
  static const String clientVersion = '1.0.0';

  /// 用户代理字符串
  static const String userAgent = 'KuaiYiXiu-OA/1.0.0';

  /// 验证码配置
  /// 默认验证码（用于开发测试）
  static const String defaultVerificationCode = '666666';

  /// 超时配置
  /// 连接超时时间（毫秒）
  static const int connectionTimeout = 30000; // 30秒

  /// 接收超时时间（毫秒）
  static const int receiveTimeout = 30000; // 30秒

  /// 重试配置
  /// 最大重试次数
  static const int maxRetries = 3;

  /// 重试延迟时间（毫秒）
  static const int retryDelay = 1000; // 1秒

  /// 日志配置
  /// 是否启用API日志记录
  static const bool enableApiLogging = true;

  /// 安全配置
  /// 默认请求头
  /// 包含内容类型、用户代理、客户端信息等
  static const Map<String, String> defaultHeaders = {
    'Content-Type': 'application/json',
    'User-Agent': userAgent,
    'X-Client-Type': clientType,
    'X-Client-Version': clientVersion,
  };
}
