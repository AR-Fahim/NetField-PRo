package com.netfieldpro.core.json;

public class JsonParseException extends Exception {

    public enum ErrorCode {
        UNEXPECTED_EOF,
        UNEXPECTED_CHAR,
        TRAILING_DATA,
        DUPLICATE_KEY,
        NULL_NOT_ALLOWED,
        FLOAT_NOT_ALLOWED,
        BAD_NUMBER,
        NUMBER_OUT_OF_RANGE,
        BAD_ESCAPE,
        LONE_SURROGATE,
        BAD_CONTROL_CHAR,
        BAD_UTF8,
        DEPTH_EXCEEDED
    }

    private final ErrorCode code;
    private final int offset;

    public JsonParseException(ErrorCode code, int offset) {
        super(code.name() + " at offset " + offset);
        this.code = code;
        this.offset = offset;
    }

    public ErrorCode getCode() {
        return code;
    }

    public int getOffset() {
        return offset;
    }
}
