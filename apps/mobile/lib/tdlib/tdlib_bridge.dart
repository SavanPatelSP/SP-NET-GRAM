import 'dart:async';
import 'package:flutter/services.dart';
import 'tdlib_models.dart';

class TdlibBridge {
  TdlibBridge();

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

  Future<void> sendMessage(int chatId, String text) async {
    await _channel.invokeMethod('sendMessage', {'chatId': chatId, 'text': text});
  }

  void dispose() {
    _authController.close();
    _chatController.close();
    _messageController.close();
  }
}
