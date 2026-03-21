# SP NET GRAM — Telegram Integration Plan

## Mobile (Android, then iOS)
- Use TDLib (official) for Telegram protocol.
- Bridge TDLib to Flutter via platform channels or a native plugin.
- Steps:
  1) Add TDLib binaries to Android project.
  2) Implement a Dart interface that wraps TDLib calls (auth, chats, messages).
  3) Map TDLib events into app state.
  4) Store session locally on-device.

## Web
- Use MTProto client library (e.g., GramJS) in a React web app.
- Handle phone auth + session storage in browser.
- Map chats/messages to UI state.
 - Current scaffold: `apps/web/telegram/mtproto_client.js`
 - Install dependencies in `apps/web` and run via Vite.

## Important Guardrails
- Do not proxy Telegram traffic through SP NET GRAM backend.
- Keep Telegram sessions on-device/in-browser.
- SP NET GRAM backend is only for premium, wallet, assistant, SPG ID.
