package com.netfieldpro.ui.survey;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.netfieldpro.core.session.SessionEvent;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.ScanScheduler;
import com.netfieldpro.core.wifi.SweepClassifier;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.core.wifi.WifiScanSource;
import com.netfieldpro.session.SessionExecutor;
import com.netfieldpro.time.AndroidClock;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WarmupCoordinator {

    public static final long TIMEOUT_NS = 20_000_000_000L;
    public static final int TARGET_SCANS = 3;
    public static final int MAX_BSSID_CAP = 60;

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

    public record NetworkGroup(
            String ssid,
            int bssidCount,
            int maxRssiDbm,
            boolean isConnected,
            boolean isSelected
    ) {}

    public interface Listener {
        void onProgress(int scansDone, int totalScans);
        void onNetworksUpdated(List<NetworkGroup> networks, int totalSelectedBssids);
        void onWarmupDone(List<Integer> referenceFreqs);
        void onWarmupFailed();
        void onCapExceeded();
    }

    private final WifiScanSource scanSource;
    private final SessionExecutor sessionExecutor;
    private final Clock clock;
    private final WifiEnvironmentProbe probe;
    private final TimeoutScheduler timeoutScheduler;

    private Listener listener;
    private ScanScheduler scheduler;
    private boolean running = false;
    private boolean completed = false;
    private boolean failed = false;
    private boolean indicative = false;

    private long startMonoNs = 0L;
    private final List<ScanBatch> batches = new ArrayList<>();
    private final List<Set<Integer>> batchFreqSets = new ArrayList<>();
    private List<Integer> referenceFreqs = Collections.emptyList();

    // Network groupings
    private final Map<String, Set<String>> bssidsBySsid = new LinkedHashMap<>();
    private final Map<String, Integer> maxRssiBySsid = new HashMap<>();
    private final Set<String> selectedSsids = new LinkedHashSet<>();
    private String connectedSsid;

    private final WifiScanSource.Listener scanListener = new WifiScanSource.Listener() {
        @Override
        public void onBatch(ScanBatch batch) {
            handleBatch(batch);
        }

        @Override
        public void onScanThrottled() {
            // Logged or handled if necessary
        }
    };

    public WarmupCoordinator(
            WifiScanSource scanSource,
            SessionExecutor sessionExecutor,
            Clock clock,
            WifiEnvironmentProbe probe,
            TimeoutScheduler timeoutScheduler
    ) {
        this.scanSource = Objects.requireNonNull(scanSource, "scanSource cannot be null");
        this.sessionExecutor = sessionExecutor;
        this.clock = clock != null ? clock : new AndroidClock();
        this.probe = probe;
        this.timeoutScheduler = timeoutScheduler != null ? timeoutScheduler : new HandlerTimeoutScheduler();
    }

    public WarmupCoordinator(
            WifiScanSource scanSource,
            SessionExecutor sessionExecutor,
            Clock clock,
            WifiEnvironmentProbe probe
    ) {
        this(scanSource, sessionExecutor, clock, probe, new HandlerTimeoutScheduler());
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        completed = false;
        failed = false;
        indicative = false;
        startMonoNs = clock.monoNs();
        batches.clear();
        batchFreqSets.clear();
        referenceFreqs = Collections.emptyList();
        bssidsBySsid.clear();
        maxRssiBySsid.clear();
        selectedSsids.clear();

        if (probe != null) {
            WifiEnvironmentProbe.ConnectedInfo info = probe.getConnectedInfo();
            if (info != null && info.ssid() != null && !info.ssid().isEmpty()) {
                connectedSsid = info.ssid();
            }
        }

        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.START_WARMUP, startMonoNs));
        }

        scheduler = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        long nowMs = clock.monoNs() / 1_000_000L;
        List<ScanScheduler.Action> startActions = scheduler.onStart(nowMs);
        for (ScanScheduler.Action action : startActions) {
            if (action instanceof ScanScheduler.Action.RequestScan) {
                scanSource.requestScan();
                scheduler.onIssued(nowMs, true);
            }
        }

        scanSource.start(scanListener);
        timeoutScheduler.schedule(this::checkTimeout, 20_000L);

        if (listener != null) {
            listener.onProgress(0, TARGET_SCANS);
        }
    }

    public synchronized void checkTimeout() {
        if (!running || completed || failed) {
            return;
        }
        long elapsedNs = clock.monoNs() - startMonoNs;
        if (elapsedNs >= TIMEOUT_NS && batches.size() < TARGET_SCANS) {
            triggerFailure();
        }
    }

    public synchronized void handleBatch(ScanBatch batch) {
        if (!running || completed || failed) {
            return;
        }

        long nowNs = clock.monoNs();
        if (nowNs - startMonoNs >= TIMEOUT_NS && batches.size() < TARGET_SCANS) {
            triggerFailure();
            return;
        }

        try {
            Log.d("NF_FACT", "tier=" + batch.tier() + ", rows=" + (batch.rows() != null ? batch.rows().size() : 0));
        } catch (Throwable ignored) {
        }

        batches.add(batch);
        Set<Integer> freqs = new HashSet<>();
        if (batch.rows() != null) {
            for (WifiObservation obs : batch.rows()) {
                freqs.add(obs.freqMHz());
                String rawSsid = obs.ssid();
                String ssidKey = (rawSsid == null || rawSsid.isEmpty()) ? "" : rawSsid;

                Set<String> bssids = bssidsBySsid.computeIfAbsent(ssidKey, k -> new HashSet<>());
                bssids.add(obs.bssid());

                int currentMax = maxRssiBySsid.getOrDefault(ssidKey, Integer.MIN_VALUE);
                if (obs.rssiDbm() > currentMax) {
                    maxRssiBySsid.put(ssidKey, obs.rssiDbm());
                }

                // Default selection: connected SSID if heard
                if (connectedSsid != null && connectedSsid.equals(ssidKey)) {
                    if (!selectedSsids.contains(ssidKey)) {
                        if (getSelectedBssidCount() + bssids.size() <= MAX_BSSID_CAP) {
                            selectedSsids.add(ssidKey);
                        }
                    }
                }
            }
        }
        batchFreqSets.add(freqs);

        long nowMs = clock.monoNs() / 1_000_000L;
        scheduler.onResult(nowMs, batches.size());

        notifyNetworksUpdated();

        int doneCount = batches.size();
        if (listener != null) {
            listener.onProgress(doneCount, TARGET_SCANS);
        }

        if (doneCount >= TARGET_SCANS) {
            timeoutScheduler.cancel();
            computeReferenceSet();
        }
    }

    private void computeReferenceSet() {
        try {
            List<Integer> ref = SweepClassifier.referenceSet(batchFreqSets);
            if (ref.isEmpty()) {
                triggerFailure();
            } else {
                this.referenceFreqs = ref;
                completed = true;
                if (sessionExecutor != null) {
                    sessionExecutor.post(new SessionEvent(SessionEvent.Type.WARMUP_DONE, clock.monoNs()));
                }
                if (listener != null) {
                    listener.onWarmupDone(referenceFreqs);
                }
            }
        } catch (Exception e) {
            triggerFailure();
        }
    }

    private void triggerFailure() {
        failed = true;
        timeoutScheduler.cancel();
        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.WARMUP_FAIL, clock.monoNs()));
        }
        if (listener != null) {
            listener.onWarmupFailed();
        }
    }

    public synchronized void triggerFailureForTest() {
        triggerFailure();
    }

    public synchronized void retry() {
        if (!failed) {
            return;
        }
        failed = false;
        completed = false;
        indicative = false;
        startMonoNs = clock.monoNs();
        batches.clear();
        batchFreqSets.clear();
        referenceFreqs = Collections.emptyList();

        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.RETRY_WARMUP, startMonoNs));
        }

        scheduler = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        long nowMs = clock.monoNs() / 1_000_000L;
        List<ScanScheduler.Action> startActions = scheduler.onStart(nowMs);
        for (ScanScheduler.Action action : startActions) {
            if (action instanceof ScanScheduler.Action.RequestScan) {
                scanSource.requestScan();
                scheduler.onIssued(nowMs, true);
            }
        }

        timeoutScheduler.schedule(this::checkTimeout, 20_000L);

        if (listener != null) {
            listener.onProgress(0, TARGET_SCANS);
        }
    }

    public synchronized void continueIndicative() {
        if (!failed) {
            return;
        }
        failed = false;
        completed = true;
        indicative = true;
        timeoutScheduler.cancel();

        if (sessionExecutor != null) {
            sessionExecutor.post(new SessionEvent(SessionEvent.Type.CONTINUE_INDICATIVE, clock.monoNs()));
        }

        if (listener != null) {
            listener.onWarmupDone(referenceFreqs);
        }
    }

    public synchronized boolean toggleSsid(String ssid) {
        String key = ssid != null ? ssid : "";
        if (selectedSsids.contains(key)) {
            selectedSsids.remove(key);
            notifyNetworksUpdated();
            return true;
        }

        int currentSelected = getSelectedBssidCount();
        int bssidsForThisSsid = bssidsBySsid.getOrDefault(key, Collections.emptySet()).size();
        if (currentSelected + bssidsForThisSsid > MAX_BSSID_CAP) {
            if (listener != null) {
                listener.onCapExceeded();
            }
            return false;
        }

        selectedSsids.add(key);
        notifyNetworksUpdated();
        return true;
    }

    public synchronized int getSelectedBssidCount() {
        int sum = 0;
        for (String ssid : selectedSsids) {
            Set<String> bssids = bssidsBySsid.get(ssid);
            if (bssids != null) {
                sum += bssids.size();
            }
        }
        return sum;
    }

    public synchronized List<String> getSelectedSsids() {
        return new ArrayList<>(selectedSsids);
    }

    public synchronized List<Integer> getReferenceFreqs() {
        return referenceFreqs;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public synchronized boolean isFailed() {
        return failed;
    }

    public synchronized boolean isIndicative() {
        return indicative;
    }

    public synchronized void stop() {
        running = false;
        timeoutScheduler.cancel();
        scanSource.stop();
        if (scheduler != null) {
            scheduler.onStop();
        }
    }

    public synchronized List<NetworkGroup> getNetworks() {
        List<NetworkGroup> list = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : bssidsBySsid.entrySet()) {
            String ssid = entry.getKey();
            int count = entry.getValue().size();
            int maxRssi = maxRssiBySsid.getOrDefault(ssid, -100);
            boolean isConn = (connectedSsid != null && connectedSsid.equals(ssid));
            boolean isSel = selectedSsids.contains(ssid);
            list.add(new NetworkGroup(ssid, count, maxRssi, isConn, isSel));
        }
        return list;
    }

    private void notifyNetworksUpdated() {
        if (listener != null) {
            listener.onNetworksUpdated(getNetworks(), getSelectedBssidCount());
        }
    }
}
