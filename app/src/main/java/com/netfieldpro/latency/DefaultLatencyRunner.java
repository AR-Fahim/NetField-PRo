package com.netfieldpro.latency;

import android.net.Network;
import android.os.Handler;
import android.os.Looper;

import com.netfieldpro.core.latency.IcmpProbe;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeMethodSelector;
import com.netfieldpro.core.latency.ProbePlanner;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.latency.Sleeper;
import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.time.AndroidClock;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultLatencyRunner implements LatencyRunner {

    private final Clock clock;
    private final Sleeper sleeper;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private Future<?> activeFuture;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicBoolean isNetworkLost = new AtomicBoolean(false);

    public DefaultLatencyRunner() {
        this(new AndroidClock());
    }

    public DefaultLatencyRunner(Clock clock) {
        this.clock = clock;
        this.sleeper = new AndroidSleeper(clock);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nf-probe");
            t.setDaemon(true);
            return t;
        });
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public DefaultLatencyRunner(Clock clock, Sleeper sleeper, ExecutorService executor, Handler handler) {
        this.clock = clock;
        this.sleeper = sleeper;
        this.executor = executor;
        this.mainHandler = handler;
    }

    @Override
    public synchronized void start(Network network, String gateway, String dns, Listener listener) {
        cancel();

        isCancelled.set(false);
        isNetworkLost.set(false);

        activeFuture = executor.submit(() -> {
            try {
                if (isCancelled.get()) {
                    postToMain(listener::onCancelled);
                    return;
                }

                List<ProbeCandidate> candidates = ProbePlanner.plan(gateway, dns);
                if (candidates.isEmpty()) {
                    postToMain(listener::onBlocked);
                    return;
                }

                ProbeMethodSelector.SelectionResult selection = ProbeMethodSelector.select(
                        candidates,
                        c -> createProbe(c, network, isNetworkLost::get)
                );

                if (isCancelled.get()) {
                    postToMain(listener::onCancelled);
                    return;
                }

                if (isNetworkLost.get()) {
                    postToMain(listener::onNetworkLost);
                    return;
                }

                if (selection.unreachable() || selection.selected() == null) {
                    postToMain(listener::onBlocked);
                    return;
                }

                ProbeCandidate winner = selection.selected();
                postToMain(() -> listener.onRunning(winner));

                try (IcmpProbe sequenceProbe = createProbe(winner, network, isNetworkLost::get)) {
                    ProbeSequence.SequenceResult seqResult = ProbeSequence.run(
                            sequenceProbe,
                            clock,
                            sleeper,
                            isCancelled::get
                    );

                    if (isCancelled.get() || seqResult.cancelled()) {
                        postToMain(listener::onCancelled);
                    } else if (isNetworkLost.get() || seqResult.aborted()) {
                        postToMain(listener::onNetworkLost);
                    } else {
                        postToMain(() -> listener.onCompleted(winner, seqResult));
                    }
                }
            } catch (InterruptedException e) {
                if (isCancelled.get()) {
                    postToMain(listener::onCancelled);
                } else if (isNetworkLost.get()) {
                    postToMain(listener::onNetworkLost);
                }
            } catch (Throwable t) {
                if (isCancelled.get()) {
                    postToMain(listener::onCancelled);
                } else {
                    postToMain(listener::onBlocked);
                }
            }
        });
    }

    @Override
    public synchronized void onNetworkLost() {
        isNetworkLost.set(true);
    }

    @Override
    public synchronized void cancel() {
        isCancelled.set(true);
        if (activeFuture != null && !activeFuture.isDone()) {
            activeFuture.cancel(true);
            activeFuture = null;
        }
    }

    @Override
    public synchronized void shutdown() {
        cancel();
        executor.shutdownNow();
    }

    @Override
    public synchronized boolean isRunning() {
        return activeFuture != null && !activeFuture.isDone();
    }

    protected IcmpProbe createProbe(ProbeCandidate candidate, Network network, java.util.function.BooleanSupplier isAborted) {
        if (candidate.method() == ProbeMethod.ICMP) {
            return new DgramIcmpProbe(network, candidate.target(), clock, isAborted);
        } else {
            return new TcpConnectProbe(network, candidate.target(), candidate.port(), clock, isAborted);
        }
    }

    private void postToMain(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else if (mainHandler != null) {
            mainHandler.post(r);
        } else {
            r.run();
        }
    }
}
