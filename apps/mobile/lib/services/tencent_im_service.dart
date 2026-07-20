import 'dart:convert';
import 'dart:developer' as developer;

import 'package:tencent_cloud_chat_sdk/enum/V2TimAdvancedMsgListener.dart';
import 'package:tencent_cloud_chat_sdk/enum/V2TimConversationListener.dart';
import 'package:tencent_cloud_chat_sdk/enum/V2TimSDKListener.dart';
import 'package:tencent_cloud_chat_sdk/enum/conversation_type.dart';
import 'package:tencent_cloud_chat_sdk/enum/group_member_filter_enum.dart';
import 'package:tencent_cloud_chat_sdk/enum/image_types.dart';
import 'package:tencent_cloud_chat_sdk/enum/log_level_enum.dart';
import 'package:tencent_cloud_chat_sdk/enum/message_elem_type.dart';
import 'package:tencent_cloud_chat_sdk/enum/receive_message_opt_enum.dart';
import 'package:tencent_cloud_chat_sdk/models/v2_tim_conversation.dart';
import 'package:tencent_cloud_chat_sdk/models/v2_tim_message.dart';
import 'package:tencent_cloud_chat_sdk/tencent_im_sdk_plugin.dart';

import 'api_service.dart';
import 'chat_models.dart';

class TencentImService {
  static final TencentImService _instance = TencentImService._internal();
  factory TencentImService() => _instance;
  TencentImService._internal();

  bool _initialized = false;
  TencentImLoginTicket? _ticket;
  ChatConnectionStatus _status = ChatConnectionStatus.disconnected;

  V2TimAdvancedMsgListener? _messageListener;
  V2TimConversationListener? _conversationListener;

  Function(ChatMessage)? onMessageReceived;
  Function(List<TencentConversationSnapshot>)? onConversationsChanged;
  Function(ChatConnectionStatus)? onStatusChanged;
  Function(String)? onError;

  String? get currentUserId => _ticket?.userID;
  ChatConnectionStatus get status => _status;

  Future<void> connect() async {
    try {
      _updateStatus(ChatConnectionStatus.connecting);

      final ticket = await ApiService.getTencentImLoginTicket();
      _ticket = ticket;

      if (!_initialized) {
        final initResult = await TencentImSDKPlugin.v2TIMManager.initSDK(
          sdkAppID: ticket.sdkAppId,
          loglevel: LogLevelEnum.V2TIM_LOG_INFO,
          listener: V2TimSDKListener(
            onConnecting: () {
              _updateStatus(ChatConnectionStatus.connecting);
            },
            onConnectSuccess: () {
              _updateStatus(ChatConnectionStatus.connected);
            },
            onConnectFailed: (code, error) {
              _updateStatus(ChatConnectionStatus.error);
              onError?.call('Tencent IM connect failed: $code $error');
            },
            onKickedOffline: () {
              _updateStatus(ChatConnectionStatus.disconnected);
              onError?.call('Tencent IM kicked offline');
            },
            onUserSigExpired: () {
              _updateStatus(ChatConnectionStatus.error);
              onError?.call('Tencent IM UserSig expired');
            },
          ),
        );
        if (initResult.code != 0) {
          throw Exception(
            'Tencent IM init failed: ${initResult.code} ${initResult.desc}',
          );
        }
        _initialized = true;
        _bindListeners();
      }

      final loginResult = await TencentImSDKPlugin.v2TIMManager.login(
        userID: ticket.userID,
        userSig: ticket.userSig,
      );
      if (loginResult.code != 0) {
        throw Exception(
          'Tencent IM login failed: ${loginResult.code} ${loginResult.desc}',
        );
      }

      _updateStatus(ChatConnectionStatus.connected);
      await refreshConversationList();
    } catch (e) {
      developer.log('Tencent IM connect failed: $e', name: 'TencentImService');
      _updateStatus(ChatConnectionStatus.error);
      onError?.call(e.toString());
    }
  }

  Future<void> reconnect() async {
    _updateStatus(ChatConnectionStatus.reconnecting);
    await connect();
  }

  Future<void> disconnect() async {
    try {
      await TencentImSDKPlugin.v2TIMManager.logout();
    } catch (e) {
      developer.log('Tencent IM logout failed: $e', name: 'TencentImService');
    }
    _ticket = null;
    _updateStatus(ChatConnectionStatus.disconnected);
  }

  Future<void> refreshConversationList() async {
    try {
      final result = await TencentImSDKPlugin.v2TIMManager
          .getConversationManager()
          .getConversationList(nextSeq: '0', count: 100);
      if (result.code != 0) {
        throw Exception(
          'Load conversation list failed: ${result.code} ${result.desc}',
        );
      }
      final conversations = <TencentConversationSnapshot>[];
      final data = result.data;
      final rawList = data?.conversationList ?? [];
      for (final item in rawList) {
        final snapshot = _conversationFromTencent(item);
        if (snapshot != null) {
          conversations.add(snapshot);
        }
      }
      onConversationsChanged?.call(conversations);
    } catch (e) {
      developer.log(
        'Load Tencent IM conversations failed: $e',
        name: 'TencentImService',
      );
      onError?.call(e.toString());
    }
  }

  Future<ChatMessage> sendTextMessage({
    required String conversationId,
    required String content,
    required String type,
  }) async {
    final createResult = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .createTextMessage(text: content);
    if (createResult.code != 0 || createResult.data?.id == null) {
      throw Exception(
        'Tencent IM create message failed: ${createResult.code} ${createResult.desc}',
      );
    }

    final result = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .sendMessage(
          // ignore: deprecated_member_use
          id: createResult.data!.id!,
          receiver: type == 'group' ? '' : conversationId,
          groupID: type == 'group' ? conversationId : '',
        );

    if (result.code != 0) {
      throw Exception('Tencent IM send failed: ${result.code} ${result.desc}');
    }

    return _messageFromTencent(
          result.data,
          fallbackConversationId: conversationId,
        ) ??
        ChatMessage(
          id: DateTime.now().millisecondsSinceEpoch.toString(),
          content: content,
          senderId: currentUserId ?? '',
          receiverId: conversationId,
          timestamp: DateTime.now(),
          isMe: true,
        );
  }

