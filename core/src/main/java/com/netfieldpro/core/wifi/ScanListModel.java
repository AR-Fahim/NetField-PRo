package com.netfieldpro.core.wifi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScanListModel {
    private ScanListModel() {}

    public record Row(
            String bssid,
            String ssid,
            String ssidDisplay,
            int freqMHz,
            int rssiDbm,
            Band band,
            int channel,
            boolean isConnected
    ) {}

    public static List<Row> build(ScanBatch batch, String connectedBssid) {
        if (batch == null || batch.rows() == null || batch.rows().isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedConnected = Normalize.bssid(connectedBssid);

        List<Row> rows = new ArrayList<>(batch.rows().size());
        for (WifiObservation obs : batch.rows()) {
            String bssid = obs.bssid();
            String ssid = obs.ssid();
            String ssidDisplay = (ssid == null || ssid.isEmpty()) ? null : ssid;
            Band band = ChannelMath.toBand(obs.freqMHz());
            int channel = ChannelMath.toChannel(obs.freqMHz());
            boolean isConnected = (normalizedConnected != null && normalizedConnected.equalsIgnoreCase(bssid));
            rows.add(new Row(bssid, ssid, ssidDisplay, obs.freqMHz(), obs.rssiDbm(), band, channel, isConnected));
        }

        rows.sort((a, b) -> {
            int cmp = Integer.compare(b.rssiDbm(), a.rssiDbm());
            if (cmp != 0) {
                return cmp;
            }
            return a.bssid().compareTo(b.bssid());
        });

        return Collections.unmodifiableList(rows);
    }
}
