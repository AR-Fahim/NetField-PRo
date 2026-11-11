package com.netfieldpro.report;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.netfieldpro.BuildConfig;
import com.netfieldpro.R;
import com.netfieldpro.core.heat.HeatPipeline;
import com.netfieldpro.core.heat.Palette;
import com.netfieldpro.core.heat.PaletteId;
import com.netfieldpro.core.heat.Polygon;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.heat.WallChain;
import com.netfieldpro.core.integrity.Evidence;
import com.netfieldpro.core.integrity.QrPayload;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.core.util.Sha256;
import com.netfieldpro.core.verdict.CritId;
import com.netfieldpro.core.verdict.CritResult;
import com.netfieldpro.core.verdict.CritStatus;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.data.repository.SurveyReadRepository.PointRecord;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.ui.heatmap.HeatOverlay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReportBuilder {

    private static final int MAX_PLAN_DIMENSION = 1600;

    private ReportBuilder() {
    }

    public static ReportModel build(
            @NonNull Context context,
            @NonNull SessionGraph graph,
            @Nullable Assessment assessment,
            @NonNull HeatPalette paletteSetting,
            @Nullable File planFile
    ) {
        boolean isSigned = (assessment != null && graph.session() != null && graph.session().state() == SessionState.FINALIZED);
        int totalPages = isSigned ? 4 : 2;

        String projectName = (graph.project() != null && graph.project().name() != null) ? graph.project().name() : "";
        String siteName = (graph.project() != null && graph.project().site() != null) ? graph.project().site() : "";
        String floorName = (graph.floor() != null) ? ("Level " + graph.floor().levelIndex()) : "";
        String sessionId = (graph.session() != null) ? graph.session().id() : "";
        String shortSessionId = (sessionId.length() > 8) ? sessionId.substring(0, 8) : sessionId;

        long dateMs = (isSigned && assessment.assessedAtMs() > 0)
                ? assessment.assessedAtMs()
                : ((graph.session() != null && graph.session().createdAtMs() > 0) ? graph.session().createdAtMs() : 0L);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String formattedDate = context.getString(R.string.date_label, dateFormat.format(dateMs));

        String reportTitle = isSigned
                ? context.getString(R.string.report_title)
                : context.getString(R.string.free_summary_title);

        Verdict verdict = isSigned ? assessment.verdict() : null;
        String verdictLabel = null;
        if (verdict != null) {
            switch (verdict) {
                case PASS:
                    verdictLabel = "✓ " + context.getString(R.string.verdict_pass);
                    break;
                case FAIL:
                    verdictLabel = "✕ " + context.getString(R.string.verdict_fail);
                    break;
                case INCOMPLETE:
                    verdictLabel = "◐ " + context.getString(R.string.verdict_incomplete);
                    break;
                case NOT_ELIGIBLE:
                    verdictLabel = "⊘ " + context.getString(R.string.verdict_not_eligible);
                    break;
            }
        }

        boolean isIndicative = graph.session() != null && graph.session().evidenceGrade() == EvidenceGrade.INDICATIVE;
        String gradeLabel = context.getString(isIndicative ? R.string.grade_indicative : R.string.grade_verdict);

        // Point counts
        int pointsCount = 0;
        int lowConfidenceCount = 0;
        int movedCount = 0;
        if (graph.points() != null) {
            pointsCount = graph.points().size();
            for (PointRecord pr : graph.points()) {
                SurveyPoint pt = pr.point();
                if (pt.quality() == PointQuality.LOW_CONFIDENCE) {
                    lowConfidenceCount++;
                } else if (pt.quality() == PointQuality.MOVED) {
                    movedCount++;
                }
            }
        }

        // Palette
        PaletteId paletteId = (paletteSetting == HeatPalette.VIRIDIS) ? PaletteId.VIRIDIS : PaletteId.TRAFFIC_LIGHT;
        String paletteName = (paletteId == PaletteId.VIRIDIS) ? "Viridis" : "Traffic Light";
        String interpNote = context.getString(R.string.interp_note);

        List<ReportModel.LegendEntry> legendEntries = buildLegendEntries(paletteId);

        // Build Criteria rows
        List<ReportModel.CriterionRow> criteriaRows = new ArrayList<>();
        if (isSigned) {
            criteriaRows = buildCriteriaRows(context, assessment);
        }

        // Composite Plan Bitmap (plan + heat overlay)
        Bitmap compositePlanBitmap = buildCompositeBitmap(graph, paletteId, planFile);

        // Integrity fields
        String qrFullJson = null;
        String evidenceHash = null;
        String assessmentHash = null;
        String signature = null;
        String publicKeyFp = null;
        String verificationInstructions = null;
        String appVersion = (graph.session() != null && graph.session().appVersion() != null) ? graph.session().appVersion() : "1.0";
        String algoVersion = (isSigned && assessment.algoVersion() != null) ? assessment.algoVersion() : "1.0.0";
        String debugNote = null;
        String watermarkText = null;

        if (isSigned) {
            evidenceHash = assessment.evidenceSha256();
            signature = assessment.sig();
            publicKeyFp = (graph.session() != null && graph.session().publicKeyFp() != null && !graph.session().publicKeyFp().isEmpty())
                    ? graph.session().publicKeyFp()
                    : extractPublicKeyFp(assessment.publicKey());

            byte[] assessBytes = Evidence.assessmentBytes(assessment);
            assessmentHash = Evidence.assessmentHash(assessBytes);

            long[] cArray = extractCArray(assessment.criteria());
            String ipLetter = QrPayload.profileLetter(assessment.intendedProfileId());
            String pfLetter = QrPayload.profileLetter(assessment.profileId());
            String vdLetter = QrPayload.verdictLetter(assessment.verdict());

            byte[] core = QrPayload.buildCore(
                    assessmentHash,
                    cArray,
                    evidenceHash,
                    ipLetter,
                    pfLetter,
                    assessment.publicKey(),
                    sessionId,
                    assessment.assessedAtMs(),
                    vdLetter
            );
            qrFullJson = QrPayload.fullJson(core, signature);

            verificationInstructions = "Verification Instructions:\n"
                    + "1. Verify evidence manifest:\n"
                    + "   sha256sum -c manifest.sha256\n"
                    + "2. Verify ECDSA signature against public key:\n"
                    + "   openssl dgst -sha256 -verify pubkey.pem -signature sig.raw manifest.json";

            if (BuildConfig.DEBUG) {
                debugNote = "Prototype build — not for contractual use";
            }
        } else {
            watermarkText = context.getString(R.string.watermark);
        }

        return new ReportModel(
                isSigned,
                totalPages,
                reportTitle,
                projectName,
                siteName,
                floorName,
                sessionId,
                shortSessionId,
                formattedDate,
                verdict,
                verdictLabel,
                gradeLabel,
                pointsCount,
                lowConfidenceCount,
                movedCount,
                criteriaRows,
                compositePlanBitmap,
                paletteName,
                interpNote,
                legendEntries,
                qrFullJson,
                evidenceHash,
                assessmentHash,
                signature,
                publicKeyFp,
                appVersion,
                algoVersion,
                verificationInstructions,
                debugNote,
                watermarkText
        );
    }

    private static String extractPublicKeyFp(String publicKey) {
        if (publicKey == null || publicKey.isEmpty()) {
            return "";
        }
        try {
            byte[] raw = Base64Url.decode(publicKey);
            byte[] hash = Sha256.hash(raw);
            String b64 = Base64Url.encode(hash);
            return (b64.length() >= 16) ? b64.substring(0, 16) : b64;
        } catch (Exception e) {
            return (publicKey.length() >= 16) ? publicKey.substring(0, 16) : publicKey;
        }
    }

    private static long[] extractCArray(JsonArray criteriaJsonArr) {
        Map<CritId, Long> cMap = new HashMap<>();
        if (criteriaJsonArr != null) {
            for (JsonValue v : criteriaJsonArr.elements()) {
                if (v instanceof JsonObject obj) {
                    JsonValue idVal = obj.get("id");
                    JsonValue cVal = obj.get("c");
                    if (idVal instanceof JsonString idStr && cVal instanceof JsonInt cInt) {
                        try {
                            CritId id = CritId.valueOf(idStr.value());
                            cMap.put(id, cInt.value());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        CritId[] values = CritId.values();
        long[] c = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            Long val = cMap.get(values[i]);
            c[i] = (val != null) ? val : -1L;
        }
        return c;
    }

    private static List<ReportModel.CriterionRow> buildCriteriaRows(Context context, Assessment assessment) {
        List<ReportModel.CriterionRow> list = new ArrayList<>();
        Map<CritId, CritResult> map = new HashMap<>();

        if (assessment.criteria() != null) {
            for (JsonValue v : assessment.criteria().elements()) {
                if (v instanceof JsonObject obj) {
                    JsonValue idVal = obj.get("id");
                    JsonValue statusVal = obj.get("status");
                    JsonValue cVal = obj.get("c");
                    if (idVal instanceof JsonString idStr && statusVal instanceof JsonString stStr && cVal instanceof JsonInt cInt) {
                        try {
                            CritId id = CritId.valueOf(idStr.value());
                            CritStatus status = CritStatus.valueOf(stStr.value());
                            map.put(id, new CritResult(id, status, cInt.value()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        JSONObject th = new JSONObject();
        try {
            if (assessment.thresholds() != null) {
                th = new JSONObject(assessment.thresholds().toString());
            }
        } catch (Exception ignored) {
        }

        for (CritId id : CritId.values()) {
            CritResult cr = map.get(id);
            CritStatus status = (cr != null) ? cr.status() : CritStatus.NOT_EVALUATED;
            long c = (cr != null) ? cr.c() : -1L;

            String name = formatCritName(id);
            String required = formatRequired(id, th);
            String measured = formatMeasured(id, status, c);
            String statusText = formatStatusText(context, status);
            boolean isMet = (status == CritStatus.MET);

            list.add(new ReportModel.CriterionRow(id, name, required, measured, statusText, isMet));
        }

        return list;
    }

    private static String formatCritName(CritId id) {
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

    private static String formatRequired(CritId id, JSONObject th) {
        switch (id) {
            case COVERAGE: {
                int covPct = th.optInt("covPct", 90);
                int covDbm = th.optInt("covDbm", -67);
                return "≥" + covPct + "% ≥" + covDbm + " dBm";
            }
            case SECONDARY: {
                if (th.has("secPct") && th.has("secDbm")) {
                    return "≥" + th.optInt("secPct") + "% ≥" + th.optInt("secDbm") + " dBm";
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
                int sticky = th.optInt("sticky", -1);
                return (sticky >= 0) ? "≤" + sticky : "–";
            }
            case MASKED: {
                return "≤5%";
            }
            default:
                return "–";
        }
    }

    private static String formatMeasured(CritId id, CritStatus status, long c) {
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
                return String.format(Locale.US, "%.0f", val);
            default:
                return String.format(Locale.US, "%.1f", val);
        }
    }

    private static String formatStatusText(Context context, CritStatus status) {
        switch (status) {
            case MET:
                return "✓ " + context.getString(R.string.crit_status_met);
            case NOT_MET:
                return "✕ " + context.getString(R.string.crit_status_not_met);
            case NOT_REQUIRED:
                return "– " + context.getString(R.string.crit_status_not_required);
            case NOT_EVALUATED:
            default:
                return "◐ " + context.getString(R.string.crit_status_not_evaluated);
        }
    }

    private static List<ReportModel.LegendEntry> buildLegendEntries(PaletteId paletteId) {
        List<ReportModel.LegendEntry> list = new ArrayList<>();
        double[] dbmLevels = new double[]{-50.0, -60.0, -70.0, -80.0, -90.0};
        String[] labels = new String[]{"≥−55 dBm", "≥−67 dBm", "≥−75 dBm", "≥−85 dBm", "<−85 dBm"};

        for (int i = 0; i < dbmLevels.length; i++) {
            int[] rgb = Palette.rgb(paletteId, dbmLevels[i]);
            int col = Color.rgb(rgb[0], rgb[1], rgb[2]);
            list.add(new ReportModel.LegendEntry(labels[i], col));
        }
        return list;
    }

    private static Bitmap buildCompositeBitmap(SessionGraph graph, PaletteId paletteId, @Nullable File planFile) {
        Bitmap basePlan = null;
        if (planFile != null && planFile.exists()) {
            basePlan = BitmapFactory.decodeFile(planFile.getAbsolutePath());
        }

        int origW = (basePlan != null) ? basePlan.getWidth() : ((graph.floor() != null) ? graph.floor().planWpx() : 1600);
        int origH = (basePlan != null) ? basePlan.getHeight() : ((graph.floor() != null) ? graph.floor().planHpx() : 1000);
        if (origW <= 0) origW = 1600;
        if (origH <= 0) origH = 1000;

        float scale = 1.0f;
        int maxDim = Math.max(origW, origH);
        if (maxDim > MAX_PLAN_DIMENSION) {
            scale = (float) MAX_PLAN_DIMENSION / maxDim;
        }

        int targetW = Math.max(1, Math.round(origW * scale));
        int targetH = Math.max(1, Math.round(origH * scale));

        Bitmap composite = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composite);
        canvas.drawColor(Color.WHITE);

        if (basePlan != null) {
            Rect src = new Rect(0, 0, basePlan.getWidth(), basePlan.getHeight());
            Rect dst = new Rect(0, 0, targetW, targetH);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(basePlan, src, dst, p);
        } else {
            // Draw floor boundary outline if plan file is missing
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2f);
            borderPaint.setColor(Color.LTGRAY);
            canvas.drawRect(2f, 2f, targetW - 2f, targetH - 2f, borderPaint);
        }

        double mpp = (graph.floor() != null && graph.floor().metersPerPixel() > 0)
                ? graph.floor().metersPerPixel() / scale
                : 0.025 / scale;

        Polygon boundary = graph.toBoundaryPolygon();
        List<WallChain> wallChains = graph.toWallChains();
        List<Sample> samples = graph.samplesForCoverage();

        ProfileSpec resolvedProf = (graph.session() != null && graph.session().intendedProfileId() != null)
                ? PresetProfiles.byId(graph.session().intendedProfileId())
                : PresetProfiles.OFFICE_STANDARD;
        if (resolvedProf == null) {
            resolvedProf = PresetProfiles.OFFICE_STANDARD;
        }
        double rEffMax = resolvedProf.maxInfluenceM();

        HeatPipeline.Request req = new HeatPipeline.Request(
                boundary,
                wallChains,
                graph.toWallPenalties(),
                samples,
                rEffMax,
                () -> false
        );

        HeatPipeline.Result heatResult = HeatPipeline.compute(req, 0L);
        if (heatResult != null) {
            Bitmap heatBitmap = HeatOverlay.createHeatBitmap(
                    heatResult.surface(),
                    heatResult.cols(),
                    heatResult.rows(),
                    paletteId,
                    false
            );

            if (heatBitmap != null && mpp > 0.0) {
                float left = (float) (boundary.minX() / mpp);
                float top = (float) (boundary.minY() / mpp);
                float right = (float) ((boundary.minX() + heatResult.cols() * heatResult.g()) / mpp);
                float bottom = (float) ((boundary.minY() + heatResult.rows() * heatResult.g()) / mpp);
                Rect src = new Rect(0, 0, heatResult.cols(), heatResult.rows());
                RectF dst = new RectF(left, top, right, bottom);
                Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(heatBitmap, src, dst, bp);
            }
        }

        // Draw Pins
        if (graph.points() != null && mpp > 0.0) {
            Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pinPaint.setStyle(Paint.Style.FILL);

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(10f);
            textPaint.setFakeBoldText(true);

            List<PointRecord> points = graph.points();
            for (int i = 0; i < points.size(); i++) {
                PointRecord pr = points.get(i);
                float px = (float) (pr.point().x() / mpp);
                float py = (float) (pr.point().y() / mpp);

                double val = -95.0;
                if (samples != null && i < samples.size()) {
                    val = samples.get(i).dbm();
                }

                int[] rgb = Palette.rgb(paletteId, val);
                pinPaint.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
                canvas.drawCircle(px, py, 12f, pinPaint);

                textPaint.setColor(Palette.textOn(rgb));
                String label = String.valueOf((int) Math.round(val));
                float textHeight = textPaint.descent() - textPaint.ascent();
                float textOffset = (textHeight / 2) - textPaint.descent();
                canvas.drawText(label, px, py + textOffset, textPaint);
            }
        }

        return composite;
    }
}
