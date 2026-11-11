package com.netfieldpro.core.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Polygon {

    private final List<Vertex> vertices;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public Polygon(List<Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("Polygon must have at least 3 vertices");
        }
        this.vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
        double mx = Double.POSITIVE_INFINITY;
        double my = Double.POSITIVE_INFINITY;
        double ax = Double.NEGATIVE_INFINITY;
        double ay = Double.NEGATIVE_INFINITY;
        for (Vertex v : this.vertices) {
            if (v.x() < mx) mx = v.x();
            if (v.y() < my) my = v.y();
            if (v.x() > ax) ax = v.x();
            if (v.y() > ay) ay = v.y();
        }
        this.minX = mx;
        this.minY = my;
        this.maxX = ax;
        this.maxY = ay;
    }

    public List<Vertex> vertices() {
        return vertices;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double bboxW() {
        return maxX - minX;
    }

    public double bboxH() {
        return maxY - minY;
    }

    public boolean contains(double x, double y) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            return false;
        }

        int n = vertices.size();
        // D6: point on a polygon edge counts as inside, exactly
        // (orientation == 0 and within the segment box). No epsilon.
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vertex v1 = vertices.get(j);
            Vertex v2 = vertices.get(i);
            double cross = (v2.x() - v1.x()) * (y - v1.y()) - (v2.y() - v1.y()) * (x - v1.x());
            if (cross == 0.0) {
                double segMinX = Math.min(v1.x(), v2.x());
                double segMaxX = Math.max(v1.x(), v2.x());
                double segMinY = Math.min(v1.y(), v2.y());
                double segMaxY = Math.max(v1.y(), v2.y());
                if (x >= segMinX && x <= segMaxX && y >= segMinY && y <= segMaxY) {
                    return true;
                }
            }
        }

        // Even-odd ray casting
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vertex vi = vertices.get(i);
            Vertex vj = vertices.get(j);
            if ((vi.y() > y) != (vj.y() > y)) {
                double xCross = vi.x() + (y - vi.y()) * (vj.x() - vi.x()) / (vj.y() - vi.y());
                if (x < xCross) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}