  Future<ChatMessage> sendCustomMessage({
    required String conversationId,
    required String data,
    required String type,
    String description = '',
    String extension = 'json',
    String fallbackContent = '[通知]',
    Map<String, dynamic> fallbackMetadata = const {},
  }) async {
    final createResult = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .createCustomMessage(
          data: data,
          desc: description,
          extension: extension,
        );

    return _sendCreatedMessage(
      createResult: createResult,
      conversationId: conversationId,
      type: type,
      fallbackContent: fallbackContent,
      fallbackMessageType: 'custom',
      fallbackMetadata: {
        ...fallbackMetadata,
        'extension': extension,
        'rawData': data,
        'description': description,
      },
    );
  }

  Future<ChatMessage> sendImageMessage({
    required String conversationId,
    required String imagePath,
    required String imageName,
    required String type,
  }) async {
    final createResult = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .createImageMessage(
          imagePath: imagePath,
          imageName: imageName.isNotEmpty ? imageName : null,
        );

    return _sendCreatedMessage(
      createResult: createResult,
      conversationId: conversationId,
      type: type,
      fallbackContent: '[图片]',
      fallbackMessageType: 'image',
      fallbackMetadata: {
        'mediaUrl': imagePath,
        'thumbnailUrl': imagePath,
        if (imageName.isNotEmpty) 'fileName': imageName,
      },
    );
  }

  Future<ChatMessage> sendFileMessage({
    required String conversationId,
    required String filePath,
    required String fileName,
    required String type,
  }) async {
    final createResult = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .createFileMessage(filePath: filePath, fileName: fileName);

    return _sendCreatedMessage(
      createResult: createResult,
      conversationId: conversationId,
      type: type,
      fallbackContent: fileName.isNotEmpty ? '[文件] $fileName' : '[文件]',
      fallbackMessageType: 'file',
      fallbackMetadata: {'fileName': fileName, 'mediaUrl': filePath},
    );
  }

  Future<ChatMessage> sendSoundMessage({
    required String conversationId,
    required String soundPath,
    required int duration,
    required String type,
  }) async {
    final safeDuration = duration <= 0 ? 1 : duration;
    final createResult = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .createSoundMessage(soundPath: soundPath, duration: safeDuration);

    return _sendCreatedMessage(
      createResult: createResult,
      conversationId: conversationId,
      type: type,
      fallbackContent: '[语音] ${safeDuration}s',
      fallbackMessageType: 'sound',
      fallbackMetadata: {'duration': safeDuration, 'mediaUrl': soundPath},
    );
  }

  Future<ChatMessage> sendFaceMessage({
    required String conversationId,
    required int index,
    required String data,
    required String type,
  }) async {
    final createResult = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .createFaceMessage(index: index, data: data);

    return _sendCreatedMessage(
      createResult: createResult,
      conversationId: conversationId,
      type: type,
      fallbackContent: data.isNotEmpty ? data : '[表情]',
      fallbackMessageType: 'face',
      fallbackMetadata: {'index': index, 'data': data},
    );
  }

  Future<ChatMessage> _sendCreatedMessage({
    required dynamic createResult,
    required String conversationId,
    required String type,
    required String fallbackContent,
    required String fallbackMessageType,
    Map<String, dynamic> fallbackMetadata = const {},
  }) async {
    final createData = createResult.data;
    final messageId = createData?.id?.toString();
    if (createResult.code != 0 ||
        ((messageId == null || messageId.isEmpty) &&
            createData?.messageInfo == null)) {
      throw Exception(
        'Tencent IM create message failed: ${createResult.code} ${createResult.desc}',
      );
    }

    final result = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .sendMessage(
          // ignore: deprecated_member_use
          id: messageId,
          message: createData?.messageInfo,
          receiver: type == 'group' ? '' : conversationId,
          groupID: type == 'group' ? conversationId : '',
        );

    if (result.code != 0) {
      throw Exception('Tencent IM send failed: ${result.code} ${result.desc}');
    }

    return _messageFromTencent(
          result.data,
          fallbackConversationId: conversationId,
        ) ??
        ChatMessage(
          id: messageId ?? DateTime.now().millisecondsSinceEpoch.toString(),
          content: fallbackContent,
          senderId: currentUserId ?? '',
          receiverId: conversationId,
          timestamp: DateTime.now(),
          messageType: fallbackMessageType,
          metadata: {
            ...fallbackMetadata,
            'conversationType': type,
            'senderName': currentUserId ?? '',
            'senderAvatar': '',
          },
          isMe: true,
        );
  }

  Future<List<ChatMessage>> loadHistoryMessages({
    required String conversationId,
    required String type,
    int count = 50,
    String? lastMessageId,
  }) async {
    developer.log(
      'Tencent IM history request: currentUser=$currentUserId peer=$conversationId type=$type count=$count lastMessageId=${lastMessageId ?? ''}',
      name: 'TencentImService',
    );

    final result = type == 'group'
        ? await TencentImSDKPlugin.v2TIMManager
              .getMessageManager()
              .getGroupHistoryMessageList(
                groupID: conversationId,
                count: count,
                lastMsgID: lastMessageId,
              )
        : await TencentImSDKPlugin.v2TIMManager
              .getMessageManager()
              .getC2CHistoryMessageList(
                userID: conversationId,
                count: count,
                lastMsgID: lastMessageId,
              );

    developer.log(
      'Tencent IM history response: peer=$conversationId code=${result.code} desc=${result.desc} rawCount=${result.data?.length ?? 0}',
      name: 'TencentImService',
    );

    if (result.code != 0) {
      throw Exception(
        'Tencent IM history failed: ${result.code} ${result.desc}',
      );
    }

    final messages = <ChatMessage>[];
    for (final item in result.data ?? []) {
      final message = _messageFromTencent(
        item,
        fallbackConversationId: conversationId,
      );
      if (message != null) {
        messages.add(message);
      }
    }
    messages.sort((a, b) => a.timestamp.compareTo(b.timestamp));
    developer.log(
      'Tencent IM history mapped: peer=$conversationId mappedCount=${messages.length}',
      name: 'TencentImService',
    );
    return messages;
  }

