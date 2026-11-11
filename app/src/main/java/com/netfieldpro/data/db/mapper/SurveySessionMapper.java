package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.data.db.entity.SurveySessionRow;

public final class SurveySessionMapper {

    private SurveySessionMapper() {
    }

    public static SurveySessionRow toRow(SurveySession s) {
        SurveySessionRow row = new SurveySessionRow();
        row.id = s.id();
        row.floorId = s.floorId();
        row.name = s.name();
        row.state = s.state().name();
        row.createdAtMs = s.createdAtMs();
        row.monoEpochNs = s.monoEpochNs();
        row.schemaVersion = s.schemaVersion();
        row.appVersion = s.appVersion();
        row.algoVersion = s.algoVersion();
        row.throttleAtStart = s.throttleAtStart();
        row.timingQuality = s.timingQuality().name();
        row.evidenceGrade = s.evidenceGrade().name();
        row.sessionType = s.sessionType().name();
        row.intendedProfileId = s.intendedProfileId();
        row.intendedProfileChanges = s.intendedProfileChanges();
        row.rssiOffsetDb = s.rssiOffsetDb();
        row.linkCadenceMs = s.linkCadenceMs();
        row.cadenceFlag = s.cadenceFlag().name();
        row.probeMethod = s.probeMethod().name();

        JsonArray refArr = new JsonArray();
        for (Long f : s.referenceFreqsMHz()) {
            refArr.add(new JsonInt(f));
        }
        row.referenceFreqsMhz = new String(CanonicalJsonWriter.toBytes(refArr), StandardCharsets.UTF_8);

        row.receiverId = s.receiverId();
        row.deviceInfoId = s.deviceInfoId();
        row.capabilityReport = new String(CanonicalJsonWriter.toBytes(s.capabilityReport()), StandardCharsets.UTF_8);
        row.finalizedAtMs = s.finalizedAtMs();
        row.evidenceSha256 = s.evidenceSha256();
        row.publicKeyFp = s.publicKeyFp();
        return row;
    }

    public static SurveySession toDomain(SurveySessionRow row) {
        try {
            JsonArray refArr = (JsonArray) JsonParser.parse(row.referenceFreqsMhz.getBytes(StandardCharsets.UTF_8));
            List<Long> refFreqs = new ArrayList<>();
            for (JsonValue v : refArr.elements()) {
                if (v instanceof JsonInt ji) {
                    refFreqs.add(ji.value());
                } else if (v instanceof JsonString js) {
                    refFreqs.add(Long.parseLong(js.value()));
                }
            }

            JsonObject cap = (JsonObject) JsonParser.parse(row.capabilityReport.getBytes(StandardCharsets.UTF_8));

            return new SurveySession(
                    row.id, row.floorId, row.name, SessionState.valueOf(row.state),
                    row.createdAtMs, row.monoEpochNs, row.schemaVersion, row.appVersion,
                    row.algoVersion, row.throttleAtStart, TimingQuality.valueOf(row.timingQuality),
                    EvidenceGrade.valueOf(row.evidenceGrade), SessionType.valueOf(row.sessionType),
                    row.intendedProfileId, row.intendedProfileChanges, row.rssiOffsetDb,
                    row.linkCadenceMs, CadenceFlag.valueOf(row.cadenceFlag),
                    ProbeMethod.valueOf(row.probeMethod), refFreqs, row.receiverId,
                    row.deviceInfoId, cap, row.finalizedAtMs, row.evidenceSha256, row.publicKeyFp
            );
        } catch (Exception e) {
            throw new RuntimeException("Error mapping SurveySessionRow to domain", e);
        }
    }
}
