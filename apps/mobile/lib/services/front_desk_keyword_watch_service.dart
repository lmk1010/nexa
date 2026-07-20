import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:sherpa_onnx/sherpa_onnx.dart' as sherpa;

import 'front_desk_keyword_config_service.dart';

enum FrontDeskKeywordWatchState {
  idle,
  starting,
  listening,
  triggered,
  unavailable,
  error,
}

enum FrontDeskReceptionKeywordAction { start, stop }

class FrontDeskReceptionKeywordMatch {
  final FrontDeskReceptionKeywordAction action;
  final String keyword;

  const FrontDeskReceptionKeywordMatch({
    required this.action,
    required this.keyword,
  });

  bool get isStart => action == FrontDeskReceptionKeywordAction.start;
  bool get isStop => action == FrontDeskReceptionKeywordAction.stop;
}

class FrontDeskKeywordModelStatus {
  final String directoryPath;
  final bool ready;
  final String? encoderPath;
  final String? decoderPath;
  final String? joinerPath;
  final String? tokensPath;
  final String? keywordsPath;
  final List<String> missingFiles;
  final List<String> keywords;

  const FrontDeskKeywordModelStatus({
    required this.directoryPath,
    required this.ready,
    this.encoderPath,
    this.decoderPath,
    this.joinerPath,
    this.tokensPath,
    this.keywordsPath,
    this.missingFiles = const [],
    this.keywords = const [],
  });
}

class FrontDeskKeywordWatchSnapshot {
  final FrontDeskKeywordWatchState state;
  final String message;
  final String? keyword;
  final FrontDeskKeywordModelStatus? modelStatus;

  const FrontDeskKeywordWatchSnapshot({
    required this.state,
    required this.message,
    this.keyword,
    this.modelStatus,
  });

  bool get isListening => state == FrontDeskKeywordWatchState.listening;
  bool get isStarting => state == FrontDeskKeywordWatchState.starting;
  bool get isUnavailable => state == FrontDeskKeywordWatchState.unavailable;
}

class FrontDeskKeywordDetectorSession {
  final sherpa.KeywordSpotter _spotter;
  final sherpa.OnlineStream _stream;
  bool _isClosed = false;

  FrontDeskKeywordDetectorSession._({
    required sherpa.KeywordSpotter spotter,
    required sherpa.OnlineStream stream,
  }) : _spotter = spotter,
       _stream = stream;

  String? acceptPcm16(Uint8List chunk) {
    if (_isClosed || chunk.isEmpty) return null;
    final samples = FrontDeskKeywordWatchService._pcm16BytesToFloat32(chunk);
    if (samples.isEmpty) return null;

    _stream.acceptWaveform(
      samples: samples,
      sampleRate: FrontDeskKeywordWatchService._sampleRate,
    );
    while (_spotter.isReady(_stream)) {
      _spotter.decode(_stream);
      final keyword = _spotter.getResult(_stream).keyword.trim();
      if (keyword.isEmpty) continue;
      _spotter.reset(_stream);
      return keyword;
    }
    return null;
  }

  void dispose() {
    if (_isClosed) return;
    _isClosed = true;
    try {
      _stream.free();
    } catch (_) {}
    try {
      _spotter.free();
    } catch (_) {}
  }
}

class FrontDeskKeywordWatchService {
  static const int _sampleRate = 16000;
  static const int _numChannels = 1;
  static bool _bindingsInitialized = false;

  final AudioRecorder _recorder = AudioRecorder();
  final StreamController<FrontDeskKeywordWatchSnapshot> _snapshotController =
      StreamController<FrontDeskKeywordWatchSnapshot>.broadcast();

  StreamSubscription<Uint8List>? _audioSubscription;
  sherpa.KeywordSpotter? _spotter;
  sherpa.OnlineStream? _stream;
  FrontDeskKeywordWatchSnapshot _snapshot = const FrontDeskKeywordWatchSnapshot(
    state: FrontDeskKeywordWatchState.idle,
    message: '关键词监听未开启',
  );
  bool _isDisposed = false;
  bool _isStarting = false;
  bool _isListening = false;
  bool _hasTriggered = false;

