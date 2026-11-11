package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.RawSample;

public final class RawSampleSerializer {

    private RawSampleSerializer() {
    }

    public static JsonObject toJson(RawSample r) {
        JsonObject obj = new JsonObject();
        obj.put("bssid", new JsonString(r.bssid()));
        obj.put("freqMHz", new JsonInt(r.freqMHz()));
        obj.put("rssi", new JsonInt(r.rssi()));
        obj.put("scanId", new JsonString(r.scanId()));
        if (r.scanResultTsNs() != null) {
            obj.put("scanResultTsNs", new JsonInt(r.scanResultTsNs()));
        }
        obj.put("ssid", new JsonString(r.ssid()));
        return obj;
    }

    public static RawSample fromJson(JsonObject obj) {
        String scanId = ((JsonString) obj.get("scanId")).value();
        String bssid = ((JsonString) obj.get("bssid")).value();
        String ssid = ((JsonString) obj.get("ssid")).value();
        int freqMHz = IntFields.parseInt(obj.get("freqMHz"), "freqMHz");
        int rssi = IntFields.parseInt(obj.get("rssi"), "rssi");
        Long scanResultTsNs = IntFields.parseOptionalLong(obj.get("scanResultTsNs"), "scanResultTsNs");
        return new RawSample(0L, scanId, bssid, ssid, freqMHz, rssi, scanResultTsNs);
    }
}
