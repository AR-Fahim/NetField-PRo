package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.data.db.entity.WallRow;

public final class WallMapper {

    private WallMapper() {
    }

    public static WallRow toRow(Wall w) {
        WallRow row = new WallRow();
        row.id = w.id();
        row.floorId = w.floorId();
        row.materialId = w.materialId().name();

        JsonArray arr = new JsonArray();
        for (Wall.Vertex v : w.vertices()) {
            JsonObject vObj = new JsonObject();
            vObj.put("x", new JsonString(FixedDecimal.format(v.x(), 3)));
            vObj.put("y", new JsonString(FixedDecimal.format(v.y(), 3)));
            arr.add(vObj);
        }
        row.vertices = new String(CanonicalJsonWriter.toBytes(arr), StandardCharsets.UTF_8);
        return row;
    }

    public static Wall toDomain(WallRow row) {
        try {
            JsonArray arr = (JsonArray) JsonParser.parse(row.vertices.getBytes(StandardCharsets.UTF_8));
            List<Wall.Vertex> vertices = new ArrayList<>();
            for (JsonValue v : arr.elements()) {
                JsonObject vObj = (JsonObject) v;
                double x = Double.parseDouble(((JsonString) vObj.get("x")).value());
                double y = Double.parseDouble(((JsonString) vObj.get("y")).value());
                vertices.add(new Wall.Vertex(x, y));
            }
            return new Wall(row.id, row.floorId, MaterialId.valueOf(row.materialId), vertices);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping WallRow to domain", e);
        }
    }
}

