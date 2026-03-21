# SP NET GRAM — API Contract (Draft)

Base URL: `/api`

## Auth
- `POST /auth/register`
  - body: `{ email, password, displayName }`
- `POST /auth/login`
  - body: `{ email, password }`
- `GET /auth/me`

## Profile + SPG ID
- `GET /profile`
- `POST /profile`
  - body: `{ displayName, handle, avatarUrl }`
- `POST /profile/spg-id/mint`
  - returns: `{ spgId }`

## Wallet
- `GET /wallet`
  - returns: `{ spCoin, gems, history[] }`
- `GET /wallet/airdrop/status`
  - returns: `{ lastClaimAt, nextClaimAt, canClaim, cooldownHours }`
- `POST /wallet/airdrop/claim`
  - returns: `{ spCoin, claimed }`

## Premium
- `GET /premium/plans`
- `GET /premium/status`
- `POST /premium/subscribe`
  - body: `{ planId, platform, receipt }`

## Assistant
- `POST /assistant/chat`
  - body: `{ messages[], intent }`
  - returns: `{ reply, suggestions[] }`

## Feature Flags
- `GET /flags`

## Admin (later)
- `POST /admin/airdrop`
- `POST /admin/grants`
