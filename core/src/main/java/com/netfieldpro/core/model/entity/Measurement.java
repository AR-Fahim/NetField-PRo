package com.netfieldpro.core.model.entity;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.Protocol;

public record Measurement(
        String id,
        String pointId,
        Protocol protocol,
        String bssidOrId,
        String ssid,
        int freqMHz,
        int channel,
        Integer widthMHz,
        MeasurementSource source,
        String receiverId,
        int n,
        double rssiMedian,
        int rssiMin,
        int rssiMax,
        Integer lqi,
        String panId
) {
    public Measurement {
        ModelValidator.checkId("Measurement", "id", id);
        ModelValidator.checkId("Measurement", "pointId", pointId);
        ModelValidator.checkRequired("Measurement", "protocol", protocol);
        ModelValidator.checkRequired("Measurement", "bssidOrId", bssidOrId);
        ModelValidator.checkString("Measurement", "bssidOrId", bssidOrId, 1024);
        ModelValidator.checkString("Measurement", "ssid", ssid, 1024);
        ModelValidator.checkPositive("Measurement", "freqMHz", freqMHz);
        ModelValidator.checkRequired("Measurement", "source", source);
        ModelValidator.checkRequired("Measurement", "receiverId", receiverId);
        ModelValidator.checkString("Measurement", "receiverId", receiverId, 1024);
        ModelValidator.checkDouble("Measurement", "rssiMedian", rssiMedian);
        ModelValidator.checkString("Measurement", "panId", panId, 1024);

        if (n < 1) {
            throw new IllegalArgumentException("Measurement.n: must be >= 1: " + n);
        }
        if (rssiMin > rssiMedian || rssiMedian > rssiMax) {
            throw new IllegalArgumentException("Measurement.rssiMedian: must satisfy rssiMin <= rssiMedian <= rssiMax: " +
                    rssiMin + " <= " + rssiMedian + " <= " + rssiMax);
        }
    }
}

