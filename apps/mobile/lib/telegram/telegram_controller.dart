import 'dart:async';
import 'package:flutter/foundation.dart';
import '../app_config.dart';
import '../tdlib/tdlib_bridge.dart';
import '../tdlib/tdlib_models.dart';

class TelegramController extends ChangeNotifier {
  TelegramController({TdlibBridge? bridge}) : _bridge = bridge ?? TdlibBridge() {
    _authSub = _bridge.authState.listen(_handleAuthState);
    _chatSub = _bridge.chats.listen(_handleChats);
    _messageSub = _bridge.messages.listen(_handleMessage);
  }

  final TdlibBridge _bridge;
  late final StreamSubscription<TdlibAuthState> _authSub;
  late final StreamSubscription<List<TdlibChatSummary>> _chatSub;
  late final StreamSubscription<TdlibMessage> _messageSub;

  TdlibAuthState authState = const TdlibAuthState(status: 'init');
  List<TdlibChatSummary> chats = [];
  final Map<int, List<TdlibMessage>> messages = {};
  int? activeChatId;
  bool initialized = false;

  Future<void> initialize() async {
    if (initialized) return;
    initialized = true;
    await _bridge.initialize(apiId: AppConfig.tgApiId, apiHash: AppConfig.tgApiHash);
    notifyListeners();
  }

  Future<void> sendPhoneNumber(String phone) async {
    await _bridge.sendPhoneNumber(phone);
  }

  Future<void> sendOtp(String code) async {
    await _bridge.sendOtp(code);
  }

  Future<void> sendPassword(String password) async {
    await _bridge.sendPassword(password);
  }

  Future<void> registerUser(String firstName, String lastName) async {
    await _bridge.registerUser(firstName, lastName);
  }

  Future<void> fetchChats() async {
    await _bridge.fetchChats();
  }

  Future<void> fetchMessages(int chatId) async {
    messages[chatId] = [];
    notifyListeners();
    await _bridge.fetchMessages(chatId);
  }

  Future<void> sendMessage(int chatId, String text) async {
    await _bridge.sendMessage(chatId, text);
  }

  void setActiveChat(int chatId) {
    activeChatId = chatId;
    notifyListeners();
  }

  void dispose() {
    _authSub.cancel();
    _chatSub.cancel();
    _messageSub.cancel();
    _bridge.dispose();
    super.dispose();
  }

  void _handleAuthState(TdlibAuthState state) {
    authState = state;
    notifyListeners();
    if (state.status == 'READY') {
      fetchChats();
    }
  }

  void _handleChats(List<TdlibChatSummary> list) {
    chats = list;
    if (activeChatId == null && list.isNotEmpty) {
      activeChatId = list.first.id;
    }
    notifyListeners();
  }

  void _handleMessage(TdlibMessage message) {
    final list = messages.putIfAbsent(message.chatId, () => []);
    list.insert(0, message);
    notifyListeners();
  }
}
