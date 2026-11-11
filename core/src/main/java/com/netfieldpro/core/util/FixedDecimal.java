package com.netfieldpro.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FixedDecimal {

    private FixedDecimal() {
    }

    public static String format(double v, int dp) {
        if (dp < 0 || dp > 9) {
            throw new IllegalArgumentException("dp must be between 0 and 9: " + dp);
        }
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException("Value must be finite: " + v);
        }
        String formatted = new BigDecimal(v).setScale(dp, RoundingMode.HALF_EVEN).toPlainString();
        if (formatted.startsWith("-")) {
            boolean allZero = true;
            for (int i = 1; i < formatted.length(); i++) {
                char c = formatted.charAt(i);
                if (c != '0' && c != '.') {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                formatted = formatted.substring(1);
            }
        }
        return formatted;
    }
}
