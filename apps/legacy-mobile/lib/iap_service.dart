import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:in_app_purchase/in_app_purchase.dart';
import 'api_client.dart';
import 'models.dart';

class IapService {
  IapService({required this.apiClient});

  final ApiClient apiClient;
  final InAppPurchase _iap = InAppPurchase.instance;
  StreamSubscription<List<PurchaseDetails>>? _subscription;
  bool _initialized = false;

  final Map<String, String> _productToPlan = {
    'spnetgram_plus_android': 'plus',
    'spnetgram_pro_android': 'pro',
    'spnetgram_plus_ios': 'plus',
    'spnetgram_pro_ios': 'pro',
  };

  Future<void> init() async {
    if (_initialized) return;
    _subscription = _iap.purchaseStream.listen(_handlePurchaseUpdates, onError: (_) {});
    _initialized = true;
  }

  Future<List<ProductDetails>> loadProducts(Set<String> productIds) async {
    final response = await _iap.queryProductDetails(productIds);
    if (response.error != null) {
      throw Exception('IAP error: ${response.error}');
    }
    return response.productDetails;
  }

  Future<void> startPurchase(BuildContext context, PremiumPlan plan) async {
    try {
      await init();
      final productId = plan.productId;
      if (productId == null) {
        throw Exception('No product ID for this plan.');
      }
      final products = await loadProducts({productId});
      final product = products.firstWhere((item) => item.id == productId);
      final purchaseParam = PurchaseParam(productDetails: product);
      await _iap.buyNonConsumable(purchaseParam: purchaseParam);
      _showSnack(context, 'Purchase started for ${plan.name}.');
    } catch (error) {
      _showSnack(context, error.toString());
    }
  }

  Future<void> _handlePurchaseUpdates(List<PurchaseDetails> purchases) async {
    for (final purchase in purchases) {
      if (purchase.status == PurchaseStatus.purchased || purchase.status == PurchaseStatus.restored) {
        await _postReceipt(purchase);
        await _iap.completePurchase(purchase);
      }
      if (purchase.status == PurchaseStatus.error) {
        // Surface errors in logs; UI handles user feedback on next tap.
      }
    }
  }

  Future<void> _postReceipt(PurchaseDetails purchase) async {
    final planId = _productToPlan[purchase.productID] ?? 'plus';
    final receipt = purchase.verificationData.serverVerificationData;
    final platform = Platform.isIOS ? 'ios' : 'android';
    await apiClient.postPremiumReceipt(
      planId: planId,
      platform: platform,
      receipt: receipt,
    );
  }

  void dispose() {
    _subscription?.cancel();
  }

  void _showSnack(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}
