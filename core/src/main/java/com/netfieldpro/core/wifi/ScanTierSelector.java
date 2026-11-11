package com.netfieldpro.core.wifi;

public final class ScanTierSelector {
    public static final double TIER_PROBE_TIMEOUT_SEC = 12.0;

    private Tier currentTier = Tier.CALLBACK;
    private boolean confirmed = false;
    private double watchStart = -1.0;

    public ScanTierSelector() {
    }

    public static ScanTierSelector startingAt(Tier tier) {
        ScanTierSelector selector = new ScanTierSelector();
        selector.currentTier = tier;
        if (tier == Tier.POLL) {
            selector.confirmed = true;
        }
        return selector;
    }

    public synchronized Tier getTier() {
        return currentTier;
    }

    public synchronized boolean isConfirmed() {
        return confirmed;
    }

    public synchronized void requestOk(double t) {
        if (watchStart < 0) {
            watchStart = t;
        }
    }

    public synchronized void tierBatch(double t) {
        confirmed = true;
    }

    public synchronized void pollAdvanced(double t) {
        if (!confirmed && watchStart >= 0) {
            if (t >= watchStart + TIER_PROBE_TIMEOUT_SEC - 1e-9) {
                demote();
                watchStart = t;
            }
        }
    }

    public synchronized void registerFailed() {
        demote();
    }

    private void demote() {
        if (confirmed) {
            return;
        }
        if (currentTier == Tier.CALLBACK) {
            currentTier = Tier.RECEIVER;
        } else if (currentTier == Tier.RECEIVER) {
            currentTier = Tier.POLL;
        }
    }
}
