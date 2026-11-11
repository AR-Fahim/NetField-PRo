package com.netfieldpro.core.integrity;

import com.netfieldpro.core.json.CanonicalJson;
import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.core.util.EcdsaCodec;
import com.netfieldpro.core.verdict.PresetProfiles;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Map;

public final class QrPayload {

    public enum Result {
        OK,
        BAD
    }

    private static final BigInteger SECP256R1_P = new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16);
    private static final BigInteger SECP256R1_B = new BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16);

    private QrPayload() {
    }

    public static byte[] buildCore(String ah, long[] c, String eh, String ip, String pf, String pk, String sid, long ts, String vdLetter) {
        if (ah == null || eh == null || ip == null || pf == null || pk == null || sid == null || vdLetter == null) {
            throw new IllegalArgumentException("Arguments to buildCore cannot be null");
        }
        JsonObject obj = new JsonObject();
        obj.put("ah", new JsonString(ah));
        JsonArray cArr = new JsonArray();
        if (c != null) {
            for (long val : c) {
                cArr.add(new JsonInt(val));
            }
        }
        obj.put("c", cArr);
        obj.put("eh", new JsonString(eh));
        obj.put("ip", new JsonString(ip));
        obj.put("pf", new JsonString(pf));
        obj.put("pk", new JsonString(pk));
        obj.put("sid", new JsonString(sid));
        obj.put("ts", new JsonInt(ts));
        obj.put("v", new JsonInt(1));
        obj.put("vd", new JsonString(vdLetter));
        return CanonicalJsonWriter.toBytes(obj);
    }

    public static String sign(byte[] core, Signer signer) {
        if (core == null || signer == null) {
            throw new IllegalArgumentException("core and signer cannot be null");
        }
        byte[] der = signer.signDer(core);
        byte[] raw = EcdsaCodec.derToRaw(der);
        return Base64Url.encode(raw);
    }

    public static String fullJson(byte[] core, String sg) {
        if (core == null || sg == null) {
            throw new IllegalArgumentException("core and sg cannot be null");
        }
        try {
            JsonValue parsed = JsonParser.parse(core);
            if (!(parsed instanceof JsonObject obj)) {
                throw new IllegalArgumentException("core must be a JsonObject");
            }
            obj.put("sg", new JsonString(sg));
            return new String(CanonicalJsonWriter.toBytes(obj), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse core JSON", e);
        }
    }

    public static String fullJson(String ah, long[] c, String eh, String ip, String pf, String pk, String sid, long ts, String vdLetter, String sg) {
        byte[] core = buildCore(ah, c, eh, ip, pf, pk, sid, ts, vdLetter);
        return fullJson(core, sg);
    }

    public static Result verifyLevel1(String fullJson) {
        if (fullJson == null) {
            return Result.BAD;
        }
        try {
            byte[] bytes = fullJson.getBytes(StandardCharsets.UTF_8);
            CanonicalJson.verifyCanonical(bytes);
            JsonValue parsed = JsonParser.parse(bytes);
            if (!(parsed instanceof JsonObject obj)) {
                return Result.BAD;
            }
            if (!obj.containsKey("ah") || !obj.containsKey("c") || !obj.containsKey("eh")
                    || !obj.containsKey("ip") || !obj.containsKey("pf") || !obj.containsKey("pk")
                    || !obj.containsKey("sg") || !obj.containsKey("sid") || !obj.containsKey("ts")
                    || !obj.containsKey("v") || !obj.containsKey("vd")) {
                return Result.BAD;
            }
            if (!(obj.get("v") instanceof JsonInt vInt) || vInt.value() != 1) {
                return Result.BAD;
            }
            if (!(obj.get("pk") instanceof JsonString pkStr) || !(obj.get("sg") instanceof JsonString sgStr)) {
                return Result.BAD;
            }
            String pk = pkStr.value();
            String sg = sgStr.value();
            if (pk.length() != 44 || sg.length() != 86) {
                return Result.BAD;
            }
            byte[] pkBytes = Base64Url.decode(pk);
            if (pkBytes.length != 33) {
                return Result.BAD;
            }
            byte[] rawSig = Base64Url.decode(sg);
            if (rawSig.length != 64) {
                return Result.BAD;
            }
            byte[] derSig = EcdsaCodec.rawToDer(rawSig);

            JsonObject coreObj = new JsonObject();
            for (Map.Entry<String, JsonValue> entry : obj.properties().entrySet()) {
                if (!entry.getKey().equals("sg")) {
                    coreObj.put(entry.getKey(), entry.getValue());
                }
            }
            byte[] coreBytes = CanonicalJsonWriter.toBytes(coreObj);

            PublicKey pubKey = decodeCompressedPublicKey(pkBytes);
            if (pubKey == null) {
                return Result.BAD;
            }

            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(pubKey);
            verifier.update(coreBytes);
            return verifier.verify(derSig) ? Result.OK : Result.BAD;
        } catch (Exception e) {
            return Result.BAD;
        }
    }

    public static PublicKey decodeCompressedPublicKey(byte[] comp) {
        if (comp == null || comp.length != 33) {
            return null;
        }
        byte header = comp[0];
        if (header != 0x02 && header != 0x03) {
            return null;
        }
        byte[] xBytes = new byte[32];
        System.arraycopy(comp, 1, xBytes, 0, 32);
        BigInteger x = new BigInteger(1, xBytes);
        if (x.compareTo(SECP256R1_P) >= 0) {
            return null;
        }

        BigInteger rhs = x.pow(3).subtract(x.multiply(BigInteger.valueOf(3))).add(SECP256R1_B).mod(SECP256R1_P);
        BigInteger exp = SECP256R1_P.add(BigInteger.ONE).shiftRight(2);
        BigInteger y = rhs.modPow(exp, SECP256R1_P);
        if (!y.modPow(BigInteger.valueOf(2), SECP256R1_P).equals(rhs)) {
            return null;
        }

        boolean wantOdd = (header == 0x03);
        if (y.testBit(0) != wantOdd) {
            y = SECP256R1_P.subtract(y);
        }

        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParams = params.getParameterSpec(ECParameterSpec.class);
            ECPoint point = new ECPoint(x, y);
            ECPublicKeySpec keySpec = new ECPublicKeySpec(point, ecParams);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePublic(keySpec);
        } catch (Exception e) {
            return null;
        }
    }

    public static String profileLetter(String profileKeyOrId) {
        if (profileKeyOrId == null) {
            return "X";
        }
        if (profileKeyOrId.equals(PresetProfiles.OFFICE_STANDARD.key()) || profileKeyOrId.equals(PresetProfiles.idFor(PresetProfiles.OFFICE_STANDARD.key()))) {
            return "O";
        }
        if (profileKeyOrId.equals(PresetProfiles.WAREHOUSE_ROBOTICS.key()) || profileKeyOrId.equals(PresetProfiles.idFor(PresetProfiles.WAREHOUSE_ROBOTICS.key()))) {
            return "W";
        }
        if (profileKeyOrId.equals(PresetProfiles.CCTV_SURVEILLANCE.key()) || profileKeyOrId.equals(PresetProfiles.idFor(PresetProfiles.CCTV_SURVEILLANCE.key()))) {
            return "C";
        }
        if (profileKeyOrId.equals(PresetProfiles.SMART_HOME_IOT.key()) || profileKeyOrId.equals(PresetProfiles.idFor(PresetProfiles.SMART_HOME_IOT.key()))) {
            return "H";
        }
        return "X";
    }

    public static String verdictLetter(Verdict verdict) {
        if (verdict == null) {
            return "N";
        }
        return switch (verdict) {
            case PASS -> "P";
            case FAIL -> "F";
            case INCOMPLETE -> "I";
            case NOT_ELIGIBLE -> "N";
        };
    }

    public static String verdictLetter(String name) {
        if (name == null) {
            return "N";
        }
        try {
            return verdictLetter(Verdict.valueOf(name));
        } catch (Exception e) {
            return "N";
        }
    }
}
