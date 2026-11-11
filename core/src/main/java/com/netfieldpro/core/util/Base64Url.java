package com.netfieldpro.core.util;

import java.util.Base64;

public final class Base64Url {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private Base64Url() {
    }

    public static String encode(byte[] src) {
        if (src == null) {
            throw new IllegalArgumentException("src cannot be null");
        }
        return ENCODER.encodeToString(src);
    }

    public static byte[] decode(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        int len = s.length();
        if (len % 4 == 1) {
            throw new IllegalArgumentException("Invalid Base64Url length: " + len);
        }
        if (s.indexOf('=') >= 0 || s.indexOf('+') >= 0 || s.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Input contains invalid characters (=, +, or /)");
        }

        int rem = len % 4;
        if (rem == 2) {
            char last = s.charAt(len - 1);
            int val = charToValue(last);
            if ((val & 0x0F) != 0) {
                throw new IllegalArgumentException("Non-zero trailing bits in Base64Url string");
            }
        } else if (rem == 3) {
            char last = s.charAt(len - 1);
            int val = charToValue(last);
            if ((val & 0x03) != 0) {
                throw new IllegalArgumentException("Non-zero trailing bits in Base64Url string");
            }
        }

        for (int i = 0; i < len; i++) {
            charToValue(s.charAt(i));
        }

        return Base64.getUrlDecoder().decode(s);
    }

    private static int charToValue(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        } else if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        } else if (c == '-') {
            return 62;
        } else if (c == '_') {
            return 63;
        } else {
            throw new IllegalArgumentException("Invalid Base64Url character: " + c);
        }
    }
}
