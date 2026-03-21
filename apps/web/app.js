const sampleChats = [
  { id: 1, name: "Nova Squad", preview: "We ship the beta tonight." },
  { id: 2, name: "SP NET Ops", preview: "Airdrop schedule updated." },
  { id: 3, name: "Design Crew", preview: "Assistant card styles ready." },
  { id: 4, name: "SPG Founders", preview: "Let’s finalize the premium perks." },
  { id: 5, name: "Launch Room", preview: "Countdown: T-5 days." },
];

const sampleMessages = [
  { id: 1, from: "them", text: "We need the assistant flow in the next build." },
  { id: 2, from: "you", text: "On it. I’ll wire the summary and smart reply UI." },
  { id: 3, from: "them", text: "Also add SPG ID panel to profile drawer." },
  { id: 4, from: "you", text: "Already in — we’ll surface it in the Settings tab too." },
];

const seedAssistantThread = [
  { id: 1, from: "assistant", text: "Hi! Want a summary of your last 50 messages?" },
  { id: 2, from: "you", text: "Summarize the Nova Squad chat." },
  { id: 3, from: "assistant", text: "Summary: Beta shipping tonight, assistant flow prioritized, SPG ID panel added." },
];

const seedPlans = [
  { id: "free", name: "Free", price: "$0", perks: ["Basic chat", "Limited assistant", "1 folder"] },
  { id: "plus", name: "Plus", price: "$4.99", perks: ["Unlimited assistant", "4 folders", "SPG badge"] },
  { id: "pro", name: "Pro", price: "$9.99", perks: ["Priority features", "8 folders", "Airdrop boosts"] },
];

const seedSpcoinTx = [
  { title: "Airdrop Claim", amount: "+2,000", date: "Today" },
  { title: "Premium Credit", amount: "-500", date: "Yesterday" },
  { title: "Referral Bonus", amount: "+980", date: "2 days ago" },
];

const seedGemsTx = [
  { title: "Gift Boost", amount: "-40", date: "Today" },
  { title: "Daily Mission", amount: "+8", date: "Yesterday" },
  { title: "Store Bundle", amount: "-120", date: "3 days ago" },
];

const envBackendUrl =
  typeof import.meta !== "undefined" && import.meta.env && import.meta.env.VITE_BACKEND_URL
    ? import.meta.env.VITE_BACKEND_URL
    : "";

const state = {
  backendUrl: localStorage.getItem("spg_backend_url") || envBackendUrl || "http://localhost:8790",
  token: localStorage.getItem("spg_token"),
  profile: null,
  premiumStatus: null,
  premiumPlans: [],
  wallet: null,
  assistantThread: [...seedAssistantThread],
};

const navItems = document.querySelectorAll(".nav-item");
const views = document.querySelectorAll(".view");

const elements = {
  chatList: document.getElementById("chatList"),
  chatMessages: document.getElementById("chatMessages"),
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
  gemsTx: document.getElementById("gemsTx"),
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
};

navItems.forEach((item) => {
  item.addEventListener("click", () => {
    navItems.forEach((btn) => btn.classList.remove("active"));
    item.classList.add("active");

    const view = item.getAttribute("data-view");
    views.forEach((section) => {
      section.classList.toggle("hidden", section.getAttribute("data-view") !== view);
    });
  });
});

function setAuthStatus(text, ok = false) {
  if (elements.authStatus) {
    elements.authStatus.textContent = text;
    elements.authStatus.style.color = ok ? "var(--accent)" : "var(--muted)";
  }
  if (elements.accountStatus) {
    elements.accountStatus.textContent = text;
    elements.accountStatus.style.color = ok ? "var(--accent)" : "var(--muted)";
  }
}

function renderChats() {
  elements.chatList.innerHTML = "";
  sampleChats.forEach((chat, index) => {
    const card = document.createElement("div");
    card.className = `chat-item ${index === 0 ? "active" : ""}`;
    card.innerHTML = `
      <div class="title">${chat.name}</div>
      <div class="preview">${chat.preview}</div>
    `;
    elements.chatList.appendChild(card);
  });
}

function renderMessages() {
  elements.chatMessages.innerHTML = "";
  sampleMessages.forEach((msg) => {
    const bubble = document.createElement("div");
    bubble.className = `message ${msg.from === "you" ? "you" : ""}`;
    bubble.textContent = msg.text;
    elements.chatMessages.appendChild(bubble);
  });
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
  const current = state.premiumStatus?.planId || "free";
  elements.premiumPlans.innerHTML = "";
  plans.forEach((plan) => {
    const card = document.createElement("div");
    card.className = "plan-card";
    const isCurrent = plan.id === current;
    const perks = Array.isArray(plan.perks) ? plan.perks : [];
    const priceLabel = typeof plan.price === "number" ? `$${plan.price}` : plan.price || "";
    card.innerHTML = `
      <h3>${plan.name}</h3>
      <div class="price">${priceLabel}</div>
      <div>${perks.join(" · ")}</div>
      <button class="ghost" ${isCurrent ? "disabled" : ""} data-plan="${plan.id}">
        ${isCurrent ? "Current" : "Activate (dev)"}
      </button>
    `;
    const btn = card.querySelector("button");
    if (btn) {
      btn.addEventListener("click", () => activatePlan(plan.id));
    }
    elements.premiumPlans.appendChild(card);
  });
  if (elements.premiumStatus) {
    elements.premiumStatus.textContent = `Plan: ${current.toUpperCase()}`;
  }
}

function renderTransactions(listElement, items) {
  listElement.innerHTML = "";
  items.forEach((tx) => {
    const row = document.createElement("div");
    row.className = "tx-item";
    row.innerHTML = `
      <div>
        <div>${tx.title || tx.description}</div>
        <div class="label">${tx.date || formatTimestamp(tx.created_at)}</div>
      </div>
      <div>${formatAmount(tx.amount, tx.currency)}</div>
    `;
    listElement.appendChild(row);
  });
}

