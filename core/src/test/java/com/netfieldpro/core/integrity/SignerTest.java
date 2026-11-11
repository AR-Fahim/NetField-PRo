package com.netfieldpro.core.integrity;

import com.netfieldpro.core.util.EcdsaCodec;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.Signature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SignerTest {

    @Test
    public void testJvmEcSignerSigningAndCompressedKey() throws Exception {
        JvmEcSigner signer = new JvmEcSigner();
        byte[] pubCompressed = signer.publicKeyCompressed();
        assertNotNull(pubCompressed);
        assertEquals(33, pubCompressed.length);
        assertTrue(pubCompressed[0] == 0x02 || pubCompressed[0] == 0x03);

        byte[] msg = "test-payload".getBytes(StandardCharsets.UTF_8);
        byte[] sigDer = signer.signDer(msg);
        assertNotNull(sigDer);
        assertTrue(sigDer.length > 0);

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(signer.getPublicKey());
        verifier.update(msg);
        assertTrue("Signature verification failed", verifier.verify(sigDer));
    }

    @Test
    public void testCompressPoint() {
        byte[] x = new byte[32];
        x[31] = 1;
        byte[] yEven = new byte[32];
        yEven[31] = 2;
        byte[] yOdd = new byte[32];
        yOdd[31] = 3;

        byte[] compEven = EcdsaCodec.compressPoint(x, yEven);
        assertEquals(33, compEven.length);
        assertEquals(0x02, compEven[0]);
        assertEquals(1, compEven[32]);

        byte[] compOdd = EcdsaCodec.compressPoint(x, yOdd);
        assertEquals(33, compOdd.length);
        assertEquals(0x03, compOdd[0]);
        assertEquals(1, compOdd[32]);
    }
}
