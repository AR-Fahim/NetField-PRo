package com.netfieldpro.core.verdict;

import com.netfieldpro.core.model.enums.EvidenceGrade;

public record Inputs(
        EvidenceGrade grade,
        long cellsInside,
        long cellsMeeting,
        long cellsMasked,
        Long secCellsMeeting,
        long tapPins,
        long tapPinsMeeting,
        Long p95RttUs,
        Long meanJitterMilli,
        Long lostSum,
        Long sentSum,
        Long worstLost,
        Long worstSent,
        Integer stickyCount
) {
}