function formatAmount(amount, currency) {
  if (amount === undefined || amount === null) return "";
  const sign = amount > 0 ? "+" : "";
  const cur = currency ? ` ${currency}` : "";
  return `${sign}${amount}${cur}`;
}

function formatTimestamp(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString();
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
    if (!email || !password) {
      setAuthStatus("Enter email + password", false);
      return;
    }
    const data = await apiFetch("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    state.token = data.token;
    localStorage.setItem("spg_token", data.token);
    await refreshProfile();
    await refreshWallet();
    await refreshPremium();
    setAuthStatus("Connected", true);
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
    await apiFetch("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ displayName, email, password }),
    });
    setAuthStatus("Registered. You can login now.", true);
  } catch (error) {
    setAuthStatus(error.message || "Register failed", false);
  }
}

function logout() {
  state.token = null;
  localStorage.removeItem("spg_token");
  state.profile = null;
  setAuthStatus("Disconnected", false);
  updateProfileUI();
  renderPlans();
  renderTransactions(elements.spcoinTx, seedSpcoinTx);
  renderTransactions(elements.gemsTx, seedGemsTx);
}

async function refreshProfile() {
  if (!state.token) {
    setAuthStatus("Disconnected", false);
    return;
  }
  try {
    const profile = await apiFetch("/api/profile");
    state.profile = profile;
    updateProfileUI();
    setAuthStatus("Connected", true);
  } catch (error) {
    logout();
  }
}

function updateProfileUI() {
  if (!state.profile) {
    elements.profileName.textContent = "—";
    elements.profileEmail.textContent = "—";
    elements.spgBadge.textContent = "SPG-UNMINTED";
    return;
  }
  elements.profileName.textContent = state.profile.displayName || "—";
  elements.profileEmail.textContent = state.profile.email || "—";
  elements.spgBadge.textContent = state.profile.spgId || "SPG-UNMINTED";
}

async function mintSpgId() {
  if (!state.token) {
    setAuthStatus("Login required to mint SPG ID", false);
    return;
  }
  try {
    const data = await apiFetch("/api/profile/spg-id/mint", { method: "POST" });
    if (state.profile) {
      state.profile.spgId = data.spgId;
    }
    updateProfileUI();
  } catch (error) {
    setAuthStatus(error.message || "SPG mint failed", false);
  }
}

async function refreshWallet() {
  if (!state.token) return;
  try {
    const wallet = await apiFetch("/api/wallet");
    state.wallet = wallet;
    elements.spCoinBalance.textContent = `${wallet.spCoin} SP`;
    elements.gemsBalance.textContent = `${wallet.gems} Gems`;
    renderTransactions(elements.spcoinTx, wallet.history || []);
    renderTransactions(elements.gemsTx, wallet.history || []);
    updateAirdropTimer(wallet.airdrop);
  } catch (error) {
    setAuthStatus(error.message || "Wallet failed", false);
  }
}

let airdropInterval = null;

function updateAirdropTimer(airdrop) {
  if (!elements.airdropTimer) return;
  if (airdropInterval) {
    clearInterval(airdropInterval);
  }
  if (!airdrop) {
    elements.airdropTimer.textContent = "Unavailable";
    return;
  }
  if (airdrop.canClaim) {
    elements.airdropTimer.textContent = "Available now";
    return;
  }
  const next = new Date(airdrop.nextClaimAt);
  const tick = () => {
    const remaining = Math.max(0, next - Date.now());
    const hours = Math.floor(remaining / 3600000);
    const minutes = Math.floor((remaining % 3600000) / 60000);
    const seconds = Math.floor((remaining % 60000) / 1000);
    elements.airdropTimer.textContent = `in ${hours.toString().padStart(2, "0")}:${minutes
      .toString()
      .padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
  };
  tick();
  airdropInterval = setInterval(tick, 1000);
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
      state.token ? apiFetch("/api/premium/status") : Promise.resolve({ planId: "free" }),
    ]);
    state.premiumPlans = plans.plans || [];
    state.premiumStatus = status;
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
      body: JSON.stringify({ planId, platform: "web", receipt: "dev" }),
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
    const payload = {
      intent: intent || "general",
      messages: state.assistantThread.map((msg) => ({
        role: msg.from === "you" ? "user" : "assistant",
        content: msg.text,
      })),
    };
    const response = await apiFetch("/api/assistant/chat", {
      method: "POST",
      body: JSON.stringify(payload),
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
    renderAssistant();
  } catch (error) {
    state.assistantThread.push({
      id: Date.now() + 3,
      from: "assistant",
      text: "Assistant offline. Check backend.",
    });
    renderAssistant();
  }
}

function bindEvents() {
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
    if (event.key === "Enter") {
      sendAssistantMessage();
    }
  });

  document.querySelectorAll(".tool").forEach((tool) => {
    tool.addEventListener("click", () => {
      const intent = tool.dataset.intent || "general";
      sendAssistantMessage(intent);
    });
  });
}

function init() {
  renderChats();
  renderMessages();
  renderAssistant();
  renderPlans();
  renderTransactions(elements.spcoinTx, seedSpcoinTx);
  renderTransactions(elements.gemsTx, seedGemsTx);
  if (elements.backendUrl) {
    elements.backendUrl.value = state.backendUrl;
  }
  bindEvents();
  if (state.token) {
    refreshProfile().then(() => {
      refreshWallet();
      refreshPremium();
    });
  } else {
    setAuthStatus("Disconnected", false);
  }
}

init();
