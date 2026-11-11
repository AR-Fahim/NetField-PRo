package com.netfieldpro.core.heat;

import java.util.function.BooleanSupplier;

public final class IdwSurface {

    public static final byte OUTSIDE = 0;
    public static final byte MASKED = 1;
    public static final byte VALUE = 2;

    private final double[] dbm;
    private final byte[] cellState;
    private final int masked;
    private final int value;

    public IdwSurface(double[] dbm, byte[] cellState, int masked, int value) {
        this.dbm = dbm;
        this.cellState = cellState;
        this.masked = masked;
        this.value = value;
    }

    public double[] dbm() {
        return dbm;
    }

    public byte[] cellState() {
        return cellState;
    }

    public int masked() {
        return masked;
    }

    public int value() {
        return value;
    }

    public static IdwSurface compute(Grid grid, Polygon inside, IdwEngine engine, BooleanSupplier cancelled) {
        if (grid == null) {
            throw new IllegalArgumentException("grid cannot be null");
        }
        if (inside == null) {
            throw new IllegalArgumentException("inside polygon cannot be null");
        }
        if (engine == null) {
            throw new IllegalArgumentException("engine cannot be null");
        }

        int cols = grid.cols();
        int rows = grid.rows();
        int total = cols * rows;

        double[] dbm = new double[total];
        byte[] cellState = new byte[total];
        int maskedCount = 0;
        int valueCount = 0;

        for (int r = 0; r < rows; r++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return null;
            }
            double cy = grid.cellCenterY(r);
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                double cx = grid.cellCenterX(c);

                if (!inside.contains(cx, cy)) {
                    cellState[idx] = OUTSIDE;
                    dbm[idx] = Double.NaN;
                } else {
                    IdwResult res = engine.estimate(cx, cy);
                    if (res.masked()) {
                        cellState[idx] = MASKED;
                        dbm[idx] = Double.NaN;
                        maskedCount++;
                    } else {
                        cellState[idx] = VALUE;
                        dbm[idx] = res.dbm();
                        valueCount++;
                    }
                }
            }
        }

        return new IdwSurface(dbm, cellState, maskedCount, valueCount);
    }
}
