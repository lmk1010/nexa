import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../services/ai_reception_service.dart';
import '../services/front_desk_keyword_config_service.dart';
import '../services/front_desk_keyword_watch_service.dart';
import '../services/oa_chat_share.dart';
import '../widgets/kyx_design.dart';
import '../widgets/oa_chat_share_sheet.dart';

class FrontDeskReceptionPage extends StatefulWidget {
  const FrontDeskReceptionPage({super.key});

  @override
  State<FrontDeskReceptionPage> createState() => _FrontDeskReceptionPageState();
}

class _FrontDeskReceptionPageState extends State<FrontDeskReceptionPage>
    with WidgetsBindingObserver {
  static const int _recordingSampleRate = 16000;
  static const int _recordingChannels = 1;
  static const int _recordingBitsPerSample = 16;
  static const int _wavHeaderLength = 44;
  static const int _minimumAutoStopSeconds = 5;

  final AudioRecorder _recorder = AudioRecorder();
  final AudioPlayer _player = AudioPlayer();
  StreamSubscription<void>? _playerCompleteSub;
  StreamSubscription<FrontDeskKeywordWatchSnapshot>? _keywordWatchSub;
  StreamSubscription<Uint8List>? _recordingStreamSub;
  final FrontDeskKeywordWatchService _keywordWatchService =
      FrontDeskKeywordWatchService();
  final FrontDeskKeywordConfigService _keywordConfigService =
      FrontDeskKeywordConfigService();
  FrontDeskKeywordDetectorSession? _recordingKeywordDetector;
  RandomAccessFile? _recordingFile;
  Future<void> _recordingWriteFuture = Future.value();
  Timer? _timer;
  Timer? _recordsPollTimer;
  DateTime? _startedAt;
  String? _activePath;
  String? _recordedPath;
  String? _errorText;
  int _recordingPcmBytes = 0;
  int _elapsedSeconds = 0;
  bool _isRecording = false;
  bool _isPlaying = false;
  bool _isBusy = false;
  bool _isSubmitting = false;
  bool _isRecordsPolling = false;
  bool _isStoppingByKeyword = false;
  FrontDeskKeywordConfig _keywordConfig = FrontDeskKeywordConfig.defaults();
  FrontDeskKeywordWatchSnapshot _keywordWatchSnapshot =
      const FrontDeskKeywordWatchSnapshot(
        state: FrontDeskKeywordWatchState.idle,
        message: '关键词监听未开启',
      );
  late Future<List<AiReceptionRecord>> _recordsFuture;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _recordsFuture = _loadRecords();
    _playerCompleteSub = _player.onPlayerComplete.listen((_) {
      if (mounted) setState(() => _isPlaying = false);
    });
    _keywordWatchSub = _keywordWatchService.snapshots.listen((snapshot) {
      if (mounted) setState(() => _keywordWatchSnapshot = snapshot);
    });
    unawaited(_loadKeywordConfig());
    unawaited(_refreshKeywordModelStatus());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    _recordsPollTimer?.cancel();
    _playerCompleteSub?.cancel();
    _keywordWatchSub?.cancel();
    unawaited(_recordingStreamSub?.cancel());
    unawaited(_keywordWatchService.dispose());
    _recordingKeywordDetector?.dispose();
    if (_isRecording) {
      _recorder.cancel();
    }
    unawaited(_recordingFile?.close());
    _recorder.dispose();
    _player.dispose();
    _deleteRecordedFileSync();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_isRecording) return;
    if (state == AppLifecycleState.resumed) {
      unawaited(_syncRecorderAfterResume());
      return;
    }
    _syncElapsed();
  }

  Future<List<AiReceptionRecord>> _loadRecords() async {
    final records = await AiReceptionService.getMyRecords();
    if (mounted) _syncRecordsPoller(records);
    return records;
  }

  Future<void> _refreshRecords() async {
    setState(() {
      _recordsFuture = _loadRecords();
    });
    await _recordsFuture;
  }

  void _syncRecordsPoller(List<AiReceptionRecord> records) {
    final shouldPoll = records.any(
      (record) => _isReceptionProcessing(record.status),
    );
    if (!shouldPoll) {
      _recordsPollTimer?.cancel();
      _recordsPollTimer = null;
      return;
    }

    _recordsPollTimer ??= Timer.periodic(const Duration(seconds: 6), (_) {
      _pollRecordsOnce();
    });
  }

  Future<void> _pollRecordsOnce() async {
    if (!mounted || _isRecordsPolling) return;
    _isRecordsPolling = true;
    final nextFuture = _loadRecords();
    setState(() => _recordsFuture = nextFuture);
    try {
      await nextFuture;
    } catch (_) {
      // Keep the current list visible; pull-to-refresh still reports failures.
    } finally {
      _isRecordsPolling = false;
    }
  }

  Future<void> _openRecordDetail(AiReceptionRecord record) async {
    if (record.id == null) return;
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => _ReceptionRecordDetailPage(initialRecord: record),
      ),
    );
    if (mounted) await _refreshRecords();
  }


  Future<void> _deleteRecord(AiReceptionRecord record) async {
    final id = record.id;
    if (id == null) return;
    final title = _joinText([
      record.visitorName,
      record.visitorCompany,
      record.purpose,
    ]);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除录音'),
        content: Text(
          '确定删除${title.isEmpty ? '这条录音记录' : '“$title”'}？删除后列表不再显示。',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: KyXColors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await AiReceptionService.deleteRecord(id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('录音已删除')),
      );
      await _refreshRecords();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('删除失败：${_cleanError(error)}')),
      );
    }
  }

  Future<void> _refreshKeywordModelStatus() async {
    if (_keywordWatchService.isListening) return;
    try {
      final status = await _keywordWatchService.checkModelStatus();
      if (!mounted || _keywordWatchService.isListening) return;
      setState(() {
        _keywordWatchSnapshot = FrontDeskKeywordWatchSnapshot(
          state: status.ready
              ? FrontDeskKeywordWatchState.idle
              : FrontDeskKeywordWatchState.unavailable,
          message: status.ready ? '关键词模型已就绪' : '关键词模型未安装',
          modelStatus: status,
        );
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _keywordWatchSnapshot = FrontDeskKeywordWatchSnapshot(
          state: FrontDeskKeywordWatchState.error,
          message: '关键词模型检查失败: ${_cleanError(error)}',
        );
      });
    }
  }

  Future<void> _loadKeywordConfig() async {
    try {
      final config = await _keywordConfigService.load();
      if (!mounted) return;
      setState(() => _keywordConfig = config);
    } catch (_) {}
  }

  Future<void> _openKeywordConfigSheet() async {
    if (_isRecording || _isBusy || _isSubmitting) return;
    final nextConfig = await showModalBottomSheet<FrontDeskKeywordConfig>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (_) => _KeywordConfigSheet(initialConfig: _keywordConfig),
    );
    if (nextConfig == null || !mounted) return;
    await _keywordConfigService.save(nextConfig);
    if (_keywordWatchService.isListening) {
      await _keywordWatchService.stop();
    }
    if (!mounted) return;
    setState(() => _keywordConfig = nextConfig);
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('关键词配置已保存')));
  }

  Future<void> _toggleKeywordWatch() async {
    if (_keywordWatchSnapshot.isListening || _keywordWatchSnapshot.isStarting) {
      await _keywordWatchService.stop();
      return;
    }
    await _startKeywordWatch();
  }

  Future<void> _startKeywordWatch() async {
    if (_isRecording || _isBusy || _isSubmitting) return;
    await _stopPlayback();
    await _keywordWatchService.start(
      onDetected: _handleKeywordDetected,
      config: _keywordConfig,
    );
  }

  Future<void> _handleKeywordDetected(String keyword) async {
    await _keywordWatchService.stop(emitIdle: false);
    if (!mounted || _isRecording || _isBusy || _isSubmitting) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text('识别到“$keyword”，已开始录音')));
    await _startRecording();
  }

  Future<void> _startRecording() async {
    if (_isBusy || _isSubmitting) return;
    if (_keywordWatchService.isListening) {
      await _keywordWatchService.stop();
    }
    await _stopPlayback();
    await _deleteRecordedFile();
    if (!mounted) return;

    setState(() {
      _isBusy = true;
      _errorText = null;
      _elapsedSeconds = 0;
      _recordedPath = null;
    });

    try {
      final hasPermission = await _recorder.hasPermission();
      if (!hasPermission) {
        if (!mounted) return;
        setState(() => _errorText = '麦克风权限未开启');
        return;
      }

      final tempDir = await getTemporaryDirectory();
      final path =
          '${tempDir.path}${Platform.pathSeparator}front_desk_${DateTime.now().millisecondsSinceEpoch}.wav';
      _activePath = path;
      _recordingPcmBytes = 0;
      _recordingWriteFuture = Future.value();
      _recordingFile = await File(path).open(mode: FileMode.write);
      await _recordingFile!.writeFrom(_buildWavHeader(0));
      _recordingKeywordDetector = await _createRecordingKeywordDetector();

      final audioStream = await _recorder.startStream(
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: _recordingSampleRate,
          numChannels: _recordingChannels,
          audioInterruption: AudioInterruptionMode.none,
          androidConfig: AndroidRecordConfig(
            // ignore: deprecated_member_use
            service: AndroidService(
              title: '前台接待录音中',
              content: '说出结束语会自动停止，切换应用不会中断录音。',
            ),
          ),
          echoCancel: true,
          noiseSuppress: true,
        ),
      );
      _recordingStreamSub = audioStream.listen(
        _handleRecordingAudioChunk,
        onError: (_) {
          if (!mounted || !_isRecording) return;
          setState(() => _errorText = '录音流异常，请手动停止后重试');
        },
      );

      _startedAt = DateTime.now();
      _timer?.cancel();
      _timer = Timer.periodic(const Duration(seconds: 1), (_) {
        _syncElapsed();
      });
      if (!mounted) return;
      setState(() => _isRecording = true);
    } catch (_) {
      await _cancelRecordingResources(cancelRecorder: true);
      if (!mounted) return;
      setState(() => _errorText = '录音启动失败');
    } finally {
      if (mounted) setState(() => _isBusy = false);
    }
  }

  Future<void> _stopRecording({String? autoKeyword}) async {
    if (!_isRecording || _isBusy) return;
    setState(() {
      _isBusy = true;
      _errorText = null;
    });

    try {
      final duration = _currentElapsedSeconds();
      _timer?.cancel();
      await _recorder.stop();
      await _recordingStreamSub?.cancel();
      _recordingStreamSub = null;
      await _recordingWriteFuture;

      final file = _recordingFile;
      _recordingFile = null;
      if (file != null) {
        await file.setPosition(0);
        await file.writeFrom(_buildWavHeader(_recordingPcmBytes));
        await file.close();
      }

      _recordingKeywordDetector?.dispose();
      _recordingKeywordDetector = null;
      _isStoppingByKeyword = false;

      final path = _activePath;
      final recordingFile = path == null ? null : File(path);
      if (path == null ||
          path.isEmpty ||
          recordingFile == null ||
          !recordingFile.existsSync() ||
          recordingFile.lengthSync() <= _wavHeaderLength) {
        if (!mounted) return;
        setState(() {
          _isRecording = false;
          _errorText = '录音文件不可用';
        });
        return;
      }

      if (!mounted) return;
      setState(() {
        _isRecording = false;
        _recordedPath = path;
        _elapsedSeconds = duration <= 0 ? 1 : duration;
      });
      if (autoKeyword != null && mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('识别到“$autoKeyword”，录音已自动停止')));
      }
    } catch (_) {
      await _cancelRecordingResources(cancelRecorder: true);
      if (!mounted) return;
      setState(() => _errorText = '录音停止失败');
    } finally {
      _isStoppingByKeyword = false;
      if (mounted) setState(() => _isBusy = false);
    }
  }

  Future<void> _discardRecording({bool force = false}) async {
    if (!force && (_isBusy || _isSubmitting)) return;
    _timer?.cancel();
    await _cancelRecordingResources(cancelRecorder: _isRecording);
    await _stopPlayback();
    await _deleteRecordedFile();
    if (!mounted) return;
    setState(() {
      _isRecording = false;
      _recordedPath = null;
      _activePath = null;
      _startedAt = null;
      _elapsedSeconds = 0;
      _errorText = null;
    });
  }

  Future<FrontDeskKeywordDetectorSession?>
  _createRecordingKeywordDetector() async {
    try {
      return await _keywordWatchService.createDetectorSession();
    } catch (_) {
      return null;
    }
  }

  void _handleRecordingAudioChunk(Uint8List chunk) {
    if (!_isRecording || chunk.isEmpty) return;

    final file = _recordingFile;
    if (file != null) {
      final data = Uint8List.fromList(chunk);
      _recordingPcmBytes += data.lengthInBytes;
      _recordingWriteFuture = _recordingWriteFuture
          .then((_) async {
            await file.writeFrom(data);
          })
          .catchError((_) {
            if (!mounted || !_isRecording) return;
            setState(() => _errorText = '录音写入失败，请手动停止后重试');
          });
    }

    final detector = _recordingKeywordDetector;
    if (detector == null || _isStoppingByKeyword) return;
    try {
      final keyword = detector.acceptPcm16(chunk);
      if (keyword == null) return;
      final match = FrontDeskKeywordWatchService.classifyKeyword(
        keyword,
        config: _keywordConfig,
      );
      if (match?.isStop != true) return;
      if (_currentElapsedSeconds() < _minimumAutoStopSeconds) return;
      _isStoppingByKeyword = true;
      unawaited(_stopRecording(autoKeyword: match!.keyword));
    } catch (_) {
      detector.dispose();
      _recordingKeywordDetector = null;
    }
  }

  Future<void> _cancelRecordingResources({required bool cancelRecorder}) async {
    if (cancelRecorder) {
      try {
        await _recorder.cancel();
      } catch (_) {}
    }
    try {
      await _recordingStreamSub?.cancel();
    } catch (_) {}
    _recordingStreamSub = null;

    try {
      await _recordingWriteFuture;
    } catch (_) {}

    try {
      await _recordingFile?.close();
    } catch (_) {}
    _recordingFile = null;

    _recordingKeywordDetector?.dispose();
    _recordingKeywordDetector = null;
    _recordingPcmBytes = 0;
    _isStoppingByKeyword = false;
  }

  Uint8List _buildWavHeader(int dataLength) {
    final header = Uint8List(_wavHeaderLength);
    final data = ByteData.sublistView(header);
    final byteRate =
        _recordingSampleRate *
        _recordingChannels *
        _recordingBitsPerSample ~/
        8;
    final blockAlign = _recordingChannels * _recordingBitsPerSample ~/ 8;

    void writeAscii(int offset, String value) {
      for (var i = 0; i < value.length; i += 1) {
        header[offset + i] = value.codeUnitAt(i);
      }
    }

    writeAscii(0, 'RIFF');
    data.setUint32(4, 36 + dataLength, Endian.little);
    writeAscii(8, 'WAVE');
    writeAscii(12, 'fmt ');
    data.setUint32(16, 16, Endian.little);
    data.setUint16(20, 1, Endian.little);
    data.setUint16(22, _recordingChannels, Endian.little);
    data.setUint32(24, _recordingSampleRate, Endian.little);
    data.setUint32(28, byteRate, Endian.little);
    data.setUint16(32, blockAlign, Endian.little);
    data.setUint16(34, _recordingBitsPerSample, Endian.little);
    writeAscii(36, 'data');
    data.setUint32(40, dataLength, Endian.little);
    return header;
  }

  Future<void> _togglePlayback() async {
    final path = _recordedPath;
    if (path == null || path.isEmpty || _isRecording || _isBusy) return;

    if (_isPlaying) {
      await _stopPlayback();
      return;
    }

    try {
      await _player.play(DeviceFileSource(path));
      if (mounted) setState(() => _isPlaying = true);
    } catch (_) {
      if (!mounted) return;
      setState(() => _errorText = '播放失败');
    }
  }

  Future<void> _submitRecording() async {
    final path = _recordedPath;
    if (path == null || path.isEmpty || _isSubmitting || _isBusy) return;

    setState(() {
      _isSubmitting = true;
      _errorText = null;
    });

    try {
      final id = await AiReceptionService.uploadRecording(
        path: path,
        duration: _elapsedSeconds <= 0 ? 1 : _elapsedSeconds,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('录音已提交，记录 #$id')));
      await _discardRecording(force: true);
      if (mounted) await _refreshRecords();
    } catch (error) {
      if (!mounted) return;
      setState(() => _errorText = _cleanError(error));
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _stopPlayback() async {
    if (!_isPlaying) return;
    try {
      await _player.stop();
    } catch (_) {}
    if (mounted) setState(() => _isPlaying = false);
  }

  Future<void> _deleteRecordedFile() async {
    final paths = {_recordedPath, _activePath};
    for (final path in paths) {
      if (path == null || path.isEmpty) continue;
      try {
        final file = File(path);
        if (file.existsSync()) await file.delete();
      } catch (_) {}
    }
  }

  void _deleteRecordedFileSync() {
    final paths = {_recordedPath, _activePath};
    for (final path in paths) {
      if (path == null || path.isEmpty) continue;
      try {
        final file = File(path);
        if (file.existsSync()) file.deleteSync();
      } catch (_) {}
    }
  }

  void _syncElapsed() {
    if (!mounted || !_isRecording) return;
    final seconds = _currentElapsedSeconds();
    if (seconds == _elapsedSeconds) return;
    setState(() => _elapsedSeconds = seconds);
  }

  Future<void> _syncRecorderAfterResume() async {
    if (!_isRecording) return;
    try {
      final stillRecording = await _recorder.isRecording();
      if (!mounted) return;
      if (stillRecording) {
        _syncElapsed();
        return;
      }
      _timer?.cancel();
      setState(() {
        _isRecording = false;
        _errorText = '录音已被系统中断，请重新录制';
      });
    } catch (_) {
      if (mounted) _syncElapsed();
    }
  }

  int _currentElapsedSeconds() {
    final startedAt = _startedAt;
    if (startedAt == null) return _elapsedSeconds;
    final seconds = DateTime.now().difference(startedAt).inSeconds;
    return seconds < 1 ? 1 : seconds;
  }

  @override
  Widget build(BuildContext context) {
    final hasRecording = _recordedPath != null;
    return PopScope(
      canPop: !_isRecording && !_isSubmitting,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop || (!_isRecording && !_isSubmitting)) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_isRecording ? '录音中，请先停止录音' : '提交中，请稍候')),
        );
      },
      child: Scaffold(
        backgroundColor: KyXColors.bg,
        appBar: AppBar(
          title: const Text(
            '前台接待',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
          ),
          backgroundColor: KyXColors.surface,
          foregroundColor: KyXColors.text,
          elevation: 0,
          scrolledUnderElevation: 0,
        ),
        body: RefreshIndicator(
          onRefresh: _refreshRecords,
          child: ListView(
            padding: const EdgeInsets.only(bottom: 28),
            children: [
              const _ReceptionHeader(),
              _KeywordWatchPanel(
                snapshot: _keywordWatchSnapshot,
                isRecording: _isRecording,
                isBusy: _isBusy,
                isSubmitting: _isSubmitting,
                onToggle: _toggleKeywordWatch,
                onRefresh: _refreshKeywordModelStatus,
                onConfigTap: _openKeywordConfigSheet,
              ),
              _RecorderPanel(
                elapsedSeconds: _elapsedSeconds,
                isRecording: _isRecording,
                isBusy: _isBusy,
                isPlaying: _isPlaying,
                isSubmitting: _isSubmitting,
                hasRecording: hasRecording,
                errorText: _errorText,
                onRecordTap: _isRecording ? _stopRecording : _startRecording,
                onPlayTap: _togglePlayback,
                onDiscardTap: _discardRecording,
                onSubmitTap: _submitRecording,
              ),
              const KyXSectionLabel('最近录音'),
              _RecordHistory(
                future: _recordsFuture,
                onRetry: _refreshRecords,
                onOpen: _openRecordDetail,
                onDelete: _deleteRecord,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ReceptionHeader extends StatelessWidget {
  const _ReceptionHeader();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 15, 16, 14),
      child: const Row(
        children: [
          Icon(
            Icons.record_voice_over_outlined,
            color: KyXColors.primary,
            size: 24,
          ),
          SizedBox(width: 12),
          Expanded(
            child: Text(
              '前台接待录音',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: KyXColors.text,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _KeywordWatchPanel extends StatelessWidget {
  final FrontDeskKeywordWatchSnapshot snapshot;
  final bool isRecording;
  final bool isBusy;
  final bool isSubmitting;
  final VoidCallback onToggle;
  final VoidCallback onRefresh;
  final VoidCallback onConfigTap;

  const _KeywordWatchPanel({
    required this.snapshot,
    required this.isRecording,
    required this.isBusy,
    required this.isSubmitting,
    required this.onToggle,
    required this.onRefresh,
    required this.onConfigTap,
  });

  @override
  Widget build(BuildContext context) {
    final canToggle =
        !isRecording && !isBusy && !isSubmitting && !snapshot.isUnavailable;
    final color = _stateColor;
    final subtitle = _subtitle;
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(bottom: BorderSide(color: KyXColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(16, 13, 16, 13),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            width: 38,
            height: 38,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(_stateIcon, color: color, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(_title, style: KyXText.bodyStrong),
                if (subtitle.isNotEmpty) ...[
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: KyXText.secondary,
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            onPressed: isRecording || isBusy || isSubmitting
                ? null
                : onConfigTap,
            icon: const Icon(Icons.tune, size: 20),
            color: KyXColors.textSecondary,
            disabledColor: KyXColors.textTertiary,
            tooltip: '关键词设置',
          ),
          if (snapshot.isUnavailable)
            IconButton(
              onPressed: onRefresh,
              icon: const Icon(Icons.refresh, size: 20),
              color: KyXColors.textSecondary,
              tooltip: '刷新模型状态',
            )
          else
            TextButton(
              onPressed: canToggle ? onToggle : null,
              style: TextButton.styleFrom(
                foregroundColor: snapshot.isListening
                    ? KyXColors.red
                    : KyXColors.primary,
                disabledForegroundColor: KyXColors.textTertiary,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                minimumSize: const Size(56, 36),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: snapshot.isStarting
                  ? const SizedBox(
                      width: 17,
                      height: 17,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(snapshot.isListening ? '关闭' : '开启'),
            ),
        ],
      ),
    );
  }

  String get _title {
    switch (snapshot.state) {
      case FrontDeskKeywordWatchState.starting:
        return '正在启动关键词值守';
      case FrontDeskKeywordWatchState.listening:
        return '关键词值守中';
      case FrontDeskKeywordWatchState.triggered:
        return '已触发接待录音';
      case FrontDeskKeywordWatchState.unavailable:
        return '关键词模型未安装';
      case FrontDeskKeywordWatchState.error:
        return '关键词值守异常';
      case FrontDeskKeywordWatchState.idle:
        return '关键词快速录音';
    }
  }

  String get _subtitle {
    if (isRecording) return '结束语自动停止，可手动结束';
    final status = snapshot.modelStatus;
    if (snapshot.isUnavailable) return '暂时使用手动录音，模型下发后可开启自动触发';
    if (snapshot.state == FrontDeskKeywordWatchState.error) {
      return snapshot.message;
    }
    if (snapshot.state == FrontDeskKeywordWatchState.triggered &&
        snapshot.keyword != null) {
      return '识别到 ${snapshot.keyword}';
    }
    if (snapshot.isListening) {
      return '正在等待唤醒语';
    }
    if (status != null && status.ready) {
      return '欢迎光临即刻唤醒';
    }
    return '欢迎光临即刻唤醒';
  }

  IconData get _stateIcon {
    switch (snapshot.state) {
      case FrontDeskKeywordWatchState.starting:
        return Icons.sync;
      case FrontDeskKeywordWatchState.listening:
        return Icons.hearing;
      case FrontDeskKeywordWatchState.triggered:
        return Icons.bolt_outlined;
      case FrontDeskKeywordWatchState.unavailable:
        return Icons.model_training_outlined;
      case FrontDeskKeywordWatchState.error:
        return Icons.error_outline;
      case FrontDeskKeywordWatchState.idle:
        return Icons.tips_and_updates_outlined;
    }
  }

  Color get _stateColor {
    switch (snapshot.state) {
      case FrontDeskKeywordWatchState.listening:
      case FrontDeskKeywordWatchState.triggered:
        return KyXColors.green;
      case FrontDeskKeywordWatchState.unavailable:
        return KyXColors.amber;
      case FrontDeskKeywordWatchState.error:
        return KyXColors.red;
      case FrontDeskKeywordWatchState.starting:
      case FrontDeskKeywordWatchState.idle:
        return KyXColors.primary;
    }
  }
}

class _KeywordConfigSheet extends StatefulWidget {
  final FrontDeskKeywordConfig initialConfig;

  const _KeywordConfigSheet({required this.initialConfig});

  @override
  State<_KeywordConfigSheet> createState() => _KeywordConfigSheetState();
}

class _KeywordConfigSheetState extends State<_KeywordConfigSheet> {
  late Set<String> _startKeywords;
  late Set<String> _stopKeywords;

  @override
  void initState() {
    super.initState();
    _startKeywords = widget.initialConfig.startKeywords.toSet();
    _stopKeywords = widget.initialConfig.stopKeywords.toSet();
  }

  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.of(context).padding.bottom;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 10, 16, bottomPadding + 16),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: KyXColors.line,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                const Expanded(
                  child: Text(
                    '关键词设置',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: KyXColors.text,
                    ),
                  ),
                ),
                TextButton(onPressed: _resetDefault, child: const Text('默认')),
              ],
            ),
            const SizedBox(height: 4),
            const Text(
              '每组 1-5 个。结束语建议只保留一个完整句，减少接待中途误停。',
              style: KyXText.secondary,
            ),
            const SizedBox(height: 18),
            _KeywordOptionGroup(
              title: '开始录音',
              selected: _startKeywords,
              candidates: FrontDeskKeywordConfig.startCandidates,
              onToggle: (keyword) => _toggleKeyword(_startKeywords, keyword),
            ),
            const SizedBox(height: 18),
            _KeywordOptionGroup(
              title: '结束录音',
              selected: _stopKeywords,
              candidates: FrontDeskKeywordConfig.stopCandidates,
              onToggle: (keyword) => _toggleKeyword(_stopKeywords, keyword),
            ),
            const SizedBox(height: 22),
            FilledButton(
              onPressed: _save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(44),
                backgroundColor: KyXColors.primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text('保存配置'),
            ),
          ],
        ),
      ),
    );
  }

  void _toggleKeyword(Set<String> selected, String keyword) {
    setState(() {
      if (selected.contains(keyword)) {
        if (selected.length <= FrontDeskKeywordConfig.minKeywords) return;
        selected.remove(keyword);
        return;
      }
      if (selected.length >= FrontDeskKeywordConfig.maxKeywords) return;
      selected.add(keyword);
    });
  }

  void _resetDefault() {
    setState(() {
      _startKeywords = FrontDeskKeywordConfig.defaultStartKeywords.toSet();
      _stopKeywords = FrontDeskKeywordConfig.defaultStopKeywords.toSet();
    });
  }

  void _save() {
    final config = FrontDeskKeywordConfig.sanitize(
      startKeywords: FrontDeskKeywordConfig.startCandidates.where(
        _startKeywords.contains,
      ),
      stopKeywords: FrontDeskKeywordConfig.stopCandidates.where(
        _stopKeywords.contains,
      ),
    );
    Navigator.of(context).pop(config);
  }
}

class _KeywordOptionGroup extends StatelessWidget {
  final String title;
  final Set<String> selected;
  final List<String> candidates;
  final ValueChanged<String> onToggle;

  const _KeywordOptionGroup({
    required this.title,
    required this.selected,
    required this.candidates,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(child: Text(title, style: KyXText.bodyStrong)),
            Text('${selected.length}/5', style: KyXText.secondary),
          ],
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: candidates.map((keyword) {
            final isSelected = selected.contains(keyword);
            return FilterChip(
              selected: isSelected,
              label: Text(keyword),
              showCheckmark: false,
              onSelected: (_) => onToggle(keyword),
              selectedColor: KyXColors.primary.withValues(alpha: 0.12),
              backgroundColor: KyXColors.lineSoft,
              labelStyle: TextStyle(
                fontSize: 13,
                fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                color: isSelected ? KyXColors.primary : KyXColors.text,
              ),
              side: BorderSide(
                color: isSelected ? KyXColors.primary : KyXColors.line,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _RecorderPanel extends StatelessWidget {
  final int elapsedSeconds;
  final bool isRecording;
  final bool isBusy;
  final bool isPlaying;
  final bool isSubmitting;
  final bool hasRecording;
  final String? errorText;
  final VoidCallback onRecordTap;
  final VoidCallback onPlayTap;
  final VoidCallback onDiscardTap;
  final VoidCallback onSubmitTap;

  const _RecorderPanel({
    required this.elapsedSeconds,
    required this.isRecording,
    required this.isBusy,
    required this.isPlaying,
    required this.isSubmitting,
    required this.hasRecording,
    required this.errorText,
    required this.onRecordTap,
    required this.onPlayTap,
    required this.onDiscardTap,
    required this.onSubmitTap,
  });

  @override
  Widget build(BuildContext context) {
    final canControl = !isBusy && !isSubmitting;
    return Container(
      decoration: const BoxDecoration(
        color: KyXColors.surface,
        border: Border(
          top: BorderSide(color: KyXColors.line),
          bottom: BorderSide(color: KyXColors.line),
        ),
      ),
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
      child: Column(
        children: [
          Text(
            _formatDuration(elapsedSeconds),
            style: const TextStyle(
              fontSize: 36,
              height: 1.05,
              fontWeight: FontWeight.w800,
              color: KyXColors.text,
            ),
          ),
          const SizedBox(height: 7),
          Text(_recordStatusText, style: KyXText.secondary),
          const SizedBox(height: 18),
          Material(
            color: isRecording
                ? KyXColors.red.withValues(alpha: 0.1)
                : KyXColors.primary.withValues(alpha: 0.1),
            shape: const CircleBorder(),
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: canControl ? onRecordTap : null,
              child: SizedBox(
                width: 82,
                height: 82,
                child: Center(
                  child: isBusy
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(strokeWidth: 2.2),
                        )
                      : Icon(
                          isRecording ? Icons.stop : Icons.mic_none,
                          color: isRecording
                              ? KyXColors.red
                              : KyXColors.primary,
                          size: 33,
                        ),
                ),
              ),
            ),
          ),
          if (errorText != null) ...[
            const SizedBox(height: 14),
            Text(
              errorText!,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 13, color: KyXColors.red),
            ),
          ],
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: canControl && hasRecording ? onPlayTap : null,
                  icon: Icon(isPlaying ? Icons.stop : Icons.play_arrow),
                  label: Text(isPlaying ? '停止' : '播放'),
                  style: _outlineButtonStyle(),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: canControl && (hasRecording || isRecording)
                      ? onDiscardTap
                      : null,
                  icon: const Icon(Icons.refresh),
                  label: const Text('重录'),
                  style: _outlineButtonStyle(),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          FilledButton.icon(
            onPressed: canControl && hasRecording ? onSubmitTap : null,
            icon: isSubmitting
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Icon(Icons.cloud_upload_outlined, size: 18),
            label: Text(isSubmitting ? '提交中' : '提交 OA'),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(44),
              backgroundColor: KyXColors.primary,
              foregroundColor: Colors.white,
              disabledBackgroundColor: KyXColors.lineSoft,
              disabledForegroundColor: KyXColors.textTertiary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
          ),
        ],
      ),
    );
  }

  String get _recordStatusText {
    if (isSubmitting) return '提交中';
    if (isRecording) return '录音中，结束语自动停止';
    if (isPlaying) return '播放中';
    if (hasRecording) return '待提交';
    return '待录音 · 欢迎光临即刻唤醒';
  }
}

class _RecordHistory extends StatelessWidget {
  final Future<List<AiReceptionRecord>> future;
  final Future<void> Function() onRetry;
  final Future<void> Function(AiReceptionRecord record) onOpen;
  final Future<void> Function(AiReceptionRecord record) onDelete;

  const _RecordHistory({
    required this.future,
    required this.onRetry,
    required this.onOpen,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<AiReceptionRecord>>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: CircularProgressIndicator()),
          );
        }
        if (snapshot.hasError) {
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Column(
              children: [
                Text(
                  _cleanError(snapshot.error),
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 13, color: KyXColors.red),
                ),
                const SizedBox(height: 10),
                OutlinedButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('重试'),
                  style: _outlineButtonStyle(),
                ),
              ],
            ),
          );
        }

        final records = snapshot.data ?? const <AiReceptionRecord>[];
        if (records.isEmpty) {
          return const SizedBox(
            height: 160,
            child: Center(child: Text('暂无录音记录', style: KyXText.secondary)),
          );
        }

        final visibleRecords = records.take(10).toList();
        return KyXListSection(
          children: visibleRecords
              .asMap()
              .entries
              .map(
                (entry) => _ReceptionRecordRow(
                  record: entry.value,
                  showDivider: entry.key != visibleRecords.length - 1,
                  onTap: () => onOpen(entry.value),
                  onDelete: () => onDelete(entry.value),
                ),
              )
              .toList(),
        );
      },
    );
  }
}

