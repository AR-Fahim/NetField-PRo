package com.netfieldpro.ui.assessment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.netfieldpro.R;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.verdict.CritId;
import com.netfieldpro.core.verdict.CritStatus;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.databinding.FragmentAssessmentBinding;
import com.netfieldpro.databinding.ItemCriterionRowBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssessmentFragment extends Fragment {

    private FragmentAssessmentBinding binding;
    private String assessmentId;
    private String sessionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAssessmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            assessmentId = getArguments().getString("assessmentId");
            sessionId = getArguments().getString("sessionId");
        }
        setupListeners();
        loadAssessmentData();
    }

    private void setupListeners() {
        binding.btnCreateReport.setOnClickListener(v -> {
            if (assessmentId != null) {
                NavController nc = Navigation.findNavController(v);
                Bundle args = new Bundle();
                args.putString("assessmentId", assessmentId);
                try {
                    nc.navigate(R.id.action_s15_to_s16, args);
                } catch (Exception e) {
                    try {
                        nc.navigate(R.id.nav_s16, args);
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        binding.btnViewHeatmap.setOnClickListener(v -> navigateToHeatmap());
    }

    private void navigateToHeatmap() {
        if (binding == null) return;
        NavController nc = Navigation.findNavController(binding.getRoot());
        if (!nc.popBackStack(R.id.nav_s09, false)) {
            if (sessionId != null) {
                Bundle args = new Bundle();
                args.putString("sessionId", sessionId);
                try {
                    nc.navigate(R.id.nav_s09, args);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void loadAssessmentData() {
        if (getContext() == null) return;
        NetFieldDao dao = NetFieldDatabase.get(requireContext()).netFieldDao();
        AssessmentRow aRow = null;
        if (assessmentId != null && !assessmentId.isEmpty()) {
            aRow = dao.getAssessment(assessmentId);
        }
        if (aRow == null && sessionId != null && !sessionId.isEmpty()) {
            aRow = dao.getAssessmentForSession(sessionId);
        }
        if (aRow == null) {
            return;
        }

        this.assessmentId = aRow.assessmentId;
        this.sessionId = aRow.sessionId;

        SurveySessionRow sessionRow = dao.getSurveySession(aRow.sessionId);
        displayAssessment(aRow, sessionRow);
    }

    private void displayAssessment(AssessmentRow aRow, @Nullable SurveySessionRow sRow) {
        if (binding == null) return;

        // Verdict Badge
        String verdictStr = aRow.verdict;
        if (Verdict.PASS.name().equals(verdictStr)) {
            binding.textVerdictBadge.setText(R.string.verdict_pass);
        } else if (Verdict.FAIL.name().equals(verdictStr)) {
            binding.textVerdictBadge.setText(R.string.verdict_fail);
        } else if (Verdict.INCOMPLETE.name().equals(verdictStr)) {
            binding.textVerdictBadge.setText(R.string.verdict_incomplete);
        } else if (Verdict.NOT_ELIGIBLE.name().equals(verdictStr)) {
            binding.textVerdictBadge.setText(R.string.verdict_not_eligible);
        } else {
            binding.textVerdictBadge.setText(verdictStr);
        }

        // Evidence Grade
        boolean isIndicative = sRow != null && EvidenceGrade.INDICATIVE.name().equals(sRow.evidenceGrade);
        if (isIndicative) {
            binding.textEvidenceGrade.setText(R.string.grade_indicative);
        } else {
            binding.textEvidenceGrade.setText(R.string.grade_verdict);
        }

        // Key fp and Evidence sha
        String keyFp = "";
        if (sRow != null && sRow.publicKeyFp != null && !sRow.publicKeyFp.isEmpty()) {
            keyFp = sRow.publicKeyFp;
        } else if (aRow.publicKey != null && !aRow.publicKey.isEmpty()) {
            keyFp = aRow.publicKey.length() >= 16 ? aRow.publicKey.substring(0, 16) : aRow.publicKey;
        }
        binding.textKeyFp.setText(getString(R.string.key_fp_format, keyFp));
        binding.textEvidenceSha.setText(getString(R.string.evidence_sha_format, aRow.evidenceSha256 != null ? aRow.evidenceSha256 : ""));

        // Criteria rows
        populateCriteriaList(aRow.criteria, aRow.thresholds);
    }

    private void populateCriteriaList(String criteriaJsonStr, String thresholdsJsonStr) {
        if (binding == null) return;
        binding.layoutCriteriaList.removeAllViews();

        JSONObject thresholds = new JSONObject();
        try {
            if (thresholdsJsonStr != null && !thresholdsJsonStr.isEmpty()) {
                thresholds = new JSONObject(thresholdsJsonStr);
            }
        } catch (Exception ignored) {
        }

        Map<CritId, CriterionData> critMap = new HashMap<>();
        try {
            if (criteriaJsonStr != null && !criteriaJsonStr.isEmpty()) {
                JSONArray arr = new JSONArray(criteriaJsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String idStr = obj.optString("id");
                    String statusStr = obj.optString("status");
                    long c = obj.optLong("c", -1L);
                    try {
                        CritId id = CritId.valueOf(idStr);
                        CritStatus status = CritStatus.valueOf(statusStr);
                        critMap.put(id, new CriterionData(status, c));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        for (CritId id : CritId.values()) {
            CriterionData cd = critMap.get(id);
            CritStatus status = cd != null ? cd.status : CritStatus.NOT_EVALUATED;
            long c = cd != null ? cd.c : -1L;

            ItemCriterionRowBinding rowBinding = ItemCriterionRowBinding.inflate(getLayoutInflater(), binding.layoutCriteriaList, false);

            rowBinding.textCritName.setText(formatCritName(id));
            rowBinding.textCritRequired.setText(formatRequired(id, thresholds));
            rowBinding.textCritMeasured.setText(formatMeasured(id, status, c));
            rowBinding.textCritStatus.setText(formatStatus(status));

            rowBinding.getRoot().setOnClickListener(v -> navigateToHeatmap());

            binding.layoutCriteriaList.addView(rowBinding.getRoot());
        }
    }

    private String formatCritName(CritId id) {
        switch (id) {
            case COVERAGE: return "Coverage";
            case SECONDARY: return "Secondary";
            case P95_RTT: return "p95 RTT";
            case MEAN_JITTER: return "Mean Jitter";
            case POOLED_LOSS: return "Pooled Loss";
            case WORST_POINT_LOSS: return "Worst Point Loss";
            case STICKY: return "Sticky Roams";
            case MASKED: return "Masked Area";
            default: return id.name();
        }
    }

    private String formatRequired(CritId id, JSONObject th) {
        switch (id) {
            case COVERAGE: {
                int covPct = th.optInt("covPct", 90);
                int covDbm = th.optInt("covDbm", -67);
                return "≥" + covPct + "% ≥" + covDbm;
            }
            case SECONDARY: {
                if (th.has("secPct") && th.has("secDbm")) {
                    return "≥" + th.optInt("secPct") + "% ≥" + th.optInt("secDbm");
                }
                return "–";
            }
            case P95_RTT: {
                int p95Ms = th.optInt("p95Ms", 50);
                return "≤" + p95Ms + " ms";
            }
            case MEAN_JITTER: {
                int jitMs = th.optInt("jitMs", 15);
                return "≤" + jitMs + " ms";
            }
            case POOLED_LOSS: {
                int lossPermille = th.optInt("lossPermille", 10);
                return String.format(Locale.US, "≤%.1f%%", lossPermille / 10.0);
            }
            case WORST_POINT_LOSS: {
                int worstLossPct = th.optInt("worstLossPct", 5);
                String op = "LT".equals(th.optString("worstLossOp")) ? "<" : "≤";
                return op + worstLossPct + "%";
            }
            case STICKY: {
                int sticky = th.optInt("sticky", 0);
                return "≤" + sticky;
            }
            case MASKED: {
                return "≤5%";
            }
            default:
                return "–";
        }
    }

    private String formatMeasured(CritId id, CritStatus status, long c) {
        if (status == CritStatus.NOT_EVALUATED || c < 0) {
            return "–";
        }
        double val = c / 10.0;
        switch (id) {
            case COVERAGE:
            case SECONDARY:
            case POOLED_LOSS:
            case WORST_POINT_LOSS:
            case MASKED:
                return String.format(Locale.US, "%.1f%%", val);
            case P95_RTT:
            case MEAN_JITTER:
                return String.format(Locale.US, "%.1f ms", val);
            case STICKY:
                return String.format(Locale.US, "%.1f", val);
            default:
                return String.format(Locale.US, "%.1f", val);
        }
    }

    private int formatStatus(CritStatus status) {
        switch (status) {
            case MET: return R.string.crit_status_met;
            case NOT_MET: return R.string.crit_status_not_met;
            case NOT_REQUIRED: return R.string.crit_status_not_required;
            case NOT_EVALUATED:
            default:
                return R.string.crit_status_not_evaluated;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class CriterionData {
        final CritStatus status;
        final long c;

        CriterionData(CritStatus status, long c) {
            this.status = status;
            this.c = c;
        }
    }
}
