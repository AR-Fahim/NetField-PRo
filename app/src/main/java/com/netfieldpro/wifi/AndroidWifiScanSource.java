package com.netfieldpro.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import com.netfieldpro.core.time.Clock;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.ScanTierSelector;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiScanSource;

public class AndroidWifiScanSource implements WifiScanSource {

    public static final long POLL_INTERVAL_MS = 1000L;

    private final Context context;
    private final Clock clock;
    private final WifiManager wifiManager;
    private ScanTierSelector tierSelector = new ScanTierSelector();

    private HandlerThread handlerThread;
    private Handler handler;
    private Listener listener;
    private boolean running = false;

    private Object callback; // WifiManager.ScanResultsCallback on API 30+
    private boolean callbackRegistered = false;

    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;

    private long lastMaxPollTs = Long.MIN_VALUE;

    private static final int DEBUG_LOG_CAPACITY = 50;
    private final List<String> debugLogRing = new ArrayList<>();

    private synchronized void logDebug(String line) {
        if (debugLogRing.size() >= DEBUG_LOG_CAPACITY) {
            debugLogRing.remove(0);
        }
        debugLogRing.add(line);
    }

    @androidx.annotation.VisibleForTesting
    public synchronized List<String> debugLog() {
        return new ArrayList<>(debugLogRing);
    }

