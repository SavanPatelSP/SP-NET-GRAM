import json
import os
import sqlite3
import secrets
import hashlib
import datetime
import mimetypes
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

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
GEMS_BONUS = 10
GEMS_COOLDOWN_HOURS = 24
IAP_VERIFY = os.getenv("IAP_VERIFY", "0") == "1"
REQUIRE_LICENSE = os.getenv("REQUIRE_LICENSE", "1") == "1"
LICENSE_KEY_PREFIX = os.getenv("LICENSE_KEY_PREFIX", "SPG")
MAX_LICENSE_BATCH = 500
RESET_TOKEN_TTL_MIN = int(os.getenv("RESET_TOKEN_TTL_MIN", "30"))
BOOTSTRAP_EMAIL = (os.getenv("SPNET_BOOTSTRAP_EMAIL", "") or "").strip().lower()
BOOTSTRAP_ROLE = (os.getenv("SPNET_BOOTSTRAP_ROLE", "manager") or "manager").strip().lower()

ROLE_ORDER = ["user", "manager", "admin"]

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
        ensure_schema(conn)
        conn.commit()


def ensure_column(conn, table_name, column_name, definition):
    cols = [row["name"] for row in conn.execute(f"PRAGMA table_info({table_name})").fetchall()]
    if column_name in cols:
        return
    conn.execute(f"ALTER TABLE {table_name} ADD COLUMN {column_name} {definition}")


