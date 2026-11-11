package com.netfieldpro.core.model.serial;

import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.util.FixedDecimal;

public final class FloorBoundarySerializer {

    private FloorBoundarySerializer() {
    }

    public static JsonObject toJson(FloorBoundary f) {
        JsonObject obj = new JsonObject();
        obj.put("floorId", new JsonString(f.floorId()));

        JsonArray arr = new JsonArray();
        for (FloorBoundary.Vertex v : f.vertices()) {
            JsonObject vObj = new JsonObject();
            vObj.put("x", new JsonString(FixedDecimal.format(v.x(), 3)));
            vObj.put("y", new JsonString(FixedDecimal.format(v.y(), 3)));
            arr.add(vObj);
        }
        obj.put("vertices", arr);
        return obj;
    }

    public static FloorBoundary fromJson(JsonObject obj) {
        String floorId = ((JsonString) obj.get("floorId")).value();
        JsonArray arr = (JsonArray) obj.get("vertices");
        List<FloorBoundary.Vertex> vertices = new ArrayList<>();
        for (JsonValue val : arr.elements()) {
            JsonObject vObj = (JsonObject) val;
            double x = DecimalFields.parseDouble(vObj.get("x"), "x", 3);
            double y = DecimalFields.parseDouble(vObj.get("y"), "y", 3);
            vertices.add(new FloorBoundary.Vertex(x, y));
        }
        return new FloorBoundary(floorId, vertices);
    }
}

