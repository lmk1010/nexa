import 'dart:async';
import 'dart:convert';
import 'dart:developer' as developer;
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';

class AppUpdateInfo {
  final bool hasUpdate;
  final String latestVersionName;
  final int latestVersionCode;
  final String downloadUrl;
  final String sha256;
  final int? fileSize;
  final bool forceUpdate;
  final String? releaseNotes;
  final int currentVersionCode;

  const AppUpdateInfo({
    required this.hasUpdate,
    required this.latestVersionName,
    required this.latestVersionCode,
    required this.downloadUrl,
    required this.sha256,
    required this.fileSize,
    required this.forceUpdate,
    required this.releaseNotes,
    required this.currentVersionCode,
  });

  factory AppUpdateInfo.fromJson(
    Map<String, dynamic> json,
    int currentVersionCode,
  ) {
    return AppUpdateInfo(
      hasUpdate: json['hasUpdate'] == true,
      latestVersionName: _stringValue(json['latestVersionName']),
      latestVersionCode: _intValue(json['latestVersionCode']),
      downloadUrl: _stringValue(json['downloadUrl']),
      sha256: _stringValue(json['sha256']).toLowerCase(),
      fileSize: _nullableIntValue(json['fileSize']),
      forceUpdate: json['forceUpdate'] == true,
      releaseNotes: _nullableStringValue(json['releaseNotes']),
      currentVersionCode: currentVersionCode,
    );
  }

  bool get shouldUpdate =>
      hasUpdate &&
      latestVersionCode > currentVersionCode &&
      downloadUrl.trim().isNotEmpty;

  static String _stringValue(dynamic value) => value?.toString().trim() ?? '';

  static String? _nullableStringValue(dynamic value) {
    final text = value?.toString().trim();
    return text == null || text.isEmpty ? null : text;
  }

  static int _intValue(dynamic value) => _nullableIntValue(value) ?? 0;

  static int? _nullableIntValue(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }
}

class AppUpdateDownloadSnapshot {
  final String status;
  final double progress;
  final int receivedBytes;
  final int totalBytes;
  final String? message;

  const AppUpdateDownloadSnapshot({
    required this.status,
    required this.progress,
    required this.receivedBytes,
    required this.totalBytes,
    this.message,
  });

  bool get isActive =>
      status == 'pending' || status == 'running' || status == 'paused';
  bool get isSuccessful => status == 'successful';
}

class AppUpdateService {
  static const MethodChannel _channel = MethodChannel('kyx/app_update');
  static const String _downloadStateKey = 'appUpdateDownloadState';

  Future<AppUpdateInfo?> checkForUpdate({bool throwOnError = false}) async {
    if (!Platform.isAndroid) return null;

    try {
      final packageInfo = await PackageInfo.fromPlatform();
      final currentVersionCode = int.tryParse(packageInfo.buildNumber) ?? 0;
      final uri =
          Uri.parse(
            '${AppConfig.baseUrl}/app-api/infra/app-release/check',
          ).replace(
            queryParameters: {
              'platform': 'android',
              'channel': AppConfig.appChannel,
              'versionCode': currentVersionCode.toString(),
            },
          );

      final response = await http
          .get(uri, headers: AppConfig.defaultHeaders)
          .timeout(const Duration(seconds: 8));
      if (response.statusCode != 200) {
        developer.log(
          '更新检测HTTP失败: ${response.statusCode}',
          name: 'AppUpdateService',
        );
        if (throwOnError) {
          throw AppUpdateException('检查更新失败：${response.statusCode}');
        }
        return null;
      }

      final body = json.decode(utf8.decode(response.bodyBytes));
      if (body is! Map<String, dynamic> || body['code'] != 0) {
        developer.log('更新检测业务失败: $body', name: 'AppUpdateService');
        if (throwOnError) {
          final message = body is Map
              ? body['msg']?.toString() ??
                    body['message']?.toString() ??
                    '服务端返回异常'
              : '服务端返回异常';
          throw AppUpdateException(message);
        }
        return null;
      }

      final data = body['data'];
      if (data is! Map<String, dynamic>) return null;

      final info = AppUpdateInfo.fromJson(data, currentVersionCode);
      return info.shouldUpdate ? info : null;
    } catch (e) {
      developer.log('更新检测异常: $e', name: 'AppUpdateService');
      if (throwOnError) {
        if (e is AppUpdateException) rethrow;
        throw AppUpdateException('检查更新失败，请稍后重试');
      }
      return null;
    }
  }

