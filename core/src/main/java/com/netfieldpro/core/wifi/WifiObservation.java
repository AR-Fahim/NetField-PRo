package com.netfieldpro.core.wifi;

import java.util.Objects;

public record WifiObservation(
        String bssid,
        String ssid,
        int freqMHz,
        int rssiDbm,
        long timestampNs
) {
    public WifiObservation {
        Objects.requireNonNull(bssid, "bssid cannot be null");
        Objects.requireNonNull(ssid, "ssid cannot be null");
    }
}