def ensure_schema(conn):
    ensure_column(conn, "users", "role", "TEXT NOT NULL DEFAULT 'user'")
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS license_keys (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          license_key TEXT UNIQUE NOT NULL,
          plan_id TEXT NOT NULL,
          status TEXT NOT NULL,
          max_uses INTEGER NOT NULL DEFAULT 1,
          uses INTEGER NOT NULL DEFAULT 0,
          duration_days INTEGER,
          expires_at TEXT,
          notes TEXT,
          created_by INTEGER,
          created_at TEXT NOT NULL,
          updated_at TEXT NOT NULL,
          FOREIGN KEY(created_by) REFERENCES users(id)
        )
        """
    )
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS license_redemptions (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          license_id INTEGER NOT NULL,
          user_id INTEGER NOT NULL,
          redeemed_at TEXT NOT NULL,
          FOREIGN KEY(license_id) REFERENCES license_keys(id),
          FOREIGN KEY(user_id) REFERENCES users(id)
        )
        """
    )
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS gems_status (
          user_id INTEGER PRIMARY KEY,
          last_claim_at TEXT,
          updated_at TEXT NOT NULL,
          FOREIGN KEY(user_id) REFERENCES users(id)
        )
        """
    )
    conn.execute("CREATE INDEX IF NOT EXISTS idx_license_keys_key ON license_keys(license_key)")
    conn.execute("CREATE INDEX IF NOT EXISTS idx_license_redemptions_user ON license_redemptions(user_id)")
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS password_resets (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          user_id INTEGER NOT NULL,
          token TEXT UNIQUE NOT NULL,
          created_at TEXT NOT NULL,
          expires_at TEXT NOT NULL,
          used_at TEXT,
          FOREIGN KEY(user_id) REFERENCES users(id)
        )
        """
    )
    conn.execute("CREATE INDEX IF NOT EXISTS idx_password_resets_token ON password_resets(token)")
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS event_logs (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          event_type TEXT NOT NULL,
          level TEXT NOT NULL,
          message TEXT NOT NULL,
          user_id INTEGER,
          metadata TEXT,
          created_at TEXT NOT NULL,
          FOREIGN KEY(user_id) REFERENCES users(id)
        )
        """
    )
    conn.execute("CREATE INDEX IF NOT EXISTS idx_event_logs_type ON event_logs(event_type)")
    conn.execute("CREATE INDEX IF NOT EXISTS idx_event_logs_user ON event_logs(user_id)")


def ensure_bootstrap_user():
    if not BOOTSTRAP_EMAIL:
        return
    desired_role = BOOTSTRAP_ROLE if BOOTSTRAP_ROLE in ROLE_ORDER else "manager"
    with db_connect() as conn:
        row = conn.execute(
            "SELECT id, role FROM users WHERE email = ? COLLATE NOCASE",
            (BOOTSTRAP_EMAIL,),
        ).fetchone()
        if not row:
            return
        if row["role"] != desired_role:
            conn.execute(
                "UPDATE users SET role = ? WHERE id = ?",
                (desired_role, row["id"]),
            )
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


def log_event(event_type, message, user_id=None, level="info", metadata=None):
    try:
        meta_text = json.dumps(metadata) if metadata is not None else None
        with db_connect() as conn:
            conn.execute(
                "INSERT INTO event_logs (event_type, level, message, user_id, metadata, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                (event_type, level, message, user_id, meta_text, now_iso()),
            )
            conn.commit()
    except Exception:
        pass


def build_reset_token():
    token = secrets.token_urlsafe(9)
    expires_at = datetime.datetime.utcnow() + datetime.timedelta(minutes=RESET_TOKEN_TTL_MIN)
    return token, expires_at.replace(microsecond=0).isoformat() + "Z"


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


def serve_static(handler, rel_path):
    static_dir = os.path.join(BASE_DIR, "static")
    full_path = os.path.realpath(os.path.join(static_dir, rel_path))
    if not full_path.startswith(os.path.realpath(static_dir)):
        handler.send_response(403)
        handler.end_headers()
        return
    if not os.path.exists(full_path) or not os.path.isfile(full_path):
        handler.send_response(404)
        handler.end_headers()
        return
    content_type, _ = mimetypes.guess_type(full_path)
    content_type = content_type or "application/octet-stream"
    with open(full_path, "rb") as f:
        data = f.read()
    handler.send_response(200)
    handler.send_header("Content-Type", content_type)
    handler.send_header("Content-Length", str(len(data)))
    handler.end_headers()
    handler.wfile.write(data)


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


def require_role(user, allowed_roles):
    if user is None:
        return False
    try:
        role = user["role"]
    except Exception:
        role = getattr(user, "role", None)
    return role in allowed_roles


def maybe_bootstrap_admin(conn, user_id):
    row = conn.execute("SELECT COUNT(*) AS cnt FROM users WHERE role = 'admin'").fetchone()
    if row and row["cnt"] == 0:
        conn.execute("UPDATE users SET role = 'admin' WHERE id = ?", (user_id,))


def generate_license_key(conn):
    for _ in range(50):
        raw = secrets.token_hex(8).upper()
        key = f"{LICENSE_KEY_PREFIX}-{raw[0:4]}-{raw[4:8]}-{raw[8:12]}-{raw[12:16]}"
        exists = conn.execute("SELECT 1 FROM license_keys WHERE license_key = ?", (key,)).fetchone()
        if not exists:
            return key
    raise RuntimeError("license_key_generation_failed")


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


def is_premium_active(status):
    if not status:
        return False
    if status["planId"] == "free":
        return False
    if status["status"] != "active":
        return False
    if status["expiresAt"]:
        exp = parse_iso(status["expiresAt"])
        if exp and exp < datetime.datetime.utcnow():
            return False
    return True


def get_access_state(conn, user_id):
    premium = get_premium_status(conn, user_id)
    can_use = is_premium_active(premium) if REQUIRE_LICENSE else True
    return {
        "canUse": can_use,
        "reason": None if can_use else "license_required",
        "premium": premium,
        "requireLicense": REQUIRE_LICENSE,
    }


def enforce_access(handler, conn, user):
    if not REQUIRE_LICENSE:
        return True
    access = get_access_state(conn, user["id"])
    if access["canUse"]:
        return True
    json_response(handler, 403, {"error": "license_required", "access": access})
    return False


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


def get_gems_status(conn, user_id):
    row = conn.execute(
        "SELECT last_claim_at FROM gems_status WHERE user_id = ?",
        (user_id,),
    ).fetchone()
    last_claim = parse_iso(row["last_claim_at"]) if row else None
    if last_claim:
        cooldown = datetime.timedelta(hours=GEMS_COOLDOWN_HOURS)
        next_claim = last_claim + cooldown
        can_claim = datetime.datetime.utcnow() >= next_claim
    else:
        next_claim = None
        can_claim = True
    return {
        "lastClaimAt": last_claim.isoformat() + "Z" if last_claim else None,
        "nextClaimAt": next_claim.isoformat().replace("+00:00", "Z") if next_claim else None,
        "canClaim": can_claim,
        "cooldownHours": GEMS_COOLDOWN_HOURS,
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
        if parsed.path in ("/", "/admin"):
            return serve_static(self, "admin.html")
        if parsed.path.startswith("/static/"):
            return serve_static(self, parsed.path[len("/static/"):])
        if parsed.path == "/api/health":
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/auth/me":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                access = get_access_state(conn, user["id"])
            role = None
            try:
                role = user["role"]
            except Exception:
                role = "user"
            return json_response(self, 200, {
                "id": user["id"],
                "email": user["email"],
                "displayName": user["display_name"],
                "spgId": user["spg_id"],
                "role": role or "user",
                "access": access,
            })

        if parsed.path == "/api/profile":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
            return json_response(self, 200, {
                "id": user["id"],
                "email": user["email"],
                "displayName": user["display_name"],
                "spgId": user["spg_id"],
            })

        if parsed.path == "/api/access/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                access = get_access_state(conn, user["id"])
            return json_response(self, 200, access)

        if parsed.path == "/api/license/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                access = get_access_state(conn, user["id"])
                redemptions = conn.execute(
                    "SELECT lk.license_key, lk.plan_id, lk.expires_at, lr.redeemed_at FROM license_redemptions lr JOIN license_keys lk ON lk.id = lr.license_id WHERE lr.user_id = ? ORDER BY lr.id DESC LIMIT 5",
                    (user["id"],),
                ).fetchall()
            return json_response(self, 200, {
                "access": access,
                "recentRedemptions": [dict(row) for row in redemptions],
            })

        if parsed.path == "/api/wallet":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
                wallet = conn.execute(
                    "SELECT sp_coin, gems FROM wallet WHERE user_id = ?",
                    (user["id"],),
                ).fetchone()
                tx = conn.execute(
                    "SELECT amount, currency, description, created_at FROM wallet_tx WHERE user_id = ? ORDER BY id DESC LIMIT 20",
                    (user["id"],),
                ).fetchall()
                airdrop = get_airdrop_status(conn, user["id"])
                gems_status = get_gems_status(conn, user["id"])
            return json_response(self, 200, {
                "spCoin": wallet["sp_coin"],
                "gems": wallet["gems"],
                "history": [dict(row) for row in tx],
                "airdrop": airdrop,
                "gemsStatus": gems_status,
            })

        if parsed.path == "/api/wallet/airdrop/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
                status = get_airdrop_status(conn, user["id"])
            return json_response(self, 200, status)

        if parsed.path == "/api/wallet/gems/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
                status = get_gems_status(conn, user["id"])
            return json_response(self, 200, status)

        if parsed.path == "/api/premium/plans":
            return json_response(self, 200, {"plans": PREMIUM_PLANS})

        if parsed.path == "/api/premium/status":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                status = get_premium_status(conn, user["id"])
                access = get_access_state(conn, user["id"])
            return json_response(self, 200, {"premium": status, "access": access})

        if parsed.path == "/api/admin/logs":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            params = parse_qs(parsed.query or "")
            limit = params.get("limit", ["200"])[0]
            event_type = params.get("type", [None])[0]
            level = params.get("level", [None])[0]
            user_id = params.get("userId", [None])[0]
            try:
                limit = max(1, min(int(limit), 500))
            except ValueError:
                limit = 200
            where = []
            values = []
            if event_type:
                where.append("event_type = ?")
                values.append(event_type)
            if level:
                where.append("level = ?")
                values.append(level)
            if user_id:
                where.append("user_id = ?")
                values.append(user_id)
            query = "SELECT id, event_type, level, message, user_id, metadata, created_at FROM event_logs"
            if where:
                query += " WHERE " + " AND ".join(where)
            query += " ORDER BY id DESC LIMIT ?"
            values.append(limit)
            with db_connect() as conn:
                rows = conn.execute(query, tuple(values)).fetchall()
            logs = []
            for row in rows:
                item = dict(row)
                if item.get("metadata"):
                    try:
                        item["metadata"] = json.loads(item["metadata"])
                    except Exception:
                        pass
                logs.append(item)
            return json_response(self, 200, {"logs": logs})

        if parsed.path == "/api/admin/licenses":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            params = parse_qs(parsed.query or "")
            status = params.get("status", [None])[0]
            limit = params.get("limit", ["200"])[0]
            try:
                limit = max(1, min(int(limit), 500))
            except ValueError:
                limit = 200
            with db_connect() as conn:
                if status:
                    rows = conn.execute(
                        "SELECT * FROM license_keys WHERE status = ? ORDER BY id DESC LIMIT ?",
                        (status, limit),
                    ).fetchall()
                else:
                    rows = conn.execute(
                        "SELECT * FROM license_keys ORDER BY id DESC LIMIT ?",
                        (limit,),
                    ).fetchall()
            return json_response(self, 200, {"licenses": [dict(row) for row in rows]})

        if parsed.path == "/api/admin/users":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            params = parse_qs(parsed.query or "")
            limit = params.get("limit", ["200"])[0]
            try:
                limit = max(1, min(int(limit), 500))
            except ValueError:
                limit = 200
            with db_connect() as conn:
                rows = conn.execute(
                    """
                    SELECT u.id, u.email, u.display_name, u.role, u.created_at,
                           (SELECT MAX(last_seen) FROM sessions s WHERE s.user_id = u.id) AS last_seen,
                           (SELECT COUNT(*) FROM sessions s WHERE s.user_id = u.id) AS session_count
                    FROM users u
                    ORDER BY u.id DESC
                    LIMIT ?
                    """,
                    (limit,),
                ).fetchall()
                users = []
                for row in rows:
                    data = dict(row)
                    data["access"] = get_access_state(conn, row["id"])
                    users.append(data)
            return json_response(self, 200, {"users": users})

        return json_response(self, 404, {"error": "Not found"})

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/auth/register":
            payload = read_json(self)
            email = (payload.get("email") or "").strip().lower()
            password = (payload.get("password") or "").strip()
            display_name = (payload.get("displayName") or "").strip()
            if not email or not password:
                return json_response(self, 400, {"error": "Missing fields"})
            if not display_name and "@" in email:
                display_name = email.split("@", 1)[0]
            if not display_name:
                display_name = "SP NET GRAM User"
            with db_connect() as conn:
                try:
                    existing = conn.execute(
                        "SELECT id FROM users WHERE email = ? COLLATE NOCASE",
                        (email,),
                    ).fetchone()
                    if existing:
                        log_event("auth.register.exists", "User already exists", existing["id"], "warn", {"email": email})
                        return json_response(self, 409, {"error": "User already exists"})
                    conn.execute(
                        "INSERT INTO users (email, password_hash, display_name, created_at) VALUES (?, ?, ?, ?)",
                        (email, encode_password(password), display_name, now_iso()),
                    )
                    user = conn.execute("SELECT id, role FROM users WHERE email = ?", (email,)).fetchone()
                    user_id = user["id"]
                    maybe_bootstrap_admin(conn, user_id)
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
                    conn.execute(
                        "INSERT INTO gems_status (user_id, last_claim_at, updated_at) VALUES (?, ?, ?)",
                        (user_id, None, now_iso()),
                    )
                    token = secrets.token_hex(16)
                    conn.execute(
                        "INSERT INTO sessions (token, user_id, created_at, last_seen) VALUES (?, ?, ?, ?)",
                        (token, user_id, now_iso(), now_iso()),
                    )
                    access = get_access_state(conn, user_id)
                    conn.commit()
                except sqlite3.IntegrityError:
                    log_event("auth.register.exists", "User already exists", None, "warn", {"email": email})
                    return json_response(self, 409, {"error": "User already exists"})
            log_event("auth.register", "User registered", user_id, "info", {"email": email})
            return json_response(self, 200, {"ok": True, "token": token, "role": user["role"], "access": access})

        if parsed.path == "/api/auth/reset/request":
            payload = read_json(self)
            email_raw = payload.get("email") or ""
            email = email_raw.strip().lower()
            if not email:
                return json_response(self, 400, {"error": "Missing fields"})
            with db_connect() as conn:
                user = conn.execute(
                    "SELECT id FROM users WHERE email = ? COLLATE NOCASE",
                    (email,),
                ).fetchone()
                if user:
                    conn.execute("DELETE FROM password_resets WHERE user_id = ?", (user["id"],))
                    token, expires_at = build_reset_token()
                    conn.execute(
                        "INSERT INTO password_resets (user_id, token, created_at, expires_at) VALUES (?, ?, ?, ?)",
                        (user["id"], token, now_iso(), expires_at),
                    )
                    conn.commit()
                    log_event("auth.reset.request", "Password reset requested", user["id"], "info", {"email": email})
                    return json_response(self, 200, {"ok": True, "resetToken": token, "expiresAt": expires_at})
            log_event("auth.reset.request", "Password reset requested (not found)", None, "warn", {"email": email})
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/auth/reset/confirm":
            payload = read_json(self)
            token = (payload.get("token") or "").strip()
            new_password = payload.get("newPassword") or ""
            if not token or not new_password:
                return json_response(self, 400, {"error": "Missing fields"})
            with db_connect() as conn:
                row = conn.execute(
                    "SELECT id, user_id, expires_at, used_at FROM password_resets WHERE token = ?",
                    (token,),
                ).fetchone()
                if not row:
                    log_event("auth.reset.confirm", "Invalid reset token", None, "warn")
                    return json_response(self, 400, {"error": "Invalid reset token"})
                if row["used_at"]:
                    log_event("auth.reset.confirm", "Reset token already used", row["user_id"], "warn")
                    return json_response(self, 400, {"error": "Reset token already used"})
                expires_at = parse_iso(row["expires_at"])
                if expires_at and expires_at < datetime.datetime.utcnow().replace(tzinfo=None):
                    log_event("auth.reset.confirm", "Reset token expired", row["user_id"], "warn")
                    return json_response(self, 400, {"error": "Reset token expired"})
                conn.execute(
                    "UPDATE users SET password_hash = ? WHERE id = ?",
                    (encode_password(new_password), row["user_id"]),
                )
                conn.execute(
                    "UPDATE password_resets SET used_at = ? WHERE id = ?",
                    (now_iso(), row["id"]),
                )
                conn.execute("DELETE FROM sessions WHERE user_id = ?", (row["user_id"],))
                conn.commit()
            log_event("auth.reset.confirm", "Password reset", row["user_id"], "info")
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/auth/login":
            payload = read_json(self)
            email_raw = payload.get("email") or ""
            password_raw = payload.get("password") or ""
            email = email_raw.strip().lower()
            password_trim = password_raw.strip()
            if not email or not password_raw:
                return json_response(self, 400, {"error": "Missing fields"})
            with db_connect() as conn:
                user = conn.execute(
                    "SELECT * FROM users WHERE email = ? COLLATE NOCASE",
                    (email,),
                ).fetchone()
                if not user and email_raw:
                    user = conn.execute(
                        "SELECT * FROM users WHERE email = ?",
                        (email_raw,),
                    ).fetchone()
                password_ok = False
                if user:
                    password_ok = verify_password(password_raw, user["password_hash"])
                    if not password_ok and password_trim and password_trim != password_raw:
                        password_ok = verify_password(password_trim, user["password_hash"])
                if not user or not password_ok:
                    log_event("auth.login.failed", "Invalid credentials", None, "warn", {"email": email})
                    return json_response(self, 401, {"error": "Invalid credentials"})
                token = secrets.token_hex(16)
                conn.execute(
                    "INSERT INTO sessions (token, user_id, created_at, last_seen) VALUES (?, ?, ?, ?)",
                    (token, user["id"], now_iso(), now_iso()),
                )
                access = get_access_state(conn, user["id"])
                conn.commit()
            log_event("auth.login", "User signed in", user["id"], "info", {"email": email})
            return json_response(self, 200, {"token": token, "role": user["role"], "access": access})

        if parsed.path == "/api/profile/spg-id/mint":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if user["spg_id"]:
                log_event("spg_id.existing", "SPG ID already minted", user["id"], "info")
                return json_response(self, 200, {"spgId": user["spg_id"], "status": "existing"})
            spg_id = mint_spg_id()
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
                conn.execute("UPDATE users SET spg_id = ? WHERE id = ?", (spg_id, user["id"]))
                conn.commit()
            log_event("spg_id.mint", "SPG ID minted", user["id"], "info", {"spgId": spg_id})
            return json_response(self, 200, {"spgId": spg_id, "status": "minted"})

        if parsed.path == "/api/wallet/airdrop/claim":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
                status = get_airdrop_status(conn, user["id"])
                if not status["canClaim"]:
                    log_event("wallet.airdrop.cooldown", "Airdrop cooldown", user["id"], "warn", {"nextClaimAt": status["nextClaimAt"]})
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
            log_event("wallet.airdrop.claim", "Airdrop claimed", user["id"], "info", {"amount": AIRDROP_BONUS})
            return json_response(self, 200, {"spCoin": new_balance, "claimed": AIRDROP_BONUS})

        if parsed.path == "/api/wallet/gems/claim":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
                status = get_gems_status(conn, user["id"])
                if not status["canClaim"]:
                    log_event("wallet.gems.cooldown", "Gems cooldown", user["id"], "warn", {"nextClaimAt": status["nextClaimAt"]})
                    return json_response(self, 400, {"error": "Gems cooldown", "nextClaimAt": status["nextClaimAt"]})
                wallet = conn.execute(
                    "SELECT gems FROM wallet WHERE user_id = ?",
                    (user["id"],),
                ).fetchone()
                new_balance = wallet["gems"] + GEMS_BONUS
                conn.execute(
                    "UPDATE wallet SET gems = ?, updated_at = ? WHERE user_id = ?",
                    (new_balance, now_iso(), user["id"]),
                )
                conn.execute(
                    "INSERT INTO wallet_tx (user_id, amount, currency, description, created_at) VALUES (?, ?, ?, ?, ?)",
                    (user["id"], GEMS_BONUS, "GEM", "Gems claim", now_iso()),
                )
                conn.execute(
                    "INSERT OR REPLACE INTO gems_status (user_id, last_claim_at, updated_at) VALUES (?, ?, ?)",
                    (user["id"], now_iso(), now_iso()),
                )
                conn.commit()
            log_event("wallet.gems.claim", "Gems claimed", user["id"], "info", {"amount": GEMS_BONUS})
            return json_response(self, 200, {"gems": new_balance, "claimed": GEMS_BONUS})

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
            log_event("premium.subscribe", "Premium updated", user["id"], "info", {"planId": plan_id, "platform": platform})
            return json_response(self, 200, {
                "planId": plan_id,
                "status": "active",
                "platform": platform,
                "expiresAt": expires_at,
                "receiptStored": bool(receipt),
            })

        if parsed.path == "/api/license/redeem":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            payload = read_json(self)
            key = (payload.get("licenseKey") or payload.get("key") or "").strip()
            if not key:
                return json_response(self, 400, {"error": "Missing license key"})
            with db_connect() as conn:
                license_row = conn.execute(
                    "SELECT * FROM license_keys WHERE license_key = ?",
                    (key,),
                ).fetchone()
                if not license_row:
                    log_event("license.redeem.failed", "License not found", user["id"], "warn")
                    return json_response(self, 404, {"error": "License not found"})
                if license_row["status"] != "active":
                    log_event("license.redeem.failed", "License not active", user["id"], "warn", {"status": license_row["status"]})
                    return json_response(self, 400, {"error": "License not active", "status": license_row["status"]})
                if license_row["expires_at"]:
                    exp = parse_iso(license_row["expires_at"])
                    if exp and exp < datetime.datetime.utcnow():
                        log_event("license.redeem.failed", "License expired", user["id"], "warn")
                        return json_response(self, 400, {"error": "License expired"})
                if license_row["uses"] >= license_row["max_uses"]:
                    log_event("license.redeem.failed", "License exhausted", user["id"], "warn")
                    return json_response(self, 400, {"error": "License exhausted"})
                existing = conn.execute(
                    "SELECT 1 FROM license_redemptions WHERE license_id = ? AND user_id = ?",
                    (license_row["id"], user["id"]),
                ).fetchone()
                if existing:
                    log_event("license.redeem.failed", "License already redeemed", user["id"], "warn")
                    return json_response(self, 400, {"error": "License already redeemed"})
                now = datetime.datetime.utcnow().replace(microsecond=0)
                duration_days = license_row["duration_days"]
                if duration_days:
                    expires_at = (now + datetime.timedelta(days=duration_days)).isoformat() + "Z"
                else:
                    expires_at = None
                conn.execute(
                    "INSERT INTO license_redemptions (license_id, user_id, redeemed_at) VALUES (?, ?, ?)",
                    (license_row["id"], user["id"], now_iso()),
                )
                new_uses = license_row["uses"] + 1
                new_status = "exhausted" if new_uses >= license_row["max_uses"] else license_row["status"]
                conn.execute(
                    "UPDATE license_keys SET uses = ?, status = ?, updated_at = ? WHERE id = ?",
                    (new_uses, new_status, now_iso(), license_row["id"]),
                )
                conn.execute(
                    "INSERT OR REPLACE INTO premium (user_id, plan_id, status, platform, receipt, expires_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (user["id"], license_row["plan_id"], "active", "license", key, expires_at, now_iso()),
                )
                conn.commit()
                access = get_access_state(conn, user["id"])
            log_event("license.redeem", "License redeemed", user["id"], "info", {"planId": license_row["plan_id"]})
            return json_response(self, 200, {
                "ok": True,
                "planId": license_row["plan_id"],
                "expiresAt": expires_at,
                "access": access,
            })

        if parsed.path == "/api/admin/licenses/create":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            payload = read_json(self)
            plan_id = payload.get("planId")
            count = payload.get("count", 1)
            max_uses = payload.get("maxUses", 1)
            duration_days = payload.get("durationDays")
            expires_at = payload.get("expiresAt")
            notes = payload.get("notes")
            if plan_id not in [p["id"] for p in PREMIUM_PLANS if p["id"] != "free"]:
                return json_response(self, 400, {"error": "Invalid planId"})
            try:
                count = max(1, min(int(count), MAX_LICENSE_BATCH))
                max_uses = max(1, int(max_uses))
                if duration_days is not None:
                    duration_days = int(duration_days)
                    if duration_days <= 0:
                        return json_response(self, 400, {"error": "durationDays must be > 0"})
            except ValueError:
                return json_response(self, 400, {"error": "Invalid numeric values"})
            now = now_iso()
            with db_connect() as conn:
                created = []
                for _ in range(count):
                    key = generate_license_key(conn)
                    conn.execute(
                        "INSERT INTO license_keys (license_key, plan_id, status, max_uses, uses, duration_days, expires_at, notes, created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        (
                            key,
                            plan_id,
                            "active",
                            max_uses,
                            0,
                            duration_days,
                            expires_at,
                            notes,
                            user["id"],
                            now,
                            now,
                        ),
                    )
                    created.append({
                        "licenseKey": key,
                        "planId": plan_id,
                        "status": "active",
                        "maxUses": max_uses,
                        "uses": 0,
                        "durationDays": duration_days,
                        "expiresAt": expires_at,
                        "notes": notes,
                        "createdAt": now,
                    })
                conn.commit()
            log_event(
                "admin.license.create",
                "Created license keys",
                user["id"],
                "info",
                {
                    "count": count,
                    "planId": plan_id,
                    "maxUses": max_uses,
                    "durationDays": duration_days,
                    "expiresAt": expires_at,
                },
            )
            return json_response(self, 200, {"created": created})

        if parsed.path == "/api/admin/licenses/revoke":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            payload = read_json(self)
            key = payload.get("licenseKey") or payload.get("key")
            if not key:
                return json_response(self, 400, {"error": "Missing licenseKey"})
            with db_connect() as conn:
                row = conn.execute("SELECT id FROM license_keys WHERE license_key = ?", (key,)).fetchone()
                if not row:
                    return json_response(self, 404, {"error": "License not found"})
                conn.execute(
                    "UPDATE license_keys SET status = ?, updated_at = ? WHERE license_key = ?",
                    ("revoked", now_iso(), key),
                )
                conn.commit()
            log_event(
                "admin.license.revoke",
                "Revoked license",
                user["id"],
                "warn",
                {"licenseKey": key},
            )
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/admin/licenses/update":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            payload = read_json(self)
            key = payload.get("licenseKey") or payload.get("key")
            if not key:
                return json_response(self, 400, {"error": "Missing licenseKey"})
            fields = []
            values = []
            for field in ("status", "maxUses", "expiresAt", "notes"):
                if field in payload:
                    if field == "maxUses":
                        try:
                            payload[field] = int(payload[field])
                        except ValueError:
                            return json_response(self, 400, {"error": "Invalid maxUses"})
                        fields.append("max_uses = ?")
                        values.append(payload[field])
                    elif field == "expiresAt":
                        fields.append("expires_at = ?")
                        values.append(payload[field])
                    else:
                        fields.append(f"{field} = ?")
                        values.append(payload[field])
            if not fields:
                return json_response(self, 400, {"error": "No fields to update"})
            values.append(now_iso())
            values.append(key)
            with db_connect() as conn:
                conn.execute(
                    f"UPDATE license_keys SET {', '.join(fields)}, updated_at = ? WHERE license_key = ?",
                    tuple(values),
                )
                conn.commit()
            log_event(
                "admin.license.update",
                "Updated license",
                user["id"],
                "info",
                {
                    "licenseKey": key,
                    "fields": {k: payload[k] for k in ("status", "maxUses", "expiresAt", "notes") if k in payload},
                },
            )
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/admin/users/revoke-sessions":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            payload = read_json(self)
            user_id = payload.get("userId")
            if not user_id:
                return json_response(self, 400, {"error": "Missing userId"})
            with db_connect() as conn:
                res = conn.execute("DELETE FROM sessions WHERE user_id = ?", (user_id,))
                conn.commit()
            log_event(
                "admin.sessions.revoke",
                "Revoked sessions",
                user["id"],
                "warn",
                {"targetId": user_id, "revoked": res.rowcount},
            )
            return json_response(self, 200, {"ok": True, "revoked": res.rowcount})

        if parsed.path == "/api/admin/users/reset-password":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["manager", "admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            payload = read_json(self)
            user_id = payload.get("userId")
            if not user_id:
                return json_response(self, 400, {"error": "Missing userId"})
            with db_connect() as conn:
                target = conn.execute("SELECT id FROM users WHERE id = ?", (user_id,)).fetchone()
                if not target:
                    return json_response(self, 404, {"error": "User not found"})
                conn.execute("DELETE FROM password_resets WHERE user_id = ?", (user_id,))
                token, expires_at = build_reset_token()
                conn.execute(
                    "INSERT INTO password_resets (user_id, token, created_at, expires_at) VALUES (?, ?, ?, ?)",
                    (user_id, token, now_iso(), expires_at),
                )
                conn.commit()
            log_event(
                "admin.password.reset",
                "Generated reset token",
                user["id"],
                "info",
                {"targetId": user_id},
            )
            return json_response(self, 200, {"ok": True, "resetToken": token, "expiresAt": expires_at})

        if parsed.path == "/api/admin/users/role":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            if not require_role(user, ["admin"]):
                return json_response(self, 403, {"error": "Forbidden"})
            payload = read_json(self)
            target_id = payload.get("userId")
            role = payload.get("role")
            if not target_id or role not in ROLE_ORDER:
                return json_response(self, 400, {"error": "Invalid userId or role"})
            with db_connect() as conn:
                conn.execute("UPDATE users SET role = ? WHERE id = ?", (role, target_id))
                conn.commit()
            log_event("admin.user.role", "Updated user role", user["id"], "info", {"targetId": target_id, "role": role})
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/logs/ingest":
            payload = read_json(self)
            event_type = payload.get("type") or payload.get("eventType") or "client.event"
            level = payload.get("level") or "info"
            message = payload.get("message") or ""
            metadata = payload.get("metadata")
            if len(message) > 2000:
                message = message[:2000]
            token = get_token(self)
            user = get_user_by_token(token) if token else None
            user_id = user["id"] if user else None
            log_event(event_type, message or "client log", user_id, level, metadata)
            return json_response(self, 200, {"ok": True})

        if parsed.path == "/api/assistant/chat":
            user = get_user_by_token(get_token(self))
            if not user:
                return json_response(self, 401, {"error": "Unauthorized"})
            payload = read_json(self)
            messages = payload.get("messages", [])
            intent = payload.get("intent", "general")
            last_user = next((m.get("content") for m in reversed(messages) if m.get("role") == "user"), "")
            with db_connect() as conn:
                if not enforce_access(self, conn, user):
                    return
            log_event("assistant.chat", "Assistant chat", user["id"], "info", {"intent": intent, "length": len(last_user or "")})
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
    ensure_bootstrap_user()
    port = int(os.getenv("PORT", "8790"))
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"SP NET GRAM backend running on http://localhost:{port}")
    server.serve_forever()