class _ReceptionRecordRow extends StatelessWidget {
  final AiReceptionRecord record;
  final bool showDivider;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  const _ReceptionRecordRow({
    required this.record,
    required this.showDivider,
    required this.onTap,
    required this.onDelete,
  });


  Future<void> _showActions(BuildContext context) async {
    final action = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: KyXColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
      ),
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 8),
            Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: KyXColors.line,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 8),
            ListTile(
              leading: const Icon(Icons.open_in_new, color: KyXColors.primary),
              title: const Text('查看详情'),
              onTap: () => Navigator.pop(context, 'open'),
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline, color: KyXColors.red),
              title: const Text('删除录音'),
              onTap: () => Navigator.pop(context, 'delete'),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
    if (action == 'open') onTap();
    if (action == 'delete') onDelete();
  }

  @override
  Widget build(BuildContext context) {
    final title = _joinText([
      record.visitorName,
      record.visitorCompany,
      record.purpose,
    ]);
    final subtitle = _joinText([
      _shortDate(record.createTime),
      record.duration > 0 ? _formatDuration(record.duration) : null,
      record.contactPerson == null ? null : '对接 ${record.contactPerson}',
    ]);
    final statusText = _receptionStatusText(record.status);
    return KyXListRow(
      onTap: onTap,
      onLongPress: () => _showActions(context),
      leading: Container(
        width: 36,
        height: 36,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: _receptionStatusColor(record.status).withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Icon(
          Icons.graphic_eq,
          size: 19,
          color: _receptionStatusColor(record.status),
        ),
      ),
      title: title.isEmpty ? '接待录音 #${record.id ?? '-'}' : title,
      subtitle: subtitle,
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _StatusBadge(
            text: statusText,
            color: _receptionStatusColor(record.status),
          ),
          const SizedBox(width: 6),
          IconButton(
            onPressed: () => _showActions(context),
            icon: const Icon(Icons.more_horiz, size: 20),
            color: KyXColors.textTertiary,
            tooltip: '更多操作',
            visualDensity: VisualDensity.compact,
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
            padding: EdgeInsets.zero,
          ),
        ],
      ),
      showDivider: showDivider,
    );
  }
}

