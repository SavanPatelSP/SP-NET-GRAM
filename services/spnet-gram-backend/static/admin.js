const state = {
  token: localStorage.getItem('spnet_token') || '',
  role: null,
};

const loginStatus = document.getElementById('login-status');
const createStatus = document.getElementById('create-status');
const licensesStatus = document.getElementById('licenses-status');
const logoutBtn = document.getElementById('logout-btn');

function setStatus(el, msg, isError = false) {
  if (!el) return;
  el.textContent = msg;
  el.style.color = isError ? '#f87171' : '#94a3b8';
}

function headers() {
  return {
    'Content-Type': 'application/json',
    ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
  };
}

async function api(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: { ...headers(), ...(options.headers || {}) },
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error || 'Request failed');
  }
  return data;
}

async function login() {
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value.trim();
  if (!email || !password) {
    setStatus(loginStatus, 'Enter email and password.', true);
    return;
  }
  try {
    setStatus(loginStatus, 'Signing in...');
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    state.token = data.token;
    localStorage.setItem('spnet_token', data.token);
    await loadMe();
    setStatus(loginStatus, 'Signed in successfully.');
  } catch (err) {
    setStatus(loginStatus, err.message, true);
  }
}

async function loadMe() {
  if (!state.token) {
    state.role = null;
    logoutBtn.disabled = true;
    return;
  }
  try {
    const me = await api('/api/auth/me');
    state.role = me.role;
    logoutBtn.disabled = false;
    if (!['manager', 'admin'].includes(state.role)) {
      setStatus(loginStatus, 'Logged in, but not a manager/admin.', true);
      return;
    }
    await loadLicenses();
  } catch (err) {
    setStatus(loginStatus, err.message, true);
  }
}

async function logout() {
  state.token = '';
  state.role = null;
  localStorage.removeItem('spnet_token');
  logoutBtn.disabled = true;
  document.getElementById('licenses-body').innerHTML = '';
  setStatus(loginStatus, 'Signed out.');
}

function renderCreated(keys) {
  const wrap = document.getElementById('created-keys');
  wrap.innerHTML = '';
  keys.forEach((key) => {
    const span = document.createElement('span');
    span.innerHTML = `<strong>${key.licenseKey}</strong> <button class="action-btn" data-key="${key.licenseKey}">Copy</button>`;
    span.querySelector('button').addEventListener('click', () => {
      navigator.clipboard.writeText(key.licenseKey);
    });
    wrap.appendChild(span);
  });
}

async function createLicenses() {
  if (!['manager', 'admin'].includes(state.role)) {
    setStatus(createStatus, 'Login as manager/admin to create licenses.', true);
    return;
  }
  const payload = {
    planId: document.getElementById('planId').value,
    count: Number(document.getElementById('count').value || 1),
    maxUses: Number(document.getElementById('maxUses').value || 1),
    durationDays: document.getElementById('durationDays').value || null,
    expiresAt: document.getElementById('expiresAt').value || null,
    notes: document.getElementById('notes').value || null,
  };
  if (payload.durationDays === '') payload.durationDays = null;
  try {
    setStatus(createStatus, 'Generating keys...');
    const data = await api('/api/admin/licenses/create', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    renderCreated(data.created || []);
    setStatus(createStatus, `Created ${data.created.length} key(s).`);
    await loadLicenses();
  } catch (err) {
    setStatus(createStatus, err.message, true);
  }
}

async function loadLicenses() {
  if (!['manager', 'admin'].includes(state.role)) {
    return;
  }
  try {
    setStatus(licensesStatus, 'Loading licenses...');
    const data = await api('/api/admin/licenses?limit=200');
    const tbody = document.getElementById('licenses-body');
    tbody.innerHTML = '';
    data.licenses.forEach((lic) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${lic.license_key}</td>
        <td>${lic.plan_id}</td>
        <td>${lic.status}</td>
        <td>${lic.uses}/${lic.max_uses}</td>
        <td>${lic.expires_at || '—'}</td>
        <td>${lic.created_at}</td>
        <td>
          <button class="action-btn" data-copy="${lic.license_key}">Copy</button>
          <button class="action-btn danger" data-revoke="${lic.license_key}">Revoke</button>
        </td>
      `;
      tbody.appendChild(tr);
    });
    tbody.querySelectorAll('[data-copy]').forEach((btn) => {
      btn.addEventListener('click', () => navigator.clipboard.writeText(btn.dataset.copy));
    });
    tbody.querySelectorAll('[data-revoke]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        if (!confirm('Revoke this license?')) return;
        try {
          await api('/api/admin/licenses/revoke', {
            method: 'POST',
            body: JSON.stringify({ licenseKey: btn.dataset.revoke }),
          });
          await loadLicenses();
        } catch (err) {
          setStatus(licensesStatus, err.message, true);
        }
      });
    });
    setStatus(licensesStatus, `Loaded ${data.licenses.length} license(s).`);
  } catch (err) {
    setStatus(licensesStatus, err.message, true);
  }
}

if (document.getElementById('login-btn')) {
  document.getElementById('login-btn').addEventListener('click', login);
  logoutBtn.addEventListener('click', logout);
  document.getElementById('create-btn').addEventListener('click', createLicenses);
  document.getElementById('refresh-btn').addEventListener('click', loadLicenses);
}

loadMe();
