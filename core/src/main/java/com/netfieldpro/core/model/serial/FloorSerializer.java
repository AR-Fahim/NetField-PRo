package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.enums.ScaleMethod;
import com.netfieldpro.core.util.FixedDecimal;

public final class FloorSerializer {

    private FloorSerializer() {
    }

    public static JsonObject toJson(Floor f) {
        JsonObject obj = new JsonObject();
        if (f.alignRotDeg() != null) {
            obj.put("alignRotDeg", new JsonString(FixedDecimal.format(f.alignRotDeg(), 3)));
        }
        if (f.alignX() != null) {
            obj.put("alignX", new JsonString(FixedDecimal.format(f.alignX(), 3)));
        }
        if (f.alignY() != null) {
            obj.put("alignY", new JsonString(FixedDecimal.format(f.alignY(), 3)));
        }
        obj.put("id", new JsonString(f.id()));
        obj.put("levelIndex", new JsonInt(f.levelIndex()));
        obj.put("metersPerPixel", new JsonString(FixedDecimal.format(f.metersPerPixel(), 9)));
        if (f.northOffsetDeg() != null) {
            obj.put("northOffsetDeg", new JsonString(FixedDecimal.format(f.northOffsetDeg(), 3)));
        }
        obj.put("planHpx", new JsonInt(f.planHpx()));
        obj.put("planSha256", new JsonString(f.planSha256()));
        obj.put("planWpx", new JsonInt(f.planWpx()));
        obj.put("projectId", new JsonString(f.projectId()));
        obj.put("scaleChecked", JsonBool.of(f.scaleChecked()));
        obj.put("scaleMethod", new JsonString(f.scaleMethod().name()));
        if (f.scaleP1() != null) {
            JsonObject p1 = new JsonObject();
            p1.put("x", new JsonString(FixedDecimal.format(f.scaleP1().x(), 3)));
            p1.put("y", new JsonString(FixedDecimal.format(f.scaleP1().y(), 3)));
            obj.put("scaleP1", p1);
        }
        if (f.scaleP2() != null) {
            JsonObject p2 = new JsonObject();
            p2.put("x", new JsonString(FixedDecimal.format(f.scaleP2().x(), 3)));
            p2.put("y", new JsonString(FixedDecimal.format(f.scaleP2().y(), 3)));
            obj.put("scaleP2", p2);
        }
        obj.put("scaleUncertaintyPct", new JsonString(FixedDecimal.format(f.scaleUncertaintyPct(), 3)));
        return obj;
    }

    public static Floor fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String projectId = ((JsonString) obj.get("projectId")).value();
        long levelIndex = IntFields.parseLong(obj.get("levelIndex"), "levelIndex");
        String planSha256 = ((JsonString) obj.get("planSha256")).value();
        int planWpx = IntFields.parseInt(obj.get("planWpx"), "planWpx");
        int planHpx = IntFields.parseInt(obj.get("planHpx"), "planHpx");
        double metersPerPixel = DecimalFields.parseDouble(obj.get("metersPerPixel"), "metersPerPixel", 9);
        Double northOffsetDeg = DecimalFields.parseOptionalDouble(obj.get("northOffsetDeg"), "northOffsetDeg", 3);
        ScaleMethod scaleMethod = ScaleMethod.valueOf(((JsonString) obj.get("scaleMethod")).value());
        double scaleUncertaintyPct = DecimalFields.parseDouble(obj.get("scaleUncertaintyPct"), "scaleUncertaintyPct", 3);
        boolean scaleChecked = ((JsonBool) obj.get("scaleChecked")).value();
        Double alignX = DecimalFields.parseOptionalDouble(obj.get("alignX"), "alignX", 3);
        Double alignY = DecimalFields.parseOptionalDouble(obj.get("alignY"), "alignY", 3);
        Double alignRotDeg = DecimalFields.parseOptionalDouble(obj.get("alignRotDeg"), "alignRotDeg", 3);

        Floor.Point scaleP1 = null;
        if (obj.get("scaleP1") instanceof JsonObject p1) {
            double x = DecimalFields.parseDouble(p1.get("x"), "x", 3);
            double y = DecimalFields.parseDouble(p1.get("y"), "y", 3);
            scaleP1 = new Floor.Point(x, y);
        }

        Floor.Point scaleP2 = null;
        if (obj.get("scaleP2") instanceof JsonObject p2) {
            double x = DecimalFields.parseDouble(p2.get("x"), "x", 3);
            double y = DecimalFields.parseDouble(p2.get("y"), "y", 3);
            scaleP2 = new Floor.Point(x, y);
        }

        return new Floor(
                id, projectId, levelIndex, planSha256, planWpx, planHpx, metersPerPixel,
                northOffsetDeg, scaleMethod, scaleUncertaintyPct, scaleChecked,
                alignX, alignY, alignRotDeg, scaleP1, scaleP2
        );
    }
}
