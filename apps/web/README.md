# SP NET GRAM Web

## Setup
```bash
cd /Users/savanpatel/Documents/sp-net-gram/apps/web
npm install
npm run dev
```

## Telegram (MTProto)
Set environment variables in `.env`:
```
VITE_TG_API_ID=123456
VITE_TG_API_HASH=your_api_hash
VITE_BACKEND_URL=https://your-backend.onrender.com
```

MTProto client wrapper lives at `telegram/mtproto_client.js`.
