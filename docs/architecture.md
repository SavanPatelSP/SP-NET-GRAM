# SP NET GRAM — Architecture

## Overview
We build a Telegram-compatible client with an added SP NET GRAM feature layer. Telegram connectivity is handled by official protocols/libraries. SP NET GRAM features are handled by our backend.

## Clients
### Mobile (Android first, iOS later)
- Flutter UI
- Telegram connectivity via TDLib (native library bridged to Flutter)
- SP NET GRAM feature API via HTTPS

### Web
- React UI (or Flutter Web if we later add a web-ready MTProto stack)
- Telegram connectivity via MTProto JS (GramJS or equivalent)
- SP NET GRAM feature API via HTTPS

## Backend (SP NET GRAM Feature Layer)
- Handles SPG ID, wallet, premium plans (IAP receipts), assistant, airdrops
- REST API + future websocket for realtime
- Stores a link between Telegram user and SPG profile

## Identity Mapping
- Telegram user ID is stored alongside SPG user record
- SPG ID is minted once and stored
- Session tokens used only for SPG feature API, not Telegram

## Assistant
- Assistant service API endpoint
- Pluggable provider (OpenAI, local LLM, etc.)
- Stores minimal context + user controls (privacy toggles)

## Data Stores
- Dev: SQLite
- Prod: Postgres
- Object storage for avatars and media (optional)

## Security Notes
- Do not proxy Telegram traffic through SP NET GRAM backend (avoid MITM)
- Keep Telegram auth on-device for mobile and in-browser for web
- Store only SP NET GRAM data in backend

## Telemetry (Optional)
- Minimal logging for errors + performance
- Privacy-first defaults
