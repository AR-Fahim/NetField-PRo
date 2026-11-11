package com.netfieldpro.core.json;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.netfieldpro.core.json.JsonParseException.ErrorCode;

public class JsonTest {

    @Test
    public void testCJ1() {
        JsonObject obj = new JsonObject();
        obj.put("b", new JsonInt(1));
        obj.put("a", new JsonInt(2));
        assertEquals("{\"a\":2,\"b\":1}", new String(CanonicalJsonWriter.toBytes(obj), StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ2() {
        JsonObject obj = new JsonObject();
        obj.put("\uFF5E", new JsonInt(2));
        obj.put("\uD83D\uDE00", new JsonInt(1));
        byte[] bytes = CanonicalJsonWriter.toBytes(obj);
        String hex = bytesToHex(bytes);
        assertEquals("7B22F09F9880223A312C22EFBD9E223A327D", hex);
    }

    @Test
    public void testCJ3_and_CJ3b() {
        assertEquals("9007199254740991", new String(CanonicalJsonWriter.toBytes(new JsonInt(9007199254740991L)), StandardCharsets.UTF_8));
        assertEquals("\"9007199254740992\"", new String(CanonicalJsonWriter.toBytes(new JsonInt(9007199254740992L)), StandardCharsets.UTF_8));
        assertEquals("\"-9007199254740992\"", new String(CanonicalJsonWriter.toBytes(new JsonInt(-9007199254740992L)), StandardCharsets.UTF_8));
        assertEquals("\"9223372036854775807\"", new String(CanonicalJsonWriter.toBytes(new JsonInt(Long.MAX_VALUE)), StandardCharsets.UTF_8));
        assertEquals("\"-9223372036854775808\"", new String(CanonicalJsonWriter.toBytes(new JsonInt(Long.MIN_VALUE)), StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ5() {
        String s = "a\"b\\c" + "\u0001" + "\n" + "\u007F" + "\u2028";
        byte[] bytes = CanonicalJsonWriter.toBytes(new JsonString(s));
        String expected = "\"a\\\"b\\\\c\\u0001\\n\u007F\u2028\"";
        assertEquals(expected, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ6() {
        JsonObject obj = new JsonObject();
        obj.put("a", null);
        obj.put("b", new JsonInt(1));
        assertEquals("{\"b\":1}", new String(CanonicalJsonWriter.toBytes(obj), StandardCharsets.UTF_8));
        assertEquals("{}", new String(CanonicalJsonWriter.toBytes(new JsonObject()), StandardCharsets.UTF_8));
        assertEquals("[]", new String(CanonicalJsonWriter.toBytes(new JsonArray()), StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ7() {
        JsonObject inner = new JsonObject();
        inner.put("b", JsonBool.TRUE);
        inner.put("a", JsonBool.FALSE);
        JsonArray arr = new JsonArray();
        arr.add(inner);
        JsonObject root = new JsonObject();
        root.put("z", arr);
        root.put("a", new JsonObject());
        assertEquals("{\"a\":{},\"z\":[{\"a\":false,\"b\":true}]}", new String(CanonicalJsonWriter.toBytes(root), StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ8() {
        JsonObject obj = new JsonObject();
        obj.put("z", new JsonInt(1));
        obj.put("\u00E9", new JsonInt(1));
        obj.put("a", new JsonInt(1));
        obj.put("B", new JsonInt(1));
        assertEquals("{\"B\":1,\"a\":1,\"z\":1,\"\u00E9\":1}", new String(CanonicalJsonWriter.toBytes(obj), StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ9() {
        assertEquals("\"a/b\"", new String(CanonicalJsonWriter.toBytes(new JsonString("a/b")), StandardCharsets.UTF_8));
    }

    @Test
    public void testCJ10() throws IOException {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < 100000; i++) {
            arr.add(new JsonInt(i));
        }
        AtomicInteger maxChunk = new AtomicInteger(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream trackingOut = new OutputStream() {
            @Override
            public void write(int b) {
                fail("Single byte write should not occur directly through buffer");
            }

            @Override
            public void write(byte[] b, int off, int len) {
                if (len > maxChunk.get()) {
                    maxChunk.set(len);
                }
                baos.write(b, off, len);
            }
        };

        CanonicalJsonWriter.write(arr, trackingOut);
        assertEquals(588891, baos.size());
        assertTrue("Max chunk was " + maxChunk.get() + ", expected <= 8192", maxChunk.get() <= 8192);
    }

    @Test
    public void testCJ11() {
        try {
            CanonicalJsonWriter.toBytes(new JsonString("unpaired: \uD800 alone"));
            fail("Expected IllegalArgumentException for lone surrogate");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testCV1() {
        CanonicalJson.verifyCanonical("{\"a\":1}".getBytes(StandardCharsets.UTF_8));

        try {
            CanonicalJson.verifyCanonical("{\"b\":1,\"a\":2}".getBytes(StandardCharsets.UTF_8));
            fail("Expected NOT_CANONICAL");
        } catch (IllegalArgumentException e) {
            assertEquals("NOT_CANONICAL", e.getMessage());
        }

        try {
            CanonicalJson.verifyCanonical("{ \"a\":1}".getBytes(StandardCharsets.UTF_8));
            fail("Expected NOT_CANONICAL");
        } catch (IllegalArgumentException e) {
            assertEquals("NOT_CANONICAL", e.getMessage());
        }
    }

    @Test
    public void testParser_P1_to_P4() throws Exception {
        JsonValue val1 = JsonParser.parse("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        assertTrue(val1 instanceof JsonObject);

        JsonValue val2 = JsonParser.parse(" { \"a\" : [ true , \"x\" ] } ".getBytes(StandardCharsets.UTF_8));
        assertTrue(val2 instanceof JsonObject);

        assertParseException("{\"a\":1,\"a\":2}", ErrorCode.DUPLICATE_KEY, 7);
        assertParseException("{\"a\":null}", ErrorCode.NULL_NOT_ALLOWED, 5);
    }

    @Test
    public void testParser_P5_to_P7() {
        assertParseException("[1.5]", ErrorCode.FLOAT_NOT_ALLOWED, 1);
        assertParseException("[1e3]", ErrorCode.FLOAT_NOT_ALLOWED, 1);

        assertParseException("[01]", ErrorCode.BAD_NUMBER, 1);
        assertParseException("[-0]", ErrorCode.BAD_NUMBER, 1);
        assertParseException("[-]", ErrorCode.BAD_NUMBER, 1);

        try {
            JsonParser.parse("[9007199254740991]".getBytes(StandardCharsets.UTF_8));
        } catch (JsonParseException e) {
            fail("9007199254740991 should be valid: " + e);
        }
        assertParseException("[9007199254740992]", ErrorCode.NUMBER_OUT_OF_RANGE, 1);
        assertParseException("[-9007199254740992]", ErrorCode.NUMBER_OUT_OF_RANGE, 1);
    }

    @Test
    public void testParser_P8_to_P10() throws Exception {
        assertParseException("[\"\\ud83d\"]", ErrorCode.LONE_SURROGATE, 2);

        JsonValue v = JsonParser.parse("[\"\\ud83d\\ude00\"]".getBytes(StandardCharsets.UTF_8));
        assertTrue(v instanceof JsonArray);
        assertEquals("\uD83D\uDE00", ((JsonString) ((JsonArray) v).get(0)).value());

        assertParseException("[\"\\x\"]", ErrorCode.BAD_ESCAPE, 2);
        assertParseException("[\"\t\"]", ErrorCode.BAD_CONTROL_CHAR, 2);
    }

    @Test
    public void testParser_P11() {
        assertParseException("{}x", ErrorCode.TRAILING_DATA, 2);
        assertParseException("", ErrorCode.UNEXPECTED_EOF, 0);
        assertParseException("[1,]", ErrorCode.UNEXPECTED_CHAR, 3);
        assertParseException("{\"a\":1", ErrorCode.UNEXPECTED_EOF, 6);
    }

    @Test
    public void testParser_P12() throws Exception {
        StringBuilder sbOk = new StringBuilder();
        for (int i = 0; i < 64; i++) sbOk.append('[');
        for (int i = 0; i < 64; i++) sbOk.append(']');
        JsonParser.parse(sbOk.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder sbTooDeep = new StringBuilder();
        for (int i = 0; i < 65; i++) sbTooDeep.append('[');
        for (int i = 0; i < 65; i++) sbTooDeep.append(']');
        assertParseException(sbTooDeep.toString(), ErrorCode.DEPTH_EXCEEDED, 64);
    }

    @Test
    public void testParser_P13_and_P14() throws Exception {
        byte[] badUtf8 = new byte[]{(byte) 0x22, (byte) 0xFF, (byte) 0x22};
        try {
            JsonParser.parse(badUtf8);
            fail("Expected BAD_UTF8");
        } catch (JsonParseException e) {
            assertEquals(ErrorCode.BAD_UTF8, e.getCode());
        }

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '{', '}'};
        try {
            JsonParser.parse(bom);
            fail("Expected UNEXPECTED_CHAR for BOM");
        } catch (JsonParseException e) {
            assertEquals(ErrorCode.UNEXPECTED_CHAR, e.getCode());
            assertEquals(0, e.getOffset());
        }

        JsonValue v = JsonParser.parse("\"abc\"".getBytes(StandardCharsets.UTF_8));
        assertTrue(v instanceof JsonString);
        assertEquals("abc", ((JsonString) v).value());
    }

    @Test
    public void testRoundTripProperty() throws Exception {
        String[] fixtures = {
                "{\"a\":2,\"b\":1}",
                "{\"a\":{},\"z\":[{\"a\":false,\"b\":true}]}",
                "{\"B\":1,\"a\":1,\"z\":1,\"\u00E9\":1}",
                "\"a/b\"",
                "\"abc\""
        };
        for (String f : fixtures) {
            byte[] in = f.getBytes(StandardCharsets.UTF_8);
            JsonValue parsed = JsonParser.parse(in);
            byte[] out = CanonicalJsonWriter.toBytes(parsed);
            assertArrayEquals("Failed round-trip for " + f, in, out);
        }
    }

    private static void assertParseException(String json, ErrorCode expectedCode, int expectedOffset) {
        try {
            JsonParser.parse(json.getBytes(StandardCharsets.UTF_8));
            fail("Expected JsonParseException with " + expectedCode + " at " + expectedOffset + " for: " + json);
        } catch (JsonParseException e) {
            assertEquals("Wrong error code for " + json, expectedCode, e.getCode());
            assertEquals("Wrong offset for " + json, expectedOffset, e.getOffset());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
