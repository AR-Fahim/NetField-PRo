package com.netfieldpro.session;

import android.content.Context;

import com.netfieldpro.core.session.Effect;
import com.netfieldpro.core.session.SessionEngine;
import com.netfieldpro.core.session.SessionEvent;
import com.netfieldpro.core.session.Step;
import com.netfieldpro.core.time.Clock;
import com.netfieldpro.data.repository.SessionRepository;
import com.netfieldpro.time.AndroidClock;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SessionExecutor {

    private final ExecutorService executor;
    private final Clock clock;
    private SessionEngine engine;
    private EffectSink sink;
    private volatile String currentSessionId;
    private volatile String lastError;

    public SessionExecutor(SessionEngine engine, EffectSink sink, Clock clock, ExecutorService executor) {
        this.engine = engine != null ? engine : new SessionEngine();
        this.sink = sink;
        this.clock = clock != null ? clock : new AndroidClock();
        this.executor = executor != null ? executor : Executors.newSingleThreadExecutor(r -> new Thread(r, "nf-session"));
    }

    public static SessionExecutor create(Context context) {
        SessionRepository repo = SessionRepository.create(context);
        Clock clock = new AndroidClock();
        SessionEngine engine = new SessionEngine();
        SessionExecutor sessionExecutor = new SessionExecutor(engine, null, clock, null);
        EffectSink sink = new DefaultEffectSink(context, repo, sessionExecutor::getCurrentSessionId);
        sessionExecutor.setSink(sink);
        return sessionExecutor;
    }

    public void post(SessionEvent event) {
        executor.submit(() -> handleEvent(event));
    }

    public synchronized void handleEvent(SessionEvent event) {
        if (event == null || engine == null) {
            return;
        }

        Step step = engine.step(event);
        if (step.reason() != null) {
            SurveyRuntime.get().setLastEndReason(step.reason());
        }

        if (sink == null || step.effects() == null) {
            return;
        }

        for (Effect effect : step.effects()) {
            applyEffect(effect);
        }
    }

    private void applyEffect(Effect effect) {
        if (effect == null || sink == null) {
            return;
        }

        switch (effect.kind()) {
            case PERSIST_STATE -> {
                try {
                    Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            sink.onPersistState(engine.getState());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    future.get(2, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    lastError = "PERSIST_TIMEOUT";
                } catch (Exception e) {
                    Throwable curr = e;
                    boolean isTimeout = false;
                    while (curr != null) {
                        if (curr instanceof TimeoutException || "PERSIST_TIMEOUT".equals(curr.getMessage())) {
                            isTimeout = true;
                            break;
                        }
                        curr = curr.getCause();
                    }
                    if (isTimeout) {
                        lastError = "PERSIST_TIMEOUT";
                    }
                }
            }
            case RUNTIME_START -> {
                try {
                    sink.onRuntimeStart();
                } catch (SecurityException se) {
                    SurveyRuntime.get().setRuntimeStatus(
                            SurveyRuntime.RuntimeStatus.blocked(se.getClass().getSimpleName())
                    );
                } catch (Throwable t) {
                    SurveyRuntime.get().setRuntimeStatus(
                            SurveyRuntime.RuntimeStatus.blocked(t.getClass().getSimpleName())
                    );
                }
            }
            case RUNTIME_STOP -> {
                try {
                    sink.onRuntimeStop();
                } catch (Throwable ignored) {
                }
            }
            case SCREEN_FLAG -> {
                try {
                    SurveyRuntime.get().keepScreenOnMutable().postValue(effect.screenFlag());
                    sink.onScreenFlag(effect.screenFlag());
                } catch (Throwable ignored) {
                }
            }
            case SCHEDULE_IDLE -> {
                long nowMono = clock.monoNs();
                long delayMs = Math.max(0L, (long) Math.ceil((effect.atNs() - nowMono) / 1_000_000.0));
                Runnable tickRunnable = () -> post(new SessionEvent(SessionEvent.Type.IDLE_TICK, clock.monoNs()));
                try {
                    sink.onScheduleIdle(effect.atNs(), tickRunnable, delayMs);
                } catch (Throwable ignored) {
                }
            }
            case CANCEL_IDLE -> {
                try {
                    sink.onCancelIdle();
                } catch (Throwable ignored) {
                }
            }
            case LOG_GAP -> {
                try {
                    sink.onLogGap(effect.gapKind(), effect.atNs());
                } catch (Throwable ignored) {
                }
            }
            case MARK_INDICATIVE -> {
                try {
                    sink.onMarkIndicative();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public SessionEngine getEngine() {
        return engine;
    }

    public synchronized void setEngine(SessionEngine engine) {
        this.engine = engine;
    }

    public EffectSink getSink() {
        return sink;
    }

    public synchronized void setSink(EffectSink sink) {
        this.sink = sink;
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void awaitIdle(long timeoutMs) {
        Future<?> f = executor.submit(() -> {});
        try {
            f.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
