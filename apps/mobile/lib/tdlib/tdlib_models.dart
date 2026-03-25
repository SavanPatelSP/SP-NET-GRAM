class TdlibAuthState {
  const TdlibAuthState({required this.status, this.message});

  final String status;
  final String? message;
}

class TdlibChatSummary {
  const TdlibChatSummary({
    required this.id,
    required this.title,
    required this.lastMessage,
    required this.unreadCount,
  });

  final int id;
  final String title;
  final String lastMessage;
  final int unreadCount;
}

class TdlibMessage {
  const TdlibMessage({
    required this.id,
    required this.chatId,
    required this.sender,
    required this.text,
    required this.timestamp,
  });

  final int id;
  final int chatId;
  final String sender;
  final String text;
  final DateTime timestamp;
}
