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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramFeatureActivity extends BaseFragment {

    private static final String PREFS_NAME = "spnetgram_settings";
    private static final String KEY_SPG_ID = "spg_id";
    private static final String KEY_PREMIUM = "premium_active";
    private static final String KEY_WALLET = "spcoin_wallet";
    private static final String KEY_AIRDROP = "spcoin_airdrop_claimed";
    private static final String KEY_GEMS = "gems_balance";
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
            return getString(R.string.SpNetGramAssistant);
        } else if (FEATURE_SPG_ID.equals(feature)) {
            return getString(R.string.SpNetGramId);
        } else if (FEATURE_PREMIUM.equals(feature)) {
            return getString(R.string.SpNetGramPremiumPlan);
        } else if (FEATURE_SP_COIN.equals(feature)) {
            return getString(R.string.SpNetGramCoinAirdrop);
        } else if (FEATURE_GEMS.equals(feature)) {
            return getString(R.string.SpNetGramGems);
        }
        return getString(R.string.SpNetGramSettings);
    }

    private CharSequence getDescriptionForFeature() {
        if (FEATURE_ASSISTANT.equals(feature)) {
            return getString(R.string.SpNetGramAssistantInfo);
        } else if (FEATURE_SPG_ID.equals(feature)) {
            return getString(R.string.SpNetGramIdInfo);
        } else if (FEATURE_PREMIUM.equals(feature)) {
            return getString(R.string.SpNetGramPremiumPlanInfo);
        } else if (FEATURE_SP_COIN.equals(feature)) {
            return getString(R.string.SpNetGramCoinAirdropInfo);
        } else if (FEATURE_GEMS.equals(feature)) {
            return getString(R.string.SpNetGramGemsInfo);
        }
        return getString(R.string.SpNetGramInfoDefault);
    }

    private void addFeatureContent(Context context, LinearLayout container) {
        if (FEATURE_ASSISTANT.equals(feature)) {
            final TextView status = createStatusText(context, getString(R.string.SpNetGramAssistantStatusReady));
            final TextView output = createBodyText(context, prefs.getString(KEY_ASSISTANT_LAST, getString(R.string.SpNetGramAssistantDefaultResponse)));
            container.addView(status);
            container.addView(output);

            TextView action = createActionButton(context, getString(R.string.SpNetGramAssistantRunDemo));
            action.setOnClickListener(v -> {
                String response = getString(R.string.SpNetGramAssistantDemoResponse);
                prefs.edit().putString(KEY_ASSISTANT_LAST, response).apply();
                output.setText(response);
            });
            container.addView(action);
            return;
        }

        if (FEATURE_SPG_ID.equals(feature)) {
            final TextView status = createStatusText(context, buildSpgStatus());
            container.addView(status);
            TextView action = createActionButton(context, getString(R.string.SpNetGramMintSpgId));
            action.setOnClickListener(v -> {
                String spgId = generateSpgId();
                prefs.edit().putString(KEY_SPG_ID, spgId).apply();
                status.setText(buildSpgStatus());
            });
            container.addView(action);
            return;
        }

        if (FEATURE_PREMIUM.equals(feature)) {
            final TextView status = createStatusText(context, buildPremiumStatus());
            container.addView(status);
            TextView action = createActionButton(context, getString(R.string.SpNetGramTogglePremium));
            action.setOnClickListener(v -> {
                boolean current = prefs.getBoolean(KEY_PREMIUM, false);
                prefs.edit().putBoolean(KEY_PREMIUM, !current).apply();
                status.setText(buildPremiumStatus());
            });
            container.addView(action);
            return;
        }

        if (FEATURE_SP_COIN.equals(feature)) {
            final TextView status = createStatusText(context, buildSpCoinStatus());
            container.addView(status);

            TextView linkWallet = createActionButton(context, getString(R.string.SpNetGramLinkWallet));
            linkWallet.setOnClickListener(v -> {
                if (TextUtils.isEmpty(prefs.getString(KEY_WALLET, ""))) {
                    prefs.edit().putString(KEY_WALLET, generateWallet()).apply();
                }
                status.setText(buildSpCoinStatus());
            });
            container.addView(linkWallet);

            TextView claim = createActionButton(context, getString(R.string.SpNetGramClaimAirdrop));
            claim.setOnClickListener(v -> {
                prefs.edit().putBoolean(KEY_AIRDROP, true).apply();
                status.setText(buildSpCoinStatus());
            });
            container.addView(claim);
            return;
        }

        if (FEATURE_GEMS.equals(feature)) {
            final TextView status = createStatusText(context, buildGemsStatus());
            container.addView(status);
            TextView action = createActionButton(context, getString(R.string.SpNetGramClaimGems));
            action.setOnClickListener(v -> {
                int current = prefs.getInt(KEY_GEMS, 0);
                prefs.edit().putInt(KEY_GEMS, current + 10).apply();
                status.setText(buildGemsStatus());
            });
            container.addView(action);
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

    private String buildSpgStatus() {
        String spgId = prefs.getString(KEY_SPG_ID, "");
        if (TextUtils.isEmpty(spgId)) {
            return getString(R.string.SpNetGramSpgStatusMissing);
        }
        return getString(R.string.SpNetGramSpgStatusReady, spgId);
    }

    private String buildPremiumStatus() {
        boolean active = prefs.getBoolean(KEY_PREMIUM, false);
        return active ? getString(R.string.SpNetGramPremiumActive) : getString(R.string.SpNetGramPremiumInactive);
    }

    private String buildSpCoinStatus() {
        String wallet = prefs.getString(KEY_WALLET, "");
        boolean claimed = prefs.getBoolean(KEY_AIRDROP, false);
        String walletLabel = TextUtils.isEmpty(wallet) ? getString(R.string.SpNetGramWalletNotLinked) : getString(R.string.SpNetGramWalletLinked, wallet);
        String airdropLabel = claimed ? getString(R.string.SpNetGramAirdropClaimed) : getString(R.string.SpNetGramAirdropUnclaimed);
        return walletLabel + "\n" + airdropLabel;
    }

    private String buildGemsStatus() {
        int balance = prefs.getInt(KEY_GEMS, 0);
        return getString(R.string.SpNetGramGemsBalance, balance);
    }

    private String generateSpgId() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SPG-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(Utilities.random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateWallet() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SPW-");
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(Utilities.random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
