import 'dart:async';
import 'package:flutter/services.dart';
import 'tdlib_models.dart';

class TdlibBridge {
  TdlibBridge() {
    _channel.setMethodCallHandler(_handleNativeCall);
  }

  static const MethodChannel _channel = MethodChannel('spnetgram/tdlib');

  final StreamController<TdlibAuthState> _authController = StreamController.broadcast();
  final StreamController<List<TdlibChatSummary>> _chatController = StreamController.broadcast();
  final StreamController<TdlibMessage> _messageController = StreamController.broadcast();

  Stream<TdlibAuthState> get authState => _authController.stream;
  Stream<List<TdlibChatSummary>> get chats => _chatController.stream;
  Stream<TdlibMessage> get messages => _messageController.stream;

  Future<void> initialize({required String apiId, required String apiHash}) async {
    await _channel.invokeMethod('initialize', {'apiId': int.parse(apiId), 'apiHash': apiHash});
  }

  Future<void> sendPhoneNumber(String phone) async {
    await _channel.invokeMethod('sendPhoneNumber', {'phone': phone});
  }

  Future<void> sendOtp(String code) async {
    await _channel.invokeMethod('sendOtp', {'code': code});
  }

  Future<void> sendPassword(String password) async {
    await _channel.invokeMethod('sendPassword', {'password': password});
  }

  Future<void> sendMessage(int chatId, String text) async {
    await _channel.invokeMethod('sendMessage', {'chatId': chatId, 'text': text});
  }

  Future<void> fetchChats() async {
    await _channel.invokeMethod('fetchChats');
  }

  Future<void> fetchMessages(int chatId) async {
    await _channel.invokeMethod('fetchMessages', {'chatId': chatId});
  }

  Future<void> _handleNativeCall(MethodCall call) async {
    final args = call.arguments;
    switch (call.method) {
      case 'onAuthState':
        if (args is Map) {
          _authController.add(
            TdlibAuthState(
              status: (args['status'] ?? 'unknown').toString(),
              message: args['message']?.toString(),
            ),
          );
        }
        break;
      case 'onChats':
        if (args is List) {
          final list = args.map((item) {
            final map = Map<String, dynamic>.from(item as Map);
            return TdlibChatSummary(
              id: (map['id'] as num).toInt(),
              title: map['title']?.toString() ?? 'Unknown',
              lastMessage: map['lastMessage']?.toString() ?? '',
              unreadCount: (map['unreadCount'] as num?)?.toInt() ?? 0,
            );
          }).toList();
          _chatController.add(list);
        }
        break;
      case 'onMessage':
        if (args is Map) {
          final map = Map<String, dynamic>.from(args);
          _messageController.add(
            TdlibMessage(
              id: (map['id'] as num).toInt(),
              chatId: (map['chatId'] as num).toInt(),
              sender: map['sender']?.toString() ?? 'Unknown',
              text: map['text']?.toString() ?? '',
              timestamp: DateTime.fromMillisecondsSinceEpoch(
                ((map['timestamp'] as num?)?.toInt() ?? 0) * 1000,
                isUtc: true,
              ),
              isOutgoing: map['isOutgoing'] == true,
            ),
          );
        }
        break;
      default:
        break;
    }
  }

  void dispose() {
    _authController.close();
    _chatController.close();
    _messageController.close();
  }
}
