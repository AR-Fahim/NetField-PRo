package com.netfieldpro.core.plan;

public final class ViewportController {

    private int imageW;
    private int imageH;
    private int viewW;
    private int viewH;

    private double fitScale;
    private double minScale;
    private double maxScale;

    private double scale;
    private double tx;
    private double ty;

    public ViewportController() {
    }

    public ViewportController(int imageW, int imageH, int viewW, int viewH) {
        setDimensions(imageW, imageH, viewW, viewH);
    }

    public void setDimensions(int imageW, int imageH, int viewW, int viewH) {
        this.imageW = imageW;
        this.imageH = imageH;
        this.viewW = viewW;
        this.viewH = viewH;

        if (imageW <= 0 || imageH <= 0 || viewW <= 0 || viewH <= 0) {
            this.fitScale = 1.0;
            this.minScale = 1.0;
            this.maxScale = 16.0;
            this.scale = 1.0;
            this.tx = 0.0;
            this.ty = 0.0;
            return;
        }

        double sx = (double) viewW / (double) imageW;
        double sy = (double) viewH / (double) imageH;
        this.fitScale = Math.min(sx, sy);
        this.minScale = this.fitScale;
        this.maxScale = 16.0 * this.fitScale;

        this.scale = this.fitScale;
        this.tx = ((double) viewW - (double) imageW * this.scale) / 2.0;
        this.ty = ((double) viewH - (double) imageH * this.scale) / 2.0;
    }

    public void zoomAt(double factor) {
        zoomAt(factor, (double) viewW / 2.0, (double) viewH / 2.0);
    }

    public void zoomAt(double factor, double focalX, double focalY) {
        double targetScale = this.scale * factor;
        if (targetScale < minScale) {
            targetScale = minScale;
        }
        if (targetScale > maxScale) {
            targetScale = maxScale;
        }

        double effectiveFactor = targetScale / this.scale;
        this.tx = focalX - effectiveFactor * (focalX - this.tx);
        this.ty = focalY - effectiveFactor * (focalY - this.ty);
        this.scale = targetScale;

        clampTranslation();
    }

    public void pan(double dx, double dy) {
        this.tx += dx;
        this.ty += dy;
        clampTranslation();
    }

    private void clampTranslation() {
        double scaledW = (double) imageW * scale;
        if (scaledW <= (double) viewW) {
            this.tx = ((double) viewW - scaledW) / 2.0;
        } else {
            double minTx = (double) viewW - scaledW;
            double maxTx = 0.0;
            if (this.tx > maxTx) {
                this.tx = maxTx;
            }
            if (this.tx < minTx) {
                this.tx = minTx;
            }
        }

        double scaledH = (double) imageH * scale;
        if (scaledH <= (double) viewH) {
            this.ty = ((double) viewH - scaledH) / 2.0;
        } else {
            double minTy = (double) viewH - scaledH;
            double maxTy = 0.0;
            if (this.ty > maxTy) {
                this.ty = maxTy;
            }
            if (this.ty < minTy) {
                this.ty = minTy;
            }
        }
    }

    public double imageToViewX(double x) {
        return x * scale + tx;
    }

    public double imageToViewY(double y) {
        return y * scale + ty;
    }

    public double viewToImageX(double vx) {
        return (vx - tx) / scale;
    }

    public double viewToImageY(double vy) {
        return (vy - ty) / scale;
    }

    public double getScale() {
        return scale;
    }

    public double getTx() {
        return tx;
    }

    public double getTy() {
        return ty;
    }

    public double getFitScale() {
        return fitScale;
    }

    public double getMinScale() {
        return minScale;
    }

    public double getMaxScale() {
        return maxScale;
    }

    public int getImageW() {
        return imageW;
    }

    public int getImageH() {
        return imageH;
    }

    public int getViewW() {
        return viewW;
    }

    public int getViewH() {
        return viewH;
    }

    public double[] getViewportInImage() {
        return new double[]{
                viewToImageX(0.0),
                viewToImageY(0.0),
                viewToImageX((double) viewW),
                viewToImageY((double) viewH)
        };
    }
}
