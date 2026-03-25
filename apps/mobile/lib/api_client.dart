import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  ApiClient({required this.baseUrlProvider, required this.tokenProvider});

  final String Function() baseUrlProvider;
  final String Function() tokenProvider;

  Future<void> postPremiumReceipt({
    required String planId,
    required String platform,
    required String receipt,
  }) async {
    final token = tokenProvider();
    if (token.isEmpty) {
      throw Exception('Missing auth token.');
    }
    final response = await http.post(
      Uri.parse('${baseUrlProvider()}/api/premium/subscribe'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'planId': planId,
        'platform': platform,
        'receipt': receipt,
      }),
    );

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception('Receipt upload failed: ${response.body}');
    }
  }
}