class ReceptionRecordDetailPage extends StatelessWidget {
  final int recordId;
  final String title;

  const ReceptionRecordDetailPage({
    super.key,
    required this.recordId,
    this.title = '接待录音',
  });

  @override
  Widget build(BuildContext context) {
    return _ReceptionRecordDetailPage(
      initialRecord: AiReceptionRecord.fromJson({
        'id': recordId,
        'visitorName': title,
      }),
    );
  }
}

class _ReceptionRecordDetailPage extends StatefulWidget {
  final AiReceptionRecord initialRecord;

  const _ReceptionRecordDetailPage({required this.initialRecord});

  @override
  State<_ReceptionRecordDetailPage> createState() =>
      _ReceptionRecordDetailPageState();
}

class _ReceptionRecordDetailPageState extends State<_ReceptionRecordDetailPage>
    with SingleTickerProviderStateMixin {
  late Future<AiReceptionRecord> _recordFuture;
  late final TabController _tabController;
  Timer? _pollTimer;
  bool _isPollingRefresh = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _recordFuture = _loadRecord();
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _tabController.dispose();
    super.dispose();
  }

  Future<AiReceptionRecord> _loadRecord() async {
    final id = widget.initialRecord.id;
    if (id == null) return Future.value(widget.initialRecord);
    final record = await AiReceptionService.getRecord(id);
    if (mounted) _syncPoller(record);
    return record;
  }

  Future<void> _refresh() async {
    final nextFuture = _loadRecord();
    setState(() => _recordFuture = nextFuture);
    await nextFuture;
  }

  void _syncPoller(AiReceptionRecord record) {
    final shouldPoll =
        record.id != null && _isReceptionProcessing(record.status);
    if (!shouldPoll) {
      _pollTimer?.cancel();
      _pollTimer = null;
      return;
    }

    _pollTimer ??= Timer.periodic(const Duration(seconds: 5), (_) {
      _pollRecordOnce();
    });
  }

  Future<void> _pollRecordOnce() async {
    if (!mounted || _isPollingRefresh) return;
    _isPollingRefresh = true;
    final nextFuture = _loadRecord();
    setState(() => _recordFuture = nextFuture);
    try {
      await nextFuture;
    } catch (_) {
      // Keep the last visible state; manual pull-to-refresh can surface errors.
    } finally {
      _isPollingRefresh = false;
    }
  }

  Future<void> _shareToChat() async {
    AiReceptionRecord record;
    try {
      record = await _recordFuture;
    } catch (_) {
      record = widget.initialRecord;
    }
    if (!mounted) return;
    final sent = await showOaChatShareSheet(
      context,
      _buildReceptionSharePayload(record),
    );
    if (!mounted || sent != true) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('已发送到会话')));
  }

  OaChatSharePayload _buildReceptionSharePayload(AiReceptionRecord record) {
    final title = _firstText([
      record.visitorName,
      record.purpose,
      record.summaryText,
      '接待录音',
    ]);
    return OaChatSharePayload(
      module: '接待',
      objectType: 'reception',
      objectId: record.id?.toString(),
      title: title,
      status: _receptionStatusText(record.status),
      fields: [
        OaChatShareField(label: '来访人', value: record.visitorName ?? '-'),
        OaChatShareField(label: '来访公司', value: record.visitorCompany ?? '-'),
        OaChatShareField(label: '对接人', value: record.contactPerson ?? '-'),
        if (_hasText(record.urgency))
          OaChatShareField(label: '紧急程度', value: _urgencyText(record.urgency)),
        if (record.duration > 0)
          OaChatShareField(
            label: '录音时长',
            value: _formatDuration(record.duration),
          ),
        OaChatShareField(label: '记录人', value: record.operatorName ?? '-'),
        OaChatShareField(label: '记录时间', value: _shortDate(record.createTime)),
      ],
      summary: _shortShareText(
        _firstText([record.summaryText, record.transcriptText]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: KyXColors.bg,
      appBar: AppBar(
        title: const Text(
          '接待详情',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        backgroundColor: KyXColors.surface,
        foregroundColor: KyXColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        actions: [
          IconButton(
            tooltip: '发送到会话',
            onPressed: _shareToChat,
            icon: const Icon(Icons.ios_share_outlined),
          ),
        ],
      ),
      body: FutureBuilder<AiReceptionRecord>(
        future: _recordFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting &&
              !snapshot.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return RefreshIndicator(
              onRefresh: _refresh,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(24),
                children: [
                  const SizedBox(height: 120),
                  Text(
                    _cleanError(snapshot.error),
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 13, color: KyXColors.red),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _refresh,
                    icon: const Icon(Icons.refresh, size: 18),
                    label: const Text('重新加载'),
                    style: _outlineButtonStyle(),
                  ),
                ],
              ),
            );
          }

          final record = snapshot.data ?? widget.initialRecord;
          return _RecordDetailTabs(
            record: record,
            onRefresh: _refresh,
            controller: _tabController,
          );
        },
      ),
    );
  }
}

