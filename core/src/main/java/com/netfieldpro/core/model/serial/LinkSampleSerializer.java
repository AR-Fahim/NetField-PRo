package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.LinkSample;

public final class LinkSampleSerializer {

    private LinkSampleSerializer() {
    }

    public static JsonObject toJson(LinkSample l) {
        JsonObject obj = new JsonObject();
        obj.put("bssid", new JsonString(l.bssid()));
        obj.put("freqMHz", new JsonInt(l.freqMHz()));
        obj.put("id", new JsonString(l.id()));
        obj.put("observedAtNs", new JsonInt(l.observedAtNs()));
        obj.put("rssi", new JsonInt(l.rssi()));
        if (l.rxMbps() != null) {
            obj.put("rxMbps", new JsonInt(l.rxMbps()));
        }
        obj.put("sessionId", new JsonString(l.sessionId()));
        obj.put("ssid", new JsonString(l.ssid()));
        if (l.txMbps() != null) {
            obj.put("txMbps", new JsonInt(l.txMbps()));
        }
        return obj;
    }

    public static LinkSample fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        long observedAtNs = IntFields.parseLong(obj.get("observedAtNs"), "observedAtNs");
        String bssid = ((JsonString) obj.get("bssid")).value();
        String ssid = ((JsonString) obj.get("ssid")).value();
        int rssi = IntFields.parseInt(obj.get("rssi"), "rssi");
        int freqMHz = IntFields.parseInt(obj.get("freqMHz"), "freqMHz");
        Integer txMbps = IntFields.parseOptionalInt(obj.get("txMbps"), "txMbps");
        Integer rxMbps = IntFields.parseOptionalInt(obj.get("rxMbps"), "rxMbps");
        return new LinkSample(id, sessionId, observedAtNs, bssid, ssid, rssi, freqMHz, txMbps, rxMbps);
    }
}
