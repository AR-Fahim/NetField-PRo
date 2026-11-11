package com.netfieldpro.core.plan;

public final class TilePlanner {

    public record TilePlan(
            boolean needsTile,
            int left,
            int top,
            int right,
            int bottom,
            int sampleSize
    ) {
    }

    private TilePlanner() {
    }

    public static TilePlan plan(
            int imageW, int imageH,
            int overviewLong,
            double vpLeft, double vpTop, double vpRight, double vpBottom,
            int screenLong
    ) {
        int imageLong = Math.max(imageW, imageH);
        if (imageLong <= 0 || screenLong <= 0 || overviewLong <= 0) {
            return new TilePlan(false, 0, 0, 0, 0, 1);
        }

        double vpW = vpRight - vpLeft;
        double vpH = vpBottom - vpTop;
        double viewportLong = Math.max(vpW, vpH);
        if (viewportLong <= 0.0) {
            return new TilePlan(false, 0, 0, 0, 0, 1);
        }

        double overviewRatio = (double) overviewLong / (double) imageLong;
        double displayRatio = (double) screenLong / viewportLong;

        if (displayRatio <= overviewRatio) {
            return new TilePlan(false, 0, 0, 0, 0, 1);
        }

        int left = (int) Math.round(Math.max(0.0, Math.min((double) imageW, vpLeft)));
        int top = (int) Math.round(Math.max(0.0, Math.min((double) imageH, vpTop)));
        int right = (int) Math.round(Math.max(0.0, Math.min((double) imageW, vpRight)));
        int bottom = (int) Math.round(Math.max(0.0, Math.min((double) imageH, vpBottom)));

        int sampleSize = 1;
        while ((viewportLong / sampleSize) > screenLong) {
            sampleSize *= 2;
        }

        return new TilePlan(true, left, top, right, bottom, sampleSize);
    }
}
