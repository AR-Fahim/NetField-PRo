package com.netfieldpro.core.trace;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;

public final class TraceWriter implements Closeable, Flushable {

    private final OutputStream out;

    public TraceWriter(OutputStream out, TraceHeader header) throws IOException {
        this.out = Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(header, "header cannot be null");

        JsonObject obj = new JsonObject();
        obj.put("createdAtMs", new JsonInt(header.createdAtMs()));
        obj.put("deviceModel", new JsonString(header.deviceModel()));
        obj.put("monoEpochNs", new JsonInt(header.monoEpochNs()));
        obj.put("nftrace", new JsonInt(header.version()));
        obj.put("sdk", new JsonInt(header.sdk()));

        CanonicalJsonWriter.write(obj, out);
        out.write('\n');
    }

    public void write(TraceEvent event) throws IOException {
        Objects.requireNonNull(event, "event cannot be null");

        JsonObject obj = new JsonObject();
        obj.put("d", event.data());
        obj.put("k", new JsonString(event.kind()));
        obj.put("t", new JsonInt(event.tNs()));

        CanonicalJsonWriter.write(obj, out);
        out.write('\n');
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } finally {
            out.close();
        }
    }
}