  Future<String> getFileMessageOnlineUrl(String messageId) async {
    final normalizedMessageId = messageId.trim();
    if (normalizedMessageId.isEmpty) {
      throw Exception('Message id is empty');
    }

    final result = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .getMessageOnlineUrl(msgID: normalizedMessageId);
    if (result.code != 0) {
      throw Exception(
        'Tencent IM file url failed: ${result.code} ${result.desc}',
      );
    }

    final fileUrl = result.data?.fileElem?.url?.trim() ?? '';
    final localUrl = result.data?.fileElem?.localUrl?.trim() ?? '';
    final path = result.data?.fileElem?.path?.trim() ?? '';
    return _firstNonEmpty([fileUrl, localUrl, path]);
  }

  Future<String> getSoundMessageOnlineUrl(String messageId) async {
    final normalizedMessageId = messageId.trim();
    if (normalizedMessageId.isEmpty) {
      throw Exception('Message id is empty');
    }

    final result = await TencentImSDKPlugin.v2TIMManager
        .getMessageManager()
        .getMessageOnlineUrl(msgID: normalizedMessageId);
    if (result.code != 0) {
      throw Exception(
        'Tencent IM sound url failed: ${result.code} ${result.desc}',
      );
    }

    final soundUrl = result.data?.soundElem?.url?.trim() ?? '';
    final localUrl = result.data?.soundElem?.localUrl?.trim() ?? '';
    final path = result.data?.soundElem?.path?.trim() ?? '';
    return _firstNonEmpty([soundUrl, localUrl, path]);
  }

  Future<ChatGroupInfo?> loadGroupInfo(
    String groupId, {
    int memberLimit = 30,
  }) async {
    final groupResult = await TencentImSDKPlugin.v2TIMManager
        .getGroupManager()
        .getGroupsInfo(groupIDList: [groupId]);
    if (groupResult.code != 0) {
      throw Exception(
        'Tencent IM group info failed: ${groupResult.code} ${groupResult.desc}',
      );
    }

    final infoResult = groupResult.data?.isNotEmpty == true
        ? groupResult.data!.first
        : null;
    if (infoResult == null || infoResult.resultCode != 0) {
      throw Exception(
        'Tencent IM group info failed: ${infoResult?.resultCode ?? -1} ${infoResult?.resultMessage ?? 'empty result'}',
      );
    }

    final groupInfo = infoResult.groupInfo;
    if (groupInfo == null || groupInfo.groupID.isEmpty) {
      return null;
    }

    final members = <ChatGroupMember>[];
    try {
      final memberResult = await TencentImSDKPlugin.v2TIMManager
          .getGroupManager()
          .getGroupMemberList(
            groupID: groupId,
            filter: GroupMemberFilterTypeEnum.V2TIM_GROUP_MEMBER_FILTER_ALL,
            nextSeq: '0',
            count: memberLimit,
          );
      if (memberResult.code == 0) {
        for (final member in memberResult.data?.memberInfoList ?? []) {
          members.add(
            ChatGroupMember(
              userId: member.userID,
              name: _firstNonEmpty([
                member.nameCard,
                member.friendRemark,
                member.nickName,
                member.userID,
              ]),
              avatar: member.faceUrl ?? '',
              role: member.role ?? 0,
              isOnline: member.isOnline == true,
            ),
          );
        }
      } else {
        developer.log(
          'Tencent IM group members failed: ${memberResult.code} ${memberResult.desc}',
          name: 'TencentImService',
        );
      }
    } catch (e) {
      developer.log(
        'Tencent IM group members failed: $e',
        name: 'TencentImService',
      );
    }

    final muted = _isMutedReceiveOpt(groupInfo.recvOpt);
    return ChatGroupInfo(
      id: groupInfo.groupID,
      name: groupInfo.groupName ?? groupInfo.groupID,
      type: groupInfo.groupType,
      avatar: groupInfo.faceUrl ?? '',
      owner: groupInfo.owner ?? '',
      notification: groupInfo.notification ?? '',
      introduction: groupInfo.introduction ?? '',
      memberCount: groupInfo.memberCount ?? members.length,
      onlineCount: groupInfo.onlineCount ?? 0,
      isAllMuted: groupInfo.isAllMuted == true,
      isMuted: muted,
      members: members,
    );
  }

  Future<void> setConversationMute({
    required String conversationId,
    required String type,
    required bool isMuted,
  }) async {
    final opt = isMuted
        ? ReceiveMsgOptEnum.V2TIM_RECEIVE_NOT_NOTIFY_MESSAGE
        : ReceiveMsgOptEnum.V2TIM_RECEIVE_MESSAGE;
    final result = type == 'group'
        ? await TencentImSDKPlugin.v2TIMManager
              .getMessageManager()
              .setGroupReceiveMessageOpt(groupID: conversationId, opt: opt)
        : await TencentImSDKPlugin.v2TIMManager
              .getMessageManager()
              .setC2CReceiveMessageOpt(userIDList: [conversationId], opt: opt);

    if (result.code != 0) {
      throw Exception(
        'Tencent IM set receive opt failed: ${result.code} ${result.desc}',
      );
    }
  }

  void _bindListeners() {
    _messageListener = V2TimAdvancedMsgListener(
      onRecvNewMessage: (newMsg) {
        final message = _messageFromTencent(newMsg);
        if (message != null) {
          onMessageReceived?.call(message);
        }
      },
    );
    TencentImSDKPlugin.v2TIMManager.getMessageManager().addAdvancedMsgListener(
      listener: _messageListener!,
    );

    _conversationListener = V2TimConversationListener(
      onNewConversation: (conversationList) {
        _emitConversationSnapshots(conversationList);
      },
      onConversationChanged: (conversationList) {
        _emitConversationSnapshots(conversationList);
      },
    );
    TencentImSDKPlugin.v2TIMManager
        .getConversationManager()
        .addConversationListener(listener: _conversationListener!);
  }

