package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SpNetGramApi;
import org.telegram.messenger.SpNetGramConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramFeatureActivity extends BaseFragment {

    private static final String PREFS_NAME = "spnetgram_settings";
    private static final String KEY_ASSISTANT_LAST = "assistant_last";

    public static final String FEATURE_ASSISTANT = "assistant";
    public static final String FEATURE_SPG_ID = "spg_id";
    public static final String FEATURE_PREMIUM = "premium";
    public static final String FEATURE_SP_COIN = "sp_coin";
    public static final String FEATURE_GEMS = "gems";

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

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        TextView description = new TextView(context);
        description.setTextSize(15);
        description.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        description.setLineSpacing(AndroidUtilities.dp(4), 1.0f);
        description.setText(getDescriptionForFeature());
        container.addView(description, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addFeatureContent(context, container);

        scrollView.addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        fragmentView = scrollView;
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
        }
        return LocaleController.getString(R.string.SpNetGramInfoDefault);
    }

    private void addFeatureContent(Context context, LinearLayout container) {
        if (FEATURE_ASSISTANT.equals(feature)) {
            final TextView status = createStatusText(context, LocaleController.getString(R.string.SpNetGramStatusLoading));
            final TextView output = createBodyText(context, prefs.getString(KEY_ASSISTANT_LAST, LocaleController.getString(R.string.SpNetGramAssistantDefaultResponse)));
            container.addView(status);
            container.addView(output);

            TextView action = createActionButton(context, LocaleController.getString(R.string.SpNetGramAssistantRunDemo));
            action.setOnClickListener(v -> runAssistant(status, output));
            container.addView(action);
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

    private TextView createStatusText(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setTextSize(15);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        view.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        view.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        view.setText(text);
        return view;
    }

    private TextView createBodyText(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setTextSize(14);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        view.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        view.setText(text);
        return view;
    }

    private TextView createActionButton(Context context, CharSequence text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Color.WHITE);
        button.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        int color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), color, color));
        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(8);
        button.setLayoutParams(params);
        return button;
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
        int color = error ? Theme.getColor(Theme.key_text_RedBold) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
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

    private void runAssistant(TextView status, TextView output) {
        if (!ensureSignedIn()) {
            setStatus(status, LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        setStatus(status, LocaleController.getString(R.string.SpNetGramStatusLoading), false);
        String token = SpNetGramConfig.getBackendToken();
        SpNetGramApi.assistantChat(token, "Run the SP NET GRAM assistant demo.", "general", json -> {
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
