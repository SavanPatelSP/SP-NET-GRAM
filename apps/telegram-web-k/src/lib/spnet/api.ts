const DEFAULT_BACKEND_URL = 'https://spnet-gram-backend.onrender.com';
const RAW_BACKEND_URL =
  import.meta.env.VITE_SPNET_BACKEND_URL ||
  (import.meta as any).env?.VITE_BACKEND_URL ||
  DEFAULT_BACKEND_URL;

const resolveBackendUrl = () => {
  if(typeof window !== 'undefined' && window.location && window.location.origin) {
    const host = window.location.hostname || '';
    if(host.endsWith('spnet.in')) {
      return window.location.origin;
    }
  }
  return RAW_BACKEND_URL;
};

const BACKEND_URL = resolveBackendUrl().replace(/\/$/, '');
const TOKEN_KEY = 'spnet_token';
const EMAIL_KEY = 'spnet_email';

type RequestOptions = {
  method?: string;
  body?: Record<string, unknown>;
  auth?: boolean;
};

export type PremiumPlan = {
  id: string;
  name: string;
  price: number;
  productIds?: {android?: string | null; ios?: string | null};
  perks?: string[];
};

export type PremiumStatus = {
  planId: string;
  status: string;
  platform?: string | null;
  expiresAt?: string | null;
  updatedAt?: string | null;
  receiptStored?: boolean;
};

export type AccessStatus = {
  canUse: boolean;
  reason?: string;
  premium?: PremiumStatus;
  requireLicense?: boolean;
};

export type AirdropStatus = {
  lastClaimAt?: string | null;
  nextClaimAt?: string | null;
  canClaim?: boolean;
  cooldownHours?: number;
};

export type WalletStatus = {
  spCoin: number;
  gems: number;
  history: Array<{amount: number; currency: string; description: string; created_at?: string; createdAt?: string}>;
  airdrop?: AirdropStatus;
  gemsStatus?: AirdropStatus;
};

export function getBackendUrl() {
  return BACKEND_URL;
}

export function getSpNetToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function setSpNetToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearSpNetToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getSpNetEmail() {
  return localStorage.getItem(EMAIL_KEY) || '';
}

export function setSpNetEmail(email: string) {
  if(email) {
    localStorage.setItem(EMAIL_KEY, email);
  }
}

async function request<T = any>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  };

  if(options.auth) {
    const token = getSpNetToken();
    if(token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${BACKEND_URL}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const text = await response.text();
  let data: any = {};
  if(text) {
    try {
      data = JSON.parse(text);
    } catch{
      data = {};
    }
  }

  if(!response.ok) {
    const errorMessage = data?.error || data?.message || response.statusText || 'Request failed';
    const error: any = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data as T;
}

export function login(email: string, password: string) {
  return request<{token: string; access?: {canUse?: boolean}}>(
    '/api/auth/login',
    {method: 'POST', body: {email, password}}
  );
}

export function register(email: string, password: string, displayName: string) {
  return request<{ok: boolean}>(
    '/api/auth/register',
    {method: 'POST', body: {email, password, displayName}}
  );
}

export function accessStatus() {
  return request<AccessStatus>(
    '/api/access/status',
    {auth: true}
  );
}

export function redeemLicense(licenseKey: string) {
  return request<{ok: boolean; planId?: string; expiresAt?: string}>(
    '/api/license/redeem',
    {method: 'POST', auth: true, body: {licenseKey}}
  );
}

export function getProfile() {
  return request<{id: number; email: string; displayName: string; spgId?: string}>(
    '/api/profile',
    {auth: true}
  );
}

export function mintSpgId() {
  return request<{spgId: string; status: string}>(
    '/api/profile/spg-id/mint',
    {method: 'POST', auth: true}
  );
}

export function premiumStatus() {
  return request<{premium: PremiumStatus; access: AccessStatus}>(
    '/api/premium/status',
    {auth: true}
  );
}

export function premiumPlans() {
  return request<{plans: PremiumPlan[]}>('/api/premium/plans');
}

export function subscribePremium(planId: string, platform: string, receipt?: string) {
  return request<PremiumStatus>(
    '/api/premium/subscribe',
    {method: 'POST', auth: true, body: {planId, platform, receipt}}
  );
}

export function walletStatus() {
  return request<WalletStatus>(
    '/api/wallet',
    {auth: true}
  );
}

export function claimAirdrop() {
  return request<{spCoin: number; claimed: number}>(
    '/api/wallet/airdrop/claim',
    {method: 'POST', auth: true}
  );
}

export function claimGems() {
  return request<{gems: number; claimed: number}>(
    '/api/wallet/gems/claim',
    {method: 'POST', auth: true}
  );
}

export function assistantChat(messages: Array<{role: string; content: string}>, intent = 'general') {
  return request<{reply: string; suggestions?: string[]}>(
    '/api/assistant/chat',
    {method: 'POST', auth: true, body: {messages, intent}}
  );
}
