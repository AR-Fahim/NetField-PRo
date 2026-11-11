package com.netfieldpro.report;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.verdict.CritId;

import java.util.List;

public record ReportModel(
        boolean isSigned,
        int totalPages,
        String reportTitle,
        String projectName,
        String siteName,
        String floorName,
        String sessionId,
        String shortSessionId,
        String formattedDate,
        @Nullable Verdict verdict,
        @Nullable String verdictLabel,
        String gradeLabel,
        int pointsCount,
        int lowConfidenceCount,
        int movedCount,
        List<CriterionRow> criteriaRows,
        @Nullable Bitmap compositePlanBitmap,
        String paletteName,
        String interpNote,
        List<LegendEntry> legendEntries,
        @Nullable String qrFullJson,
        @Nullable String evidenceHash,
        @Nullable String assessmentHash,
        @Nullable String signature,
        @Nullable String publicKeyFp,
        String appVersion,
        String algoVersion,
        @Nullable String verificationInstructions,
        @Nullable String debugNote,
        @Nullable String watermarkText
) {
    public record CriterionRow(
            CritId id,
            String name,
            String required,
            String measured,
            String statusText,
            boolean isMet
    ) {}

    public record LegendEntry(
            String label,
            int color
    ) {}
}
