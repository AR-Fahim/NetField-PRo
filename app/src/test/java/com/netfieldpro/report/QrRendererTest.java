package com.netfieldpro.report;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.test.core.app.ApplicationProvider;

import com.google.zxing.qrcode.encoder.QRCode;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.integrity.JvmEcSigner;
import com.netfieldpro.core.integrity.QrPayload;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.seed.DemoSeeder;
import com.netfieldpro.integrity.FinalizeCoordinator;
import com.netfieldpro.time.AndroidClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class QrRendererTest {

    private Context context;
    private NetFieldDatabase db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = NetFieldDatabase.get(context);
        NetFieldApp.onboardingStore().markDone();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
    }

    @Test
    public void testQRS2_demoAssessmentPayloadVersionAtMost16() throws Exception {
        String sessionId = DemoSeeder.seedPass(context);
        assertNotNull(sessionId);

        FinalizeCoordinator coordinator = new FinalizeCoordinator(
                context,
                db,
                NetFieldApp.surveyReadRepository(),
                NetFieldApp.entitlements(),
                new AndroidClock(),
                NetFieldApp.planFileStore(),
                null
        );
        coordinator.setSignerForTest(new JvmEcSigner());

        Assessment assessment = Executors.newSingleThreadExecutor()
                .submit(() -> coordinator.finalizeSessionSync(sessionId, (step, text) -> {}))
                .get();
        assertNotNull(assessment);

        // Build the QR fullJson for this demo assessment
        String ah = assessment.assessmentId();
        String eh = assessment.evidenceSha256();
        String pk = assessment.publicKey();
        String sg = assessment.sig();

        long[] c = new long[]{925, -1, 480, 150, 10, 400, -1, 0};
        String ip = QrPayload.profileLetter(assessment.intendedProfileId());
        String pf = "O";
        String vd = "P";

        byte[] core = QrPayload.buildCore(ah, c, eh, ip, pf, pk, sessionId, assessment.assessedAtMs(), vd);
        String fullJson = QrPayload.fullJson(core, sg);

        QRCode code = QrRenderer.encode(fullJson);
        assertNotNull(code);
        int version = code.getVersion().getVersionNumber();
        assertTrue("QR version must be <= 16, but was " + version, version <= 16);
    }

    @Test
    public void testWorstCasePayloadVersionAtMost16() throws Exception {
        String sid = "A".repeat(22);
        long ts = 1111111111111L;
        String eh = "A".repeat(43);
        String ah = "A".repeat(43);
        String pf = "WR";
        String ip = "WR";
        String vd = "P";
        long[] c = new long[]{1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
        String pk = "A".repeat(44);
        String sg = "A".repeat(86);

        String full = QrPayload.fullJson(ah, c, eh, ip, pf, pk, sid, ts, vd, sg);
        QRCode code = QrRenderer.encode(full);
        assertNotNull(code);
        int version = code.getVersion().getVersionNumber();
        assertTrue("Worst case QR version must be <= 16, but was " + version, version <= 16);
    }

    @Test
    public void testDrawVectorRectangles() throws Exception {
        String testJson = "{\"v\":1,\"test\":true}";
        QRCode code = QrRenderer.encode(testJson);
        assertNotNull(code);

        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float size = QrRenderer.MIN_SIZE_POINTS;
        assertTrue(size >= 141.0f); // at least 5 cm square

        QrRenderer.draw(canvas, code, 10, 10, size, null);
    }
}
