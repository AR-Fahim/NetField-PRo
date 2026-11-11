package com.netfieldpro.core.trace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParseException;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;

public final class TraceReader {

    private static final int MAX_LINE_BYTES = 1048576;

    private final InputStream in;
    private final TraceHeader header;
    private int currentLineNo = 0;
    private long prevTNanos = -1;

    public TraceReader(InputStream in) throws IOException, TraceFormatException {
        this.in = Objects.requireNonNull(in, "in cannot be null");
        this.header = readHeader();
    }

    public TraceHeader header() {
        return header;
    }

    public TraceEvent next() throws IOException, TraceFormatException {
        byte[] lineBytes = readNextLine();
        if (lineBytes == null) {
            return null;
        }

        JsonValue json;
        try {
            json = JsonParser.parse(lineBytes);
        } catch (JsonParseException e) {
            throw new TraceFormatException(e.getCode().name(), currentLineNo, e);
        }

        if (!(json instanceof JsonObject obj)) {
            throw new TraceFormatException(TraceFormatException.BAD_EVENT, currentLineNo);
        }

        JsonValue kVal = obj.get("k");
        JsonValue tVal = obj.get("t");
        JsonValue dVal = obj.get("d");

        if (!(kVal instanceof JsonString kStr) || !(tVal instanceof JsonInt tInt) || !(dVal instanceof JsonObject dObj)) {
            throw new TraceFormatException(TraceFormatException.BAD_EVENT, currentLineNo);
        }

        long tNs = tInt.value();
        if (prevTNanos != -1 && tNs < prevTNanos) {
            throw new TraceFormatException(TraceFormatException.TIME_REGRESSION, currentLineNo);
        }
        prevTNanos = tNs;

        return new TraceEvent(tNs, kStr.value(), dObj);
    }

    private TraceHeader readHeader() throws IOException, TraceFormatException {
        byte[] lineBytes = readNextLine();
        if (lineBytes == null) {
            throw new TraceFormatException(TraceFormatException.MISSING_HEADER, 1);
        }

        JsonValue json;
        try {
            json = JsonParser.parse(lineBytes);
        } catch (JsonParseException e) {
            throw new TraceFormatException(e.getCode().name(), 1, e);
        }

        if (!(json instanceof JsonObject obj)) {
            throw new TraceFormatException(TraceFormatException.BAD_HEADER, 1);
        }

        // Check if this looks like an event instead of a header
        if (obj.containsKey("k") && obj.containsKey("t") && !obj.containsKey("nftrace")) {
            throw new TraceFormatException(TraceFormatException.MISSING_HEADER, 1);
        }

        JsonValue nftraceVal = obj.get("nftrace");
        if (!(nftraceVal instanceof JsonInt nftraceInt)) {
            throw new TraceFormatException(TraceFormatException.MISSING_HEADER, 1);
        }

        int version = (int) nftraceInt.value();
        if (version != 1) {
            throw new TraceFormatException(TraceFormatException.UNSUPPORTED_VERSION, 1);
        }

        JsonValue modelVal = obj.get("deviceModel");
        JsonValue sdkVal = obj.get("sdk");
        JsonValue epochVal = obj.get("monoEpochNs");
        JsonValue createdVal = obj.get("createdAtMs");

        if (!(modelVal instanceof JsonString modelStr) ||
                !(sdkVal instanceof JsonInt sdkInt) ||
                !(epochVal instanceof JsonInt epochInt) ||
                !(createdVal instanceof JsonInt createdInt)) {
            throw new TraceFormatException(TraceFormatException.BAD_HEADER, 1);
        }

        return new TraceHeader(
                version,
                modelStr.value(),
                (int) sdkInt.value(),
                epochInt.value(),
                createdInt.value()
        );
    }

    private byte[] readNextLine() throws IOException, TraceFormatException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        boolean hasRead = false;
        while ((b = in.read()) != -1) {
            hasRead = true;
            if (b == '\n') {
                break;
            }
            if (b == '\r') {
                // If CRLF, consume \n if present
                in.mark(1);
                int next = in.read();
                if (next != '\n' && next != -1) {
                    in.reset();
                }
                break;
            }
            baos.write(b);
            if (baos.size() > MAX_LINE_BYTES) {
                currentLineNo++;
                throw new TraceFormatException(TraceFormatException.LINE_TOO_LONG, currentLineNo);
            }
        }

        if (!hasRead && baos.size() == 0) {
            return null;
        }

        currentLineNo++;
        return baos.toByteArray();
    }
}
