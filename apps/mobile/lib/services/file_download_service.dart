// 文件下载 —— agent 生成的 excel/pdf 等直接在 APP 内后台下载 → 用系统打开
// 不跳浏览器；下载到 app 的 documents 目录（免存储权限）→ open_filex 触发 WPS/Excel
import 'dart:async';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

class FileDownloadService {
  static final Dio _dio = Dio(BaseOptions(
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(minutes: 5),
    responseType: ResponseType.bytes,
  ));

  /// 下载 URL 到 app 的私有目录，返回本地路径
  /// [onProgress] 回调 0.0 - 1.0
  /// [headers] 可选 auth header
  static Future<String> download({
    required String url,
    required String filename,
    Map<String, String>? headers,
    void Function(double progress)? onProgress,
  }) async {
    final dir = await getApplicationDocumentsDirectory();
    final safeSubdir = Directory('${dir.path}/kyx-exports');
    if (!await safeSubdir.exists()) await safeSubdir.create(recursive: true);
    final localPath = '${safeSubdir.path}/$filename';

    // 如果已经下载过，直接返回（省流量）
    final existing = File(localPath);
    if (await existing.exists() && (await existing.length()) > 0) {
      return localPath;
    }

    await _dio.download(
      url,
      localPath,
      options: headers != null && headers.isNotEmpty
          ? Options(headers: headers)
          : null,
      onReceiveProgress: (received, total) {
        if (total > 0 && onProgress != null) {
          onProgress(received / total);
        }
      },
    );
    return localPath;
  }

  /// 检查已下载过的本地路径 (返回 null 表示未下载)
  static Future<String?> cachedPath(String filename) async {
    final dir = await getApplicationDocumentsDirectory();
    final localPath = '${dir.path}/kyx-exports/$filename';
    final f = File(localPath);
    if (await f.exists() && (await f.length()) > 0) return localPath;
    return null;
  }

  /// 下载 + 打开
  static Future<OpenResult> downloadAndOpen({
    required String url,
    required String filename,
    Map<String, String>? headers,
    void Function(double progress)? onProgress,
  }) async {
    try {
      final path = await download(
        url: url,
        filename: filename,
        headers: headers,
        onProgress: onProgress,
      );
      final res = await OpenFilex.open(path);
      if (kDebugMode) {
        debugPrint('[download] $filename → $path → open ${res.type} ${res.message}');
      }
      return res;
    } catch (e) {
      if (kDebugMode) debugPrint('[download] fail: $e');
      return OpenResult(type: ResultType.error, message: e.toString());
    }
  }
}
