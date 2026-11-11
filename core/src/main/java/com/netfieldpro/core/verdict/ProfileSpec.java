package com.netfieldpro.core.verdict;

import java.util.Objects;

public record ProfileSpec(
        String key,
        int covDbm,
        int covPct,
        boolean covBasisTapPins,
        Integer secDbm,
        Integer secPct,
        int p95Ms,
        int jitMs,
        int lossPermille,
        int worstLossPct,
        boolean worstLossInclusive,
        int sticky,
        int maxInfluenceM
) {
    public ProfileSpec {
        Objects.requireNonNull(key, "key cannot be null");
    }
}
