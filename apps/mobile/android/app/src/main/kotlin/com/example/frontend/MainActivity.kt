package com.example.frontend

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File

class MainActivity : FlutterActivity() {
    private val appUpdateChannel = "kyx/app_update"
    private val nativeIntentsChannel = "kyx/native_intents"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置一体化状态栏
        setupUnifiedStatusBar()
    }
    
    override fun onResume() {
        super.onResume()
        // 当应用从后台恢复时，重新设置一体化状态栏
        setupUnifiedStatusBar()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, appUpdateChannel)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "installApk" -> {
                        val path = call.argument<String>("path")
                        if (path.isNullOrBlank()) {
                            result.error("APK_PATH_EMPTY", "安装包路径为空", null)
                            return@setMethodCallHandler
                        }
                        installApk(path, result)
                    }
                    "startApkDownload" -> startApkDownload(call.argument("url"), call.argument("fileName"), call.argument("title"), result)
                    "queryApkDownload" -> queryApkDownload(call.argument("downloadId"), call.argument("path"), result)
                    "removeApkDownload" -> removeApkDownload(call.argument("downloadId"), call.argument("path"), result)
                    else -> result.notImplemented()
                }
            }
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, nativeIntentsChannel)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "openDingTalkAttendance" -> openDingTalkAttendance(result)
                    else -> result.notImplemented()
                }
            }
    }

    private fun openDingTalkAttendance(result: MethodChannel.Result) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.alibaba.android.rimet")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                result.success(true)
                return
            }

            val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("dingtalk://dingtalkclient")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(schemeIntent)
            result.success(true)
        } catch (e: Exception) {
            result.success(false)
        }
    }

    private fun startApkDownload(url: String?, fileName: String?, title: String?, result: MethodChannel.Result) {
        try {
            if (url.isNullOrBlank()) {
                result.error("DOWNLOAD_URL_EMPTY", "下载地址为空", null)
                return
            }

            val safeFileName = sanitizeFileName(fileName ?: "kyx_oa_update.apk")
            val directory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val targetFile = File(directory, safeFileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title ?: "快易修OA更新")
                setDescription("正在下载更新安装包")
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                addRequestHeader("User-Agent", "KuaiYiXiu-OA/1.0.0")
                setDestinationUri(Uri.fromFile(targetFile))
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            result.success(mapOf("downloadId" to downloadId, "path" to targetFile.absolutePath))
        } catch (e: Exception) {
            result.error("DOWNLOAD_START_FAILED", e.message ?: "启动下载失败", null)
        }
    }

    private fun queryApkDownload(downloadIdValue: Any?, fallbackPath: String?, result: MethodChannel.Result) {
        try {
            val downloadId = parseLong(downloadIdValue)
            if (downloadId == null) {
                result.error("DOWNLOAD_ID_EMPTY", "下载任务编号为空", null)
                return
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (cursor == null || !cursor.moveToFirst()) {
                    result.success(mapOf("status" to "not_found"))
                    return
                }

                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                val receivedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                val localPath = resolveLocalPath(localUri) ?: fallbackPath

                result.success(
                    mapOf(
                        "status" to mapDownloadStatus(status),
                        "receivedBytes" to receivedBytes,
                        "totalBytes" to if (totalBytes > 0) totalBytes else 0L,
                        "path" to localPath,
                        "message" to mapDownloadReason(status, reason)
                    )
                )
            }
        } catch (e: Exception) {
            result.error("DOWNLOAD_QUERY_FAILED", e.message ?: "查询下载失败", null)
        }
    }

    private fun removeApkDownload(downloadIdValue: Any?, path: String?, result: MethodChannel.Result) {
        try {
            val downloadId = parseLong(downloadIdValue)
            if (downloadId != null) {
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.remove(downloadId)
            }
            if (!path.isNullOrBlank()) {
                File(path).takeIf { it.exists() }?.delete()
            }
            result.success(true)
        } catch (e: Exception) {
            result.error("DOWNLOAD_REMOVE_FAILED", e.message ?: "清理下载失败", null)
        }
    }

    private fun installApk(path: String, result: MethodChannel.Result) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(settingsIntent)
                result.success("permission_required")
                return
            }

            val apkFile = File(path)
            if (!apkFile.exists()) {
                result.error("APK_NOT_FOUND", "安装包不存在", null)
                return
            }

            val apkUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(installIntent)
            result.success("started")
        } catch (e: Exception) {
            result.error("INSTALL_FAILED", e.message ?: "安装失败", null)
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^0-9A-Za-z._-]"), "_")
    }

    private fun parseLong(value: Any?): Long? {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun resolveLocalPath(localUri: String?): String? {
        if (localUri.isNullOrBlank()) {
            return null
        }
        val uri = Uri.parse(localUri)
        return if (uri.scheme == "file") uri.path else localUri
    }

    private fun mapDownloadStatus(status: Int): String {
        return when (status) {
            DownloadManager.STATUS_PENDING -> "pending"
            DownloadManager.STATUS_RUNNING -> "running"
            DownloadManager.STATUS_PAUSED -> "paused"
            DownloadManager.STATUS_SUCCESSFUL -> "successful"
            DownloadManager.STATUS_FAILED -> "failed"
            else -> "unknown"
        }
    }

    private fun mapDownloadReason(status: Int, reason: Int): String? {
        if (status == DownloadManager.STATUS_PAUSED) {
            return when (reason) {
                DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "等待网络恢复"
                DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "等待 WiFi 网络"
                DownloadManager.PAUSED_WAITING_TO_RETRY -> "等待系统重试"
                else -> "下载已暂停"
            }
        }
        if (status == DownloadManager.STATUS_FAILED) {
            return when (reason) {
                DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
                DownloadManager.ERROR_CANNOT_RESUME -> "无法续传，请重新下载"
                DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "安装包已存在"
                DownloadManager.ERROR_HTTP_DATA_ERROR -> "网络数据异常"
                else -> "下载失败"
            }
        }
        return null
    }
    
    private fun setupUnifiedStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 (API 23) 及以上版本
            // 设置状态栏为浅色背景，深色图标
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            )
            
            // 设置状态栏颜色为白色
            window.statusBarColor = android.graphics.Color.WHITE
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9.0 (API 28) 及以上版本
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}
