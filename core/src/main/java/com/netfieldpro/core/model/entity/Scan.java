package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.SweepClass;

public record Scan(
        String id,
        String sessionId,
        String pointId,
        SweepClass sweepClass,
        long startNs,
        long endNs,
        long arrivalNs,
        boolean tsPlausible,
        boolean throttleOffAtScan
) {
    public Scan {
        ModelValidator.checkId("Scan", "id", id);
        ModelValidator.checkId("Scan", "sessionId", sessionId);
        ModelValidator.checkOptionalId("Scan", "pointId", pointId);
        ModelValidator.checkRequired("Scan", "sweepClass", sweepClass);
        ModelValidator.checkNonNegative("Scan", "startNs", startNs);
        ModelValidator.checkNonNegative("Scan", "endNs", endNs);
        ModelValidator.checkNonNegative("Scan", "arrivalNs", arrivalNs);
    }
}

