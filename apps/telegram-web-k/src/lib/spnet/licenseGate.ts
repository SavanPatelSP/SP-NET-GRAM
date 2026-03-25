import {toastNew} from '@components/toast';
import {LangPackKey, i18n} from '@lib/langPack';
import {
  accessStatus,
  clearSpNetToken,
  getSpNetEmail,
  getSpNetToken,
  login,
  requestPasswordReset,
  confirmPasswordReset,
  redeemLicense,
  register,
  setSpNetEmail,
  setSpNetToken
} from './api';

type GateOptions = {
  focus?: 'login' | 'license';
  lock?: boolean;
};

let gateRoot: HTMLDivElement | null = null;
let gateVisible = false;
let gateLocked = true;
let refreshTimer: number | null = null;
const ACCESS_REFRESH_INTERVAL = 15 * 60 * 1000;

let statusText: HTMLParagraphElement | null = null;
let emailInput: HTMLInputElement | null = null;
let passwordInput: HTMLInputElement | null = null;
let displayNameInput: HTMLInputElement | null = null;
let resetCodeInput: HTMLInputElement | null = null;
let licenseInput: HTMLInputElement | null = null;
let closeButton: HTMLButtonElement | null = null;

let loginButton: HTMLButtonElement | null = null;
let registerButton: HTMLButtonElement | null = null;
let resetButton: HTMLButtonElement | null = null;
let redeemButton: HTMLButtonElement | null = null;
let refreshButton: HTMLButtonElement | null = null;
let logoutButton: HTMLButtonElement | null = null;

const t = (key: LangPackKey) => (i18n(key) as HTMLElement).textContent || '';

function setStatus(text: string, isError = false) {
  if(!statusText) return;
  statusText.textContent = text;
  statusText.dataset.state = isError ? 'error' : 'ok';
}

function setLocked(locked: boolean) {
  gateLocked = locked;
  if(gateRoot) {
    gateRoot.classList.toggle('is-locked', locked);
  }
}

function showGate(options: GateOptions = {}) {
  if(!gateRoot) {
    buildGate();
  }
  if(!gateRoot) return;

  setLocked(options.lock !== false);
  gateRoot.classList.add('is-visible');
  gateVisible = true;

  if(options.focus === 'license' && licenseInput) {
    licenseInput.focus();
  } else if(emailInput) {
    emailInput.focus();
  }

  refreshAccess();
}

function hideGate() {
  if(!gateRoot) return;
  gateRoot.classList.remove('is-visible');
  gateVisible = false;
}

async function refreshAccess() {
  const token = getSpNetToken();
  if(!token) {
    setStatus(t('SpNetGramLicenseLoginRequired'), true);
    return;
  }
  setStatus(t('SpNetGramLicenseChecking'));
  try {
    const access = await accessStatus();
    if(access?.canUse) {
      setStatus(t('SpNetGramLicenseActive'));
      if(gateLocked) {
        hideGate();
      }
      return;
    }
    setStatus(t('SpNetGramLicenseRequired'), true);
  } catch(err: any) {
    setStatus(t('SpNetGramLicenseCheckFailed'), true);
  }
}

async function handleLogin() {
  if(!emailInput || !passwordInput) return;
  const email = emailInput.value.trim().toLowerCase();
  const password = passwordInput.value.trim();
  if(!email || !password) {
    setStatus(t('SpNetGramLicenseMissingFields'), true);
    return;
  }
  setStatus(t('SpNetGramLicenseSigningIn'));
  try {
    const data = await login(email, password);
    if(data?.token) {
      setSpNetToken(data.token);
      setSpNetEmail(email);
      await refreshAccess();
      toastNew({langPackKey: 'SpNetGramLicenseSignedIn'});
    } else {
      setStatus(t('SpNetGramLicenseCheckFailed'), true);
    }
  } catch(err: any) {
    setStatus(err?.message || t('SpNetGramLicenseCheckFailed'), true);
  }
}

