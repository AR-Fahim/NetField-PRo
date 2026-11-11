package com.netfieldpro.ui.assessment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.button.MaterialButton;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.integrity.JvmEcSigner;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.seed.DemoSeeder;
import com.netfieldpro.integrity.FinalizeCoordinator;
import com.netfieldpro.time.AndroidClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S15AssessmentSmokeTest {

    private Context context;
    private NetFieldDatabase db;
    private Assessment assessment;
    private String sessionId;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        db = NetFieldDatabase.get(context);
        NetFieldApp.onboardingStore().markDone();
        NetFieldApp.entitlements().setDebugTier(Tier.PRO);

        sessionId = DemoSeeder.seedPass(context);
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

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Assessment> future = pool.submit(() -> coordinator.finalizeSessionSync(sessionId, (s, m) -> {}));
            assessment = future.get();
        } finally {
            pool.shutdown();
        }
        assertNotNull(assessment);
        assertEquals(Verdict.PASS, assessment.verdict());
    }

    @Test
    public void testS15SmokeTest_launchesAndDisplaysCriteriaAndHashes() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("assessmentId", assessment.assessmentId());
                activity.getNavController().navigate(R.id.nav_s15, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof AssessmentFragment);

                View root = current.getView();
                assertNotNull(root);

                TextView screenTitle = root.findViewById(R.id.text_screen_title);
                assertNotNull(screenTitle);
                assertEquals(activity.getString(R.string.screen_s15_title), screenTitle.getText().toString());

                TextView verdictBadge = root.findViewById(R.id.text_verdict_badge);
                assertNotNull(verdictBadge);
                assertEquals(activity.getString(R.string.verdict_pass), verdictBadge.getText().toString());

                TextView evidenceGrade = root.findViewById(R.id.text_evidence_grade);
                assertNotNull(evidenceGrade);
                assertEquals(activity.getString(R.string.grade_verdict), evidenceGrade.getText().toString());

                TextView textKeyFp = root.findViewById(R.id.text_key_fp);
                assertNotNull(textKeyFp);
                assertTrue(textKeyFp.getText().toString().startsWith("Key fp"));

                TextView textEvidenceSha = root.findViewById(R.id.text_evidence_sha);
                assertNotNull(textEvidenceSha);
                assertTrue(textEvidenceSha.getText().toString().startsWith("Evidence"));

                LinearLayout criteriaList = root.findViewById(R.id.layout_criteria_list);
                assertNotNull(criteriaList);
                // 8 criteria rows
                assertEquals(8, criteriaList.getChildCount());

                MaterialButton btnReport = root.findViewById(R.id.btn_create_report);
                assertNotNull(btnReport);

                MaterialButton btnHeatmap = root.findViewById(R.id.btn_view_heatmap);
                assertNotNull(btnHeatmap);
            });
        }
    }

    @Test
    public void testHeatmapFragment_showsViewAssessmentWhenFinalized() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("sessionId", sessionId);
                activity.getNavController().navigate(R.id.nav_s09, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof com.netfieldpro.ui.heatmap.HeatmapFragment);

                MaterialButton btnFinalize = current.getView().findViewById(R.id.btn_finalize);
                assertNotNull(btnFinalize);
                assertEquals(activity.getString(R.string.btn_view_assessment), btnFinalize.getText().toString());
            });
        }
    }
}
