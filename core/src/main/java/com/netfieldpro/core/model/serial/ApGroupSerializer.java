package com.netfieldpro.core.model.serial;

import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.ApGroup;
import com.netfieldpro.core.model.enums.ApGroupOrigin;

public final class ApGroupSerializer {

    private ApGroupSerializer() {
    }

    public static JsonObject toJson(ApGroup a) {
        JsonObject obj = new JsonObject();
        obj.put("id", new JsonString(a.id()));
        obj.put("label", new JsonString(a.label()));

        JsonArray arr = new JsonArray();
        for (String bssid : a.memberBssids()) {
            arr.add(new JsonString(bssid));
        }
        obj.put("memberBssids", arr);

        obj.put("origin", new JsonString(a.origin().name()));
        obj.put("sessionId", new JsonString(a.sessionId()));
        return obj;
    }

    public static ApGroup fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        String label = ((JsonString) obj.get("label")).value();
        ApGroupOrigin origin = ApGroupOrigin.valueOf(((JsonString) obj.get("origin")).value());

        JsonArray arr = (JsonArray) obj.get("memberBssids");
        List<String> memberBssids = new ArrayList<>();
        for (JsonValue v : arr.elements()) {
            memberBssids.add(((JsonString) v).value());
        }

        return new ApGroup(id, sessionId, label, origin, memberBssids);
    }
}

