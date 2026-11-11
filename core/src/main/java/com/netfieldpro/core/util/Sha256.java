package com.netfieldpro.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256 {

    private Sha256() {
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public static byte[] hash(byte[] src) {
        if (src == null) {
            throw new IllegalArgumentException("src cannot be null");
        }
        MessageDigest md = newDigest();
        return md.digest(src);
    }

    public static byte[] hash(InputStream in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("in cannot be null");
        }
        MessageDigest md = newDigest();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            md.update(buffer, 0, read);
        }
        return md.digest();
    }

    public static String hex(byte[] digest) {
        if (digest == null) {
            throw new IllegalArgumentException("digest cannot be null");
        }
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0x0F, 16));
            sb.append(Character.forDigit(b & 0x0F, 16));
        }
        return sb.toString();
    }

    public static String b64(byte[] digest) {
        return Base64Url.encode(digest);
    }
}
