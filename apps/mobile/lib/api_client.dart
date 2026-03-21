import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  ApiClient({required this.baseUrlProvider, required this.tokenProvider});

  final String Function() baseUrlProvider;
  final String Function() tokenProvider;

  Future<Map<String, dynamic>> register({
    required String displayName,
    required String email,
    required String password,
  }) async {
    return _post('/api/auth/register', body: {
      'displayName': displayName,
      'email': email,
      'password': password,
    }, authenticated: false);
  }

  Future<Map<String, dynamic>> login({
    required String email,
    required String password,
  }) async {
    return _post('/api/auth/login', body: {
      'email': email,
      'password': password,
    }, authenticated: false);
  }

  Future<Map<String, dynamic>> getProfile() async {
    return _get('/api/profile');
  }

  Future<Map<String, dynamic>> mintSpgId() async {
    return _post('/api/profile/spg-id/mint', body: {});
  }

  Future<Map<String, dynamic>> getWallet() async {
    return _get('/api/wallet');
  }

  Future<Map<String, dynamic>> claimAirdrop() async {
    return _post('/api/wallet/airdrop/claim', body: {});
  }

  Future<Map<String, dynamic>> getPremiumPlans() async {
    return _get('/api/premium/plans');
  }

  Future<Map<String, dynamic>> getPremiumStatus() async {
    return _get('/api/premium/status');
  }

  Future<Map<String, dynamic>> activatePlan({
    required String planId,
    required String platform,
    String? receipt,
  }) async {
    return _post('/api/premium/subscribe', body: {
      'planId': planId,
      'platform': platform,
      'receipt': receipt ?? '',
    });
  }

  Future<Map<String, dynamic>> assistantChat({
    required List<Map<String, String>> messages,
    required String intent,
  }) async {
    return _post('/api/assistant/chat', body: {
      'messages': messages,
      'intent': intent,
    });
  }

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

  Future<Map<String, dynamic>> _get(String path) async {
    final token = tokenProvider();
    final response = await http.get(
      Uri.parse('${baseUrlProvider()}$path'),
      headers: {
        'Content-Type': 'application/json',
        if (token.isNotEmpty) 'Authorization': 'Bearer $token',
      },
    );
    return _parseResponse(response);
  }

  Future<Map<String, dynamic>> _post(
    String path, {
    required Map<String, dynamic> body,
    bool authenticated = true,
  }) async {
    final token = tokenProvider();
    final response = await http.post(
      Uri.parse('${baseUrlProvider()}$path'),
      headers: {
        'Content-Type': 'application/json',
        if (authenticated && token.isNotEmpty) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );
    return _parseResponse(response);
  }

  Map<String, dynamic> _parseResponse(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception('Request failed: ${response.body}');
    }
    if (response.body.isEmpty) {
      return {};
    }
    return jsonDecode(response.body) as Map<String, dynamic>;
  }
}
