import { MtprotoClient } from "./telegram/mtproto_client.js";

const envBackendUrl =
  typeof import.meta !== "undefined" && import.meta.env && import.meta.env.VITE_BACKEND_URL
    ? import.meta.env.VITE_BACKEND_URL
    : "";
const envTgApiId =
  typeof import.meta !== "undefined" && import.meta.env && import.meta.env.VITE_TG_API_ID
    ? Number(import.meta.env.VITE_TG_API_ID)
    : 0;
const envTgApiHash =
  typeof import.meta !== "undefined" && import.meta.env && import.meta.env.VITE_TG_API_HASH
    ? import.meta.env.VITE_TG_API_HASH
    : "";

const seedAssistantThread = [
  {
    id: 1,
    from: "assistant",
    text: "Connect Telegram, open a dialog, then ask me for summaries, replies, translations, or action items.",
  },
];

const seedPlans = [
  { id: "free", name: "Free", price: "$0", perks: ["Telegram client beta", "Limited assistant", "SPG profile"] },
  { id: "plus", name: "Plus", price: "$4.99", perks: ["Unlimited assistant", "SPG badge", "More folders"] },
  { id: "pro", name: "Pro", price: "$9.99", perks: ["Priority tools", "Airdrop boosts", "Power-user controls"] },
];

const state = {
  backendUrl: localStorage.getItem("spg_backend_url") || envBackendUrl || "http://localhost:8790",
  token: localStorage.getItem("spg_token"),
  profile: null,
  premiumStatus: null,
  premiumPlans: [],
  wallet: null,
  assistantThread: [...seedAssistantThread],
  tgApiId: Number(localStorage.getItem("spg_tg_api_id") || envTgApiId || 0),
  tgApiHash: localStorage.getItem("spg_tg_api_hash") || envTgApiHash || "",
  tgClient: null,
  tgUser: null,
  tgChats: [],
  tgMessages: new Map(),
  tgPhoneCodeHash: null,
  activeChatId: null,
  chatFilter: "",
};

const navItems = document.querySelectorAll(".nav-item");
const views = document.querySelectorAll(".view");

const elements = {
  chatList: document.getElementById("chatList"),
  chatMessages: document.getElementById("chatMessages"),
  chatSearch: document.getElementById("chatSearch"),
  threadName: document.getElementById("threadName"),
  threadMeta: document.getElementById("threadMeta"),
  composerInput: document.getElementById("composerInput"),
  messageSendBtn: document.getElementById("messageSendBtn"),
  summarizeThreadBtn: document.getElementById("summarizeThreadBtn"),
  assistantThread: document.getElementById("assistantThread"),
  assistantInput: document.getElementById("assistantInput"),
  assistantSend: document.getElementById("assistantSend"),
  mintSpgBtn: document.getElementById("mintSpgBtn"),
  spgBadge: document.getElementById("spgBadge"),
  spgHandle: document.getElementById("spgHandle"),
  spgRank: document.getElementById("spgRank"),
  spgBadges: document.getElementById("spgBadges"),
  premiumPlans: document.getElementById("premiumPlans"),
  premiumStatus: document.getElementById("premiumStatus"),
  refreshPremiumBtn: document.getElementById("refreshPremiumBtn"),
  spCoinBalance: document.getElementById("spCoinBalance"),
  gemsBalance: document.getElementById("gemsBalance"),
  spcoinTx: document.getElementById("spcoinTx"),
  airdropTimer: document.getElementById("airdropTimer"),
  airdropClaimBtn: document.getElementById("airdropClaimBtn"),
  authStatus: document.getElementById("authStatus"),
  accountStatus: document.getElementById("accountStatus"),
  loginEmail: document.getElementById("loginEmail"),
  loginPassword: document.getElementById("loginPassword"),
  loginBtn: document.getElementById("loginBtn"),
  registerName: document.getElementById("registerName"),
  registerEmail: document.getElementById("registerEmail"),
  registerPassword: document.getElementById("registerPassword"),
  registerBtn: document.getElementById("registerBtn"),
  profileName: document.getElementById("profileName"),
  profileEmail: document.getElementById("profileEmail"),
  logoutBtn: document.getElementById("logoutBtn"),
  backendUrl: document.getElementById("backendUrl"),
  saveBackendBtn: document.getElementById("saveBackendBtn"),
  tgStatus: document.getElementById("tgStatus"),
  tgNavStatus: document.getElementById("tgNavStatus"),
  tgHint: document.getElementById("tgHint"),
  tgApiId: document.getElementById("tgApiId"),
  tgApiHash: document.getElementById("tgApiHash"),
  tgSaveConfigBtn: document.getElementById("tgSaveConfigBtn"),
  tgPhone: document.getElementById("tgPhone"),
  tgSendCodeBtn: document.getElementById("tgSendCodeBtn"),
  tgCode: document.getElementById("tgCode"),
  tgSignInBtn: document.getElementById("tgSignInBtn"),
  tgPassword: document.getElementById("tgPassword"),
  tgPasswordBtn: document.getElementById("tgPasswordBtn"),
  tgReloadChatsBtn: document.getElementById("tgReloadChatsBtn"),
  tgLogoutBtn: document.getElementById("tgLogoutBtn"),
  issueText: document.getElementById("issueText"),
  issueSend: document.getElementById("issueSend"),
  issueStatus: document.getElementById("issueStatus"),
};