  void _emitConversationSnapshots(List<dynamic> conversationList) {
    final snapshots = <TencentConversationSnapshot>[];
    for (final item in conversationList) {
      final snapshot = _conversationFromTencent(item);
      if (snapshot != null) {
        snapshots.add(snapshot);
      }
    }
    if (snapshots.isNotEmpty) {
      onConversationsChanged?.call(snapshots);
    }
  }

  TencentConversationSnapshot? _conversationFromTencent(dynamic conversation) {
    if (conversation is V2TimConversation) {
      final isGroup =
          conversation.type == ConversationType.V2TIM_GROUP ||
          (conversation.groupID?.isNotEmpty == true);
      final id = isGroup
          ? (conversation.groupID ?? '')
          : (conversation.userID?.isNotEmpty == true
                ? conversation.userID!
                : _stripConversationPrefix(conversation.conversationID));
      if (id.isEmpty) return null;

      return TencentConversationSnapshot(
        id: id,
        name: _firstNonEmpty([conversation.showName, id]),
        avatar: conversation.faceUrl ?? '',
        lastMessage: _parseMessageContentFromV2TimMessage(
          conversation.lastMessage,
        ).content,
        lastMessageTime: _timeFromValue(
          conversation.lastMessage?.timestamp ??
              conversation.draftTimestamp ??
              conversation.orderkey,
        ),
        unreadCount: conversation.unreadCount ?? 0,
        type: isGroup ? 'group' : 'single',
        isMuted: _isMutedReceiveOpt(conversation.recvOpt),
      );
    }

    final json = _toJsonMap(conversation);
    if (json == null) return null;

    final conversationID =
        json['conversationID']?.toString() ?? json['conv_id']?.toString() ?? '';
    final conversationType = _parseInt(json['type'] ?? json['conv_type']);
    final convId = json['conv_id']?.toString();
    final groupID =
        json['groupID']?.toString() ??
        (conversationType == ConversationType.V2TIM_GROUP ? convId : null);
    final userID =
        json['userID']?.toString() ??
        (conversationType == ConversationType.V2TIM_C2C ? convId : null);
    final isGroup = groupID != null && groupID.isNotEmpty;
    final id = isGroup
        ? groupID
        : (userID?.isNotEmpty == true
              ? userID!
              : _stripConversationPrefix(conversationID));
    if (id.isEmpty) return null;

    final lastMessageJson = _asMap(json['lastMessage']);
    final nativeLastMessageJson = _asMap(json['conv_last_msg']);
    return TencentConversationSnapshot(
      id: id,
      name: _firstNonEmpty([json['showName'], json['conv_show_name'], id]),
      avatar:
          json['faceUrl']?.toString() ??
          json['conv_face_url']?.toString() ??
          '',
      lastMessage: _parseMessageContentFromJson(
        lastMessageJson ?? nativeLastMessageJson,
      ).content,
      lastMessageTime: _timeFromValue(
        lastMessageJson?['timestamp'] ??
            nativeLastMessageJson?['timestamp'] ??
            nativeLastMessageJson?['message_server_time'] ??
            nativeLastMessageJson?['message_client_time'] ??
            json['draftTimestamp'] ??
            json['conv_active_time'],
      ),
      unreadCount:
          _parseInt(json['unreadCount'] ?? json['conv_unread_num']) ?? 0,
      type: isGroup ? 'group' : 'single',
      isMuted: _isMutedReceiveOpt(json['recvOpt'] ?? json['conv_recv_opt']),
    );
  }

  ChatMessage? _messageFromTencent(
    dynamic message, {
    String? fallbackConversationId,
  }) {
    if (message is V2TimMessage) {
      final senderId = message.sender?.trim() ?? '';
      final groupID = message.groupID?.trim();
      final userID = message.userID?.trim();
      final isGroupConversation = groupID?.isNotEmpty == true;
      final receiverId = groupID?.isNotEmpty == true
          ? groupID!
          : (userID?.isNotEmpty == true
                ? userID!
                : (fallbackConversationId ?? ''));
      final parsedContent = _parseMessageContentFromV2TimMessage(message);

      return ChatMessage(
        id:
            message.msgID?.toString() ??
            message.id?.toString() ??
            DateTime.now().millisecondsSinceEpoch.toString(),
        content: parsedContent.content,
        senderId: senderId,
        receiverId: receiverId,
        timestamp: _timeFromValue(message.timestamp),
        messageType: parsedContent.type,
        metadata: {
          ...parsedContent.metadata,
          'conversationType': isGroupConversation ? 'group' : 'single',
          'senderName': _firstNonEmpty([
            message.nameCard,
            message.friendRemark,
            message.nickName,
            senderId,
          ]),
          'senderAvatar': message.faceUrl ?? '',
        },
        isMe: message.isSelf == true || senderId == currentUserId,
      );
    }

    final json = _toJsonMap(message);
    if (json == null) return null;

    final senderId =
        json['sender']?.toString() ?? json['message_sender']?.toString() ?? '';
    final conversationType = _parseInt(json['message_conv_type']);
    final conversationId = json['message_conv_id']?.toString();
    final groupID =
        json['groupID']?.toString() ??
        (conversationType == ConversationType.V2TIM_GROUP
            ? conversationId
            : null);
    final isGroupConversation = groupID?.isNotEmpty == true;
    final userID =
        json['userID']?.toString() ??
        (conversationType == ConversationType.V2TIM_C2C
            ? conversationId
            : null);
    final receiverId = groupID?.isNotEmpty == true
        ? groupID!
        : (userID?.isNotEmpty == true
              ? userID!
              : (fallbackConversationId ?? ''));
    final parsedContent = _parseMessageContentFromJson(json);
    final senderProfile = _asMap(json['message_sender_profile']);
    final senderGroupMemberInfo = _asMap(
      json['message_sender_group_member_info'],
    );

    return ChatMessage(
      id:
          json['msgID']?.toString() ??
          json['message_msg_id']?.toString() ??
          json['id']?.toString() ??
          DateTime.now().millisecondsSinceEpoch.toString(),
      content: parsedContent.content,
      senderId: senderId,
      receiverId: receiverId,
      timestamp: _timeFromValue(
        json['timestamp'] ??
            json['message_server_time'] ??
            json['message_client_time'],
      ),
      messageType: parsedContent.type,
      metadata: {
        ...parsedContent.metadata,
        'conversationType': isGroupConversation ? 'group' : 'single',
        'senderName': _firstNonEmpty([
          senderGroupMemberInfo?['group_member_info_name_card'],
          senderProfile?['user_profile_friend_remark'],
          senderProfile?['user_profile_nick_name'],
          senderId,
        ]),
        'senderAvatar':
            senderProfile?['user_profile_face_url']?.toString() ?? '',
      },
      isMe:
          json['isSelf'] == true ||
          json['message_is_from_self'] == true ||
          senderId == currentUserId,
    );
  }