  Stream<FrontDeskKeywordWatchSnapshot> get snapshots =>
      _snapshotController.stream;

  FrontDeskKeywordWatchSnapshot get snapshot => _snapshot;

  bool get isListening => _isListening;

  static FrontDeskReceptionKeywordMatch? classifyKeyword(
    String keyword, {
    FrontDeskKeywordConfig? config,
  }) {
    final activeConfig = config ?? FrontDeskKeywordConfig.defaults();
    final normalized = FrontDeskKeywordConfig.normalize(keyword);
    if (normalized.isEmpty) return null;
    if (activeConfig.startKeywordSet.contains(normalized)) {
      return FrontDeskReceptionKeywordMatch(
        action: FrontDeskReceptionKeywordAction.start,
        keyword: normalized,
      );
    }
    if (activeConfig.stopKeywordSet.contains(normalized)) {
      return FrontDeskReceptionKeywordMatch(
        action: FrontDeskReceptionKeywordAction.stop,
        keyword: normalized,
      );
    }
    return null;
  }

  static bool isStartKeyword(String keyword) {
    return classifyKeyword(keyword)?.isStart == true;
  }

  static bool isStopKeyword(String keyword) {
    return classifyKeyword(keyword)?.isStop == true;
  }

  Future<FrontDeskKeywordModelStatus> checkModelStatus() async {
    final directory = await _modelDirectory();
    if (!directory.existsSync()) {
      return FrontDeskKeywordModelStatus(
        directoryPath: directory.path,
        ready: false,
        missingFiles: const [
          'encoder*.onnx',
          'decoder*.onnx',
          'joiner*.onnx',
          'tokens.txt',
          'keywords.txt',
        ],
      );
    }

    final files =
        directory
            .listSync(recursive: true)
            .whereType<File>()
            .where(
              (file) =>
                  !file.path.split(Platform.pathSeparator).last.startsWith('.'),
            )
            .toList()
          ..sort((a, b) => a.path.compareTo(b.path));

    final encoder = _findFile(files, (name) {
      return name.endsWith('.onnx') && name.contains('encoder');
    });
    final decoder = _findFile(files, (name) {
      return name.endsWith('.onnx') && name.contains('decoder');
    });
    final joiner = _findFile(files, (name) {
      return name.endsWith('.onnx') && name.contains('joiner');
    });
    final tokens = _findFile(files, (name) => name == 'tokens.txt');
    final keywords =
        _findFile(files, (name) => name == 'keywords.txt') ??
        _findFile(files, (name) {
          return name.endsWith('.txt') && name.contains('keyword');
        });

    final missing = <String>[
      if (encoder == null) 'encoder*.onnx',
      if (decoder == null) 'decoder*.onnx',
      if (joiner == null) 'joiner*.onnx',
      if (tokens == null) 'tokens.txt',
      if (keywords == null) 'keywords.txt',
    ];

    return FrontDeskKeywordModelStatus(
      directoryPath: directory.path,
      ready: missing.isEmpty,
      encoderPath: encoder?.path,
      decoderPath: decoder?.path,
      joinerPath: joiner?.path,
      tokensPath: tokens?.path,
      keywordsPath: keywords?.path,
      missingFiles: missing,
      keywords: keywords == null ? const [] : _readKeywordPreview(keywords),
    );
  }

  Future<FrontDeskKeywordDetectorSession?> createDetectorSession() async {
    final status = await checkModelStatus();
    if (!status.ready) return null;
    _ensureBindings();
    final spotter = _buildSpotter(status);
    return FrontDeskKeywordDetectorSession._(
      spotter: spotter,
      stream: spotter.createStream(),
    );
  }

