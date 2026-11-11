package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.data.db.entity.AssessmentRow;

public final class AssessmentMapper {

    private AssessmentMapper() {
    }

    public static AssessmentRow toRow(Assessment a, String sessionId) {
        AssessmentRow row = new AssessmentRow();
        row.assessmentId = a.assessmentId();
        row.sessionId = sessionId != null ? sessionId : "";
        row.evidenceSha256 = a.evidenceSha256();
        row.profileId = a.profileId();
        row.profileSnapshot = new String(CanonicalJsonWriter.toBytes(a.profileSnapshot()), StandardCharsets.UTF_8);
        row.intendedProfileId = a.intendedProfileId();
        row.isReassessment = a.isReassessment();
        row.thresholds = new String(CanonicalJsonWriter.toBytes(a.thresholds()), StandardCharsets.UTF_8);
        row.computedMetrics = new String(CanonicalJsonWriter.toBytes(a.computedMetrics()), StandardCharsets.UTF_8);
        row.criteria = new String(CanonicalJsonWriter.toBytes(a.criteria()), StandardCharsets.UTF_8);
        row.verdict = a.verdict().name();
        row.assessedAtMs = a.assessedAtMs();
        row.algoVersion = a.algoVersion();
        row.gridG = a.gridG();
        row.publicKey = a.publicKey();
        row.sig = a.sig();
        return row;
    }

    public static AssessmentRow toRow(Assessment a) {
        return toRow(a, "");
    }

    public static Assessment toDomain(AssessmentRow row) {
        try {
            JsonObject profileSnapshot = (JsonObject) JsonParser.parse(row.profileSnapshot.getBytes(StandardCharsets.UTF_8));
            JsonObject thresholds = (JsonObject) JsonParser.parse(row.thresholds.getBytes(StandardCharsets.UTF_8));
            JsonObject computedMetrics = (JsonObject) JsonParser.parse(row.computedMetrics.getBytes(StandardCharsets.UTF_8));
            JsonArray criteria = (JsonArray) JsonParser.parse(row.criteria.getBytes(StandardCharsets.UTF_8));

            return new Assessment(
                    row.assessmentId, row.evidenceSha256, row.profileId, profileSnapshot,
                    row.intendedProfileId, row.isReassessment, thresholds, computedMetrics,
                    criteria, Verdict.valueOf(row.verdict), row.assessedAtMs, row.algoVersion,
                    row.gridG, row.publicKey, row.sig
            );
        } catch (Exception e) {
            throw new RuntimeException("Error mapping AssessmentRow to domain", e);
        }
    }
}