  Future<AppUpdateDownloadSnapshot?> getSavedDownloadSnapshot(
    AppUpdateInfo info,
  ) async {
    final resolvedUrl = _resolveDownloadUri(info.downloadUrl).toString();
    final state = await _loadDownloadState();
    if (state == null || !state.matches(info, resolvedUrl)) return null;

    if (state.downloadId != null) {
      final nativeSnapshot = await _queryNativeDownload(state);
      if (nativeSnapshot != null) {
        await _saveDownloadState(state.mergeNative(nativeSnapshot));
        return nativeSnapshot.toPublicSnapshot();
      }
    }

    final fallbackSnapshot = await _buildFallbackSnapshot(state);
    if (fallbackSnapshot != null) return fallbackSnapshot;
    return state.toPublicSnapshot();
  }

  Future<String> downloadAndInstall(
    AppUpdateInfo info, {
    void Function(double progress)? onProgress,
    void Function(AppUpdateDownloadSnapshot snapshot)? onSnapshot,
    void Function(String status)? onStatus,
  }) async {
    final resolvedUrl = _resolveDownloadUri(info.downloadUrl).toString();

    try {
      return await _downloadWithSystemDownloader(
        info,
        resolvedUrl,
        onProgress: onProgress,
        onSnapshot: onSnapshot,
        onStatus: onStatus,
      );
    } on MissingPluginException catch (e) {
      developer.log('系统下载器不可用，使用Dart续传: $e', name: 'AppUpdateService');
      return _downloadWithRangeResume(
        info,
        resolvedUrl,
        onProgress: onProgress,
        onSnapshot: onSnapshot,
        onStatus: onStatus,
      );
    } on PlatformException catch (e) {
      developer.log('系统下载器异常，使用Dart续传: $e', name: 'AppUpdateService');
      return _downloadWithRangeResume(
        info,
        resolvedUrl,
        onProgress: onProgress,
        onSnapshot: onSnapshot,
        onStatus: onStatus,
      );
    }
  }

  Future<String> _downloadWithSystemDownloader(
    AppUpdateInfo info,
    String resolvedUrl, {
    void Function(double progress)? onProgress,
    void Function(AppUpdateDownloadSnapshot snapshot)? onSnapshot,
    void Function(String status)? onStatus,
  }) async {
    final savedState = await _loadDownloadState();
    late _DownloadState state;
    if (savedState == null || !savedState.matches(info, resolvedUrl)) {
      await _removeDownloadState(savedState);
      state = await _startNativeDownload(info, resolvedUrl);
    } else if (savedState.downloadId == null) {
      return _downloadWithRangeResume(
        info,
        resolvedUrl,
        onProgress: onProgress,
        onSnapshot: onSnapshot,
        onStatus: onStatus,
      );
    } else {
      state = savedState;
    }

    while (true) {
      final nativeSnapshot = await _queryNativeDownload(state);
      if (nativeSnapshot == null) {
        await _removeDownloadState(state);
        state = await _startNativeDownload(info, resolvedUrl);
        continue;
      }

      state = state.mergeNative(nativeSnapshot);
      await _saveDownloadState(state);
      final publicSnapshot = nativeSnapshot.toPublicSnapshot();
      onProgress?.call(publicSnapshot.progress);
      onSnapshot?.call(publicSnapshot);
      onStatus?.call(_statusText(nativeSnapshot.status));

      if (nativeSnapshot.isSuccessful) {
        final apkPath = nativeSnapshot.path ?? state.apkPath;
        if (apkPath == null || apkPath.isEmpty) {
          throw AppUpdateException('安装包路径为空');
        }

        await _verifyDownloadedApk(File(apkPath), info);
        await _saveDownloadState(state.copyWith(status: 'successful'));
        return await _installApk(apkPath);
      }

      if (nativeSnapshot.status == 'failed') {
        await _removeDownloadState(state);
        throw AppUpdateException(nativeSnapshot.message ?? '下载失败，请重试');
      }

      await Future.delayed(const Duration(milliseconds: 900));
    }
  }

