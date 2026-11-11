package com.netfieldpro.ui.heatmap;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.assess.PerPoint;
import com.netfieldpro.core.assess.SessionMetrics;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.heat.HeatPipeline;
import com.netfieldpro.core.heat.IdwSurface;
import com.netfieldpro.core.heat.Legend;
import com.netfieldpro.core.heat.PaletteId;
import com.netfieldpro.core.heat.Polygon;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.heat.WallChain;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.verdict.Inputs;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.core.verdict.Result;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.verdict.VerdictEngine;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.data.repository.SurveyReadRepository.PointRecord;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.databinding.FragmentHeatmapBinding;
import com.netfieldpro.paywall.PaywallGate;
import com.netfieldpro.settings.AppSettings;

public class HeatmapFragment extends Fragment {

    private static final String KEY_LAYER_INDEX = "heatmap_layer_index";
    private static final String KEY_PALETTE_ID = "heatmap_palette_id";

    private static final int LAYER_COVERAGE = 0;
    private static final int LAYER_24 = 1;
    private static final int LAYER_5 = 2;
    private static final int LAYER_6 = 3;
    private static final int LAYER_LATENCY = 4;

    private FragmentHeatmapBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String sessionId;
    private int currentLayer = LAYER_COVERAGE;
    private PaletteId currentPalette = PaletteId.TRAFFIC_LIGHT;
    private SessionGraph sessionGraph;

