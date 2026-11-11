package com.netfieldpro.core.json;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.netfieldpro.core.json.JsonParseException.ErrorCode;
import com.netfieldpro.core.util.Int64Json;

public final class JsonParser {

    private static final int MAX_DEPTH = 64;

    private final byte[] bytes;
    private int pos = 0;
    private int depth = 0;

    private JsonParser(byte[] bytes) {
        this.bytes = bytes;
    }

    public static JsonValue parse(byte[] bytes) throws JsonParseException {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null");
        }
        validateUtf8(bytes);

        JsonParser parser = new JsonParser(bytes);
        parser.skipWhitespace();
        if (parser.pos >= bytes.length) {
            throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
        }

        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos < bytes.length) {
            throw new JsonParseException(ErrorCode.TRAILING_DATA, parser.pos);
        }
        return value;
    }

    private static void validateUtf8(byte[] bytes) throws JsonParseException {
        // Check for UTF-8 BOM
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, 0);
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        CharBuffer cb = CharBuffer.allocate(bytes.length);
        CoderResult cr1 = decoder.decode(bb, cb, true);
        if (cr1.isError()) {
            throw new JsonParseException(ErrorCode.BAD_UTF8, bb.position());
        }
        CoderResult cr2 = decoder.flush(cb);
        if (cr2.isError()) {
            throw new JsonParseException(ErrorCode.BAD_UTF8, bb.position());
        }
    }

    private JsonValue parseValue() throws JsonParseException {
        skipWhitespace();
        if (pos >= bytes.length) {
            throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
        }

        byte b = bytes[pos];
        if (b == '{') {
            return parseObject();
        } else if (b == '[') {
            return parseArray();
        } else if (b == '"') {
            return new JsonString(parseString());
        } else if (b == 't' || b == 'f') {
            return parseBool();
        } else if (b == 'n') {
            parseNull();
            return null; // unreachable
        } else if (b == '-' || (b >= '0' && b <= '9')) {
            return parseNumber();
        } else {
            throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
        }
    }

    private JsonObject parseObject() throws JsonParseException {
        if (depth >= MAX_DEPTH) {
            throw new JsonParseException(ErrorCode.DEPTH_EXCEEDED, pos);
        }
        depth++;
        pos++; // consume '{'
        JsonObject obj = new JsonObject();
        Set<String> seenKeys = new HashSet<>();

        skipWhitespace();
        if (pos >= bytes.length) {
            throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
        }
        if (bytes[pos] == '}') {
            pos++;
            depth--;
            return obj;
        }

        while (true) {
            skipWhitespace();
            if (pos >= bytes.length) {
                throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
            }
            if (bytes[pos] != '"') {
                throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
            }

            int keyStart = pos;
            String key = parseString();
            if (seenKeys.contains(key)) {
                throw new JsonParseException(ErrorCode.DUPLICATE_KEY, keyStart);
            }
            seenKeys.add(key);

            skipWhitespace();
            if (pos >= bytes.length) {
                throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
            }
            if (bytes[pos] != ':') {
                throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
            }
            pos++; // consume ':'

            JsonValue val = parseValue();
            obj.put(key, val);

            skipWhitespace();
            if (pos >= bytes.length) {
                throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
            }
            if (bytes[pos] == ',') {
                pos++;
                skipWhitespace();
                if (pos >= bytes.length) {
                    throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
                }
                if (bytes[pos] == '}') {
                    throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
                }
            } else if (bytes[pos] == '}') {
                pos++;
                depth--;
                return obj;
            } else {
                throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
            }
        }
    }

    private JsonArray parseArray() throws JsonParseException {
        if (depth >= MAX_DEPTH) {
            throw new JsonParseException(ErrorCode.DEPTH_EXCEEDED, pos);
        }
        depth++;
        pos++; // consume '['
        JsonArray arr = new JsonArray();

        skipWhitespace();
        if (pos >= bytes.length) {
            throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
        }
        if (bytes[pos] == ']') {
            pos++;
            depth--;
            return arr;
        }

        while (true) {
            JsonValue val = parseValue();
            arr.add(val);

            skipWhitespace();
            if (pos >= bytes.length) {
                throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
            }
            if (bytes[pos] == ',') {
                pos++;
                skipWhitespace();
                if (pos >= bytes.length) {
                    throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
                }
                if (bytes[pos] == ']') {
                    throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
                }
            } else if (bytes[pos] == ']') {
                pos++;
                depth--;
                return arr;
            } else {
                throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, pos);
            }
        }
    }

    private String parseString() throws JsonParseException {
        pos++; // consume opening '"'
        StringBuilder sb = new StringBuilder();

        while (pos < bytes.length) {
            int b = bytes[pos] & 0xFF;
            if (b == '"') {
                pos++; // consume closing '"'
                return sb.toString();
            }

            if (b < 0x20) {
                throw new JsonParseException(ErrorCode.BAD_CONTROL_CHAR, pos);
            }

            if (b == '\\') {
                int escapeStart = pos;
                pos++;
                if (pos >= bytes.length) {
                    throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
                }
                char esc = (char) bytes[pos++];
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        int code = parseHex4(escapeStart);
                        if (Character.isHighSurrogate((char) code)) {
                            // Must be followed by \uDC00..\uDFFF
                            if (pos + 6 <= bytes.length && bytes[pos] == '\\' && bytes[pos + 1] == 'u') {
                                int nextEscapeStart = pos;
                                pos += 2;
                                int lowCode = parseHex4(nextEscapeStart);
                                if (Character.isLowSurrogate((char) lowCode)) {
                                    sb.append((char) code);
                                    sb.append((char) lowCode);
                                } else {
                                    throw new JsonParseException(ErrorCode.LONE_SURROGATE, escapeStart);
                                }
                            } else {
                                throw new JsonParseException(ErrorCode.LONE_SURROGATE, escapeStart);
                            }
                        } else if (Character.isLowSurrogate((char) code)) {
                            throw new JsonParseException(ErrorCode.LONE_SURROGATE, escapeStart);
                        } else {
                            sb.append((char) code);
                        }
                    }
                    default -> throw new JsonParseException(ErrorCode.BAD_ESCAPE, escapeStart);
                }
            } else if (b < 0x80) {
                sb.append((char) b);
                pos++;
            } else {
                // Multi-byte UTF-8 sequence (already validated as valid UTF-8)
                int seqLen;
                if ((b & 0xE0) == 0xC0) seqLen = 2;
                else if ((b & 0xF0) == 0xE0) seqLen = 3;
                else if ((b & 0xF8) == 0xF0) seqLen = 4;
                else throw new JsonParseException(ErrorCode.BAD_UTF8, pos);

                String charStr = new String(bytes, pos, seqLen, StandardCharsets.UTF_8);
                sb.append(charStr);
                pos += seqLen;
            }
        }

        throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
    }

    private int parseHex4(int escapeStart) throws JsonParseException {
        if (pos + 4 > bytes.length) {
            throw new JsonParseException(ErrorCode.UNEXPECTED_EOF, bytes.length);
        }
        int code = 0;
        for (int i = 0; i < 4; i++) {
            byte b = bytes[pos++];
            int digit = Character.digit((char) b, 16);
            if (digit == -1) {
                throw new JsonParseException(ErrorCode.BAD_ESCAPE, escapeStart);
            }
            code = (code << 4) | digit;
        }
        return code;
    }

    private JsonInt parseNumber() throws JsonParseException {
        int start = pos;
        boolean negative = false;
        if (bytes[pos] == '-') {
            negative = true;
            pos++;
            if (pos >= bytes.length) {
                throw new JsonParseException(ErrorCode.BAD_NUMBER, start);
            }
            if (bytes[pos] < '0' || bytes[pos] > '9') {
                throw new JsonParseException(ErrorCode.BAD_NUMBER, start);
            }
        }

        int digitsStart = pos;
        if (bytes[pos] == '0') {
            pos++;
            // Check leading zero followed by other digit
            if (pos < bytes.length && bytes[pos] >= '0' && bytes[pos] <= '9') {
                throw new JsonParseException(ErrorCode.BAD_NUMBER, start);
            }
            // Check negative zero: -0 is BAD_NUMBER
            if (negative) {
                // If it's -0 or -0.xxx etc.
                if (pos >= bytes.length || bytes[pos] != '.') {
                    throw new JsonParseException(ErrorCode.BAD_NUMBER, start);
                }
            }
        } else {
            while (pos < bytes.length && bytes[pos] >= '0' && bytes[pos] <= '9') {
                pos++;
            }
        }

        if (pos < bytes.length) {
            byte b = bytes[pos];
            if (b == '.' || b == 'e' || b == 'E') {
                throw new JsonParseException(ErrorCode.FLOAT_NOT_ALLOWED, start);
            }
        }

        String numStr = new String(bytes, start, pos - start, StandardCharsets.US_ASCII);
        try {
            long val = Long.parseLong(numStr);
            if (!Int64Json.fitsNumber(val)) {
                throw new JsonParseException(ErrorCode.NUMBER_OUT_OF_RANGE, start);
            }
            return new JsonInt(val);
        } catch (NumberFormatException e) {
            throw new JsonParseException(ErrorCode.NUMBER_OUT_OF_RANGE, start);
        }
    }

    private JsonBool parseBool() throws JsonParseException {
        int start = pos;
        if (pos + 4 <= bytes.length && bytes[pos] == 't' && bytes[pos + 1] == 'r' && bytes[pos + 2] == 'u' && bytes[pos + 3] == 'e') {
            pos += 4;
            return JsonBool.TRUE;
        } else if (pos + 5 <= bytes.length && bytes[pos] == 'f' && bytes[pos + 1] == 'a' && bytes[pos + 2] == 'l' && bytes[pos + 3] == 's' && bytes[pos + 4] == 'e') {
            pos += 5;
            return JsonBool.FALSE;
        } else {
            throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, start);
        }
    }

    private void parseNull() throws JsonParseException {
        int start = pos;
        if (pos + 4 <= bytes.length && bytes[pos] == 'n' && bytes[pos + 1] == 'u' && bytes[pos + 2] == 'l' && bytes[pos + 3] == 'l') {
            pos += 4;
            throw new JsonParseException(ErrorCode.NULL_NOT_ALLOWED, start);
        }
        throw new JsonParseException(ErrorCode.UNEXPECTED_CHAR, start);
    }

    private void skipWhitespace() {
        while (pos < bytes.length) {
            byte b = bytes[pos];
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }
}