navItems.forEach((item) => {
  item.addEventListener("click", () => activateView(item.getAttribute("data-view")));
});

function activateView(view) {
  navItems.forEach((btn) => btn.classList.toggle("active", btn.getAttribute("data-view") === view));
  views.forEach((section) => {
    section.classList.toggle("hidden", section.getAttribute("data-view") !== view);
  });
  logClientEvent("feature.view", `View ${view}`, { view });
}

function setAuthStatus(text, ok = false) {
  for (const el of [elements.authStatus, elements.accountStatus]) {
    if (!el) continue;
    el.textContent = text;
    el.style.color = ok ? "var(--accent)" : "var(--muted)";
  }
}

function setTelegramStatus(text, ok = false) {
  for (const el of [elements.tgStatus, elements.tgNavStatus]) {
    if (!el) continue;
    el.textContent = text;
    el.style.color = ok ? "var(--accent)" : "var(--muted)";
  }
}

function setTelegramHint(text, ok = false) {
  if (!elements.tgHint) return;
  elements.tgHint.textContent = text;
  elements.tgHint.style.color = ok ? "var(--accent)" : "var(--muted)";
}

function setIssueStatus(text, ok = false) {
  if (!elements.issueStatus) return;
  elements.issueStatus.textContent = text;
  elements.issueStatus.style.color = ok ? "var(--accent)" : "var(--muted)";
}

function saveTelegramConfig() {
  const apiId = Number(elements.tgApiId.value || 0);
  const apiHash = elements.tgApiHash.value.trim();
  if (!apiId || !apiHash) {
    setTelegramHint("Enter a Telegram API ID and API hash first.", false);
    return false;
  }
  state.tgApiId = apiId;
  state.tgApiHash = apiHash;
  localStorage.setItem("spg_tg_api_id", String(apiId));
  localStorage.setItem("spg_tg_api_hash", apiHash);
  state.tgClient = null;
  setTelegramHint("Telegram API config saved in this browser.", true);
  return true;
}

async function ensureTelegramClient() {
  if (!state.tgApiId || !state.tgApiHash) {
    if (!saveTelegramConfig()) {
      throw new Error("Missing Telegram API config");
    }
  }
  if (!state.tgClient) {
    state.tgClient = new MtprotoClient({
      apiId: state.tgApiId,
      apiHash: state.tgApiHash,
    });
  }
  return state.tgClient;
}

async function restoreTelegramSession() {
  if (!state.tgApiId || !state.tgApiHash) {
    setTelegramStatus("Telegram config needed", false);
    renderTelegramEmpty();
    return;
  }
  try {
    const client = await ensureTelegramClient();
    const authorized = await client.isAuthorized();
    if (!authorized) {
      setTelegramStatus("Telegram login needed", false);
      renderTelegramEmpty();
      return;
    }
    state.tgUser = await client.getMe();
    setTelegramStatus(`Connected: ${state.tgUser.displayName}`, true);
    await loadTelegramChats();
  } catch (error) {
    setTelegramStatus("Telegram login needed", false);
    setTelegramHint(error.message || "Could not restore Telegram session.", false);
    renderTelegramEmpty();
  }
}

