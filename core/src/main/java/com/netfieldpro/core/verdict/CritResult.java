package com.netfieldpro.core.verdict;

import java.util.Objects;

public record CritResult(CritId id, CritStatus status, long c) {
    public CritResult {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
    }
}
