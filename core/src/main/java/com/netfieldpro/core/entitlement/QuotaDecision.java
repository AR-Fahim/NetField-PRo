package com.netfieldpro.core.entitlement;

public record QuotaDecision(
        boolean allowed,
        int limit,
        Tier upgradeTo
) {
}
