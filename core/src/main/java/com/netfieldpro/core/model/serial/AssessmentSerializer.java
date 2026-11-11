package com.netfieldpro.core.model.serial;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.util.FixedDecimal;

public final class AssessmentSerializer {

    private AssessmentSerializer() {
    }

    public static JsonObject toJson(Assessment a) {
        JsonObject obj = new JsonObject();
        obj.put("algoVersion", new JsonString(a.algoVersion()));
        obj.put("assessedAtMs", new JsonInt(a.assessedAtMs()));
        obj.put("assessmentId", new JsonString(a.assessmentId()));
        obj.put("computedMetrics", a.computedMetrics());
        obj.put("criteria", a.criteria());
        obj.put("evidenceSha256", new JsonString(a.evidenceSha256()));
        obj.put("gridG", new JsonString(FixedDecimal.format(a.gridG(), 6)));
        obj.put("intendedProfileId", new JsonString(a.intendedProfileId()));
        obj.put("isReassessment", JsonBool.of(a.isReassessment()));
        obj.put("profileId", new JsonString(a.profileId()));
        obj.put("profileSnapshot", a.profileSnapshot());
        obj.put("publicKey", new JsonString(a.publicKey()));
        obj.put("sig", new JsonString(a.sig()));
        obj.put("thresholds", a.thresholds());
        obj.put("verdict", new JsonString(a.verdict().name()));
        return obj;
    }

    public static Assessment fromJson(JsonObject obj) {
        String assessmentId = ((JsonString) obj.get("assessmentId")).value();
        String evidenceSha256 = ((JsonString) obj.get("evidenceSha256")).value();
        String profileId = ((JsonString) obj.get("profileId")).value();
        JsonObject profileSnapshot = (JsonObject) obj.get("profileSnapshot");
        String intendedProfileId = ((JsonString) obj.get("intendedProfileId")).value();
        boolean isReassessment = ((JsonBool) obj.get("isReassessment")).value();
        JsonObject thresholds = (JsonObject) obj.get("thresholds");
        JsonObject computedMetrics = (JsonObject) obj.get("computedMetrics");
        JsonArray criteria = (JsonArray) obj.get("criteria");
        Verdict verdict = Verdict.valueOf(((JsonString) obj.get("verdict")).value());
        long assessedAtMs = IntFields.parseLong(obj.get("assessedAtMs"), "assessedAtMs");
        String algoVersion = ((JsonString) obj.get("algoVersion")).value();
        double gridG = DecimalFields.parseDouble(obj.get("gridG"), "gridG", 6);
        String publicKey = ((JsonString) obj.get("publicKey")).value();
        String sig = ((JsonString) obj.get("sig")).value();
        return new Assessment(assessmentId, evidenceSha256, profileId, profileSnapshot, intendedProfileId, isReassessment, thresholds, computedMetrics, criteria, verdict, assessedAtMs, algoVersion, gridG, publicKey, sig);
    }
}
