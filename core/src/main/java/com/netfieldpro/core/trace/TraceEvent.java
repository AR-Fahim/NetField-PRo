package com.netfieldpro.core.trace;

import java.util.Objects;

import com.netfieldpro.core.json.JsonObject;

public record TraceEvent(long tNs, String kind, JsonObject data) {
    public TraceEvent {
        Objects.requireNonNull(kind, "kind cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
    }
}
