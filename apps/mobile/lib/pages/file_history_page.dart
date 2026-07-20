// Agent 导出文件历史页 —— 显示当前用户所有导出的 excel/pdf 等
// 点击 → APP 内后台下载 → 用系统的 WPS/Excel/Numbers 打开
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:open_filex/open_filex.dart';

import '../services/file_download_service.dart';
import '../services/file_history_service.dart';

class FileHistoryPage extends StatefulWidget {
  const FileHistoryPage({super.key});

  @override
  State<FileHistoryPage> createState() => _FileHistoryPageState();
}

class _FileHistoryPageState extends State<FileHistoryPage> {
  List<ExportedFile>? _files;
  String? _error;
  bool _loading = false;
  final Map<int, double> _progress = {};

  static const _canvas = Color(0xFFF7F7F5);
  static const _ink = Color(0xFF0E0E10);
  static const _dim = Color(0xFF6E6E76);

  @override
  void initState() {
    super.initState();
    _fetch();
  }

  Future<void> _fetch() async {
    if (!mounted) return;
    setState(() => _loading = true);
    try {
      final l = await FileHistoryService.list();
      if (!mounted) return;
      setState(() {
        _files = l;
        _error = null;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  Future<void> _openFile(ExportedFile f) async {
    setState(() => _progress[f.id] = 0);
    final res = await FileDownloadService.downloadAndOpen(
      url: f.downloadUrl,
      filename: f.filename,
      onProgress: (p) {
        if (!mounted) return;
        setState(() => _progress[f.id] = p);
      },
    );
    if (!mounted) return;
    setState(() => _progress.remove(f.id));
    if (res.type != ResultType.done && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('打开失败：${res.message}'),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        foregroundColor: _ink,
        elevation: 0,
        scrolledUnderElevation: 0,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        title: const Text(
          '导出文件',
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: _ink),
        ),
      ),
      body: RefreshIndicator(
        onRefresh: _fetch,
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (_files == null && _loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _files == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text('加载失败：$_error', style: const TextStyle(color: _dim)),
        ),
      );
    }
    final files = _files ?? [];
    if (files.isEmpty) {
      return ListView(
        children: const [
          SizedBox(height: 120),
          Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Text(
                '还没有导出过文件\n\n在对话里跟 AI 说\n"帮我把 XX 导出为 Excel"',
                textAlign: TextAlign.center,
                style: TextStyle(color: _dim, height: 1.6, fontSize: 13),
              ),
            ),
          ),
        ],
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      itemCount: files.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (context, i) => _FileRow(
        file: files[i],
        progress: _progress[files[i].id],
        onTap: () => _openFile(files[i]),
      ),
    );
  }
}

class _FileRow extends StatelessWidget {
  final ExportedFile file;
  final double? progress;
  final VoidCallback onTap;

  const _FileRow({required this.file, required this.progress, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final isBusy = progress != null;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: isBusy ? null : onTap,
        child: Container(
          padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: const Color(0xFFEEEEEE)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: const Color(0x1415A47A),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.grid_on_rounded,
                      color: Color(0xFF15A47A),
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          file.filename,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: _FileHistoryPageState._ink,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          _subtitle(file),
                          style: const TextStyle(
                            fontSize: 11,
                            color: _FileHistoryPageState._dim,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    isBusy ? Icons.downloading_rounded : Icons.open_in_new_rounded,
                    color: const Color(0xFF15A47A),
                    size: 22,
                  ),
                ],
              ),
              if (isBusy) ...[
                const SizedBox(height: 10),
                ClipRRect(
                  borderRadius: BorderRadius.circular(2),
                  child: LinearProgressIndicator(
                    value: progress,
                    minHeight: 2.5,
                    backgroundColor: const Color(0xFFECECE8),
                    valueColor: const AlwaysStoppedAnimation(Color(0xFF15A47A)),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  String _subtitle(ExportedFile f) {
    final size = f.bytes > 1024
        ? '${(f.bytes / 1024).toStringAsFixed(1)} KB'
        : '${f.bytes} B';
    final rows = f.rows > 0 ? '${f.rows} 行 · ' : '';
    final when = f.createdAt != null ? _formatMinute(f.createdAt!) : '';
    return '$rows$size · $when';
  }
}

String _formatMinute(DateTime t) {
  final l = t.toLocal();
  return '${l.month.toString().padLeft(2, '0')}-${l.day.toString().padLeft(2, '0')} '
      '${l.hour.toString().padLeft(2, '0')}:${l.minute.toString().padLeft(2, '0')}';
}
