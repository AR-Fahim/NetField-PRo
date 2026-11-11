package com.netfieldpro.core.json;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.netfieldpro.core.util.Int64Json;

public final class CanonicalJsonWriter {

    private static final int BUFFER_SIZE = 8192;

    private CanonicalJsonWriter() {
    }

    public static byte[] toBytes(JsonValue value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(value, baos);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException in toBytes", e);
        }
        return baos.toByteArray();
    }

    public static void write(JsonValue value, OutputStream out) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null");
        }

        ChunkedOutputStream chunkedOut = new ChunkedOutputStream(out, BUFFER_SIZE);
        writeValue(value, chunkedOut);
        chunkedOut.flush();
    }

    private static void writeValue(JsonValue value, ChunkedOutputStream out) throws IOException {
        if (value instanceof JsonObject obj) {
            out.write('{');
            boolean first = true;
            for (Map.Entry<String, JsonValue> entry : obj.properties().entrySet()) {
                if (!first) {
                    out.write(',');
                }
                first = false;
                writeString(entry.getKey(), out);
                out.write(':');
                writeValue(entry.getValue(), out);
            }
            out.write('}');
        } else if (value instanceof JsonArray arr) {
            out.write('[');
            boolean first = true;
            for (JsonValue elem : arr.elements()) {
                if (!first) {
                    out.write(',');
                }
                first = false;
                writeValue(elem, out);
            }
            out.write(']');
        } else if (value instanceof JsonString str) {
            writeString(str.value(), out);
        } else if (value instanceof JsonInt num) {
            long v = num.value();
            if (Int64Json.fitsNumber(v)) {
                byte[] ascii = Long.toString(v).getBytes(StandardCharsets.US_ASCII);
                out.write(ascii);
            } else {
                out.write('"');
                byte[] ascii = Long.toString(v).getBytes(StandardCharsets.US_ASCII);
                out.write(ascii);
                out.write('"');
            }
        } else if (value instanceof JsonBool bool) {
            byte[] ascii = (bool.value() ? "true" : "false").getBytes(StandardCharsets.US_ASCII);
            out.write(ascii);
        } else {
            throw new IllegalArgumentException("Unknown JsonValue type: " + value.getClass());
        }
    }

    private static void writeString(String s, ChunkedOutputStream out) throws IOException {
        out.write('"');
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < len && Character.isLowSurrogate(s.charAt(i + 1))) {
                    char low = s.charAt(i + 1);
                    int codePoint = Character.toCodePoint(c, low);
                    byte[] utf8 = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                    out.write(utf8);
                    i++;
                } else {
                    throw new IllegalArgumentException("Unpaired high surrogate in string");
                }
            } else if (Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("Unpaired low surrogate in string");
            } else if (c == '"') {
                out.write('\\');
                out.write('"');
            } else if (c == '\\') {
                out.write('\\');
                out.write('\\');
            } else if (c == '\b') {
                out.write('\\');
                out.write('b');
            } else if (c == '\t') {
                out.write('\\');
                out.write('t');
            } else if (c == '\n') {
                out.write('\\');
                out.write('n');
            } else if (c == '\f') {
                out.write('\\');
                out.write('f');
            } else if (c == '\r') {
                out.write('\\');
                out.write('r');
            } else if (c < 0x20) {
                out.write('\\');
                out.write('u');
                out.write('0');
                out.write('0');
                out.write(Character.forDigit((c >> 4) & 0x0F, 16));
                out.write(Character.forDigit(c & 0x0F, 16));
            } else {
                byte[] utf8 = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                out.write(utf8);
            }
        }
        out.write('"');
    }

    private static final class ChunkedOutputStream extends FilterOutputStream {
        private final byte[] buf;
        private int count = 0;

        ChunkedOutputStream(OutputStream out, int size) {
            super(out);
            this.buf = new byte[size];
        }

        @Override
        public void write(int b) throws IOException {
            if (count >= buf.length) {
                flushBuffer();
            }
            buf[count++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int remaining = len;
            int offset = off;
            while (remaining > 0) {
                int space = buf.length - count;
                if (space == 0) {
                    flushBuffer();
                    space = buf.length;
                }
                int toCopy = Math.min(remaining, space);
                System.arraycopy(b, offset, buf, count, toCopy);
                count += toCopy;
                offset += toCopy;
                remaining -= toCopy;
            }
        }

        private void flushBuffer() throws IOException {
            if (count > 0) {
                out.write(buf, 0, count);
                count = 0;
            }
        }

        @Override
        public void flush() throws IOException {
            flushBuffer();
            out.flush();
        }
    }
}
