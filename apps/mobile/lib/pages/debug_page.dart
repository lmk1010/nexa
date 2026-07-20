import 'package:flutter/material.dart';

import '../services/api_service.dart';

class DebugPage extends StatefulWidget {
  const DebugPage({super.key});

  @override
  State<DebugPage> createState() => _DebugPageState();
}

class _DebugPageState extends State<DebugPage> {
  bool _loading = false;
  String _log = 'Ready';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('IM调试工具'),
        backgroundColor: Colors.blue,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ElevatedButton.icon(
              onPressed: _loading ? null : _loadTencentTicket,
              icon: _loading
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.key),
              label: const Text('获取腾讯 IM 登录票据'),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: SingleChildScrollView(
                child: SelectableText(_log),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _loadTencentTicket() async {
    setState(() {
      _loading = true;
      _log = 'Loading...';
    });

    try {
      final ticket = await ApiService.getTencentImLoginTicket();
      setState(() {
        _log = [
          'SDKAppID: ${ticket.sdkAppId}',
          'userID: ${ticket.userID}',
          'expire: ${ticket.expire}',
          'userSig length: ${ticket.userSig.length}',
        ].join('\n');
      });
    } catch (e) {
      setState(() {
        _log = 'Failed: $e';
      });
    } finally {
      setState(() {
        _loading = false;
      });
    }
  }
}
