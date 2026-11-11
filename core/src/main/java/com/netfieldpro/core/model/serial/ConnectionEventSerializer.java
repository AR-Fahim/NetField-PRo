package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.ConnectionEvent;
import com.netfieldpro.core.model.enums.ConnectionEventType;

public final class ConnectionEventSerializer {

    private ConnectionEventSerializer() {
    }

    public static JsonObject toJson(ConnectionEvent c) {
        JsonObject obj = new JsonObject();
        obj.put("atNs", new JsonInt(c.atNs()));
        if (c.fromBssid() != null) {
            obj.put("fromBssid", new JsonString(c.fromBssid()));
        }
        obj.put("id", new JsonString(c.id()));
        if (c.pointId() != null) {
            obj.put("pointId", new JsonString(c.pointId()));
        }
        obj.put("sessionId", new JsonString(c.sessionId()));
        if (c.toBssid() != null) {
            obj.put("toBssid", new JsonString(c.toBssid()));
        }
        obj.put("type", new JsonString(c.type().name()));
        return obj;
    }

    public static ConnectionEvent fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        ConnectionEventType type = ConnectionEventType.valueOf(((JsonString) obj.get("type")).value());
        long atNs = IntFields.parseLong(obj.get("atNs"), "atNs");
        String fromBssid = obj.get("fromBssid") instanceof JsonString s ? s.value() : null;
        String toBssid = obj.get("toBssid") instanceof JsonString s ? s.value() : null;
        String pointId = obj.get("pointId") instanceof JsonString s ? s.value() : null;
        return new ConnectionEvent(id, sessionId, type, atNs, fromBssid, toBssid, pointId);
    }
}
