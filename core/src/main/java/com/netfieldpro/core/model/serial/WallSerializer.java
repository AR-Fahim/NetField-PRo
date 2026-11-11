package com.netfieldpro.core.model.serial;

import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.util.FixedDecimal;

public final class WallSerializer {

    private WallSerializer() {
    }

    public static JsonObject toJson(Wall w) {
        JsonObject obj = new JsonObject();
        obj.put("floorId", new JsonString(w.floorId()));
        obj.put("id", new JsonString(w.id()));
        obj.put("materialId", new JsonString(w.materialId().name()));

        JsonArray arr = new JsonArray();
        for (Wall.Vertex v : w.vertices()) {
            JsonObject vObj = new JsonObject();
            vObj.put("x", new JsonString(FixedDecimal.format(v.x(), 3)));
            vObj.put("y", new JsonString(FixedDecimal.format(v.y(), 3)));
            arr.add(vObj);
        }
        obj.put("vertices", arr);
        return obj;
    }

    public static Wall fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String floorId = ((JsonString) obj.get("floorId")).value();
        MaterialId materialId = MaterialId.valueOf(((JsonString) obj.get("materialId")).value());

        JsonArray arr = (JsonArray) obj.get("vertices");
        List<Wall.Vertex> vertices = new ArrayList<>();
        for (JsonValue val : arr.elements()) {
            JsonObject vObj = (JsonObject) val;
            double x = DecimalFields.parseDouble(vObj.get("x"), "x", 3);
            double y = DecimalFields.parseDouble(vObj.get("y"), "y", 3);
            vertices.add(new Wall.Vertex(x, y));
        }
        return new Wall(id, floorId, materialId, vertices);
    }
}

