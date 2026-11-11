package com.netfieldpro.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

public class PrimitivesTest {

    @Test
    public void testFixedDecimal_FD1_to_FD5() {
        assertEquals("0.12", FixedDecimal.format(0.125, 2));
        assertEquals("0.38", FixedDecimal.format(0.375, 2));
        assertEquals("2.67", FixedDecimal.format(2.675, 2));
        assertEquals("0.00", FixedDecimal.format(-0.004, 2));
        assertEquals("0.000", FixedDecimal.format(-0.0, 3));
    }

    @Test
    public void testFixedDecimal_FD6() {
        assertEquals("0", FixedDecimal.format(0.5, 0));
        assertEquals("2", FixedDecimal.format(1.5, 0));
        assertEquals("2", FixedDecimal.format(2.5, 0));
        assertEquals("-2", FixedDecimal.format(-2.5, 0));
    }

    @Test
    public void testFixedDecimal_FD7() {
        assertEquals("100000000000000000000.0", FixedDecimal.format(1e20, 1));
    }

    @Test
    public void testFixedDecimal_FD8() {
        assertThrowsIllegalArgument(() -> FixedDecimal.format(Double.NaN, 2));
        assertThrowsIllegalArgument(() -> FixedDecimal.format(Double.POSITIVE_INFINITY, 2));
        assertThrowsIllegalArgument(() -> FixedDecimal.format(1.0, -1));
        assertThrowsIllegalArgument(() -> FixedDecimal.format(1.0, 10));
    }

    @Test
    public void testInt64Json_NU1() {
        assertFalse(Int64Json.fitsNumber(Long.MAX_VALUE));
        assertFalse(Int64Json.fitsNumber(Long.MIN_VALUE));
        assertTrue(Int64Json.fitsNumber(9007199254740991L));
        assertFalse(Int64Json.fitsNumber(9007199254740992L));
        assertTrue(Int64Json.fitsNumber(-9007199254740991L));
        assertFalse(Int64Json.fitsNumber(-9007199254740992L));
    }

    @Test
    public void testBase64Url_B64_1() {
        byte[] bytes = new byte[]{(byte) 0xFB, (byte) 0xFF};
        assertEquals("-_8", Base64Url.encode(bytes));
        assertArrayEquals(bytes, Base64Url.decode("-_8"));
    }

