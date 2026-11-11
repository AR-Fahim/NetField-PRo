package com.netfieldpro.wifi;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netfieldpro.core.wifi.Normalize;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.TimestampPlausibility;
import com.netfieldpro.core.wifi.WifiObservation;

public final class ScanResultMapper {
    private ScanResultMapper() {}

    public static WifiObservation toObservation(ScanResult sr) {
        if (sr == null) {
            return null;
        }
        String bssid = Normalize.bssid(sr.BSSID);
        if (bssid == null) {
            return null;
        }
        String ssid = Normalize.scanSsid(sr.SSID);
        int freq = sr.frequency;
        int rssi = sr.level;
        Long tsNs = Normalize.scanResultTsToNs(sr.timestamp);
        long finalTs = (tsNs != null) ? tsNs : 0L;
        return new WifiObservation(bssid, ssid, freq, rssi, finalTs);
    }

    public static ScanBatch map(List<ScanResult> results, long arrivalMonoNs, Tier tier, boolean updated) {
        if (results == null || results.isEmpty()) {
            return new ScanBatch(Collections.emptyList(), arrivalMonoNs, tier, updated, false, 0);
        }

        List<WifiObservation> rawRows = new ArrayList<>(results.size());
        int droppedInvalid = 0;
        for (ScanResult sr : results) {
            WifiObservation obs = toObservation(sr);
            if (obs == null) {
                droppedInvalid++;
            } else {
                rawRows.add(obs);
            }
        }

        boolean plausible = TimestampPlausibility.isBatchPlausible(rawRows, arrivalMonoNs);
        boolean degradedTiming = !plausible;

        List<WifiObservation> finalRows = new ArrayList<>(rawRows.size());
        for (WifiObservation obs : rawRows) {
            long effTs = TimestampPlausibility.effective(obs.timestampNs(), arrivalMonoNs, plausible);
            finalRows.add(new WifiObservation(obs.bssid(), obs.ssid(), obs.freqMHz(), obs.rssiDbm(), effTs));
        }

        return new ScanBatch(finalRows, arrivalMonoNs, tier, updated, degradedTiming, droppedInvalid);
    }
}
