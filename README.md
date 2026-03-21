# SP-NET-GRAM

A Telegram-compatible client inspired by Nicegram, with SP NET GRAM identity, assistant features, premium plans, SP Coin airdrops, and gems.

## Goals
- Full chat client experience for Android + Web first, then iOS.
- Preserve Telegram protocol compatibility while adding SP NET GRAM features.
- Unique SP NET GRAM identity layer (SPG ID) tied to a user profile.
- Built-in assistant surface for summaries, smart replies, and tools.
- Premium plan management (with in-app purchases), SP Coin, gems, and airdrop flows.

## Repo Structure
- `apps/web` — Web client (prototype + future production app)
- `apps/mobile` — Flutter mobile client (Android first, iOS next)
- `services/spnet-gram-backend` — SP NET GRAM feature backend (accounts, wallet, premium, assistant)
- `docs` — product requirements, architecture, API contracts
  - `docs/iap.md` — in-app purchase plan

## Status
- Initial product/architecture docs and UI skeletons are in progress.
- Telegram integration (TDLib / MTProto) is planned but not wired yet.

## Next Steps
- Finalize feature list and UX scope for “full clone”.
- Choose exact Telegram integration strategy (TDLib on mobile, MTProto on web).
- Implement backend data models for SPG ID, wallet, premium, assistant.
- Build chat UI parity and connect to Telegram APIs.

## Local Development (later)
This repo is a scaffold. Mobile and web runtime dependencies are not installed here.
- Flutter SDK (for mobile)
- Node.js (for web)

Once those are installed, we can wire build scripts and run the apps.
