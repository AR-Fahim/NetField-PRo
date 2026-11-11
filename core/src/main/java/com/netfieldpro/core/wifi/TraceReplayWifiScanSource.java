package com.netfieldpro.core.wifi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.trace.TraceEvent;
import com.netfieldpro.core.trace.TraceFormatException;
import com.netfieldpro.core.trace.TraceReader;

public final class TraceReplayWifiScanSource implements WifiScanSource {

    private final List<ScanBatch> batches;
    private int nextIndex = 0;
    private Listener currentListener;
    private boolean running = false;
    private Tier currentTier = Tier.CALLBACK;

    public TraceReplayWifiScanSource(List<ScanBatch> batches) {
        this.batches = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(batches, "batches cannot be null")));
        if (!this.batches.isEmpty()) {
            this.currentTier = this.batches.get(0).tier();
        }
    }

    public static ScanBatch jsonToBatch(long tNs, JsonObject d) {
        JsonValue tierVal = d.get("tier");
        JsonValue updatedVal = d.get("updated");
        JsonValue degradedVal = d.get("degraded");
        JsonValue rowsVal = d.get("rows");

        Tier tier = Tier.CALLBACK;
        if (tierVal instanceof JsonString s) {
            tier = Tier.valueOf(s.value());
        }
        boolean updated = false;
        if (updatedVal instanceof JsonBool b) {
            updated = b.value();
        }
        boolean degraded = false;
        if (degradedVal instanceof JsonBool b) {
            degraded = b.value();
        }

        List<WifiObservation> obsList = new ArrayList<>();
        if (rowsVal instanceof JsonArray arr) {
            for (int i = 0; i < arr.size(); i++) {
                JsonValue el = arr.get(i);
                if (el instanceof JsonObject rowObj) {
                    String bssid = ((JsonString) rowObj.get("b")).value();
                    String ssid = ((JsonString) rowObj.get("s")).value();
                    int freq = (int) ((JsonInt) rowObj.get("f")).value();
                    int rssi = (int) ((JsonInt) rowObj.get("r")).value();
                    long t = ((JsonInt) rowObj.get("t")).value();
                    obsList.add(new WifiObservation(bssid, ssid, freq, rssi, t));
                }
            }
        }
        return new ScanBatch(obsList, tNs, tier, updated, degraded, 0);
    }

    public static TraceReplayWifiScanSource fromTraceReader(TraceReader reader) throws IOException, TraceFormatException {
        List<ScanBatch> batches = new ArrayList<>();
        TraceEvent event;
        while ((event = reader.next()) != null) {
            if ("wifi.batch".equals(event.kind())) {
                batches.add(jsonToBatch(event.tNs(), event.data()));
            }
        }
        return new TraceReplayWifiScanSource(batches);
    }

    public List<ScanBatch> getBatches() {
        return batches;
    }

    @Override
    public void start(Listener listener) {
        this.currentListener = listener;
        this.running = true;
    }

    @Override
    public void requestScan() {
        if (!running || currentListener == null) {
            return;
        }
        if (nextIndex < batches.size()) {
            ScanBatch batch = batches.get(nextIndex++);
            currentTier = batch.tier();
            currentListener.onBatch(batch);
        }
    }

    public void playAll() {
        while (nextIndex < batches.size() && running && currentListener != null) {
            requestScan();
        }
    }

    @Override
    public void stop() {
        this.running = false;
        this.currentListener = null;
    }

    @Override
    public Tier getTier() {
        return currentTier;
    }
}
