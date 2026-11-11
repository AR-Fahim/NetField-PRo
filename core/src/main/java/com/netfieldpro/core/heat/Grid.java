package com.netfieldpro.core.heat;

public record Grid(double originX, double originY, double g, int cols, int rows) {

    public double cellCenterX(int col) {
        return originX + (col + 0.5) * g;
    }

    public double cellCenterY(int row) {
        return originY + (row + 0.5) * g;
    }

    public int totalCells() {
        return cols * rows;
    }
}