  Future<void> start({
    required FutureOr<void> Function(String keyword) onDetected,
    FrontDeskKeywordConfig? config,
  }) async {
    final activeConfig = config ?? FrontDeskKeywordConfig.defaults();
    if (_isDisposed || _isStarting || _isListening) return;
    _isStarting = true;
    _hasTriggered = false;
    _emit(
      const FrontDeskKeywordWatchSnapshot(
        state: FrontDeskKeywordWatchState.starting,
        message: '正在启动关键词监听',
      ),
    );

    try {
      final status = await checkModelStatus();
      if (!status.ready) {
        _emit(
          FrontDeskKeywordWatchSnapshot(
            state: FrontDeskKeywordWatchState.unavailable,
            message: '关键词模型未安装',
            modelStatus: status,
          ),
        );
        return;
      }

      final hasPermission = await _recorder.hasPermission();
      if (!hasPermission) {
        _emit(
          FrontDeskKeywordWatchSnapshot(
            state: FrontDeskKeywordWatchState.error,
            message: '麦克风权限未开启',
            modelStatus: status,
          ),
        );
        return;
      }

      _ensureBindings();
      _spotter = _buildSpotter(status);
      _stream = _spotter!.createStream();

      final audioStream = await _recorder.startStream(
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: _sampleRate,
          numChannels: _numChannels,
          audioInterruption: AudioInterruptionMode.none,
          androidConfig: AndroidRecordConfig(
            // ignore: deprecated_member_use
            service: AndroidService(
              title: '前台关键词值守中',
              content: '说出关键词后将自动开始接待录音。',
            ),
          ),
          echoCancel: true,
          noiseSuppress: true,
        ),
      );

      _isListening = true;
      _emit(
        FrontDeskKeywordWatchSnapshot(
          state: FrontDeskKeywordWatchState.listening,
          message: '正在等待关键词',
          modelStatus: status,
        ),
      );

      _audioSubscription = audioStream.listen(
        (chunk) => _handleAudioChunk(chunk, onDetected, activeConfig),
        onError: (Object error) {
          _emit(
            FrontDeskKeywordWatchSnapshot(
              state: FrontDeskKeywordWatchState.error,
              message: '关键词监听异常: ${_cleanError(error)}',
              modelStatus: status,
            ),
          );
          unawaited(stop());
        },
        onDone: () {
          if (_isListening && !_hasTriggered) {
            _emit(
              FrontDeskKeywordWatchSnapshot(
                state: FrontDeskKeywordWatchState.idle,
                message: '关键词监听已停止',
                modelStatus: status,
              ),
            );
          }
        },
      );
    } catch (error) {
      _emit(
        FrontDeskKeywordWatchSnapshot(
          state: FrontDeskKeywordWatchState.error,
          message: '关键词监听启动失败: ${_cleanError(error)}',
        ),
      );
      await stop(emitIdle: false);
    } finally {
      _isStarting = false;
    }
  }

  Future<void> stop({bool emitIdle = true}) async {
    _isStarting = false;
    final wasListening = _isListening;
    _isListening = false;

    await _audioSubscription?.cancel();
    _audioSubscription = null;

    try {
      if (wasListening || await _recorder.isRecording()) {
        await _recorder.stop();
      }
    } catch (_) {
      try {
        await _recorder.cancel();
      } catch (_) {}
    }

    _freeSherpaRuntime();

    if (emitIdle && !_isDisposed) {
      _emit(
        const FrontDeskKeywordWatchSnapshot(
          state: FrontDeskKeywordWatchState.idle,
          message: '关键词监听未开启',
        ),
      );
    }
  }

  Future<void> dispose() async {
    _isDisposed = true;
    await stop(emitIdle: false);
    await _snapshotController.close();
    await _recorder.dispose();
  }

  void _handleAudioChunk(
    Uint8List chunk,
    FutureOr<void> Function(String keyword) onDetected,
    FrontDeskKeywordConfig config,
  ) {
    if (_hasTriggered || !_isListening) return;
    final spotter = _spotter;
    final stream = _stream;
    if (spotter == null || stream == null) return;

    final samples = _pcm16BytesToFloat32(chunk);
    if (samples.isEmpty) return;

    try {
      stream.acceptWaveform(samples: samples, sampleRate: _sampleRate);
      while (spotter.isReady(stream)) {
        spotter.decode(stream);
        final keyword = spotter.getResult(stream).keyword.trim();
        if (keyword.isEmpty) continue;

        final match = classifyKeyword(keyword, config: config);
        spotter.reset(stream);
        if (match?.isStart != true) continue;

        _hasTriggered = true;
        _emit(
          FrontDeskKeywordWatchSnapshot(
            state: FrontDeskKeywordWatchState.triggered,
            message: '已识别关键词',
            keyword: match!.keyword,
            modelStatus: _snapshot.modelStatus,
          ),
        );
        unawaited(
          Future<void>(() async {
            await onDetected(match.keyword);
          }),
        );
        return;
      }
    } catch (error) {
      _emit(
        FrontDeskKeywordWatchSnapshot(
          state: FrontDeskKeywordWatchState.error,
          message: '关键词识别失败: ${_cleanError(error)}',
          modelStatus: _snapshot.modelStatus,
        ),
      );
      unawaited(stop(emitIdle: false));
    }
  }

  Future<Directory> _modelDirectory() async {
    if (Platform.isAndroid) {
      final externalDirectory = await getExternalStorageDirectory();
      if (externalDirectory != null) {
        return Directory(
          '${externalDirectory.path}${Platform.pathSeparator}sherpa_onnx_kws',
        );
      }
    }

    final supportDirectory = await getApplicationSupportDirectory();
    return Directory(
      '${supportDirectory.path}${Platform.pathSeparator}sherpa_onnx_kws',
    );
  }

  File? _findFile(List<File> files, bool Function(String name) matches) {
    for (final file in files) {
      final name = file.path.split(Platform.pathSeparator).last.toLowerCase();
      if (matches(name)) return file;
    }
    return null;
  }

  List<String> _readKeywordPreview(File file) {
    try {
      return file
          .readAsLinesSync()
          .map((line) => line.trim())
          .where((line) => line.isNotEmpty && !line.startsWith('#'))
          .take(6)
          .map((line) {
            final displayIndex = line.lastIndexOf('@');
            if (displayIndex >= 0 && displayIndex < line.length - 1) {
              return line.substring(displayIndex + 1).trim();
            }
            return line.split(RegExp(r'\s+')).first;
          })
          .where((line) => line.isNotEmpty)
          .toList();
    } catch (_) {
      return const [];
    }
  }

  static Float32List _pcm16BytesToFloat32(Uint8List bytes) {
    final sampleCount = bytes.lengthInBytes ~/ 2;
    if (sampleCount <= 0) return Float32List(0);

    final samples = Float32List(sampleCount);
    final data = ByteData.sublistView(bytes);
    for (var i = 0; i < sampleCount; i += 1) {
      samples[i] = data.getInt16(i * 2, Endian.little) / 32768.0;
    }
    return samples;
  }

  static void _ensureBindings() {
    if (_bindingsInitialized) return;
    sherpa.initBindings();
    _bindingsInitialized = true;
  }

  static sherpa.KeywordSpotter _buildSpotter(
    FrontDeskKeywordModelStatus status,
  ) {
    return sherpa.KeywordSpotter(
      sherpa.KeywordSpotterConfig(
        model: sherpa.OnlineModelConfig(
          transducer: sherpa.OnlineTransducerModelConfig(
            encoder: status.encoderPath!,
            decoder: status.decoderPath!,
            joiner: status.joinerPath!,
          ),
          tokens: status.tokensPath!,
          numThreads: 1,
          debug: false,
          modelingUnit: 'cjkchar',
        ),
        keywordsFile: status.keywordsPath!,
        keywordsScore: 1.0,
        keywordsThreshold: 0.20,
      ),
    );
  }

  void _freeSherpaRuntime() {
    try {
      _stream?.free();
    } catch (_) {}
    _stream = null;

    try {
      _spotter?.free();
    } catch (_) {}
    _spotter = null;
  }

  void _emit(FrontDeskKeywordWatchSnapshot snapshot) {
    _snapshot = snapshot;
    if (!_isDisposed && !_snapshotController.isClosed) {
      _snapshotController.add(snapshot);
    }
  }

  String _cleanError(Object error) {
    final text = error.toString().trim();
    if (text.isEmpty) return '未知错误';
    return text.replaceFirst(RegExp(r'^Exception:\s*'), '');
  }
}
