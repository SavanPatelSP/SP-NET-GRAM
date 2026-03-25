package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SpNetGramApi;
import org.telegram.messenger.SpNetGramConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramLicenseGateActivity extends BaseFragment {

    private final boolean lockMode;
    private Runnable onDismiss;

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 4000;

    private TextView statusView;
    private EditTextBoldCursor emailInput;
    private EditTextBoldCursor passwordInput;
    private EditTextBoldCursor displayNameInput;
    private EditTextBoldCursor resetCodeInput;
    private EditTextBoldCursor licenseInput;
    private EditTextBoldCursor backendInput;

    public SpNetGramLicenseGateActivity(boolean lockMode) {
        super(null);
        this.lockMode = lockMode;
    }

    public SpNetGramLicenseGateActivity setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
        return this;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (onDismiss != null) {
            onDismiss.run();
            onDismiss = null;
        }
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        return !lockMode;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.SpNetGramLicenseGateTitle));
        if (!lockMode) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });
        } else {
            actionBar.setBackButtonImage(0);
        }

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        TextView subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitle.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        subtitle.setText(LocaleController.getString(R.string.SpNetGramLicenseGateSubtitle));
        container.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        statusView = new TextView(context);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
        statusView.setText(LocaleController.getString(R.string.SpNetGramLicenseChecking));
        container.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView accountTitle = createSectionTitle(context, LocaleController.getString(R.string.SpNetGramLicenseAccountTitle));
        container.addView(accountTitle);

        LinearLayout accountCard = createCard(context);
        emailInput = createInput(context, LocaleController.getString(R.string.SpNetGramLicenseEmail), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(SpNetGramConfig.getBackendEmail());
        accountCard.addView(emailInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        passwordInput = createInput(context, LocaleController.getString(R.string.SpNetGramLicensePassword), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        accountCard.addView(passwordInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        displayNameInput = createInput(context, LocaleController.getString(R.string.SpNetGramLicenseDisplayName), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        accountCard.addView(displayNameInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        resetCodeInput = createInput(context, LocaleController.getString(R.string.SpNetGramLicenseResetCode), InputType.TYPE_CLASS_TEXT);
        accountCard.addView(resetCodeInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        container.addView(accountCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView backendTitle = createSectionTitle(context, LocaleController.getString(R.string.SpNetGramLicenseBackendTitle));
        container.addView(backendTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));

        LinearLayout backendCard = createCard(context);
        backendInput = createInput(context, LocaleController.getString(R.string.SpNetGramLicenseBackendHint), InputType.TYPE_TEXT_VARIATION_URI);
        backendInput.setText(SpNetGramConfig.getBackendBase());
        backendCard.addView(backendInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        container.addView(backendCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView testBackendButton = createSecondaryButton(context, LocaleController.getString(R.string.SpNetGramLicenseBackendTest));
        testBackendButton.setOnClickListener(v -> testBackend());
        container.addView(testBackendButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView useDefaultButton = createSecondaryButton(context, LocaleController.getString(R.string.SpNetGramLicenseBackendDefault));
        useDefaultButton.setOnClickListener(v -> useDefaultBackend());
        container.addView(useDefaultButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView signInButton = createActionButton(context, LocaleController.getString(R.string.SpNetGramLicenseSignIn));
        signInButton.setOnClickListener(v -> signIn());
        container.addView(signInButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        TextView registerButton = createSecondaryButton(context, LocaleController.getString(R.string.SpNetGramLicenseCreateAccount));
        registerButton.setOnClickListener(v -> register());
        container.addView(registerButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView resetButton = createSecondaryButton(context, LocaleController.getString(R.string.SpNetGramLicenseResetPassword));
        resetButton.setOnClickListener(v -> resetPassword());
        container.addView(resetButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView licenseTitle = createSectionTitle(context, LocaleController.getString(R.string.SpNetGramLicenseRedeemTitle));
        container.addView(licenseTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        LinearLayout licenseCard = createCard(context);
        licenseInput = createInput(context, LocaleController.getString(R.string.SpNetGramLicenseKeyHint), InputType.TYPE_CLASS_TEXT);
        licenseCard.addView(licenseInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        container.addView(licenseCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView redeemButton = createActionButton(context, LocaleController.getString(R.string.SpNetGramLicenseRedeem));
        redeemButton.setOnClickListener(v -> redeem());
        container.addView(redeemButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView refreshButton = createSecondaryButton(context, LocaleController.getString(R.string.SpNetGramLicenseRefresh));
        refreshButton.setOnClickListener(v -> refreshAccess());
        container.addView(refreshButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        TextView logoutButton = createSecondaryButton(context, LocaleController.getString(R.string.SpNetGramLicenseLogout));
        logoutButton.setOnClickListener(v -> logout());
        container.addView(logoutButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        scrollView.addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        refreshAccess(0);
        return fragmentView;
    }

    private TextView createSectionTitle(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        view.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(4));
        return view;
    }

    private LinearLayout createCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));
        card.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_windowBackgroundWhite)));
        return card;
    }

    private EditTextBoldCursor createInput(Context context, CharSequence hint, int inputType) {
        EditTextBoldCursor input = new EditTextBoldCursor(context);
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setBackgroundDrawable(null);
        input.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        input.setHint(hint);
        input.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setCursorSize(AndroidUtilities.dp(20));
        input.setCursorWidth(1.5f);
        return input;
    }

    private TextView createActionButton(Context context, CharSequence text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        button.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        int color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), color, color));
        button.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return button;
    }

    private TextView createSecondaryButton(Context context, CharSequence text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        button.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        int color = Theme.getColor(Theme.key_windowBackgroundWhite);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), color, color));
        button.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return button;
    }

    private void updateStatus(CharSequence text, boolean error) {
        if (statusView == null) {
            return;
        }
        statusView.setText(text);
        int color = error ? Theme.getColor(Theme.key_text_RedBold) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        statusView.setTextColor(color);
    }

    private String getErrorMessage(JSONObject json) {
        if (json == null) return null;
        String error = json.optString("error");
        if (TextUtils.isEmpty(error)) {
            error = json.optString("message");
        }
        return TextUtils.isEmpty(error) ? null : error;
    }

    private void signIn() {
        signIn(0);
    }

    private void signIn(int attempt) {
        applyBackendOverride();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseMissingFields), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseSigningIn), false);
        SpNetGramApi.login(email, password, json -> {
            if (json == null) {
                retryLater(() -> signIn(attempt + 1), attempt);
                return;
            }
            String error = getErrorMessage(json);
            String token = json.optString("token");
            if (!TextUtils.isEmpty(error) && TextUtils.isEmpty(token)) {
                updateStatus(error, true);
                return;
            }
            if (!TextUtils.isEmpty(token)) {
                SpNetGramConfig.setBackendToken(token);
                SpNetGramConfig.setBackendEmail(email);
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseSignedIn), false);
                refreshAccess(0);
            } else {
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
            }
        });
    }

    private void register() {
        register(0);
    }

    private void register(int attempt) {
        applyBackendOverride();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim();
        String displayName = displayNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(displayName) && email.contains("@")) {
            displayName = email.substring(0, email.indexOf('@'));
        }
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseMissingFields), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseCreating), false);
        SpNetGramApi.register(email, password, displayName, json -> {
            if (json == null) {
                retryLater(() -> register(attempt + 1), attempt);
                return;
            }
            String error = getErrorMessage(json);
            String token = json.optString("token");
            if (!json.optBoolean("ok", false)) {
                if (!TextUtils.isEmpty(error)) {
                    updateStatus(error, true);
                    return;
                }
                retryLater(() -> register(attempt + 1), attempt);
                return;
            }
            if (!TextUtils.isEmpty(token)) {
                SpNetGramConfig.setBackendToken(token);
                SpNetGramConfig.setBackendEmail(email);
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseAccountReady), false);
                refreshAccess(0);
            } else {
                loginAfterRegister(email, password, attempt);
            }
        });
    }

    private void resetPassword() {
        resetPassword(0);
    }

    private void resetPassword(int attempt) {
        applyBackendOverride();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String newPassword = passwordInput.getText().toString();
        String resetCode = resetCodeInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseMissingFields), true);
            return;
        }
        if (TextUtils.isEmpty(resetCode)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseResetSending), false);
            SpNetGramApi.requestPasswordReset(email, json -> {
                if (json == null) {
                    retryLater(() -> resetPassword(attempt + 1), attempt);
                    return;
                }
                String error = getErrorMessage(json);
                if (!TextUtils.isEmpty(error)) {
                    updateStatus(error, true);
                    return;
                }
                String token = json.optString("resetToken");
                if (!TextUtils.isEmpty(token)) {
                    resetCodeInput.setText(token);
                    resetCodeInput.setSelection(token.length());
                    AndroidUtilities.addToClipboard(token);
                    showResetTokenDialog(token);
                }
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseResetSent), false);
            });
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseMissingFields), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseResetUpdating), false);
        SpNetGramApi.confirmPasswordReset(resetCode, newPassword, json -> {
            if (json == null) {
                retryLater(() -> resetPassword(attempt + 1), attempt);
                return;
            }
            String error = getErrorMessage(json);
            if (!json.optBoolean("ok", false)) {
                updateStatus(TextUtils.isEmpty(error) ? LocaleController.getString(R.string.SpNetGramLicenseCheckFailed) : error, true);
                return;
            }
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseResetDone), false);
        });
    }

    private void showResetTokenDialog(String token) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SpNetGramLicenseResetCode));
        builder.setMessage(LocaleController.formatString("SpNetGramLicenseResetDialog", R.string.SpNetGramLicenseResetDialog, token));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(builder.create());
    }

    private void loginAfterRegister(String email, String password, int attempt) {
        SpNetGramApi.login(email, password, loginJson -> {
            if (loginJson == null) {
                retryLater(() -> loginAfterRegister(email, password, attempt + 1), attempt);
                return;
            }
            String loginError = getErrorMessage(loginJson);
            String token = loginJson.optString("token");
            if (!TextUtils.isEmpty(loginError) && TextUtils.isEmpty(token)) {
                updateStatus(loginError, true);
                return;
            }
            if (!TextUtils.isEmpty(token)) {
                SpNetGramConfig.setBackendToken(token);
                SpNetGramConfig.setBackendEmail(email);
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseAccountReady), false);
                refreshAccess(0);
            } else {
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
            }
        });
    }

    private void redeem() {
        applyBackendOverride();
        String licenseKey = licenseInput.getText().toString().trim();
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(licenseKey)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseMissingKey), true);
            return;
        }
        if (TextUtils.isEmpty(token)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseRedeeming), false);
        SpNetGramApi.redeemLicense(token, licenseKey, json -> {
            if (json == null) {
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                return;
            }
            String error = getErrorMessage(json);
            if (!json.optBoolean("ok", false)) {
                if (!TextUtils.isEmpty(error)) {
                    updateStatus(error, true);
                } else {
                    updateStatus(LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
                }
                return;
            }
            licenseInput.setText("");
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseRedeemed), false);
            refreshAccess();
        });
    }

    private void refreshAccess() {
        refreshAccess(0);
    }

    private void refreshAccess(int attempt) {
        applyBackendOverride();
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseLoginRequired), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseChecking), false);
        SpNetGramApi.accessStatus(token, json -> {
            if (json == null) {
                retryLater(() -> refreshAccess(attempt + 1), attempt);
                return;
            }
            String error = getErrorMessage(json);
            if (!TextUtils.isEmpty(error)) {
                updateStatus(error, true);
                return;
            }
            boolean canUse = json.optBoolean("canUse", false);
            if (canUse) {
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseActive), false);
                if (lockMode) {
                    finishFragment();
                }
            } else {
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseRequired), true);
            }
        });
    }

    private void retryLater(Runnable action, int attempt) {
        if (attempt >= MAX_RETRIES) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseCheckFailed), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseWakingServer), false);
        AndroidUtilities.runOnUIThread(action, RETRY_DELAY_MS);
    }

    private void logout() {
        SpNetGramConfig.clearBackendToken();
        SpNetGramConfig.setBackendEmail("");
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseLoggedOut), false);
    }

    private void applyBackendOverride() {
        if (backendInput == null) {
            return;
        }
        String backend = backendInput.getText().toString().trim();
        if (TextUtils.isEmpty(backend)) {
            SpNetGramConfig.setBackendBase(SpNetGramConfig.BACKEND_URL);
            return;
        }
        SpNetGramConfig.setBackendBase(backend);
    }

    private void useDefaultBackend() {
        SpNetGramConfig.setBackendBase(SpNetGramConfig.BACKEND_URL);
        if (backendInput != null) {
            backendInput.setText(SpNetGramConfig.getBackendBase());
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseBackendDefaultOk), false);
    }

    private void testBackend() {
        testBackend(0);
    }

    private void testBackend(int attempt) {
        applyBackendOverride();
        String backend = backendInput == null ? "" : backendInput.getText().toString().trim();
        if (!TextUtils.isEmpty(backend) && !backend.startsWith("http")) {
            updateStatus(LocaleController.getString(R.string.SpNetGramLicenseBackendInvalid), true);
            return;
        }
        updateStatus(LocaleController.getString(R.string.SpNetGramLicenseBackendTesting), false);
        SpNetGramApi.health(json -> {
            if (json == null) {
                retryLater(() -> testBackend(attempt + 1), attempt);
                return;
            }
            if (json.optBoolean("ok", false)) {
                updateStatus(LocaleController.getString(R.string.SpNetGramLicenseBackendOk), false);
            } else {
                String error = getErrorMessage(json);
                if (!TextUtils.isEmpty(error)) {
                    updateStatus(error, true);
                } else {
                    updateStatus(LocaleController.getString(R.string.SpNetGramLicenseBackendFailed), true);
                }
            }
        });
    }
}