  _ParsedMessageContent _parseMessageContentFromV2TimMessage(
    V2TimMessage? message,
  ) {
    if (message == null) return const _ParsedMessageContent.empty();

    switch (message.elemType) {
      case MessageElemType.V2TIM_ELEM_TYPE_TEXT:
        return _parseTextContent(message.textElem?.text);
      case MessageElemType.V2TIM_ELEM_TYPE_CUSTOM:
        return _parseCustomContent(
          data: message.customElem?.data,
          description: message.customElem?.desc,
          extension: message.customElem?.extension,
        );
      case MessageElemType.V2TIM_ELEM_TYPE_IMAGE:
        return _parseImageContentFromElem(message.imageElem);
      case MessageElemType.V2TIM_ELEM_TYPE_SOUND:
        return _parseSoundContentFromElem(message.soundElem);
      case MessageElemType.V2TIM_ELEM_TYPE_VIDEO:
        return _parseVideoContentFromElem(message.videoElem);
      case MessageElemType.V2TIM_ELEM_TYPE_FILE:
        return _parseFileContentFromElem(message.fileElem);
      case MessageElemType.V2TIM_ELEM_TYPE_LOCATION:
        return _parseLocationContentFromMap(_asMap(message.locationElem));
      case MessageElemType.V2TIM_ELEM_TYPE_FACE:
        return _parseFaceContentFromMap(_asMap(message.faceElem));
      case MessageElemType.V2TIM_ELEM_TYPE_MERGER:
        return _parseMergerContentFromMap(_asMap(message.mergerElem));
      case MessageElemType.V2TIM_ELEM_TYPE_GROUP_TIPS:
        return const _ParsedMessageContent('[群通知]', 'groupTips');
    }

    final localCustomData = message.localCustomData;
    if (localCustomData != null && localCustomData.isNotEmpty) {
      return _parseCustomContent(data: localCustomData);
    }
    final cloudCustomData = message.cloudCustomData;
    if (cloudCustomData != null && cloudCustomData.isNotEmpty) {
      return _parseCustomContent(data: cloudCustomData);
    }

    final parsed = _parseMessageContentFromJson(message.toJson());
    if (!parsed.isUnknown) return parsed;
    developer.log(
      'Unknown Tencent IM message: elemType=${message.elemType} raw=${_truncate(message.toJson().toString(), 1200)}',
      name: 'TencentImService',
    );
    return parsed;
  }

  _ParsedMessageContent _parseMessageContentFromJson(
    Map<String, dynamic>? json,
  ) {
    if (json == null) return const _ParsedMessageContent.empty();

    final elemArray = json['message_elem_array'];
    if (elemArray is List) {
      for (final item in elemArray) {
        final parsed = _parseMessageContentFromJson(_asMap(item));
        if (!parsed.isEmpty && !parsed.isUnknown) {
          return parsed;
        }
      }
    }

    final textElem = _asMap(json['textElem']);
    if (textElem != null) {
      final text =
          textElem['text']?.toString() ??
          textElem['text_elem_content']?.toString();
      final parsed = _parseTextContent(text);
      if (!parsed.isEmpty) return parsed;
    }

    final text = _firstNonEmpty([
      json['text'],
      json['message'],
      json['text_elem_content'],
    ]);
    if (text.isNotEmpty) {
      return _parseTextContent(text);
    }

    final customElem = _asMap(json['customElem']);
    if (customElem != null) {
      return _parseCustomContent(
        data:
            customElem['data']?.toString() ??
            customElem['custom_elem_data']?.toString(),
        description:
            customElem['desc']?.toString() ??
            customElem['custom_elem_desc']?.toString(),
        extension:
            customElem['extension']?.toString() ??
            customElem['custom_elem_ext']?.toString(),
      );
    }

    if (_hasAnyKey(json, ['custom_elem_data', 'custom_elem_desc'])) {
      return _parseCustomContent(
        data: json['custom_elem_data']?.toString(),
        description: json['custom_elem_desc']?.toString(),
        extension: json['custom_elem_ext']?.toString(),
      );
    }

    final imageElem = _asMap(json['imageElem']);
    if (imageElem != null ||
        _hasAnyKey(json, ['image_elem_orig_url', 'path'])) {
      return _parseImageContentFromMap(imageElem ?? json);
    }

    final videoElem = _asMap(json['videoElem']);
    if (videoElem != null || _hasAnyKey(json, ['video_elem_video_id'])) {
      return _parseVideoContentFromMap(videoElem ?? json);
    }

    final fileElem = _asMap(json['fileElem']);
    if (fileElem != null || _hasAnyKey(json, ['file_elem_file_name'])) {
      return _parseFileContentFromMap(fileElem ?? json);
    }

    final soundElem = _asMap(json['soundElem']);
    if (soundElem != null || _hasAnyKey(json, ['sound_elem_file_id'])) {
      return _parseSoundContentFromMap(soundElem ?? json);
    }

    final locationElem = _asMap(json['locationElem']);
    if (locationElem != null || _hasAnyKey(json, ['location_elem_desc'])) {
      return _parseLocationContentFromMap(locationElem ?? json);
    }

    final faceElem = _asMap(json['faceElem']);
    if (faceElem != null || _hasAnyKey(json, ['face_elem_buf'])) {
      return _parseFaceContentFromMap(faceElem ?? json);
    }

    final mergerElem = _asMap(json['mergerElem']);
    if (mergerElem != null || _hasAnyKey(json, ['merge_elem_title'])) {
      return _parseMergerContentFromMap(mergerElem ?? json);
    }

    developer.log(
      'Unknown Tencent IM message json: ${_truncate(json.toString(), 1200)}',
      name: 'TencentImService',
    );
    return const _ParsedMessageContent('[非文本消息]', 'unknown');
  }

