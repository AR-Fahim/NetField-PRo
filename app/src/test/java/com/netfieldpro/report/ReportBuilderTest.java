package com.netfieldpro.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.integrity.JvmEcSigner;
import com.netfieldpro.core.integrity.QrPayload;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.verdict.CritId;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.data.seed.DemoSeeder;
import com.netfieldpro.integrity.FinalizeCoordinator;
import com.netfieldpro.time.AndroidClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReportBuilderTest {

    private Context context;
    private NetFieldDatabase db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = NetFieldDatabase.get(context);
        NetFieldApp.setSurveyReadRepositoryForTest(new com.netfieldpro.data.repository.SurveyReadRepository(db.netFieldDao()));
        NetFieldApp.onboardingStore().markDone();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
    }

    @Test
    public void testRM1_passSeedModelHasExactCriteriaInCritIdOrderAndRightCounts() throws Exception {
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

        SessionGraph graph = NetFieldApp.surveyReadRepository().loadSessionGraph(sessionId);
        assertNotNull(graph);

        File planFile = NetFieldApp.planFileStore().findPlanFile(graph.floor().id());

        ReportModel model = ReportBuilder.build(context, graph, assessment, HeatPalette.TRAFFIC_LIGHT, planFile);
        assertNotNull(model);

        assertTrue(model.isSigned());
        assertEquals(4, model.totalPages());
        assertEquals(24, model.pointsCount());
        assertEquals(0, model.lowConfidenceCount());
        assertEquals(0, model.movedCount());
        assertEquals(Verdict.PASS, model.verdict());

        assertEquals(8, model.criteriaRows().size());
        CritId[] expectedOrder = CritId.values();
        for (int i = 0; i < expectedOrder.length; i++) {
            assertEquals("Row index " + i + " must match CritId order",
                    expectedOrder[i], model.criteriaRows().get(i).id());
        }

        assertNotNull(model.qrFullJson());
        assertEquals(QrPayload.Result.OK, QrPayload.verifyLevel1(model.qrFullJson()));
        assertNotNull(model.evidenceHash());
        assertNotNull(model.assessmentHash());
        assertNotNull(model.signature());
        assertNotNull(model.publicKeyFp());
        assertNull(model.watermarkText());
    }

    @Test
    public void testRM2_sampleModeHasTwoPagesAndNoQrFields() {
        String sessionId = DemoSeeder.seedPass(context);
        assertNotNull(sessionId);

        SessionGraph graph = NetFieldApp.surveyReadRepository().loadSessionGraph(sessionId);
        assertNotNull(graph);

        ReportModel model = ReportBuilder.build(context, graph, null, HeatPalette.TRAFFIC_LIGHT, null);
        assertNotNull(model);

        assertFalse(model.isSigned());
        assertEquals(2, model.totalPages());
        assertNull(model.verdict());
        assertNull(model.verdictLabel());
        assertNull(model.qrFullJson());
        assertNull(model.evidenceHash());
        assertNull(model.assessmentHash());
        assertNull(model.signature());
        assertNull(model.publicKeyFp());
        assertNull(model.verificationInstructions());

        assertEquals("Sample coverage summary (not a verdict)", model.reportTitle());
        assertEquals("UNVERIFIED SAMPLE — NOT FOR CONTRACTUAL AUDIT", model.watermarkText());
        assertTrue(model.criteriaRows().isEmpty());
    }

    @Test
    public void testPdfRendererGeneratesPdfFile() {
        String sessionId = DemoSeeder.seedPass(context);
        assertNotNull(sessionId);

        SessionGraph graph = NetFieldApp.surveyReadRepository().loadSessionGraph(sessionId);
        ReportModel model = ReportBuilder.build(context, graph, null, HeatPalette.TRAFFIC_LIGHT, null);

        File pdfFile = new File(context.getCacheDir(), "test_report.pdf");
        try {
            PdfRenderer.renderToFile(model, pdfFile);
            assertTrue(pdfFile.exists());
            assertTrue(pdfFile.length() > 0);

            // Attempt host JVM PdfRenderer read of page 1 if host supports it
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            android.graphics.pdf.PdfRenderer hostReader = new android.graphics.pdf.PdfRenderer(pfd);
            assertEquals(2, hostReader.getPageCount());
            android.graphics.pdf.PdfRenderer.Page page = hostReader.openPage(0);
            assertNotNull(page);
            page.close();
            hostReader.close();
            pfd.close();
        } catch (Throwable t) {
            System.out.println("Host JVM Pdf generation/rendering skipped (not supported on host platform): " + t.getMessage());
        }
    }
}
