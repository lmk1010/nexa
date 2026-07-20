enum ChatConnectionStatus {
  disconnected,
  connecting,
  connected,
  reconnecting,
  error,
}

class ChatMessage {
  final String id;
  final String content;
  final String senderId;
  final String receiverId;
  final DateTime timestamp;
  final String messageType;
  final Map<String, dynamic> metadata;
  final bool isMe;

  ChatMessage({
    required this.id,
    required this.content,
    required this.senderId,
    required this.receiverId,
    required this.timestamp,
    this.messageType = 'text',
    this.metadata = const {},
    this.isMe = false,
  });

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      id: json['id']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      senderId: json['senderId']?.toString() ?? '',
      receiverId: json['receiverId']?.toString() ?? '',
      timestamp: DateTime.fromMillisecondsSinceEpoch(
        _parseInt(json['timestamp']) ?? 0,
      ),
      messageType: json['messageType']?.toString() ?? 'text',
      metadata: _parseMap(json['metadata']),
      isMe: json['isMe'] == true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'content': content,
      'senderId': senderId,
      'receiverId': receiverId,
      'timestamp': timestamp.millisecondsSinceEpoch,
      'messageType': messageType,
      'metadata': metadata,
      'isMe': isMe,
    };
  }

  String get previewText {
    final trimmedContent = content.trim();
    if (trimmedContent.isNotEmpty) {
      return trimmedContent;
    }

    switch (messageType) {
      case 'image':
        return '[图片]';
      case 'video':
        return '[视频]';
      case 'file':
        return '[文件]';
      case 'sound':
        return '[语音]';
      case 'location':
        return '[位置]';
      case 'face':
        return '[表情]';
      case 'merger':
        return '[聊天记录]';
      case 'html':
      case 'custom':
        return '[自定义消息]';
      default:
        return '[非文本消息]';
    }
  }

  static Map<String, dynamic> _parseMap(dynamic value) {
    if (value == null) return const {};
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    return const {};
  }

  static int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}

class ChatGroupInfo {
  final String id;
  final String name;
  final String type;
  final String avatar;
  final String owner;
  final String notification;
  final String introduction;
  final int memberCount;
  final int onlineCount;
  final bool isAllMuted;
  final bool isMuted;
  final List<ChatGroupMember> members;

  const ChatGroupInfo({
    required this.id,
    required this.name,
    required this.type,
    required this.avatar,
    required this.owner,
    required this.notification,
    required this.introduction,
    required this.memberCount,
    required this.onlineCount,
    required this.isAllMuted,
    required this.isMuted,
    required this.members,
  });
}

class ChatGroupMember {
  final String userId;
  final String name;
  final String avatar;
  final int role;
  final bool isOnline;

  const ChatGroupMember({
    required this.userId,
    required this.name,
    required this.avatar,
    required this.role,
    required this.isOnline,
  });

  String get displayName {
    final trimmedName = name.trim();
    return trimmedName.isNotEmpty ? trimmedName : userId;
  }
}