async function handleRegister() {
  if(!emailInput || !passwordInput || !displayNameInput) return;
  const email = emailInput.value.trim().toLowerCase();
  const password = passwordInput.value.trim();
  let displayName = displayNameInput.value.trim();
  if(!displayName && email.includes('@')) {
    displayName = email.split('@')[0];
  }
  if(!email || !password) {
    setStatus(t('SpNetGramLicenseMissingFields'), true);
    return;
  }
  setStatus(t('SpNetGramLicenseCreating'));
  try {
    const registerData = await register(email, password, displayName);
    if(registerData?.token) {
      setSpNetToken(registerData.token);
      setSpNetEmail(email);
      await refreshAccess();
      toastNew({langPackKey: 'SpNetGramLicenseAccountReady'});
      return;
    }
    const data = await login(email, password);
    if(data?.token) {
      setSpNetToken(data.token);
      setSpNetEmail(email);
      await refreshAccess();
      toastNew({langPackKey: 'SpNetGramLicenseAccountReady'});
      return;
    }
    setStatus(t('SpNetGramLicenseCheckFailed'), true);
  } catch(err: any) {
    setStatus(err?.message || t('SpNetGramLicenseCheckFailed'), true);
  }
}

async function handleRedeem() {
  if(!licenseInput) return;
  const key = licenseInput.value.trim();
  if(!key) {
    setStatus(t('SpNetGramLicenseMissingKey'), true);
    return;
  }
  if(!getSpNetToken()) {
    setStatus(t('SpNetGramLicenseLoginRequired'), true);
    return;
  }
  setStatus(t('SpNetGramLicenseRedeeming'));
  try {
    await redeemLicense(key);
    licenseInput.value = '';
    toastNew({langPackKey: 'SpNetGramLicenseRedeemed'});
    await refreshAccess();
  } catch(err: any) {
    setStatus(err?.message || t('SpNetGramLicenseCheckFailed'), true);
  }
}

async function handleResetPassword() {
  if(!emailInput || !passwordInput || !resetCodeInput) return;
  const email = emailInput.value.trim().toLowerCase();
  const newPassword = passwordInput.value.trim();
  const code = resetCodeInput.value.trim();
  if(!email) {
    setStatus(t('SpNetGramLicenseMissingFields'), true);
    return;
  }
  if(!code) {
    setStatus(t('SpNetGramLicenseResetSending'));
    try {
      const data = await requestPasswordReset(email);
      if(data?.resetToken) {
        resetCodeInput.value = data.resetToken;
      }
      setStatus(t('SpNetGramLicenseResetSent'));
    } catch(err: any) {
      setStatus(err?.message || t('SpNetGramLicenseCheckFailed'), true);
    }
    return;
  }
  if(!newPassword) {
    setStatus(t('SpNetGramLicenseMissingFields'), true);
    return;
  }
  setStatus(t('SpNetGramLicenseResetUpdating'));
  try {
    await confirmPasswordReset(code, newPassword);
    setStatus(t('SpNetGramLicenseResetDone'));
  } catch(err: any) {
    setStatus(err?.message || t('SpNetGramLicenseCheckFailed'), true);
  }
}

function handleLogout() {
  clearSpNetToken();
  setStatus(t('SpNetGramLicenseLoggedOut'));
}

