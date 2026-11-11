package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;

public record LinkSample(
        String id,
        String sessionId,
        long observedAtNs,
        String bssid,
        String ssid,
        int rssi,
        int freqMHz,
        Integer txMbps,
        Integer rxMbps
) {
    public LinkSample {
        ModelValidator.checkId("LinkSample", "id", id);
        ModelValidator.checkId("LinkSample", "sessionId", sessionId);
        ModelValidator.checkNonNegative("LinkSample", "observedAtNs", observedAtNs);
        ModelValidator.checkRequired("LinkSample", "bssid", bssid);
        ModelValidator.checkString("LinkSample", "bssid", bssid, 1024);
        ModelValidator.checkRequired("LinkSample", "ssid", ssid);
        ModelValidator.checkString("LinkSample", "ssid", ssid, 1024);
        ModelValidator.checkPositive("LinkSample", "freqMHz", freqMHz);
        if (txMbps != null) {
            ModelValidator.checkNonNegative("LinkSample", "txMbps", txMbps);
        }
        if (rxMbps != null) {
            ModelValidator.checkNonNegative("LinkSample", "rxMbps", rxMbps);
        }
    }
}

