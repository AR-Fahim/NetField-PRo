package com.netfieldpro.core.model;

import java.util.List;

import com.netfieldpro.core.model.entity.WallMaterial;
import com.netfieldpro.core.model.enums.MaterialId;

public final class WallMaterials {

    public static final WallMaterial DRYWALL_LIGHT =
            new WallMaterial(MaterialId.DRYWALL_LIGHT, 3.0, 3.0, 4.5);
    public static final WallMaterial GLASS_PARTITION =
            new WallMaterial(MaterialId.GLASS_PARTITION, 2.5, 2.5, 4.0);
    public static final WallMaterial BRICK_MEDIUM =
            new WallMaterial(MaterialId.BRICK_MEDIUM, 8.0, 8.0, 12.0);
    public static final WallMaterial CONCRETE_HEAVY =
            new WallMaterial(MaterialId.CONCRETE_HEAVY, 15.0, 12.0, 18.0);
    public static final WallMaterial METAL_ELEVATOR =
            new WallMaterial(MaterialId.METAL_ELEVATOR, 25.0, 20.0, 28.0);

    public static final List<WallMaterial> ALL = List.of(
            DRYWALL_LIGHT,
            GLASS_PARTITION,
            BRICK_MEDIUM,
            CONCRETE_HEAVY,
            METAL_ELEVATOR
    );

    private WallMaterials() {
    }
}

