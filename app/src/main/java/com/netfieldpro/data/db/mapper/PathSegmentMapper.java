package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.PathSegment;
import com.netfieldpro.core.model.enums.PathMode;
import com.netfieldpro.core.model.enums.PathState;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.data.db.entity.PathSegmentRow;

public final class PathSegmentMapper {

    private PathSegmentMapper() {
    }

    public static PathSegmentRow toRow(PathSegment p) {
        PathSegmentRow row = new PathSegmentRow();
        row.id = p.id();
        row.sessionId = p.sessionId();
        row.state = p.state().name();
        row.rawStartNs = p.rawStartNs();
        row.rawEndNs = p.rawEndNs();
        row.trimStartNs = p.trimStartNs();
        row.trimEndNs = p.trimEndNs();
        row.speedMps = p.speedMps();
        row.mode = p.mode().name();

        JsonArray wpArr = new JsonArray();
        for (PathSegment.Waypoint w : p.waypoints()) {
            JsonObject wObj = new JsonObject();
            wObj.put("tNs", new JsonInt(w.tNs()));
            wObj.put("x", new JsonString(FixedDecimal.format(w.x(), 3)));
            wObj.put("y", new JsonString(FixedDecimal.format(w.y(), 3)));
            wpArr.add(wObj);
        }
        row.waypoints = new String(CanonicalJsonWriter.toBytes(wpArr), StandardCharsets.UTF_8);

        JsonArray anArr = new JsonArray();
        for (PathSegment.Anchor a : p.anchors()) {
            JsonObject aObj = new JsonObject();
            aObj.put("tNs", new JsonInt(a.tNs()));
            aObj.put("vertexIdx", new JsonInt(a.vertexIdx()));
            anArr.add(aObj);
        }
        row.anchors = new String(CanonicalJsonWriter.toBytes(anArr), StandardCharsets.UTF_8);

        return row;
    }

    public static PathSegment toDomain(PathSegmentRow row) {
        try {
            JsonArray wpArr = (JsonArray) JsonParser.parse(row.waypoints.getBytes(StandardCharsets.UTF_8));
            List<PathSegment.Waypoint> waypoints = new ArrayList<>();
            for (JsonValue v : wpArr.elements()) {
                JsonObject wObj = (JsonObject) v;
                double x = Double.parseDouble(((JsonString) wObj.get("x")).value());
                double y = Double.parseDouble(((JsonString) wObj.get("y")).value());
                long tNs;
                JsonValue tVal = wObj.get("tNs");
                if (tVal instanceof JsonInt ji) {
                    tNs = ji.value();
                } else {
                    tNs = Long.parseLong(((JsonString) tVal).value());
                }
                waypoints.add(new PathSegment.Waypoint(x, y, tNs));
            }

            JsonArray anArr = (JsonArray) JsonParser.parse(row.anchors.getBytes(StandardCharsets.UTF_8));
            List<PathSegment.Anchor> anchors = new ArrayList<>();
            for (JsonValue v : anArr.elements()) {
                JsonObject aObj = (JsonObject) v;
                int vertexIdx = (int) ((JsonInt) aObj.get("vertexIdx")).value();
                long tNs;
                JsonValue tVal = aObj.get("tNs");
                if (tVal instanceof JsonInt ji) {
                    tNs = ji.value();
                } else {
                    tNs = Long.parseLong(((JsonString) tVal).value());
                }
                anchors.add(new PathSegment.Anchor(vertexIdx, tNs));
            }

            return new PathSegment(
                    row.id, row.sessionId, PathState.valueOf(row.state),
                    row.rawStartNs, row.rawEndNs, row.trimStartNs, row.trimEndNs,
                    row.speedMps, waypoints, anchors, PathMode.valueOf(row.mode)
            );
        } catch (Exception e) {
            throw new RuntimeException("Error mapping PathSegmentRow to domain", e);
        }
    }
}
