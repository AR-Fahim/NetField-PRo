package com.netfieldpro.core.model.serial;

import java.util.regex.Pattern;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.util.Int64Json;

public final class IntFields {

    private static final Pattern STRING_INT_PATTERN = Pattern.compile("^-?[1-9][0-9]*$");

    private IntFields() {
    }

    public static long parseLong(JsonValue v, String field) {
        if (v instanceof JsonInt ji) {
            long val = ji.value();
            if (Int64Json.fitsNumber(val)) {
                return val;
            }
            throw new IllegalArgumentException("NOT_CANONICAL_INT:" + field);
        } else if (v instanceof JsonString js) {
            String s = js.value();
            if (STRING_INT_PATTERN.matcher(s).matches()) {
                try {
                    long val = Long.parseLong(s);
                    if (!Int64Json.fitsNumber(val)) {
                        return val;
                    }
                } catch (NumberFormatException ignored) {
                    // Overflow
                }
            }
            throw new IllegalArgumentException("NOT_CANONICAL_INT:" + field);
        }
        throw new IllegalArgumentException("NOT_CANONICAL_INT:" + field);
    }

    public static int parseInt(JsonValue v, String field) {
        long val = parseLong(v, field);
        if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NOT_CANONICAL_INT:" + field);
        }
        return (int) val;
    }

    public static Long parseOptionalLong(JsonValue v, String field) {
        if (v == null) {
            return null;
        }
        return parseLong(v, field);
    }

    public static Integer parseOptionalInt(JsonValue v, String field) {
        if (v == null) {
            return null;
        }
        return parseInt(v, field);
    }
}

