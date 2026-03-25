package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SpNetGramConfig {
    private SpNetGramConfig() {
    }

    public static final String PREFS_NAME = "spnetgram_settings";

    public static final String KEY_GHOST_MODE = "ghost_mode";
    public static final String KEY_ANTI_REVOKE = "anti_revoke";
    public static final String KEY_HIDE_TYPING = "hide_typing";
    public static final String KEY_NO_READ = "no_read_receipts";
    public static final String KEY_EDIT_HISTORY = "edit_history";
    public static final String KEY_QUICK_ACTIONS = "quick_actions";
    public static final String KEY_TRANSLATE_BAR = "translate_bar";
    public static final String KEY_SPAM_FILTER = "spam_filter";
    public static final String KEY_HIDE_SPONSORED = "hide_sponsored";
    public static final String KEY_HIDDEN_DIALOGS = "hidden_dialogs";
    public static final String KEY_LOCKED_DIALOGS = "locked_dialogs";
    public static final String KEY_ENABLE_HIDDEN_CHATS = "enable_hidden_chats";
    public static final String KEY_ENABLE_CHAT_LOCKS = "enable_chat_locks";
    public static final String KEY_UI_REDESIGN = "ui_redesign";
    public static final String KEY_BACKEND_TOKEN = "backend_token";
    public static final String KEY_BACKEND_EMAIL = "backend_email";

    public static final String BACKEND_URL = "https://spnet-gram-backend.onrender.com";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isGhostMode() {
        return prefs().getBoolean(KEY_GHOST_MODE, false);
    }

    public static void setGhostMode(boolean enabled) {
        prefs().edit().putBoolean(KEY_GHOST_MODE, enabled).apply();
    }

    public static boolean isAntiRevokeEnabled() {
        return prefs().getBoolean(KEY_ANTI_REVOKE, false);
    }

    public static void setAntiRevokeEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ANTI_REVOKE, enabled).apply();
    }

    public static boolean isHideTypingEnabled() {
        return prefs().getBoolean(KEY_HIDE_TYPING, false) || isGhostMode();
    }

    public static void setHideTypingEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_HIDE_TYPING, enabled).apply();
    }

    public static boolean isNoReadReceiptsEnabled() {
        return prefs().getBoolean(KEY_NO_READ, false) || isGhostMode();
    }

    public static void setNoReadReceiptsEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_NO_READ, enabled).apply();
    }

    public static boolean isEditHistoryEnabled() {
        return prefs().getBoolean(KEY_EDIT_HISTORY, true);
    }

    public static void setEditHistoryEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_EDIT_HISTORY, enabled).apply();
    }

    public static boolean isQuickActionsEnabled() {
        return prefs().getBoolean(KEY_QUICK_ACTIONS, true);
    }

    public static void setQuickActionsEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_QUICK_ACTIONS, enabled).apply();
    }

    public static boolean isTranslateBarEnabled() {
        return prefs().getBoolean(KEY_TRANSLATE_BAR, true);
    }

    public static void setTranslateBarEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_TRANSLATE_BAR, enabled).apply();
    }

    public static boolean isSpamFilterEnabled() {
        return prefs().getBoolean(KEY_SPAM_FILTER, true);
    }

    public static void setSpamFilterEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_SPAM_FILTER, enabled).apply();
    }

    public static boolean isHideSponsoredEnabled() {
        return prefs().getBoolean(KEY_HIDE_SPONSORED, true);
    }

    public static void setHideSponsoredEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_HIDE_SPONSORED, enabled).apply();
    }

    public static boolean isUiRedesignEnabled() {
        return prefs().getBoolean(KEY_UI_REDESIGN, true);
    }

    public static void setUiRedesignEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_UI_REDESIGN, enabled).apply();
    }

    public static boolean isHiddenChatsEnabled() {
        return prefs().getBoolean(KEY_ENABLE_HIDDEN_CHATS, true);
    }

    public static void setHiddenChatsEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLE_HIDDEN_CHATS, enabled).apply();
    }

    public static boolean isChatLocksEnabled() {
        return prefs().getBoolean(KEY_ENABLE_CHAT_LOCKS, true);
    }

    public static void setChatLocksEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLE_CHAT_LOCKS, enabled).apply();
    }

    public static String getBackendToken() {
        return prefs().getString(KEY_BACKEND_TOKEN, "");
    }

    public static void setBackendToken(String token) {
        prefs().edit().putString(KEY_BACKEND_TOKEN, token == null ? "" : token).apply();
    }

    public static void clearBackendToken() {
        prefs().edit().remove(KEY_BACKEND_TOKEN).apply();
    }

    public static String getBackendEmail() {
        return prefs().getString(KEY_BACKEND_EMAIL, "");
    }

    public static void setBackendEmail(String email) {
        prefs().edit().putString(KEY_BACKEND_EMAIL, email == null ? "" : email).apply();
    }

    public static boolean isDialogHidden(long dialogId) {
        return getIdSet(KEY_HIDDEN_DIALOGS).contains(String.valueOf(dialogId));
    }

    public static void setDialogHidden(long dialogId, boolean hidden) {
        updateIdSet(KEY_HIDDEN_DIALOGS, dialogId, hidden);
    }

    public static boolean isDialogLocked(long dialogId) {
        return getIdSet(KEY_LOCKED_DIALOGS).contains(String.valueOf(dialogId));
    }

    public static void setDialogLocked(long dialogId, boolean locked) {
        updateIdSet(KEY_LOCKED_DIALOGS, dialogId, locked);
    }

    public static Set<String> getHiddenDialogs() {
        return getIdSet(KEY_HIDDEN_DIALOGS);
    }

    public static Set<String> getLockedDialogs() {
        return getIdSet(KEY_LOCKED_DIALOGS);
    }

    private static Set<String> getIdSet(String key) {
        Set<String> set = prefs().getStringSet(key, null);
        if (set == null || set.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(set);
    }

    private static void updateIdSet(String key, long dialogId, boolean enabled) {
        Set<String> set = new HashSet<>(prefs().getStringSet(key, Collections.emptySet()));
        String value = String.valueOf(dialogId);
        if (enabled) {
            set.add(value);
        } else {
            set.remove(value);
        }
        prefs().edit().putStringSet(key, set).apply();
    }
}
