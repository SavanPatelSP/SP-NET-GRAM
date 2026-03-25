package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SpNetGramInfoActivity extends BaseFragment {

    public static SpNetGramInfoActivity of(int titleRes, int bodyRes) {
        Bundle args = new Bundle();
        args.putInt("titleRes", titleRes);
        args.putInt("bodyRes", bodyRes);
        return new SpNetGramInfoActivity(args);
    }

    public SpNetGramInfoActivity() {
        super(null);
    }

    public SpNetGramInfoActivity(Bundle args) {
        super(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(arguments != null ? arguments.getInt("titleRes", R.string.SpNetGramSettings) : R.string.SpNetGramSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        TextView textView = new TextView(context);
        textView.setTextSize(16);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setLineSpacing(AndroidUtilities.dp(4), 1.0f);
        textView.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));
        textView.setText(LocaleController.getString(arguments != null ? arguments.getInt("bodyRes", R.string.SpNetGramInfoDefault) : R.string.SpNetGramInfoDefault));

        scrollView.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = frameLayout;
        return fragmentView;
    }
}
