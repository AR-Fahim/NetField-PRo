package com.netfieldpro.core.heat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SecondaryOverlap {

    public static final double DEFAULT_SINGLE_GROUP_DBM = -95.0;

    private SecondaryOverlap() {
    }

    public static double second(double[] groupMaxes) {
        if (groupMaxes == null || groupMaxes.length < 2) {
            return DEFAULT_SINGLE_GROUP_DBM;
        }
        List<Double> valid = new ArrayList<>(groupMaxes.length);
        for (double v : groupMaxes) {
            if (!Double.isNaN(v)) {
                valid.add(v);
            }
        }
        if (valid.size() < 2) {
            return DEFAULT_SINGLE_GROUP_DBM;
        }
        valid.sort(Collections.reverseOrder());
        return valid.get(1);
    }

    public static double[] surface(List<double[]> perGroupSurfaces) {
        if (perGroupSurfaces == null || perGroupSurfaces.isEmpty()) {
            return new double[0];
        }
        int n = perGroupSurfaces.get(0).length;
        double[] result = new double[n];

        for (int i = 0; i < n; i++) {
            List<Double> valid = new ArrayList<>(perGroupSurfaces.size());
            for (double[] group : perGroupSurfaces) {
                if (group != null && i < group.length && !Double.isNaN(group[i])) {
                    valid.add(group[i]);
                }
            }
            if (valid.isEmpty()) {
                result[i] = Double.NaN;
            } else if (valid.size() == 1) {
                result[i] = DEFAULT_SINGLE_GROUP_DBM;
            } else {
                valid.sort(Collections.reverseOrder());
                result[i] = valid.get(1);
            }
        }

        return result;
    }
}