    @Test
    public void testBase64Url_B64_2() {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            bytes[i] = (byte) i;
        }
        assertEquals("AAECAwQFBgcICQoLDA0ODw", Base64Url.encode(bytes));
        assertArrayEquals(bytes, Base64Url.decode("AAECAwQFBgcICQoLDA0ODw"));
    }

    @Test
    public void testBase64Url_B64_3() {
        assertThrowsIllegalArgument(() -> Base64Url.decode("AB"));
        assertThrowsIllegalArgument(() -> Base64Url.decode("A="));
        assertThrowsIllegalArgument(() -> Base64Url.decode("A+"));
        assertThrowsIllegalArgument(() -> Base64Url.decode("A"));
    }

    @Test
    public void testSha256_HA1_to_HA3() {
        byte[] ha1 = Sha256.hash(new byte[0]);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(ha1));
        assertEquals("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", Sha256.b64(ha1));

        byte[] ha2 = Sha256.hash("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.hex(ha2));

        byte[] ha3 = Sha256.hash("{}".getBytes(StandardCharsets.UTF_8));
        assertEquals("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", Sha256.hex(ha3));
    }

    @Test
    public void testSha256_HA4_and_HA5() throws IOException {
        int count = 1_000_000;
        byte[] millionA = new byte[count];
        Arrays.fill(millionA, (byte) 'a');
        String expectedHex = "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0";

        byte[] dByte = Sha256.hash(millionA);
        assertEquals(expectedHex, Sha256.hex(dByte));

        byte[] dStreamDefault = Sha256.hash(new ByteArrayInputStream(millionA));
        assertEquals(expectedHex, Sha256.hex(dStreamDefault));

        for (int bufSize : new int[]{1, 7, 8192}) {
            byte[] d = hashWithCustomBuffer(new ByteArrayInputStream(millionA), bufSize);
            assertEquals(expectedHex, Sha256.hex(d));
        }
    }

    private static byte[] hashWithCustomBuffer(InputStream in, int bufSize) throws IOException {
        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] buf = new byte[bufSize];
        int r;
        while ((r = in.read(buf)) != -1) {
            md.update(buf, 0, r);
        }
        return md.digest();
    }

    @Test
    public void testEcdsaCodec_DER1_to_DER3() {
        byte[] der1 = hexToBytes("3006020101020102");
        byte[] raw1 = EcdsaCodec.derToRaw(der1);
        assertEquals(64, raw1.length);
        assertEquals(1, raw1[31]);
        assertEquals(2, raw1[63]);

        byte[] der2 = hexToBytes("30080202008002020081");
        byte[] raw2 = EcdsaCodec.derToRaw(der2);
        assertEquals(64, raw2.length);
        assertEquals((byte) 0x80, raw2[31]);
        assertEquals((byte) 0x81, raw2[63]);

        byte[] rawZeroR = new byte[64];
        rawZeroR[63] = 1;
        assertThrowsInvalidSignature(() -> EcdsaCodec.rawToDer(rawZeroR));

        byte[] rawZeroS = new byte[64];
        rawZeroS[31] = 1;
        assertThrowsInvalidSignature(() -> EcdsaCodec.rawToDer(rawZeroS));
    }

    @Test
    public void testEcdsaCodec_DER4() {
        byte[] raw64FF = new byte[64];
        Arrays.fill(raw64FF, (byte) 0xFF);
        byte[] der = EcdsaCodec.rawToDer(raw64FF);
        assertEquals(72, der.length);
        assertEquals((byte) 0x30, der[0]);
        assertEquals((byte) 0x46, der[1]);
        assertEquals((byte) 0x02, der[2]);
        assertEquals((byte) 0x21, der[3]);
        assertEquals((byte) 0x00, der[4]);
        for (int i = 5; i < 37; i++) {
            assertEquals((byte) 0xFF, der[i]);
        }
        assertEquals((byte) 0x02, der[37]);
        assertEquals((byte) 0x21, der[38]);
        assertEquals((byte) 0x00, der[39]);
        for (int i = 40; i < 72; i++) {
            assertEquals((byte) 0xFF, der[i]);
        }
    }

    @Test
    public void testEcdsaCodec_DER5() {
        byte[] der2 = hexToBytes("30080202008002020081");
        byte[] raw = EcdsaCodec.derToRaw(der2);
        byte[] roundTripDer = EcdsaCodec.rawToDer(raw);
        assertArrayEquals(der2, roundTripDer);
    }

    @Test
    public void testEcdsaCodec_DER6() {
        byte[] nonMinimalDer = hexToBytes("300702020001020101");
        assertThrowsInvalidSignature(() -> EcdsaCodec.derToRaw(nonMinimalDer));
    }

    @Test
    public void testIdGenerator_IDA_and_IDB() {
        byte[] zeroes = new byte[16];
        assertEquals("AAAAAAAAAAAAAAAAAAAAAA", IdGenerator.fromBytes(zeroes));

        byte[] ffs = new byte[16];
        Arrays.fill(ffs, (byte) 0xFF);
        assertEquals("_____________________w", IdGenerator.fromBytes(ffs));

        byte[] zeroToF = new byte[16];
        for (int i = 0; i < 16; i++) {
            zeroToF[i] = (byte) i;
        }
        assertEquals("AAECAwQFBgcICQoLDA0ODw", IdGenerator.fromBytes(zeroToF));

        assertTrue(IdGenerator.isValid("AAAAAAAAAAAAAAAAAAAAAA"));
        assertFalse(IdGenerator.isValid("AAAAAAAAAAAAAAAAAAAAA")); // 21 chars
        assertFalse(IdGenerator.isValid("AAAAAAAAAAAAAAAAAAAAAAA")); // 23 chars
        assertFalse(IdGenerator.isValid("AAAAAAAAAAAAAAAAAAAAA=")); // '='
        assertFalse(IdGenerator.isValid("AAAAAAAAAAAAAAAAAAAAA+")); // '+'

        String gen = IdGenerator.generate();
        assertEquals(22, gen.length());
        assertTrue(IdGenerator.isValid(gen));
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static void assertThrowsIllegalArgument(Runnable r) {
        try {
            r.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    private static void assertThrowsInvalidSignature(Runnable r) {
        try {
            r.run();
            fail("Expected INVALID_SIGNATURE");
        } catch (IllegalArgumentException expected) {
            assertEquals("INVALID_SIGNATURE", expected.getMessage());
        }
    }
}
