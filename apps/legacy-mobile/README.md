# SP NET GRAM Mobile (Flutter)

This is a UI scaffold for the Android-first mobile app. Once Flutter is installed, we can wire builds and add Telegram integration.

Planned integrations:
- TDLib via native bridge
- SP NET GRAM backend for wallet, premium, assistant
- In-app purchases via platform billing (wired with `in_app_purchase`)

IAP wiring details:
- `lib/iap_service.dart` uses `in_app_purchase` to start purchases.
- `lib/api_client.dart` posts receipts to `/api/premium/subscribe`.
- Update `AppConfig.sessionToken` in Settings to test receipt uploads.
