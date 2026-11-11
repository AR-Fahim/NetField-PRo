package com.netfieldpro.data.db.mapper;

import com.netfieldpro.core.model.entity.WallMaterial;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.data.db.entity.WallMaterialRow;

public final class WallMaterialMapper {

    private WallMaterialMapper() {
    }

    public static WallMaterialRow toRow(WallMaterial w) {
        WallMaterialRow row = new WallMaterialRow();
        row.materialId = w.materialId().name();
        row.penaltyM = w.penaltyM();
        row.lossDb24 = w.lossDb24();
        row.lossDb5 = w.lossDb5();
        return row;
    }

    public static WallMaterial toDomain(WallMaterialRow row) {
        return new WallMaterial(
                MaterialId.valueOf(row.materialId),
                row.penaltyM,
                row.lossDb24,
                row.lossDb5
        );
    }
}

