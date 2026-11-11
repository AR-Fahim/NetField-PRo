package com.netfieldpro.core.model.serial;

import java.util.regex.Pattern;

import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;

public final class DecimalFields {

    private static final Pattern[] DP_PATTERNS = new Pattern[11];

    static {
        DP_PATTERNS[0] = Pattern.compile("^-?(0|[1-9][0-9]*)$");
        for (int i = 1; i <= 10; i++) {
            DP_PATTERNS[i] = Pattern.compile("^-?(0|[1-9][0-9]*)\\.[0-9]{" + i + "}$");
        }
    }

    private DecimalFields() {
    }

    public static double parseDouble(JsonValue v, String field, int dp) {
        if (!(v instanceof JsonString js)) {
            throw new IllegalArgumentException("NOT_CANONICAL_DECIMAL:" + field);
        }
        String s = js.value();
        Pattern pattern = (dp >= 0 && dp <= 10)
                ? DP_PATTERNS[dp]
                : Pattern.compile("^-?(0|[1-9][0-9]*)\\.[0-9]{" + dp + "}$");

        if (!pattern.matcher(s).matches()) {
            throw new IllegalArgumentException("NOT_CANONICAL_DECIMAL:" + field);
        }
        if (s.startsWith("-")) {
            boolean allZero = true;
            for (int i = 1; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c != '0' && c != '.') {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                throw new IllegalArgumentException("NOT_CANONICAL_DECIMAL:" + field);
            }
        }
        return Double.parseDouble(s);
    }

    public static Double parseOptionalDouble(JsonValue v, String field, int dp) {
        if (v == null) {
            return null;
        }
        return parseDouble(v, field, dp);
    }
}

