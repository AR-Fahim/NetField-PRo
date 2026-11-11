package com.netfieldpro.onboarding;

import android.content.SharedPreferences;

import com.netfieldpro.core.permissions.PermissionKey;

public class OnboardingStore {

    public static final String PREFS_NAME = "nf_onboarding";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";
    public static final String KEY_ASKED_LOCATION = "asked_location";
    public static final String KEY_ASKED_NOTIFICATIONS = "asked_notifications";

    private final SharedPreferences prefs;
    private final boolean resetAllowed;

    public OnboardingStore(SharedPreferences prefs, boolean resetAllowed) {
        this.prefs = prefs;
        this.resetAllowed = resetAllowed;
    }

    public boolean isDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void markDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public boolean wasAsked(PermissionKey key) {
        if (key == PermissionKey.LOCATION) {
            return prefs.getBoolean(KEY_ASKED_LOCATION, false);
        } else if (key == PermissionKey.NOTIFICATIONS) {
            return prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false);
        }
        return false;
    }

    public void markAsked(PermissionKey key) {
        if (key == PermissionKey.LOCATION) {
            prefs.edit().putBoolean(KEY_ASKED_LOCATION, true).apply();
        } else if (key == PermissionKey.NOTIFICATIONS) {
            prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply();
        }
    }

    public void reset() {
        if (!resetAllowed) {
            throw new IllegalStateException("Reset is not allowed");
        }
        prefs.edit()
                .remove(KEY_ONBOARDING_DONE)
                .remove(KEY_ASKED_LOCATION)
                .remove(KEY_ASKED_NOTIFICATIONS)
                .apply();
    }
}

