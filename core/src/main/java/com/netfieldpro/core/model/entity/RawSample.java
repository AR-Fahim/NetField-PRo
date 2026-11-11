package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;

public record RawSample(
        long rowId,
        String scanId,
        String bssid,
        String ssid,
        int freqMHz,
        int rssi,
        Long scanResultTsNs
) {
    public RawSample {
        ModelValidator.checkNonNegative("RawSample", "rowId", rowId);
        ModelValidator.checkId("RawSample", "scanId", scanId);
        ModelValidator.checkRequired("RawSample", "bssid", bssid);
        ModelValidator.checkString("RawSample", "bssid", bssid, 1024);
        ModelValidator.checkRequired("RawSample", "ssid", ssid);
        ModelValidator.checkString("RawSample", "ssid", ssid, 1024);
        ModelValidator.checkPositive("RawSample", "freqMHz", freqMHz);
        if (scanResultTsNs != null) {
            ModelValidator.checkNonNegative("RawSample", "scanResultTsNs", scanResultTsNs);
        }
    }
}