  Future<_DownloadState> _startNativeDownload(
    AppUpdateInfo info,
    String resolvedUrl,
  ) async {
    final fileName = _buildApkFileName(info);
    final raw = await _channel
        .invokeMethod<Map<dynamic, dynamic>>('startApkDownload', {
          'url': resolvedUrl,
          'fileName': fileName,
          'title': '快易修OA v${info.latestVersionName}',
        });
    final data = Map<String, dynamic>.from(raw ?? {});
    final downloadId = _nullableIntValue(data['downloadId']);
    final apkPath = _nullableStringValue(data['path']);
    if (downloadId == null) {
      throw const FormatException('系统下载器未返回任务编号');
    }

    final state = _DownloadState(
      versionName: info.latestVersionName,
      versionCode: info.latestVersionCode,
      downloadUrl: resolvedUrl,
      sha256: info.sha256,
      fileSize: info.fileSize,
      downloadId: downloadId,
      apkPath: apkPath,
      status: 'running',
      receivedBytes: 0,
      totalBytes: info.fileSize ?? 0,
      updatedAt: DateTime.now().millisecondsSinceEpoch,
    );
    await _saveDownloadState(state);
    return state;
  }

  Future<_NativeDownloadSnapshot?> _queryNativeDownload(
    _DownloadState state,
  ) async {
    final downloadId = state.downloadId;
    if (downloadId == null) return null;

    final raw = await _channel.invokeMethod<Map<dynamic, dynamic>>(
      'queryApkDownload',
      {'downloadId': downloadId, 'path': state.apkPath},
    );
    if (raw == null) return null;
    final data = Map<String, dynamic>.from(raw);
    final status = _stringValue(data['status']);
    if (status == 'not_found') return null;

    final rawReceivedBytes = _intValue(data['receivedBytes']);
    final rawTotalBytes = _intValue(data['totalBytes']);
    final expectedTotalBytes = rawTotalBytes > 0
        ? rawTotalBytes
        : (state.fileSize ?? state.totalBytes);
    final receivedBytes = status == 'successful' && expectedTotalBytes > 0
        ? expectedTotalBytes
        : rawReceivedBytes;
    final path = _nullableStringValue(data['path']) ?? state.apkPath;
    final progress = expectedTotalBytes > 0
        ? (receivedBytes / expectedTotalBytes).clamp(0.0, 1.0).toDouble()
        : 0.0;

    return _NativeDownloadSnapshot(
      status: status.isEmpty ? 'unknown' : status,
      progress: status == 'successful' ? 1.0 : progress,
      receivedBytes: receivedBytes,
      totalBytes: expectedTotalBytes,
      path: path,
      message: _nullableStringValue(data['message']),
    );
  }