async function sendTelegramCode() {
  if (!saveTelegramConfig()) return;
  const phone = elements.tgPhone.value.trim();
  if (!phone) {
    setTelegramHint("Enter your Telegram phone number.", false);
    return;
  }
  try {
    const client = await ensureTelegramClient();
    const result = await client.sendCode(phone);
    state.tgPhoneCodeHash = result.phoneCodeHash;
    setTelegramStatus("Code sent", true);
    setTelegramHint(result.isCodeViaApp ? "Open Telegram and enter the app code." : "Enter the SMS code.", true);
  } catch (error) {
    setTelegramStatus("Code failed", false);
    setTelegramHint(error.message || "Telegram could not send a code.", false);
  }
}

async function signInTelegram() {
  const phone = elements.tgPhone.value.trim();
  const code = elements.tgCode.value.trim();
  if (!phone || !code || !state.tgPhoneCodeHash) {
    setTelegramHint("Send a code first, then enter the code.", false);
    return;
  }
  try {
    const client = await ensureTelegramClient();
    state.tgUser = await client.signIn({
      phone,
      code,
      phoneCodeHash: state.tgPhoneCodeHash,
    });
    setTelegramStatus(`Connected: ${state.tgUser.displayName || "Telegram"}`, true);
    setTelegramHint("Telegram session saved in this browser.", true);
    await loadTelegramChats();
  } catch (error) {
    if (error.errorMessage === "SESSION_PASSWORD_NEEDED" || String(error.message).includes("SESSION_PASSWORD_NEEDED")) {
      setTelegramStatus("2FA required", false);
      setTelegramHint("Enter your Telegram 2FA password and press Submit 2FA.", false);
      return;
    }
    setTelegramStatus("Sign in failed", false);
    setTelegramHint(error.message || "Telegram sign in failed.", false);
  }
}

async function signInTelegramPassword() {
  const password = elements.tgPassword.value;
  if (!password) {
    setTelegramHint("Enter your Telegram 2FA password.", false);
    return;
  }
  try {
    const client = await ensureTelegramClient();
    state.tgUser = await client.signInWithPassword(password);
    elements.tgPassword.value = "";
    setTelegramStatus(`Connected: ${state.tgUser.displayName || "Telegram"}`, true);
    setTelegramHint("Telegram 2FA accepted. Session saved.", true);
    await loadTelegramChats();
  } catch (error) {
    setTelegramStatus("2FA failed", false);
    setTelegramHint(error.message || "Telegram 2FA failed.", false);
  }
}

async function logoutTelegram() {
  if (state.tgClient) {
    await state.tgClient.logout();
  }
  state.tgClient = null;
  state.tgUser = null;
  state.tgChats = [];
  state.tgMessages.clear();
  state.tgPhoneCodeHash = null;
  state.activeChatId = null;
  setTelegramStatus("Telegram offline", false);
  setTelegramHint("Telegram session cleared from this browser.", true);
  renderTelegramEmpty();
}

async function loadTelegramChats() {
  try {
    const client = await ensureTelegramClient();
    setTelegramStatus("Loading dialogs...", true);
    state.tgChats = await client.getChats();
    state.activeChatId = state.tgChats[0]?.id || null;
    renderChats();
    if (state.activeChatId) {
      await loadTelegramMessages(state.activeChatId);
    } else {
      renderMessages();
    }
    setTelegramStatus(`Connected: ${state.tgUser?.displayName || "Telegram"}`, true);
  } catch (error) {
    setTelegramStatus("Load failed", false);
    setTelegramHint(error.message || "Could not load Telegram dialogs.", false);
  }
}

