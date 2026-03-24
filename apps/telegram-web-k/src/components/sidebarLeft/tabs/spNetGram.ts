import {SliderSuperTab} from '@components/slider';
import Row from '@components/row';
import SettingSection from '@components/settingSection';
import {LangPackKey, i18n} from '@lib/langPack';
import replaceContent from '@helpers/dom/replaceContent';
import {attachClickEvent} from '@helpers/dom/clickEvent';
import {toastNew} from '@components/toast';
import type {SpNetGramState} from '@config/state';

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

    const mintSpgId = () => {
      const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
      let id = 'SPG-';
      for(let i = 0; i < 8; i++) {
        id += chars[Math.floor(Math.random() * chars.length)];
      }
      updateState({spgId: id});
      toastNew({langPackKey: 'SpNetGramToastMinted', langPackArguments: [id]});
    };

    const assistantRow = new Row({
      titleLangKey: 'SpNetGramAssistant',
      subtitleLangKey: 'SpNetGramAssistantSubtitle',
      icon: 'bot_filled',
      buttonRightLangKey: 'SpNetGramRunDemo',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(assistantRow.buttonRight, () => {
      const response = i18n('SpNetGramAssistantDemoResponse');
      updateState({assistantLast: (response as HTMLElement).textContent || ''});
      toastNew({langPackKey: 'SpNetGramAssistantToast'});
    }, {listenerSetter: this.listenerSetter});

    const spgIdRow = new Row({
      titleLangKey: 'SpNetGramId',
      subtitleLangKey: 'SpNetGramIdSubtitle',
      icon: 'username',
      buttonRightLangKey: 'SpNetGramMint',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(spgIdRow.buttonRight, () => mintSpgId(), {listenerSetter: this.listenerSetter});

    const premiumRow = new Row({
      titleLangKey: 'SpNetGramPremiumPlan',
      subtitleLangKey: 'SpNetGramPremiumPlanSubtitle',
      icon: 'star',
      buttonRightLangKey: 'SpNetGramTogglePremium',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(premiumRow.buttonRight, () => {
      const next = !spState.premium;
      updateState({premium: next});
      toastNew({langPackKey: next ? 'SpNetGramPremiumOnToast' : 'SpNetGramPremiumOffToast'});
    }, {listenerSetter: this.listenerSetter});

    const coinRow = new Row({
      titleLangKey: 'SpNetGramCoinAirdrop',
      subtitleLangKey: 'SpNetGramCoinAirdropSubtitle',
      icon: 'ton',
      buttonRightLangKey: 'SpNetGramLinkWallet',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(coinRow.buttonRight, () => {
      if(!spState.wallet) {
        const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
        let wallet = 'SPW-';
        for(let i = 0; i < 10; i++) {
          wallet += chars[Math.floor(Math.random() * chars.length)];
        }
        updateState({wallet});
        toastNew({langPackKey: 'SpNetGramWalletLinkedToast'});
      } else {
        updateState({airdropClaimed: true});
        toastNew({langPackKey: 'SpNetGramAirdropClaimedToast'});
      }
    }, {listenerSetter: this.listenerSetter});

    const gemsRow = new Row({
      titleLangKey: 'SpNetGramGems',
      subtitleLangKey: 'SpNetGramGemsSubtitle',
      icon: 'gem',
      buttonRightLangKey: 'SpNetGramClaimGems',
      listenerSetter: this.listenerSetter
    });

    attachClickEvent(gemsRow.buttonRight, () => {
      const next = (spState.gems || 0) + 10;
      updateState({gems: next});
      toastNew({langPackKey: 'SpNetGramGemsClaimedToast', langPackArguments: [next]});
    }, {listenerSetter: this.listenerSetter});

    featureSection.content.append(
      assistantRow.container,
      spgIdRow.container,
      premiumRow.container,
      coinRow.container,
      gemsRow.container
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
      const assistantSubtitle = spState.assistantLast ? spState.assistantLast : (i18n('SpNetGramAssistantSubtitle') as HTMLElement).textContent || '';
      setSubtitle(assistantRow, assistantSubtitle);

      const spgSubtitle = spState.spgId ? `SPG ID: ${spState.spgId}` : (i18n('SpNetGramIdSubtitle') as HTMLElement).textContent || '';
      setSubtitle(spgIdRow, spgSubtitle);

      const premiumStatus = spState.premium ? i18n('SpNetGramPremiumActive') : i18n('SpNetGramPremiumInactive');
      setSubtitle(premiumRow, premiumStatus);

      const walletStatus = spState.wallet ? i18n('SpNetGramWalletLinked', [spState.wallet]) : i18n('SpNetGramWalletNotLinked');
      const airdropStatus = spState.airdropClaimed ? i18n('SpNetGramAirdropClaimed') : i18n('SpNetGramAirdropUnclaimed');
      setSubtitle(coinRow, `${(walletStatus as HTMLElement).textContent} · ${(airdropStatus as HTMLElement).textContent}`);
      setButtonText(coinRow, spState.wallet ? 'SpNetGramClaimAirdrop' : 'SpNetGramLinkWallet');

      const gemsBalance = spState.gems || 0;
      setSubtitle(gemsRow, (i18n('SpNetGramGemsBalance', [gemsBalance]) as HTMLElement).textContent || '');
    };

    refreshRows();

    this.scrollable.append(featureSection.container, privacySection.container);
  }
}
