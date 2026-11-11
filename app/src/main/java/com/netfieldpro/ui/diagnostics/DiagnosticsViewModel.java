package com.netfieldpro.ui.diagnostics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.wifi.EnvironmentAdvisor;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.ScanListModel;
import com.netfieldpro.core.wifi.ScanScheduler;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiScanSource;
import com.netfieldpro.latency.LatencyRunner;
import com.netfieldpro.latency.WifiNetworkTracker;
import com.netfieldpro.wifi.WifiEnvironmentProbe;

public class DiagnosticsViewModel extends AndroidViewModel {

    public static class LatencyUiModel {
        public enum State {
            NO_WIFI,
            IDLE_READY,
            RUNNING,
            RESULT,
            BLOCKED,
            NETWORK_LOST
        }

        public final State state;
        public final ProbeCandidate candidate;
        public final ProbeSequence.SequenceResult sequenceResult;

        public LatencyUiModel(State state, ProbeCandidate candidate, ProbeSequence.SequenceResult sequenceResult) {
            this.state = state;
            this.candidate = candidate;
            this.sequenceResult = sequenceResult;
        }

        public static LatencyUiModel noWifi() {
            return new LatencyUiModel(State.NO_WIFI, null, null);
        }

        public static LatencyUiModel idleReady() {
            return new LatencyUiModel(State.IDLE_READY, null, null);
        }

        public static LatencyUiModel running(ProbeCandidate candidate) {
            return new LatencyUiModel(State.RUNNING, candidate, null);
        }

        public static LatencyUiModel result(ProbeCandidate candidate, ProbeSequence.SequenceResult result) {
            return new LatencyUiModel(State.RESULT, candidate, result);
        }

        public static LatencyUiModel blocked() {
            return new LatencyUiModel(State.BLOCKED, null, null);
        }

        public static LatencyUiModel networkLost() {
            return new LatencyUiModel(State.NETWORK_LOST, null, null);
        }
    }

    private WifiScanSource scanSource;
    private WifiEnvironmentProbe environmentProbe;
    private LatencyRunner latencyRunner;
    private WifiNetworkTracker networkTracker;
    private ScanScheduler scheduler;
    private boolean isRunning = false;

    private final MutableLiveData<EnvironmentAdvisor.Advice> adviceLive = new MutableLiveData<>();
    private final MutableLiveData<WifiEnvironmentProbe.ConnectedInfo> connectedLive = new MutableLiveData<>();
    private final MutableLiveData<Tier> tierLive = new MutableLiveData<>();
    private final MutableLiveData<List<ScanListModel.Row>> networksLive = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> isScanningLive = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> throttledEventLive = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> lastScanCountLive = new MutableLiveData<>(null);
    private final MutableLiveData<LatencyUiModel> latencyLive = new MutableLiveData<>(LatencyUiModel.noWifi());

    private final WifiNetworkTracker.Listener trackerListener = (network, gateway, dns) -> {
        if (network == null || gateway == null) {
            if (latencyRunner != null && latencyRunner.isRunning()) {
                latencyRunner.onNetworkLost();
            }
            setOrPost(latencyLive, LatencyUiModel.noWifi());
        } else {
            LatencyUiModel cur = latencyLive.getValue();
            if (cur == null || cur.state == LatencyUiModel.State.NO_WIFI || cur.state == LatencyUiModel.State.NETWORK_LOST) {
                setOrPost(latencyLive, LatencyUiModel.idleReady());
            }
        }
    };

    private final WifiScanSource.Listener scanListener = new WifiScanSource.Listener() {
        @Override
        public void onBatch(ScanBatch batch) {
            handleBatch(batch);
        }

        @Override
        public void onScanThrottled() {
            handleThrottled();
        }
    };

    public DiagnosticsViewModel(@NonNull Application application) {
        super(application);
        this.scanSource = NetFieldApp.wifiScanSource();
        this.environmentProbe = NetFieldApp.wifiEnvironmentProbe();
        this.latencyRunner = NetFieldApp.latencyRunner();
        this.networkTracker = NetFieldApp.wifiNetworkTracker();
        this.scheduler = new ScanScheduler(ScanScheduler.Mode.ONE_SHOT);
    }

    public void setLatencyRunner(LatencyRunner runner) {
        this.latencyRunner = runner;
    }

    public LatencyRunner getLatencyRunner() {
        return latencyRunner;
    }

    public void setWifiNetworkTracker(WifiNetworkTracker tracker) {
        if (this.networkTracker != null && isRunning) {
            this.networkTracker.stopTracking();
        }
        this.networkTracker = tracker;
        if (this.networkTracker != null && isRunning) {
            this.networkTracker.setListener(trackerListener);
            this.networkTracker.startTracking();
        }
    }

    public LiveData<LatencyUiModel> getLatencyState() {
        return latencyLive;
    }

    public void setScanSource(WifiScanSource source) {
        if (this.scanSource == source) {
            return;
        }
        if (isRunning && this.scanSource != null) {
            this.scanSource.stop();
        }
        this.scanSource = source;
        if (isRunning && this.scanSource != null) {
            this.scanSource.start(scanListener);
            setOrPost(tierLive, this.scanSource.getTier());
        }
    }

    public void restartScanSource() {
        if (scanSource != null) {
            scanSource.stop();
            if (isRunning) {
                scanSource.start(scanListener);
                setOrPost(tierLive, scanSource.getTier());
            }
        }
    }

