import 'dart:async';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';

import '../config/app_config.dart';
import 'chat_models.dart';
import 'notification_service.dart';
import 'storage_service.dart';
import 'tencent_im_service.dart';

export 'chat_models.dart';

class ChatConversation {
  final String id;
  final String name;
  final String avatar;
  final String lastMessage;
  final DateTime lastMessageTime;
  final int unreadCount;
  final String type;
  final bool isMuted;

  ChatConversation({
    required this.id,
    required this.name,
    required this.avatar,
    required this.lastMessage,
    required this.lastMessageTime,
    this.unreadCount = 0,
    this.type = 'single',
    this.isMuted = false,
  });

  factory ChatConversation.fromJson(Map<String, dynamic> json) {
    return ChatConversation(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      avatar: json['avatar']?.toString() ?? '',
      lastMessage: json['lastMessage']?.toString() ?? '',
      lastMessageTime: DateTime.fromMillisecondsSinceEpoch(
        _parseInt(json['lastMessageTime']) ?? 0,
      ),
      unreadCount: _parseInt(json['unreadCount']) ?? 0,
      type: json['type']?.toString() ?? 'single',
      isMuted: json['isMuted'] == true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'avatar': avatar,
      'lastMessage': lastMessage,
      'lastMessageTime': lastMessageTime.millisecondsSinceEpoch,
      'unreadCount': unreadCount,
      'type': type,
      'isMuted': isMuted,
    };
  }

  ChatConversation copyWith({
    String? id,
    String? name,
    String? avatar,
    String? lastMessage,
    DateTime? lastMessageTime,
    int? unreadCount,
    String? type,
    bool? isMuted,
  }) {
    return ChatConversation(
      id: id ?? this.id,
      name: name ?? this.name,
      avatar: avatar ?? this.avatar,
      lastMessage: lastMessage ?? this.lastMessage,
      lastMessageTime: lastMessageTime ?? this.lastMessageTime,
      unreadCount: unreadCount ?? this.unreadCount,
      type: type ?? this.type,
      isMuted: isMuted ?? this.isMuted,
    );
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}

class ChatService extends ChangeNotifier {
  static const int _initialHistoryCount = 50;
  static const int _olderHistoryCount = 30;
  static const String _legacyConversationsCacheKey = 'chat_conversations';

  static final ChatService _instance = ChatService._internal();
  factory ChatService() => _instance;

  ChatService._internal() {
    _imService.onMessageReceived = _handleIncomingMessage;
    _imService.onConversationsChanged = _mergeTencentConversations;
    _imService.onStatusChanged = _handleStatusChange;
    _imService.onError = _handleError;
  }

  final TencentImService _imService = TencentImService();
  final StorageService _storageService = StorageService();

  List<ChatConversation> _conversations = [];
  final Map<String, List<ChatMessage>> _chatMessages = {};
  final Set<String> _loadingMoreHistoryConversations = {};
  final Set<String> _historyExhaustedConversations = {};
  ChatConnectionStatus _connectionStatus = ChatConnectionStatus.disconnected;
  String? _errorMessage;
  Future<void>? _startFuture;
  String? _localCacheScope;
  int _generation = 0;

  List<ChatConversation> get conversations => List.unmodifiable(_conversations);
  ChatConnectionStatus get connectionStatus => _connectionStatus;
  String? get errorMessage => _errorMessage;
  String? get currentImUserId => _imService.currentUserId;

  ChatConversation? getConversation(String conversationId) {
    return _findConversation(conversationId);
  }

  List<ChatMessage> getMessagesForConversation(String conversationId) {
    return List.unmodifiable(_chatMessages[conversationId] ?? []);
  }

  bool isLoadingMoreHistory(String conversationId) {
    return _loadingMoreHistoryConversations.contains(conversationId);
  }

  bool hasMoreHistory(String conversationId) {
    return !_historyExhaustedConversations.contains(conversationId);
  }

