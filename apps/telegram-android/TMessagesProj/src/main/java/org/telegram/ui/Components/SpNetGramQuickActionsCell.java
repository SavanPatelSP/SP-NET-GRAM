package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SpNetGramConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.FiltersSetupActivity;
import org.telegram.ui.SpNetGramFeatureActivity;
import org.telegram.ui.SpNetGramSettingsActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramQuickActionsCell extends FrameLayout {
    private final BaseFragment fragment;
    private final TextView titleView;
    private final TextView subtitleView;
    private final LinearLayout actionsRow;
    private final LinearLayout ghostButton;
    private final TextView ghostLabel;

    public SpNetGramQuickActionsCell(@NonNull Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;

        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { 0xFF0EA5E9, 0xFF22D3EE }
        );
        bg.setCornerRadius(AndroidUtilities.dp(18));
        setBackground(bg);
        setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        titleView = new TextView(context);
        titleView.setText(LocaleController.getString(R.string.SpNetGramSettings));
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setText(LocaleController.getString(R.string.SpNetGramSettingsInfo));
        subtitleView.setTextColor(0xE6FFFFFF);
        subtitleView.setTextSize(12);
        subtitleView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(10));
        container.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setDividerPadding(AndroidUtilities.dp(8));
        container.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        actionsRow.addView(buildAction(R.drawable.msg_mention, LocaleController.getString(R.string.SpNetGramAssistant), () -> {
            if (fragment != null) {
                fragment.presentFragment(new SpNetGramFeatureActivity(SpNetGramFeatureActivity.FEATURE_ASSISTANT));
            }
        }));

        ghostButton = buildAction(R.drawable.msg_secret, LocaleController.getString(R.string.SpNetGramGhostMode), () -> {
            SpNetGramConfig.setGhostMode(!SpNetGramConfig.isGhostMode());
            updateGhostState();
            if (fragment != null) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.drawable.msg_secret, LocaleController.getString(R.string.SpNetGramGhostMode)).show();
            }
        });
        ghostLabel = (TextView) ghostButton.getChildAt(1);
        actionsRow.addView(ghostButton);

        actionsRow.addView(buildAction(R.drawable.msg_list, LocaleController.getString(R.string.SettingsFolders), () -> {
            if (fragment != null) {
                fragment.presentFragment(new FiltersSetupActivity());
            }
        }));

        actionsRow.addView(buildAction(R.drawable.settings_features, LocaleController.getString(R.string.SpNetGramSettings), () -> {
            if (fragment != null) {
                fragment.presentFragment(new SpNetGramSettingsActivity());
            }
        }));

        updateGhostState();
    }

    private LinearLayout buildAction(int iconRes, CharSequence label, Runnable onClick) {
        LinearLayout button = new LinearLayout(getContext());
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), 0x1AFFFFFF, 0x33FFFFFF));
        button.setOnClickListener(v -> onClick.run());

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFFFFFFFF);
        button.addView(icon, LayoutHelper.createLinear(20, 20));

        TextView text = new TextView(getContext());
        text.setText(label);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(11);
        text.setMaxLines(1);
        text.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        button.addView(text, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout.LayoutParams params = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f);
        params.rightMargin = AndroidUtilities.dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private void updateGhostState() {
        if (ghostLabel == null) {
            return;
        }
        boolean enabled = SpNetGramConfig.isGhostMode();
        ghostLabel.setText(enabled
            ? LocaleController.getString(R.string.SpNetGramGhostMode) + " ✓"
            : LocaleController.getString(R.string.SpNetGramGhostMode));
    }
}
