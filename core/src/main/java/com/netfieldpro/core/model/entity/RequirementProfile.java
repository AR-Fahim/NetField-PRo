package com.netfieldpro.core.model.entity;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.BandPolicy;
import com.netfieldpro.core.model.enums.ProfileKind;

public record RequirementProfile(
        String id,
        ProfileKind kind,
        JsonObject thresholds,
        BandPolicy bandPolicy,
        double maxInfluenceM,
        Map<String, Boolean> criterionEnabled
) {
    public RequirementProfile {
        ModelValidator.checkId("RequirementProfile", "id", id);
        ModelValidator.checkRequired("RequirementProfile", "kind", kind);
        ModelValidator.checkRequired("RequirementProfile", "thresholds", thresholds);
        ModelValidator.checkRequired("RequirementProfile", "bandPolicy", bandPolicy);
        ModelValidator.checkDouble("RequirementProfile", "maxInfluenceM", maxInfluenceM);
        if (maxInfluenceM < 0.0) {
            throw new IllegalArgumentException("RequirementProfile.maxInfluenceM: must be >= 0: " + maxInfluenceM);
        }
        ModelValidator.checkRequired("RequirementProfile", "criterionEnabled", criterionEnabled);
        for (Map.Entry<String, Boolean> e : criterionEnabled.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                throw new IllegalArgumentException("RequirementProfile.criterionEnabled: entry key/value cannot be null");
            }
        }
        criterionEnabled = Collections.unmodifiableMap(new TreeMap<>(criterionEnabled));
    }
}

