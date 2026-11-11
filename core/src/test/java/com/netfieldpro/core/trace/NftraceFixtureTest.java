package com.netfieldpro.core.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.security.MessageDigest;

import org.junit.Test;

public class NftraceFixtureTest {

    private static final String EXPECTED_SHA256 =
            "367b4a45039739a764853a456acbb1fd000963643d79681ea0803e907908331d";

    @Test
    public void testGoldenFixtureSha256() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("traces/sample_v1.nftrace");
        assertNotNull("Could not find sample_v1.nftrace on classpath", stream);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[1024];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        stream.close();

        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }

        assertEquals(EXPECTED_SHA256, sb.toString());
    }
}