async function loadTelegramMessages(chatId) {
  if (!chatId) return;
  try {
    const client = await ensureTelegramClient();
    const messages = await client.getMessages(chatId);
    state.tgMessages.set(chatId.toString(), messages);
    renderMessages();
  } catch (error) {
    setTelegramHint(error.message || "Could not load messages.", false);
  }
}

function renderChats() {
  elements.chatList.innerHTML = "";
  const filter = state.chatFilter.toLowerCase();
  const chats = state.tgChats.filter((chat) => chat.title.toLowerCase().includes(filter));
  if (!chats.length) {
    elements.chatList.innerHTML = `<div class="empty-state">No Telegram dialogs loaded yet.</div>`;
    return;
  }
  chats.forEach((chat) => {
    const card = document.createElement("button");
    card.className = `chat-item ${chat.id === state.activeChatId ? "active" : ""}`;
    card.type = "button";
    card.innerHTML = `
      <div class="title">${escapeHtml(chat.title)}</div>
      <div class="preview">${escapeHtml(chat.lastMessage || chat.kind || "")}</div>
      ${chat.unreadCount ? `<div class="unread">${chat.unreadCount}</div>` : ""}
    `;
    card.addEventListener("click", async () => {
      state.activeChatId = chat.id;
      renderChats();
      await loadTelegramMessages(chat.id);
    });
    elements.chatList.appendChild(card);
  });
}

