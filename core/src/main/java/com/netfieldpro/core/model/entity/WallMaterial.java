package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.MaterialId;

public record WallMaterial(
        MaterialId materialId,
        double penaltyM,
        double lossDb24,
        double lossDb5
) {
    public WallMaterial {
        ModelValidator.checkRequired("WallMaterial", "materialId", materialId);
        ModelValidator.checkDouble("WallMaterial", "penaltyM", penaltyM);
        if (penaltyM < 0.0) {
            throw new IllegalArgumentException("WallMaterial.penaltyM: must be >= 0: " + penaltyM);
        }
        ModelValidator.checkDouble("WallMaterial", "lossDb24", lossDb24);
        if (lossDb24 < 0.0) {
            throw new IllegalArgumentException("WallMaterial.lossDb24: must be >= 0: " + lossDb24);
        }
        ModelValidator.checkDouble("WallMaterial", "lossDb5", lossDb5);
        if (lossDb5 < 0.0) {
            throw new IllegalArgumentException("WallMaterial.lossDb5: must be >= 0: " + lossDb5);
        }
    }
}