  Future<String> _downloadWithRangeResume(
    AppUpdateInfo info,
    String resolvedUrl, {
    void Function(double progress)? onProgress,
    void Function(AppUpdateDownloadSnapshot snapshot)? onSnapshot,
    void Function(String status)? onStatus,
  }) async {
    final apkFile = await _resolveApkFile(info);
    final partFile = File('${apkFile.path}.part');
    var state = await _loadDownloadState();
    if (state == null || !state.matches(info, resolvedUrl)) {
      await _removeDownloadState(state);
      state = _DownloadState(
        versionName: info.latestVersionName,
        versionCode: info.latestVersionCode,
        downloadUrl: resolvedUrl,
        sha256: info.sha256,
        fileSize: info.fileSize,
        apkPath: apkFile.path,
        status: 'running',
        receivedBytes: 0,
        totalBytes: info.fileSize ?? 0,
        updatedAt: DateTime.now().millisecondsSinceEpoch,
      );
    }

    if (await apkFile.exists()) {
      await _verifyDownloadedApk(apkFile, info);
      await _saveDownloadState(state.copyWith(status: 'successful'));
      onProgress?.call(1);
      onSnapshot?.call(
        AppUpdateDownloadSnapshot(
          status: 'successful',
          progress: 1,
          receivedBytes: info.fileSize ?? await apkFile.length(),
          totalBytes: info.fileSize ?? await apkFile.length(),
        ),
      );
      return _installApk(apkFile.path);
    }

    var receivedBytes = await partFile.exists() ? await partFile.length() : 0;
    onStatus?.call(receivedBytes > 0 ? '继续下载' : '正在下载');

    final client = http.Client();
    IOSink? sink;
    try {
      final request = http.Request('GET', Uri.parse(resolvedUrl));
      if (receivedBytes > 0) {
        request.headers['Range'] = 'bytes=$receivedBytes-';
      }
      final response = await client.send(request);

      if (response.statusCode == 416) {
        await _finalizePartFile(partFile, apkFile);
        await _verifyDownloadedApk(apkFile, info);
        await _saveDownloadState(state.copyWith(status: 'successful'));
        onProgress?.call(1);
        onSnapshot?.call(
          AppUpdateDownloadSnapshot(
            status: 'successful',
            progress: 1,
            receivedBytes: info.fileSize ?? await apkFile.length(),
            totalBytes: info.fileSize ?? await apkFile.length(),
          ),
        );
        return _installApk(apkFile.path);
      }

      if (response.statusCode != 200 && response.statusCode != 206) {
        throw AppUpdateException('下载失败：${response.statusCode}');
      }

      if (response.statusCode == 200 && receivedBytes > 0) {
        await partFile.delete();
        receivedBytes = 0;
      }

      final responseLength = response.contentLength ?? 0;
      final totalBytes = response.statusCode == 206
          ? receivedBytes + responseLength
          : (responseLength > 0 ? responseLength : (info.fileSize ?? 0));
      if (totalBytes > 0) {
        final progress = (receivedBytes / totalBytes)
            .clamp(0.0, 1.0)
            .toDouble();
        onProgress?.call(progress);
        onSnapshot?.call(
          AppUpdateDownloadSnapshot(
            status: 'running',
            progress: progress,
            receivedBytes: receivedBytes,
            totalBytes: totalBytes,
          ),
        );
      }
      sink = partFile.openWrite(mode: FileMode.append);

      var lastSavedBytes = receivedBytes;
      await for (final chunk in response.stream) {
        sink.add(chunk);
        receivedBytes += chunk.length;
        if (totalBytes > 0) {
          final progress = (receivedBytes / totalBytes)
              .clamp(0.0, 1.0)
              .toDouble();
          onProgress?.call(progress);
          onSnapshot?.call(
            AppUpdateDownloadSnapshot(
              status: 'running',
              progress: progress,
              receivedBytes: receivedBytes,
              totalBytes: totalBytes,
            ),
          );
        }
        if (receivedBytes - lastSavedBytes >= 512 * 1024) {
          lastSavedBytes = receivedBytes;
          await _saveDownloadState(
            state.copyWith(
              status: 'running',
              receivedBytes: receivedBytes,
              totalBytes: totalBytes,
              updatedAt: DateTime.now().millisecondsSinceEpoch,
            ),
          );
        }
      }

      await sink.flush();
      await sink.close();
      sink = null;

      await _finalizePartFile(partFile, apkFile);
      await _verifyDownloadedApk(apkFile, info);
      await _saveDownloadState(
        state.copyWith(
          status: 'successful',
          receivedBytes: totalBytes,
          totalBytes: totalBytes,
          updatedAt: DateTime.now().millisecondsSinceEpoch,
        ),
      );
      onProgress?.call(1);
      onSnapshot?.call(
        AppUpdateDownloadSnapshot(
          status: 'successful',
          progress: 1,
          receivedBytes: totalBytes,
          totalBytes: totalBytes,
        ),
      );
      return _installApk(apkFile.path);
    } catch (e) {
      await sink?.flush();
      await sink?.close();
      await _saveDownloadState(
        state.copyWith(
          status: 'paused',
          receivedBytes: receivedBytes,
          totalBytes: info.fileSize ?? state.totalBytes,
          updatedAt: DateTime.now().millisecondsSinceEpoch,
        ),
      );
      if (e is AppUpdateException) rethrow;
      throw AppUpdateException('下载已暂停，网络恢复后可继续');
    } finally {
      client.close();
    }
  }

  Future<void> _verifyDownloadedApk(File apkFile, AppUpdateInfo info) async {
    if (!await apkFile.exists()) {
      throw AppUpdateException('安装包不存在');
    }

    final expectedSize = info.fileSize;
    if (expectedSize != null && expectedSize > 0) {
      final actualSize = await apkFile.length();
      if (actualSize != expectedSize) {
        throw AppUpdateException('安装包大小不完整，请重新下载');
      }
    }

    if (info.sha256.isNotEmpty) {
      final actualSha256 = (await sha256.bind(apkFile.openRead()).first)
          .toString()
          .toLowerCase();
      if (actualSha256 != info.sha256) {
        await apkFile.delete().catchError((_) => apkFile);
        throw AppUpdateException('安装包校验失败，请重新下载');
      }
    }
  }

