package com.netfieldpro.data.repository;

import android.content.Context;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.session.RecoveryPlanner;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.WriteCallback;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.DeviceInfoRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.data.seed.ProfileSeeder;
import com.netfieldpro.session.SessionRuntimeStore;
import com.netfieldpro.time.AndroidClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SessionRepository {

    private final NetFieldDao dao;
    private final BatchedWriter batchedWriter;
    private final SessionRuntimeStore runtimeStore;
    private final QuotaGuard quotaGuard;
    private final Clock clock;

    public SessionRepository(
            NetFieldDao dao,
            BatchedWriter batchedWriter,
            SessionRuntimeStore runtimeStore,
            QuotaGuard quotaGuard,
            Clock clock
    ) {
        this.dao = Objects.requireNonNull(dao, "dao cannot be null");
        this.batchedWriter = batchedWriter;
        this.runtimeStore = Objects.requireNonNull(runtimeStore, "runtimeStore cannot be null");
        this.quotaGuard = quotaGuard;
        this.clock = clock != null ? clock : new AndroidClock();
    }

    public static SessionRepository create(Context context) {
        return create(context, new AndroidClock());
    }

    public static SessionRepository create(Context context, Clock clock) {
        NetFieldDatabase db = NetFieldDatabase.get(context);
        return new SessionRepository(
                db.netFieldDao(),
                new BatchedWriter(db),
                SessionRuntimeStore.create(context),
                NetFieldApp.quotaGuard(),
                clock
        );
    }

    public SurveySessionRow create(String floorId, String name) {
        if (quotaGuard != null) {
            QuotaDecision decision = quotaGuard.check(Resource.SESSION);
            if (!decision.allowed()) {
                throw new IllegalStateException("Session quota exceeded");
            }
        }

        String sessionId = IdGenerator.generate();
        long nowWall = clock.wallMs();
        long nowMono = clock.monoNs();

        SurveySessionRow row = new SurveySessionRow();
        row.id = sessionId;
        row.floorId = floorId != null ? floorId : "";
        row.name = (name != null && !name.isEmpty()) ? name : "Survey";
        row.state = "DRAFT";
        row.createdAtMs = nowWall;
        row.monoEpochNs = nowMono;
        row.schemaVersion = 1;
        row.appVersion = "1.0.0";
        row.algoVersion = "1.0.0";
        row.throttleAtStart = false;
        row.timingQuality = "GOOD";
        row.evidenceGrade = "VERDICT";
        row.sessionType = "STANDARD";
        row.intendedProfileId = "OFFICE_STANDARD";
        row.intendedProfileChanges = 0;
        row.rssiOffsetDb = 0.0;
        row.linkCadenceMs = 3000L;
        row.cadenceFlag = "DEFAULT";
        row.probeMethod = "NONE";
        row.referenceFreqsMhz = "[]";
        row.receiverId = "";
        row.deviceInfoId = "";
        row.capabilityReport = "{}";
        row.finalizedAtMs = null;
        row.evidenceSha256 = null;
        row.publicKeyFp = null;

        if (batchedWriter != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            batchedWriter.submit(db -> db.netFieldDao().insertSurveySession(row), new WriteCallback() {
                @Override
                public void onSuccess() {
                    future.complete(null);
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Fallback to direct DAO insert if writer times out or fails
                dao.insertSurveySession(row);
            }
        } else {
            dao.insertSurveySession(row);
        }

        runtimeStore.putEntry(sessionId, Sm1State.CREATED, nowMono, Collections.emptyList());
        return row;
    }

    public SurveySessionRow createFull(
            String floorId,
            String name,
            DeviceInfoRow deviceInfo,
            boolean throttleAtStart,
            String evidenceGrade,
            String intendedProfileId,
            String probeMethod,
            List<Integer> referenceFreqsMhz,
            String tierName,
            List<String> targetSsids
    ) {
        if (quotaGuard != null) {
            QuotaDecision decision = quotaGuard.check(Resource.SESSION);
            if (!decision.allowed()) {
                throw new IllegalStateException("Session quota exceeded");
            }
        }

        ProfileSeeder.seed(dao);

        String sessionId = IdGenerator.generate();
        long nowWall = clock.wallMs();
        long nowMono = clock.monoNs();

        if (deviceInfo == null) {
            deviceInfo = new DeviceInfoRow();
            deviceInfo.id = IdGenerator.generate();
            deviceInfo.model = android.os.Build.MODEL;
            deviceInfo.manufacturer = android.os.Build.MANUFACTURER;
            deviceInfo.sdk = android.os.Build.VERSION.SDK_INT;
            deviceInfo.wifiStandards = "802.11a/b/g/n/ac/ax";
            deviceInfo.linkCadenceMs = 3000L;
            deviceInfo.buildFingerprint = android.os.Build.FINGERPRINT;
        }

        SurveySessionRow row = new SurveySessionRow();
        row.id = sessionId;
        row.floorId = floorId != null ? floorId : "";
        row.name = (name != null && !name.isEmpty()) ? name : "Survey";
        row.state = "DRAFT";
        row.createdAtMs = nowWall;
        row.monoEpochNs = nowMono;
        row.schemaVersion = 1;
        row.appVersion = "1.0.0";
        row.algoVersion = "1.0.0";
        row.throttleAtStart = throttleAtStart;
        row.timingQuality = "GOOD";
        row.evidenceGrade = evidenceGrade != null ? evidenceGrade : "VERDICT";
        row.sessionType = "STANDARD";
        row.intendedProfileId = intendedProfileId != null ? intendedProfileId : "OFFICE_STANDARD";
        row.intendedProfileChanges = 0;
        row.rssiOffsetDb = 0.0;
        row.linkCadenceMs = 3000L;
        row.cadenceFlag = "DEFAULT";
        row.probeMethod = probeMethod != null ? probeMethod : "NONE";
        row.referenceFreqsMhz = referenceFreqsMhz != null ? referenceFreqsMhz.toString() : "[]";
        row.receiverId = "builtin-wifi";
        row.deviceInfoId = deviceInfo.id;
        row.capabilityReport = String.format("{\"tier\":\"%s\",\"sdk\":%d,\"throttleOff\":%b}",
                tierName != null ? tierName : "CALLBACK",
                android.os.Build.VERSION.SDK_INT,
                !throttleAtStart);
        row.finalizedAtMs = null;
        row.evidenceSha256 = null;
        row.publicKeyFp = null;

        List<TargetNetworkRow> targetRows = new ArrayList<>();
        if (targetSsids != null) {
            for (String ssid : targetSsids) {
                TargetNetworkRow tn = new TargetNetworkRow();
                tn.id = IdGenerator.generate();
                tn.sessionId = sessionId;
                tn.ssid = ssid;
                targetRows.add(tn);
            }
        }

        final DeviceInfoRow finalDeviceInfo = deviceInfo;
        if (batchedWriter != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            batchedWriter.submit(db -> {
                db.netFieldDao().insertDeviceInfo(finalDeviceInfo);
                db.netFieldDao().insertSurveySession(row);
                for (TargetNetworkRow tn : targetRows) {
                    db.netFieldDao().insertTargetNetwork(tn);
                }
            }, new WriteCallback() {
                @Override
                public void onSuccess() {
                    future.complete(null);
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                dao.insertDeviceInfo(finalDeviceInfo);
                dao.insertSurveySession(row);
                for (TargetNetworkRow tn : targetRows) {
                    dao.insertTargetNetwork(tn);
                }
            }
        } else {
            dao.insertDeviceInfo(finalDeviceInfo);
            dao.insertSurveySession(row);
            for (TargetNetworkRow tn : targetRows) {
                dao.insertTargetNetwork(tn);
            }
        }

        runtimeStore.putEntry(sessionId, Sm1State.READY, nowMono, Collections.emptyList());
        return row;
    }

    public void updateState(String id, String stateName) {
        Sm1State state;
        try {
            state = Sm1State.valueOf(stateName);
        } catch (Exception e) {
            state = Sm1State.ENDED;
        }
        runtimeStore.putState(id, state);
    }

    public void appendGap(String sessionId, String kind, long tNs) {
        runtimeStore.appendGap(sessionId, kind, tNs);
    }

    public int scanCount(String id) {
        return dao.getScansForSession(id).size();
    }

    public List<RecoveryPlanner.SessionFacts> listNonTerminal() {
        List<SurveySessionRow> drafts = dao.getDraftSessions();
        List<RecoveryPlanner.SessionFacts> result = new ArrayList<>();
        for (SurveySessionRow row : drafts) {
            if (runtimeStore.hasEntry(row.id)) {
                Sm1State state = runtimeStore.getState(row.id);
                if (state != Sm1State.ENDED) {
                    result.add(new RecoveryPlanner.SessionFacts(
                            row.id,
                            state.name(),
                            row.createdAtMs,
                            row.monoEpochNs,
                            scanCount(row.id)
                    ));
                }
            }
        }
        return result;
    }

    public void delete(String id) {
        dao.deleteSurveySession(id);
        runtimeStore.remove(id);
    }

    public void setEvidenceGrade(String id, String grade) {
        dao.updateSessionEvidenceGrade(id, grade);
    }

    public void deletePoint(String pointId) {
        dao.deleteSurveyPoint(pointId);
    }

    public SurveySessionRow getSurveySession(String id) {
        return dao.getSurveySession(id);
    }

    public SessionRuntimeStore getRuntimeStore() {
        return runtimeStore;
    }
}
