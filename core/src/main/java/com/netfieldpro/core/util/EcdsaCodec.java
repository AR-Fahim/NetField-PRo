package com.netfieldpro.core.util;

import java.io.ByteArrayOutputStream;

public final class EcdsaCodec {

    private static final String ERR = "INVALID_SIGNATURE";

    private EcdsaCodec() {
    }

    public static byte[] derToRaw(byte[] der) {
        if (der == null || der.length < 8) {
            throw new IllegalArgumentException(ERR);
        }
        if (der[0] != 0x30) {
            throw new IllegalArgumentException(ERR);
        }

        int seqLen = der[1] & 0xFF;
        if (seqLen > 127) {
            throw new IllegalArgumentException(ERR);
        }
        if (der.length != 2 + seqLen) {
            throw new IllegalArgumentException(ERR);
        }

        int idx = 2;
        // Parse r
        if (idx >= der.length || der[idx] != 0x02) {
            throw new IllegalArgumentException(ERR);
        }
        idx++;
        int rLen = der[idx] & 0xFF;
        idx++;
        if (rLen == 0 || idx + rLen > der.length) {
            throw new IllegalArgumentException(ERR);
        }
        byte[] rBytes = validateAndExtractInteger(der, idx, rLen);
        idx += rLen;

        // Parse s
        if (idx >= der.length || der[idx] != 0x02) {
            throw new IllegalArgumentException(ERR);
        }
        idx++;
        int sLen = der[idx] & 0xFF;
        idx++;
        if (sLen == 0 || idx + sLen != der.length) {
            throw new IllegalArgumentException(ERR);
        }
        byte[] sBytes = validateAndExtractInteger(der, idx, sLen);

        byte[] raw = new byte[64];
        System.arraycopy(rBytes, 0, raw, 0, 32);
        System.arraycopy(sBytes, 0, raw, 32, 32);
        return raw;
    }

    private static byte[] validateAndExtractInteger(byte[] buf, int offset, int len) {
        if ((buf[offset] & 0x80) != 0) {
            throw new IllegalArgumentException(ERR);
        }
        // Minimal integer check
        if (len > 1 && buf[offset] == 0) {
            if ((buf[offset + 1] & 0x80) == 0) {
                throw new IllegalArgumentException(ERR);
            }
        }

        // Check for zero
        boolean allZero = true;
        for (int i = 0; i < len; i++) {
            if (buf[offset + i] != 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            throw new IllegalArgumentException(ERR);
        }

        int srcOffset = offset;
        int srcLen = len;
        if (srcLen > 1 && buf[srcOffset] == 0) {
            srcOffset++;
            srcLen--;
        }
        if (srcLen > 32) {
            throw new IllegalArgumentException(ERR);
        }

        byte[] out = new byte[32];
        System.arraycopy(buf, srcOffset, out, 32 - srcLen, srcLen);
        return out;
    }

    public static byte[] rawToDer(byte[] raw) {
        if (raw == null || raw.length != 64) {
            throw new IllegalArgumentException(ERR);
        }

        byte[] rEnc = encodeInteger(raw, 0, 32);
        byte[] sEnc = encodeInteger(raw, 32, 32);

        int seqLen = 2 + rEnc.length + 2 + sEnc.length;
        byte[] der = new byte[2 + seqLen];
        der[0] = 0x30;
        der[1] = (byte) seqLen;
        der[2] = 0x02;
        der[3] = (byte) rEnc.length;
        System.arraycopy(rEnc, 0, der, 4, rEnc.length);
        int sOffset = 4 + rEnc.length;
        der[sOffset] = 0x02;
        der[sOffset + 1] = (byte) sEnc.length;
        System.arraycopy(sEnc, 0, der, sOffset + 2, sEnc.length);
        return der;
    }

    private static byte[] encodeInteger(byte[] raw, int offset, int len) {
        int firstNz = -1;
        for (int i = 0; i < len; i++) {
            if (raw[offset + i] != 0) {
                firstNz = i;
                break;
            }
        }
        if (firstNz == -1) {
            throw new IllegalArgumentException(ERR);
        }

        int valLen = len - firstNz;
        boolean needZero = (raw[offset + firstNz] & 0x80) != 0;
        int outLen = needZero ? valLen + 1 : valLen;
        byte[] out = new byte[outLen];
        if (needZero) {
            out[0] = 0;
            System.arraycopy(raw, offset + firstNz, out, 1, valLen);
        } else {
            System.arraycopy(raw, offset + firstNz, out, 0, valLen);
        }
        return out;
    }

    public static byte[] compressPoint(byte[] x, byte[] y) {
        if (x == null || y == null) {
            throw new IllegalArgumentException(ERR);
        }
        byte[] normX = to32Bytes(x);
        byte[] normY = to32Bytes(y);
        byte[] out = new byte[33];
        out[0] = (byte) ((normY[31] & 1) == 0 ? 0x02 : 0x03);
        System.arraycopy(normX, 0, out, 1, 32);
        return out;
    }

    private static byte[] to32Bytes(byte[] b) {
        if (b.length == 32) {
            return b;
        }
        if (b.length == 33 && b[0] == 0) {
            byte[] r = new byte[32];
            System.arraycopy(b, 1, r, 0, 32);
            return r;
        }
        if (b.length < 32) {
            byte[] r = new byte[32];
            System.arraycopy(b, 0, r, 32 - b.length, b.length);
            return r;
        }
        throw new IllegalArgumentException(ERR);
    }
}
