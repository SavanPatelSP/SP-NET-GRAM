package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;

public class SpNetGramUi {

    public static final int COLOR_BG = 0xFF0A0A0F;
    public static final int COLOR_SURFACE = 0xFF151624;
    public static final int COLOR_SURFACE_ALT = 0xFF1A1C2B;
    public static final int COLOR_TEXT = 0xFFF7F5FF;
    public static final int COLOR_MUTED = 0xFFB7BDD3;
    public static final int COLOR_ACCENT = 0xFFFF4D8D;
    public static final int COLOR_ACCENT_2 = 0xFF4EE1FF;
    public static final int COLOR_GOLD = 0xFFF4C96B;
    public static final int COLOR_STROKE = 0x24FFFFFF;
    public static final int COLOR_DANGER = 0xFFFF5C6C;

    public static void applyActionBar(ActionBar actionBar) {
        if (actionBar == null) {
            return;
        }
        actionBar.setBackgroundColor(COLOR_BG);
        actionBar.setTitleColor(COLOR_TEXT);
        actionBar.setItemsColor(COLOR_TEXT, false);
        actionBar.setItemsBackgroundColor(0x22FFFFFF, false);
    }

    public static Drawable createCardBackground(float radiusDp, boolean alt) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(alt ? COLOR_SURFACE_ALT : COLOR_SURFACE);
        drawable.setCornerRadius(AndroidUtilities.dp(radiusDp));
        drawable.setStroke(AndroidUtilities.dp(1), COLOR_STROKE);
        return drawable;
    }

    public static Drawable createPrimaryButtonBackground(Context context) {
        GradientDrawable normal = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {COLOR_ACCENT, COLOR_GOLD}
        );
        normal.setCornerRadius(AndroidUtilities.dp(14));
        GradientDrawable pressed = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {darken(COLOR_ACCENT, 0.85f), darken(COLOR_GOLD, 0.85f)}
        );
        pressed.setCornerRadius(AndroidUtilities.dp(14));
        StateListDrawable state = new StateListDrawable();
        state.addState(new int[] {android.R.attr.state_pressed}, pressed);
        state.addState(new int[] {}, normal);
        return state;
    }

    public static Drawable createOutlineButtonBackground(float radiusDp, int strokeColor) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0x1FFFFFFF);
        normal.setCornerRadius(AndroidUtilities.dp(radiusDp));
        normal.setStroke(AndroidUtilities.dp(1), strokeColor);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(0x33FFFFFF);
        pressed.setCornerRadius(AndroidUtilities.dp(radiusDp));
        pressed.setStroke(AndroidUtilities.dp(1), strokeColor);

        StateListDrawable state = new StateListDrawable();
        state.addState(new int[] {android.R.attr.state_pressed}, pressed);
        state.addState(new int[] {}, normal);
        return state;
    }

    private static int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }
}