function renderMessages() {
  elements.chatMessages.innerHTML = "";
  const active = state.tgChats.find((chat) => chat.id === state.activeChatId);
  if (active) {
    elements.threadName.textContent = active.title;
    elements.threadMeta.textContent = `${active.kind} via Telegram API`;
  }
  if (!state.activeChatId) {
    renderTelegramEmpty();
    return;
  }
  const messages = state.tgMessages.get(state.activeChatId.toString()) || [];
  if (!messages.length) {
    elements.chatMessages.innerHTML = `<div class="empty-state">No messages loaded for this dialog.</div>`;
    return;
  }
  messages.forEach((msg) => {
    const bubble = document.createElement("div");
    bubble.className = `message ${msg.from === "you" ? "you" : ""}`;
    bubble.innerHTML = `
      <div>${escapeHtml(msg.text || "[Unsupported message]")}</div>
      <time>${formatTimestamp(msg.timestamp)}</time>
    `;
    elements.chatMessages.appendChild(bubble);
  });
  elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

function renderTelegramEmpty() {
  elements.threadName.textContent = "No Telegram chat selected";
  elements.threadMeta.textContent = "Connect Telegram to load dialogs.";
  elements.chatMessages.innerHTML = `<div class="empty-state">This is now a real Telegram API client. Add API credentials, send a code, sign in, and your dialogs will appear here.</div>`;
  renderChats();
}

async function sendTelegramMessage() {
  const text = elements.composerInput.value.trim();
  if (!text || !state.activeChatId) {
    setTelegramHint("Select a Telegram chat and enter a message.", false);
    return;
  }
  try {
    const client = await ensureTelegramClient();
    await client.sendMessage(state.activeChatId, text);
    elements.composerInput.value = "";
    await loadTelegramMessages(state.activeChatId);
    setTelegramHint("Telegram message sent.", true);
  } catch (error) {
    setTelegramHint(error.message || "Could not send Telegram message.", false);
  }
}

function summarizeCurrentThread() {
  const active = state.tgChats.find((chat) => chat.id === state.activeChatId);
  const messages = state.activeChatId ? state.tgMessages.get(state.activeChatId.toString()) || [] : [];
  if (!active || !messages.length) {
    setTelegramHint("Open a Telegram dialog before summarizing.", false);
    return;
  }
  const excerpt = messages.slice(-20).map((msg) => `${msg.from}: ${msg.text}`).join("\n");
  elements.assistantInput.value = `Summarize this Telegram chat (${active.title}):\n${excerpt}`;
  activateView("assistant");
}

function renderAssistant() {
  elements.assistantThread.innerHTML = "";
  state.assistantThread.forEach((msg) => {
    const bubble = document.createElement("div");
    bubble.className = `message ${msg.from === "you" ? "you" : ""}`;
    bubble.textContent = msg.text;
    elements.assistantThread.appendChild(bubble);
  });
  elements.assistantThread.scrollTop = elements.assistantThread.scrollHeight;
}

function renderPlans() {
  const plans = state.premiumPlans.length ? state.premiumPlans : seedPlans;
  const current = state.premiumStatus?.planId || state.premiumStatus?.premium?.planId || "free";
  elements.premiumPlans.innerHTML = "";
  plans.forEach((plan) => {
    const card = document.createElement("div");
    card.className = "plan-card";
    const isCurrent = plan.id === current;
    const priceLabel = typeof plan.price === "number" ? `$${plan.price}` : plan.price || "";
    card.innerHTML = `
      <h3>${escapeHtml(plan.name)}</h3>
      <div class="price">${escapeHtml(priceLabel)}</div>
      <div class="muted">${escapeHtml((plan.perks || []).join(" - "))}</div>
      <button class="ghost" ${isCurrent ? "disabled" : ""} data-plan="${plan.id}">
        ${isCurrent ? "Current" : "Activate beta"}
      </button>
    `;
    const btn = card.querySelector("button");
    btn?.addEventListener("click", () => activatePlan(plan.id));
    elements.premiumPlans.appendChild(card);
  });
  elements.premiumStatus.textContent = `Plan: ${current.toUpperCase()}`;
}

function renderWallet(wallet = state.wallet) {
  if (!wallet) {
    elements.spCoinBalance.textContent = "0 SP";
    elements.gemsBalance.textContent = "0 Gems";
    elements.airdropTimer.textContent = "Login to check";
    elements.spcoinTx.innerHTML = `<div class="empty-state">Login with SP NET GRAM account to load wallet.</div>`;
    return;
  }
  elements.spCoinBalance.textContent = `${wallet.spCoin} SP`;
  elements.gemsBalance.textContent = `${wallet.gems} Gems`;
  elements.airdropTimer.textContent = wallet.airdrop?.canClaim ? "Available now" : formatNextClaim(wallet.airdrop?.nextClaimAt);
  renderTransactions(elements.spcoinTx, wallet.history || []);
}

function renderTransactions(listElement, items) {
  listElement.innerHTML = "";
  if (!items.length) {
    listElement.innerHTML = `<div class="empty-state">No wallet history yet.</div>`;
    return;
  }
  items.forEach((tx) => {
    const row = document.createElement("div");
    row.className = "tx-item";
    row.innerHTML = `
      <div>
        <div>${escapeHtml(tx.title || tx.description || "Transaction")}</div>
        <div class="label">${formatTimestamp(tx.date || tx.created_at)}</div>
      </div>
      <div>${escapeHtml(formatAmount(tx.amount, tx.currency))}</div>
    `;
    listElement.appendChild(row);
  });
}

function formatAmount(amount, currency) {
  if (amount === undefined || amount === null) return "";
  const sign = Number(amount) > 0 ? "+" : "";
  return `${sign}${amount}${currency ? ` ${currency}` : ""}`;
}

function formatTimestamp(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString();
}

function formatNextClaim(value) {
  if (!value) return "Unavailable";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unavailable";
  return `Next: ${date.toLocaleString()}`;
}

function setBackendUrl(value) {
  state.backendUrl = value;
  localStorage.setItem("spg_backend_url", value);
  if (elements.backendUrl) {
    elements.backendUrl.value = value;
  }
}

async function apiFetch(path, options = {}) {
  const url = `${state.backendUrl}${path}`;
  const headers = options.headers ? { ...options.headers } : {};
  headers["Content-Type"] = "application/json";
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: response.statusText }));
    throw new Error(error.error || "Request failed");
  }
  return response.json();
}

