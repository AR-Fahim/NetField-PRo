package com.netfieldpro.core.model.entity;

import java.util.List;

import com.netfieldpro.core.model.ModelValidator;

public record FloorBoundary(
        String floorId,
        List<Vertex> vertices
) {
    public record Vertex(double x, double y) {
        public Vertex {
            ModelValidator.checkDouble("FloorBoundary.Vertex", "x", x);
            ModelValidator.checkDouble("FloorBoundary.Vertex", "y", y);
        }
    }

    public FloorBoundary {
        ModelValidator.checkId("FloorBoundary", "floorId", floorId);
        vertices = ModelValidator.checkList("FloorBoundary", "vertices", vertices);
    }
}