  _ParsedMessageContent _parseTextContent(String? text) {
    final content = text?.trim() ?? '';
    if (content.isEmpty) return const _ParsedMessageContent.empty();
    if (_looksLikeHtml(content)) {
      return _parseHtmlContent(content);
    }
    return _ParsedMessageContent(content, 'text');
  }

  _ParsedMessageContent _parseCustomContent({
    String? data,
    String? description,
    String? extension,
  }) {
    final normalizedExtension = extension?.trim().toLowerCase() ?? '';
    final rawData = data?.trim() ?? '';
    final rawDescription = description?.trim() ?? '';
    final raw = _firstNonEmpty([rawData, rawDescription]);
    final metadata = <String, dynamic>{
      'extension': normalizedExtension,
      'rawData': rawData,
      'description': rawDescription,
    };

    if (normalizedExtension == 'html') {
      return _parseHtmlContent(raw, extraMetadata: metadata);
    }

    if (normalizedExtension == 'json' || _looksLikeJson(rawData)) {
      final parsedJson = _tryDecodeJsonMap(rawData) ?? _tryDecodeJsonMap(raw);
      if (parsedJson != null) {
        final messageHtml = _firstNonEmpty([
          parsedJson['message'],
          parsedJson['extra'],
        ]);
        final content = _firstNonEmpty([
          parsedJson['remark'],
          parsedJson['content'],
          _htmlToPlainText(messageHtml),
          parsedJson['title'],
        ]);
        return _ParsedMessageContent(
          content.isNotEmpty ? content : '[通知]',
          'custom',
          {
            ...metadata,
            'json': parsedJson,
            'title': parsedJson['title']?.toString() ?? '',
            'messageHtml': parsedJson['message']?.toString() ?? '',
            'extraHtml': parsedJson['extra']?.toString() ?? '',
            'desktop': parsedJson['desktop'] == true,
          },
        );
      }
    }

    if (normalizedExtension == 'execute') {
      final parsedJson = _tryDecodeJsonMap(rawData) ?? _tryDecodeJsonMap(raw);
      final content = _firstNonEmpty([
        parsedJson?['type'],
        parsedJson?['title'],
        parsedJson?['remark'],
        parsedJson?['content'],
        raw,
      ]);
      return _ParsedMessageContent(
        content.isNotEmpty ? content : '[执行消息]',
        'custom',
        {...metadata, if (parsedJson != null) 'json': parsedJson},
      );
    }

    if (_looksLikeHtml(raw)) {
      return _parseHtmlContent(raw, extraMetadata: metadata);
    }

    return _ParsedMessageContent(
      raw.isNotEmpty ? raw : '[自定义消息]',
      'custom',
      metadata,
    );
  }

  _ParsedMessageContent _parseHtmlContent(
    String html, {
    Map<String, dynamic> extraMetadata = const {},
  }) {
    final plainText = _htmlToPlainText(html);
    return _ParsedMessageContent(
      plainText.isNotEmpty ? plainText : '[HTML消息]',
      'html',
      {...extraMetadata, 'html': html},
    );
  }

  _ParsedMessageContent _parseImageContentFromElem(dynamic imageElem) {
    return _parseImageContentFromMap(_asMap(imageElem));
  }

  _ParsedMessageContent _parseImageContentFromMap(Map<String, dynamic>? json) {
    final imageList = json?['imageList'];
    Map<String, dynamic>? originImage;
    Map<String, dynamic>? thumbImage;
    Map<String, dynamic>? largeImage;
    if (imageList is List) {
      for (final item in imageList) {
        final image = _asMap(item);
        if (image == null) continue;
        final type = _parseInt(image['type']);
        if (type == V2TIM_IMAGE_TYPE.V2TIM_IMAGE_TYPE_ORIGIN) {
          originImage = image;
        } else if (type == V2TIM_IMAGE_TYPE.V2TIM_IMAGE_TYPE_THUMB) {
          thumbImage = image;
        } else if (type == V2TIM_IMAGE_TYPE.V2TIM_IMAGE_TYPE_LARGE) {
          largeImage = image;
        }
      }
    }

    final mediaUrl = _firstNonEmpty([
      originImage?['url'],
      largeImage?['url'],
      thumbImage?['url'],
      json?['image_elem_orig_url'],
      json?['image_elem_large_url'],
      json?['image_elem_thumb_url'],
      json?['path'],
    ]);
    final thumbnailUrl = _firstNonEmpty([
      thumbImage?['url'],
      largeImage?['url'],
      originImage?['url'],
      json?['image_elem_thumb_url'],
      json?['image_elem_large_url'],
      json?['image_elem_orig_url'],
      json?['path'],
    ]);
    final width =
        _parseInt(thumbImage?['width']) ??
        _parseInt(largeImage?['width']) ??
        _parseInt(originImage?['width']) ??
        _parseInt(json?['image_elem_thumb_pic_width']) ??
        _parseInt(json?['image_elem_large_pic_width']) ??
        _parseInt(json?['image_elem_orig_pic_width']);
    final height =
        _parseInt(thumbImage?['height']) ??
        _parseInt(largeImage?['height']) ??
        _parseInt(originImage?['height']) ??
        _parseInt(json?['image_elem_thumb_pic_height']) ??
        _parseInt(json?['image_elem_large_pic_height']) ??
        _parseInt(json?['image_elem_orig_pic_height']);

    return _ParsedMessageContent('[图片]', 'image', {
      'mediaUrl': mediaUrl,
      'thumbnailUrl': thumbnailUrl,
      if (width != null) 'width': width,
      if (height != null) 'height': height,
    });
  }

