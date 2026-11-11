package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.ApGroup;
import com.netfieldpro.core.model.enums.ApGroupOrigin;
import com.netfieldpro.data.db.entity.ApGroupRow;

public final class ApGroupMapper {

    private ApGroupMapper() {
    }

    public static ApGroupRow toRow(ApGroup a) {
        ApGroupRow row = new ApGroupRow();
        row.id = a.id();
        row.sessionId = a.sessionId();
        row.label = a.label();
        row.origin = a.origin().name();

        JsonArray arr = new JsonArray();
        for (String b : a.memberBssids()) {
            arr.add(new JsonString(b));
        }
        row.memberBssids = new String(CanonicalJsonWriter.toBytes(arr), StandardCharsets.UTF_8);
        return row;
    }

    public static ApGroup toDomain(ApGroupRow row) {
        try {
            JsonArray arr = (JsonArray) JsonParser.parse(row.memberBssids.getBytes(StandardCharsets.UTF_8));
            List<String> bssids = new ArrayList<>();
            for (JsonValue v : arr.elements()) {
                bssids.add(((JsonString) v).value());
            }
            return new ApGroup(row.id, row.sessionId, row.label, ApGroupOrigin.valueOf(row.origin), bssids);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping ApGroupRow to domain", e);
        }
    }
}

