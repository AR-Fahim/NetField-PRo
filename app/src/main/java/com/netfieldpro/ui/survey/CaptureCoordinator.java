package com.netfieldpro.ui.survey;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.netfieldpro.core.capture.CaptureEngine;
import com.netfieldpro.core.capture.CaptureEvent;
import com.netfieldpro.core.heat.Median;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.sensor.MotionVariance;
import com.netfieldpro.core.sensor.SensorSource;
import com.netfieldpro.core.session.SessionEvent;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.core.wifi.Freshness;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.model.enums.SweepClass;
import com.netfieldpro.core.wifi.SweepClassifier;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.core.wifi.WifiScanSource;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.WriteCallback;
import com.netfieldpro.data.db.entity.LatencyResultRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.RawSampleRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.latency.LatencyRunner;
import com.netfieldpro.session.SessionExecutor;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CaptureCoordinator {

    public interface TimeoutScheduler {
        void schedule(Runnable runnable, long delayMs);
        void cancel();
    }

    public static class HandlerTimeoutScheduler implements TimeoutScheduler {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable pending;

        @Override
        public void schedule(Runnable runnable, long delayMs) {
            cancel();
            pending = runnable;
            handler.postDelayed(runnable, delayMs);
        }

        @Override
        public void cancel() {
            if (pending != null) {
                handler.removeCallbacks(pending);
                pending = null;
            }
        }
    }

    public interface Listener {
        void onPinPosition(double x, double y);
        void onCaptureStep(String stepDescription, int currentScan, int totalScans);
        void onCaptureCommitted(String pointId, String quality);
        void onCaptureDiscarded(String reason);
        void onError(String message);
    }

    private static class ClassifiedBatch {
        final ScanBatch batch;
        final SweepClass sweepClass;

        ClassifiedBatch(ScanBatch batch, SweepClass sweepClass) {
            this.batch = batch;
            this.sweepClass = sweepClass;
        }
    }

    private final String sessionId;
    private final double metersPerPixel;
    private final List<Integer> referenceFreqs;
    private final Set<String> targetSsids;
    private final String probeMethod;

    private final SessionExecutor sessionExecutor;
    private final WifiScanSource wifiScanSource;
    private final SensorSource sensorSource;
    private final LatencyRunner latencyRunner;
    private final WifiEnvironmentProbe probe;
    private final BatchedWriter batchedWriter;
    private final NetFieldDatabase db;
    private final Clock clock;
    private final TimeoutScheduler timeoutScheduler;

    private Listener listener;
    private CaptureEngine engine;
    private final MotionVariance motionVariance = new MotionVariance();
    private final List<ClassifiedBatch> capturedBatches = new ArrayList<>();
    private LatencyResultRow pendingLatencyResult;

    private boolean capturing = false;
    private long currentTapMonoNs = 0L;
    private long currentTapWallMs = 0L;
    private String currentPointId;
    private double currentPinX;
    private double currentPinY;

    public CaptureCoordinator(
            String sessionId,
            double metersPerPixel,
            List<Integer> referenceFreqs,
            Set<String> targetSsids,
            String probeMethod,
            SessionExecutor sessionExecutor,
            WifiScanSource wifiScanSource,
            SensorSource sensorSource,
            LatencyRunner latencyRunner,
            WifiEnvironmentProbe probe,
            BatchedWriter batchedWriter,
            NetFieldDatabase db,
            Clock clock,
            TimeoutScheduler timeoutScheduler
    ) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.metersPerPixel = metersPerPixel;
        this.referenceFreqs = referenceFreqs != null ? new ArrayList<>(referenceFreqs) : Collections.emptyList();
        this.targetSsids = targetSsids != null ? new HashSet<>(targetSsids) : Collections.emptySet();
        this.probeMethod = probeMethod != null ? probeMethod : "NONE";
        this.sessionExecutor = sessionExecutor;
        this.wifiScanSource = wifiScanSource;
        this.sensorSource = sensorSource;
        this.latencyRunner = latencyRunner;
        this.probe = probe;
        this.batchedWriter = batchedWriter;
        this.db = db;
        this.clock = clock;
        this.timeoutScheduler = timeoutScheduler;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isCapturing() {
        return capturing;
    }

    public double getCurrentPinX() {
        return currentPinX;
    }

    public double getCurrentPinY() {
        return currentPinY;
    }

    public void onSurveyOpened() {
        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.SURVEY_OPENED, clock.monoNs()));
        }
    }

    public void startCapture(double px, double py) {
        if (capturing) {
            return;
        }
        capturing = true;
        currentPinX = px;
        currentPinY = py;
        currentPointId = IdGenerator.generate();
        currentTapMonoNs = clock.monoNs();
        currentTapWallMs = clock.wallMs();
        capturedBatches.clear();
        pendingLatencyResult = null;
        motionVariance.reset();

        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.ACTIVITY, currentTapMonoNs));
        }

        boolean probeEnabled = false;
        if (!"NONE".equalsIgnoreCase(probeMethod) && probe != null && latencyRunner != null) {
            WifiEnvironmentProbe.ConnectedInfo connected = probe.getConnectedInfo();
            if (connected != null && (targetSsids.contains(connected.ssid()) || targetSsids.isEmpty())) {
                probeEnabled = true;
            }
        }

        engine = new CaptureEngine(probeEnabled);

        if (sensorSource != null) {
            sensorSource.start((tNs, x, y, z) -> {
                motionVariance.addSample(tNs, x, y, z);
                if (motionVariance.moved()) {
                    executeOnSessionThread(() -> processEngineEvent(CaptureEvent.motion(tNs, true)));
                }
            });
        }

        processEngineEvent(CaptureEvent.tapStart(currentTapMonoNs, px, py));
    }

    public void dragPin(double px, double py) {
        if (!capturing || engine == null) {
            return;
        }
        processEngineEvent(CaptureEvent.pinDrag(clock.monoNs(), px, py));
    }

    public void cancelCapture() {
        if (!capturing || engine == null) {
            return;
        }
        processEngineEvent(CaptureEvent.cancel(clock.monoNs()));
    }

    private void executeOnSessionThread(Runnable r) {
        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.ACTIVITY, clock.monoNs()));
            // sessionExecutor runs sequentially on its looper/thread
            new Handler(Looper.getMainLooper()).post(r);
        } else {
            r.run();
        }
    }

    private synchronized void processEngineEvent(CaptureEvent event) {
        if (engine == null) {
            return;
        }
        List<String> effects = engine.onEvent(event);
        interpretEffects(effects);
    }

    private void interpretEffects(List<String> effects) {
        for (String effect : effects) {
            if ("START_SCANNING".equals(effect)) {
                startScanning();
            } else if ("STOP_SCANNING".equals(effect)) {
                stopScanning();
            } else if (effect.startsWith("WAKE_AT@")) {
                long wakeNs = Long.parseLong(effect.substring("WAKE_AT@".length()));
                scheduleWake(wakeNs);
            } else if ("CANCEL_WAKE".equals(effect)) {
                if (timeoutScheduler != null) {
                    timeoutScheduler.cancel();
                }
            } else if ("START_PROBE".equals(effect)) {
                startProbe();
            } else if ("ABORT_PROBE".equals(effect)) {
                abortProbe();
            } else if (effect.startsWith("COMMIT(")) {
                handleCommitEffect(effect);
            } else if (effect.startsWith("DISCARD(")) {
                handleDiscardEffect(effect);
            } else if (effect.startsWith("PIN_AT(")) {
                handlePinAtEffect(effect);
            }
        }
    }

    private void startScanning() {
        if (wifiScanSource != null) {
            wifiScanSource.start(new WifiScanSource.Listener() {
                @Override
                public void onBatch(ScanBatch batch) {
                    handleScanBatch(batch);
                }

                @Override
                public void onScanThrottled() {
                }
            });
        }
        if (listener != null) {
            listener.onCaptureStep("Measuring… Scan 0 of 2", 0, 2);
        }
    }

    private synchronized void handleScanBatch(ScanBatch batch) {
        if (!capturing || engine == null) {
            return;
        }
        boolean isFresh = Freshness.isFresh(batch.arrivalMonoNs(), currentTapMonoNs);
        Set<Integer> sweepFreqs = new HashSet<>();
        for (WifiObservation obs : batch.rows()) {
            sweepFreqs.add(obs.freqMHz());
        }
        SweepClass sweepClass = SweepClassifier.classify(referenceFreqs, sweepFreqs);

        if (isFresh) {
            capturedBatches.add(new ClassifiedBatch(batch, sweepClass));
        }

        boolean isFull = (sweepClass == SweepClass.FULL);
        processEngineEvent(CaptureEvent.scan(batch.arrivalMonoNs(), isFresh, isFull));

        if (listener != null && engine != null) {
            int current = engine.getNFull();
            listener.onCaptureStep("Measuring… Scan " + current + " of 2", current, 2);
        }
    }

    private void stopScanning() {
        if (wifiScanSource != null) {
            wifiScanSource.stop();
        }
    }

    private void scheduleWake(long wakeNs) {
        if (timeoutScheduler != null) {
            long delayNs = wakeNs - clock.monoNs();
            long delayMs = Math.max(0L, delayNs / 1_000_000L);
            timeoutScheduler.schedule(() -> processEngineEvent(CaptureEvent.timeout(wakeNs)), delayMs);
        }
    }

    private void startProbe() {
        if (listener != null) {
            listener.onCaptureStep("Measuring latency…", 2, 2);
        }
        if (latencyRunner != null) {
            latencyRunner.start(null, null, null, new LatencyRunner.Listener() {
                @Override
                public void onRunning(ProbeCandidate candidate) {
                }

                @Override
                public void onCompleted(ProbeCandidate candidate, ProbeSequence.SequenceResult result) {
                    if (result != null && candidate != null) {
                        LatencyResultRow row = new LatencyResultRow();
                        row.id = IdGenerator.generate();
                        row.pointId = currentPointId;
                        row.method = candidate.method() != null ? candidate.method().name() : "ICMP";
                        row.target = candidate.target() != null ? candidate.target() : "";
                        row.boundIface = "wlan0";
                        row.scope = "GATEWAY";
                        if (result.summary() != null) {
                            row.sent = result.summary().sent();
                            row.recv = result.summary().sent() - result.summary().lost();
                            row.minMs = result.summary().minMs();
                            row.avgMs = result.summary().avgMs();
                            row.maxMs = result.summary().maxMs();
                            row.jitterMs = result.summary().jitterMs();
                            row.lossCount = result.summary().lost();
                        }
                        row.duringScan = false;
                        if (probe != null && probe.getConnectedInfo() != null) {
                            row.connectedBssid = probe.getConnectedInfo().bssid();
                            row.ssid = probe.getConnectedInfo().ssid();
                        } else {
                            row.connectedBssid = "";
                            row.ssid = "";
                        }
                        row.rtts = "[]";
                        pendingLatencyResult = row;
                    }
                    processEngineEvent(CaptureEvent.probeDone(clock.monoNs()));
                }

                @Override
                public void onBlocked() {
                    processEngineEvent(CaptureEvent.probeDone(clock.monoNs()));
                }

                @Override
                public void onNetworkLost() {
                    processEngineEvent(CaptureEvent.probeDone(clock.monoNs()));
                }

                @Override
                public void onCancelled() {
                    processEngineEvent(CaptureEvent.probeDone(clock.monoNs()));
                }
            });
        } else {
            processEngineEvent(CaptureEvent.probeDone(clock.monoNs()));
        }
    }

    private void abortProbe() {
        if (latencyRunner != null) {
            latencyRunner.cancel();
        }
    }

    private void handleCommitEffect(String effect) {
        cleanupPeripherals();
        if (listener != null) {
            listener.onCaptureStep("Saving…", 2, 2);
        }

        // COMMIT(<quality>,nFull=<n>)
        String quality = "OK";
        int nFull = 0;
        try {
            int openParen = effect.indexOf('(');
            int comma = effect.indexOf(',', openParen);
            int closeParen = effect.indexOf(')', comma);
            quality = effect.substring(openParen + 1, comma);
            String nFullStr = effect.substring(comma + 1, closeParen).replace("nFull=", "").trim();
            nFull = Integer.parseInt(nFullStr);
        } catch (Exception ignored) {
        }

        final String finalQuality = quality;
        final int finalNFull = nFull;
        final String pointId = currentPointId;
        final double px = currentPinX;
        final double py = currentPinY;
        final long tapMonoNs = currentTapMonoNs;
        final long tapWallMs = currentTapWallMs;
        final List<ClassifiedBatch> batches = new ArrayList<>(capturedBatches);
        final LatencyResultRow latencyRow = pendingLatencyResult;

        if (batchedWriter != null && db != null) {
            batchedWriter.submit(dbInstance -> {
                performCommit(dbInstance, pointId, px, py, tapMonoNs, tapWallMs, finalQuality, finalNFull, batches, latencyRow);
            }, new WriteCallback() {
                @Override
                public void onSuccess() {
                    capturing = false;
                    processEngineEvent(CaptureEvent.commitDone(clock.monoNs()));
                    if (listener != null) {
                        listener.onCaptureCommitted(pointId, finalQuality);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    capturing = false;
                    processEngineEvent(CaptureEvent.commitDone(clock.monoNs()));
                    if (listener != null) {
                        listener.onError("Commit failed: " + (error != null ? error.getMessage() : "unknown"));
                    }
                }
            });
        } else if (db != null) {
            try {
                performCommit(db, pointId, px, py, tapMonoNs, tapWallMs, finalQuality, finalNFull, batches, latencyRow);
                capturing = false;
                processEngineEvent(CaptureEvent.commitDone(clock.monoNs()));
                if (listener != null) {
                    listener.onCaptureCommitted(pointId, finalQuality);
                }
            } catch (Exception e) {
                capturing = false;
                processEngineEvent(CaptureEvent.commitDone(clock.monoNs()));
                if (listener != null) {
                    listener.onError("Commit failed: " + e.getMessage());
                }
            }
        }
    }

    private void performCommit(
            NetFieldDatabase dbInstance,
            String pointId,
            double px,
            double py,
            long tapMonoNs,
            long tapWallMs,
            String quality,
            int nFull,
            List<ClassifiedBatch> batches,
            @Nullable LatencyResultRow latencyRow
    ) {
        // 1. SurveyPoint
        SurveyPointRow pointRow = new SurveyPointRow();
        pointRow.id = pointId;
        pointRow.sessionId = sessionId;
        pointRow.x = px * metersPerPixel;
        pointRow.y = py * metersPerPixel;
        pointRow.source = "TAP";
        pointRow.quality = quality;
        pointRow.tMonoNs = tapMonoNs;
        pointRow.tWallMs = tapWallMs;
        pointRow.nScans = nFull;
        pointRow.positionUncertaintyM = 0.5;
        dbInstance.netFieldDao().insertSurveyPoint(pointRow);

        // 2. Scan and RawSample rows
        boolean anyDegradedTiming = false;
        for (ClassifiedBatch cb : batches) {
            ScanRow scanRow = new ScanRow();
            scanRow.id = IdGenerator.generate();
            scanRow.sessionId = sessionId;
            scanRow.pointId = pointId;
            scanRow.sweepClass = cb.sweepClass != null ? cb.sweepClass.name() : SweepClass.FULL.name();
            scanRow.startNs = cb.batch.arrivalMonoNs();
            scanRow.endNs = cb.batch.arrivalMonoNs();
            scanRow.arrivalNs = cb.batch.arrivalMonoNs();
            scanRow.tsPlausible = !cb.batch.degradedTiming();
            scanRow.throttleOffAtScan = true;
            if (!scanRow.tsPlausible) {
                anyDegradedTiming = true;
            }
            dbInstance.netFieldDao().insertScan(scanRow);

            List<RawSampleRow> rawRows = new ArrayList<>(cb.batch.rows().size());
            for (WifiObservation obs : cb.batch.rows()) {
                RawSampleRow raw = new RawSampleRow();
                raw.scanId = scanRow.id;
                raw.bssid = obs.bssid();
                raw.ssid = obs.ssid();
                raw.freqMHz = obs.freqMHz();
                raw.rssi = obs.rssiDbm();
                raw.scanResultTsNs = obs.timestampNs();
                rawRows.add(raw);
            }
            dbInstance.netFieldDao().insertRawSamples(rawRows);
        }

        // 3. Measurement rows per target BSSID heard in >= 1 FULL scan
        Map<String, List<Integer>> bssidRssiMap = new LinkedHashMap<>();
        Map<String, WifiObservation> bssidMetaMap = new HashMap<>();

        for (ClassifiedBatch cb : batches) {
            if (cb.sweepClass == SweepClass.FULL) {
                Set<String> seenInScan = new HashSet<>();
                for (WifiObservation obs : cb.batch.rows()) {
                    boolean isTarget = targetSsids.isEmpty() || targetSsids.contains(obs.ssid());
                    if (isTarget && seenInScan.add(obs.bssid())) {
                        bssidRssiMap.computeIfAbsent(obs.bssid(), k -> new ArrayList<>()).add(obs.rssiDbm());
                        bssidMetaMap.putIfAbsent(obs.bssid(), obs);
                    }
                }
            }
        }

        List<MeasurementRow> mRows = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : bssidRssiMap.entrySet()) {
            String bssid = entry.getKey();
            List<Integer> rssis = entry.getValue();
            int[] arr = new int[rssis.size()];
            int minRssi = Integer.MAX_VALUE;
            int maxRssi = Integer.MIN_VALUE;
            for (int i = 0; i < rssis.size(); i++) {
                int r = rssis.get(i);
                arr[i] = r;
                if (r < minRssi) minRssi = r;
                if (r > maxRssi) maxRssi = r;
            }
            int medianTenths = Median.tenths(arr);
            double medianDbm = medianTenths / 10.0;
            WifiObservation meta = bssidMetaMap.get(bssid);

            MeasurementRow mRow = new MeasurementRow();
            mRow.id = IdGenerator.generate();
            mRow.pointId = pointId;
            mRow.protocol = "WIFI";
            mRow.bssidOrId = bssid;
            mRow.ssid = meta != null ? meta.ssid() : "";
            mRow.freqMHz = meta != null ? meta.freqMHz() : 0;
            mRow.channel = freqToChannel(mRow.freqMHz);
            mRow.widthMHz = null;
            mRow.source = "SCAN";
            mRow.receiverId = "builtin-wifi";
            mRow.n = arr.length;
            mRow.rssiMedian = medianDbm;
            mRow.rssiMin = minRssi;
            mRow.rssiMax = maxRssi;
            mRows.add(mRow);
        }
        if (!mRows.isEmpty()) {
            dbInstance.netFieldDao().insertMeasurements(mRows);
        }

        // 4. LatencyResult
        if (latencyRow != null) {
            latencyRow.pointId = pointId;
            dbInstance.netFieldDao().insertLatencyResult(latencyRow);
        }

        // 5. Degraded timing update
        if (anyDegradedTiming) {
            dbInstance.netFieldDao().updateSessionDegradedTiming(sessionId, "INDICATIVE", "DEGRADED_TIMING");
        }
    }

    private void handleDiscardEffect(String effect) {
        cleanupPeripherals();
        capturing = false;
        String reason = effect.contains("NO_SCANS") ? "NO_SCANS" : "CANCELLED";
        if (listener != null) {
            listener.onCaptureDiscarded(reason);
        }
    }

    private void handlePinAtEffect(String effect) {
        try {
            int openParen = effect.indexOf('(');
            int comma = effect.indexOf(',', openParen);
            int closeParen = effect.indexOf(')', comma);
            double x = Double.parseDouble(effect.substring(openParen + 1, comma).trim());
            double y = Double.parseDouble(effect.substring(comma + 1, closeParen).trim());
            currentPinX = x;
            currentPinY = y;
            if (listener != null) {
                listener.onPinPosition(x, y);
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanupPeripherals() {
        if (sensorSource != null) {
            sensorSource.stop();
        }
        if (timeoutScheduler != null) {
            timeoutScheduler.cancel();
        }
        stopScanning();
    }

    public static int freqToChannel(int freqMHz) {
        if (freqMHz >= 2412 && freqMHz <= 2484) {
            if (freqMHz == 2484) return 14;
            return (freqMHz - 2407) / 5;
        } else if (freqMHz >= 5170 && freqMHz <= 5825) {
            return (freqMHz - 5000) / 5;
        } else if (freqMHz >= 5955 && freqMHz <= 7115) {
            return (freqMHz - 5950) / 5;
        }
        return 0;
    }
}
