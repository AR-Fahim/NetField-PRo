package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.DeviceInfo;

public final class DeviceInfoSerializer {

    private DeviceInfoSerializer() {
    }

    public static JsonObject toJson(DeviceInfo d) {
        JsonObject obj = new JsonObject();
        obj.put("buildFingerprint", new JsonString(d.buildFingerprint()));
        obj.put("id", new JsonString(d.id()));
        if (d.linkCadenceMs() != null) {
            obj.put("linkCadenceMs", new JsonInt(d.linkCadenceMs()));
        }
        obj.put("manufacturer", new JsonString(d.manufacturer()));
        obj.put("model", new JsonString(d.model()));
        obj.put("sdk", new JsonInt(d.sdk()));
        obj.put("wifiStandards", new JsonString(d.wifiStandards()));
        return obj;
    }

    public static DeviceInfo fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String model = ((JsonString) obj.get("model")).value();
        String manufacturer = ((JsonString) obj.get("manufacturer")).value();
        int sdk = IntFields.parseInt(obj.get("sdk"), "sdk");
        String wifiStandards = ((JsonString) obj.get("wifiStandards")).value();
        Long linkCadenceMs = IntFields.parseOptionalLong(obj.get("linkCadenceMs"), "linkCadenceMs");
        String buildFingerprint = ((JsonString) obj.get("buildFingerprint")).value();
        return new DeviceInfo(id, model, manufacturer, sdk, wifiStandards, linkCadenceMs, buildFingerprint);
    }
}
