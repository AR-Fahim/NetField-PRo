package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.WallMaterial;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.util.FixedDecimal;

public final class WallMaterialSerializer {

    private WallMaterialSerializer() {
    }

    public static JsonObject toJson(WallMaterial w) {
        JsonObject obj = new JsonObject();
        obj.put("lossDb24", new JsonString(FixedDecimal.format(w.lossDb24(), 1)));
        obj.put("lossDb5", new JsonString(FixedDecimal.format(w.lossDb5(), 1)));
        obj.put("materialId", new JsonString(w.materialId().name()));
        obj.put("penaltyM", new JsonString(FixedDecimal.format(w.penaltyM(), 1)));
        return obj;
    }

    public static WallMaterial fromJson(JsonObject obj) {
        MaterialId materialId = MaterialId.valueOf(((JsonString) obj.get("materialId")).value());
        double penaltyM = DecimalFields.parseDouble(obj.get("penaltyM"), "penaltyM", 1);
        double lossDb24 = DecimalFields.parseDouble(obj.get("lossDb24"), "lossDb24", 1);
        double lossDb5 = DecimalFields.parseDouble(obj.get("lossDb5"), "lossDb5", 1);
        return new WallMaterial(materialId, penaltyM, lossDb24, lossDb5);
    }
}

