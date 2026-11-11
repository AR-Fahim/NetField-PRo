package com.netfieldpro.core.entitlement;

import java.util.Objects;

public final class EntitlementDecision {

    private static final EntitlementDecision ALLOWED_INSTANCE = new EntitlementDecision(DecisionKind.ALLOWED, null);
    private static final EntitlementDecision NOT_APPLICABLE_INSTANCE = new EntitlementDecision(DecisionKind.NOT_APPLICABLE, null);
    private static final EntitlementDecision LOCKED_PRO_INSTANCE = new EntitlementDecision(DecisionKind.LOCKED, Tier.PRO);
    private static final EntitlementDecision LOCKED_ENTERPRISE_INSTANCE = new EntitlementDecision(DecisionKind.LOCKED, Tier.ENTERPRISE);

    private final DecisionKind kind;
    private final Tier requiredTier;

    private EntitlementDecision(DecisionKind kind, Tier requiredTier) {
        this.kind = Objects.requireNonNull(kind);
        this.requiredTier = requiredTier;
    }

    public static EntitlementDecision allowed() {
        return ALLOWED_INSTANCE;
    }

    public static EntitlementDecision locked(Tier requiredTier) {
        Objects.requireNonNull(requiredTier, "requiredTier cannot be null for LOCKED");
        if (requiredTier == Tier.PRO) {
            return LOCKED_PRO_INSTANCE;
        } else if (requiredTier == Tier.ENTERPRISE) {
            return LOCKED_ENTERPRISE_INSTANCE;
        }
        return new EntitlementDecision(DecisionKind.LOCKED, requiredTier);
    }

    public static EntitlementDecision notApplicable() {
        return NOT_APPLICABLE_INSTANCE;
    }

    public DecisionKind kind() {
        return kind;
    }

    public Tier requiredTier() {
        return requiredTier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntitlementDecision that = (EntitlementDecision) o;
        return kind == that.kind && requiredTier == that.requiredTier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, requiredTier);
    }

    @Override
    public String toString() {
        if (kind == DecisionKind.LOCKED) {
            return "LOCKED(" + requiredTier + ")";
        }
        return kind.name();
    }
}
