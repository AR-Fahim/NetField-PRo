package com.netfieldpro.core.heat;

import java.util.Arrays;
import java.util.List;

public final class RadiusPolicy {

    private RadiusPolicy() {
    }

    public static double r(List<Sample> samples) {
        if (samples == null || samples.size() < 2) {
            return 25.0;
        }
        int n = samples.size();
        double[] nn = new double[n];
        for (int i = 0; i < n; i++) {
            Sample s1 = samples.get(i);
            double minD = Double.POSITIVE_INFINITY;
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                Sample s2 = samples.get(j);
                double dx = s1.x() - s2.x();
                double dy = s1.y() - s2.y();
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d < minD) {
                    minD = d;
                }
            }
            nn[i] = minD;
        }
        return rFromNearestNeighbors(nn);
    }

    public static double rFromNearestNeighbors(double[] nnList) {
        if (nnList == null || nnList.length < 2) {
            return 25.0;
        }
        double[] sorted = nnList.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        double med = (n % 2 == 1) ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
        return Math.max(4.0, Math.min(25.0, (double) Math.round(2.0 * med)));
    }

    public static double rEff(double r, double maxInfluenceM) {
        return Math.min(r, maxInfluenceM);
    }
}
