import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  static final GlobalKey<NavigatorState> navigatorKey =
      GlobalKey<NavigatorState>();

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();

  bool _initialized = false;
  Future<bool>? _permissionRequestFuture;

  static const String _imChannelId = 'kyx_im_messages_v2';
  static const String _imChannelName = 'IM消息';
  static const String _imChannelDescription = 'OA即时通讯消息提醒';

  Future<void> initialize() async {
    if (_initialized) return;

    const androidSettings = AndroidInitializationSettings(
      '@mipmap/ic_launcher',
    );
    const darwinSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );
    const settings = InitializationSettings(
      android: androidSettings,
      iOS: darwinSettings,
      macOS: darwinSettings,
    );

    try {
      await _plugin.initialize(
        settings: settings,
        onDidReceiveNotificationResponse: (response) {
          _handleNotificationPayload(response.payload);
        },
      );
      _initialized = true;

      final androidPlugin = _plugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >();
      await androidPlugin?.createNotificationChannel(
        const AndroidNotificationChannel(
          _imChannelId,
          _imChannelName,
          description: _imChannelDescription,
          importance: Importance.high,
        ),
      );

      final launchDetails = await _plugin.getNotificationAppLaunchDetails();
      if (launchDetails?.didNotificationLaunchApp == true) {
        _handleNotificationPayload(
          launchDetails?.notificationResponse?.payload,
          delayNavigation: true,
        );
      }
    } catch (e) {
      developer.log(
        'Initialize local notifications failed: $e',
        name: 'NotificationService',
      );
    }
  }

  Future<bool> areNotificationsEnabled() async {
    await initialize();

    try {
      final androidPlugin = _plugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >();
      final androidEnabled = await androidPlugin?.areNotificationsEnabled();
      if (androidEnabled != null) return androidEnabled;

      final iosPlugin = _plugin
          .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin
          >();
      final iosEnabled = await iosPlugin?.requestPermissions(
        alert: false,
        badge: false,
        sound: false,
      );
      return iosEnabled ?? true;
    } catch (e) {
      developer.log(
        'Check notification permission failed: $e',
        name: 'NotificationService',
      );
      return false;
    }
  }

  Future<bool> requestPermissionIfNeeded() async {
    await initialize();

    final enabled = await areNotificationsEnabled();
    if (enabled) return true;

    if (_permissionRequestFuture != null) {
      return _permissionRequestFuture!;
    }

    _permissionRequestFuture = _requestNotificationPermission();
    try {
      return await _permissionRequestFuture!;
    } finally {
      _permissionRequestFuture = null;
    }
  }

  Future<bool> _requestNotificationPermission() async {
    try {
      final androidPlugin = _plugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >();
      final androidGranted = await androidPlugin
          ?.requestNotificationsPermission();
      if (androidGranted != null) return androidGranted;

      final iosPlugin = _plugin
          .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin
          >();
      final iosGranted = await iosPlugin?.requestPermissions(
        alert: true,
        badge: true,
        sound: true,
      );
      return iosGranted ?? true;
    } catch (e) {
      developer.log(
        'Request notification permission failed: $e',
        name: 'NotificationService',
      );
      return false;
    }
  }

  Future<void> showImMessage({
    required String conversationId,
    required String conversationName,
    required String conversationType,
    required String senderName,
    required String content,
    bool isSelf = false,
  }) async {
    if (isSelf || conversationId.trim().isEmpty) return;
    await initialize();
    if (!_initialized) return;

    final title = conversationType == 'group'
        ? (conversationName.trim().isNotEmpty ? conversationName.trim() : '群聊')
        : (senderName.trim().isNotEmpty
              ? senderName.trim()
              : conversationName.trim().isNotEmpty
              ? conversationName.trim()
              : conversationId.trim());
    final body = content.trim().isNotEmpty ? content.trim() : '收到一条新消息';
    final payload = jsonEncode({
      'conversationId': conversationId,
      'conversationName': conversationName.trim().isNotEmpty
          ? conversationName.trim()
          : title,
      'conversationType': conversationType,
    });

    const androidDetails = AndroidNotificationDetails(
      _imChannelId,
      _imChannelName,
      channelDescription: _imChannelDescription,
      importance: Importance.high,
      priority: Priority.high,
      category: AndroidNotificationCategory.message,
      enableVibration: true,
    );
    const darwinDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    try {
      await _plugin.show(
        id: _notificationId(conversationId),
        title: title,
        body: body,
        notificationDetails: const NotificationDetails(
          android: androidDetails,
          iOS: darwinDetails,
        ),
        payload: payload,
      );
    } catch (e) {
      developer.log(
        'Show IM local notification failed: $e',
        name: 'NotificationService',
      );
    }
  }

  int _notificationId(String conversationId) {
    var hash = 0;
    for (final codeUnit in conversationId.codeUnits) {
      hash = 0x1fffffff & (hash + codeUnit);
      hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
      hash ^= hash >> 6;
    }
    return hash.abs();
  }

  void _handleNotificationPayload(
    String? payload, {
    bool delayNavigation = false,
  }) {
    if (payload == null || payload.isEmpty) return;

    void navigate() {
      final navigator = navigatorKey.currentState;
      if (navigator == null) return;

      try {
        final decoded = jsonDecode(payload);
        if (decoded is! Map) return;
        final conversationId = decoded['conversationId']?.toString() ?? '';
        if (conversationId.isEmpty) return;
        navigator.pushNamed(
          '/chat',
          arguments: {
            'conversationId': conversationId,
            'contactName': decoded['conversationName']?.toString() ?? '',
            'conversationType':
                decoded['conversationType']?.toString() ?? 'single',
          },
        );
      } catch (e) {
        developer.log(
          'Handle notification payload failed: $e',
          name: 'NotificationService',
        );
      }
    }

    if (delayNavigation) {
      WidgetsBinding.instance.addPostFrameCallback((_) => navigate());
    } else {
      navigate();
    }
  }
}
