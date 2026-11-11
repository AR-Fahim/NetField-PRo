package com.netfieldpro.core.util;

public final class Int64Json {

    public static final long MAX_SAFE_INTEGER = 9007199254740991L;
    public static final long MIN_SAFE_INTEGER = -9007199254740991L;

    private Int64Json() {
    }

    public static boolean fitsNumber(long v) {
        return v >= MIN_SAFE_INTEGER && v <= MAX_SAFE_INTEGER;
    }
}
