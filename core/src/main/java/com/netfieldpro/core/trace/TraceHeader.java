package com.netfieldpro.core.trace;

import java.util.Objects;

public record TraceHeader(int version, String deviceModel, int sdk, long monoEpochNs, long createdAtMs) {
    public TraceHeader {
        Objects.requireNonNull(deviceModel, "deviceModel cannot be null");
    }
}
