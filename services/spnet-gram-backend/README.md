# SP NET GRAM Backend (Prototype)

Local backend for SPG ID, wallet, premium, and assistant stubs.

## Run
```bash
cd /Users/savanpatel/Documents/sp-net-gram/services/spnet-gram-backend
pip install -r requirements.txt
python3 server.py
```

Server runs at `http://localhost:8790`.

## Endpoints
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/profile`
- `POST /api/profile/spg-id/mint`
- `GET /api/wallet`
- `GET /api/wallet/airdrop/status`
- `POST /api/wallet/airdrop/claim`
- `GET /api/premium/plans`
- `GET /api/premium/status`
- `POST /api/assistant/chat`
- `POST /api/premium/subscribe`

## IAP Verification (optional)
Set environment variables to enable receipt verification:
- `IAP_VERIFY=1`
- `GOOGLE_PACKAGE_NAME`
- `GOOGLE_SERVICE_ACCOUNT_JSON` (service account JSON string)
- `APPLE_SHARED_SECRET`

Apple verification uses the legacy `verifyReceipt` endpoint with sandbox fallback when needed.
