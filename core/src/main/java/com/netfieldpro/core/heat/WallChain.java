package com.netfieldpro.core.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record WallChain(String id, String materialId, List<Vertex> vertices) {

    public WallChain {
        if (vertices == null || vertices.size() < 2) {
            throw new IllegalArgumentException("WallChain must have at least 2 vertices");
        }
        vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
    }
}
