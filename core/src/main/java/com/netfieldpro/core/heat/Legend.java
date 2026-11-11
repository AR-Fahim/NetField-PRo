package com.netfieldpro.core.heat;

public final class Legend {

    private Legend() {
    }

    public static int latencyBucket(long us) {
        if (us <= 10000L) return 0;
        if (us <= 25000L) return 1;
        if (us <= 50000L) return 2;
        if (us <= 100000L) return 3;
        return 4;
    }

    public static int jitterBucket(long thousandthsMs) {
        if (thousandthsMs <= 5000L) return 0;
        if (thousandthsMs <= 10000L) return 1;
        if (thousandthsMs <= 15000L) return 2;
        if (thousandthsMs <= 30000L) return 3;
        return 4;
    }

    public static int lossBucket(int lost) {
        if (lost <= 0) return 0;
        if (lost == 1) return 1;
        return 2;
    }

    public static int phyBucket(int mbps) {
        if (mbps >= 600) return 0;
        if (mbps >= 300) return 1;
        if (mbps >= 150) return 2;
        if (mbps >= 50) return 3;
        return 4;
    }

    public static String color(PaletteId id, int bucket, int bucketCount) {
        if (id == null) {
            id = PaletteId.TRAFFIC_LIGHT;
        }
        if (bucketCount == 3) {
            // 3 buckets use best, middle (FEE08B / 21908C), worst
            if (id == PaletteId.VIRIDIS) {
                switch (bucket) {
                    case 0: return "#FDE725";
                    case 1: return "#21908C";
                    default: return "#440154";
                }
            } else {
                switch (bucket) {
                    case 0: return "#1A9850";
                    case 1: return "#FEE08B";
                    default: return "#D73027";
                }
            }
        } else {
            // 5 buckets use the five anchors best -> worst
            // best: -55, then -67, -75, -85, worst: -95
            if (id == PaletteId.VIRIDIS) {
                switch (bucket) {
                    case 0: return "#FDE725";
                    case 1: return "#5DC863";
                    case 2: return "#21908C";
                    case 3: return "#3B528B";
                    default: return "#440154";
                }
            } else {
                switch (bucket) {
                    case 0: return "#1A9850";
                    case 1: return "#91CF60";
                    case 2: return "#FEE08B";
                    case 3: return "#FC8D59";
                    default: return "#D73027";
                }
            }
        }
    }
}
