package com.netfieldpro.core.heat;

import java.util.Arrays;

public final class Median {

    private Median() {
    }

    public static int tenths(int[] rssiDbm) {
        if (rssiDbm == null || rssiDbm.length == 0) {
            throw new IllegalArgumentException("rssiDbm cannot be null or empty");
        }
        int[] sorted = rssiDbm.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 1) {
            return sorted[n / 2] * 10;
        } else {
            return (sorted[n / 2 - 1] + sorted[n / 2]) * 5;
        }
    }
}
