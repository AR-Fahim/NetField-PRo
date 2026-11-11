package com.netfieldpro.core.entitlement;

import java.util.Objects;

public final class QuotaPolicy {

    private QuotaPolicy() {
    }

    public static int limit(Tier tier, Resource resource) {
        Objects.requireNonNull(tier, "tier cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        if (tier == Tier.FREE) {
            return switch (resource) {
                case PROJECT -> 1;
                case FLOOR -> 1;
                case SESSION -> 2;
            };
        }
        return -1;
    }

    public static QuotaDecision canCreate(Tier tier, Resource resource, int currentCount) {
        if (currentCount < 0) {
            throw new IllegalArgumentException("currentCount cannot be negative: " + currentCount);
        }
        int lim = limit(tier, resource);
        if (lim == -1 || currentCount < lim) {
            return new QuotaDecision(true, lim, null);
        } else {
            return new QuotaDecision(false, lim, Tier.PRO);
        }
    }
}
