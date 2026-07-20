import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';

import '../config/app_config.dart';
import '../services/api_service.dart';
import '../services/app_update_service.dart';
import '../services/auth_service.dart';
import '../services/notification_service.dart';
import '../services/permissions_service.dart';
import '../services/storage_service.dart';
import '../widgets/kyx_design.dart';
import 'debug_page.dart';
import 'login_page.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  static const _pageBg = Color(0xFFF8FAFC);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _pageBg,
      appBar: AppBar(
        title: const Text(
          '设置',
          style: TextStyle(
            color: KyXColors.text,
            fontSize: 17,
            fontWeight: FontWeight.w800,
          ),
        ),
        centerTitle: false,
        backgroundColor: _pageBg,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
      ),
      body: ListView(
        physics: const BouncingScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 28),
        children: [
          _buildUserInfo(),
          const SizedBox(height: 12),
          _buildSettingsSection('账号', [
            _SettingEntry(
              title: '个人资料',
              icon: Icons.person_outline,
              onTap: () => _showComingSoon(context, '个人资料'),
            ),
            _SettingEntry(
              title: '账号安全',
              icon: Icons.verified_user_outlined,
              onTap: () => _showComingSoon(context, '账号安全'),
            ),
            _SettingEntry(
              title: '隐私设置',
              icon: Icons.lock_outline,
              onTap: () => _showComingSoon(context, '隐私设置'),
            ),
          ]),
          const SizedBox(height: 12),
          _buildSettingsSection('偏好', [
            _SettingEntry(
              title: '消息通知',
              subtitle: '系统通知权限与提醒',
              icon: Icons.notifications_none,
              onTap: () => _handleNotificationSettings(context),
            ),
            _SettingEntry(
              title: '声音设置',
              icon: Icons.volume_up_outlined,
              onTap: () => _showComingSoon(context, '声音设置'),
            ),
            _SettingEntry(
              title: '语言设置',
              icon: Icons.language_outlined,
              onTap: () => _showComingSoon(context, '语言设置'),
            ),
            _SettingEntry(
              title: '主题设置',
              icon: Icons.dark_mode_outlined,
              onTap: () => _showComingSoon(context, '主题设置'),
            ),
          ]),
          const SizedBox(height: 12),
          _buildSettingsSection('支持', [
            _SettingEntry(
              title: '帮助与反馈',
              icon: Icons.help_outline,
              onTap: () => _showComingSoon(context, '帮助与反馈'),
            ),
            _SettingEntry(
              title: '关于我们',
              icon: Icons.info_outline,
              onTap: () => _showComingSoon(context, '关于我们'),
            ),
            _SettingEntry(
              title: '检查更新',
              subtitle: '检测新版本与安装包',
              icon: Icons.system_update_alt_outlined,
              onTap: () => _handleCheckUpdate(context),
            ),
            if (kDebugMode)
              _SettingEntry(
                title: 'IM调试工具',
                icon: Icons.bug_report_outlined,
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => const DebugPage()),
                  );
                },
              ),
          ]),
          const SizedBox(height: 12),
          _buildLogoutRow(context),
        ],
      ),
    );
  }

  Widget _buildUserInfo() {
    return FutureBuilder<String>(
      future: _getUserInfo(),
      builder: (context, snapshot) {
        final userID = snapshot.data ?? '未知用户';
        final displayName = userID.trim().isNotEmpty
            ? userID.trim().substring(0, 1)
            : '?';

        return Container(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 18),
          decoration: const BoxDecoration(
            color: _pageBg,
            border: Border(bottom: BorderSide(color: KyXColors.lineSoft)),
          ),
          child: Row(
            children: [
              KyXAvatar(text: displayName, size: 44, color: KyXColors.primary),
              const SizedBox(width: 13),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      userID,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: KyXColors.text,
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text('已登录 · 在线', style: KyXText.secondary),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.edit_outlined, size: 20),
                color: KyXColors.textSecondary,
                tooltip: '编辑资料',
                onPressed: () {},
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSettingsSection(String title, List<_SettingEntry> items) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
          child: Text(
            title,
            style: KyXText.section.copyWith(
              color: KyXColors.textTertiary,
              fontSize: 12,
            ),
          ),
        ),
        Container(
          color: KyXColors.surface,
          child: Column(
            children: [
              const Divider(height: 1, color: KyXColors.lineSoft),
              for (var i = 0; i < items.length; i++)
                _SettingsRow(
                  entry: items[i],
                  showDivider: i != items.length - 1,
                ),
              const Divider(height: 1, color: KyXColors.lineSoft),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildLogoutRow(BuildContext context) {
    return Container(
      color: KyXColors.surface,
      child: Column(
        children: [
          const Divider(height: 1, color: KyXColors.lineSoft),
          InkWell(
            onTap: () => _confirmLogout(context),
            child: const SizedBox(
              height: 52,
              child: Center(
                child: Text(
                  '退出登录',
                  style: TextStyle(
                    color: KyXColors.red,
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
          ),
          const Divider(height: 1, color: KyXColors.lineSoft),
        ],
      ),
    );
  }

  Future<void> _confirmLogout(BuildContext context) async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final shouldLogout = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('确认退出'),
          content: const Text('确定要退出登录吗？'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: TextButton.styleFrom(foregroundColor: KyXColors.red),
              child: const Text('确定'),
            ),
          ],
        );
      },
    );

    if (shouldLogout != true) return;
    await authService.logout();

    if (!context.mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (context) => const LoginPage()),
      (route) => false,
    );
  }

  Future<void> _handleNotificationSettings(BuildContext context) async {
    final messenger = ScaffoldMessenger.of(context);
    final service = NotificationService();
    final enabled = await service.areNotificationsEnabled();

    if (!context.mounted) return;
    if (enabled) {
      messenger.showSnackBar(const SnackBar(content: Text('通知权限已开启')));
      return;
    }

    final granted = await service.requestPermissionIfNeeded();
    if (!context.mounted) return;
    messenger.showSnackBar(
      SnackBar(content: Text(granted ? '通知权限已开启' : '通知权限未开启，请到系统设置里允许快易修OA通知')),
    );
  }

  Future<void> _handleCheckUpdate(BuildContext context) async {
    final messenger = ScaffoldMessenger.of(context);
    if (!Platform.isAndroid) {
      messenger.showSnackBar(const SnackBar(content: Text('当前平台暂不支持 APK 更新')));
      return;
    }

    var dialogShowing = true;
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const _CheckingUpdateDialog(),
    );

    final updateService = AppUpdateService();
    try {
      final info = await updateService.checkForUpdate(throwOnError: true);
      if (!context.mounted) return;
      if (dialogShowing) {
        Navigator.of(context, rootNavigator: true).pop();
        dialogShowing = false;
      }

      if (info == null) {
        final packageInfo = await PackageInfo.fromPlatform();
        if (!context.mounted) return;
        await _showNoUpdateDialog(context, packageInfo);
        return;
      }

      await _showUpdateDialog(context, updateService, info);
    } catch (error) {
      if (!context.mounted) return;
      if (dialogShowing) {
        Navigator.of(context, rootNavigator: true).pop();
        dialogShowing = false;
      }
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            error is AppUpdateException ? error.message : '检查更新失败，请稍后重试',
          ),
        ),
      );
    }
  }

  Future<void> _showNoUpdateDialog(
    BuildContext context,
    PackageInfo packageInfo,
  ) {
    return showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('已是最新版本'),
        content: Text(
          '当前版本 v${packageInfo.version} (${packageInfo.buildNumber})\n'
          '发布渠道 ${AppConfig.appChannel}',
          style: KyXText.secondary.copyWith(height: 1.55),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('知道了'),
          ),
        ],
      ),
    );
  }

  Future<void> _showUpdateDialog(
    BuildContext context,
    AppUpdateService updateService,
    AppUpdateInfo info,
  ) async {
    final savedSnapshot = await updateService.getSavedDownloadSnapshot(info);
    if (!context.mounted) return;

    await showDialog<void>(
      context: context,
      barrierDismissible: !info.forceUpdate,
      builder: (dialogContext) {
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
                final status = await updateService.downloadAndInstall(
                  info,
                  onProgress: (value) {
                    if (!context.mounted) return;
                    setDialogState(() => progress = value);
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
                Navigator.of(dialogContext).pop();
              } catch (error) {
                if (!context.mounted) return;
                setDialogState(() {
                  downloading = false;
                  statusText = error is AppUpdateException
                      ? error.message
                      : '更新失败，请稍后重试';
                  statusIsError = true;
                });
              }
            }

            if (!autoStarted && savedSnapshot?.isActive == true) {
              autoStarted = true;
              WidgetsBinding.instance.addPostFrameCallback((_) {
                if (context.mounted) startUpdate();
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
                    if (downloading || progress > 0) ...[
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
                            onPressed: () => Navigator.of(context).pop(),
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

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context).clearSnackBars();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('$feature 功能建设中，即将上线'),
        duration: const Duration(milliseconds: 1600),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<String> _getUserInfo() async {
    final storage = StorageService();

    // 1. 先读 cert 里的 nickname（新登录路径已经在 _hydrateProfile 合进去了）
    var cert = await storage.getLoginCertificateAsync();
    String? fromCert = _pickName(cert);
    if (fromCert != null) return fromCert;

    // 2. cert 里没 nickname（旧 cert 或 profile fetch 失败）—— 兜底 PermissionsService
    if (!PermissionsService.isLoaded) {
      await PermissionsService.loadCacheAndScheduleRefresh();
    }
    final fromPerms = PermissionsService.displayName();
    if (fromPerms.isNotEmpty) return fromPerms;

    // 3. 还没有 —— 直接现调 OA profile 接口拉一次（一次性慢，之后就快）
    final token = cert?['accessToken']?.toString();
    if (token != null && token.isNotEmpty) {
      final profile = await ApiService.fetchUserProfile(
        accessToken: token,
        tenantId: cert?['tenantId']?.toString(),
      );
      if (profile != null) {
        // 合进 cert 存本地，下次直接读 cert 就有了
        final merged = Map<String, dynamic>.from(cert ?? {});
        for (final e in profile.entries) {
          if (e.value != null && e.value.toString().isNotEmpty) merged[e.key] = e.value;
        }
        await storage.saveLoginCertificate(merged);
        final again = _pickName(merged);
        if (again != null) return again;
      }
    }

    // 4. 全都拉不到，最后回退到 userId
    final id = cert?['userID']?.toString() ?? cert?['userId']?.toString();
    return (id != null && id.isNotEmpty) ? id : '未知用户';
  }

  String? _pickName(Map<String, dynamic>? cert) {
    if (cert == null) return null;
    for (final k in const ['nickname', 'nickName', 'name', 'username', 'userName']) {
      final v = cert[k]?.toString().trim();
      if (v != null && v.isNotEmpty && v != 'null') return v;
    }
    return null;
  }
}

class _CheckingUpdateDialog extends StatelessWidget {
  const _CheckingUpdateDialog();

  @override
  Widget build(BuildContext context) {
    return const Dialog(
      insetPadding: EdgeInsets.symmetric(horizontal: 42),
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 18, 20, 18),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2.2),
            ),
            SizedBox(width: 14),
            Text('正在检查更新', style: KyXText.bodyStrong),
          ],
        ),
      ),
    );
  }
}

class _SettingEntry {
  final String title;
  final String? subtitle;
  final IconData icon;
  final VoidCallback onTap;

  const _SettingEntry({
    required this.title,
    required this.icon,
    required this.onTap,
    this.subtitle,
  });
}

class _SettingsRow extends StatelessWidget {
  final _SettingEntry entry;
  final bool showDivider;

  const _SettingsRow({required this.entry, required this.showDivider});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        InkWell(
          onTap: entry.onTap,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 11, 16, 11),
            child: Row(
              children: [
                SizedBox(
                  width: 30,
                  height: 30,
                  child: Icon(
                    entry.icon,
                    size: 21,
                    color: KyXColors.textSecondary,
                  ),
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        entry.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: KyXText.bodyStrong,
                      ),
                      if (entry.subtitle != null) ...[
                        const SizedBox(height: 3),
                        Text(
                          entry.subtitle!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: KyXText.caption,
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                const Icon(
                  Icons.chevron_right,
                  color: KyXColors.textTertiary,
                  size: 21,
                ),
              ],
            ),
          ),
        ),
        if (showDivider)
          const Divider(height: 1, indent: 61, color: KyXColors.lineSoft),
      ],
    );
  }
}
