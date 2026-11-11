package com.netfieldpro.integrity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.integrity.JvmEcSigner;
import com.netfieldpro.core.integrity.Signer;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.seed.DemoSeeder;
import com.netfieldpro.time.AndroidClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FinalizeCoordinatorTest {

    private Context context;
    private NetFieldDatabase db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = NetFieldDatabase.get(context);
        NetFieldApp.onboardingStore().markDone();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);
    }

    private Assessment runFinalize(FinalizeCoordinator coordinator, String sessionId, FinalizeCoordinator.ProgressCallback progress) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Assessment> future = pool.submit(() -> coordinator.finalizeSessionSync(sessionId, progress));
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception ex) {
                throw ex;
            }
            throw new RuntimeException(e.getCause());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    public void testFC1_successFinalizesAndPassSeedProducesPass() throws Exception {
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

        List<FinalizeCoordinator.Step> progressSteps = new ArrayList<>();
        Assessment assessment = runFinalize(coordinator, sessionId, (step, text) -> progressSteps.add(step));

        assertNotNull(assessment);
        assertEquals(Verdict.PASS, assessment.verdict());
        assertEquals(7, progressSteps.size());

        SurveySessionRow sessionRow = db.netFieldDao().getSurveySession(sessionId);
        assertNotNull(sessionRow);
        assertEquals(SessionState.FINALIZED.name(), sessionRow.state);
        assertTrue(sessionRow.finalizedAtMs > 0);
        assertNotNull(sessionRow.evidenceSha256);
        assertEquals(43, sessionRow.evidenceSha256.length());
        assertNotNull(sessionRow.publicKeyFp);
        assertEquals(16, sessionRow.publicKeyFp.length());

        AssessmentRow assessRow = db.netFieldDao().getAssessment(assessment.assessmentId());
        assertNotNull(assessRow);
        assertEquals(assessment.assessmentId(), assessRow.assessmentId);
        assertEquals("PASS", assessRow.verdict);
    }

    @Test
    public void testFC1_failureAtSignInjectedLeavesSessionDraftAndNoAssessment() {
        String sessionId = DemoSeeder.seedPass(context);
        assertNotNull(sessionId);

        JvmEcSigner realSigner = new JvmEcSigner();
        FinalizeCoordinator coordinator = new FinalizeCoordinator(
                context,
                db,
                NetFieldApp.surveyReadRepository(),
                NetFieldApp.entitlements(),
                new AndroidClock(),
                NetFieldApp.planFileStore(),
                null
        );

        // Inject failure at sign
        coordinator.setSignerForTest(new Signer() {
            @Override
            public byte[] signDer(byte[] msg) {
                throw new RuntimeException("Injected signing failure for test");
            }

            @Override
            public byte[] publicKeyCompressed() {
                return realSigner.publicKeyCompressed();
            }
        });

        try {
            runFinalize(coordinator, sessionId, null);
            fail("Expected exception during finalize");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Injected signing failure")
                    || (e.getCause() != null && e.getCause().getMessage().contains("Injected signing failure")));
        }

        // Verify session remains DRAFT
        SurveySessionRow sessionRow = db.netFieldDao().getSurveySession(sessionId);
        assertNotNull(sessionRow);
        assertEquals(SessionState.DRAFT.name(), sessionRow.state);
        assertTrue(sessionRow.finalizedAtMs == null || sessionRow.finalizedAtMs == 0L);

        // Verify no assessment exists for this session evidence
        AssessmentRow assessRow = db.netFieldDao().getAssessmentByEvidence(sessionRow.evidenceSha256 != null ? sessionRow.evidenceSha256 : "");
        assertNull(assessRow);
    }
}
