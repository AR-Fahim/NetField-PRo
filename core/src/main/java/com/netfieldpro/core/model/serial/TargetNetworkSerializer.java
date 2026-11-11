package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.TargetNetwork;

public final class TargetNetworkSerializer {

    private TargetNetworkSerializer() {
    }

    public static JsonObject toJson(TargetNetwork t) {
        JsonObject obj = new JsonObject();
        obj.put("id", new JsonString(t.id()));
        obj.put("sessionId", new JsonString(t.sessionId()));
        obj.put("ssid", new JsonString(t.ssid()));
        return obj;
    }

    public static TargetNetwork fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        String ssid = ((JsonString) obj.get("ssid")).value();
        return new TargetNetwork(id, sessionId, ssid);
    }
}

