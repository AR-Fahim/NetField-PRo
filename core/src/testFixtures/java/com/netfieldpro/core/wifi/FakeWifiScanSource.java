package com.netfieldpro.core.wifi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeWifiScanSource implements WifiScanSource {

    private final List<ScanBatch> scriptedBatches;
    private int nextIndex = 0;
    private Listener listener;
    private boolean running = false;
    private Tier tier = Tier.CALLBACK;

    public FakeWifiScanSource() {
        this.scriptedBatches = new ArrayList<>();
    }

    public FakeWifiScanSource(List<ScanBatch> batches) {
        this.scriptedBatches = new ArrayList<>(batches);
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public void addBatch(ScanBatch batch) {
        scriptedBatches.add(batch);
    }

    public void emitBatch(ScanBatch batch) {
        if (running && listener != null) {
            tier = batch.tier();
            listener.onBatch(batch);
        }
    }

    public void emitThrottled() {
        if (running && listener != null) {
            listener.onScanThrottled();
        }
    }

    public boolean emitNext() {
        if (running && listener != null && nextIndex < scriptedBatches.size()) {
            ScanBatch batch = scriptedBatches.get(nextIndex++);
            tier = batch.tier();
            listener.onBatch(batch);
            return true;
        }
        return false;
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        this.running = true;
    }

    @Override
    public void requestScan() {
        emitNext();
    }

    @Override
    public void stop() {
        this.running = false;
        this.listener = null;
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    public boolean isRunning() {
        return running;
    }
}