class _RecordDetailTabs extends StatelessWidget {
  final AiReceptionRecord record;
  final Future<void> Function() onRefresh;
  final TabController controller;

  const _RecordDetailTabs({
    required this.record,
    required this.onRefresh,
    required this.controller,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _RecordDetailHeader(record: record),
        Container(
          color: KyXColors.surface,
          child: TabBar(
            controller: controller,
            labelColor: KyXColors.primary,
            unselectedLabelColor: KyXColors.textSecondary,
            labelStyle: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w700,
            ),
            unselectedLabelStyle: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
            ),
            indicatorColor: KyXColors.primary,
            indicatorWeight: 2.5,
            indicatorSize: TabBarIndicatorSize.label,
            dividerColor: KyXColors.line,
            tabs: const [
              Tab(text: '概要'),
              Tab(text: '对话'),
              Tab(text: '原文'),
              Tab(text: '处理'),
            ],
          ),
        ),
        Expanded(
          child: TabBarView(
            controller: controller,
            children: [
              _RecordDetailTabScroll(
                onRefresh: onRefresh,
                children: [
                  _RecordAnalysisSection(record: record),
                  _RecordRoleSection(record: record),
                  _RecordInsightSection(record: record),
                ],
              ),
              _RecordDialogueTab(record: record, onRefresh: onRefresh),
              _RecordDetailTabScroll(
                onRefresh: onRefresh,
                children: [_RecordTranscriptSection(record: record)],
              ),
              _RecordDetailTabScroll(
                onRefresh: onRefresh,
                children: [
                  _RecordAudioSection(record: record),
                  _RecordMetaSection(record: record),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _RecordDetailTabScroll extends StatelessWidget {
  final Future<void> Function() onRefresh;
  final List<Widget> children;

  const _RecordDetailTabScroll({
    required this.onRefresh,
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 28),
        children: children,
      ),
    );
  }
}

class _RecordDetailHeader extends StatelessWidget {
  final AiReceptionRecord record;

  const _RecordDetailHeader({required this.record});

  @override
  Widget build(BuildContext context) {
    final title = _firstText([
      record.visitorName,
      record.purpose,
      record.summaryText,
      '接待录音',
    ]);
    final subtitle = _joinText([
      _shortDate(record.createTime),
      record.duration > 0 ? _formatDuration(record.duration) : null,
      record.operatorName == null ? null : '记录人 ${record.operatorName}',
    ]);
    return Container(
      color: KyXColors.surface,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 42,
            height: 42,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: _receptionStatusColor(
                record.status,
              ).withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(
              Icons.graphic_eq,
              color: _receptionStatusColor(record.status),
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 18,
                    height: 1.22,
                    fontWeight: FontWeight.w800,
                    color: KyXColors.text,
                  ),
                ),
                if (subtitle.isNotEmpty) ...[
                  const SizedBox(height: 6),
                  Text(subtitle, style: KyXText.secondary),
                ],
              ],
            ),
          ),
          const SizedBox(width: 12),
          _StatusBadge(
            text: _receptionStatusText(record.status),
            color: _receptionStatusColor(record.status),
          ),
        ],
      ),
    );
  }
}

