package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;

public record SurveyPoint(
        String id,
        String sessionId,
        double x,
        double y,
        PointSource source,
        PointQuality quality,
        long tMonoNs,
        long tWallMs,
        int nScans,
        double positionUncertaintyM,
        String segmentId
) {
    public SurveyPoint {
        ModelValidator.checkId("SurveyPoint", "id", id);
        ModelValidator.checkId("SurveyPoint", "sessionId", sessionId);
        ModelValidator.checkDouble("SurveyPoint", "x", x);
        ModelValidator.checkDouble("SurveyPoint", "y", y);
        ModelValidator.checkRequired("SurveyPoint", "source", source);
        ModelValidator.checkRequired("SurveyPoint", "quality", quality);
        ModelValidator.checkNonNegative("SurveyPoint", "tMonoNs", tMonoNs);
        ModelValidator.checkNonNegative("SurveyPoint", "tWallMs", tWallMs);
        ModelValidator.checkNonNegative("SurveyPoint", "nScans", nScans);
        ModelValidator.checkDouble("SurveyPoint", "positionUncertaintyM", positionUncertaintyM);
        if (positionUncertaintyM < 0.0) {
            throw new IllegalArgumentException("SurveyPoint.positionUncertaintyM: must be >= 0: " + positionUncertaintyM);
        }
        ModelValidator.checkOptionalId("SurveyPoint", "segmentId", segmentId);
    }
}

