package com.netfieldpro.core.verdict;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.core.util.Sha256;

public final class PresetProfiles {

    public static final ProfileSpec OFFICE_STANDARD = new ProfileSpec(
            "OFFICE_STANDARD",
            -67,
            90,
            false,
            null,
            null,
            50,
            15,
            10,
            40,
            false,
            -1,
            10
    );

    public static final ProfileSpec WAREHOUSE_ROBOTICS = new ProfileSpec(
            "WAREHOUSE_ROBOTICS",
            -65,
            98,
            false,
            -72,
            90,
            30,
            10,
            2,
            40,
            false,
            0,
            8
    );

    public static final ProfileSpec CCTV_SURVEILLANCE = new ProfileSpec(
            "CCTV_SURVEILLANCE",
            -65,
            100,
            true,
            null,
            null,
            25,
            10,
            5,
            40,
            false,
            -1,
            10
    );

    public static final ProfileSpec SMART_HOME_IOT = new ProfileSpec(
            "SMART_HOME_IOT",
            -70,
            90,
            false,
            null,
            null,
            80,
            25,
            20,
            60,
            true,
            -1,
            12
    );

    private static final List<ProfileSpec> ALL = List.of(
            OFFICE_STANDARD,
            WAREHOUSE_ROBOTICS,
            CCTV_SURVEILLANCE,
            SMART_HOME_IOT
    );

    private PresetProfiles() {
    }

    public static String idFor(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        byte[] payload = ("netfield.preset." + key).getBytes(StandardCharsets.UTF_8);
        byte[] hash = Sha256.hash(payload);
        byte[] first16 = Arrays.copyOf(hash, 16);
        return Base64Url.encode(first16);
    }

    public static ProfileSpec byId(String id) {
        if (id == null) {
            return null;
        }
        for (ProfileSpec spec : ALL) {
            if (id.equals(idFor(spec.key())) || id.equals(spec.key())) {
                return spec;
            }
        }
        return null;
    }

    public static List<ProfileSpec> all() {
        return ALL;
    }

    public static JsonObject thresholdsJson(ProfileSpec spec) {
        Objects.requireNonNull(spec, "spec cannot be null");
        JsonObject json = new JsonObject();
        json.put("covDbm", new JsonInt(spec.covDbm()));
        json.put("covPct", new JsonInt(spec.covPct()));
        json.put("covBasis", new JsonString(spec.covBasisTapPins() ? "TAP_PINS" : "AREA"));
        if (spec.secDbm() != null) {
            json.put("secDbm", new JsonInt(spec.secDbm()));
        }
        if (spec.secPct() != null) {
            json.put("secPct", new JsonInt(spec.secPct()));
        }
        json.put("p95Ms", new JsonInt(spec.p95Ms()));
        json.put("jitMs", new JsonInt(spec.jitMs()));
        json.put("lossPermille", new JsonInt(spec.lossPermille()));
        json.put("worstLossPct", new JsonInt(spec.worstLossPct()));
        json.put("worstLossOp", new JsonString(spec.worstLossInclusive() ? "LE" : "LT"));
        json.put("sticky", new JsonInt(spec.sticky()));
        json.put("maxInfluenceM", new JsonInt(spec.maxInfluenceM()));
        json.put("bandPolicy", new JsonString("BEST_ANY"));
        return json;
    }
}

