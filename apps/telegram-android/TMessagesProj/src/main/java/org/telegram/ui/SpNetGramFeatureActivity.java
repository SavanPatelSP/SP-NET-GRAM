package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.BuildVars;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SpNetGramApi;
import org.telegram.messenger.SpNetGramConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramFeatureActivity extends BaseFragment {

    private static final String PREFS_NAME = "spnetgram_settings";
    private static final String KEY_ASSISTANT_LAST = "assistant_last";

    public static final String FEATURE_ASSISTANT = "assistant";
    public static final String FEATURE_SPG_ID = "spg_id";
    public static final String FEATURE_PREMIUM = "premium";
    public static final String FEATURE_SP_COIN = "sp_coin";
    public static final String FEATURE_GEMS = "gems";
    public static final String FEATURE_WALLET_SEND_COIN = "wallet_send_coin";
    public static final String FEATURE_WALLET_SEND_GEMS = "wallet_send_gems";
    public static final String FEATURE_GENERAL = "general";
    public static final String FEATURE_APPEARANCE = "appearance";
    public static final String FEATURE_CHATS = "chats";
    public static final String FEATURE_PRIVACY = "privacy";
    public static final String FEATURE_WALLET = "wallet";
    public static final String FEATURE_CAMERA = "camera";
    public static final String FEATURE_EXPERIMENTAL = "experimental";
    public static final String FEATURE_UPDATES = "updates";
    public static final String FEATURE_OFFICIAL_CHANNEL = "official_channel";
    public static final String FEATURE_OFFICIAL_GROUP = "official_group";
    public static final String FEATURE_SOURCE_CODE = "source_code";
    public static final String FEATURE_TRANSLATE = "translate";
    public static final String FEATURE_DONATE = "donate";
    public static final String FEATURE_COPY_REPORT = "copy_report";

    private final String feature;
    private SharedPreferences prefs;

    public SpNetGramFeatureActivity(String feature) {
        super(null);
        this.feature = feature;
    }

    @Override
    public boolean onFragmentCreate() {
        prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        SpNetGramUi.applyActionBar(actionBar);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getTitleForFeature());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(SpNetGramUi.COLOR_BG);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(SpNetGramUi.COLOR_BG);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        TextView description = new TextView(context);
        description.setTextSize(15);
        description.setTextColor(SpNetGramUi.COLOR_MUTED);
        description.setLineSpacing(AndroidUtilities.dp(4), 1.0f);
        description.setText(getDescriptionForFeature());

        LinearLayout hero = createCard(context, true);
        hero.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        hero.addView(description, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(hero, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        addFeatureContent(context, container);

        scrollView.addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = root;
        return fragmentView;
    }

    private CharSequence getTitleForFeature() {
        if (FEATURE_ASSISTANT.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramAssistant);
        } else if (FEATURE_SPG_ID.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramId);
        } else if (FEATURE_PREMIUM.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramPremiumPlan);
        } else if (FEATURE_SP_COIN.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramCoinAirdrop);
        } else if (FEATURE_GEMS.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramGems);
        } else if (FEATURE_WALLET_SEND_COIN.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramWalletSendCoinTitle);
        } else if (FEATURE_WALLET_SEND_GEMS.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramWalletSendGemsTitle);
        } else if (FEATURE_GENERAL.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramGeneral);
        } else if (FEATURE_APPEARANCE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramAppearance);
        } else if (FEATURE_CHATS.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramChats);
        } else if (FEATURE_PRIVACY.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramPrivacyPower);
        } else if (FEATURE_WALLET.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramWallet);
        } else if (FEATURE_CAMERA.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramCameraMedia);
        } else if (FEATURE_EXPERIMENTAL.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramExperimental);
        } else if (FEATURE_UPDATES.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramUpdates);
        } else if (FEATURE_OFFICIAL_CHANNEL.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramOfficialChannel);
        } else if (FEATURE_OFFICIAL_GROUP.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramOfficialGroup);
        } else if (FEATURE_SOURCE_CODE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramSourceCode);
        } else if (FEATURE_TRANSLATE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramTranslate);
        } else if (FEATURE_DONATE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramDonate);
        } else if (FEATURE_COPY_REPORT.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramCopyReport);
        }
        return LocaleController.getString(R.string.SpNetGramSettings);
    }

    private CharSequence getDescriptionForFeature() {
        if (FEATURE_ASSISTANT.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramAssistantInfo);
        } else if (FEATURE_SPG_ID.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramIdInfo);
        } else if (FEATURE_PREMIUM.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramPremiumPlanInfo);
        } else if (FEATURE_SP_COIN.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramCoinAirdropInfo);
        } else if (FEATURE_GEMS.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramGemsInfo);
        } else if (FEATURE_WALLET_SEND_COIN.equals(feature) || FEATURE_WALLET_SEND_GEMS.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramWalletSendInfo);
        } else if (FEATURE_GENERAL.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramGeneralInfo);
        } else if (FEATURE_APPEARANCE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramAppearanceInfo);
        } else if (FEATURE_CHATS.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramChatsInfo);
        } else if (FEATURE_PRIVACY.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramPrivacyPowerInfo);
        } else if (FEATURE_WALLET.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramWalletInfo);
        } else if (FEATURE_CAMERA.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramCameraMediaInfo);
        } else if (FEATURE_EXPERIMENTAL.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramExperimentalInfo);
        } else if (FEATURE_UPDATES.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramUpdatesInfo);
        } else if (FEATURE_OFFICIAL_CHANNEL.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramOfficialChannelInfo);
        } else if (FEATURE_OFFICIAL_GROUP.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramOfficialGroupInfo);
        } else if (FEATURE_SOURCE_CODE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramSourceCodeInfo);
        } else if (FEATURE_TRANSLATE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramTranslateInfo);
        } else if (FEATURE_DONATE.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramDonateInfo);
        } else if (FEATURE_COPY_REPORT.equals(feature)) {
            return LocaleController.getString(R.string.SpNetGramCopyReportInfo);
        }
        return LocaleController.getString(R.string.SpNetGramInfoDefault);
    }

    private void addFeatureContent(Context context, LinearLayout container) {
        if (FEATURE_GENERAL.equals(feature)) {
            TextSettingsCell drawer = createOptionCell(context,
                    LocaleController.getString(R.string.SpNetGramDrawerLayout),
                    getDrawerLabel());
            drawer.setOnClickListener(v -> {
                int next = (SpNetGramConfig.getDrawerStyle() + 1) % 3;
                SpNetGramConfig.setDrawerStyle(next);
                drawer.setTextAndValue(LocaleController.getString(R.string.SpNetGramDrawerLayout), getDrawerLabel(), false);
            });
            container.addView(drawer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextSettingsCell title = createOptionCell(context,
                    LocaleController.getString(R.string.SpNetGramAppTitle),
                    getAppTitleLabel());
            title.setOnClickListener(v -> {
                int next = (SpNetGramConfig.getAppTitleStyle() + 1) % 3;
                SpNetGramConfig.setAppTitleStyle(next);
                title.setTextAndValue(LocaleController.getString(R.string.SpNetGramAppTitle), getAppTitleLabel(), false);
            });
            container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextSettingsCell folders = createOptionCell(context,
                    LocaleController.getString(R.string.SpNetGramFolderStyle),
                    getFolderLabel());
            folders.setOnClickListener(v -> {
                int next = (SpNetGramConfig.getFolderStyle() + 1) % 3;
                SpNetGramConfig.setFolderStyle(next);
                folders.setTextAndValue(LocaleController.getString(R.string.SpNetGramFolderStyle), getFolderLabel(), false);
            });
            container.addView(folders, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        if (FEATURE_APPEARANCE.equals(feature)) {
            TextCheckCell center = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramCenterChat),
                    LocaleController.getString(R.string.SpNetGramCenterChatInfo),
                    SpNetGramConfig.isCenterChatEnabled(),
                    SpNetGramConfig::setCenterChatEnabled);
            container.addView(center, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell compact = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramCompactList),
                    LocaleController.getString(R.string.SpNetGramCompactListInfo),
                    SpNetGramConfig.isCompactListEnabled(),
                    SpNetGramConfig::setCompactListEnabled);
            container.addView(compact, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        if (FEATURE_CHATS.equals(feature)) {
            TextCheckCell quick = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramQuickActions),
                    LocaleController.getString(R.string.SpNetGramQuickActionsInfo),
                    SpNetGramConfig.isQuickActionsEnabled(),
                    SpNetGramConfig::setQuickActionsEnabled);
            container.addView(quick, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell translate = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramTranslateBar),
                    LocaleController.getString(R.string.SpNetGramTranslateBarInfo),
                    SpNetGramConfig.isTranslateBarEnabled(),
                    SpNetGramConfig::setTranslateBarEnabled);
            container.addView(translate, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell history = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramEditHistory),
                    LocaleController.getString(R.string.SpNetGramEditHistoryInfo),
                    SpNetGramConfig.isEditHistoryEnabled(),
                    SpNetGramConfig::setEditHistoryEnabled);
            container.addView(history, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        if (FEATURE_PRIVACY.equals(feature)) {
            TextCheckCell ghost = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramGhostMode),
                    LocaleController.getString(R.string.SpNetGramGhostModeInfo),
                    SpNetGramConfig.isGhostMode(),
                    SpNetGramConfig::setGhostMode);
            container.addView(ghost, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell hideTyping = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramHideTyping),
                    LocaleController.getString(R.string.SpNetGramHideTypingInfo),
                    SpNetGramConfig.isHideTypingEnabled(),
                    SpNetGramConfig::setHideTypingEnabled);
            container.addView(hideTyping, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell noRead = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramNoReadReceipts),
                    LocaleController.getString(R.string.SpNetGramNoReadReceiptsInfo),
                    SpNetGramConfig.isNoReadReceiptsEnabled(),
                    SpNetGramConfig::setNoReadReceiptsEnabled);
            container.addView(noRead, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell antiRevoke = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramAntiRevoke),
                    LocaleController.getString(R.string.SpNetGramAntiRevokeInfo),
                    SpNetGramConfig.isAntiRevokeEnabled(),
                    SpNetGramConfig::setAntiRevokeEnabled);
            container.addView(antiRevoke, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell hiddenChats = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramHiddenChatsToggle),
                    LocaleController.getString(R.string.SpNetGramHiddenChatsInfo),
                    SpNetGramConfig.isHiddenChatsEnabled(),
                    SpNetGramConfig::setHiddenChatsEnabled);
            container.addView(hiddenChats, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell locks = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramChatLocks),
                    LocaleController.getString(R.string.SpNetGramChatLocksInfo),
                    SpNetGramConfig.isChatLocksEnabled(),
                    SpNetGramConfig::setChatLocksEnabled);
            container.addView(locks, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

            TextView manage = createActionButton(context, LocaleController.getString(R.string.SpNetGramManageHiddenChats));
            manage.setOnClickListener(v -> presentFragment(new SpNetGramHiddenChatsActivity()));
            container.addView(manage);
            return;
        }

        if (FEATURE_WALLET.equals(feature)) {
            TextView walletStatus = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            container.addView(walletStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

            TextView transferLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramWalletTransfer));
            container.addView(transferLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            TextView sendCoin = createActionButton(context, LocaleController.getString(R.string.SpNetGramWalletSendCoinTitle));
            sendCoin.setOnClickListener(v -> presentFragment(new SpNetGramFeatureActivity(FEATURE_WALLET_SEND_COIN)));
            container.addView(sendCoin, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
            TextView sendGems = createActionButton(context, LocaleController.getString(R.string.SpNetGramWalletSendGemsTitle));
            sendGems.setOnClickListener(v -> presentFragment(new SpNetGramFeatureActivity(FEATURE_WALLET_SEND_GEMS)));
            container.addView(sendGems, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

            TextView coinLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramCoinAirdrop));
            container.addView(coinLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            TextView coinStatus = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            final boolean[] coinClaim = new boolean[] {false};
            TextView coinAction = createActionButton(context, LocaleController.getString(R.string.SpNetGramRefresh));
            coinAction.setOnClickListener(v -> {
                if (!ensureSignedIn()) return;
                if (coinClaim[0]) {
                    claimAirdrop(coinStatus, coinAction, coinClaim);
                } else {
                    loadCoinStatus(coinStatus, coinAction, coinClaim);
                }
            });
            container.addView(coinStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
            container.addView(coinAction, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

            TextView gemsLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramGems));
            container.addView(gemsLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            TextView gemsStatus = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            final boolean[] gemsClaim = new boolean[] {false};
            TextView gemsAction = createActionButton(context, LocaleController.getString(R.string.SpNetGramRefresh));
            gemsAction.setOnClickListener(v -> {
                if (!ensureSignedIn()) return;
                if (gemsClaim[0]) {
                    claimGems(gemsStatus, gemsAction, gemsClaim);
                } else {
                    loadGemsStatus(gemsStatus, gemsAction, gemsClaim);
                }
            });
            container.addView(gemsStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
            container.addView(gemsAction, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            loadWalletStatus(walletStatus);
            loadCoinStatus(coinStatus, coinAction, coinClaim);
            loadGemsStatus(gemsStatus, gemsAction, gemsClaim);
            return;
        }

        if (FEATURE_WALLET_SEND_COIN.equals(feature) || FEATURE_WALLET_SEND_GEMS.equals(feature)) {
            boolean isGems = FEATURE_WALLET_SEND_GEMS.equals(feature);
            String assetName = isGems ? LocaleController.getString(R.string.SpNetGramGems) : LocaleController.getString(R.string.SpNetGramCoin);

            TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramWalletSendInfo));
            container.addView(status, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

            TextView recipientLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramWalletRecipient));
            container.addView(recipientLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            TextView recipientValue = createBodyText(context, LocaleController.getString(R.string.SpNetGramWalletRecipientHint));
            container.addView(recipientValue, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextView amountLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramWalletAmount));
            container.addView(amountLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            TextView amountValue = createBodyText(context, LocaleController.getString(R.string.SpNetGramWalletAmountHint));
            container.addView(amountValue, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextView noteLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramWalletNote));
            container.addView(noteLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            TextView noteValue = createBodyText(context, LocaleController.getString(R.string.SpNetGramWalletNoteHint));
            container.addView(noteValue, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextView send = createActionButton(context, LocaleController.formatString(R.string.SpNetGramWalletSendAction, assetName));
            send.setOnClickListener(v -> {
                if (!ensureSignedIn()) {
                    setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
                    return;
                }
                setStatus(status, LocaleController.formatString(R.string.SpNetGramWalletSendStub, assetName), false);
            });
            container.addView(send);
            return;
        }

        if (FEATURE_CAMERA.equals(feature)) {
            TextCheckCell cameraX = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramCameraX),
                    LocaleController.getString(R.string.SpNetGramCameraXInfo),
                    SpNetGramConfig.isCameraXEnabled(),
                    SpNetGramConfig::setCameraXEnabled);
            container.addView(cameraX, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell flash = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramCameraFlash),
                    LocaleController.getString(R.string.SpNetGramCameraFlashInfo),
                    SpNetGramConfig.isCameraFlashEnabled(),
                    SpNetGramConfig::setCameraFlashEnabled);
            container.addView(flash, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell ultra = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramCameraUltra),
                    LocaleController.getString(R.string.SpNetGramCameraUltraInfo),
                    SpNetGramConfig.isCameraUltrawideEnabled(),
                    SpNetGramConfig::setCameraUltrawideEnabled);
            container.addView(ultra, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        if (FEATURE_EXPERIMENTAL.equals(feature)) {
            TextCheckCell spam = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramSpamFilter),
                    LocaleController.getString(R.string.SpNetGramSpamFilterInfo),
                    SpNetGramConfig.isSpamFilterEnabled(),
                    SpNetGramConfig::setSpamFilterEnabled);
            container.addView(spam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell sponsored = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramHideSponsored),
                    LocaleController.getString(R.string.SpNetGramHideSponsoredInfo),
                    SpNetGramConfig.isHideSponsoredEnabled(),
                    SpNetGramConfig::setHideSponsoredEnabled);
            container.addView(sponsored, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

            TextCheckCell ui = createToggleCell(context,
                    LocaleController.getString(R.string.SpNetGramUiRedesign),
                    LocaleController.getString(R.string.SpNetGramUiRedesignInfo),
                    SpNetGramConfig.isUiRedesignEnabled(),
                    SpNetGramConfig::setUiRedesignEnabled);
            container.addView(ui, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        if (FEATURE_UPDATES.equals(feature)) {
            final TextView status = createStatusText(context,
                    LocaleController.formatString(R.string.SpNetGramCurrentBuild, BuildVars.BUILD_VERSION_STRING));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramCheckUpdates));
            action.setOnClickListener(v -> status.setText(LocaleController.getString(R.string.SpNetGramUpToDate)));
            container.addView(action);
            return;
        }

        if (FEATURE_OFFICIAL_CHANNEL.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramOfficialChannelInfo));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramOpenChannel));
            action.setOnClickListener(v -> Browser.openUrl(context, "https://t.me/SP_NETGRAM"));
            container.addView(action);
            return;
        }

        if (FEATURE_OFFICIAL_GROUP.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramOfficialGroupInfo));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramOpenGroup));
            action.setOnClickListener(v -> Browser.openUrl(context, "https://t.me/SP_NETGRAM_GROUP"));
            container.addView(action);
            return;
        }

        if (FEATURE_SOURCE_CODE.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramSourceCodeInfo));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramOpenSource));
            action.setOnClickListener(v -> Browser.openUrl(context, "https://github.com/SavanPatelSP/SP-NET-GRAM"));
            container.addView(action);
            return;
        }

        if (FEATURE_TRANSLATE.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramTranslateInfo));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramTranslateAction));
            action.setOnClickListener(v -> Browser.openUrl(context, "https://github.com/SavanPatelSP/SP-NET-GRAM"));
            container.addView(action);
            return;
        }

        if (FEATURE_DONATE.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramDonateInfo));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramDonateAction));
            action.setOnClickListener(v -> status.setText(LocaleController.getString(R.string.SpNetGramDonateThanks)));
            container.addView(action);
            return;
        }

        if (FEATURE_COPY_REPORT.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramCopyReportInfo));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramCopyReport));
            action.setOnClickListener(v -> {
                String report = "SP NET GRAM " + BuildVars.BUILD_VERSION_STRING + "\\nAndroid " + android.os.Build.VERSION.RELEASE;
                AndroidUtilities.addToClipboard(report);
                status.setText(LocaleController.getString(R.string.SpNetGramReportCopied));
            });
            container.addView(action);
            return;
        }

        if (FEATURE_ASSISTANT.equals(feature)) {
            String contextText = SpNetGramConfig.getAssistantContext();
            if (!TextUtils.isEmpty(contextText)) {
                TextView contextLabel = createSectionLabel(context, LocaleController.getString(R.string.SpNetGramAssistantContext));
                container.addView(contextLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
                TextView contextCard = createBodyText(context, contextText);
                container.addView(contextCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
            }

            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            final TextView output = createBodyText(context, prefs.getString(KEY_ASSISTANT_LAST, LocaleController.getString(R.string.SpNetGramAssistantDefaultResponse)));
            container.addView(status, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
            container.addView(output, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

            TextView insert = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantInsert));
            insert.setOnClickListener(v -> {
                CharSequence reply = output.getText();
                if (TextUtils.isEmpty(reply)) {
                    setStatus(status, LocaleController.getString(R.string.SpNetGramAssistantNoOutput), true);
                    return;
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.assistantReplyReady, reply);
                setStatus(status, LocaleController.getString(R.string.SpNetGramAssistantInserted), false);
            });
            container.addView(insert, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView summarize = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantSummarize));
            summarize.setOnClickListener(v -> runAssistant(status, output, buildAssistantPrompt(LocaleController.getString(R.string.SpNetGramAssistantSummarizePrompt), contextText), "summarize"));
            container.addView(summarize, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView replies = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantReplies));
            replies.setOnClickListener(v -> runAssistant(status, output, buildAssistantPrompt(LocaleController.getString(R.string.SpNetGramAssistantRepliesPrompt), contextText), "replies"));
            container.addView(replies, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView rewrite = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantRewrite));
            rewrite.setOnClickListener(v -> runAssistant(status, output, buildAssistantPrompt(LocaleController.getString(R.string.SpNetGramAssistantRewritePrompt), contextText), "rewrite"));
            container.addView(rewrite, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView translate = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantTranslate));
            translate.setOnClickListener(v -> runAssistant(status, output, buildAssistantPrompt(LocaleController.getString(R.string.SpNetGramAssistantTranslatePrompt), contextText), "translate"));
            container.addView(translate, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView image = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantImage));
            image.setOnClickListener(v -> runAssistant(status, output, buildAssistantPrompt(LocaleController.getString(R.string.SpNetGramAssistantImagePrompt), contextText), "image"));
            container.addView(image, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            TextView tts = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantTts));
            tts.setOnClickListener(v -> runAssistant(status, output, buildAssistantPrompt(LocaleController.getString(R.string.SpNetGramAssistantTtsPrompt), contextText), "tts"));
            container.addView(tts);

            updateAssistantStatus(status);
            return;
        }

        if (FEATURE_SPG_ID.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramMintSpgId));
            action.setOnClickListener(v -> mintSpgId(status));
            container.addView(action);
            loadSpgStatus(status);
            return;
        }

        if (FEATURE_PREMIUM.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            container.addView(status);
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramManageAccess));
            action.setOnClickListener(v -> presentFragment(new SpNetGramLicenseGateActivity(false)));
            container.addView(action);
            loadPremiumStatus(status, action);
            return;
        }

        if (FEATURE_SP_COIN.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            container.addView(status);
            final boolean[] canClaim = new boolean[] {false};
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramRefresh));
            action.setOnClickListener(v -> {
                if (!ensureSignedIn()) return;
                if (canClaim[0]) {
                    claimAirdrop(status, action, canClaim);
                } else {
                    loadCoinStatus(status, action, canClaim);
                }
            });
            container.addView(action);
            loadCoinStatus(status, action, canClaim);
            return;
        }

        if (FEATURE_GEMS.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            container.addView(status);
            final boolean[] canClaim = new boolean[] {false};
            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramRefresh));
            action.setOnClickListener(v -> {
                if (!ensureSignedIn()) return;
                if (canClaim[0]) {
                    claimGems(status, action, canClaim);
                } else {
                    loadGemsStatus(status, action, canClaim);
                }
            });
            container.addView(action);
            loadGemsStatus(status, action, canClaim);
        }
    }

    private TextSettingsCell createOptionCell(Context context, CharSequence title, CharSequence value) {
        TextSettingsCell cell = new TextSettingsCell(context);
        cell.setTextAndValue(title, value, false);
        cell.setTextColor(SpNetGramUi.COLOR_TEXT);
        cell.setTextValueColor(SpNetGramUi.COLOR_ACCENT_2);
        cell.setBackground(SpNetGramUi.createCardBackground(14, true));
        return cell;
    }

    private TextCheckCell createToggleCell(Context context, CharSequence title, CharSequence subtitle, boolean checked, ToggleConsumer consumer) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setTextAndValueAndCheck(title.toString(), subtitle == null ? "" : subtitle.toString(), checked, true, false);
        cell.setBackground(SpNetGramUi.createCardBackground(14, true));
        cell.setOnClickListener(v -> {
            boolean next = !cell.getCheckBox().isChecked();
            cell.getCheckBox().setChecked(next, true);
            consumer.accept(next);
        });
        return cell;
    }

    private String getDrawerLabel() {
        int style = SpNetGramConfig.getDrawerStyle();
        if (style == 1) {
            return LocaleController.getString(R.string.SpNetGramDrawerCompact);
        } else if (style == 2) {
            return LocaleController.getString(R.string.SpNetGramDrawerMinimal);
        }
        return LocaleController.getString(R.string.SpNetGramDrawerClassic);
    }

    private String getAppTitleLabel() {
        int style = SpNetGramConfig.getAppTitleStyle();
        if (style == 1) {
            return LocaleController.getString(R.string.SpNetGramTitleChat);
        } else if (style == 2) {
            return LocaleController.getString(R.string.SpNetGramTitleHidden);
        }
        return LocaleController.getString(R.string.SpNetGramTitleBrand);
    }

    private String getFolderLabel() {
        int style = SpNetGramConfig.getFolderStyle();
        if (style == 1) {
            return LocaleController.getString(R.string.SpNetGramFolderTabs);
        } else if (style == 2) {
            return LocaleController.getString(R.string.SpNetGramFolderCompact);
        }
        return LocaleController.getString(R.string.SpNetGramFolderPills);
    }

    private interface ToggleConsumer {
        void accept(boolean enabled);
    }

    private TextView createStatusText(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setTextSize(15);
        view.setTextColor(SpNetGramUi.COLOR_MUTED);
        view.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        view.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        view.setBackground(SpNetGramUi.createCardBackground(16, false));
        view.setText(text);
        return view;
    }

    private TextView createBodyText(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setTextSize(14);
        view.setTextColor(SpNetGramUi.COLOR_TEXT);
        view.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        view.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        view.setBackground(SpNetGramUi.createCardBackground(16, true));
        view.setText(text);
        return view;
    }

    private TextView createActionButton(Context context, CharSequence text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(SpNetGramUi.COLOR_BG);
        button.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        button.setBackground(SpNetGramUi.createPrimaryButtonBackground(context));
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout createCard(Context context, boolean alt) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(SpNetGramUi.createCardBackground(18, alt));
        return card;
    }

    private boolean ensureSignedIn() {
        String token = SpNetGramConfig.getBackendToken();
        if (!TextUtils.isEmpty(token)) {
            return true;
        }
        presentFragment(new SpNetGramLicenseGateActivity(false));
        return false;
    }

    private void setStatus(TextView view, CharSequence text, boolean error) {
        if (view == null) return;
        view.setText(text);
        int color = error ? SpNetGramUi.COLOR_DANGER : SpNetGramUi.COLOR_ACCENT_2;
        view.setTextColor(color);
    }

    private String formatDate(String value) {
        if (TextUtils.isEmpty(value)) return "";
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private void updateAssistantStatus(TextView status) {
        if (TextUtils.isEmpty(SpNetGramConfig.getBackendToken())) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramAssistantStatusReady), false);
    }

    private void runAssistant(TextView status, TextView output, String prompt, String intent) {
        if (!ensureSignedIn()) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        String token = SpNetGramConfig.getBackendToken();
        SpNetGramApi.assistantChat(token, prompt, intent == null ? "general" : intent, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            String reply = json.optString("reply", "");
            if (TextUtils.isEmpty(reply)) {
                reply = LocaleController.getString(R.string.SpNetGramAssistantDefaultResponse);
            }
            output.setText(reply);
            prefs.edit().putString(KEY_ASSISTANT_LAST, reply).apply();
            setStatus(status, LocaleController.getString(R.string.SpNetGramAssistantStatusReady), false);
        });
    }

    private String buildAssistantPrompt(String task, String context) {
        if (TextUtils.isEmpty(context)) {
            return task;
        }
        return task + "\n\n" + context;
    }

    private TextView createSectionLabel(Context context, CharSequence text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(13);
        label.setTextColor(SpNetGramUi.COLOR_ACCENT_2);
        label.setPadding(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6), 0);
        return label;
    }

    private void loadSpgStatus(TextView status) {
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        SpNetGramApi.getProfile(token, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            String spgId = json.optString("spgId", "");
            if (TextUtils.isEmpty(spgId)) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramSpgStatusMissing), false);
            } else {
                setStatus(status, LocaleController.formatString(R.string.SpNetGramSpgStatusReady, spgId), false);
            }
        });
    }

    private void mintSpgId(TextView status) {
        if (!ensureSignedIn()) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        String token = SpNetGramConfig.getBackendToken();
        SpNetGramApi.mintSpgId(token, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            String spgId = json.optString("spgId", "");
            if (TextUtils.isEmpty(spgId)) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramSpgStatusMissing), false);
            } else {
                setStatus(status, LocaleController.formatString(R.string.SpNetGramSpgStatusReady, spgId), false);
            }
        });
    }

    private void loadPremiumStatus(TextView status, TextView action) {
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            action.setText(LocaleController.getString(R.string.SpNetGramManageAccess));
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        SpNetGramApi.premiumStatus(token, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            JSONObject premium = json.optJSONObject("premium");
            if (premium == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramPremiumInactive), false);
                action.setText(LocaleController.getString(R.string.SpNetGramUpgrade));
                return;
            }
            String planId = premium.optString("planId", "free");
            String state = premium.optString("status", "inactive");
            boolean active = "active".equals(state) && !"free".equals(planId);
            String planLabel = LocaleController.formatString(R.string.SpNetGramPremiumPlanLabel, planId);
            String statusLabel = active
                ? LocaleController.getString(R.string.SpNetGramPremiumActive)
                : LocaleController.getString(R.string.SpNetGramPremiumInactive);
            String expiresAt = premium.optString("expiresAt", "");
            String expiryLabel = TextUtils.isEmpty(expiresAt)
                ? LocaleController.getString(R.string.SpNetGramAccessNoExpiry)
                : LocaleController.formatString(R.string.SpNetGramAccessExpires, formatDate(expiresAt));
            setStatus(status, planLabel + "\n" + statusLabel + "\n" + expiryLabel, false);
            action.setText(LocaleController.getString(active ? R.string.SpNetGramManageAccess : R.string.SpNetGramUpgrade));
        });
    }

    private void loadCoinStatus(TextView status, TextView action, boolean[] canClaim) {
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            action.setText(LocaleController.getString(R.string.SpNetGramManageAccess));
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        SpNetGramApi.walletStatus(token, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            int spCoin = json.optInt("spCoin", 0);
            JSONObject airdrop = json.optJSONObject("airdrop");
            boolean claimReady = airdrop != null && airdrop.optBoolean("canClaim", false);
            String nextClaim = airdrop != null ? airdrop.optString("nextClaimAt", "") : "";
            canClaim[0] = claimReady;
            String balanceText = LocaleController.formatString(R.string.SpNetGramCoinBalance, spCoin);
            String airdropText;
            if (claimReady) {
                airdropText = LocaleController.getString(R.string.SpNetGramAirdropReady);
            } else if (!TextUtils.isEmpty(nextClaim)) {
                airdropText = LocaleController.formatString(R.string.SpNetGramAirdropNext, formatDate(nextClaim));
            } else {
                airdropText = LocaleController.getString(R.string.SpNetGramAirdropClaimed);
            }
            setStatus(status, balanceText + "\n" + airdropText, false);
            action.setText(LocaleController.getString(claimReady ? R.string.SpNetGramClaimAirdrop : R.string.SpNetGramRefresh));
        });
    }

    private void loadWalletStatus(TextView status) {
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        SpNetGramApi.walletStatus(token, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            int spCoin = json.optInt("spCoin", 0);
            int gems = json.optInt("gems", 0);
            String text = LocaleController.formatString(R.string.SpNetGramWalletBalance, spCoin, gems);
            setStatus(status, text, false);
        });
    }

    private void claimAirdrop(TextView status, TextView action, boolean[] canClaim) {
        if (!ensureSignedIn()) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        String token = SpNetGramConfig.getBackendToken();
        SpNetGramApi.claimAirdrop(token, json -> {
            loadCoinStatus(status, action, canClaim);
        });
    }

    private void loadGemsStatus(TextView status, TextView action, boolean[] canClaim) {
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            action.setText(LocaleController.getString(R.string.SpNetGramManageAccess));
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        SpNetGramApi.walletStatus(token, json -> {
            if (json == null) {
                setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            int gems = json.optInt("gems", 0);
            JSONObject gemsStatus = json.optJSONObject("gemsStatus");
            boolean claimReady = gemsStatus != null && gemsStatus.optBoolean("canClaim", false);
            String nextClaim = gemsStatus != null ? gemsStatus.optString("nextClaimAt", "") : "";
            canClaim[0] = claimReady;
            String balanceText = LocaleController.formatString(R.string.SpNetGramGemsBalance, gems);
            String claimText;
            if (claimReady) {
                claimText = LocaleController.getString(R.string.SpNetGramGemsClaimReady);
            } else if (!TextUtils.isEmpty(nextClaim)) {
                claimText = LocaleController.formatString(R.string.SpNetGramGemsNext, formatDate(nextClaim));
            } else {
                claimText = "";
            }
            String combined = TextUtils.isEmpty(claimText) ? balanceText : balanceText + "\n" + claimText;
            setStatus(status, combined, false);
            action.setText(LocaleController.getString(claimReady ? R.string.SpNetGramClaimGems : R.string.SpNetGramRefresh));
        });
    }

    private void claimGems(TextView status, TextView action, boolean[] canClaim) {
        if (!ensureSignedIn()) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        String token = SpNetGramConfig.getBackendToken();
        SpNetGramApi.claimGems(token, json -> {
            loadGemsStatus(status, action, canClaim);
        });
    }
}
