package com.netfieldpro.core.plan;

public final class PlanImportValidator {

    public static final long MAX_FILE_SIZE_BYTES = 52428800L;
    public static final int MIN_SHORT_SIDE_PX = 200;

    public enum Status {
        OK,
        TOO_LARGE,
        UNSUPPORTED,
        TOO_SMALL,
        DECODE_FAILED
    }

    private PlanImportValidator() {
    }

    public static Status validateFileSize(long bytes) {
        if (bytes < 0 || bytes > MAX_FILE_SIZE_BYTES) {
            return Status.TOO_LARGE;
        }
        return Status.OK;
    }

    public static Status validateMimeType(String mimeType) {
        if (mimeType == null) {
            return Status.UNSUPPORTED;
        }
        String lower = mimeType.trim().toLowerCase();
        if ("image/png".equals(lower) || "image/jpeg".equals(lower) || "application/pdf".equals(lower)) {
            return Status.OK;
        }
        return Status.UNSUPPORTED;
    }

    public static Status validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            return Status.DECODE_FAILED;
        }
        int shortSide = Math.min(width, height);
        if (shortSide < MIN_SHORT_SIDE_PX) {
            return Status.TOO_SMALL;
        }
        return Status.OK;
    }
}
