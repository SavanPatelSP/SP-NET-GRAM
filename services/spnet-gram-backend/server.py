import json
import os
import sqlite3
import secrets
import hashlib
import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DB_PATH = os.getenv("SPNET_DB_PATH", os.path.join(BASE_DIR, "spnet_gram.db"))


def resolve_db_path(candidate):
    if not candidate:
        return None
    folder = os.path.dirname(candidate)
    if folder:
        try:
            os.makedirs(folder, exist_ok=True)
        except Exception:
            return None
    try:
        with open(candidate, "a", encoding="utf-8"):
            pass
    except Exception:
        return None
    return candidate


DB_PATH = resolve_db_path(RAW_DB_PATH)
if DB_PATH is None:
    DB_PATH = resolve_db_path(os.path.join("/tmp", "spnet_gram.db"))
if DB_PATH is None:
    DB_PATH = os.path.join(BASE_DIR, "spnet_gram.db")

DEFAULT_SP_COIN = 2000
DEFAULT_GEMS = 50
AIRDROP_BONUS = 500
AIRDROP_COOLDOWN_HOURS = 24
IAP_VERIFY = os.getenv("IAP_VERIFY", "0") == "1"

PREMIUM_PLANS = [
    {
        "id": "free",
        "name": "Free",
        "price": 0,
        "productIds": {"android": None, "ios": None},
        "perks": ["Basic chat", "Limited assistant", "1 folder"],
    },
    {
        "id": "plus",
        "name": "Plus",
        "price": 4.99,
        "productIds": {"android": "spnetgram_plus_android", "ios": "spnetgram_plus_ios"},
        "perks": ["Unlimited assistant", "4 folders", "SPG badge"],
    },
    {
        "id": "pro",
        "name": "Pro",
        "price": 9.99,
        "productIds": {"android": "spnetgram_pro_android", "ios": "spnetgram_pro_ios"},
        "perks": ["Priority features", "8 folders", "Airdrop boosts"],
    },
]


def now_iso():
    return datetime.datetime.utcnow().replace(microsecond=0).isoformat() + "Z"


def parse_iso(ts):
    if not ts:
        return None
    if ts.endswith("Z"):
        ts = ts.replace("Z", "+00:00")
    try:
        return datetime.datetime.fromisoformat(ts)
    except ValueError:
        return None


def db_connect():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    schema_path = os.path.join(BASE_DIR, "schema.sql")
    with db_connect() as conn, open(schema_path, "r", encoding="utf-8") as f:
        conn.executescript(f.read())
        conn.commit()


def hash_password(password: str, salt: str) -> str:
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 120000)
    return digest.hex()


def encode_password(password: str) -> str:
    salt = secrets.token_hex(8)
    return f"{salt}${hash_password(password, salt)}"


def verify_password(password: str, encoded: str) -> bool:
    try:
        salt, digest = encoded.split("$", 1)
    except ValueError:
        return False
    return hash_password(password, salt) == digest


def read_json(handler):
    length = int(handler.headers.get("Content-Length", 0))
    if length == 0:
        return {}
    raw = handler.rfile.read(length)
    return json.loads(raw.decode("utf-8"))


def json_response(handler, status, payload):
    body = json.dumps(payload).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


def get_token(handler):
    auth = handler.headers.get("Authorization", "")
    if auth.startswith("Bearer "):
        return auth.replace("Bearer ", "", 1)
    return None


def get_user_by_token(token):
    if not token:
        return None
    with db_connect() as conn:
        row = conn.execute(
            "SELECT u.* FROM sessions s JOIN users u ON u.id = s.user_id WHERE s.token = ?",
            (token,),
        ).fetchone()
        if row:
            conn.execute("UPDATE sessions SET last_seen = ? WHERE token = ?", (now_iso(), token))
            conn.commit()
        return row


def mint_spg_id():
    return f"SPG-{secrets.token_hex(2).upper()}-{secrets.token_hex(2).upper()}"


def verify_iap(platform, receipt):
    if not IAP_VERIFY:
        return {"ok": True, "status": "skipped"}
    if not receipt:
        return {"ok": False, "reason": "missing_receipt"}
    try:
        from iap_verification import verify_receipt
    except Exception as exc:
        return {"ok": False, "reason": f"verification_module_error: {exc}"}
    return verify_receipt(platform, receipt)


def get_premium_status(conn, user_id):
    row = conn.execute(
        "SELECT plan_id, status, platform, receipt, expires_at, updated_at FROM premium WHERE user_id = ?",
        (user_id,),
    ).fetchone()
    if not row:
        return {
            "planId": "free",
            "status": "active",
            "platform": None,
            "expiresAt": None,
            "updatedAt": now_iso(),
            "receiptStored": False,
        }
    return {
        "planId": row["plan_id"],
        "status": row["status"],
        "platform": row["platform"],
        "expiresAt": row["expires_at"],
        "updatedAt": row["updated_at"],
        "receiptStored": bool(row["receipt"]),
    }