async function login() {
  try {
    const email = elements.loginEmail.value.trim();
    const password = elements.loginPassword.value.trim();
    const data = await apiFetch("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    state.token = data.token;
    localStorage.setItem("spg_token", data.token);
    await refreshProfile();
    await refreshWallet();
    await refreshPremium();
    setAuthStatus("SPG connected", true);
  } catch (error) {
    setAuthStatus(error.message || "Login failed", false);
  }
}

async function registerUser() {
  try {
    const displayName = elements.registerName.value.trim();
    const email = elements.registerEmail.value.trim();
    const password = elements.registerPassword.value.trim();
    if (!displayName || !email || !password) {
      setAuthStatus("Fill all register fields", false);
      return;
    }
    const data = await apiFetch("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ displayName, email, password }),
    });
    if (data.token) {
      state.token = data.token;
      localStorage.setItem("spg_token", data.token);
      await refreshProfile();
      await refreshWallet();
      await refreshPremium();
    }
    setAuthStatus("SPG connected", true);
  } catch (error) {
    setAuthStatus(error.message || "Register failed", false);
  }
}

function logout() {
  state.token = null;
  localStorage.removeItem("spg_token");
  state.profile = null;
  state.wallet = null;
  setAuthStatus("SPG disconnected", false);
  updateProfileUI();
  renderWallet(null);
  renderPlans();
}

async function refreshProfile() {
  if (!state.token) {
    setAuthStatus("SPG disconnected", false);
    return;
  }
  try {
    state.profile = await apiFetch("/api/profile");
    updateProfileUI();
    setAuthStatus("SPG connected", true);
  } catch (error) {
    logout();
  }
}

function updateProfileUI() {
  if (!state.profile) {
    elements.profileName.textContent = "-";
    elements.profileEmail.textContent = "-";
    elements.spgBadge.textContent = "SPG-UNMINTED";
    return;
  }
  elements.profileName.textContent = state.profile.displayName || "-";
  elements.profileEmail.textContent = state.profile.email || "-";
  elements.spgBadge.textContent = state.profile.spgId || "SPG-UNMINTED";
}

async function mintSpgId() {
  if (!state.token) {
    setAuthStatus("Login required to mint SPG ID", false);
    return;
  }
  try {
    const data = await apiFetch("/api/profile/spg-id/mint", { method: "POST" });
    state.profile = { ...state.profile, spgId: data.spgId };
    updateProfileUI();
  } catch (error) {
    setAuthStatus(error.message || "SPG mint failed", false);
  }
}

async function refreshWallet() {
  if (!state.token) return;
  try {
    state.wallet = await apiFetch("/api/wallet");
    renderWallet(state.wallet);
  } catch (error) {
    setAuthStatus(error.message || "Wallet failed", false);
  }
}

async function claimAirdrop() {
  if (!state.token) {
    setAuthStatus("Login required for airdrop", false);
    return;
  }
  try {
    await apiFetch("/api/wallet/airdrop/claim", { method: "POST" });
    await refreshWallet();
  } catch (error) {
    setAuthStatus(error.message || "Airdrop failed", false);
  }
}

async function refreshPremium() {
  try {
    const [plans, status] = await Promise.all([
      apiFetch("/api/premium/plans"),
      state.token ? apiFetch("/api/premium/status") : Promise.resolve({ premium: { planId: "free" } }),
    ]);
    state.premiumPlans = plans.plans || [];
    state.premiumStatus = status.premium || status;
    renderPlans();
  } catch (error) {
    renderPlans();
  }
}

async function activatePlan(planId) {
  if (!state.token) {
    setAuthStatus("Login required for premium", false);
    return;
  }
  try {
    await apiFetch("/api/premium/subscribe", {
      method: "POST",
      body: JSON.stringify({ planId, platform: "web", receipt: "beta" }),
    });
    await refreshPremium();
  } catch (error) {
    setAuthStatus(error.message || "Premium update failed", false);
  }
}

async function sendAssistantMessage(intent) {
  const content = elements.assistantInput.value.trim();
  if (!content && !intent) return;
  if (content) {
    state.assistantThread.push({ id: Date.now(), from: "you", text: content });
    elements.assistantInput.value = "";
  }
  renderAssistant();
  try {
    const response = await apiFetch("/api/assistant/chat", {
      method: "POST",
      body: JSON.stringify({
        intent: intent || "general",
        messages: state.assistantThread.map((msg) => ({
          role: msg.from === "you" ? "user" : "assistant",
          content: msg.text,
        })),
      }),
    });
    state.assistantThread.push({
      id: Date.now() + 1,
      from: "assistant",
      text: response.reply || "(no response)",
    });
    if (response.suggestions?.length) {
      state.assistantThread.push({
        id: Date.now() + 2,
        from: "assistant",
        text: `Suggestions: ${response.suggestions.join(", ")}`,
      });
    }
  } catch (error) {
    state.assistantThread.push({
      id: Date.now() + 3,
      from: "assistant",
      text: "Assistant needs SP NET GRAM login. Register or login in Settings.",
    });
  }
  renderAssistant();
}

