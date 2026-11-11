package com.netfieldpro;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.netfieldpro.core.json.CanonicalJson;
import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.EntityRegistry;
import com.netfieldpro.core.model.EntitySpec;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.core.util.EcdsaCodec;
import com.netfieldpro.core.util.Sha256;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class CoreSmokeOnDeviceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testEntityRegistrySmoke() throws Exception {
        for (EntitySpec spec : EntityRegistry.ALL) {
            Object sample = spec.sampleFactory().get();
            assertNotNull(spec.name() + " sample is null", sample);
            JsonObject json = (JsonObject) spec.toJson().apply(sample);
            assertNotNull(spec.name() + " json is null", json);
            byte[] canonicalBytes = CanonicalJsonWriter.toBytes(json);
            assertNotNull(spec.name() + " canonicalBytes is null", canonicalBytes);
            JsonValue parsed = JsonParser.parse(canonicalBytes);
            assertNotNull(spec.name() + " parsed is null", parsed);
            CanonicalJson.verifyCanonical(canonicalBytes);
        }
    }

    @Test
    public void testVectorsSmoke() {
        // HA-1
        byte[] ha1 = Sha256.hash(new byte[0]);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(ha1));
        assertEquals("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", Sha256.b64(ha1));

        // HA-2
        byte[] ha2 = Sha256.hash("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.hex(ha2));

        // HA-3
        byte[] ha3 = Sha256.hash("{}".getBytes(StandardCharsets.UTF_8));
        assertEquals("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", Sha256.hex(ha3));

        // B64-2
        byte[] b64Bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            b64Bytes[i] = (byte) i;
        }
        assertEquals("AAECAwQFBgcICQoLDA0ODw", Base64Url.encode(b64Bytes));
        assertArrayEquals(b64Bytes, Base64Url.decode("AAECAwQFBgcICQoLDA0ODw"));

        // DER-1
        byte[] der1 = hexToBytes("3006020101020102");
        byte[] raw1 = EcdsaCodec.derToRaw(der1);
        assertEquals(64, raw1.length);
        assertEquals(1, raw1[31]);
        assertEquals(2, raw1[63]);

        // DER-4
        byte[] raw64FF = new byte[64];
        Arrays.fill(raw64FF, (byte) 0xFF);
        byte[] der4 = EcdsaCodec.rawToDer(raw64FF);
        assertEquals(72, der4.length);
        assertEquals((byte) 0x30, der4[0]);
        assertEquals((byte) 0x46, der4[1]);
        assertEquals((byte) 0x02, der4[2]);
        assertEquals((byte) 0x21, der4[3]);
        assertEquals((byte) 0x00, der4[4]);
        for (int i = 5; i < 37; i++) {
            assertEquals((byte) 0xFF, der4[i]);
        }
        assertEquals((byte) 0x02, der4[37]);
        assertEquals((byte) 0x21, der4[38]);
        assertEquals((byte) 0x00, der4[39]);
        for (int i = 40; i < 72; i++) {
            assertEquals((byte) 0xFF, der4[i]);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
