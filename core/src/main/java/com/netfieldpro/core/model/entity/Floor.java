package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.ScaleMethod;

public record Floor(
        String id,
        String projectId,
        long levelIndex,
        String planSha256,
        int planWpx,
        int planHpx,
        double metersPerPixel,
        Double northOffsetDeg,
        ScaleMethod scaleMethod,
        double scaleUncertaintyPct,
        boolean scaleChecked,
        Double alignX,
        Double alignY,
        Double alignRotDeg,
        Point scaleP1,
        Point scaleP2
) {
    public record Point(double x, double y) {
        public Point {
            ModelValidator.checkDouble("Floor.Point", "x", x);
            ModelValidator.checkDouble("Floor.Point", "y", y);
        }
    }

    public Floor {
        ModelValidator.checkId("Floor", "id", id);
        ModelValidator.checkId("Floor", "projectId", projectId);
        ModelValidator.checkRequired("Floor", "planSha256", planSha256);
        ModelValidator.checkString("Floor", "planSha256", planSha256, 1024);
        ModelValidator.checkNonNegative("Floor", "planWpx", planWpx);
        ModelValidator.checkNonNegative("Floor", "planHpx", planHpx);
        ModelValidator.checkPositive("Floor", "metersPerPixel", metersPerPixel);
        ModelValidator.checkOptionalDouble("Floor", "northOffsetDeg", northOffsetDeg);
        ModelValidator.checkRequired("Floor", "scaleMethod", scaleMethod);
        ModelValidator.checkDouble("Floor", "scaleUncertaintyPct", scaleUncertaintyPct);
        if (scaleUncertaintyPct < 0.0) {
            throw new IllegalArgumentException("Floor.scaleUncertaintyPct: must be >= 0: " + scaleUncertaintyPct);
        }
        ModelValidator.checkOptionalDouble("Floor", "alignX", alignX);
        ModelValidator.checkOptionalDouble("Floor", "alignY", alignY);
        ModelValidator.checkOptionalDouble("Floor", "alignRotDeg", alignRotDeg);
    }
}

