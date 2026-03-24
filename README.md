# SP-NET-GRAM

A Telegram-compatible client inspired by Nicegram, with SP NET GRAM identity, assistant features, premium plans, SP Coin airdrops, and gems.

## Goals
- Full chat client experience for Android + Web first, then iOS.
- Preserve Telegram protocol compatibility while adding SP NET GRAM features.
- Unique SP NET GRAM identity layer (SPG ID) tied to a user profile.
- Built-in assistant surface for summaries, smart replies, and tools.
- Premium plan management (with in-app purchases), SP Coin, gems, and airdrop flows.

## Repo Structure
- `apps/telegram-android` — Official Telegram Android client source (GPL-3.0 base for SP NET GRAM)
- `apps/telegram-web-k` — Official Telegram Web K client source (GPL-3.0 base for SP NET GRAM)
- `apps/legacy-mobile` — Legacy Flutter prototype (archived)
- `apps/legacy-web` — Legacy web prototype (archived)
- `services/spnet-gram-backend` — SP NET GRAM feature backend (accounts, wallet, premium, assistant)
- `docs` — product requirements, architecture, API contracts
  - `docs/iap.md` — in-app purchase plan

## Status
- Official Telegram Android and Web K codebases are now the primary client foundations.
- All SP NET GRAM work will be done as GPL-compliant forks of these codebases.

## GPL Compliance
- See `GPL-COMPLIANCE.md` for corresponding source and build details.
- See `NOTICE.md` for upstream notices and attributions.
- License text is in `LICENSE`.

## Next Steps
- Rebrand UI/strings/assets to SP NET GRAM (no Telegram name/logo).
- Add SP NET GRAM features: assistant, SPG ID, premium, SP Coin, gems, airdrops.
- Wire backend integration into both official clients.
  - Android: add SP NET GRAM settings + assistant + wallet panels.
  - Web K: add SP NET GRAM sections and connect to backend.

## Local Development (later)
This repo is a scaffold. Mobile and web runtime dependencies are not installed here.
- Flutter SDK (for mobile)
- Node.js (for web)

Once those are installed, we can wire build scripts and run the apps.

## Deployment (suggested)
- Backend: Render (see `render.yaml`)
- Web: Vercel (set env vars from `apps/web/.env.example`)