    private AtomicBoolean currentCancelFlag = new AtomicBoolean(false);
    private Runnable pendingSpinnerRunnable;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHeatmapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId");
        }

        if (savedInstanceState != null) {
            currentLayer = savedInstanceState.getInt(KEY_LAYER_INDEX, LAYER_COVERAGE);
            String palName = savedInstanceState.getString(KEY_PALETTE_ID);
            if (palName != null) {
                try {
                    currentPalette = PaletteId.valueOf(palName);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } else {
            HeatPalette hp = AppSettings.get(requireContext()).getHeatPalette();
            currentPalette = (hp == HeatPalette.VIRIDIS) ? PaletteId.VIRIDIS : PaletteId.TRAFFIC_LIGHT;
        }

        setupButtons();
        setupLayerChips();
        loadSessionData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionGraph != null) {
            loadSessionData();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LAYER_INDEX, currentLayer);
        outState.putString(KEY_PALETTE_ID, currentPalette.name());
    }

    private void setupButtons() {
        binding.btnPalette.setOnClickListener(v -> togglePalette());

        binding.btnSampleReport.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("sessionId", sessionId);
            try {
                Navigation.findNavController(v).navigate(R.id.action_s09_to_s16, args);
            } catch (Exception e) {
                Navigation.findNavController(v).navigate(R.id.nav_s16, args);
            }
        });

        binding.btnEditGeometry.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("sessionId", sessionId);
            if (sessionGraph != null && sessionGraph.floor() != null) {
                args.putString("floorId", sessionGraph.floor().id());
            }
            try {
                Navigation.findNavController(v).navigate(R.id.action_s09_to_s10, args);
            } catch (Exception e) {
                try {
                    Navigation.findNavController(v).navigate(R.id.nav_s10, args);
                } catch (Exception ignored) {
                }
            }
        });

        binding.btnFinalize.setOnClickListener(v -> {
            if (sessionGraph != null && sessionGraph.session() != null && sessionGraph.session().state() == SessionState.FINALIZED) {
                Bundle args = new Bundle();
                args.putString("sessionId", sessionId);
                NavController nc = Navigation.findNavController(binding.getRoot());
                try {
                    nc.navigate(R.id.action_s09_to_s13, args);
                } catch (Exception e) {
                    try {
                        nc.navigate(R.id.nav_s13, args);
                    } catch (Exception ignored) {
                    }
                }
                return;
            }
            PaywallGate.require(this, Feature.FINALIZE_SIGN, () -> {
                Bundle args = new Bundle();
                args.putString("sessionId", sessionId);
                NavController nc = Navigation.findNavController(binding.getRoot());
                try {
                    nc.navigate(R.id.action_s09_to_s13, args);
                } catch (Exception e) {
                    try {
                        nc.navigate(R.id.nav_s13, args);
                    } catch (Exception ignored) {
                    }
                }
            });
        });
    }

    private void togglePalette() {
        currentPalette = (currentPalette == PaletteId.TRAFFIC_LIGHT)
                ? PaletteId.VIRIDIS
                : PaletteId.TRAFFIC_LIGHT;

        HeatPalette hp = (currentPalette == PaletteId.VIRIDIS)
                ? HeatPalette.VIRIDIS
                : HeatPalette.TRAFFIC_LIGHT;
        AppSettings.get(requireContext()).setHeatPalette(hp);

        updateLegend();
        recomputeHeat();
    }

    private void setupLayerChips() {
        binding.chipLayerCoverage.setOnClickListener(v -> selectLayer(LAYER_COVERAGE));
        binding.chipLayer24.setOnClickListener(v -> selectLayer(LAYER_24));
        binding.chipLayer5.setOnClickListener(v -> selectLayer(LAYER_5));
        binding.chipLayer6.setOnClickListener(v -> selectLayer(LAYER_6));
        binding.chipLayerLatency.setOnClickListener(v -> selectLayer(LAYER_LATENCY));

        updateLayerChipSelection();
    }

    private void selectLayer(int layer) {
        currentLayer = layer;
        updateLayerChipSelection();
        updateLegend();
        recomputeHeat();
    }

    private void updateLayerChipSelection() {
        binding.chipLayerCoverage.setChecked(currentLayer == LAYER_COVERAGE);
        binding.chipLayer24.setChecked(currentLayer == LAYER_24);
        binding.chipLayer5.setChecked(currentLayer == LAYER_5);
        binding.chipLayer6.setChecked(currentLayer == LAYER_6);
        binding.chipLayerLatency.setChecked(currentLayer == LAYER_LATENCY);
    }

    private void loadSessionData() {
        if (sessionId == null) {
            return;
        }

        sessionGraph = NetFieldApp.surveyReadRepository().loadSessionGraph(sessionId);
        if (sessionGraph == null) {
            return;
        }

        // Header text
        String floorLabel = (sessionGraph.floor() != null)
                ? getString(R.string.floor_level_label, sessionGraph.floor().levelIndex())
                : "";
        String projName = (sessionGraph.project() != null) ? sessionGraph.project().name() : "";
        if (!floorLabel.isEmpty() && !projName.isEmpty()) {
            binding.textHeatmapHeader.setText(floorLabel + " · " + projName);
        } else if (!projName.isEmpty()) {
            binding.textHeatmapHeader.setText(projName);
        }

        if (sessionGraph.session() != null && sessionGraph.session().state() == SessionState.FINALIZED) {
            binding.btnFinalize.setText(R.string.btn_view_assessment);
            binding.btnSampleReport.setVisibility(View.GONE);
        } else {
            binding.btnFinalize.setText(R.string.btn_finalize);
            binding.btnSampleReport.setVisibility(View.VISIBLE);
        }

        // Load plan image into PlanView
        if (sessionGraph.floor() != null) {
            File planFile = NetFieldApp.planFileStore().findPlanFile(sessionGraph.floor().id());
            if (planFile != null && planFile.exists()) {
                binding.planView.loadPlanFile(planFile);
            } else if (sessionGraph.floor().planWpx() > 0 && sessionGraph.floor().planHpx() > 0) {
                binding.planView.setImageDimensions(
                        sessionGraph.floor().planWpx(),
                        sessionGraph.floor().planHpx()
                );
            }
        }

        // Update layer chips availability
        boolean has24 = sessionGraph.hasBandData(2400, 2500);
        boolean has5 = sessionGraph.hasBandData(4900, 5900);
        boolean has6 = sessionGraph.hasBandData(5925, 7125);
        boolean hasLat = sessionGraph.hasLatencyData();

        binding.chipLayer24.setEnabled(has24);
        if (!has24) binding.chipLayer24.setText(getString(R.string.layer_24) + " (" + getString(R.string.layer_no_data) + ")");

        binding.chipLayer5.setEnabled(has5);
        if (!has5) binding.chipLayer5.setText(getString(R.string.layer_5) + " (" + getString(R.string.layer_no_data) + ")");

        binding.chipLayer6.setEnabled(has6);
        if (!has6) binding.chipLayer6.setText(getString(R.string.layer_6) + " (" + getString(R.string.layer_no_data) + ")");

        binding.chipLayerLatency.setEnabled(hasLat);
        if (!hasLat) binding.chipLayerLatency.setText(getString(R.string.layer_latency) + " (" + getString(R.string.layer_no_data) + ")");

        updateLegend();
        recomputeHeat();
    }

    private void updateLegend() {
        if (binding == null) return;
        if (currentLayer == LAYER_LATENCY) {
            binding.legendRow1.setText(R.string.legend_row_1_lat);
            binding.legendRow2.setText(R.string.legend_row_2_lat);
            binding.legendRow3.setText(R.string.legend_row_3_lat);
            binding.legendRow4.setText(R.string.legend_row_4_lat);
            binding.legendRow5.setVisibility(View.GONE);
        } else {
            binding.legendRow1.setText(R.string.legend_row_1_rssi);
            binding.legendRow2.setText(R.string.legend_row_2_rssi);
            binding.legendRow3.setText(R.string.legend_row_3_rssi);
            binding.legendRow4.setText(R.string.legend_row_4_rssi);
            binding.legendRow5.setText(R.string.legend_row_5_rssi);
            binding.legendRow5.setVisibility(View.VISIBLE);
        }
    }

    private void recomputeHeat() {
        if (sessionGraph == null || binding == null) {
            return;
        }

        if (sessionGraph.points() == null || sessionGraph.points().size() < 3) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.chipVerdict.setVisibility(View.GONE);
            binding.textCells.setVisibility(View.GONE);
            binding.planView.setOverlayDrawer(null);
            return;
        }

        binding.textEmpty.setVisibility(View.GONE);
        binding.chipVerdict.setVisibility(View.VISIBLE);
        binding.textCells.setVisibility(View.VISIBLE);

        currentCancelFlag.set(true);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        currentCancelFlag = cancelFlag;

        if (pendingSpinnerRunnable != null) {
            mainHandler.removeCallbacks(pendingSpinnerRunnable);
        }
        pendingSpinnerRunnable = () -> {
            if (binding != null && !cancelFlag.get()) {
                binding.progressLoading.setVisibility(View.VISIBLE);
            }
        };
        mainHandler.postDelayed(pendingSpinnerRunnable, 300);

        final int layer = currentLayer;
        final PaletteId palette = currentPalette;
        final SessionGraph graph = sessionGraph;
        final boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        final float density = getResources().getDisplayMetrics().density;

        executor.execute(() -> {
            List<Sample> samples;
            List<Double> pointDisplayValues = new ArrayList<>();
            switch (layer) {
                case LAYER_24:
                    samples = graph.samplesForBand(2400, 2500);
                    for (Sample s : samples) pointDisplayValues.add(s.dbm());
                    break;
                case LAYER_5:
                    samples = graph.samplesForBand(4900, 5900);
                    for (Sample s : samples) pointDisplayValues.add(s.dbm());
                    break;
                case LAYER_6:
                    samples = graph.samplesForBand(5925, 7125);
                    for (Sample s : samples) pointDisplayValues.add(s.dbm());
                    break;
                case LAYER_LATENCY:
                    samples = graph.samplesForLatency();
                    for (Sample s : samples) pointDisplayValues.add(s.dbm());
                    break;
                case LAYER_COVERAGE:
                default:
                    samples = graph.samplesForCoverage();
                    for (Sample s : samples) pointDisplayValues.add(s.dbm());
                    break;
            }

            Polygon boundary = graph.toBoundaryPolygon();
            List<WallChain> wallChains = graph.toWallChains();

            ProfileSpec resolvedProf = (graph.profile() != null)
                    ? PresetProfiles.byId(graph.session().intendedProfileId())
                    : PresetProfiles.OFFICE_STANDARD;
            if (resolvedProf == null) {
                resolvedProf = PresetProfiles.OFFICE_STANDARD;
            }
            final ProfileSpec profile = resolvedProf;

            double rEffMax = (profile != null) ? profile.maxInfluenceM() : 10.0;

            HeatPipeline.Request req = new HeatPipeline.Request(
                    boundary,
                    wallChains,
                    graph.toWallPenalties(),
                    samples,
                    rEffMax,
                    cancelFlag::get
            );

            HeatPipeline.Result heatResult = HeatPipeline.compute(req, 0L);

            if (cancelFlag.get() || heatResult == null) {
                return;
            }

            Bitmap heatBitmap = HeatOverlay.createHeatBitmap(
                    heatResult.surface(),
                    heatResult.cols(),
                    heatResult.rows(),
                    palette,
                    isDark
            );

            double mpp = (graph.floor() != null) ? graph.floor().metersPerPixel() : 0.025;
            HeatOverlay overlay = new HeatOverlay(
                    heatBitmap,
                    boundary.minX(),
                    boundary.minY(),
                    heatResult.g(),
                    heatResult.cols(),
                    heatResult.rows(),
                    mpp,
                    palette,
                    graph.points(),
                    pointDisplayValues,
                    density
            );

            // Compute metrics & verdict for the session
            Inputs inputs = SessionMetrics.compute(
                    graph.session().evidenceGrade(),
                    heatResult,
                    null,
                    profile,
                    graph.toPerPoints()
            );

            mainHandler.post(() -> {
                if (cancelFlag.get() || binding == null) {
                    return;
                }
                if (pendingSpinnerRunnable != null) {
                    mainHandler.removeCallbacks(pendingSpinnerRunnable);
                }
                binding.progressLoading.setVisibility(View.GONE);

                binding.planView.setOverlayDrawer(overlay);
                binding.planView.invalidate();

                binding.textCells.setText(getString(R.string.text_cells_format, heatResult.inside()));

                updateVerdictChip(graph, profile, inputs);
            });
        });
    }

    private void updateVerdictChip(SessionGraph graph, ProfileSpec profile, Inputs inputs) {
        if (!graph.hasBoundary()) {
            binding.chipVerdict.setText(R.string.heatmap_no_boundary);
            return;
        }

        Tier tier = NetFieldApp.entitlements().current();
        PaywallGate.GateOutcome outcome = PaywallGate.evaluate(tier, Feature.VERDICT_PREVIEW);

        double covPct = (inputs.cellsInside() > 0)
                ? (inputs.cellsMeeting() * 100.0 / inputs.cellsInside())
                : 0.0;

        if (outcome != PaywallGate.GateOutcome.RUN) {
            // Free tier: "92.5% of the area ≥ −67 dBm", no verdict
            String text = String.format(Locale.US, "%.1f%% of the area ≥ %d dBm", covPct, profile.covDbm());
            binding.chipVerdict.setText(getString(R.string.free_summary_title) + " · " + text);
        } else {
            Result vResult = VerdictEngine.evaluate(profile, inputs);
            String verdictText;
            switch (vResult.verdict()) {
                case PASS:
                    verdictText = getString(R.string.verdict_pass);
                    break;
                case FAIL:
                    verdictText = getString(R.string.verdict_fail);
                    break;
                case INCOMPLETE:
                    verdictText = getString(R.string.verdict_incomplete);
                    break;
                case NOT_ELIGIBLE:
                default:
                    verdictText = getString(R.string.verdict_not_eligible);
                    break;
            }
            binding.chipVerdict.setText(String.format(Locale.US, "%s · %.1f%% ≥ %d dBm", verdictText, covPct, profile.covDbm()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSpinnerRunnable != null) {
            mainHandler.removeCallbacks(pendingSpinnerRunnable);
        }
        currentCancelFlag.set(true);
        binding = null;
    }
}