  Future<String> _installApk(String apkPath) async {
    return await _channel.invokeMethod<String>('installApk', {
          'path': apkPath,
        }) ??
        'started';
  }

  Future<File> _resolveApkFile(AppUpdateInfo info) async {
    final baseDirectory =
        await getExternalStorageDirectory() ?? await getTemporaryDirectory();
    final directory = Directory('${baseDirectory.path}/Download');
    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }
    return File('${directory.path}/${_buildApkFileName(info)}');
  }

  String _buildApkFileName(AppUpdateInfo info) {
    final safeVersionName = info.latestVersionName.replaceAll(
      RegExp(r'[^0-9A-Za-z._-]'),
      '_',
    );
    return 'kyx_oa_${safeVersionName}_${info.latestVersionCode}.apk';
  }

  Future<void> _finalizePartFile(File partFile, File apkFile) async {
    if (await apkFile.exists()) {
      await apkFile.delete();
    }
    await partFile.rename(apkFile.path);
  }

  Future<AppUpdateDownloadSnapshot?> _buildFallbackSnapshot(
    _DownloadState state,
  ) async {
    final apkPath = state.apkPath;
    if (apkPath == null || apkPath.isEmpty) return null;

    final apkFile = File(apkPath);
    if (await apkFile.exists()) {
      return const AppUpdateDownloadSnapshot(
        status: 'successful',
        progress: 1,
        receivedBytes: 1,
        totalBytes: 1,
      );
    }

    final partFile = File('$apkPath.part');
    if (!await partFile.exists()) return null;
    final receivedBytes = await partFile.length();
    final totalBytes = state.fileSize ?? state.totalBytes;
    final progress = totalBytes > 0
        ? (receivedBytes / totalBytes).clamp(0.0, 1.0).toDouble()
        : 0.0;
    return AppUpdateDownloadSnapshot(
      status: 'paused',
      progress: progress,
      receivedBytes: receivedBytes,
      totalBytes: totalBytes,
      message: '可继续下载',
    );
  }

  Future<_DownloadState?> _loadDownloadState() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_downloadStateKey);
    if (jsonString == null || jsonString.trim().isEmpty) return null;
    try {
      final decoded = json.decode(jsonString);
      if (decoded is Map<String, dynamic>) {
        return _DownloadState.fromJson(decoded);
      }
      if (decoded is Map) {
        return _DownloadState.fromJson(Map<String, dynamic>.from(decoded));
      }
    } catch (e) {
      developer.log('解析更新下载状态失败: $e', name: 'AppUpdateService');
    }
    return null;
  }

  Future<void> _saveDownloadState(_DownloadState state) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_downloadStateKey, json.encode(state.toJson()));
  }

  Future<void> _removeDownloadState(_DownloadState? state) async {
    if (state?.downloadId != null) {
      await _channel
          .invokeMethod<void>('removeApkDownload', {
            'downloadId': state!.downloadId,
            'path': state.apkPath,
          })
          .catchError((_) {});
    }
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_downloadStateKey);
  }

  String _statusText(String status) {
    switch (status) {
      case 'pending':
        return '等待系统下载';
      case 'running':
        return '后台下载中';
      case 'paused':
        return '等待网络恢复';
      case 'successful':
        return '下载完成';
      default:
        return '正在下载';
    }
  }

  Uri _resolveDownloadUri(String downloadUrl) {
    final trimmed = downloadUrl.trim();
    final uri = Uri.tryParse(trimmed);
    if (uri != null && uri.hasScheme) {
      return uri;
    }
    final baseUrl = AppConfig.baseUrl.replaceAll(RegExp(r'/+$'), '');
    if (trimmed.startsWith('/')) {
      return Uri.parse('$baseUrl$trimmed');
    }
    return Uri.parse('$baseUrl/$trimmed');
  }
}

class _NativeDownloadSnapshot {
  final String status;
  final double progress;
  final int receivedBytes;
  final int totalBytes;
  final String? path;
  final String? message;

  const _NativeDownloadSnapshot({
    required this.status,
    required this.progress,
    required this.receivedBytes,
    required this.totalBytes,
    this.path,
    this.message,
  });

  bool get isSuccessful => status == 'successful';

  AppUpdateDownloadSnapshot toPublicSnapshot() {
    return AppUpdateDownloadSnapshot(
      status: status,
      progress: progress,
      receivedBytes: receivedBytes,
      totalBytes: totalBytes,
      message: message,
    );
  }
}

