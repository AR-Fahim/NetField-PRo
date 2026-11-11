package com.netfieldpro.data.seed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.assess.PerPoint;
import com.netfieldpro.core.assess.SessionMetrics;
import com.netfieldpro.core.heat.HeatPipeline;
import com.netfieldpro.core.heat.Polygon;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.heat.WallChain;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.core.verdict.Inputs;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.core.verdict.Result;
import com.netfieldpro.core.verdict.VerdictEngine;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.ui.heatmap.HeatmapFragment;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DemoSeederTest {

    @Before
    public void setUp() {
        // Ensure app repositories are initialized with application context
        NetFieldApp app = ApplicationProvider.getApplicationContext();
        NetFieldApp.onboardingStore().markDone();
    }

    @Test
    public void testDEMO1_passSeedGivesVerdictPass() {
        String sessionId = DemoSeeder.seedPass(ApplicationProvider.getApplicationContext());
        assertNotNull(sessionId);

        SurveyReadRepository repo = NetFieldApp.surveyReadRepository();
        SessionGraph graph = repo.loadSessionGraph(sessionId);
        assertNotNull(graph);
        assertEquals(24, graph.points().size());
        assertTrue(graph.hasBoundary());

        Polygon boundary = graph.toBoundaryPolygon();
        List<WallChain> wallChains = graph.toWallChains();
        List<Sample> samples = graph.samplesForCoverage();
        ProfileSpec profile = PresetProfiles.OFFICE_STANDARD;

        HeatPipeline.Request req = new HeatPipeline.Request(
                boundary,
                wallChains,
                graph.toWallPenalties(),
                samples,
                profile.maxInfluenceM(),
                () -> false
        );
        HeatPipeline.Result heatResult = HeatPipeline.compute(req, 0L);
        assertNotNull(heatResult);

        PerPoint[] perPoints = graph.toPerPoints();
        Inputs inputs = SessionMetrics.compute(
                graph.session().evidenceGrade(),
                heatResult,
                null,
                profile,
                perPoints
        );

        Result result = VerdictEngine.evaluate(profile, inputs);
        assertEquals(Verdict.PASS, result.verdict());
    }

    @Test
    public void testDEMO2_failSeedGivesVerdictFail() {
        String sessionId = DemoSeeder.seedFail(ApplicationProvider.getApplicationContext());
        assertNotNull(sessionId);

        SurveyReadRepository repo = NetFieldApp.surveyReadRepository();
        SessionGraph graph = repo.loadSessionGraph(sessionId);
        assertNotNull(graph);
        assertEquals(24, graph.points().size());
        assertTrue(graph.hasBoundary());

        Polygon boundary = graph.toBoundaryPolygon();
        List<WallChain> wallChains = graph.toWallChains();
        List<Sample> samples = graph.samplesForCoverage();
        ProfileSpec profile = PresetProfiles.OFFICE_STANDARD;

        HeatPipeline.Request req = new HeatPipeline.Request(
                boundary,
                wallChains,
                graph.toWallPenalties(),
                samples,
                profile.maxInfluenceM(),
                () -> false
        );
        HeatPipeline.Result heatResult = HeatPipeline.compute(req, 0L);
        assertNotNull(heatResult);

        PerPoint[] perPoints = graph.toPerPoints();
        Inputs inputs = SessionMetrics.compute(
                graph.session().evidenceGrade(),
                heatResult,
                null,
                profile,
                perPoints
        );

        Result result = VerdictEngine.evaluate(profile, inputs);
        assertEquals(Verdict.FAIL, result.verdict());
    }

    @Test
    public void testS09SmokeTest_launchesWithSeedAndChipVerdictExists() {
        String sessionId = DemoSeeder.seedPass(ApplicationProvider.getApplicationContext());
        assertNotNull(sessionId);

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
                assertTrue(current instanceof HeatmapFragment);
                assertNotNull(current.getView());
                assertNotNull(current.getView().findViewById(R.id.chip_verdict));
                assertNotNull(current.getView().findViewById(R.id.plan_view));
                assertNotNull(current.getView().findViewById(R.id.chip_layer_coverage));
                assertNotNull(current.getView().findViewById(R.id.btn_finalize));
            });
        }
    }

    @Test
    public void testSeedOnLatestFloor_syntheticFloor_assertPointsAndBoundary() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        NetFieldDao dao = NetFieldDatabase.get(context).netFieldDao();

        String projId = IdGenerator.generate();
        ProjectRow proj = new ProjectRow(projId, "Test Project", "Client", "Site", 1700000000000L);
        dao.insertProject(proj);

        String floorId = IdGenerator.generate();
        FloorRow floor = new FloorRow();
        floor.id = floorId;
        floor.projectId = projId;
        floor.levelIndex = 1L;
        floor.planSha256 = "dummy";
        floor.planWpx = 1000;
        floor.planHpx = 800;
        floor.metersPerPixel = 0.04;
        floor.scaleMethod = "LINE";
        floor.scaleUncertaintyPct = 0.1;
        floor.scaleChecked = true;
        dao.insertFloor(floor);

        String sessId = DemoSeeder.seedOnLatestFloor(context, true);
        assertNotNull(sessId);

        SurveyReadRepository repo = NetFieldApp.surveyReadRepository();
        SessionGraph graph = repo.loadSessionGraph(sessId);
        assertNotNull(graph);
        assertEquals(24, graph.points().size());
        assertTrue(graph.hasBoundary());
        assertNotNull(graph.toBoundaryPolygon());
    }
}