  Future<void> startChatService() async {
    if (!AppConfig.enableIm) {
      _connectionStatus = ChatConnectionStatus.disconnected;
      _errorMessage = null;
      notifyListeners();
      return;
    }

    if (_connectionStatus == ChatConnectionStatus.connected) {
      return;
    }
    if (_startFuture != null) {
      return _startFuture!;
    }

    final generation = _generation;
    _startFuture = _startChatService(generation);
    try {
      await _startFuture;
    } finally {
      _startFuture = null;
    }
  }

  Future<void> _startChatService(int generation) async {
    await _loadLocalConversations();
    if (generation != _generation) return;
    await _imService.connect();
  }

  void stopChatService() {
    _imService.disconnect();
    _errorMessage = null;
    notifyListeners();
  }

  Future<void> resetForAccountChange() async {
    _generation++;
    _startFuture = null;
    await _imService.disconnect();
    _clearInMemoryChatState();
    notifyListeners();
  }

  Future<void> openConversation({
    required String conversationId,
    required String name,
    String avatar = '',
    String? type,
  }) async {
    final trimmedConversationId = conversationId.trim();
    if (trimmedConversationId.isEmpty) {
      throw Exception('Conversation id is empty');
    }

    final displayName = name.trim().isNotEmpty
        ? name.trim()
        : trimmedConversationId;
    final index = _conversations.indexWhere(
      (conv) => conv.id == trimmedConversationId,
    );
    if (index >= 0) {
      final current = _conversations[index];
      _conversations[index] = current.copyWith(
        name: displayName,
        avatar: avatar.isNotEmpty ? avatar : current.avatar,
        type: type ?? current.type,
      );
    } else {
      _conversations.add(
        ChatConversation(
          id: trimmedConversationId,
          name: displayName,
          avatar: avatar,
          lastMessage: '',
          lastMessageTime: DateTime.now(),
          type: type ?? 'single',
          isMuted: false,
        ),
      );
    }

    _sortConversations();
    await _saveConversationsToLocal();
    notifyListeners();
  }

  Future<void> sendMessage(String conversationId, String content) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final message = await _imService.sendTextMessage(
        conversationId: conversationId,
        content: content,
        type: type,
      );

