package com.netfieldpro.core.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern G2_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{22}$");

    private IdGenerator() {
    }

    public static String fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Id bytes must be exactly 16 bytes");
        }
        return Base64Url.encode(bytes);
    }

    public static String generate() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return fromBytes(bytes);
    }

    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        return G2_PATTERN.matcher(id).matches();
    }
}
