package com.netfieldpro.core.model.serial;

import java.util.Map;
import java.util.TreeMap;

import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.enums.BandPolicy;
import com.netfieldpro.core.model.enums.ProfileKind;
import com.netfieldpro.core.util.FixedDecimal;

public final class RequirementProfileSerializer {

    private RequirementProfileSerializer() {
    }

    public static JsonObject toJson(RequirementProfile r) {
        JsonObject obj = new JsonObject();
        obj.put("bandPolicy", new JsonString(r.bandPolicy().name()));

        JsonObject ceObj = new JsonObject();
        for (Map.Entry<String, Boolean> e : r.criterionEnabled().entrySet()) {
            ceObj.put(e.getKey(), JsonBool.of(e.getValue()));
        }
        obj.put("criterionEnabled", ceObj);

        obj.put("id", new JsonString(r.id()));
        obj.put("kind", new JsonString(r.kind().name()));
        obj.put("maxInfluenceM", new JsonString(FixedDecimal.format(r.maxInfluenceM(), 1)));
        obj.put("thresholds", r.thresholds());
        return obj;
    }

    public static RequirementProfile fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        ProfileKind kind = ProfileKind.valueOf(((JsonString) obj.get("kind")).value());
        JsonObject thresholds = (JsonObject) obj.get("thresholds");
        BandPolicy bandPolicy = BandPolicy.valueOf(((JsonString) obj.get("bandPolicy")).value());
        double maxInfluenceM = DecimalFields.parseDouble(obj.get("maxInfluenceM"), "maxInfluenceM", 1);

        JsonObject ceObj = (JsonObject) obj.get("criterionEnabled");
        Map<String, Boolean> criterionEnabled = new TreeMap<>();
        for (Map.Entry<String, JsonValue> e : ceObj.properties().entrySet()) {
            criterionEnabled.put(e.getKey(), ((JsonBool) e.getValue()).value());
        }

        return new RequirementProfile(id, kind, thresholds, bandPolicy, maxInfluenceM, criterionEnabled);
    }
}

