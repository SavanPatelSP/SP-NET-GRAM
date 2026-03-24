# SP NET GRAM Backend (Prototype)

Local backend for SPG ID, wallet, premium, assistant stubs, and paid license keys.

## Run
```bash
cd /Users/savanpatel/Documents/sp-net-gram/services/spnet-gram-backend
pip install -r requirements.txt
python3 server.py
```

Server runs at `http://localhost:8790`.
Open the manager console at `http://localhost:8790/admin`.

The first registered user is automatically promoted to `admin` if no admin exists yet.

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
- `GET /api/access/status`
- `GET /api/license/status`
- `POST /api/license/redeem`
- `GET /api/admin/licenses` (manager/admin)
- `POST /api/admin/licenses/create` (manager/admin)
- `POST /api/admin/licenses/revoke` (manager/admin)
- `POST /api/admin/licenses/update` (manager/admin)
- `GET /api/admin/users` (admin)
- `POST /api/admin/users/role` (admin)

## IAP Verification (optional)
Set environment variables to enable receipt verification:
- `IAP_VERIFY=1`
- `GOOGLE_PACKAGE_NAME`
- `GOOGLE_SERVICE_ACCOUNT_JSON` (service account JSON string)
- `APPLE_SHARED_SECRET`

## License Enforcement
By default, the backend requires an active premium license to use protected endpoints. You can disable enforcement in development with:
- `REQUIRE_LICENSE=0`

Apple verification uses the legacy `verifyReceipt` endpoint with sandbox fallback when needed.