def get_airdrop_status(conn, user_id):
    row = conn.execute(
        "SELECT last_claim_at FROM airdrop_status WHERE user_id = ?",
        (user_id,),
    ).fetchone()
    last_claim = parse_iso(row["last_claim_at"]) if row else None
    if last_claim:
        cooldown = datetime.timedelta(hours=AIRDROP_COOLDOWN_HOURS)
        next_claim = last_claim + cooldown
        can_claim = datetime.datetime.utcnow() >= next_claim
    else:
        next_claim = None
        can_claim = True
    return {
        "lastClaimAt": last_claim.isoformat() + "Z" if last_claim else None,
        "nextClaimAt": next_claim.isoformat().replace("+00:00", "Z") if next_claim else None,
        "canClaim": can_claim,
        "cooldownHours": AIRDROP_COOLDOWN_HOURS,
    }


class Handler(BaseHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/health":
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/auth/me":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            return json_response(self, 200, {
                "id": user["id"],
                "email": user["email"],
                "displayName": user["display_name"],
                "spgId": user["spg_id"],
            })

        if parsed.path == "/api/profile":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            return json_response(self, 200, {
                "id": user["id"],
                "email": user["email"],
                "displayName": user["display_name"],
                "spgId": user["spg_id"],
            })

        if parsed.path == "/api/wallet":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                wallet = conn.execute(
                    "SELECT sp_coin, gems FROM wallet WHERE user_id = ?",
                    (user["id"],),
                ).fetchone()
                tx = conn.execute(
                    "SELECT amount, currency, description, created_at FROM wallet_tx WHERE user_id = ? ORDER BY id DESC LIMIT 20",
                    (user["id"],),
                ).fetchall()
                airdrop = get_airdrop_status(conn, user["id"])
            return json_response(self, 200, {
                "spCoin": wallet["sp_coin"],
                "gems": wallet["gems"],
                "history": [dict(row) for row in tx],
                "airdrop": airdrop,
            })

        if parsed.path == "/api/wallet/airdrop/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                status = get_airdrop_status(conn, user["id"])
            return json_response(self, 200, status)

        if parsed.path == "/api/premium/plans":
            return json_response(self, 200, {"plans": PREMIUM_PLANS})

        if parsed.path == "/api/premium/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                status = get_premium_status(conn, user["id"])
            return json_response(self, 200, status)

        return json_response(self, 404, {"error": "Not found"})

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/auth/register":
            payload = read_json(self)
            email = payload.get("email")
            password = payload.get("password")
            display_name = payload.get("displayName")
            if not email or not password or not display_name:
                return json_response(self, 400, {"error": "Missing fields"})
            with db_connect() as conn:
                try:
                    conn.execute(
                        "INSERT INTO users (email, password_hash, display_name, created_at) VALUES (?, ?, ?, ?)",
                        (email, encode_password(password), display_name, now_iso()),
                    )
                    user_id = conn.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone()["id"]
                    conn.execute(
                        "INSERT INTO wallet (user_id, sp_coin, gems, updated_at) VALUES (?, ?, ?, ?)",
                        (user_id, DEFAULT_SP_COIN, DEFAULT_GEMS, now_iso()),
                    )
                    conn.execute(
                        "INSERT INTO wallet_tx (user_id, amount, currency, description, created_at) VALUES (?, ?, ?, ?, ?)",
                        (user_id, DEFAULT_SP_COIN, "SP", "Welcome airdrop", now_iso()),
                    )
                    conn.execute(
                        "INSERT INTO premium (user_id, plan_id, status, platform, receipt, expires_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        (user_id, "free", "active", None, None, None, now_iso()),
                    )
                    conn.execute(
                        "INSERT INTO airdrop_status (user_id, last_claim_at, updated_at) VALUES (?, ?, ?)",
                        (user_id, None, now_iso()),
                    )
                    conn.commit()
                except sqlite3.IntegrityError:
                    return json_response(self, 409, {"error": "User already exists"})
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/auth/login":
            payload = read_json(self)
            email = payload.get("email")
            password = payload.get("password")
            if not email or not password:
                return json_response(self, 400, {"error": "Missing fields"})
            with db_connect() as conn:
                user = conn.execute(
                    "SELECT * FROM users WHERE email = ?",
                    (email,),
                ).fetchone()
                if not user or not verify_password(password, user["password_hash"]):
                    return json_response(self, 401, {"error": "Invalid credentials"})
                token = secrets.token_hex(16)
                conn.execute(
                    "INSERT INTO sessions (token, user_id, created_at, last_seen) VALUES (?, ?, ?, ?)",
                    (token, user["id"], now_iso(), now_iso()),
                )
                conn.commit()
            return json_response(self, 200, {"token": token})

        if parsed.path == "/api/profile/spg-id/mint":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if user["spg_id"]:
                return json_response(self, 200, {"spgId": user["spg_id"], "status": "existing"})
            spg_id = mint_spg_id()
            with db_connect() as conn:
                conn.execute("UPDATE users SET spg_id = ? WHERE id = ?", (spg_id, user["id"]))
                conn.commit()
            return json_response(self, 200, {"spgId": spg_id, "status": "minted"})

        if parsed.path == "/api/wallet/airdrop/claim":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                status = get_airdrop_status(conn, user["id"])
                if not status["canClaim"]:
                    return json_response(self, 400, {"error": "Airdrop cooldown", "nextClaimAt": status["nextClaimAt"]})
                wallet = conn.execute(
                    "SELECT sp_coin FROM wallet WHERE user_id = ?",
                    (user["id"],),
                ).fetchone()
                new_balance = wallet["sp_coin"] + AIRDROP_BONUS
                conn.execute(
                    "UPDATE wallet SET sp_coin = ?, updated_at = ? WHERE user_id = ?",
                    (new_balance, now_iso(), user["id"]),
                )
                conn.execute(
                    "INSERT INTO wallet_tx (user_id, amount, currency, description, created_at) VALUES (?, ?, ?, ?, ?)",
                    (user["id"], AIRDROP_BONUS, "SP", "Airdrop claim", now_iso()),
                )
                conn.execute(
                    "INSERT OR REPLACE INTO airdrop_status (user_id, last_claim_at, updated_at) VALUES (?, ?, ?)",
                    (user["id"], now_iso(), now_iso()),
                )
                conn.commit()
            return json_response(self, 200, {"spCoin": new_balance, "claimed": AIRDROP_BONUS})

        if parsed.path == "/api/premium/subscribe":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            payload = read_json(self)
            plan_id = payload.get("planId")
            platform = payload.get("platform")
            receipt = payload.get("receipt")
            if not plan_id:
                return json_response(self, 400, {"error": "Missing planId"})
            plan = next((p for p in PREMIUM_PLANS if p["id"] == plan_id), None)
            if not plan:
                return json_response(self, 400, {"error": "Unknown plan"})
            if plan_id != "free" and platform in ("android", "ios"):
                verification = verify_iap(platform, receipt)
                if not verification.get("ok"):
                    return json_response(self, 400, {"error": "Receipt verification failed", "detail": verification})
            if plan_id == "free":
                expires_at = None
            else:
                expires_at = (datetime.datetime.utcnow() + datetime.timedelta(days=30)).replace(microsecond=0).isoformat() + "Z"
            with db_connect() as conn:
                conn.execute(
                    "INSERT OR REPLACE INTO premium (user_id, plan_id, status, platform, receipt, expires_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (user["id"], plan_id, "active", platform, receipt, expires_at, now_iso()),
                )
                conn.commit()
            return json_response(self, 200, {
                "planId": plan_id,
                "status": "active",
                "platform": platform,
                "expiresAt": expires_at,
                "receiptStored": bool(receipt),
            })

        if parsed.path == "/api/assistant/chat":
            payload = read_json(self)
            messages = payload.get("messages", [])
            intent = payload.get("intent", "general")
            last_user = next((m.get("content") for m in reversed(messages) if m.get("role") == "user"), "")
            if intent == "summarize":
                reply = "(stub) Summary: key goals, decisions, and next steps captured."
                suggestions = ["Action items", "Smart replies", "Translate"]
            elif intent == "translate":
                reply = "(stub) Translation ready. Choose target language."
                suggestions = ["English", "Hindi", "Spanish"]
            elif intent == "smart_reply":
                reply = "(stub) Smart replies generated."
                suggestions = ["Sounds good", "On it", "Let’s discuss"]
            else:
                reply = f"(stub) Assistant received: {last_user[:80]}"
                suggestions = ["Summarize", "Translate", "Action items"]
            return json_response(self, 200, {"reply": reply, "suggestions": suggestions})

        return json_response(self, 404, {"error": "Not found"})

    def log_message(self, format, *args):
        return


if __name__ == "__main__":
    init_db()
    server = ThreadingHTTPServer(("0.0.0.0", 8790), Handler)
    print("SP NET GRAM backend running on http://localhost:8790")
    server.serve_forever()