function sendIssueReport() {
  const text = elements.issueText.value.trim();
  if (!text) {
    setIssueStatus("Describe the issue before sending.", false);
    return;
  }
  logClientEvent("issue.report", text, { userAgent: navigator.userAgent }, "warn");
  elements.issueText.value = "";
  setIssueStatus("Issue report sent.", true);
}

function logClientEvent(eventType, message, metadata = {}, level = "info") {
  if (!state.backendUrl) return;
  const headers = { "Content-Type": "application/json" };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  fetch(`${state.backendUrl}/api/logs/ingest`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      type: eventType,
      level,
      message,
      metadata: { app: "web", path: window.location.pathname, ...metadata },
    }),
    keepalive: true,
  }).catch(() => {});
}

function bindEvents() {
  elements.tgSaveConfigBtn.addEventListener("click", saveTelegramConfig);
  elements.tgSendCodeBtn.addEventListener("click", sendTelegramCode);
  elements.tgSignInBtn.addEventListener("click", signInTelegram);
  elements.tgPasswordBtn.addEventListener("click", signInTelegramPassword);
  elements.tgReloadChatsBtn.addEventListener("click", loadTelegramChats);
  elements.tgLogoutBtn.addEventListener("click", logoutTelegram);
  elements.messageSendBtn.addEventListener("click", sendTelegramMessage);
  elements.composerInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") sendTelegramMessage();
  });
  elements.chatSearch.addEventListener("input", () => {
    state.chatFilter = elements.chatSearch.value.trim();
    renderChats();
  });
  elements.summarizeThreadBtn.addEventListener("click", summarizeCurrentThread);

  elements.loginBtn.addEventListener("click", login);
  elements.registerBtn.addEventListener("click", registerUser);
  elements.logoutBtn.addEventListener("click", logout);
  elements.saveBackendBtn.addEventListener("click", () => {
    setBackendUrl(elements.backendUrl.value.trim());
    setAuthStatus("Backend updated", true);
  });
  elements.mintSpgBtn.addEventListener("click", mintSpgId);
  elements.refreshPremiumBtn.addEventListener("click", refreshPremium);
  elements.airdropClaimBtn.addEventListener("click", claimAirdrop);
  elements.assistantSend.addEventListener("click", () => sendAssistantMessage());
  elements.assistantInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") sendAssistantMessage();
  });
  elements.issueSend.addEventListener("click", sendIssueReport);

  document.querySelectorAll(".tool").forEach((tool) => {
    tool.addEventListener("click", () => sendAssistantMessage(tool.dataset.intent || "general"));
  });
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function init() {
  elements.backendUrl.value = state.backendUrl;
  elements.tgApiId.value = state.tgApiId || "";
  elements.tgApiHash.value = state.tgApiHash || "";
  bindEvents();
  renderAssistant();
  renderPlans();
  renderWallet(null);
  renderTelegramEmpty();
  restoreTelegramSession();
  if (state.token) {
    refreshProfile().then(() => {
      refreshWallet();
      refreshPremium();
    });
  } else {
    setAuthStatus("SPG disconnected", false);
  }
}

window.addEventListener("error", (event) => {
  logClientEvent(
    "client.error",
    event.message || "Client error",
    {
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno,
      stack: event.error?.stack,
    },
    "error",
  );
});

window.addEventListener("unhandledrejection", (event) => {
  const reason = event.reason;
  logClientEvent(
    "client.unhandledrejection",
    reason?.message || String(reason || "Unhandled rejection"),
    { stack: reason?.stack },
    "error",
  );
});

init();
