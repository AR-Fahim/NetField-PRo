package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.Verdict;

public record Assessment(
        String assessmentId,
        String evidenceSha256,
        String profileId,
        JsonObject profileSnapshot,
        String intendedProfileId,
        boolean isReassessment,
        JsonObject thresholds,
        JsonObject computedMetrics,
        JsonArray criteria,
        Verdict verdict,
        long assessedAtMs,
        String algoVersion,
        double gridG,
        String publicKey,
        String sig
) {
    public Assessment {
        ModelValidator.checkId("Assessment", "assessmentId", assessmentId);
        ModelValidator.checkRequired("Assessment", "evidenceSha256", evidenceSha256);
        ModelValidator.checkString("Assessment", "evidenceSha256", evidenceSha256, 1024);
        ModelValidator.checkId("Assessment", "profileId", profileId);
        ModelValidator.checkRequired("Assessment", "profileSnapshot", profileSnapshot);
        ModelValidator.checkId("Assessment", "intendedProfileId", intendedProfileId);
        ModelValidator.checkRequired("Assessment", "thresholds", thresholds);
        ModelValidator.checkRequired("Assessment", "computedMetrics", computedMetrics);
        ModelValidator.checkRequired("Assessment", "criteria", criteria);
        ModelValidator.checkRequired("Assessment", "verdict", verdict);
        ModelValidator.checkNonNegative("Assessment", "assessedAtMs", assessedAtMs);
        ModelValidator.checkRequired("Assessment", "algoVersion", algoVersion);
        ModelValidator.checkString("Assessment", "algoVersion", algoVersion, 1024);
        ModelValidator.checkPositive("Assessment", "gridG", gridG);
        ModelValidator.checkRequired("Assessment", "publicKey", publicKey);
        if (publicKey.length() != 44) {
            throw new IllegalArgumentException("Assessment.publicKey: must be 44 chars: " + publicKey.length());
        }
        ModelValidator.checkRequired("Assessment", "sig", sig);
        if (sig.length() != 86) {
            throw new IllegalArgumentException("Assessment.sig: must be 86 chars: " + sig.length());
        }
    }
}

