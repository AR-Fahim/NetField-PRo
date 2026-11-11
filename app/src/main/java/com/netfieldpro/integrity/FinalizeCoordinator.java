package com.netfieldpro.integrity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.netfieldpro.core.assess.PerPoint;
import com.netfieldpro.core.assess.SessionMetrics;
import com.netfieldpro.core.entitlement.Tier;
import com.netfieldpro.core.geom.PolygonValidator;
import com.netfieldpro.core.geom.Pt;
import com.netfieldpro.core.heat.HeatPipeline;
import com.netfieldpro.core.heat.Polygon;
import com.netfieldpro.core.heat.Sample;
import com.netfieldpro.core.heat.Vertex;
import com.netfieldpro.core.heat.WallChain;
import com.netfieldpro.core.integrity.Evidence;
import com.netfieldpro.core.integrity.QrPayload;
import com.netfieldpro.core.integrity.Signer;
import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.util.Base64Url;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.core.util.Sha256;
import com.netfieldpro.core.verdict.CritResult;
import com.netfieldpro.core.verdict.Inputs;
import com.netfieldpro.core.verdict.PresetProfiles;
import com.netfieldpro.core.verdict.ProfileSpec;
import com.netfieldpro.core.verdict.Result;
import com.netfieldpro.core.verdict.VerdictEngine;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorBoundaryRow;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.mapper.AssessmentMapper;
import com.netfieldpro.data.db.mapper.FloorBoundaryMapper;
import com.netfieldpro.data.repository.PlanFileStore;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.entitlement.EntitlementRepository;
import com.netfieldpro.ui.debug.DebugDataLabFragment;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinalizeCoordinator {

    public enum Step {
        PRECHECK("Checking session requirements…"),
        HASH_PARTS("Hashing evidence parts…"),
        ENSURE_KEY("Preparing signing key…"),
        MANIFEST("Creating evidence manifest…"),
        ASSESS("Computing assessment metrics…"),
        SIGN("Signing evidence…"),
        COMMIT("Finalizing session…");

        private final String defaultText;

        Step(String defaultText) {
            this.defaultText = defaultText;
        }

        public String defaultText() {
            return defaultText;
        }
    }

    public interface Listener {
        void onProgress(Step step, String message);
        void onSuccess(Assessment assessment);
        void onError(Throwable error);
    }

    private final Context context;
    private final NetFieldDatabase db;
    private final SurveyReadRepository surveyReadRepo;
    private final EntitlementRepository entitlementRepo;
    private final Clock clock;
    private final PlanFileStore planFileStore;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Signer signerOverride;

    public static FinalizeCoordinator create(Context context) {
        return new FinalizeCoordinator(
                context,
                com.netfieldpro.data.db.NetFieldDatabase.get(context),
                com.netfieldpro.data.repository.SurveyReadRepository.create(context),
                com.netfieldpro.NetFieldApp.entitlements(),
                new com.netfieldpro.time.AndroidClock(),
                com.netfieldpro.NetFieldApp.planFileStore(),
                null
        );
    }

    public FinalizeCoordinator(
            Context context,
            NetFieldDatabase db,
            SurveyReadRepository surveyReadRepo,
            EntitlementRepository entitlementRepo,
            Clock clock,
            PlanFileStore planFileStore,
            ExecutorService executor
    ) {
        this.context = context;
        this.db = db;
        this.surveyReadRepo = surveyReadRepo;
        this.entitlementRepo = entitlementRepo;
        this.clock = clock;
        this.planFileStore = planFileStore;
        this.executor = executor != null ? executor : Executors.newSingleThreadExecutor();
    }

    public void setSignerForTest(Signer signer) {
        this.signerOverride = signer;
    }

    public void finalizeSession(String sessionId, Listener listener) {
        executor.submit(() -> {
            try {
                Assessment assessment = finalizeSessionSync(sessionId, (step, text) -> {
                    if (listener != null) {
                        mainHandler.post(() -> listener.onProgress(step, text));
                    }
                });
                if (listener != null) {
                    mainHandler.post(() -> listener.onSuccess(assessment));
                }
            } catch (Throwable t) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onError(t));
                }
            }
        });
    }

    public interface ProgressCallback {
        void onProgress(Step step, String text);
    }

    public Assessment finalizeSessionSync(String sessionId, ProgressCallback progress) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("FinalizeCoordinator cannot run on the main thread");
        }

        NetFieldDao dao = db.netFieldDao();

        // 1. PRECHECK
        notifyStep(progress, Step.PRECHECK);
        SurveySessionRow sRow = dao.getSurveySession(sessionId);
        if (sRow == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        if (SessionState.FINALIZED.name().equals(sRow.state)) {
            throw new IllegalStateException("Session is already finalized: " + sessionId);
        }
        if (entitlementRepo.current() == Tier.FREE) {
            throw new IllegalStateException("Finalize is locked on Free tier");
        }
        List<SurveyPointRow> ptRows = dao.getSurveyPointsForSession(sessionId);
        if (ptRows.isEmpty()) {
            throw new IllegalStateException("Cannot finalize session with zero points");
        }
        FloorBoundaryRow bRow = dao.getFloorBoundary(sRow.floorId);
        if (bRow == null) {
            throw new IllegalStateException("Cannot finalize session without a boundary");
        }
        FloorBoundary boundary = FloorBoundaryMapper.toDomain(bRow);
        List<Pt> pts = new ArrayList<>();
        for (FloorBoundary.Vertex v : boundary.vertices()) {
            pts.add(new Pt(v.x(), v.y()));
        }
        PolygonValidator.Result valResult = PolygonValidator.validate(pts);
        if (valResult != PolygonValidator.Result.OK) {
            throw new IllegalStateException("Boundary is invalid: " + valResult);
        }

        // 2. HASH_PARTS
        notifyStep(progress, Step.HASH_PARTS);
        JsonObject coreExport = DebugDataLabFragment.forSession(dao, sessionId);
        byte[] coreBytes = CanonicalJsonWriter.toBytes(coreExport);
        String coreSha256 = Sha256.hex(Sha256.hash(coreBytes));

        List<RawSample> neighborSamples = DebugDataLabFragment.neighborSamplesForSession(dao, sessionId);
        byte[] neighborBytes = Evidence.neighborsJson(neighborSamples);
        String neighborSha256 = Sha256.hex(Sha256.hash(neighborBytes));

        String imageName = "floor.png";
        String imageSha256;
        File planFile = planFileStore != null ? planFileStore.findPlanFile(sRow.floorId) : null;
        if (planFile != null && planFile.exists()) {
            String lower = planFile.getName().toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                imageName = "floor.jpg";
            }
            imageSha256 = Sha256.hex(Sha256.hash(Files.readAllBytes(planFile.toPath())));
        } else {
            FloorRow fRow = dao.getFloor(sRow.floorId);
            imageSha256 = (fRow != null && fRow.planSha256 != null && !fRow.planSha256.isEmpty())
                    ? fRow.planSha256
                    : Sha256.hex(Sha256.hash(new byte[0]));
        }

        // 3. ENSURE_KEY
        notifyStep(progress, Step.ENSURE_KEY);
        Signer signer = (signerOverride != null) ? signerOverride : new AndroidSigner(context);
        byte[] compressedPk = signer.publicKeyCompressed();
        String pkB64Url = Base64Url.encode(compressedPk);

        // 4. MANIFEST
        notifyStep(progress, Step.MANIFEST);
        long finalizedAtMs = clock.wallMs();
        List<Evidence.Part> parts = List.of(
                new Evidence.Part("core.json", coreSha256),
                new Evidence.Part("neighbors.json", neighborSha256),
                new Evidence.Part(imageName, imageSha256)
        );
        byte[] manifestBytes = Evidence.partsManifest(finalizedAtMs, parts);
        String evidenceSha256 = Evidence.evidenceSha256(manifestBytes);

        // 5. ASSESS
        notifyStep(progress, Step.ASSESS);
        SessionGraph graph = surveyReadRepo.loadSessionGraph(sessionId);
        Polygon boundaryPoly = graph.toBoundaryPolygon();
        List<WallChain> wallChains = graph.toWallChains();
        List<Sample> samples = graph.samplesForCoverage();
        ProfileSpec profile = PresetProfiles.byId(graph.session().intendedProfileId());
        if (profile == null) {
            profile = PresetProfiles.OFFICE_STANDARD;
        }

        HeatPipeline.Request req = new HeatPipeline.Request(
                boundaryPoly,
                wallChains,
                graph.toWallPenalties(),
                samples,
                profile.maxInfluenceM(),
                () -> false
        );
        HeatPipeline.Result heatResult = HeatPipeline.compute(req, 0L);

        PerPoint[] perPoints = graph.toPerPoints();
        Inputs inputs = SessionMetrics.compute(
                graph.session().evidenceGrade(),
                heatResult,
                null,
                profile,
                perPoints
        );
        Result result = VerdictEngine.evaluate(profile, inputs);

        JsonObject profileSnapshot = PresetProfiles.thresholdsJson(profile);
        JsonObject thresholds = PresetProfiles.thresholdsJson(profile);
        JsonObject computedMetrics = new JsonObject();
        computedMetrics.put("cellsInside", new JsonInt(inputs.cellsInside()));
        computedMetrics.put("cellsMeeting", new JsonInt(inputs.cellsMeeting()));
        computedMetrics.put("cellsMasked", new JsonInt(inputs.cellsMasked()));
        computedMetrics.put("tapPins", new JsonInt(inputs.tapPins()));
        computedMetrics.put("tapPinsMeeting", new JsonInt(inputs.tapPinsMeeting()));
        if (inputs.secCellsMeeting() != null) {
            computedMetrics.put("secCellsMeeting", new JsonInt(inputs.secCellsMeeting()));
        }
        if (inputs.p95RttUs() != null) {
            computedMetrics.put("p95RttUs", new JsonInt(inputs.p95RttUs()));
        }
        if (inputs.meanJitterMilli() != null) {
            computedMetrics.put("meanJitterMilli", new JsonInt(inputs.meanJitterMilli()));
        }
        if (inputs.lostSum() != null) {
            computedMetrics.put("lostSum", new JsonInt(inputs.lostSum()));
        }
        if (inputs.sentSum() != null) {
            computedMetrics.put("sentSum", new JsonInt(inputs.sentSum()));
        }
        if (inputs.worstLost() != null) {
            computedMetrics.put("worstLost", new JsonInt(inputs.worstLost()));
        }
        if (inputs.worstSent() != null) {
            computedMetrics.put("worstSent", new JsonInt(inputs.worstSent()));
        }
        if (inputs.stickyCount() != null) {
            computedMetrics.put("stickyCount", new JsonInt(inputs.stickyCount()));
        }

        JsonArray criteriaArr = new JsonArray();
        for (CritResult cr : result.rows()) {
            JsonObject cObj = new JsonObject();
            cObj.put("id", new JsonString(cr.id().name()));
            cObj.put("status", new JsonString(cr.status().name()));
            cObj.put("c", new JsonInt(cr.c()));
            criteriaArr.add(cObj);
        }

        // 6. SIGN
        notifyStep(progress, Step.SIGN);
        String assessmentId = IdGenerator.generate();
        String algoVersion = (sRow.algoVersion != null && !sRow.algoVersion.isEmpty()) ? sRow.algoVersion : "1.0.0";
        double gridG = heatResult.g();

        // 86 chars dummy for assessmentBytes
        StringBuilder dummySig = new StringBuilder(86);
        for (int i = 0; i < 86; i++) dummySig.append('0');

        Assessment unsignedAssessment = new Assessment(
                assessmentId,
                evidenceSha256,
                PresetProfiles.idFor(profile.key()),
                profileSnapshot,
                sRow.intendedProfileId,
                false,
                thresholds,
                computedMetrics,
                criteriaArr,
                result.verdict(),
                finalizedAtMs,
                algoVersion,
                gridG,
                pkB64Url,
                dummySig.toString()
        );

        byte[] assessBytes = Evidence.assessmentBytes(unsignedAssessment);
        String ah = Evidence.assessmentHash(assessBytes);
        String vdLetter = QrPayload.verdictLetter(result.verdict());
        String pfLetter = QrPayload.profileLetter(profile.key());
        String ipLetter = QrPayload.profileLetter(sRow.intendedProfileId);
        long[] cArray = result.cArray();

        byte[] qrCoreBytes = QrPayload.buildCore(
                ah, cArray, evidenceSha256, ipLetter, pfLetter, pkB64Url, sessionId, finalizedAtMs, vdLetter
        );
        String sg = QrPayload.sign(qrCoreBytes, signer);

        Assessment finalAssessment = new Assessment(
                assessmentId,
                evidenceSha256,
                PresetProfiles.idFor(profile.key()),
                profileSnapshot,
                sRow.intendedProfileId,
                false,
                thresholds,
                computedMetrics,
                criteriaArr,
                result.verdict(),
                finalizedAtMs,
                algoVersion,
                gridG,
                pkB64Url,
                sg
        );

        // 7. COMMIT
        notifyStep(progress, Step.COMMIT);
        String publicKeyFp = Base64Url.encode(Sha256.hash(compressedPk)).substring(0, 16);

        db.runInTransaction(() -> {
            sRow.state = SessionState.FINALIZED.name();
            sRow.finalizedAtMs = finalizedAtMs;
            sRow.evidenceSha256 = evidenceSha256;
            sRow.publicKeyFp = publicKeyFp;
            dao.updateSurveySession(sRow);
            dao.insertAssessment(AssessmentMapper.toRow(finalAssessment, sessionId));
        });

        return finalAssessment;
    }

    private void notifyStep(ProgressCallback callback, Step step) {
        if (callback != null) {
            callback.onProgress(step, step.defaultText());
        }
    }
}
