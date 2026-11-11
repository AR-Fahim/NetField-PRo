package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.AccessPointMarker;
import com.netfieldpro.core.util.FixedDecimal;

public final class AccessPointMarkerSerializer {

    private AccessPointMarkerSerializer() {
    }

    public static JsonObject toJson(AccessPointMarker a) {
        JsonObject obj = new JsonObject();
        if (a.bssid() != null) {
            obj.put("bssid", new JsonString(a.bssid()));
        }
        obj.put("floorId", new JsonString(a.floorId()));
        obj.put("id", new JsonString(a.id()));
        if (a.txPowerDbm() != null) {
            obj.put("txPowerDbm", new JsonInt(a.txPowerDbm()));
        }
        obj.put("x", new JsonString(FixedDecimal.format(a.x(), 3)));
        obj.put("y", new JsonString(FixedDecimal.format(a.y(), 3)));
        return obj;
    }

    public static AccessPointMarker fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String floorId = ((JsonString) obj.get("floorId")).value();
        double x = DecimalFields.parseDouble(obj.get("x"), "x", 3);
        double y = DecimalFields.parseDouble(obj.get("y"), "y", 3);
        String bssid = obj.get("bssid") instanceof JsonString s ? s.value() : null;
        Integer txPowerDbm = IntFields.parseOptionalInt(obj.get("txPowerDbm"), "txPowerDbm");
        return new AccessPointMarker(id, floorId, x, y, bssid, txPowerDbm);
    }
}