    public void setEnvironmentProbe(WifiEnvironmentProbe probe) {
        this.environmentProbe = probe;
    }

    public WifiScanSource getScanSource() {
        return scanSource;
    }

    public LiveData<EnvironmentAdvisor.Advice> getAdvice() {
        return adviceLive;
    }

    public LiveData<WifiEnvironmentProbe.ConnectedInfo> getConnectedInfo() {
        return connectedLive;
    }

    public LiveData<Tier> getTier() {
        return tierLive;
    }

    public LiveData<List<ScanListModel.Row>> getNetworks() {
        return networksLive;
    }

    public LiveData<Boolean> getIsScanning() {
        return isScanningLive;
    }

    public LiveData<Boolean> getThrottledEvent() {
        return throttledEventLive;
    }

    public LiveData<Integer> getLastScanCount() {
        return lastScanCountLive;
    }

    private <T> void setOrPost(MutableLiveData<T> live, T val) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            live.setValue(val);
        } else {
            live.postValue(val);
        }
    }

    public void refreshEnvironment() {
        if (environmentProbe != null) {
            setOrPost(adviceLive, environmentProbe.getAdvice());
            setOrPost(connectedLive, environmentProbe.getConnectedInfo());
        }
        if (scanSource != null) {
            setOrPost(tierLive, scanSource.getTier());
        }
    }

    public void onStart() {
        isRunning = true;
        refreshEnvironment();
        if (scanSource != null) {
            scanSource.start(scanListener);
            setOrPost(tierLive, scanSource.getTier());
        }
        if (networkTracker != null) {
            networkTracker.setListener(trackerListener);
            networkTracker.startTracking();
            if (networkTracker.isConnected()) {
                setOrPost(latencyLive, LatencyUiModel.idleReady());
            } else {
                setOrPost(latencyLive, LatencyUiModel.noWifi());
            }
        }
    }

    public void startOrCancelLatencyProbe() {
        LatencyUiModel current = latencyLive.getValue();
        if (current != null && current.state == LatencyUiModel.State.RUNNING) {
            if (latencyRunner != null) {
                latencyRunner.cancel();
            }
            return;
        }

        if (networkTracker == null || !networkTracker.isConnected() || latencyRunner == null) {
            setOrPost(latencyLive, LatencyUiModel.noWifi());
            return;
        }

        android.net.Network net = networkTracker.getCurrentNetwork();
        String gw = networkTracker.getGateway();
        String dns = networkTracker.getDns();

        setOrPost(latencyLive, LatencyUiModel.running(null));

        latencyRunner.start(net, gw, dns, new LatencyRunner.Listener() {
            @Override
            public void onRunning(ProbeCandidate candidate) {
                setOrPost(latencyLive, LatencyUiModel.running(candidate));
            }

            @Override
            public void onCompleted(ProbeCandidate candidate, ProbeSequence.SequenceResult result) {
                setOrPost(latencyLive, LatencyUiModel.result(candidate, result));
            }

            @Override
            public void onBlocked() {
                setOrPost(latencyLive, LatencyUiModel.blocked());
            }

            @Override
            public void onNetworkLost() {
                setOrPost(latencyLive, LatencyUiModel.networkLost());
            }

            @Override
            public void onCancelled() {
                boolean connected = networkTracker != null && networkTracker.isConnected();
                setOrPost(latencyLive, connected ? LatencyUiModel.idleReady() : LatencyUiModel.noWifi());
            }
        });
    }

    public void scanNow() {
        setOrPost(isScanningLive, true);
        if (scanSource != null) {
            scanSource.requestScan();
        }
    }

    public void handleBatch(ScanBatch batch) {
        setOrPost(isScanningLive, false);
        String connectedBssid = null;
        if (environmentProbe != null) {
            WifiEnvironmentProbe.ConnectedInfo ci = environmentProbe.getConnectedInfo();
            if (ci != null) {
                connectedBssid = ci.bssid();
            }
        }
        List<ScanListModel.Row> rows = ScanListModel.build(batch, connectedBssid);
        setOrPost(networksLive, rows);
        setOrPost(lastScanCountLive, rows.size());
        if (scanSource != null) {
            setOrPost(tierLive, scanSource.getTier());
        }
    }

    public void handleThrottled() {
        setOrPost(isScanningLive, false);
        setOrPost(throttledEventLive, true);
    }

    public void clearThrottledEvent() {
        setOrPost(throttledEventLive, false);
    }

    public void onStop() {
        isRunning = false;
        if (scanSource != null) {
            scanSource.stop();
        }
        isScanningLive.setValue(false);
        if (latencyRunner != null) {
            latencyRunner.cancel();
        }
        if (networkTracker != null) {
            networkTracker.stopTracking();
        }
        LatencyUiModel cur = latencyLive.getValue();
        if (cur != null && cur.state == LatencyUiModel.State.RUNNING) {
            boolean connected = networkTracker != null && networkTracker.isConnected();
            setOrPost(latencyLive, connected ? LatencyUiModel.idleReady() : LatencyUiModel.noWifi());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (latencyRunner != null) {
            latencyRunner.shutdown();
        }
        if (networkTracker != null) {
            networkTracker.stopTracking();
        }
    }
}
