package com.netfieldpro.core.plan;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PlanStoragePolicy {

    public static final int MAX_IMAGE_LONG_SIDE_PX = 4096;
    public static final int PDF_TARGET_LONG_SIDE_PX = 3072;

    public enum Action {
        STORE_ORIGINAL,
        REENCODE,
        REENCODE_ROTATE,
        RASTERIZE
    }

    public record Decision(
            Action action,
            int targetWidth,
            int targetHeight,
            String targetFormat
    ) {
    }

    private PlanStoragePolicy() {
    }

    public static Decision decide(String mimeType, int width, int height, int exifOrientation) {
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType cannot be null");
        }
        String lower = mimeType.trim().toLowerCase();

        if ("application/pdf".equals(lower)) {
            int longSide = Math.max(width, height);
            int targetW;
            int targetH;
            if (width >= height) {
                targetW = PDF_TARGET_LONG_SIDE_PX;
                targetH = scaleDim(height, PDF_TARGET_LONG_SIDE_PX, longSide);
            } else {
                targetH = PDF_TARGET_LONG_SIDE_PX;
                targetW = scaleDim(width, PDF_TARGET_LONG_SIDE_PX, longSide);
            }
            return new Decision(Action.RASTERIZE, targetW, targetH, "png");
        }

        boolean isPng = "image/png".equals(lower);
        boolean isJpeg = "image/jpeg".equals(lower) || "image/jpg".equals(lower);

        if (!isPng && !isJpeg) {
            throw new IllegalArgumentException("Unsupported mimeType: " + mimeType);
        }

        boolean normalOrient = (exifOrientation == 1 || exifOrientation == 0);
        boolean swapsDim = (exifOrientation == 6 || exifOrientation == 8);

        int effW = swapsDim ? height : width;
        int effH = swapsDim ? width : height;
        int longSide = Math.max(effW, effH);

        if (normalOrient && longSide <= MAX_IMAGE_LONG_SIDE_PX) {
            String format = isPng ? "png" : "jpg";
            return new Decision(Action.STORE_ORIGINAL, width, height, format);
        }

        int targetW;
        int targetH;
        if (longSide <= MAX_IMAGE_LONG_SIDE_PX) {
            targetW = effW;
            targetH = effH;
        } else {
            if (effW >= effH) {
                targetW = MAX_IMAGE_LONG_SIDE_PX;
                targetH = scaleDim(effH, MAX_IMAGE_LONG_SIDE_PX, longSide);
            } else {
                targetH = MAX_IMAGE_LONG_SIDE_PX;
                targetW = scaleDim(effW, MAX_IMAGE_LONG_SIDE_PX, longSide);
            }
        }

        Action action = normalOrient ? Action.REENCODE : Action.REENCODE_ROTATE;
        return new Decision(action, targetW, targetH, "png");
    }

    private static int scaleDim(int dim, int targetLong, int sourceLong) {
        double scaled = ((double) dim) * ((double) targetLong) / ((double) sourceLong);
        return new BigDecimal(scaled).setScale(0, RoundingMode.HALF_EVEN).intValue();
    }
}
