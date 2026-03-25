import {SliderSuperTab} from '@components/slider';
import Row from '@components/row';
import SettingSection from '@components/settingSection';
import {LangPackKey, i18n} from '@lib/langPack';
import replaceContent from '@helpers/dom/replaceContent';
import {attachClickEvent} from '@helpers/dom/clickEvent';
import {toastNew} from '@components/toast';
import type {SpNetGramState} from '@config/state';
import {showLicenseGate} from '@lib/spnet/licenseGate';
import {
  accessStatus,
  assistantChat,
  claimAirdrop,
  claimGems,
  getProfile,
  getSpNetToken,
  mintSpgId,
  premiumPlans,
  premiumStatus,
  walletStatus
} from '@lib/spnet/api';

export default class AppSpNetGramTab extends SliderSuperTab {
  public async init() {
    this.setTitle('SpNetGramTitle');

    const featureSection = new SettingSection({name: 'SpNetGramSectionTitle'});

    const getState = async(): Promise<SpNetGramState> => {
      const state = await this.managers.appStateManager.getState();
      return state.spnetgram || {};
    };

    let spState = await getState();

    const updateState = async(patch: Partial<SpNetGramState>) => {
      spState = {...spState, ...patch};
      await this.managers.appStateManager.setByKey('spnetgram', spState);
      refreshRows();
    };

    const setSubtitle = (row: Row, text: string | HTMLElement) => {
      replaceContent(row.subtitle, text);
    };

    const setButtonText = (row: Row, key: LangPackKey) => {
      if(!row.buttonRight) return;
      replaceContent(row.buttonRight, i18n(key));
    };

    const signedIn = () => !!getSpNetToken();

    const ensureSignedIn = (focus: 'login' | 'license' = 'login') => {
      if(signedIn()) return true;
      showLicenseGate({lock: false, focus});
      return false;
    };

    const handleAuthError = (err: any) => {
      const status = err?.status;
      if(status === 401 || status === 403) {
        showLicenseGate({lock: false, focus: 'login'});
      }
    };

    const formatExpiry = (expiresAt?: string) => {
      if(!expiresAt) {
        return (i18n('SpNetGramAccessNoExpiry') as HTMLElement).textContent || '';
      }
      const date = expiresAt.length >= 10 ? expiresAt.slice(0, 10) : expiresAt;
      return (i18n('SpNetGramAccessExpires', [date]) as HTMLElement).textContent || '';
    };

    const formatDate = (value?: string | null) => {
      if(!value) return '';
      return value.length >= 10 ? value.slice(0, 10) : value;
    };

    let plansCache = spState.premiumPlans || [];

    const planName = (planId?: string) => {
      if(!planId) return '';
      const plan = plansCache.find((item) => item.id === planId);
      return plan?.name || planId;
    };

    const refreshPlans = async() => {
      try {
        const data = await premiumPlans();
        plansCache = data?.plans || [];
        await updateState({premiumPlans: plansCache});
      } catch (err) {
        // optional
      }
    };

    const refreshProfile = async() => {
      if(!signedIn()) return;
      try {
        const profile = await getProfile();
        await updateState({spgId: profile?.spgId || ''});
      } catch (err) {
        handleAuthError(err);
      }
    };

    const refreshPremium = async() => {
      if(!signedIn()) return;
      try {
        const data = await premiumStatus();
        await updateState({premiumStatus: data?.premium});
      } catch (err) {
        handleAuthError(err);
      }
    };

    const refreshWallet = async() => {
      if(!signedIn()) return;
      try {
        const data = await walletStatus();
        await updateState({
          spCoin: data?.spCoin,
          gems: data?.gems,
          airdrop: data?.airdrop,
          gemsStatus: data?.gemsStatus
        });
      } catch (err) {
        handleAuthError(err);
      }
    };

    const accessBadge = document.createElement('span');
    accessBadge.classList.add('row-title-badge', 'spnet-access-badge');

    const accessRow = new Row({
      titleLangKey: 'SpNetGramAccess',
      subtitleLangKey: 'SpNetGramAccessSubtitle',
      icon: 'lock',
      titleRight: accessBadge,
      listenerSetter: this.listenerSetter
    });

    const assistantRow = new Row({
      titleLangKey: 'SpNetGramAssistant',
      subtitleLangKey: 'SpNetGramAssistantSubtitle',
      icon: 'bot_filled',
      buttonRightLangKey: 'SpNetGramRunDemo',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(assistantRow.buttonRight, async() => {
      if(!ensureSignedIn('login')) return;
      try {
        const result = await assistantChat([
          {role: 'user', content: 'Run the SP NET GRAM assistant demo.'}
        ]);
        if(result?.reply) {
          await updateState({assistantLast: result.reply});
        }
        toastNew({langPackKey: 'SpNetGramAssistantToast'});
      } catch (err) {
        handleAuthError(err);
        toastNew({langPackKey: 'Error.AnError'});
      }
    }, {listenerSetter: this.listenerSetter});

    const spgIdRow = new Row({
      titleLangKey: 'SpNetGramId',
      subtitleLangKey: 'SpNetGramIdSubtitle',
      icon: 'username',
      buttonRightLangKey: 'SpNetGramMint',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(spgIdRow.buttonRight, async() => {
      if(!ensureSignedIn('login')) return;
      try {
        const result = await mintSpgId();
        if(result?.spgId) {
          await updateState({spgId: result.spgId});
          toastNew({langPackKey: 'SpNetGramToastMinted', langPackArguments: [result.spgId]});
        }
      } catch (err) {
        handleAuthError(err);
        toastNew({langPackKey: 'Error.AnError'});
      }
    }, {listenerSetter: this.listenerSetter});

    const premiumRow = new Row({
      titleLangKey: 'SpNetGramPremiumPlan',
      subtitleLangKey: 'SpNetGramPremiumPlanSubtitle',
      icon: 'star',
      buttonRightLangKey: 'SpNetGramManageAccess',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(premiumRow.buttonRight, async() => {
      if(!ensureSignedIn('license')) return;
      showLicenseGate({lock: false, focus: 'license'});
      await refreshPremium();
    }, {listenerSetter: this.listenerSetter});

    const coinRow = new Row({
      titleLangKey: 'SpNetGramCoinAirdrop',
      subtitleLangKey: 'SpNetGramCoinAirdropSubtitle',
      icon: 'ton',
      buttonRightLangKey: 'SpNetGramRefresh',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(coinRow.buttonRight, async() => {
      if(!ensureSignedIn('license')) return;
      if(spState.airdrop?.canClaim) {
        try {
          const result = await claimAirdrop();
          if(result?.spCoin !== undefined) {
            await updateState({spCoin: result.spCoin});
          }
          toastNew({langPackKey: 'SpNetGramAirdropClaimedToast'});
        } catch (err) {
          handleAuthError(err);
          toastNew({langPackKey: 'Error.AnError'});
        }
      }
      await refreshWallet();
    }, {listenerSetter: this.listenerSetter});

    const gemsRow = new Row({
      titleLangKey: 'SpNetGramGems',
      subtitleLangKey: 'SpNetGramGemsSubtitle',
      icon: 'gem',
      buttonRightLangKey: 'SpNetGramRefresh',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(gemsRow.buttonRight, async() => {
      if(!ensureSignedIn('license')) return;
      if(spState.gemsStatus?.canClaim) {
        try {
          const result = await claimGems();
          if(result?.gems !== undefined) {
            await updateState({gems: result.gems});
            toastNew({langPackKey: 'SpNetGramGemsClaimedToast', langPackArguments: [result.gems]});
          } else {
            toastNew({langPackKey: 'SpNetGramGemsClaimedToast', langPackArguments: [spState.gems || 0]});
          }
        } catch (err) {
          handleAuthError(err);
          toastNew({langPackKey: 'Error.AnError'});
        }
      }
      await refreshWallet();
    }, {listenerSetter: this.listenerSetter});

    const licenseRow = new Row({
      titleLangKey: 'SpNetGramRedeemLicense',
      subtitleLangKey: 'SpNetGramRedeemLicenseSubtitle',
      icon: 'key',
      buttonRightLangKey: 'SpNetGramRedeem',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(licenseRow.buttonRight, () => {
      showLicenseGate({lock: false, focus: 'license'});
    }, {listenerSetter: this.listenerSetter});

    featureSection.content.append(
      accessRow.container,
      assistantRow.container,
      spgIdRow.container,
      premiumRow.container,
      coinRow.container,
      gemsRow.container,
      licenseRow.container
    );

    const privacySection = new SettingSection({
      name: 'SpNetGramPrivacySection',
      caption: 'SpNetGramPrivacyInfo'
    });

    const ghostRow = new Row({
      titleLangKey: 'SpNetGramGhostMode',
      subtitleLangKey: 'SpNetGramGhostModeInfo',
      checkboxFieldOptions: {
        toggle: true,
        stateKey: 'spnetgram.ghostMode',
        listenerSetter: this.listenerSetter
      },
      listenerSetter: this.listenerSetter
    });

    const antiRevokeRow = new Row({
      titleLangKey: 'SpNetGramAntiRevoke',
      subtitleLangKey: 'SpNetGramAntiRevokeInfo',
      checkboxFieldOptions: {
        toggle: true,
        stateKey: 'spnetgram.antiRevoke',
        listenerSetter: this.listenerSetter
      },
      listenerSetter: this.listenerSetter
    });

    const hideTypingRow = new Row({
      titleLangKey: 'SpNetGramHideTyping',
      subtitleLangKey: 'SpNetGramHideTypingInfo',
      checkboxFieldOptions: {
        toggle: true,
        stateKey: 'spnetgram.hideTyping',
        listenerSetter: this.listenerSetter
      },
      listenerSetter: this.listenerSetter
    });

    const noReadRow = new Row({
      titleLangKey: 'SpNetGramNoReadReceipts',
      subtitleLangKey: 'SpNetGramNoReadReceiptsInfo',
      checkboxFieldOptions: {
        toggle: true,
        stateKey: 'spnetgram.noReadReceipts',
        listenerSetter: this.listenerSetter
      },
      listenerSetter: this.listenerSetter
    });

    privacySection.content.append(
      ghostRow.container,
      antiRevokeRow.container,
      hideTypingRow.container,
      noReadRow.container
    );

    const refreshRows = () => {
      const loginRequired = (i18n('SpNetGramLicenseLoginRequired') as HTMLElement).textContent || '';
      const loadingText = (i18n('SpNetGramStatusLoading') as HTMLElement).textContent || '';
      const isSignedIn = signedIn();

      const assistantSubtitle = spState.assistantLast ? spState.assistantLast : (i18n('SpNetGramAssistantSubtitle') as HTMLElement).textContent || '';
      setSubtitle(assistantRow, isSignedIn ? assistantSubtitle : loginRequired);

      const spgSubtitle = spState.spgId ? `SPG ID: ${spState.spgId}` : (i18n('SpNetGramSpgStatusMissing') as HTMLElement).textContent || '';
      setSubtitle(spgIdRow, isSignedIn ? spgSubtitle : loginRequired);

      if(isSignedIn) {
        const premium = spState.premiumStatus;
        if(premium) {
          const planLabel = (i18n('SpNetGramPremiumPlanLabel', [planName(premium.planId)]) as HTMLElement).textContent || '';
          const statusKey: LangPackKey = premium.planId && premium.planId !== 'free' && premium.status === 'active'
            ? 'SpNetGramPremiumActive'
            : 'SpNetGramPremiumInactive';
          const statusText = (i18n(statusKey) as HTMLElement).textContent || '';
          const expiryText = formatExpiry(premium.expiresAt || undefined);
          setSubtitle(premiumRow, `${planLabel} · ${statusText} · ${expiryText}`);
          setButtonText(premiumRow, premium.planId && premium.planId !== 'free' ? 'SpNetGramManageAccess' : 'SpNetGramUpgrade');
        } else {
          setSubtitle(premiumRow, loadingText);
          setButtonText(premiumRow, 'SpNetGramManageAccess');
        }
      } else {
        setSubtitle(premiumRow, loginRequired);
        setButtonText(premiumRow, 'SpNetGramManageAccess');
      }

      if(isSignedIn) {
        const balanceText = spState.spCoin !== undefined && spState.spCoin !== null
          ? (i18n('SpNetGramCoinBalance', [spState.spCoin]) as HTMLElement).textContent || ''
          : loadingText;
        let airdropText = (i18n('SpNetGramAirdropUnclaimed') as HTMLElement).textContent || '';
        if(spState.airdrop?.canClaim) {
          airdropText = (i18n('SpNetGramAirdropReady') as HTMLElement).textContent || '';
        } else if(spState.airdrop?.nextClaimAt) {
          airdropText = (i18n('SpNetGramAirdropNext', [formatDate(spState.airdrop.nextClaimAt)]) as HTMLElement).textContent || '';
        } else if(spState.airdrop) {
          airdropText = (i18n('SpNetGramAirdropClaimed') as HTMLElement).textContent || '';
        }
        setSubtitle(coinRow, `${balanceText} · ${airdropText}`);
        setButtonText(coinRow, spState.airdrop?.canClaim ? 'SpNetGramClaimAirdrop' : 'SpNetGramRefresh');
      } else {
        setSubtitle(coinRow, loginRequired);
        setButtonText(coinRow, 'SpNetGramRefresh');
      }

      if(isSignedIn) {
        const gemsBalance = spState.gems !== undefined && spState.gems !== null
          ? (i18n('SpNetGramGemsBalance', [spState.gems]) as HTMLElement).textContent || ''
          : loadingText;
        let gemsStatusText = '';
        if(spState.gemsStatus?.canClaim) {
          gemsStatusText = (i18n('SpNetGramGemsClaimReady') as HTMLElement).textContent || '';
        } else if(spState.gemsStatus?.nextClaimAt) {
          gemsStatusText = (i18n('SpNetGramGemsNext', [formatDate(spState.gemsStatus.nextClaimAt)]) as HTMLElement).textContent || '';
        }
        setSubtitle(gemsRow, gemsStatusText ? `${gemsBalance} · ${gemsStatusText}` : gemsBalance);
        setButtonText(gemsRow, spState.gemsStatus?.canClaim ? 'SpNetGramClaimGems' : 'SpNetGramRefresh');
      } else {
        setSubtitle(gemsRow, loginRequired);
        setButtonText(gemsRow, 'SpNetGramRefresh');
      }

      const access = spState.access;
      let statusKey: LangPackKey = 'SpNetGramAccessUnknown';
      if(access && access.canUse === true) statusKey = 'SpNetGramAccessActive';
      else if(access && access.canUse === false) statusKey = 'SpNetGramAccessLocked';

      const statusText = (i18n(statusKey) as HTMLElement).textContent || '';
      const expiryText = formatExpiry(access?.expiresAt);
      setSubtitle(accessRow, `${statusText} · ${expiryText}`);

      accessBadge.textContent = statusText || '';
      accessBadge.classList.toggle('spnet-access-badge--locked', statusKey === 'SpNetGramAccessLocked');
      accessBadge.classList.toggle('spnet-access-badge--unknown', statusKey === 'SpNetGramAccessUnknown');
    };

    refreshRows();

    const refreshAccessStatus = async() => {
      try {
        const access = await accessStatus();
        await updateState({access: {
          canUse: access?.canUse,
          expiresAt: access?.premium?.expiresAt,
          checkedAt: Date.now()
        }});
      } catch (err: any) {
        handleAuthError(err);
        const status = err?.status;
        const locked = status === 401 || status === 403;
        await updateState({access: {canUse: locked ? false : undefined, expiresAt: undefined, checkedAt: Date.now()}});
      }
    };

    refreshAccessStatus();
    refreshPlans();
    refreshProfile();
    refreshPremium();
    refreshWallet();

    this.scrollable.append(featureSection.container, privacySection.container);
  }
}