  _ParsedMessageContent _parseVideoContentFromElem(dynamic videoElem) {
    return _parseVideoContentFromMap(_asMap(videoElem));
  }

  _ParsedMessageContent _parseVideoContentFromMap(Map<String, dynamic>? json) {
    final duration = _parseInt(
      json?['duration'] ?? json?['video_elem_video_duration'],
    );
    final mediaUrl = _firstNonEmpty([
      json?['videoUrl'],
      json?['localVideoUrl'],
      json?['videoPath'],
      json?['video_elem_video_url'],
      json?['video_elem_video_path'],
    ]);
    final thumbnailUrl = _firstNonEmpty([
      json?['snapshotUrl'],
      json?['localSnapshotUrl'],
      json?['snapshotPath'],
      json?['video_elem_image_url'],
      json?['video_elem_image_path'],
    ]);
    return _ParsedMessageContent(
      duration != null && duration > 0
          ? '[视频] ${_formatDuration(duration)}'
          : '[视频]',
      'video',
      {
        'mediaUrl': mediaUrl,
        'thumbnailUrl': thumbnailUrl,
        'duration': duration ?? 0,
        'fileSize':
            _parseInt(json?['videoSize'] ?? json?['video_elem_video_size']) ??
            0,
        'videoType':
            json?['videoType']?.toString() ??
            json?['video_elem_video_type']?.toString() ??
            '',
      },
    );
  }

  _ParsedMessageContent _parseFileContentFromElem(dynamic fileElem) {
    return _parseFileContentFromMap(_asMap(fileElem));
  }

  _ParsedMessageContent _parseFileContentFromMap(Map<String, dynamic>? json) {
    final fileName = _firstNonEmpty([
      json?['fileName'],
      json?['file_elem_file_name'],
      json?['UUID'],
      json?['file_elem_file_id'],
    ]);
    final fileSize =
        _parseInt(json?['fileSize'] ?? json?['file_elem_file_size']) ?? 0;
    final url = _firstNonEmpty([json?['url'], json?['file_elem_url']]);
    final localUrl = _firstNonEmpty([json?['localUrl']]);
    final path = _firstNonEmpty([json?['path'], json?['file_elem_file_path']]);
    final fileUUID = _firstNonEmpty([
      json?['UUID'],
      json?['file_elem_file_id'],
    ]);
    final mediaUrl = _firstNonEmpty([url, localUrl, path]);
    return _ParsedMessageContent(
      fileName.isNotEmpty ? '[文件] $fileName' : '[文件]',
      'file',
      {
        'fileName': fileName,
        'fileSize': fileSize,
        'mediaUrl': mediaUrl,
        'url': url,
        'localUrl': localUrl,
        'path': path,
        'fileUUID': fileUUID,
      },
    );
  }

  _ParsedMessageContent _parseSoundContentFromElem(dynamic soundElem) {
    return _parseSoundContentFromMap(_asMap(soundElem));
  }

  _ParsedMessageContent _parseSoundContentFromMap(Map<String, dynamic>? json) {
    final duration = _parseInt(
      json?['duration'] ?? json?['sound_elem_file_time'],
    );
    final mediaUrl = _firstNonEmpty([
      json?['url'],
      json?['localUrl'],
      json?['path'],
      json?['sound_elem_url'],
      json?['sound_elem_file_path'],
    ]);
    final fileUUID = _firstNonEmpty([
      json?['UUID'],
      json?['sound_elem_file_id'],
    ]);
    return _ParsedMessageContent(
      duration != null && duration > 0 ? '[语音] ${duration}s' : '[语音]',
      'sound',
      {
        'duration': duration ?? 0,
        'fileSize':
            _parseInt(json?['dataSize'] ?? json?['sound_elem_file_size']) ?? 0,
        'mediaUrl': mediaUrl,
        'url': _firstNonEmpty([json?['url'], json?['sound_elem_url']]),
        'localUrl': _firstNonEmpty([json?['localUrl']]),
        'path': _firstNonEmpty([json?['path'], json?['sound_elem_file_path']]),
        'fileUUID': fileUUID,
      },
    );
  }

  _ParsedMessageContent _parseLocationContentFromMap(
    Map<String, dynamic>? json,
  ) {
    final desc = _firstNonEmpty([json?['desc'], json?['location_elem_desc']]);
    return _ParsedMessageContent(
      desc.isNotEmpty ? '[位置] $desc' : '[位置]',
      'location',
      {
        'description': desc,
        'longitude':
            json?['longitude'] ?? json?['location_elem_longitude'] ?? 0.0,
        'latitude': json?['latitude'] ?? json?['location_elem_latitude'] ?? 0.0,
      },
    );
  }

  _ParsedMessageContent _parseFaceContentFromMap(Map<String, dynamic>? json) {
    final data = _firstNonEmpty([json?['data'], json?['face_elem_buf']]);
    return _ParsedMessageContent(data.isNotEmpty ? data : '[表情]', 'face', {
      'index': _parseInt(json?['index'] ?? json?['face_elem_index']) ?? 0,
      'data': data,
    });
  }

  _ParsedMessageContent _parseMergerContentFromMap(Map<String, dynamic>? json) {
    final title = _firstNonEmpty([
      json?['title'],
      json?['merge_elem_title'],
      json?['compatibleText'],
      json?['merge_elem_compatible_text'],
    ]);
    final abstractList =
        json?['abstractList'] ?? json?['merge_elem_abstract_array'];
    return _ParsedMessageContent(
      title.isNotEmpty ? '[聊天记录] $title' : '[聊天记录]',
      'merger',
      {
        'title': title,
        'abstractList': abstractList is List
            ? abstractList.map((item) => item.toString()).toList()
            : const <String>[],
      },
    );
  }

