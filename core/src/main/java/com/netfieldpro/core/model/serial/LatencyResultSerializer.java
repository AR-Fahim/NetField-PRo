package com.netfieldpro.core.model.serial;

import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.util.FixedDecimal;

public final class LatencyResultSerializer {

    private LatencyResultSerializer() {
    }

    public static JsonObject toJson(LatencyResult lr) {
        JsonObject obj = new JsonObject();
        if (lr.avgMs() != null) {
            obj.put("avgMs", new JsonString(FixedDecimal.format(lr.avgMs(), 3)));
        }
        obj.put("boundIface", new JsonString(lr.boundIface()));
        obj.put("connectedBssid", new JsonString(lr.connectedBssid()));
        obj.put("duringScan", JsonBool.of(lr.duringScan()));
        obj.put("id", new JsonString(lr.id()));
        if (lr.jitterMs() != null) {
            obj.put("jitterMs", new JsonString(FixedDecimal.format(lr.jitterMs(), 3)));
        }
        obj.put("lossCount", new JsonInt(lr.lossCount()));
        if (lr.maxMs() != null) {
            obj.put("maxMs", new JsonString(FixedDecimal.format(lr.maxMs(), 3)));
        }
        obj.put("method", new JsonString(lr.method().name()));
        if (lr.minMs() != null) {
            obj.put("minMs", new JsonString(FixedDecimal.format(lr.minMs(), 3)));
        }
        obj.put("pointId", new JsonString(lr.pointId()));
        obj.put("recv", new JsonInt(lr.recv()));

        JsonArray rttsArray = new JsonArray();
        for (Double rtt : lr.rtts()) {
            rttsArray.add(new JsonString(FixedDecimal.format(rtt, 3)));
        }
        obj.put("rtts", rttsArray);

        obj.put("scope", new JsonString(lr.scope().name()));
        obj.put("sent", new JsonInt(lr.sent()));
        obj.put("ssid", new JsonString(lr.ssid()));
        obj.put("target", new JsonString(lr.target()));
        return obj;
    }

    public static LatencyResult fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String pointId = ((JsonString) obj.get("pointId")).value();
        LatencyMethod method = LatencyMethod.valueOf(((JsonString) obj.get("method")).value());
        String target = ((JsonString) obj.get("target")).value();
        String boundIface = ((JsonString) obj.get("boundIface")).value();
        LatencyScope scope = LatencyScope.valueOf(((JsonString) obj.get("scope")).value());
        int sent = IntFields.parseInt(obj.get("sent"), "sent");
        int recv = IntFields.parseInt(obj.get("recv"), "recv");
        Double minMs = DecimalFields.parseOptionalDouble(obj.get("minMs"), "minMs", 3);
        Double avgMs = DecimalFields.parseOptionalDouble(obj.get("avgMs"), "avgMs", 3);
        Double maxMs = DecimalFields.parseOptionalDouble(obj.get("maxMs"), "maxMs", 3);
        Double jitterMs = DecimalFields.parseOptionalDouble(obj.get("jitterMs"), "jitterMs", 3);
        int lossCount = IntFields.parseInt(obj.get("lossCount"), "lossCount");
        boolean duringScan = ((JsonBool) obj.get("duringScan")).value();
        String connectedBssid = ((JsonString) obj.get("connectedBssid")).value();
        String ssid = ((JsonString) obj.get("ssid")).value();

        JsonArray rttsArray = (JsonArray) obj.get("rtts");
        List<Double> rtts = new ArrayList<>();
        for (JsonValue v : rttsArray.elements()) {
            rtts.add(DecimalFields.parseDouble(v, "rtts", 3));
        }

        return new LatencyResult(id, pointId, method, target, boundIface, scope, sent, recv, minMs, avgMs, maxMs, jitterMs, lossCount, duringScan, connectedBssid, ssid, rtts);
    }
}

