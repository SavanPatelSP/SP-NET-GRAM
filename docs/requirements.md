# SP NET GRAM — Product Requirements

## 1) Core Client Parity (Telegram-like)
- Phone number auth + OTP
- Chats list (pins, folders, unread counts)
- 1:1 chats, groups, channels
- Message types: text, photo, video, voice, files, stickers, GIFs
- Replies, forwards, edits, deletes, reactions
- Search (messages, chats, contacts)
- Media gallery per chat
- Notifications and mute controls
- Settings (privacy, language, chat settings)

## 2) SP NET GRAM Identity Layer
- Unique SP NET GRAM ID (SPG ID)
  - Example format: `SPG-XXXX-XXXX`
- Profile card: handle, name, avatar, SPG ID, badges
- Public profile link/share

## 3) Assistant Feature (Nicegram-style, SP NET GRAM branded)
- Dedicated Assistant page/tab
- Tools:
  - Summarize chat or message thread
  - Smart replies (3–5 suggestions)
  - Translate + rephrase
  - Tone rewrite (formal, friendly, concise)
  - Ask assistant about messages (“What was agreed?”, “Next steps?”)
- System prompt tailored to SP NET GRAM

## 4) Premium Plan
- Premium plan page
- Tier comparison (Free / Plus / Pro)
- Billing and subscription flow (Android/iOS in-app purchases)
- Premium badge + perks gating (assistant limits, higher file size, extra folders)

## 5) Wallet: SP Coin + Gems
- SP Coin balance
- Gems balance
- Airdrop page (claimable schedule, history)
- Transaction history (earn, spend, transfer)

## 6) “Nicegram-like” UX Enhancements
- Chat folders + quick filters
- Quick message templates
- Pinned assistant shortcuts
- Message speed actions (swipe actions)
- Multi-account support (optional phase)

## 7) Admin / Ops (Later)
- Admin dashboard for airdrops, promos
- Premium plan management
- Feature flags

## Non-Goals (For Now)
- Full Telegram server implementation
- Non-telegram custom messenger network

## Milestones
- M1: Design system + core UI skeleton
- M2: Telegram auth + chats list integration
- M3: Assistant + SPG ID + wallet
- M4: Premium, airdrop, and ops tools
- M5: iOS parity
