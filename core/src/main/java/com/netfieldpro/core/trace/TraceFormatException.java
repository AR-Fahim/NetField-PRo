package com.netfieldpro.core.trace;

public class TraceFormatException extends Exception {

    public static final String MISSING_HEADER = "MISSING_HEADER";
    public static final String UNSUPPORTED_VERSION = "UNSUPPORTED_VERSION";
    public static final String BAD_HEADER = "BAD_HEADER";
    public static final String BAD_EVENT = "BAD_EVENT";
    public static final String TIME_REGRESSION = "TIME_REGRESSION";
    public static final String LINE_TOO_LONG = "LINE_TOO_LONG";

    private final String code;
    private final int lineNo;

    public TraceFormatException(String code, int lineNo) {
        super(code + " at line " + lineNo);
        this.code = code;
        this.lineNo = lineNo;
    }

    public TraceFormatException(String code, int lineNo, Throwable cause) {
        super(code + " at line " + lineNo, cause);
        this.code = code;
        this.lineNo = lineNo;
    }

    public String getCode() {
        return code;
    }

    public int getLineNo() {
        return lineNo;
    }
}
