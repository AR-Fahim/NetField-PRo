package com.netfieldpro.core.geom;

import java.util.List;

public final class PolygonValidator {

    public enum Result {
        OK,
        TOO_FEW,
        TOO_SMALL,
        SELF_INTERSECT
    }

    private PolygonValidator() {
    }

    public static Result validate(List<Pt> pts) {
        if (pts == null || pts.size() < 3) {
            return Result.TOO_FEW;
        }

        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Pt a = pts.get(i);
            Pt b = pts.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                if (j == (i + 1) % n || i == (j + 1) % n) {
                    continue;
                }
                Pt c = pts.get(j);
                Pt d = pts.get((j + 1) % n);
                if (segmentsIntersect(a, b, c, d)) {
                    return Result.SELF_INTERSECT;
                }
            }
        }

        double area = shoelaceArea(pts);
        if (area < 1.0) {
            return Result.TOO_SMALL;
        }

        return Result.OK;
    }

    public static double shoelaceArea(List<Pt> pts) {
        if (pts == null || pts.size() < 3) {
            return 0.0;
        }
        double sum = 0.0;
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Pt p1 = pts.get(i);
            Pt p2 = pts.get((i + 1) % n);
            sum += (p1.x() * p2.y() - p2.x() * p1.y());
        }
        return Math.abs(sum) * 0.5;
    }

    private static double crossProduct(Pt a, Pt b, Pt c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    private static int orientation(Pt a, Pt b, Pt c) {
        double cp = crossProduct(a, b, c);
        if (Math.abs(cp) <= 1e-9) {
            return 0;
        }
        return cp > 0 ? 1 : -1;
    }

    private static boolean onSegment(Pt a, Pt b, Pt c) {
        return c.x() >= Math.min(a.x(), b.x()) - 1e-9 &&
               c.x() <= Math.max(a.x(), b.x()) + 1e-9 &&
               c.y() >= Math.min(a.y(), b.y()) - 1e-9 &&
               c.y() <= Math.max(a.y(), b.y()) + 1e-9;
    }

    private static boolean segmentsIntersect(Pt a, Pt b, Pt c, Pt d) {
        int o1 = orientation(a, b, c);
        int o2 = orientation(a, b, d);
        int o3 = orientation(c, d, a);
        int o4 = orientation(c, d, b);

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        if (o1 == 0 && onSegment(a, b, c)) return true;
        if (o2 == 0 && onSegment(a, b, d)) return true;
        if (o3 == 0 && onSegment(c, d, a)) return true;
        if (o4 == 0 && onSegment(c, d, b)) return true;

        return false;
    }
}

