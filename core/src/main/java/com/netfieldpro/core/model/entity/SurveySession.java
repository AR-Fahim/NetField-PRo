package com.netfieldpro.core.model.entity;

import java.util.List;

import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.model.ModelValidator;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.TimingQuality;

public record SurveySession(
        String id,
        String floorId,
        String name,
        SessionState state,
        long createdAtMs,
        long monoEpochNs,
        int schemaVersion,
        String appVersion,
        String algoVersion,
        boolean throttleAtStart,
        TimingQuality timingQuality,
        EvidenceGrade evidenceGrade,
        SessionType sessionType,
        String intendedProfileId,
        long intendedProfileChanges,
        double rssiOffsetDb,
        long linkCadenceMs,
        CadenceFlag cadenceFlag,
        ProbeMethod probeMethod,
        List<Long> referenceFreqsMHz,
        String receiverId,
        String deviceInfoId,
        JsonObject capabilityReport,
        Long finalizedAtMs,
        String evidenceSha256,
        String publicKeyFp
) {
    public SurveySession {
        ModelValidator.checkId("SurveySession", "id", id);
        ModelValidator.checkId("SurveySession", "floorId", floorId);
        ModelValidator.checkRequired("SurveySession", "name", name);
        ModelValidator.checkString("SurveySession", "name", name, 1024);
        ModelValidator.checkRequired("SurveySession", "state", state);
        ModelValidator.checkNonNegative("SurveySession", "createdAtMs", createdAtMs);
        ModelValidator.checkNonNegative("SurveySession", "monoEpochNs", monoEpochNs);
        ModelValidator.checkNonNegative("SurveySession", "schemaVersion", schemaVersion);
        ModelValidator.checkRequired("SurveySession", "appVersion", appVersion);
        ModelValidator.checkString("SurveySession", "appVersion", appVersion, 1024);
        ModelValidator.checkRequired("SurveySession", "algoVersion", algoVersion);
        ModelValidator.checkString("SurveySession", "algoVersion", algoVersion, 1024);
        ModelValidator.checkRequired("SurveySession", "timingQuality", timingQuality);
        ModelValidator.checkRequired("SurveySession", "evidenceGrade", evidenceGrade);
        ModelValidator.checkRequired("SurveySession", "sessionType", sessionType);
        ModelValidator.checkId("SurveySession", "intendedProfileId", intendedProfileId);
        ModelValidator.checkNonNegative("SurveySession", "intendedProfileChanges", intendedProfileChanges);
        ModelValidator.checkDouble("SurveySession", "rssiOffsetDb", rssiOffsetDb);
        ModelValidator.checkPositive("SurveySession", "linkCadenceMs", linkCadenceMs);
        ModelValidator.checkRequired("SurveySession", "cadenceFlag", cadenceFlag);
        ModelValidator.checkRequired("SurveySession", "probeMethod", probeMethod);
        referenceFreqsMHz = ModelValidator.checkList("SurveySession", "referenceFreqsMHz", referenceFreqsMHz);
        ModelValidator.checkRequired("SurveySession", "receiverId", receiverId);
        ModelValidator.checkString("SurveySession", "receiverId", receiverId, 1024);
        ModelValidator.checkId("SurveySession", "deviceInfoId", deviceInfoId);
        ModelValidator.checkRequired("SurveySession", "capabilityReport", capabilityReport);

        if (state == SessionState.FINALIZED) {
            if (finalizedAtMs == null || evidenceSha256 == null || publicKeyFp == null) {
                throw new IllegalArgumentException("SurveySession.finalizedAtMs: FINALIZED requires finalizedAtMs, evidenceSha256, and publicKeyFp");
            }
        } else if (state == SessionState.DRAFT) {
            if (finalizedAtMs != null || evidenceSha256 != null || publicKeyFp != null) {
                throw new IllegalArgumentException("SurveySession.finalizedAtMs: DRAFT requires finalizedAtMs, evidenceSha256, and publicKeyFp to be null");
            }
        }

        if (finalizedAtMs != null) {
            ModelValidator.checkNonNegative("SurveySession", "finalizedAtMs", finalizedAtMs);
        }
        ModelValidator.checkString("SurveySession", "evidenceSha256", evidenceSha256, 1024);
        ModelValidator.checkString("SurveySession", "publicKeyFp", publicKeyFp, 1024);
    }
}

