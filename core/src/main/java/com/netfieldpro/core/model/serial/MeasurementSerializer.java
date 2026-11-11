package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.core.util.FixedDecimal;

public final class MeasurementSerializer {

    private MeasurementSerializer() {
    }

    public static JsonObject toJson(Measurement m) {
        JsonObject obj = new JsonObject();
        obj.put("bssidOrId", new JsonString(m.bssidOrId()));
        obj.put("channel", new JsonInt(m.channel()));
        obj.put("freqMHz", new JsonInt(m.freqMHz()));
        obj.put("id", new JsonString(m.id()));
        if (m.lqi() != null) {
            obj.put("lqi", new JsonInt(m.lqi()));
        }
        obj.put("n", new JsonInt(m.n()));
        if (m.panId() != null) {
            obj.put("panId", new JsonString(m.panId()));
        }
        obj.put("pointId", new JsonString(m.pointId()));
        obj.put("protocol", new JsonString(m.protocol().name()));
        obj.put("receiverId", new JsonString(m.receiverId()));
        obj.put("rssiMax", new JsonInt(m.rssiMax()));
        obj.put("rssiMedian", new JsonString(FixedDecimal.format(m.rssiMedian(), 1)));
        obj.put("rssiMin", new JsonInt(m.rssiMin()));
        obj.put("source", new JsonString(m.source().name()));
        if (m.ssid() != null) {
            obj.put("ssid", new JsonString(m.ssid()));
        }
        if (m.widthMHz() != null) {
            obj.put("widthMHz", new JsonInt(m.widthMHz()));
        }
        return obj;
    }

    public static Measurement fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String pointId = ((JsonString) obj.get("pointId")).value();
        Protocol protocol = Protocol.valueOf(((JsonString) obj.get("protocol")).value());
        String bssidOrId = ((JsonString) obj.get("bssidOrId")).value();
        String ssid = obj.get("ssid") instanceof JsonString s ? s.value() : null;
        int freqMHz = IntFields.parseInt(obj.get("freqMHz"), "freqMHz");
        int channel = IntFields.parseInt(obj.get("channel"), "channel");
        Integer widthMHz = IntFields.parseOptionalInt(obj.get("widthMHz"), "widthMHz");
        MeasurementSource source = MeasurementSource.valueOf(((JsonString) obj.get("source")).value());
        String receiverId = ((JsonString) obj.get("receiverId")).value();
        int n = IntFields.parseInt(obj.get("n"), "n");
        double rssiMedian = DecimalFields.parseDouble(obj.get("rssiMedian"), "rssiMedian", 1);
        int rssiMin = IntFields.parseInt(obj.get("rssiMin"), "rssiMin");
        int rssiMax = IntFields.parseInt(obj.get("rssiMax"), "rssiMax");
        Integer lqi = IntFields.parseOptionalInt(obj.get("lqi"), "lqi");
        String panId = obj.get("panId") instanceof JsonString s ? s.value() : null;
        return new Measurement(id, pointId, protocol, bssidOrId, ssid, freqMHz, channel, widthMHz, source, receiverId, n, rssiMedian, rssiMin, rssiMax, lqi, panId);
    }
}

