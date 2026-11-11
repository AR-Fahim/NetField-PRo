package com.netfieldpro.core.integrity;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonBool;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.core.model.serial.RawSampleSerializer;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.core.util.Sha256;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Evidence {

    public record Part(String name, String sha256) {
        public Part {
            if (name == null || sha256 == null) {
                throw new IllegalArgumentException("Part fields cannot be null");
            }
        }
    }

    private Evidence() {
    }

    public static byte[] partsManifest(long finalizedAtMs, List<Part> parts) {
        if (parts == null) {
            throw new IllegalArgumentException("parts cannot be null");
        }
        List<Part> sorted = new ArrayList<>(parts);
        sorted.sort(Comparator.comparing(Part::name));

        JsonObject manifest = new JsonObject();
        manifest.put("finalizedAtMs", new JsonInt(finalizedAtMs));

        JsonArray partsArr = new JsonArray();
        for (Part p : sorted) {
            JsonObject pObj = new JsonObject();
            pObj.put("name", new JsonString(p.name()));
            pObj.put("sha256", new JsonString(p.sha256()));
            partsArr.add(pObj);
        }
        manifest.put("parts", partsArr);
        manifest.put("schemaVersion", new JsonInt(1));

        return CanonicalJsonWriter.toBytes(manifest);
    }

    public static String evidenceSha256(byte[] manifestBytes) {
        if (manifestBytes == null) {
            throw new IllegalArgumentException("manifestBytes cannot be null");
        }
        return Base64Url.encode(Sha256.hash(manifestBytes));
    }

    public static byte[] assessmentBytes(Assessment a) {
        if (a == null) {
            throw new IllegalArgumentException("Assessment cannot be null");
        }
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
        obj.put("thresholds", a.thresholds());
        obj.put("verdict", new JsonString(a.verdict().name()));
        return CanonicalJsonWriter.toBytes(obj);
    }

    public static String assessmentHash(byte[] assessmentBytes) {
        if (assessmentBytes == null) {
            throw new IllegalArgumentException("assessmentBytes cannot be null");
        }
        return Base64Url.encode(Sha256.hash(assessmentBytes));
    }

    public static String assessmentHash(Assessment a) {
        return assessmentHash(assessmentBytes(a));
    }

    public static byte[] neighborsJson(List<RawSample> nonTargetRawSamples) {
        if (nonTargetRawSamples == null) {
            throw new IllegalArgumentException("nonTargetRawSamples cannot be null");
        }
        List<RawSample> sorted = new ArrayList<>(nonTargetRawSamples);
        sorted.sort(Comparator.comparingLong(RawSample::rowId));

        JsonArray arr = new JsonArray();
        for (RawSample r : sorted) {
            arr.add(RawSampleSerializer.toJson(r));
        }
        return CanonicalJsonWriter.toBytes(arr);
    }
}
