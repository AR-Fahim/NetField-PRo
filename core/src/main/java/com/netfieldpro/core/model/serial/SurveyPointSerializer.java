package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;
import com.netfieldpro.core.util.FixedDecimal;

public final class SurveyPointSerializer {

    private SurveyPointSerializer() {
    }

    public static JsonObject toJson(SurveyPoint p) {
        JsonObject obj = new JsonObject();
        obj.put("id", new JsonString(p.id()));
        obj.put("nScans", new JsonInt(p.nScans()));
        obj.put("positionUncertaintyM", new JsonString(FixedDecimal.format(p.positionUncertaintyM(), 2)));
        obj.put("quality", new JsonString(p.quality().name()));
        if (p.segmentId() != null) {
            obj.put("segmentId", new JsonString(p.segmentId()));
        }
        obj.put("sessionId", new JsonString(p.sessionId()));
        obj.put("source", new JsonString(p.source().name()));
        obj.put("tMonoNs", new JsonInt(p.tMonoNs()));
        obj.put("tWallMs", new JsonInt(p.tWallMs()));
        obj.put("x", new JsonString(FixedDecimal.format(p.x(), 3)));
        obj.put("y", new JsonString(FixedDecimal.format(p.y(), 3)));
        return obj;
    }

    public static SurveyPoint fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        double x = DecimalFields.parseDouble(obj.get("x"), "x", 3);
        double y = DecimalFields.parseDouble(obj.get("y"), "y", 3);
        PointSource source = PointSource.valueOf(((JsonString) obj.get("source")).value());
        PointQuality quality = PointQuality.valueOf(((JsonString) obj.get("quality")).value());
        long tMonoNs = IntFields.parseLong(obj.get("tMonoNs"), "tMonoNs");
        long tWallMs = IntFields.parseLong(obj.get("tWallMs"), "tWallMs");
        int nScans = IntFields.parseInt(obj.get("nScans"), "nScans");
        double positionUncertaintyM = DecimalFields.parseDouble(obj.get("positionUncertaintyM"), "positionUncertaintyM", 2);
        String segmentId = obj.get("segmentId") instanceof JsonString s ? s.value() : null;
        return new SurveyPoint(id, sessionId, x, y, source, quality, tMonoNs, tWallMs, nScans, positionUncertaintyM, segmentId);
    }
}
