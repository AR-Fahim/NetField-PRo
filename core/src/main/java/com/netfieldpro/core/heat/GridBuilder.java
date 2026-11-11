package com.netfieldpro.core.heat;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GridBuilder {

    public static final double MIN_G = 0.5;
    public static final int MAX_CELLS = 25000;

    private GridBuilder() {
    }

    public static Grid build(Polygon polygon) {
        if (polygon == null) {
            throw new IllegalArgumentException("polygon cannot be null");
        }
        double bboxW = polygon.bboxW();
        double bboxH = polygon.bboxH();

        double rawG = Math.max(MIN_G, Math.sqrt((bboxW * bboxH) / (double) MAX_CELLS));
        double g = round6(rawG);

        int cols = ceilDiv(bboxW, g);
        int rows = ceilDiv(bboxH, g);

        while ((long) cols * rows > MAX_CELLS) {
            g = round6(g * 1.01);
            cols = ceilDiv(bboxW, g);
            rows = ceilDiv(bboxH, g);
        }

        return new Grid(polygon.minX(), polygon.minY(), g, cols, rows);
    }

    public static int ceilDiv(double span, double g) {
        BigDecimal spanBd = BigDecimal.valueOf(span);
        BigDecimal gBd = BigDecimal.valueOf(g);
        BigDecimal quotient = spanBd.divide(gBd, 9, RoundingMode.HALF_EVEN);
        return quotient.setScale(0, RoundingMode.CEILING).intValueExact();
    }

    public static double round6(double val) {
        return BigDecimal.valueOf(val).setScale(6, RoundingMode.HALF_EVEN).doubleValue();
    }
}
