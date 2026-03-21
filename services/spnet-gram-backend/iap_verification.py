import json
import os
import requests
from google.oauth2 import service_account
from google.auth.transport.requests import Request as GoogleRequest

GOOGLE_SCOPE = ["https://www.googleapis.com/auth/androidpublisher"]
GOOGLE_PACKAGE_NAME = os.getenv("GOOGLE_PACKAGE_NAME", "")
GOOGLE_SERVICE_ACCOUNT_JSON = os.getenv("GOOGLE_SERVICE_ACCOUNT_JSON", "")

APPLE_SHARED_SECRET = os.getenv("APPLE_SHARED_SECRET", "")
APPLE_VERIFY_URL = os.getenv("APPLE_VERIFY_URL", "https://buy.itunes.apple.com/verifyReceipt")
APPLE_SANDBOX_URL = os.getenv("APPLE_SANDBOX_URL", "https://sandbox.itunes.apple.com/verifyReceipt")


def verify_receipt(platform, receipt):
    if platform == "android":
        return _verify_google(receipt)
    if platform == "ios":
        return _verify_apple(receipt)
    return {"ok": False, "reason": "unsupported_platform"}


def _verify_google(purchase_token):
    if not GOOGLE_SERVICE_ACCOUNT_JSON or not GOOGLE_PACKAGE_NAME:
        return {"ok": False, "reason": "google_not_configured"}

    credentials = service_account.Credentials.from_service_account_info(
        json.loads(GOOGLE_SERVICE_ACCOUNT_JSON),
        scopes=GOOGLE_SCOPE,
    )
    credentials.refresh(GoogleRequest())

    url = (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/"
        f"applications/{GOOGLE_PACKAGE_NAME}/purchases/subscriptionsv2/"
        f"tokens/{purchase_token}"
    )
    response = requests.get(url, headers={"Authorization": f"Bearer {credentials.token}"}, timeout=20)
    if response.status_code != 200:
        return {"ok": False, "reason": "google_api_error", "status": response.status_code, "body": response.text}

    payload = response.json()
    state = payload.get("subscriptionState")
    ok_states = {
        "SUBSCRIPTION_STATE_ACTIVE",
        "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
        "SUBSCRIPTION_STATE_ON_HOLD",
    }
    return {"ok": state in ok_states, "state": state, "raw": payload}


def _verify_apple(receipt_data):
    if not APPLE_SHARED_SECRET:
        return {"ok": False, "reason": "apple_not_configured"}

    payload = {
        "receipt-data": receipt_data,
        "password": APPLE_SHARED_SECRET,
        "exclude-old-transactions": True,
    }
    response = requests.post(APPLE_VERIFY_URL, json=payload, timeout=20)
    if response.status_code != 200:
        return {"ok": False, "reason": "apple_api_error", "status": response.status_code}

    body = response.json()
    status = body.get("status")
    if status == 21007:
        response = requests.post(APPLE_SANDBOX_URL, json=payload, timeout=20)
        body = response.json()
        status = body.get("status")

    return {"ok": status == 0, "status": status, "raw": body}
