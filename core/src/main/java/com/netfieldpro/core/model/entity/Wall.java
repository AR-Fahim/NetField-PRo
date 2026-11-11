package com.netfieldpro.core.model.entity;

import java.util.List;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.MaterialId;

public record Wall(
        String id,
        String floorId,
        MaterialId materialId,
        List<Vertex> vertices
) {
    public record Vertex(double x, double y) {
        public Vertex {
            ModelValidator.checkDouble("Wall.Vertex", "x", x);
            ModelValidator.checkDouble("Wall.Vertex", "y", y);
        }
    }

    public Wall {
        ModelValidator.checkId("Wall", "id", id);
        ModelValidator.checkId("Wall", "floorId", floorId);
        ModelValidator.checkRequired("Wall", "materialId", materialId);
        vertices = ModelValidator.checkList("Wall", "vertices", vertices);
        if (vertices.size() < 2) {
            throw new IllegalArgumentException("Wall.vertices: must have at least 2 vertices: " + vertices.size());
        }
    }
}

