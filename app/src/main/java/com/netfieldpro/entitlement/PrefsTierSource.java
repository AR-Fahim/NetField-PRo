package com.netfieldpro.entitlement;

import android.content.SharedPreferences;

import com.netfieldpro.core.entitlement.Tier;

public class PrefsTierSource implements TierSource {

    public static final String PREF_NAME = "nf_entitlement";
    public static final String KEY_DEBUG_TIER = "debug_tier";

    private final SharedPreferences prefs;
    private final boolean debugOverrideAllowed;

    public PrefsTierSource(SharedPreferences prefs, boolean debugOverrideAllowed) {
        this.prefs = prefs;
        this.debugOverrideAllowed = debugOverrideAllowed;
    }

    @Override
    public Tier current() {
        if (!debugOverrideAllowed) {
            return Tier.FREE;
        }
        String stored = prefs.getString(KEY_DEBUG_TIER, null);
        if (stored == null) {
            return Tier.FREE;
        }
        try {
            return Tier.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return Tier.FREE;
        }
    }

    public void setDebugTier(Tier tier) {
        if (!debugOverrideAllowed) {
            throw new IllegalStateException("Debug override is not allowed in release builds");
        }
        if (tier == null) {
            prefs.edit().remove(KEY_DEBUG_TIER).apply();
        } else {
            prefs.edit().putString(KEY_DEBUG_TIER, tier.name()).apply();
        }
    }
}
