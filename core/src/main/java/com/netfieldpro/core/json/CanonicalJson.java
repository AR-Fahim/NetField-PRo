package com.netfieldpro.core.json;

import java.util.Arrays;

public final class CanonicalJson {

    public static final String NOT_CANONICAL = "NOT_CANONICAL";

    private CanonicalJson() {
    }

    public static void verifyCanonical(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException(NOT_CANONICAL);
        }
        try {
            JsonValue val = JsonParser.parse(bytes);
            byte[] encoded = CanonicalJsonWriter.toBytes(val);
            if (!Arrays.equals(bytes, encoded)) {
                throw new IllegalArgumentException(NOT_CANONICAL);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(NOT_CANONICAL, e);
        }
    }
}
