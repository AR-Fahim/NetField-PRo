package com.netfieldpro.core.wifi;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ScanBatch(
        List<WifiObservation> rows,
        long arrivalMonoNs,
        Tier tier,
        boolean updated,
        boolean degradedTiming,
        int droppedInvalid
) {
    public ScanBatch {
        Objects.requireNonNull(rows, "rows cannot be null");
        Objects.requireNonNull(tier, "tier cannot be null");
        rows = Collections.unmodifiableList(List.copyOf(rows));
    }
}
