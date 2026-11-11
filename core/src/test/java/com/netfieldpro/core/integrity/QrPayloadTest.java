package com.netfieldpro.core.integrity;

import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.util.Base64Url;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QrPayloadTest {

    private static String strOf(char ch, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    @Test
    public void testQr1WorstCaseSize() {
        String sid = strOf('A', 22);
        long ts = 1111111111111L;
        String eh = strOf('A', 43);
        String ah = strOf('A', 43);
        String pf = "WR";
        String ip = "WR";
        String vd = "P";
        long[] c = new long[]{1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
        String pk = strOf('A', 44);
        String sg = strOf('A', 86);

        String full = QrPayload.fullJson(ah, c, eh, ip, pf, pk, sid, ts, vd, sg);
        byte[] bytes = full.getBytes(StandardCharsets.UTF_8);
        assertEquals(380, bytes.length);
        assertTrue(bytes.length <= 384);
    }

    @Test
    public void testSignAndVerifyLevel1() {
        JvmEcSigner signer = new JvmEcSigner();
        String pk = Base64Url.encode(signer.publicKeyCompressed());
        String sid = "test-session-id-123456";
        long ts = 1700000000000L;
        String eh = strOf('E', 43);
        String ah = strOf('A', 43);
        String pf = "O";
        String ip = "O";
        String vd = "P";
        long[] c = new long[]{925, -1, 480, 150, 10, 400, -1, 0};

        byte[] core = QrPayload.buildCore(ah, c, eh, ip, pf, pk, sid, ts, vd);
        String sg = QrPayload.sign(core, signer);
        String full = QrPayload.fullJson(core, sg);

        assertEquals(QrPayload.Result.OK, QrPayload.verifyLevel1(full));

        // Tamper ts
        String tamperedTs = full.replace("\"ts\":1700000000000", "\"ts\":1700000000001");
        assertEquals(QrPayload.Result.BAD, QrPayload.verifyLevel1(tamperedTs));

        // Tamper vd
        String tamperedVd = full.replace("\"vd\":\"P\"", "\"vd\":\"F\"");
        assertEquals(QrPayload.Result.BAD, QrPayload.verifyLevel1(tamperedVd));

        // Tamper c
        String tamperedC = full.replace("925", "926");
        assertEquals(QrPayload.Result.BAD, QrPayload.verifyLevel1(tamperedC));

        // Tamper ah
        String tamperedAh = full.replace(ah, strOf('B', 43));
        assertEquals(QrPayload.Result.BAD, QrPayload.verifyLevel1(tamperedAh));
    }

    @Test
    public void testProfileAndVerdictLetters() {
        assertEquals("O", QrPayload.profileLetter("OFFICE_STANDARD"));
        assertEquals("W", QrPayload.profileLetter("WAREHOUSE_ROBOTICS"));
        assertEquals("C", QrPayload.profileLetter("CCTV_SURVEILLANCE"));
        assertEquals("H", QrPayload.profileLetter("SMART_HOME_IOT"));
        assertEquals("X", QrPayload.profileLetter("CUSTOM"));

        assertEquals("P", QrPayload.verdictLetter(Verdict.PASS));
        assertEquals("F", QrPayload.verdictLetter(Verdict.FAIL));
        assertEquals("I", QrPayload.verdictLetter(Verdict.INCOMPLETE));
        assertEquals("N", QrPayload.verdictLetter(Verdict.NOT_ELIGIBLE));
    }
}
