package com.netfieldpro.core.wifi;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.trace.TraceEvent;
import com.netfieldpro.core.trace.TraceWriter;

public final class RecordingWifiScanSource implements WifiScanSource, Closeable {

    private final WifiScanSource delegate;
    private final TraceWriter writer;

    public RecordingWifiScanSource(WifiScanSource delegate, TraceWriter writer) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
    }

    public static JsonObject batchToJson(ScanBatch batch) {
        JsonObject d = new JsonObject();
        d.put("degraded", JsonBool.of(batch.degradedTiming()));
        JsonArray rowsArray = new JsonArray();
        for (WifiObservation obs : batch.rows()) {
            JsonObject r = new JsonObject();
            r.put("b", new JsonString(obs.bssid()));
            r.put("f", new JsonInt(obs.freqMHz()));
            r.put("r", new JsonInt(obs.rssiDbm()));
            r.put("s", new JsonString(obs.ssid()));
            r.put("t", new JsonInt(obs.timestampNs()));
            rowsArray.add(r);
        }
        d.put("rows", rowsArray);
        d.put("tier", new JsonString(batch.tier().name()));
        d.put("updated", JsonBool.of(batch.updated()));
        return d;
    }

    public static TraceEvent batchToTraceEvent(ScanBatch batch) {
        return new TraceEvent(batch.arrivalMonoNs(), "wifi.batch", batchToJson(batch));
    }

    @Override
    public void start(Listener listener) {
        delegate.start(new Listener() {
            @Override
            public void onBatch(ScanBatch batch) {
                try {
                    writer.write(batchToTraceEvent(batch));
                    writer.flush();
                } catch (IOException ignored) {
                }
                if (listener != null) {
                    listener.onBatch(batch);
                }
            }

            @Override
            public void onScanThrottled() {
                if (listener != null) {
                    listener.onScanThrottled();
                }
            }
        });
    }

    @Override
    public void requestScan() {
        delegate.requestScan();
    }

    @Override
    public void stop() {
        delegate.stop();
        try {
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    public Tier getTier() {
        return delegate.getTier();
    }

    @Override
    public void close() throws IOException {
        stop();
        writer.close();
    }
}