class _RecordAnalysisSection extends StatelessWidget {
  final AiReceptionRecord record;

  const _RecordAnalysisSection({required this.record});

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[
      if (_hasText(record.visitorName))
        _DetailLine(label: '来访人', value: record.visitorName!),
      if (_hasText(record.visitorCompany))
        _DetailLine(label: '来访公司', value: record.visitorCompany!),
      if (_hasText(record.purpose))
        _DetailLine(label: '来访目的', value: record.purpose!),
      if (_hasText(record.contactPerson))
        _DetailLine(label: '对接人', value: record.contactPerson!),
      if (_hasText(record.urgency))
        _DetailLine(label: '紧急程度', value: _urgencyText(record.urgency)),
      if (_hasText(record.summaryText))
        _DetailLine(label: '摘要', value: record.summaryText!, multiline: true),
      for (final todo in record.todoTexts)
        _DetailLine(label: '待办', value: todo, multiline: true),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const KyXSectionLabel('解析结果'),
        KyXListSection(
          children: rows.isEmpty
              ? const [_EmptyDetailHint(text: 'AI 未提取出明确的接待对象或事项，可查看下方转写原文。')]
              : _withDividers(rows),
        ),
      ],
    );
  }
}

class _RecordRoleSection extends StatelessWidget {
  final AiReceptionRecord record;