class _DownloadState {
  final String versionName;
  final int versionCode;
  final String downloadUrl;
  final String sha256;
  final int? fileSize;
  final int? downloadId;
  final String? apkPath;
  final String status;
  final int receivedBytes;
  final int totalBytes;
  final int updatedAt;

  const _DownloadState({
    required this.versionName,
    required this.versionCode,
    required this.downloadUrl,
    required this.sha256,
    required this.fileSize,
    this.downloadId,
    this.apkPath,
    required this.status,
    required this.receivedBytes,
    required this.totalBytes,
    required this.updatedAt,
  });

  factory _DownloadState.fromJson(Map<String, dynamic> json) {
    return _DownloadState(
      versionName: _stringValue(json['versionName']),
      versionCode: _intValue(json['versionCode']),
      downloadUrl: _stringValue(json['downloadUrl']),
      sha256: _stringValue(json['sha256']).toLowerCase(),
      fileSize: _nullableIntValue(json['fileSize']),
      downloadId: _nullableIntValue(json['downloadId']),
      apkPath: _nullableStringValue(json['apkPath']),
      status: _stringValue(json['status']),
      receivedBytes: _intValue(json['receivedBytes']),
      totalBytes: _intValue(json['totalBytes']),
      updatedAt: _intValue(json['updatedAt']),
    );
  }

  bool matches(AppUpdateInfo info, String resolvedUrl) {
    return versionCode == info.latestVersionCode &&
        downloadUrl == resolvedUrl &&
        sha256 == info.sha256.toLowerCase();
  }

  _DownloadState copyWith({
    String? status,
    int? receivedBytes,
    int? totalBytes,
    int? updatedAt,
  }) {
    return _DownloadState(
      versionName: versionName,
      versionCode: versionCode,
      downloadUrl: downloadUrl,
      sha256: sha256,
      fileSize: fileSize,
      downloadId: downloadId,
      apkPath: apkPath,
      status: status ?? this.status,
      receivedBytes: receivedBytes ?? this.receivedBytes,
      totalBytes: totalBytes ?? this.totalBytes,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  _DownloadState mergeNative(_NativeDownloadSnapshot snapshot) {
    final mergedTotalBytes = snapshot.totalBytes > 0
        ? snapshot.totalBytes
        : (fileSize ?? totalBytes);
    return _DownloadState(
      versionName: versionName,
      versionCode: versionCode,
      downloadUrl: downloadUrl,
      sha256: sha256,
      fileSize: fileSize,
      downloadId: downloadId,
      apkPath: snapshot.path ?? apkPath,
      status: snapshot.status,
      receivedBytes: snapshot.receivedBytes,
      totalBytes: mergedTotalBytes,
      updatedAt: DateTime.now().millisecondsSinceEpoch,
    );
  }

  AppUpdateDownloadSnapshot toPublicSnapshot() {
    final resolvedTotalBytes = totalBytes > 0 ? totalBytes : (fileSize ?? 0);
    final resolvedReceivedBytes =
        status == 'successful' && resolvedTotalBytes > 0
        ? resolvedTotalBytes
        : receivedBytes;
    final progress = resolvedTotalBytes > 0
        ? (resolvedReceivedBytes / resolvedTotalBytes)
              .clamp(0.0, 1.0)
              .toDouble()
        : 0.0;
    return AppUpdateDownloadSnapshot(
      status: status,
      progress: status == 'successful' ? 1 : progress,
      receivedBytes: resolvedReceivedBytes,
      totalBytes: resolvedTotalBytes,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'versionName': versionName,
      'versionCode': versionCode,
      'downloadUrl': downloadUrl,
      'sha256': sha256,
      'fileSize': fileSize,
      'downloadId': downloadId,
      'apkPath': apkPath,
      'status': status,
      'receivedBytes': receivedBytes,
      'totalBytes': totalBytes,
      'updatedAt': updatedAt,
    };
  }
}

class AppUpdateException implements Exception {
  final String message;

  AppUpdateException(this.message);

  @override
  String toString() => message;
}

String _stringValue(dynamic value) => value?.toString().trim() ?? '';

String? _nullableStringValue(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty || text == 'null' ? null : text;
}

int _intValue(dynamic value) => _nullableIntValue(value) ?? 0;

int? _nullableIntValue(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '');
}
