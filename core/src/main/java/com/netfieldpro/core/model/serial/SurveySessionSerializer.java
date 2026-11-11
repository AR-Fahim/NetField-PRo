package com.netfieldpro.core.model.serial;

import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.core.model.enums.CadenceFlag;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.SessionType;
import com.netfieldpro.core.model.enums.TimingQuality;
import com.netfieldpro.core.util.FixedDecimal;

public final class SurveySessionSerializer {

    private SurveySessionSerializer() {
    }

    public static JsonObject toJson(SurveySession s) {
        JsonObject obj = new JsonObject();
        obj.put("algoVersion", new JsonString(s.algoVersion()));
        obj.put("appVersion", new JsonString(s.appVersion()));
        obj.put("cadenceFlag", new JsonString(s.cadenceFlag().name()));
        obj.put("capabilityReport", s.capabilityReport());
        obj.put("createdAtMs", new JsonInt(s.createdAtMs()));
        obj.put("deviceInfoId", new JsonString(s.deviceInfoId()));
        obj.put("evidenceGrade", new JsonString(s.evidenceGrade().name()));
        if (s.evidenceSha256() != null) {
            obj.put("evidenceSha256", new JsonString(s.evidenceSha256()));
        }
        if (s.finalizedAtMs() != null) {
            obj.put("finalizedAtMs", new JsonInt(s.finalizedAtMs()));
        }
        obj.put("floorId", new JsonString(s.floorId()));
        obj.put("id", new JsonString(s.id()));
        obj.put("intendedProfileChanges", new JsonInt(s.intendedProfileChanges()));
        obj.put("intendedProfileId", new JsonString(s.intendedProfileId()));
        obj.put("linkCadenceMs", new JsonInt(s.linkCadenceMs()));
        obj.put("monoEpochNs", new JsonInt(s.monoEpochNs()));
        obj.put("name", new JsonString(s.name()));
        obj.put("probeMethod", new JsonString(s.probeMethod().name()));
        if (s.publicKeyFp() != null) {
            obj.put("publicKeyFp", new JsonString(s.publicKeyFp()));
        }
        obj.put("receiverId", new JsonString(s.receiverId()));

        JsonArray refArray = new JsonArray();
        for (Long freq : s.referenceFreqsMHz()) {
            refArray.add(new JsonInt(freq));
        }
        obj.put("referenceFreqsMHz", refArray);

        obj.put("rssiOffsetDb", new JsonString(FixedDecimal.format(s.rssiOffsetDb(), 1)));
        obj.put("schemaVersion", new JsonInt(s.schemaVersion()));
        obj.put("sessionType", new JsonString(s.sessionType().name()));
        obj.put("state", new JsonString(s.state().name()));
        obj.put("throttleAtStart", JsonBool.of(s.throttleAtStart()));
        obj.put("timingQuality", new JsonString(s.timingQuality().name()));
        return obj;
    }

    public static SurveySession fromJson(JsonObject obj) {
        String id = ((JsonString) obj.get("id")).value();
        String floorId = ((JsonString) obj.get("floorId")).value();
        String name = ((JsonString) obj.get("name")).value();
        SessionState state = SessionState.valueOf(((JsonString) obj.get("state")).value());
        long createdAtMs = IntFields.parseLong(obj.get("createdAtMs"), "createdAtMs");
        long monoEpochNs = IntFields.parseLong(obj.get("monoEpochNs"), "monoEpochNs");
        int schemaVersion = IntFields.parseInt(obj.get("schemaVersion"), "schemaVersion");
        String appVersion = ((JsonString) obj.get("appVersion")).value();
        String algoVersion = ((JsonString) obj.get("algoVersion")).value();
        boolean throttleAtStart = ((JsonBool) obj.get("throttleAtStart")).value();
        TimingQuality timingQuality = TimingQuality.valueOf(((JsonString) obj.get("timingQuality")).value());
        EvidenceGrade evidenceGrade = EvidenceGrade.valueOf(((JsonString) obj.get("evidenceGrade")).value());
        SessionType sessionType = SessionType.valueOf(((JsonString) obj.get("sessionType")).value());
        String intendedProfileId = ((JsonString) obj.get("intendedProfileId")).value();
        long intendedProfileChanges = IntFields.parseLong(obj.get("intendedProfileChanges"), "intendedProfileChanges");
        double rssiOffsetDb = DecimalFields.parseDouble(obj.get("rssiOffsetDb"), "rssiOffsetDb", 1);
        long linkCadenceMs = IntFields.parseLong(obj.get("linkCadenceMs"), "linkCadenceMs");
        CadenceFlag cadenceFlag = CadenceFlag.valueOf(((JsonString) obj.get("cadenceFlag")).value());
        ProbeMethod probeMethod = ProbeMethod.valueOf(((JsonString) obj.get("probeMethod")).value());

        JsonArray refArray = (JsonArray) obj.get("referenceFreqsMHz");
        List<Long> referenceFreqsMHz = new ArrayList<>();
        for (JsonValue v : refArray.elements()) {
            referenceFreqsMHz.add(IntFields.parseLong(v, "referenceFreqsMHz"));
        }

        String receiverId = ((JsonString) obj.get("receiverId")).value();
        String deviceInfoId = ((JsonString) obj.get("deviceInfoId")).value();
        JsonObject capabilityReport = (JsonObject) obj.get("capabilityReport");
        Long finalizedAtMs = IntFields.parseOptionalLong(obj.get("finalizedAtMs"), "finalizedAtMs");
        String evidenceSha256 = obj.get("evidenceSha256") instanceof JsonString s ? s.value() : null;
        String publicKeyFp = obj.get("publicKeyFp") instanceof JsonString s ? s.value() : null;

        return new SurveySession(
                id, floorId, name, state, createdAtMs, monoEpochNs, schemaVersion,
                appVersion, algoVersion, throttleAtStart, timingQuality, evidenceGrade,
                sessionType, intendedProfileId, intendedProfileChanges, rssiOffsetDb,
                linkCadenceMs, cadenceFlag, probeMethod, referenceFreqsMHz, receiverId,
                deviceInfoId, capabilityReport, finalizedAtMs, evidenceSha256, publicKeyFp
        );
    }
}
