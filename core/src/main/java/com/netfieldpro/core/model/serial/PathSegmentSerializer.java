package com.netfieldpro.core.model.serial;

import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.PathSegment;
import com.netfieldpro.core.model.enums.PathMode;
import com.netfieldpro.core.model.enums.PathState;
import com.netfieldpro.core.util.FixedDecimal;

public final class PathSegmentSerializer {

    private PathSegmentSerializer() {
    }

    public static JsonObject toJson(PathSegment p) {
        JsonObject obj = new JsonObject();

        JsonArray anchorsArr = new JsonArray();
        for (PathSegment.Anchor a : p.anchors()) {
            JsonObject aObj = new JsonObject();
            aObj.put("tNs", new JsonInt(a.tNs()));
            aObj.put("vertexIdx", new JsonInt(a.vertexIdx()));
            anchorsArr.add(aObj);
        }
        obj.put("anchors", anchorsArr);

        obj.put("id", new JsonString(p.id()));
        obj.put("mode", new JsonString(p.mode().name()));
        obj.put("rawEndNs", new JsonInt(p.rawEndNs()));
        obj.put("rawStartNs", new JsonInt(p.rawStartNs()));
        obj.put("sessionId", new JsonString(p.sessionId()));
        obj.put("speedMps", new JsonString(FixedDecimal.format(p.speedMps(), 3)));
        obj.put("state", new JsonString(p.state().name()));
        obj.put("trimEndNs", new JsonInt(p.trimEndNs()));
        obj.put("trimStartNs", new JsonInt(p.trimStartNs()));

        JsonArray wpArr = new JsonArray();
        for (PathSegment.Waypoint w : p.waypoints()) {
            JsonObject wObj = new JsonObject();
            wObj.put("tNs", new JsonInt(w.tNs()));
            wObj.put("x", new JsonString(FixedDecimal.format(w.x(), 3)));
            wObj.put("y", new JsonString(FixedDecimal.format(w.y(), 3)));
            wpArr.add(wObj);
        }
        obj.put("waypoints", wpArr);

        return obj;
    }

    public static PathSegment fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String sessionId = ((JsonString) obj.get("sessionId")).value();
        PathState state = PathState.valueOf(((JsonString) obj.get("state")).value());
        long rawStartNs = IntFields.parseLong(obj.get("rawStartNs"), "rawStartNs");
        long rawEndNs = IntFields.parseLong(obj.get("rawEndNs"), "rawEndNs");
        long trimStartNs = IntFields.parseLong(obj.get("trimStartNs"), "trimStartNs");
        long trimEndNs = IntFields.parseLong(obj.get("trimEndNs"), "trimEndNs");
        double speedMps = DecimalFields.parseDouble(obj.get("speedMps"), "speedMps", 3);

        JsonArray wpArr = (JsonArray) obj.get("waypoints");
        List<PathSegment.Waypoint> waypoints = new ArrayList<>();
        for (JsonValue v : wpArr.elements()) {
            JsonObject wObj = (JsonObject) v;
            double x = DecimalFields.parseDouble(wObj.get("x"), "x", 3);
            double y = DecimalFields.parseDouble(wObj.get("y"), "y", 3);
            long tNs = IntFields.parseLong(wObj.get("tNs"), "tNs");
            waypoints.add(new PathSegment.Waypoint(x, y, tNs));
        }

        JsonArray anArr = (JsonArray) obj.get("anchors");
        List<PathSegment.Anchor> anchors = new ArrayList<>();
        for (JsonValue v : anArr.elements()) {
            JsonObject aObj = (JsonObject) v;
            int vertexIdx = IntFields.parseInt(aObj.get("vertexIdx"), "vertexIdx");
            long tNs = IntFields.parseLong(aObj.get("tNs"), "tNs");
            anchors.add(new PathSegment.Anchor(vertexIdx, tNs));
        }

        PathMode mode = PathMode.valueOf(((JsonString) obj.get("mode")).value());

        return new PathSegment(id, sessionId, state, rawStartNs, rawEndNs, trimStartNs, trimEndNs, speedMps, waypoints, anchors, mode);
    }
}
