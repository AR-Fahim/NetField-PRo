package com.netfieldpro.core.scale;

public final class Affine3x3 {

    private final double m00;
    private final double m01;
    private final double m02;
    private final double m10;
    private final double m11;
    private final double m12;

    public Affine3x3(double m00, double m01, double m02, double m10, double m11, double m12) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
    }

    public static Affine3x3 identity() {
        return new Affine3x3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0);
    }

    public static Affine3x3 translate(double tx, double ty) {
        return new Affine3x3(1.0, 0.0, tx, 0.0, 1.0, ty);
    }

    public static Affine3x3 scale(double sx, double sy) {
        return new Affine3x3(sx, 0.0, 0.0, 0.0, sy, 0.0);
    }

    public static Affine3x3 fromTwoPoints(
            double srcX1, double srcY1, double srcX2, double srcY2,
            double dstX1, double dstY1, double dstX2, double dstY2
    ) {
        double dSrcX = srcX2 - srcX1;
        double dSrcY = srcY2 - srcY1;
        double srcLenSq = dSrcX * dSrcX + dSrcY * dSrcY;
        if (srcLenSq <= 0.0) {
            throw new IllegalArgumentException("Source points cannot be coincident");
        }

        double dDstX = dstX2 - dstX1;
        double dDstY = dstY2 - dstY1;

        double a = (dDstX * dSrcX + dDstY * dSrcY) / srcLenSq;
        double b = (dDstY * dSrcX - dDstX * dSrcY) / srcLenSq;

        double tx = dstX1 - (a * srcX1 - b * srcY1);
        double ty = dstY1 - (b * srcX1 + a * srcY1);

        return new Affine3x3(a, -b, tx, b, a, ty);
    }

    public Affine3x3 multiply(Affine3x3 o) {
        return new Affine3x3(
                this.m00 * o.m00 + this.m01 * o.m10,
                this.m00 * o.m01 + this.m01 * o.m11,
                this.m00 * o.m02 + this.m01 * o.m12 + this.m02,
                this.m10 * o.m00 + this.m11 * o.m10,
                this.m10 * o.m01 + this.m11 * o.m11,
                this.m10 * o.m02 + this.m11 * o.m12 + this.m12
        );
    }

    public Affine3x3 invert() {
        double det = m00 * m11 - m01 * m10;
        if (Math.abs(det) < 1e-12) {
            throw new IllegalStateException("Matrix is not invertible");
        }
        double invDet = 1.0 / det;
        double n00 = m11 * invDet;
        double n01 = -m01 * invDet;
        double n02 = (m01 * m12 - m02 * m11) * invDet;
        double n10 = -m10 * invDet;
        double n11 = m00 * invDet;
        double n12 = (m02 * m10 - m00 * m12) * invDet;
        return new Affine3x3(n00, n01, n02, n10, n11, n12);
    }

    public double transformX(double x, double y) {
        return m00 * x + m01 * y + m02;
    }

    public double transformY(double x, double y) {
        return m10 * x + m11 * y + m12;
    }

    public double[] transform(double x, double y) {
        return new double[]{transformX(x, y), transformY(x, y)};
    }

    public double[] inverseTransform(double x, double y) {
        return invert().transform(x, y);
    }

    public double getM00() {
        return m00;
    }

    public double getM01() {
        return m01;
    }

    public double getM02() {
        return m02;
    }

    public double getM10() {
        return m10;
    }

    public double getM11() {
        return m11;
    }

    public double getM12() {
        return m12;
    }
}