    private final Runnable shadowPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || handler == null) {
                return;
            }
            if (!tierSelector.isConfirmed()) {
                double nowSec = clock.monoNs() / 1_000_000_000.0;
                Tier before = tierSelector.getTier();
                tierSelector.pollAdvanced(nowSec);
                Tier after = tierSelector.getTier();
                if (before != after) {
                    logDebug("TIER " + before + "->" + after + " reason=WATCH_TIMEOUT");
                    setupTier(after);
                }
                handler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || handler == null) {
                return;
            }
            if (tierSelector.getTier() == Tier.POLL) {
                handlePoll();
                handler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    public AndroidWifiScanSource(Context context, Clock clock) {
        this.context = Objects.requireNonNull(context, "context cannot be null").getApplicationContext();
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    public ScanTierSelector getTierSelector() {
        return tierSelector;
    }

    @Override
    public synchronized void start(Listener listener) {
        this.listener = listener;
        if (running) {
            return;
        }
        running = true;

        if (com.netfieldpro.BuildConfig.DEBUG) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("nf_debug", Context.MODE_PRIVATE);
            if (prefs.getBoolean("scan_force_poll", false)) {
                tierSelector = ScanTierSelector.startingAt(Tier.POLL);
            } else {
                tierSelector = new ScanTierSelector();
            }
        } else {
            tierSelector = new ScanTierSelector();
        }

        handlerThread = new HandlerThread("nf-wifi");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        handler.post(() -> {
            setupTier(tierSelector.getTier());
            if (!tierSelector.isConfirmed()) {
                handler.postDelayed(shadowPollRunnable, POLL_INTERVAL_MS);
            }
        });
    }

    private synchronized void setupTier(Tier tier) {
        if (!running) {
            return;
        }

        if (tier == Tier.CALLBACK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiManager != null) {
                try {
                    Executor executor = r -> {
                        if (handler != null) {
                            handler.post(r);
                        }
                    };
                    WifiManager.ScanResultsCallback cb = new WifiManager.ScanResultsCallback() {
                        @Override
                        public void onScanResultsAvailable() {
                            handleCallbackResults();
                        }
                    };
                    wifiManager.registerScanResultsCallback(executor, cb);
                    callback = cb;
                    callbackRegistered = true;
                    logDebug("REGISTER callback ok");
                    return;
                } catch (Throwable t) {
                    logDebug("REGISTER callback fail(" + t.getClass().getSimpleName() + ")");
                    Tier from = tierSelector.getTier();
                    tierSelector.registerFailed();
                    Tier to = tierSelector.getTier();
                    logDebug("TIER " + from + "->" + to + " reason=REGISTER_FAILED");
                    setupTier(to);
                    return;
                }
            } else {
                logDebug("REGISTER callback fail(UnsupportedOperationException)");
                Tier from = tierSelector.getTier();
                tierSelector.registerFailed();
                Tier to = tierSelector.getTier();
                logDebug("TIER " + from + "->" + to + " reason=REGISTER_FAILED");
                setupTier(to);
                return;
            }
        }

        if (tier == Tier.RECEIVER) {
            if (callbackRegistered && callback != null) {
                unregisterCallback();
            }
            if (!receiverRegistered) {
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        boolean updated = intent != null && intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                        if (updated) {
                            handleReceiverResults();
                        } else {
                            logDebug("IGNORED receiver updated=false");
                        }
                    }
                };
                try {
                    IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                    context.registerReceiver(receiver, filter, null, handler);
                    receiverRegistered = true;
                } catch (Throwable t) {
                    Tier from = tierSelector.getTier();
                    tierSelector.registerFailed();
                    Tier to = tierSelector.getTier();
                    logDebug("TIER " + from + "->" + to + " reason=REGISTER_FAILED");
                    setupTier(to);
                }
            }
            return;
        }

        if (tier == Tier.POLL) {
            if (callbackRegistered && callback != null) {
                unregisterCallback();
            }
            if (receiverRegistered && receiver != null) {
                unregisterReceiver();
            }
            handler.post(pollRunnable);
        }
    }

    private void handleCallbackResults() {
        if (!running) {
            return;
        }
        double nowSec = clock.monoNs() / 1_000_000_000.0;
        tierSelector.tierBatch(nowSec);
        emitBatch(Tier.CALLBACK, true);
    }

    private void handleReceiverResults() {
        if (!running) {
            return;
        }
        double nowSec = clock.monoNs() / 1_000_000_000.0;
        tierSelector.tierBatch(nowSec);
        emitBatch(Tier.RECEIVER, true);
    }

    @SuppressWarnings("MissingPermission")
    private void handlePoll() {
        if (!running || wifiManager == null) {
            return;
        }
        List<ScanResult> results;
        try {
            results = wifiManager.getScanResults();
        } catch (SecurityException e) {
            return;
        } catch (Exception e) {
            return;
        }
        if (results == null) {
            results = Collections.emptyList();
        }

        long maxTs = Long.MIN_VALUE;
        for (ScanResult sr : results) {
            if (sr.timestamp > maxTs) {
                maxTs = sr.timestamp;
            }
        }

        if (maxTs > lastMaxPollTs) {
            lastMaxPollTs = maxTs;
            double nowSec = clock.monoNs() / 1_000_000_000.0;
            tierSelector.tierBatch(nowSec);
            long arrivalMonoNs = clock.monoNs();
            ScanBatch batch = ScanResultMapper.map(results, arrivalMonoNs, Tier.POLL, true);
            logDebug("BATCH tier=" + Tier.POLL + " rows=" + batch.rows().size() + " updated=" + batch.updated() + " degraded=" + batch.degradedTiming());
            if (listener != null) {
                listener.onBatch(batch);
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void emitBatch(Tier tier, boolean updated) {
        if (wifiManager == null) {
            return;
        }
        List<ScanResult> results;
        try {
            results = wifiManager.getScanResults();
        } catch (SecurityException e) {
            return;
        } catch (Exception e) {
            return;
        }
        if (results == null) {
            results = Collections.emptyList();
        }
        long arrivalMonoNs = clock.monoNs();
        ScanBatch batch = ScanResultMapper.map(results, arrivalMonoNs, tier, updated);
        logDebug("BATCH tier=" + tier + " rows=" + batch.rows().size() + " updated=" + batch.updated() + " degraded=" + batch.degradedTiming());
        if (listener != null) {
            listener.onBatch(batch);
        }
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void requestScan() {
        if (!running || wifiManager == null) {
            return;
        }
        double nowSec = clock.monoNs() / 1_000_000_000.0;
        try {
            boolean ok = wifiManager.startScan();
            logDebug("REQUEST startScan=" + ok);
            if (ok) {
                tierSelector.requestOk(nowSec);
            } else {
                if (listener != null) {
                    listener.onScanThrottled();
                }
            }
        } catch (Exception e) {
            logDebug("REQUEST startScan=false");
            if (listener != null) {
                listener.onScanThrottled();
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            // Unregister-twice must not crash
            unregisterInternal();
            return;
        }
        running = false;
        unregisterInternal();
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
            handler = null;
        }
    }

    private synchronized void unregisterInternal() {
        unregisterCallback();
        unregisterReceiver();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void unregisterCallback() {
        if (callbackRegistered && callback != null && wifiManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    wifiManager.unregisterScanResultsCallback((WifiManager.ScanResultsCallback) callback);
                }
            } catch (Throwable ignored) {
            }
            callback = null;
            callbackRegistered = false;
        }
    }

    private void unregisterReceiver() {
        if (receiverRegistered && receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Throwable ignored) {
            }
            receiver = null;
            receiverRegistered = false;
        }
    }

    @Override
    public Tier getTier() {
        return tierSelector.getTier();
    }
}