  const _RecordRoleSection({required this.record});

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[
      if (record.canDistinguishRoles != null)
        _DetailLine(
          label: '角色判断',
          value: record.canDistinguishRoles! ? '可区分客户与员工' : '暂无法可靠区分',
        ),
      if (_hasText(record.roleConfidence))
        _DetailLine(
          label: '置信度',
          value: _confidenceText(record.roleConfidence),
        ),
      if (_hasText(record.roleDetectionReason))
        _DetailLine(
          label: '判断依据',
          value: record.roleDetectionReason!,
          multiline: true,
        ),
    ];
    if (rows.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const KyXSectionLabel('角色识别'),
        KyXListSection(children: _withDividers(rows)),
      ],
    );
  }
}

class _RecordDialogueTab extends StatelessWidget {
  final AiReceptionRecord record;
  final Future<void> Function() onRefresh;

  const _RecordDialogueTab({required this.record, required this.onRefresh});

  @override
  Widget build(BuildContext context) {
    final turns = record.dialogueTurns;
    if (turns.isEmpty) {
      return _RecordDetailTabScroll(
        onRefresh: onRefresh,
        children: const [
          KyXSectionLabel('对话轮次'),
          KyXListSection(child: _EmptyDetailHint(text: '暂无可展示的对话轮次，可查看转写原文。')),
        ],
      );
    }

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 28),
        itemCount: turns.length + 1,
        itemBuilder: (context, index) {
          if (index == 0) return KyXSectionLabel('对话轮次 (${turns.length})');

          final turnIndex = index - 1;
          final isFirst = turnIndex == 0;
          final isLast = turnIndex == turns.length - 1;
          return DecoratedBox(
            decoration: BoxDecoration(
              color: KyXColors.surface,
              border: Border(
                top: isFirst
                    ? const BorderSide(color: KyXColors.line)
                    : BorderSide.none,
                bottom: isLast
                    ? const BorderSide(color: KyXColors.line)
                    : BorderSide.none,
              ),
            ),
            child: Column(
              children: [
                _DialogueTurnLine(turn: turns[turnIndex]),
                if (!isLast)
                  const Divider(height: 1, color: KyXColors.lineSoft),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _RecordInsightSection extends StatelessWidget {
  final AiReceptionRecord record;

  const _RecordInsightSection({required this.record});

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[
      for (final point in record.keyPointTexts)
        _DetailLine(label: '关键事实', value: point, multiline: true),
      if (_hasText(record.customerEmotion))
        _DetailLine(label: '客户情绪', value: _emotionText(record.customerEmotion)),
      if (record.customerNeedTexts.isNotEmpty)
        _DetailLine(
          label: '客户诉求',
          value: _joinBullets(record.customerNeedTexts),
          multiline: true,
        ),
      if (record.customerConcernTexts.isNotEmpty)
        _DetailLine(
          label: '客户顾虑',
          value: _joinBullets(record.customerConcernTexts),
          multiline: true,
        ),
      if (_hasText(record.employeeResponseQuality))
        _DetailLine(
          label: '服务质量',
          value: _qualityText(record.employeeResponseQuality),
        ),
      if (_hasText(record.employeeSentiment))
        _DetailLine(
          label: '员工状态',
          value: _employeeSentimentText(record.employeeSentiment),
        ),
      if (record.qualityScore != null)
        _DetailLine(label: '质检评分', value: '${record.qualityScore} 分'),
      if (record.qualityStrengthTexts.isNotEmpty)
        _DetailLine(
          label: '做得好',
          value: _joinBullets(record.qualityStrengthTexts),
          multiline: true,
        ),
      if (record.qualityImprovementTexts.isNotEmpty)
        _DetailLine(
          label: '改进项',
          value: _joinBullets(record.qualityImprovementTexts),
          multiline: true,
        ),
      if (record.employeeMissingActionTexts.isNotEmpty)
        _DetailLine(
          label: '遗漏动作',
          value: _joinBullets(record.employeeMissingActionTexts),
          multiline: true,
        ),
      if (record.riskPointTexts.isNotEmpty)
        _DetailLine(
          label: '风险点',
          value: _joinBullets(record.riskPointTexts),
          multiline: true,
        ),
      if (record.followUpQuestionTexts.isNotEmpty)
        _DetailLine(
          label: '待追问',
          value: _joinBullets(record.followUpQuestionTexts),
          multiline: true,
        ),
    ];
    if (rows.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const KyXSectionLabel('深度洞察'),
        KyXListSection(children: _withDividers(rows)),
      ],
    );
  }
}

class _DialogueTurnLine extends StatelessWidget {
  final AiReceptionDialogueTurn turn;

  const _DialogueTurnLine({required this.turn});

  @override
  Widget build(BuildContext context) {
    final meta = _joinText([
      _intentText(turn.intent),
      _confidenceText(turn.confidence),
    ]);
    final color = _speakerColor(turn.speaker);
    return SizedBox(
      width: double.infinity,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 13, 16, 13),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 42,
              height: 24,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                _speakerLabel(turn),
                style: TextStyle(
                  fontSize: 12,
                  height: 1.1,
                  fontWeight: FontWeight.w700,
                  color: color,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    turn.text,
                    softWrap: true,
                    overflow: TextOverflow.visible,
                    style: KyXText.body,
                  ),
                  if (meta.isNotEmpty) ...[
                    const SizedBox(height: 5),
                    Text(
                      meta,
                      softWrap: true,
                      overflow: TextOverflow.visible,
                      style: KyXText.caption,
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _RecordTranscriptSection extends StatelessWidget {
  final AiReceptionRecord record;

  const _RecordTranscriptSection({required this.record});

  @override
  Widget build(BuildContext context) {
    final text = record.transcriptText;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const KyXSectionLabel('转写原文'),
        KyXListSection(
          child: SizedBox(
            width: double.infinity,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 15),
              child: Text(
                _hasText(text) ? text! : '暂未生成转写内容',
                softWrap: true,
                overflow: TextOverflow.visible,
                style: _hasText(text) ? KyXText.body : KyXText.secondary,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _RecordAudioSection extends StatefulWidget {
  final AiReceptionRecord record;

  const _RecordAudioSection({required this.record});

  @override
  State<_RecordAudioSection> createState() => _RecordAudioSectionState();
}

class _RecordAudioSectionState extends State<_RecordAudioSection> {
  final AudioPlayer _player = AudioPlayer();
  StreamSubscription<void>? _completeSub;
  bool _isPlaying = false;
  bool _isBusy = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _completeSub = _player.onPlayerComplete.listen((_) {
      if (mounted) setState(() => _isPlaying = false);
    });
  }

  @override
  void didUpdateWidget(covariant _RecordAudioSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.record.audioPlaybackUrl != widget.record.audioPlaybackUrl) {
      unawaited(_stopPlayback());
    }
  }

  @override
  void dispose() {
    _completeSub?.cancel();
    _player.dispose();
    super.dispose();
  }

  Future<void> _togglePlayback() async {
    if (_isBusy) return;
    if (_isPlaying) {
      await _stopPlayback();
      return;
    }

    final url = widget.record.audioPlaybackUrl;
    if (!_hasText(url)) {
      setState(() => _errorText = '暂无原始录音文件');
      return;
    }

    setState(() {
      _isBusy = true;
      _errorText = null;
    });
    try {
      await _player.play(UrlSource(url!, mimeType: 'audio/mp4'));
      if (mounted) setState(() => _isPlaying = true);
    } catch (_) {
      if (mounted) setState(() => _errorText = '原始录音播放失败');
    } finally {
      if (mounted) setState(() => _isBusy = false);
    }
  }

  Future<void> _stopPlayback() async {
    try {
      await _player.stop();
    } catch (_) {}
    if (mounted) setState(() => _isPlaying = false);
  }

  @override
  Widget build(BuildContext context) {
    final hasAudio = _hasText(widget.record.audioPlaybackUrl);
    final subtitle = _joinText([
      widget.record.duration > 0
          ? '时长 ${_formatDuration(widget.record.duration)}'
          : null,
      hasAudio ? '原始录音已保存' : '暂无原始录音',
    ]);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const KyXSectionLabel('原始录音'),
        KyXListSection(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: Column(
              children: [
                Row(
                  children: [
                    Container(
                      width: 38,
                      height: 38,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: KyXColors.primary.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(
                        _isPlaying ? Icons.stop : Icons.play_arrow,
                        size: 22,
                        color: KyXColors.primary,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('录音文件', style: KyXText.bodyStrong),
                          const SizedBox(height: 3),
                          Text(subtitle, style: KyXText.secondary),
                        ],
                      ),
                    ),
                    const SizedBox(width: 12),
                    TextButton.icon(
                      onPressed: hasAudio && !_isBusy ? _togglePlayback : null,
                      icon: _isBusy
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Icon(_isPlaying ? Icons.stop : Icons.play_arrow),
                      label: Text(_isPlaying ? '停止' : '播放'),
                    ),
                  ],
                ),
                if (_errorText != null) ...[
                  const SizedBox(height: 10),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      _errorText!,
                      style: const TextStyle(
                        fontSize: 13,
                        color: KyXColors.red,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _RecordMetaSection extends StatelessWidget {
  final AiReceptionRecord record;

  const _RecordMetaSection({required this.record});

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[
      _DetailLine(label: '处理状态', value: _receptionStatusText(record.status)),
      if (_hasText(record.createTime))
        _DetailLine(label: '记录时间', value: _shortDate(record.createTime)),
      if (record.duration > 0)
        _DetailLine(label: '录音时长', value: _formatDuration(record.duration)),
      if (_hasText(record.operatorName))
        _DetailLine(label: '记录人', value: record.operatorName!),
      if (_hasText(record.errorMessage))
        _DetailLine(
          label: '失败原因',
          value: record.errorMessage!,
          multiline: true,
        ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const KyXSectionLabel('处理信息'),
        KyXListSection(children: _withDividers(rows)),
      ],
    );
  }
}

class _DetailLine extends StatelessWidget {
  final String label;
  final String value;
  final bool multiline;

  const _DetailLine({
    required this.label,
    required this.value,
    this.multiline = false,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          16,
          multiline ? 13 : 12,
          16,
          multiline ? 13 : 12,
        ),
        child: multiline
            ? Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label, style: KyXText.caption),
                  const SizedBox(height: 6),
                  Text(
                    value,
                    softWrap: true,
                    overflow: TextOverflow.visible,
                    style: KyXText.body,
                  ),
                ],
              )
            : Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 74,
                    child: Text(label, style: KyXText.secondary),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      value,
                      softWrap: true,
                      overflow: TextOverflow.visible,
                      textAlign: TextAlign.right,
                      style: KyXText.bodyStrong,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}

class _EmptyDetailHint extends StatelessWidget {
  final String text;

  const _EmptyDetailHint({required this.text});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 20),
        child: Text(
          text,
          softWrap: true,
          overflow: TextOverflow.visible,
          style: KyXText.secondary,
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String text;
  final Color color;

  const _StatusBadge({required this.text, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          color: color,
        ),
      ),
    );
  }
}

ButtonStyle _outlineButtonStyle() {
  return OutlinedButton.styleFrom(
    minimumSize: const Size.fromHeight(42),
    foregroundColor: KyXColors.text,
    side: const BorderSide(color: KyXColors.line),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
    textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
  );
}

String _formatDuration(int seconds) {
  final safeSeconds = seconds < 0 ? 0 : seconds;
  final minutes = safeSeconds ~/ 60;
  final remainder = safeSeconds % 60;
  return '${minutes.toString().padLeft(2, '0')}:${remainder.toString().padLeft(2, '0')}';
}

String _receptionStatusText(int? status) {
  switch (status) {
    case 0:
      return '排队中';
    case 1:
      return '识别中';
    case 2:
      return '已转写';
    case 3:
      return '已通知';
    case 4:
      return '失败';
    default:
      return '处理中';
  }
}

bool _isReceptionProcessing(int? status) => status == 0 || status == 1;

Color _receptionStatusColor(int? status) {
  switch (status) {
    case 2:
    case 3:
      return KyXColors.green;
    case 4:
      return KyXColors.red;
    case 1:
      return KyXColors.primary;
    case 0:
      return KyXColors.amber;
    default:
      return KyXColors.slate;
  }
}

String _speakerLabel(AiReceptionDialogueTurn turn) {
  switch (turn.speaker.toLowerCase()) {
    case 'customer':
    case 'visitor':
      return '客户';
    case 'employee':
    case 'staff':
    case 'operator':
      return '员工';
    default:
      return _hasText(turn.speakerLabel) ? turn.speakerLabel : '未知';
  }
}

Color _speakerColor(String speaker) {
  switch (speaker.toLowerCase()) {
    case 'customer':
    case 'visitor':
      return KyXColors.primary;
    case 'employee':
    case 'staff':
    case 'operator':
      return KyXColors.green;
    default:
      return KyXColors.slate;
  }
}

String _confidenceText(String? value) {
  switch ((value ?? '').trim().toLowerCase()) {
    case 'high':
      return '高置信度';
    case 'medium':
      return '中置信度';
    case 'low':
      return '低置信度';
    default:
      return value?.trim() ?? '';
  }
}

String? _intentText(String? value) {
  final text = value?.trim();
  if (text == null || text.isEmpty) return null;
  switch (text.toLowerCase()) {
    case 'consult':
    case 'consultation':
      return '咨询';
    case 'confirm':
    case 'confirmation':
      return '确认';
    case 'explain':
    case 'statement':
      return '说明';
    case 'promise':
    case 'commitment':
      return '承诺';
    case 'complaint':
      return '投诉';
    case 'other':
      return '其他';
    default:
      return text;
  }
}

String _emotionText(String? value) {
  switch ((value ?? '').trim().toLowerCase()) {
    case 'positive':
      return '积极';
    case 'neutral':
      return '中性';
    case 'negative':
      return '负面';
    case 'anxious':
      return '焦虑';
    case 'unknown':
      return '未知';
    default:
      return value?.trim() ?? '';
  }
}

String _qualityText(String? value) {
  switch ((value ?? '').trim().toLowerCase()) {
    case 'good':
      return '良好';
    case 'average':
      return '一般';
    case 'poor':
      return '需改进';
    case 'unknown':
      return '未知';
    default:
      return value?.trim() ?? '';
  }
}

String _employeeSentimentText(String? value) {
  switch ((value ?? '').trim().toLowerCase()) {
    case 'professional':
      return '专业';
    case 'neutral':
      return '中性';
    case 'impatient':
      return '不耐烦';
    case 'unknown':
      return '未知';
    default:
      return value?.trim() ?? '';
  }
}

String _urgencyText(String? value) {
  switch ((value ?? '').trim().toLowerCase()) {
    case 'high':
    case 'urgent':
      return '高';
    case 'medium':
      return '中';
    case 'normal':
    case 'low':
      return '普通';
    case 'unknown':
      return '未判断';
    default:
      return value?.trim() ?? '';
  }
}

String _joinBullets(List<String> values) {
  return values
      .where((value) => value.trim().isNotEmpty)
      .map((value) => '- ${value.trim()}')
      .join('\n');
}

String _joinText(List<String?> values) {
  return values
      .where((value) => value != null && value.trim().isNotEmpty)
      .map((value) => value!.trim())
      .join(' / ');
}

String _shortShareText(String value) {
  final text = value.replaceAll(RegExp(r'\s+'), ' ').trim();
  if (text.isEmpty) return '';
  return text.length <= 90 ? text : '${text.substring(0, 90)}...';
}

String _firstText(List<String?> values) {
  for (final value in values) {
    if (value != null && value.trim().isNotEmpty) return value.trim();
  }
  return '';
}

bool _hasText(String? value) => value != null && value.trim().isNotEmpty;

List<Widget> _withDividers(List<Widget> rows) {
  final widgets = <Widget>[];
  for (var i = 0; i < rows.length; i++) {
    widgets.add(rows[i]);
    if (i != rows.length - 1) {
      widgets.add(const Divider(height: 1, color: KyXColors.lineSoft));
    }
  }
  return widgets;
}

String _shortDate(String? value) {
  final raw = value?.trim();
  if (raw == null || raw.isEmpty) return '';

  final numeric = int.tryParse(raw);
  if (numeric != null) {
    if (raw.length >= 13) {
      return _formatLocalDateTime(
        DateTime.fromMillisecondsSinceEpoch(numeric).toLocal(),
      );
    }
    if (raw.length == 10) {
      return _formatLocalDateTime(
        DateTime.fromMillisecondsSinceEpoch(numeric * 1000).toLocal(),
      );
    }
  }

  final parsed = DateTime.tryParse(raw);
  if (parsed != null) return _formatLocalDateTime(parsed.toLocal());

  final normalized = raw.replaceFirst('T', ' ');
  return normalized.length > 16 ? normalized.substring(0, 16) : normalized;
}

String _formatLocalDateTime(DateTime dateTime) {
  final year = dateTime.year.toString().padLeft(4, '0');
  final month = dateTime.month.toString().padLeft(2, '0');
  final day = dateTime.day.toString().padLeft(2, '0');
  final hour = dateTime.hour.toString().padLeft(2, '0');
  final minute = dateTime.minute.toString().padLeft(2, '0');
  return '$year-$month-$day $hour:$minute';
}

String _cleanError(Object? error) {
  return (error?.toString() ?? '请求失败')
      .replaceFirst('Exception: ', '')
      .replaceFirst('ApiException: ', '');
}
