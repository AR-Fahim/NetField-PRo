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
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;

public final class FloorBoundaryMapper {

    private FloorBoundaryMapper() {
    }

    public static FloorBoundaryRow toRow(FloorBoundary f) {
        FloorBoundaryRow row = new FloorBoundaryRow();
        row.floorId = f.floorId();

        JsonArray arr = new JsonArray();
        for (FloorBoundary.Vertex v : f.vertices()) {
            JsonObject vObj = new JsonObject();
            vObj.put("x", new JsonString(FixedDecimal.format(v.x(), 3)));
            vObj.put("y", new JsonString(FixedDecimal.format(v.y(), 3)));
            arr.add(vObj);
        }
        row.vertices = new String(CanonicalJsonWriter.toBytes(arr), StandardCharsets.UTF_8);
        return row;
    }

    public static FloorBoundary toDomain(FloorBoundaryRow row) {
        try {
            JsonArray arr = (JsonArray) JsonParser.parse(row.vertices.getBytes(StandardCharsets.UTF_8));
            List<FloorBoundary.Vertex> vertices = new ArrayList<>();
            for (JsonValue v : arr.elements()) {
                JsonObject vObj = (JsonObject) v;
                double x = Double.parseDouble(((JsonString) vObj.get("x")).value());
                double y = Double.parseDouble(((JsonString) vObj.get("y")).value());
                vertices.add(new FloorBoundary.Vertex(x, y));
            }
            return new FloorBoundary(row.floorId, vertices);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping FloorBoundaryRow to domain", e);
        }
    }
}

