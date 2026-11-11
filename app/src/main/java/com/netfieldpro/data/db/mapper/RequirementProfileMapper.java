package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.RequirementProfile;
import com.netfieldpro.core.model.enums.BandPolicy;
import com.netfieldpro.core.model.enums.ProfileKind;
import com.netfieldpro.data.db.entity.RequirementProfileRow;

public final class RequirementProfileMapper {

    private RequirementProfileMapper() {
    }

    public static RequirementProfileRow toRow(RequirementProfile r) {
        RequirementProfileRow row = new RequirementProfileRow();
        row.id = r.id();
        row.kind = r.kind().name();
        row.thresholds = new String(CanonicalJsonWriter.toBytes(r.thresholds()), StandardCharsets.UTF_8);
        row.bandPolicy = r.bandPolicy().name();
        row.maxInfluenceM = r.maxInfluenceM();

        JsonObject ceObj = new JsonObject();
        for (Map.Entry<String, Boolean> e : r.criterionEnabled().entrySet()) {
            ceObj.put(e.getKey(), JsonBool.of(e.getValue()));
        }
        row.criterionEnabled = new String(CanonicalJsonWriter.toBytes(ceObj), StandardCharsets.UTF_8);
        return row;
    }

    public static RequirementProfile toDomain(RequirementProfileRow row) {
        try {
            JsonObject th = (JsonObject) JsonParser.parse(row.thresholds.getBytes(StandardCharsets.UTF_8));
            JsonObject ce = (JsonObject) JsonParser.parse(row.criterionEnabled.getBytes(StandardCharsets.UTF_8));
            Map<String, Boolean> map = new TreeMap<>();
            for (Map.Entry<String, JsonValue> e : ce.properties().entrySet()) {
                map.put(e.getKey(), ((JsonBool) e.getValue()).value());
            }
            return new RequirementProfile(
                    row.id, ProfileKind.valueOf(row.kind), th,
                    BandPolicy.valueOf(row.bandPolicy), row.maxInfluenceM, map
            );
        } catch (Exception e) {
            throw new RuntimeException("Error mapping RequirementProfileRow to domain", e);
        }
    }
}