  bool _hasAnyKey(Map<String, dynamic> json, List<String> keys) {
    for (final key in keys) {
      final value = json[key];
      if (value == null) continue;
      if (value is String && value.trim().isEmpty) continue;
      return true;
    }
    return false;
  }

  bool _isMutedReceiveOpt(dynamic value) {
    final recvOpt = _parseInt(value);
    if (recvOpt == null) return false;
    return recvOpt != ReceiveMsgOptEnum.V2TIM_RECEIVE_MESSAGE.index;
  }

  bool _looksLikeHtml(String value) {
    return RegExp(r'</?[a-zA-Z][^>]*>').hasMatch(value);
  }

  bool _looksLikeJson(String value) {
    final trimmed = value.trim();
    return trimmed.startsWith('{') && trimmed.endsWith('}');
  }

  Map<String, dynamic>? _tryDecodeJsonMap(String? value) {
    final trimmed = value?.trim();
    if (trimmed == null || trimmed.isEmpty) return null;
    try {
      final decoded = jsonDecode(trimmed);
      if (decoded is Map<String, dynamic>) return decoded;
      if (decoded is Map) return Map<String, dynamic>.from(decoded);
    } catch (_) {
      return null;
    }
    return null;
  }

  String _htmlToPlainText(String? value) {
    final html = value?.trim() ?? '';
    if (html.isEmpty) return '';
    final withBreaks = html
        .replaceAll(RegExp(r'<\s*br\s*/?\s*>', caseSensitive: false), '\n')
        .replaceAll(
          RegExp(r'</\s*(p|div|li|tr|h[1-6])\s*>', caseSensitive: false),
          '\n',
        )
        .replaceAll(RegExp(r'<\s*li[^>]*>', caseSensitive: false), '- ');
    final withoutTags = withBreaks.replaceAll(RegExp(r'<[^>]+>'), '');
    return _decodeHtmlEntities(withoutTags)
        .split('\n')
        .map((line) => line.trim())
        .where((line) => line.isNotEmpty)
        .join('\n')
        .trim();
  }

  String _decodeHtmlEntities(String value) {
    return value
        .replaceAll('&nbsp;', ' ')
        .replaceAll('&amp;', '&')
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll('&quot;', '"')
        .replaceAll('&#39;', "'")
        .replaceAllMapped(RegExp(r'&#(\d+);'), (match) {
          final codePoint = int.tryParse(match.group(1) ?? '');
          if (codePoint == null) return match.group(0) ?? '';
          return String.fromCharCode(codePoint);
        })
        .replaceAllMapped(RegExp(r'&#x([0-9a-fA-F]+);'), (match) {
          final codePoint = int.tryParse(match.group(1) ?? '', radix: 16);
          if (codePoint == null) return match.group(0) ?? '';
          return String.fromCharCode(codePoint);
        });
  }

  String _formatDuration(int seconds) {
    final minutes = seconds ~/ 60;
    final remainder = seconds % 60;
    if (minutes <= 0) return '${remainder}s';
    return '$minutes:${remainder.toString().padLeft(2, '0')}';
  }

  String _truncate(String value, int maxLength) {
    if (value.length <= maxLength) return value;
    return '${value.substring(0, maxLength)}...';
  }

  String _firstNonEmpty(List<dynamic> values) {
    for (final value in values) {
      final text = value?.toString().trim();
      if (text != null && text.isNotEmpty) {
        return text;
      }
    }
    return '';
  }

  Map<String, dynamic>? _toJsonMap(dynamic value) {
    if (value == null) return null;
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    try {
      final json = value.toJson();
      if (json is Map<String, dynamic>) return json;
      if (json is Map) return Map<String, dynamic>.from(json);
    } catch (_) {
      return null;
    }
    return null;
  }

  Map<String, dynamic>? _asMap(dynamic value) {
    if (value == null) return null;
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    try {
      final json = value.toJson();
      if (json is Map<String, dynamic>) return json;
      if (json is Map) return Map<String, dynamic>.from(json);
    } catch (_) {
      return null;
    }
    return null;
  }

  String _stripConversationPrefix(String conversationID) {
    if (conversationID.startsWith('C2C')) return conversationID.substring(3);
    if (conversationID.startsWith('GROUP')) return conversationID.substring(5);
    if (conversationID.startsWith('c2c_')) return conversationID.substring(4);
    if (conversationID.startsWith('group_')) {
      return conversationID.substring(6);
    }
    return conversationID;
  }

  DateTime _timeFromValue(dynamic value) {
    final parsed = _parseInt(value);
    if (parsed == null || parsed <= 0) return DateTime.now();
    final milliseconds = parsed < 9999999999 ? parsed * 1000 : parsed;
    return DateTime.fromMillisecondsSinceEpoch(milliseconds);
  }

  int? _parseInt(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  void _updateStatus(ChatConnectionStatus newStatus) {
    if (_status == newStatus) return;
    _status = newStatus;
    onStatusChanged?.call(newStatus);
  }
}

class TencentConversationSnapshot {
  final String id;
  final String name;
  final String avatar;
  final String lastMessage;
  final DateTime lastMessageTime;
  final int unreadCount;
  final String type;
  final bool isMuted;

  TencentConversationSnapshot({
    required this.id,
    required this.name,
    required this.avatar,
    required this.lastMessage,
    required this.lastMessageTime,
    required this.unreadCount,
    required this.type,
    required this.isMuted,
  });
}

class _ParsedMessageContent {
  final String content;
  final String type;
  final Map<String, dynamic> metadata;

  const _ParsedMessageContent(
    this.content,
    this.type, [
    this.metadata = const {},
  ]);

  const _ParsedMessageContent.empty()
    : content = '',
      type = 'text',
      metadata = const {};

  bool get isEmpty => content.isEmpty && type == 'text' && metadata.isEmpty;
  bool get isUnknown => type == 'unknown';
}
