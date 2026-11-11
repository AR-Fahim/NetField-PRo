package com.netfieldpro.core.model.entity;

import java.util.List;

import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;

public record LatencyResult(
        String id,
        String pointId,
        LatencyMethod method,
        String target,
        String boundIface,
        LatencyScope scope,
        int sent,
        int recv,
        Double minMs,
        Double avgMs,
        Double maxMs,
        Double jitterMs,
        int lossCount,
        boolean duringScan,
        String connectedBssid,
        String ssid,
        List<Double> rtts
) {
    public LatencyResult {
        ModelValidator.checkId("LatencyResult", "id", id);
        ModelValidator.checkId("LatencyResult", "pointId", pointId);
        ModelValidator.checkRequired("LatencyResult", "method", method);
        ModelValidator.checkRequired("LatencyResult", "target", target);
        ModelValidator.checkString("LatencyResult", "target", target, 1024);
        ModelValidator.checkRequired("LatencyResult", "boundIface", boundIface);
        ModelValidator.checkString("LatencyResult", "boundIface", boundIface, 1024);
        ModelValidator.checkRequired("LatencyResult", "scope", scope);
        ModelValidator.checkNonNegative("LatencyResult", "sent", sent);
        ModelValidator.checkNonNegative("LatencyResult", "recv", recv);
        ModelValidator.checkNonNegative("LatencyResult", "lossCount", lossCount);
        ModelValidator.checkOptionalDouble("LatencyResult", "minMs", minMs);
        ModelValidator.checkOptionalDouble("LatencyResult", "avgMs", avgMs);
        ModelValidator.checkOptionalDouble("LatencyResult", "maxMs", maxMs);
        ModelValidator.checkOptionalDouble("LatencyResult", "jitterMs", jitterMs);
        ModelValidator.checkRequired("LatencyResult", "connectedBssid", connectedBssid);
        ModelValidator.checkString("LatencyResult", "connectedBssid", connectedBssid, 1024);
        ModelValidator.checkRequired("LatencyResult", "ssid", ssid);
        ModelValidator.checkString("LatencyResult", "ssid", ssid, 1024);
        rtts = ModelValidator.checkList("LatencyResult", "rtts", rtts);

        if (sent != recv + lossCount) {
            throw new IllegalArgumentException("LatencyResult.sent: must equal recv + lossCount: " + sent + " != " + recv + " + " + lossCount);
        }
        if (rtts.size() != recv) {
            throw new IllegalArgumentException("LatencyResult.rtts: size must equal recv: " + rtts.size() + " != " + recv);
        }
        if (recv == 0) {
            if (minMs != null || avgMs != null || maxMs != null) {
                throw new IllegalArgumentException("LatencyResult.minMs: must be null when recv == 0");
            }
        } else {
            if (minMs == null || avgMs == null || maxMs == null) {
                throw new IllegalArgumentException("LatencyResult.minMs: must not be null when recv > 0");
            }
        }
        if (recv < 2) {
            if (jitterMs != null) {
                throw new IllegalArgumentException("LatencyResult.jitterMs: must be null when recv < 2");
            }
        } else {
            if (jitterMs == null) {
                throw new IllegalArgumentException("LatencyResult.jitterMs: must not be null when recv >= 2");
            }
        }
    }
}

