package com.netfieldpro.core.wifi;

public interface WifiScanSource {

    interface Listener {
        void onBatch(ScanBatch batch);
        default void onScanThrottled() {}
    }

    void start(Listener listener);
    void requestScan();
    void stop();
    Tier getTier();
}
