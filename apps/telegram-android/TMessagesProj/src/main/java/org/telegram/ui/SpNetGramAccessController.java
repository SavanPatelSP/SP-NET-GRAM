package org.telegram.ui;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SpNetGramApi;
import org.telegram.messenger.SpNetGramConfig;

public class SpNetGramAccessController {

    private static boolean checkInProgress = false;
    private static boolean gateShown = false;
    private static boolean timerScheduled = false;
    private static final long REFRESH_INTERVAL_MS = 15 * 60 * 1000L;

    private static final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            LaunchActivity activity = LaunchActivity.instance;
            if (activity != null && LaunchActivity.isActive) {
                ensureAccess(activity);
            }
            AndroidUtilities.runOnUIThread(this, REFRESH_INTERVAL_MS);
        }
    };

    public static void ensureAccess(LaunchActivity activity) {
        if (activity == null || checkInProgress || gateShown) {
            return;
        }
        String token = SpNetGramConfig.getBackendToken();
        if (TextUtils.isEmpty(token)) {
            showGate(activity);
            return;
        }
        checkInProgress = true;
        SpNetGramApi.accessStatus(token, json -> {
            checkInProgress = false;
            if (json == null) {
                showGate(activity);
                return;
            }
            boolean canUse = json.optBoolean("canUse", false);
            if (!canUse) {
                showGate(activity);
            }
        });
    }

    private static void showGate(LaunchActivity activity) {
        if (gateShown || activity == null) {
            return;
        }
        gateShown = true;
        SpNetGramLicenseGateActivity gate = new SpNetGramLicenseGateActivity(true).setOnDismiss(() -> gateShown = false);
        activity.presentFragment(gate, false, true);
    }

    public static void startPeriodicChecks() {
        if (timerScheduled) {
            return;
        }
        timerScheduled = true;
        AndroidUtilities.runOnUIThread(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    public static void stopPeriodicChecks() {
        if (!timerScheduled) {
            return;
        }
        timerScheduled = false;
        AndroidUtilities.cancelRunOnUIThread(refreshRunnable);
    }
}
