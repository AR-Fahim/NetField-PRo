package com.netfieldpro.core.model.entity;

import java.util.List;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.PathMode;
import com.netfieldpro.core.model.enums.PathState;

public record PathSegment(
        String id,
        String sessionId,
        PathState state,
        long rawStartNs,
        long rawEndNs,
        long trimStartNs,
        long trimEndNs,
        double speedMps,
        List<Waypoint> waypoints,
        List<Anchor> anchors,
        PathMode mode
) {
    public record Waypoint(double x, double y, long tNs) {
        public Waypoint {
            ModelValidator.checkDouble("PathSegment.Waypoint", "x", x);
            ModelValidator.checkDouble("PathSegment.Waypoint", "y", y);
            ModelValidator.checkNonNegative("PathSegment.Waypoint", "tNs", tNs);
        }
    }

    public record Anchor(int vertexIdx, long tNs) {
        public Anchor {
            ModelValidator.checkNonNegative("PathSegment.Anchor", "vertexIdx", vertexIdx);
            ModelValidator.checkNonNegative("PathSegment.Anchor", "tNs", tNs);
        }
    }

    public PathSegment {
        ModelValidator.checkId("PathSegment", "id", id);
        ModelValidator.checkId("PathSegment", "sessionId", sessionId);
        ModelValidator.checkRequired("PathSegment", "state", state);
        ModelValidator.checkDouble("PathSegment", "speedMps", speedMps);
        if (speedMps < 0.0) {
            throw new IllegalArgumentException("PathSegment.speedMps: must be >= 0: " + speedMps);
        }
        ModelValidator.checkRequired("PathSegment", "mode", mode);
        waypoints = ModelValidator.checkList("PathSegment", "waypoints", waypoints);
        anchors = ModelValidator.checkList("PathSegment", "anchors", anchors);

        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("PathSegment.waypoints: must have at least 2 waypoints: " + waypoints.size());
        }

        long prevT = -1;
        for (Waypoint w : waypoints) {
            if (prevT != -1 && w.tNs() < prevT) {
                throw new IllegalArgumentException("PathSegment.waypoints: tNs must be non-decreasing");
            }
            prevT = w.tNs();
        }

        int prevV = -1;
        for (Anchor a : anchors) {
            if (prevV != -1 && a.vertexIdx() < prevV) {
                throw new IllegalArgumentException("PathSegment.anchors: must be sorted by vertexIdx");
            }
            prevV = a.vertexIdx();
        }

        if (!(rawStartNs <= trimStartNs && trimStartNs <= trimEndNs && trimEndNs <= rawEndNs)) {
            throw new IllegalArgumentException("PathSegment.rawStartNs: must satisfy rawStart <= trimStart <= trimEnd <= rawEnd");
        }
    }
}

