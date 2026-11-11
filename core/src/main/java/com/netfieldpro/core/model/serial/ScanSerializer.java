package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Scan;
import com.netfieldpro.core.model.enums.SweepClass;

public final class ScanSerializer {

    private ScanSerializer() {
    }

    public static JsonObject toJson(Scan s) {
        JsonObject obj = new JsonObject();
        obj.put("arrivalNs", new JsonInt(s.arrivalNs()));
        obj.put("endNs", new JsonInt(s.endNs()));
        obj.put("id", new JsonString(s.id()));
        if (s.pointId() != null) {
            obj.put("pointId", new JsonString(s.pointId()));
        }
        obj.put("sessionId", new JsonString(s.sessionId()));
        obj.put("startNs", new JsonInt(s.startNs()));
        obj.put("sweepClass", new JsonString(s.sweepClass().name()));
        obj.put("throttleOffAtScan", JsonBool.of(s.throttleOffAtScan()));
        obj.put("tsPlausible", JsonBool.of(s.tsPlausible()));
        return obj;
    }

    public static Scan fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        String pointId = obj.get("pointId") instanceof JsonString s ? s.value() : null;
        SweepClass sweepClass = SweepClass.valueOf(((JsonString) obj.get("sweepClass")).value());
        long startNs = IntFields.parseLong(obj.get("startNs"), "startNs");
        long endNs = IntFields.parseLong(obj.get("endNs"), "endNs");
        long arrivalNs = IntFields.parseLong(obj.get("arrivalNs"), "arrivalNs");
        boolean tsPlausible = ((JsonBool) obj.get("tsPlausible")).value();
        boolean throttleOffAtScan = ((JsonBool) obj.get("throttleOffAtScan")).value();
        return new Scan(id, sessionId, pointId, sweepClass, startNs, endNs, arrivalNs, tsPlausible, throttleOffAtScan);
    }
}
