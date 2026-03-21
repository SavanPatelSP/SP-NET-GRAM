# SP NET GRAM — In-App Purchases (Android + iOS)

## Objective
Enable paid Premium plans (Plus/Pro) using platform in-app purchases while keeping the backend as the source of truth for active entitlements.

## Mobile Flow (Android first)
1. User taps "Upgrade" on Premium page.
2. App calls platform purchase flow (Google Play Billing).
3. On success, app posts receipt/token to backend:
   - `POST /api/premium/subscribe` with `{ planId, platform, receipt }`
4. Backend stores the receipt and sets active plan + expiration.
5. App fetches `GET /api/premium/status` to confirm entitlements.

## Android Setup (Google Play)
- Create in-app products:
  - `spnetgram_plus_android`
  - `spnetgram_pro_android`
- Set price tiers and activate products.
- Configure test accounts in Play Console.
- Use the Play Billing Library in the app.

## iOS Setup (App Store)
- Create in-app products:
  - `spnetgram_plus_ios`
  - `spnetgram_pro_ios`
- Configure price tiers and App Store Connect test users.
- Use StoreKit in the app.

## Backend Verification (Later)
Backend now supports optional receipt verification:
- Google Play: calls `purchases.subscriptionsv2.get` using a service account.
- Apple: calls `verifyReceipt` with sandbox fallback.
- Enable via `IAP_VERIFY=1` and required env vars.

Handle renewals, cancellations, and grace periods once we add webhook processing.
Schedule a background job to downgrade expired subscriptions.

## Status
- Current backend uses a stubbed subscription model (30-day duration).
- Client should treat premium as optimistic until server confirms.
