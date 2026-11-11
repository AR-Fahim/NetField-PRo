package com.netfieldpro.core.verdict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.netfieldpro.core.model.enums.Verdict;

public record Result(Verdict verdict, List<CritResult> rows) {
    public Result {
        Objects.requireNonNull(verdict, "verdict cannot be null");
        rows = (rows == null) ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public long[] cArray() {
        return VerdictEngine.cArray(this);
    }
}
