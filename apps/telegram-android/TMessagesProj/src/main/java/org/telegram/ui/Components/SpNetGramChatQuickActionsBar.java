package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SpNetGramConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramChatQuickActionsBar extends FrameLayout {
    private final LinearLayout row;
    private final TextView ghostLabel;

    public SpNetGramChatQuickActionsBar(@NonNull Context context, Runnable onTranslate, Runnable onAssistant, Runnable onInstantForward, Runnable onGhostToggle, Runnable onOpenLabs) {
        super(context);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        setBackground(bg);
        setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));

        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addView(row, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        row.addView(buildAction(R.drawable.msg_translate, LocaleController.getString(R.string.TranslateMessage), onTranslate));
        row.addView(buildAction(R.drawable.msg_bot, LocaleController.getString(R.string.SpNetGramAssistant), onAssistant));
        row.addView(buildAction(R.drawable.msg_forward, LocaleController.getString(R.string.SpNetGramInstantForward), onInstantForward));

        LinearLayout ghostButton = buildAction(R.drawable.msg_secret, LocaleController.getString(R.string.SpNetGramGhostMode), onGhostToggle);
        ghostLabel = (TextView) ghostButton.getChildAt(1);
        row.addView(ghostButton);

        row.addView(buildAction(R.drawable.settings_features, LocaleController.getString(R.string.SpNetGramSettings), onOpenLabs));

        updateGhostState();
    }

    private LinearLayout buildAction(int iconRes, CharSequence label, Runnable onClick) {
        LinearLayout button = new LinearLayout(getContext());
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray), Theme.getColor(Theme.key_listSelector)));
        button.setOnClickListener(v -> onClick.run());

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        button.addView(icon, LayoutHelper.createLinear(18, 18));

        TextView text = new TextView(getContext());
        text.setText(label);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        text.setTextSize(12);
        text.setPadding(AndroidUtilities.dp(6), 0, 0, 0);
        button.addView(text, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        params.rightMargin = AndroidUtilities.dp(8);
        button.setLayoutParams(params);
        return button;
    }

    public void updateGhostState() {
        if (ghostLabel == null) {
            return;
        }
        boolean enabled = SpNetGramConfig.isGhostMode();
        ghostLabel.setText(enabled
            ? LocaleController.getString(R.string.SpNetGramGhostMode) + " ✓"
            : LocaleController.getString(R.string.SpNetGramGhostMode));
    }
}