function buildGate() {
  gateRoot = document.createElement('div');
  gateRoot.className = 'spnet-license-gate';

  const card = document.createElement('div');
  card.className = 'spnet-license-gate__card';

  const header = document.createElement('div');
  header.className = 'spnet-license-gate__header';

  const title = document.createElement('h2');
  title.textContent = t('SpNetGramLicenseGateTitle');

  closeButton = document.createElement('button');
  closeButton.className = 'spnet-license-gate__close';
  closeButton.textContent = '×';
  closeButton.addEventListener('click', () => {
    if(gateLocked) return;
    hideGate();
  });

  header.append(title, closeButton);

  const subtitle = document.createElement('p');
  subtitle.className = 'spnet-license-gate__subtitle';
  subtitle.textContent = t('SpNetGramLicenseGateSubtitle');

  statusText = document.createElement('p');
  statusText.className = 'spnet-license-gate__status';
  statusText.textContent = t('SpNetGramLicenseChecking');

  const accountSection = document.createElement('div');
  accountSection.className = 'spnet-license-gate__section';
  const accountTitle = document.createElement('h3');
  accountTitle.textContent = t('SpNetGramLicenseAccountTitle');

  emailInput = document.createElement('input');
  emailInput.type = 'email';
  emailInput.placeholder = t('SpNetGramLicenseEmail');
  emailInput.className = 'spnet-license-gate__input';
  emailInput.value = getSpNetEmail();

  passwordInput = document.createElement('input');
  passwordInput.type = 'password';
  passwordInput.placeholder = t('SpNetGramLicensePassword');
  passwordInput.className = 'spnet-license-gate__input';

  displayNameInput = document.createElement('input');
  displayNameInput.type = 'text';
  displayNameInput.placeholder = t('SpNetGramLicenseDisplayName');
  displayNameInput.className = 'spnet-license-gate__input';

  resetCodeInput = document.createElement('input');
  resetCodeInput.type = 'text';
  resetCodeInput.placeholder = t('SpNetGramLicenseResetCode');
  resetCodeInput.className = 'spnet-license-gate__input';

  const accountButtons = document.createElement('div');
  accountButtons.className = 'spnet-license-gate__actions';

  loginButton = document.createElement('button');
  loginButton.className = 'spnet-license-gate__button primary';
  loginButton.textContent = t('SpNetGramLicenseSignIn');
  loginButton.addEventListener('click', handleLogin);

  registerButton = document.createElement('button');
  registerButton.className = 'spnet-license-gate__button';
  registerButton.textContent = t('SpNetGramLicenseCreateAccount');
  registerButton.addEventListener('click', handleRegister);

  resetButton = document.createElement('button');
  resetButton.className = 'spnet-license-gate__button';
  resetButton.textContent = t('SpNetGramLicenseResetPassword');
  resetButton.addEventListener('click', handleResetPassword);

  accountButtons.append(loginButton, registerButton, resetButton);

  accountSection.append(accountTitle, emailInput, passwordInput, displayNameInput, resetCodeInput, accountButtons);

  const licenseSection = document.createElement('div');
  licenseSection.className = 'spnet-license-gate__section';
  const licenseTitle = document.createElement('h3');
  licenseTitle.textContent = t('SpNetGramLicenseRedeemTitle');

  licenseInput = document.createElement('input');
  licenseInput.type = 'text';
  licenseInput.placeholder = t('SpNetGramLicenseKeyHint');
  licenseInput.className = 'spnet-license-gate__input';

  redeemButton = document.createElement('button');
  redeemButton.className = 'spnet-license-gate__button primary';
  redeemButton.textContent = t('SpNetGramLicenseRedeem');
  redeemButton.addEventListener('click', handleRedeem);

  licenseSection.append(licenseTitle, licenseInput, redeemButton);

  const footer = document.createElement('div');
  footer.className = 'spnet-license-gate__footer';

  refreshButton = document.createElement('button');
  refreshButton.className = 'spnet-license-gate__button';
  refreshButton.textContent = t('SpNetGramLicenseRefresh');
  refreshButton.addEventListener('click', refreshAccess);

  logoutButton = document.createElement('button');
  logoutButton.className = 'spnet-license-gate__button ghost';
  logoutButton.textContent = t('SpNetGramLicenseLogout');
  logoutButton.addEventListener('click', handleLogout);

  footer.append(refreshButton, logoutButton);

  card.append(header, subtitle, statusText, accountSection, licenseSection, footer);
  gateRoot.append(card);
  document.body.append(gateRoot);
}

export function ensureLicenseAccess() {
  const token = getSpNetToken();
  if(!token) {
    showGate({lock: true});
    return;
  }
  accessStatus()
  .then((access) => {
    if(!access?.canUse) {
      showGate({lock: true});
    } else if(gateLocked) {
      hideGate();
    }
  })
  .catch(() => {
    showGate({lock: true});
  });
}

export function showLicenseGate(options: GateOptions = {}) {
  showGate(options);
}

export function startAccessRefreshTimer() {
  if(refreshTimer) return;
  refreshTimer = window.setInterval(() => {
    const token = getSpNetToken();
    if(!token) {
      showGate({lock: true});
      return;
    }
    accessStatus()
    .then((access) => {
      if(!access?.canUse) {
        showGate({lock: true});
      } else if(gateLocked) {
        hideGate();
      }
    })
    .catch(() => {
      showGate({lock: true});
    });
  }, ACCESS_REFRESH_INTERVAL);

  document.addEventListener('visibilitychange', () => {
    if(!document.hidden) {
      ensureLicenseAccess();
    }
  });
}
