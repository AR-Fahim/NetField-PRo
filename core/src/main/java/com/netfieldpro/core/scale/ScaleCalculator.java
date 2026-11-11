package com.netfieldpro.core.scale;

import com.netfieldpro.core.util.FixedDecimal;

public final class ScaleCalculator {

    public static final double EPSILON_PX = 3.0;
    public static final double MIN_DISTANCE_M = 0.5;
    public static final double MAX_DISTANCE_M = 500.0;
    public static final double MIN_SPAN_PERCENT = 15.0;

    public enum ValidationStatus {
        OK,
        TOO_SHORT,
        INVALID_DISTANCE
    }

    public record ScaleResult(
            double metersPerPixel,
            double uncertaintyPct,
            String mppFormatted,
            String uncertaintyFormatted,
            double pixelLength,
            boolean spanOk
    ) {
    }

    private ScaleCalculator() {
    }

    public static ValidationStatus validateDistance(double meters) {
        if (Double.isNaN(meters) || Double.isInfinite(meters) || meters < MIN_DISTANCE_M || meters > MAX_DISTANCE_M) {
            return ValidationStatus.INVALID_DISTANCE;
        }
        return ValidationStatus.OK;
    }

    public static ValidationStatus validateLineSpan(double x1, double y1, double x2, double y2, int imageW, int imageH) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lPx = Math.sqrt(dx * dx + dy * dy);
        if (lPx <= 0.0) {
            return ValidationStatus.TOO_SHORT;
        }
        double longerSide = Math.max(imageW, imageH);
        if (100.0 * lPx < MIN_SPAN_PERCENT * longerSide) {
            return ValidationStatus.TOO_SHORT;
        }
        return ValidationStatus.OK;
    }

    public static ScaleResult calculateLine(
            double x1, double y1, double x2, double y2,
            double distanceM,
            int imageW, int imageH
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lPx = Math.sqrt(dx * dx + dy * dy);
        double longerSide = Math.max(imageW, imageH);
        boolean spanOk = (lPx > 0.0) && (100.0 * lPx >= MIN_SPAN_PERCENT * longerSide);

        double mpp = (lPx > 0.0) ? (distanceM / lPx) : 0.0;
        double uncertainty = (lPx > 0.0) ? (100.0 * Math.sqrt(2.0) * EPSILON_PX / lPx + 5.0) : 0.0;

        String mppFmt = FixedDecimal.format(mpp, 6);
        String uncFmt = FixedDecimal.format(uncertainty, 3);

        return new ScaleResult(mpp, uncertainty, mppFmt, uncFmt, lPx, spanOk);
    }

    public static ScaleResult calculateRoomWidth(double distanceM, int imageW) {
        double lPx = (double) imageW;
        double mpp = (lPx > 0.0) ? (distanceM / lPx) : 0.0;
        double uncertainty = (lPx > 0.0) ? (100.0 * Math.sqrt(2.0) * EPSILON_PX / lPx + 15.0) : 0.0;

        String mppFmt = FixedDecimal.format(mpp, 6);
        String uncFmt = FixedDecimal.format(uncertainty, 3);

        return new ScaleResult(mpp, uncertainty, mppFmt, uncFmt, lPx, true);
    }

    public static double metersBetween(double x1, double y1, double x2, double y2, double mpp) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lPx = Math.sqrt(dx * dx + dy * dy);
        return lPx * mpp;
    }

    public static String metersBetweenFormatted(double x1, double y1, double x2, double y2, double mpp) {
        double m = metersBetween(x1, y1, x2, y2, mpp);
        return FixedDecimal.format(m, 3);
    }
}
