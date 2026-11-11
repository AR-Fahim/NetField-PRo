package com.netfieldpro.core.entitlement;

import java.util.Objects;

public final class EntitlementPolicy {

    private static final EntitlementDecision[][] MATRIX = new EntitlementDecision[Feature.values().length][Tier.values().length];

    static {
        EntitlementDecision a = EntitlementDecision.allowed();
        EntitlementDecision lp = EntitlementDecision.locked(Tier.PRO);
        EntitlementDecision le = EntitlementDecision.locked(Tier.ENTERPRISE);
        EntitlementDecision n = EntitlementDecision.notApplicable();

        // 0..12: TAP_CAPTURE through SHARE_EXISTING_REPORT
        for (int f = 0; f <= 12; f++) {
            MATRIX[f][Tier.FREE.ordinal()] = a;
            MATRIX[f][Tier.PRO.ordinal()] = a;
            MATRIX[f][Tier.ENTERPRISE.ordinal()] = a;
        }

        // 13: WATERMARKED_PDF
        MATRIX[Feature.WATERMARKED_PDF.ordinal()][Tier.FREE.ordinal()] = a;
        MATRIX[Feature.WATERMARKED_PDF.ordinal()][Tier.PRO.ordinal()] = n;
        MATRIX[Feature.WATERMARKED_PDF.ordinal()][Tier.ENTERPRISE.ordinal()] = n;

        // 14..24: VERDICT_PREVIEW to RE_ASSESSMENTS
        for (int f = Feature.VERDICT_PREVIEW.ordinal(); f <= Feature.RE_ASSESSMENTS.ordinal(); f++) {
            MATRIX[f][Tier.FREE.ordinal()] = lp;
            MATRIX[f][Tier.PRO.ordinal()] = a;
            MATRIX[f][Tier.ENTERPRISE.ordinal()] = a;
        }

        // 25..26: ROAMING_AUDIT, PRESET_WAREHOUSE
        MATRIX[Feature.ROAMING_AUDIT.ordinal()][Tier.FREE.ordinal()] = le;
        MATRIX[Feature.ROAMING_AUDIT.ordinal()][Tier.PRO.ordinal()] = le;
        MATRIX[Feature.ROAMING_AUDIT.ordinal()][Tier.ENTERPRISE.ordinal()] = a;

        MATRIX[Feature.PRESET_WAREHOUSE.ordinal()][Tier.FREE.ordinal()] = le;
        MATRIX[Feature.PRESET_WAREHOUSE.ordinal()][Tier.PRO.ordinal()] = le;
        MATRIX[Feature.PRESET_WAREHOUSE.ordinal()][Tier.ENTERPRISE.ordinal()] = a;
    }

    private EntitlementPolicy() {
    }

    public static EntitlementDecision decide(Tier tier, Feature feature) {
        Objects.requireNonNull(tier, "tier cannot be null");
        Objects.requireNonNull(feature, "feature cannot be null");
        return MATRIX[feature.ordinal()][tier.ordinal()];
    }
}