      await _storeOutgoingMessage(conversationId, message, type: type);
    } catch (e) {
      developer.log('Send Tencent IM message failed: $e', name: 'ChatService');
      _errorMessage = 'Send message failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> sendCustomMessage({
    required String conversationId,
    required String data,
    String description = '',
    String extension = 'json',
    String fallbackContent = '[通知]',
    Map<String, dynamic> fallbackMetadata = const {},
  }) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final message = await _imService.sendCustomMessage(
        conversationId: conversationId,
        data: data,
        description: description,
        extension: extension,
        fallbackContent: fallbackContent,
        fallbackMetadata: fallbackMetadata,
        type: type,
      );

      await _storeOutgoingMessage(conversationId, message, type: type);
    } catch (e) {
      developer.log('Send Tencent IM custom failed: $e', name: 'ChatService');
      _errorMessage = 'Send custom failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> sendImageMessage({
    required String conversationId,
    required String imagePath,
    String imageName = '',
  }) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final message = await _imService.sendImageMessage(
        conversationId: conversationId,
        imagePath: imagePath,
        imageName: imageName,
        type: type,
      );
      await _storeOutgoingMessage(conversationId, message, type: type);
    } catch (e) {
      developer.log('Send Tencent IM image failed: $e', name: 'ChatService');
      _errorMessage = 'Send image failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> sendFileMessage({
    required String conversationId,
    required String filePath,
    required String fileName,
  }) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final message = await _imService.sendFileMessage(
        conversationId: conversationId,
        filePath: filePath,
        fileName: fileName,
        type: type,
      );
      await _storeOutgoingMessage(conversationId, message, type: type);
    } catch (e) {
      developer.log('Send Tencent IM file failed: $e', name: 'ChatService');
      _errorMessage = 'Send file failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> sendSoundMessage({
    required String conversationId,
    required String soundPath,
    required int duration,
  }) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final message = await _imService.sendSoundMessage(
        conversationId: conversationId,
        soundPath: soundPath,
        duration: duration,
        type: type,
      );
      await _storeOutgoingMessage(conversationId, message, type: type);
    } catch (e) {
      developer.log('Send Tencent IM sound failed: $e', name: 'ChatService');
      _errorMessage = 'Send sound failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> sendFaceMessage({
    required String conversationId,
    required int index,
    required String data,
  }) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final message = await _imService.sendFaceMessage(
        conversationId: conversationId,
        index: index,
        data: data,
        type: type,
      );
      await _storeOutgoingMessage(conversationId, message, type: type);
    } catch (e) {
      developer.log('Send Tencent IM face failed: $e', name: 'ChatService');
      _errorMessage = 'Send face failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<ChatGroupInfo?> loadGroupInfo(String conversationId) async {
    final conversation = _findConversation(conversationId);
    if (conversation != null && conversation.type != 'group') {
      return null;
    }

    try {
      await _ensureConnected();
      final groupInfo = await _imService.loadGroupInfo(conversationId);
      if (groupInfo != null) {
        final index = _conversations.indexWhere(
          (conv) => conv.id == conversationId,
        );
        if (index >= 0) {
          _conversations[index] = _conversations[index].copyWith(
            name: groupInfo.name.isNotEmpty ? groupInfo.name : null,
            avatar: groupInfo.avatar.isNotEmpty ? groupInfo.avatar : null,
            isMuted: groupInfo.isMuted,
          );
          await _saveConversationsToLocal();
          notifyListeners();
        }
      }
      return groupInfo;
    } catch (e) {
      developer.log(
        'Load Tencent IM group info failed: $e',
        name: 'ChatService',
      );
      _errorMessage = 'Load group info failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> setConversationMute(
    String conversationId, {
    required bool isMuted,
  }) async {
    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      await _imService.setConversationMute(
        conversationId: conversationId,
        type: type,
        isMuted: isMuted,
      );

      final index = _conversations.indexWhere(
        (conv) => conv.id == conversationId,
      );
      if (index >= 0) {
        _conversations[index] = _conversations[index].copyWith(
          isMuted: isMuted,
        );
      }
      await _saveConversationsToLocal();
      notifyListeners();
    } catch (e) {
      developer.log(
        'Set Tencent IM conversation mute failed: $e',
        name: 'ChatService',
      );
      _errorMessage = 'Set mute failed: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> loadHistoryMessages(String conversationId) async {
    try {
      _historyExhaustedConversations.remove(conversationId);
      await _loadLocalMessages(conversationId);
      if (!AppConfig.enableIm) {
        _historyExhaustedConversations.add(conversationId);
        return;
      }

      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final messages = await _imService.loadHistoryMessages(
        conversationId: conversationId,
        type: type,
        count: _initialHistoryCount,
      );
      developer.log(
        'Tencent IM history loaded: conversationId=$conversationId type=$type count=${messages.length}',
        name: 'ChatService',
      );
      if (messages.isEmpty) {
        _historyExhaustedConversations.add(conversationId);
        notifyListeners();
        return;
      }

      _addMessagesToConversation(conversationId, messages);
      if (messages.length < _initialHistoryCount) {
        _historyExhaustedConversations.add(conversationId);
      }
      await _saveMessageToLocal(conversationId, messages.last);
      notifyListeners();
    } catch (e) {
      developer.log('Load Tencent IM history failed: $e', name: 'ChatService');
      _errorMessage = 'Load history failed: $e';
      notifyListeners();
    }
  }

  Future<int> loadMoreHistoryMessages(String conversationId) async {
    if (!AppConfig.enableIm ||
        _loadingMoreHistoryConversations.contains(conversationId) ||
        _historyExhaustedConversations.contains(conversationId)) {
      return 0;
    }

    final beforeMessageId = _oldestMessageId(conversationId);
    if (beforeMessageId == null || beforeMessageId.isEmpty) {
      _historyExhaustedConversations.add(conversationId);
      notifyListeners();
      return 0;
    }

    _loadingMoreHistoryConversations.add(conversationId);
    notifyListeners();

    try {
      await _ensureConnected();
      final conversation = _findConversation(conversationId);
      final type = conversation?.type ?? 'single';
      final messages = await _imService.loadHistoryMessages(
        conversationId: conversationId,
        type: type,
        count: _olderHistoryCount,
        lastMessageId: beforeMessageId,
      );
      developer.log(
        'Tencent IM older history loaded: conversationId=$conversationId type=$type before=$beforeMessageId count=${messages.length}',
        name: 'ChatService',
      );

      final addedCount = _addMessagesToConversation(conversationId, messages);
      if (messages.isEmpty ||
          messages.length < _olderHistoryCount ||
          addedCount == 0) {
        _historyExhaustedConversations.add(conversationId);
      }
      if (addedCount > 0) {
        await _saveMessageToLocal(conversationId, messages.last);
      }
      notifyListeners();
      return addedCount;
    } catch (e) {
      developer.log(
        'Load older Tencent IM history failed: $e',
        name: 'ChatService',
      );
      _errorMessage = 'Load older history failed: $e';
      notifyListeners();
      rethrow;
    } finally {
      _loadingMoreHistoryConversations.remove(conversationId);
      notifyListeners();
    }
  }

  Future<String> getFileMessageOnlineUrl(String messageId) async {
    await _ensureConnected();
    return _imService.getFileMessageOnlineUrl(messageId);
  }

  Future<String> getSoundMessageOnlineUrl(String messageId) async {
    await _ensureConnected();
    return _imService.getSoundMessageOnlineUrl(messageId);
  }

  Future<void> _ensureConnected() async {
    if (!AppConfig.enableIm) return;
    if (_connectionStatus == ChatConnectionStatus.connected) return;

    await startChatService();
    if (_connectionStatus != ChatConnectionStatus.connected) {
      throw Exception(_errorMessage ?? 'Tencent IM is not connected');
    }
  }

  Future<void> _storeOutgoingMessage(
    String conversationId,
    ChatMessage message, {
    required String type,
  }) async {
    _addMessageToConversation(conversationId, message);
    _updateConversationLastMessage(
      conversationId,
      message.previewText,
      message.timestamp,
      type: type,
    );
    await _saveMessageToLocal(conversationId, message);
    notifyListeners();
  }

  Future<void> reconnect() async {
    if (!AppConfig.enableIm) return;
    await _imService.reconnect();
  }

  Future<void> reconnectWithFreshToken() async {
    await reconnect();
  }

  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  void markConversationAsRead(String conversationId) {
    final index = _conversations.indexWhere(
      (conv) => conv.id == conversationId,
    );
    if (index != -1) {
      _conversations[index] = _conversations[index].copyWith(unreadCount: 0);
      _saveConversationsToLocal();
      notifyListeners();
    }
  }

  void _handleIncomingMessage(ChatMessage message) {
    final isGroupMessage = message.metadata['conversationType'] == 'group';
    final conversationId = isGroupMessage || message.isMe
        ? message.receiverId
        : message.senderId;
    if (conversationId.isEmpty) return;
    final conversationType = isGroupMessage ? 'group' : 'single';

    _addMessageToConversation(conversationId, message);
    _updateConversationLastMessage(
      conversationId,
      message.previewText,
      message.timestamp,
      type: conversationType,
    );
    _saveMessageToLocal(conversationId, message);
    final conversation = _findConversation(conversationId);
    if (conversation?.isMuted != true) {
      unawaited(
        NotificationService().showImMessage(
          conversationId: conversationId,
          conversationName: conversation?.name ?? conversationId,
          conversationType: conversationType,
          senderName:
              message.metadata['senderName']?.toString() ?? message.senderId,
          content: message.previewText,
          isSelf: message.isMe,
        ),
      );
    }
    notifyListeners();
  }

  void _mergeTencentConversations(List<TencentConversationSnapshot> snapshots) {
    for (final snapshot in snapshots) {
      final index = _conversations.indexWhere((conv) => conv.id == snapshot.id);
      final current = index >= 0 ? _conversations[index] : null;
      final name = _resolveConversationName(
        currentName: current?.name,
        snapshotName: snapshot.name,
        conversationId: snapshot.id,
      );
      final conversation = ChatConversation(
        id: snapshot.id,
        name: name,
        avatar: snapshot.avatar.isNotEmpty
            ? snapshot.avatar
            : current?.avatar ?? '',
        lastMessage: snapshot.lastMessage,
        lastMessageTime: snapshot.lastMessageTime,
        unreadCount: snapshot.unreadCount,
        type: snapshot.type,
        isMuted: snapshot.isMuted,
      );
      if (index >= 0) {
        _conversations[index] = conversation;
      } else {
        _conversations.add(conversation);
      }
    }
    _sortConversations();
    _saveConversationsToLocal();
    notifyListeners();
  }

  String _resolveConversationName({
    required String? currentName,
    required String snapshotName,
    required String conversationId,
  }) {
    if (_isHumanReadableConversationName(currentName, conversationId)) {
      return currentName!.trim();
    }
    if (_isHumanReadableConversationName(snapshotName, conversationId)) {
      return snapshotName.trim();
    }
    return conversationId;
  }

  bool _isHumanReadableConversationName(String? name, String conversationId) {
    final text = name?.trim();
    if (text == null || text.isEmpty) return false;
    final lowerText = text.toLowerCase();
    final lowerConversationId = conversationId.toLowerCase();
    if (lowerText == lowerConversationId ||
        lowerText == 'c2c$lowerConversationId' ||
        lowerText == 'c2c_$lowerConversationId' ||
        lowerText == 'group$lowerConversationId' ||
        lowerText == 'group_$lowerConversationId') {
      return false;
    }
    if (lowerText.startsWith('ordersys prod export')) return false;
    if (lowerText.startsWith('employee_')) return false;
    return true;
  }

  void _handleStatusChange(ChatConnectionStatus status) {
    _connectionStatus = status;
    if (status == ChatConnectionStatus.connected) {
      _errorMessage = null;
    }
    notifyListeners();
  }

  void _handleError(String error) {
    _errorMessage = error;
    notifyListeners();
  }

  void _addMessageToConversation(String conversationId, ChatMessage message) {
    _chatMessages.putIfAbsent(conversationId, () => []);
    if (_chatMessages[conversationId]!.any((item) => item.id == message.id)) {
      return;
    }
    _chatMessages[conversationId]!.add(message);
    _chatMessages[conversationId]!.sort(
      (a, b) => a.timestamp.compareTo(b.timestamp),
    );
  }

  int _addMessagesToConversation(
    String conversationId,
    Iterable<ChatMessage> messages,
  ) {
    var addedCount = 0;
    for (final message in messages) {
      final beforeCount = _chatMessages[conversationId]?.length ?? 0;
      _addMessageToConversation(conversationId, message);
      final afterCount = _chatMessages[conversationId]?.length ?? 0;
      if (afterCount > beforeCount) {
        addedCount++;
      }
    }
    return addedCount;
  }

  String? _oldestMessageId(String conversationId) {
    final messages = _chatMessages[conversationId] ?? const <ChatMessage>[];
    for (final message in messages) {
      final messageId = message.id.trim();
      if (messageId.isNotEmpty) return messageId;
    }
    return null;
  }

  void _updateConversationLastMessage(
    String conversationId,
    String lastMessage,
    DateTime timestamp, {
    String type = 'single',
  }) {
    final index = _conversations.indexWhere(
      (conv) => conv.id == conversationId,
    );
    if (index != -1) {
      _conversations[index] = _conversations[index].copyWith(
        lastMessage: lastMessage,
        lastMessageTime: timestamp,
        type: type,
      );
    } else {
      _conversations.add(
        ChatConversation(
          id: conversationId,
          name: conversationId,
          avatar: '',
          lastMessage: lastMessage,
          lastMessageTime: timestamp,
          type: type,
          isMuted: false,
        ),
      );
    }
    _sortConversations();
    _saveConversationsToLocal();
  }

  Future<void> _loadLocalConversations() async {
    try {
      final cacheScope = await _resolveLocalCacheScope();
      _localCacheScope = cacheScope;
      if (cacheScope == null) {
        _conversations = [];
        notifyListeners();
        return;
      }

      final List<dynamic>? conversationsList = _storageService
          .getData<List<dynamic>>(_conversationsCacheKey(cacheScope));
      if (conversationsList != null && conversationsList.isNotEmpty) {
        _conversations = conversationsList
            .map(
              (item) =>
                  ChatConversation.fromJson(Map<String, dynamic>.from(item)),
            )
            .toList();
        _sortConversations();
      } else {
        _conversations = [];
      }
      notifyListeners();
    } catch (e) {
      developer.log('Load local conversations failed: $e', name: 'ChatService');
      _conversations = [];
      notifyListeners();
    }
  }

  Future<void> _loadLocalMessages(String conversationId) async {
    try {
      if (conversationId.isEmpty) return;
      final cacheScope = await _ensureLocalCacheScope();
      if (cacheScope == null) return;

      final List<dynamic>? messagesList = _storageService
          .getData<List<dynamic>>(
            _messagesCacheKey(cacheScope, conversationId),
          );
      if (messagesList == null || messagesList.isEmpty) {
        return;
      }

      var changed = false;
      for (final item in messagesList) {
        final message = ChatMessage.fromJson(Map<String, dynamic>.from(item));
        final beforeCount = _chatMessages[conversationId]?.length ?? 0;
        _addMessageToConversation(conversationId, message);
        final afterCount = _chatMessages[conversationId]?.length ?? 0;
        if (afterCount > beforeCount) {
          changed = true;
        }
      }
      if (changed) {
        notifyListeners();
      }
    } catch (e) {
      developer.log('Load local messages failed: $e', name: 'ChatService');
    }
  }

  Future<void> _saveMessageToLocal(
    String conversationId,
    ChatMessage message,
  ) async {
    try {
      final cacheScope = await _ensureLocalCacheScope();
      if (cacheScope == null) return;

      final key = _messagesCacheKey(cacheScope, conversationId);
      final messagesJson = (_chatMessages[conversationId] ?? [])
          .map((item) => item.toJson())
          .toList();
      await _storageService.saveData(key, messagesJson);
    } catch (e) {
      developer.log('Save local message failed: $e', name: 'ChatService');
    }
  }

  Future<void> _saveConversationsToLocal() async {
    try {
      final cacheScope = await _ensureLocalCacheScope();
      if (cacheScope == null) return;

      final conversationsJson = _conversations
          .map((conv) => conv.toJson())
          .toList();
      await _storageService.saveData(
        _conversationsCacheKey(cacheScope),
        conversationsJson,
      );
    } catch (e) {
      developer.log('Save local conversations failed: $e', name: 'ChatService');
    }
  }

  Future<String?> _ensureLocalCacheScope() async {
    final currentScope = _localCacheScope;
    if (currentScope != null) return currentScope;

    _localCacheScope = await _resolveLocalCacheScope();
    return _localCacheScope;
  }

  Future<String?> _resolveLocalCacheScope() async {
    final cert = await _storageService.getLoginCertificateAsync();
    final userId =
        _stringValue(cert?['userID']) ??
        _stringValue(cert?['userId']) ??
        _stringValue(cert?['id']);
    if (userId == null) return null;

    final tenantId = _stringValue(cert?['tenantId']) ?? 'default';
    return '${tenantId}_$userId';
  }

  String _conversationsCacheKey(String cacheScope) {
    return '${_legacyConversationsCacheKey}_$cacheScope';
  }

  String _messagesCacheKey(String cacheScope, String conversationId) {
    return 'chat_messages_${cacheScope}_$conversationId';
  }

  void _clearInMemoryChatState() {
    _conversations = [];
    _chatMessages.clear();
    _loadingMoreHistoryConversations.clear();
    _historyExhaustedConversations.clear();
    _connectionStatus = ChatConnectionStatus.disconnected;
    _errorMessage = null;
    _localCacheScope = null;
  }

  String? _stringValue(dynamic value) {
    final string = value?.toString().trim();
    if (string == null || string.isEmpty || string == 'null') return null;
    return string;
  }

  ChatConversation? _findConversation(String conversationId) {
    for (final conversation in _conversations) {
      if (conversation.id == conversationId) {
        return conversation;
      }
    }
    return null;
  }

  void _sortConversations() {
    _conversations.sort(
      (a, b) => b.lastMessageTime.compareTo(a.lastMessageTime),
    );
  }

  @override
  void dispose() {
    _imService.disconnect();
    super.dispose();
  }
}
