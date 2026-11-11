package com.netfieldpro.ui.finalize;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.assess.SessionMetrics;
import com.netfieldpro.core.entitlement.Feature;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.geom.PolygonValidator;
import com.netfieldpro.core.geom.Pt;
import com.netfieldpro.core.heat.HeatPipeline;
import com.netfieldpro.core.heat.Polygon;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.heat.Vertex;
import com.netfieldpro.core.heat.WallChain;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.verdict.Inputs;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.core.verdict.Result;
import com.netfieldpro.core.verdict.VerdictEngine;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.mapper.FloorBoundaryMapper;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.databinding.FragmentFinalizeBinding;
import com.netfieldpro.integrity.FinalizeCoordinator;
import com.netfieldpro.paywall.PaywallGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinalizeFragment extends Fragment {

    private static final long HOLD_DURATION_MS = 1500L;

    private FragmentFinalizeBinding binding;
    private String sessionId;
    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isHolding = false;
    private boolean isFinalizing = false;

    private final Runnable finalizeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isHolding || isFinalizing) return;
            isHolding = false;
            isFinalizing = true;
            if (binding != null) {
                performConfirmHaptic(binding.btnHoldFinalize);
                executeFinalize();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFinalizeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId");
        }
        setupViews();
        loadSessionData();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        binding.btnHoldFinalize.setOnTouchListener((v, event) -> {
            if (isFinalizing) return false;

            Tier tier = NetFieldApp.entitlements().current();
            if (tier == Tier.FREE) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    PaywallGate.require(this, Feature.FINALIZE_SIGN, () -> {});
                }
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    binding.btnHoldFinalize.setText("Holding…");
                    holdHandler.postDelayed(finalizeRunnable, HOLD_DURATION_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isHolding) {
                        isHolding = false;
                        holdHandler.removeCallbacks(finalizeRunnable);
                        binding.btnHoldFinalize.setText(R.string.hold_to_finalize);
                        performRejectHaptic(v);
                    }
                    return true;
            }
            return false;
        });
    }

    private void loadSessionData() {
        if (sessionId == null) return;

        executor.submit(() -> {
            NetFieldDao dao = NetFieldDatabase.get(requireContext()).netFieldDao();
            SurveySessionRow sessionRow = dao.getSurveySession(sessionId);
            if (sessionRow == null) return;

            if (SessionState.FINALIZED.name().equals(sessionRow.state)) {
                AssessmentRow aRow = dao.getAssessmentByEvidence(sessionRow.evidenceSha256 != null ? sessionRow.evidenceSha256 : "");
                if (aRow != null) {
                    navigateToAssessment(aRow.assessmentId);
                    return;
                }
            }

            FloorRow floorRow = dao.getFloor(sessionRow.floorId);
            FloorBoundaryRow bRow = dao.getFloorBoundary(sessionRow.floorId);
            List<SurveyPointRow> pointRows = dao.getSurveyPointsForSession(sessionId);

            boolean hasBoundary = false;
            if (bRow != null) {
                FloorBoundary boundary = FloorBoundaryMapper.toDomain(bRow);
                List<Pt> pts = new ArrayList<>();
                for (FloorBoundary.Vertex v : boundary.vertices()) {
                    pts.add(new Pt(v.x(), v.y()));
                }
                hasBoundary = (PolygonValidator.validate(pts) == PolygonValidator.Result.OK);
            }

            int pointCount = pointRows.size();
            int lowConfidenceOrMoved = 0;
            for (SurveyPointRow pr : pointRows) {
                if (PointQuality.LOW_CONFIDENCE.name().equals(pr.quality) || PointQuality.MOVED.name().equals(pr.quality)) {
                    lowConfidenceOrMoved++;
                }
            }

            Tier tier = NetFieldApp.entitlements().current();
            boolean isFree = (tier == Tier.FREE);

            ProfileSpec profile = PresetProfiles.byId(sessionRow.intendedProfileId);
            if (profile == null) {
                profile = PresetProfiles.OFFICE_STANDARD;
            }

            String verdictPreview = null;
            boolean maskedOver5 = false;
            if (hasBoundary && pointCount >= 3) {
                try {
                    SurveyReadRepository repo = NetFieldApp.surveyReadRepository();
                    SessionGraph graph = repo.loadSessionGraph(sessionId);
                    Polygon boundaryPoly = graph.toBoundaryPolygon();
                    List<WallChain> wallChains = graph.toWallChains();
                    List<Sample> samples = graph.samplesForCoverage();
                    HeatPipeline.Request req = new HeatPipeline.Request(
                            boundaryPoly, wallChains, graph.toWallPenalties(), samples, profile.maxInfluenceM(), () -> false
                    );
                    HeatPipeline.Result heatResult = HeatPipeline.compute(req, 0L);
                    Inputs inputs = SessionMetrics.compute(graph.session().evidenceGrade(), heatResult, null, profile, graph.toPerPoints());
                    Result res = VerdictEngine.evaluate(profile, inputs);
                    verdictPreview = res.verdict().name();
                    if (inputs.cellsInside() > 0 && (inputs.cellsMasked() * 100 > 5 * inputs.cellsInside())) {
                        maskedOver5 = true;
                    }
                } catch (Exception ignored) {
                }
            }

            boolean canFinalize = hasBoundary && pointCount > 0;
            boolean finalHasBoundary = hasBoundary;
            int finalLowConf = lowConfidenceOrMoved;
            boolean finalMasked = maskedOver5;
            String finalVerdict = verdictPreview;
            ProfileSpec finalProfile = profile;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateUi(sessionRow, floorRow, finalHasBoundary, pointCount, isFree, tier, finalLowConf, finalMasked, finalVerdict, finalProfile, canFinalize));
            }
        });
    }

    private void updateUi(
            SurveySessionRow sessionRow,
            FloorRow floorRow,
            boolean hasBoundary,
            int pointCount,
            boolean isFree,
            Tier tier,
            int lowConfCount,
            boolean maskedOver5,
            String verdictPreview,
            ProfileSpec profile,
            boolean canFinalize
    ) {
        if (binding == null) return;

        String floorName = (floorRow != null) ? "Level " + floorRow.levelIndex : "Floor";
        binding.textFinalizeHeader.setText(getString(R.string.header_finalize_format, floorName, pointCount, profile.key()));

        if (hasBoundary) {
            binding.textCheckBoundary.setText(R.string.check_boundary_ok);
        } else {
            binding.textCheckBoundary.setText(R.string.check_boundary_missing);
        }

        if (pointCount > 0) {
            binding.textCheckPoints.setText(getString(R.string.check_points_format, pointCount));
        } else {
            binding.textCheckPoints.setText(R.string.check_points_missing);
        }

        if (isFree) {
            binding.textCheckTier.setText(R.string.check_tier_free);
        } else {
            binding.textCheckTier.setText(getString(R.string.check_tier_ok, tier.name()));
        }

        if (lowConfCount > 0) {
            binding.textWarningPins.setVisibility(View.VISIBLE);
            binding.textWarningPins.setText(getString(R.string.warning_pins_format, lowConfCount));
        } else {
            binding.textWarningPins.setVisibility(View.GONE);
        }

        binding.textWarningMasked.setVisibility(maskedOver5 ? View.VISIBLE : View.GONE);
        binding.textWarningGrade.setVisibility(EvidenceGrade.INDICATIVE.name().equals(sessionRow.evidenceGrade) ? View.VISIBLE : View.GONE);

        if (verdictPreview != null) {
            binding.textVerdictPreview.setText(getString(R.string.verdict_preview_format, verdictPreview));
        } else {
            binding.textVerdictPreview.setText(R.string.verdict_placeholder);
        }

        if (isFree) {
            binding.btnHoldFinalize.setEnabled(true);
            binding.btnHoldFinalize.setText(R.string.badge_pro);
        } else {
            binding.btnHoldFinalize.setEnabled(canFinalize);
            binding.btnHoldFinalize.setText(R.string.hold_to_finalize);
        }
    }

    private void executeFinalize() {
        if (binding == null) return;

        binding.progressFinalize.setVisibility(View.VISIBLE);
        binding.btnHoldFinalize.setEnabled(false);

        FinalizeCoordinator coordinator = NetFieldApp.finalizeCoordinator();
        coordinator.finalizeSession(sessionId, new FinalizeCoordinator.Listener() {
            @Override
            public void onProgress(FinalizeCoordinator.Step step, String message) {
                if (binding != null) {
                    binding.textFinalizeStatus.setText(message);
                }
            }

            @Override
            public void onSuccess(Assessment assessment) {
                if (binding != null) {
                    binding.progressFinalize.setVisibility(View.GONE);
                    performConfirmHaptic(binding.getRoot());
                    navigateToAssessment(assessment.assessmentId());
                }
            }

            @Override
            public void onError(Throwable error) {
                if (binding != null) {
                    binding.progressFinalize.setVisibility(View.GONE);
                    binding.btnHoldFinalize.setEnabled(true);
                    binding.btnHoldFinalize.setText(R.string.hold_to_finalize);
                    binding.textFinalizeStatus.setText("");
                    isFinalizing = false;

                    if (getContext() != null) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.finalize_error_dialog_title)
                                .setMessage(R.string.finalize_error_dialog_msg)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                }
            }
        });
    }

    private void navigateToAssessment(String assessmentId) {
        if (binding == null) return;
        NavController nc = Navigation.findNavController(binding.getRoot());
        Bundle args = new Bundle();
        args.putString("assessmentId", assessmentId);
        try {
            nc.navigate(R.id.action_s13_to_s15, args);
        } catch (Exception e) {
            try {
                nc.navigate(R.id.nav_s15, args);
            } catch (Exception ignored) {
            }
        }
    }

    private void performRejectHaptic(View v) {
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            v.performHapticFeedback(HapticFeedbackConstants.REJECT);
        } else {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void performConfirmHaptic(View v) {
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } else {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        holdHandler.removeCallbacks(finalizeRunnable);
        binding = null;
    }
}
