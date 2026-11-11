package com.netfieldpro.ui.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.button.MaterialButton;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.integrity.JvmEcSigner;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.data.seed.DemoSeeder;
import com.netfieldpro.integrity.FinalizeCoordinator;
import com.netfieldpro.time.AndroidClock;
import com.netfieldpro.ui.heatmap.HeatmapFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class S16ReportSmokeTest {

    private Context context;
    private NetFieldDatabase db;
    private Assessment assessment;
    private String sessionId;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        db = NetFieldDatabase.get(context);
        NetFieldApp.setSurveyReadRepositoryForTest(new SurveyReadRepository(db.netFieldDao()));
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
    public void testS16SmokeTest_signedMode() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("assessmentId", assessment.assessmentId());
                args.putString("sessionId", sessionId);
                activity.getNavController().navigate(R.id.nav_s16, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof ReportFragment);
            });

            // Allow background generation and main thread posting
            for (int i = 0; i < 50; i++) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }

            scenario.onActivity(activity -> {
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                ReportFragment fragment = (ReportFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                View content = root.findViewById(R.id.layout_content);
                assertNotNull(content);
                assertEquals(View.VISIBLE, content.getVisibility());

                TextView textTitle = root.findViewById(R.id.text_report_title);
                assertNotNull(textTitle);
                assertTrue(textTitle.getText().toString().contains("NetField-"));
                assertTrue(textTitle.getText().toString().contains(".pdf"));

                TextView textPages = root.findViewById(R.id.text_page_count);
                assertNotNull(textPages);
                assertEquals("4 pages", textPages.getText().toString());

                MaterialButton btnShare = root.findViewById(R.id.btn_share_pdf);
                assertNotNull(btnShare);
                assertEquals(View.VISIBLE, btnShare.getVisibility());

                MaterialButton btnView = root.findViewById(R.id.btn_open_viewer);
                assertNotNull(btnView);
                assertEquals(View.VISIBLE, btnView.getVisibility());
            });
        }
    }

    @Test
    public void testS16SmokeTest_sampleMode() {
        String draftSessionId = DemoSeeder.seedFail(context);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            final String sid = draftSessionId;
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("sessionId", sid);
                activity.getNavController().navigate(R.id.nav_s16, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof ReportFragment);
            });

            for (int i = 0; i < 50; i++) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }

            scenario.onActivity(activity -> {
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                ReportFragment fragment = (ReportFragment) navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(fragment);
                View root = fragment.getView();
                assertNotNull(root);

                View content = root.findViewById(R.id.layout_content);
                assertNotNull(content);
                assertEquals(View.VISIBLE, content.getVisibility());

                TextView textPages = root.findViewById(R.id.text_page_count);
                assertNotNull(textPages);
                assertEquals("2 pages", textPages.getText().toString());
            });
        }
    }

    @Test
    public void testS09_sampleReportButton_navigatesToS16() {
        String draftSessionId = DemoSeeder.seedPass(context);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            final String sid = draftSessionId;
            scenario.onActivity(activity -> {
                Bundle args = new Bundle();
                args.putString("sessionId", sid);
                activity.getNavController().navigate(R.id.nav_s09, args);

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(navHost);
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(current);
                assertTrue(current instanceof HeatmapFragment);

                View root = current.getView();
                assertNotNull(root);
                View btnSample = root.findViewById(R.id.btn_sample_report);
                assertNotNull(btnSample);
                assertEquals(View.VISIBLE, btnSample.getVisibility());

                btnSample.performClick();
                navHost.getChildFragmentManager().executePendingTransactions();

                Fragment reportFrag = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(reportFrag);
                assertTrue(reportFrag instanceof ReportFragment);
            });
        }
    }
}
