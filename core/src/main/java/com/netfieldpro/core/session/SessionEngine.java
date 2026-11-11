package com.netfieldpro.core.session;

import java.util.Collections;
import java.util.List;

public class SessionEngine {

    public static final long LIMIT = 600_000_000_000L;

    private Sm1State state;
    private long lastActivityNs;
    private Long screenOffSinceNs;
    private EndReason lastEndReason;
    private Step lastStep;

    public SessionEngine() {
        this(Sm1State.CREATED, 0L);
    }

    public SessionEngine(Sm1State state, long lastActivityNs) {
        this.state = state != null ? state : Sm1State.CREATED;
        this.lastActivityNs = lastActivityNs;
        this.lastStep = new Step(this.state, null, Collections.emptyList());
    }

    static SessionEngine at(Sm1State state, long lastActivityNs) {
        return new SessionEngine(state, lastActivityNs);
    }

    public static SessionEngine restored(Sm1State persisted, long nowNs) {
        Sm1State initial = persisted != null ? persisted : Sm1State.CREATED;
        Sm1State targetState;
        List<Effect> initialEffects;
        long targetActivity = 0L;

        if (initial == Sm1State.WARMING_UP) {
            targetState = Sm1State.WARMUP_FAILED;
            initialEffects = List.of(Effect.persistState());
            targetActivity = nowNs;
        } else {
            targetState = initial;
            initialEffects = Collections.emptyList();
            if (targetState == Sm1State.WARMUP_FAILED
                    || targetState == Sm1State.READY
                    || targetState == Sm1State.ACTIVE) {
                targetActivity = nowNs;
            }
        }

        SessionEngine engine = new SessionEngine(targetState, targetActivity);
        engine.lastStep = new Step(targetState, null, initialEffects);
        return engine;
    }

    public Sm1State getState() {
        return state;
    }

    public long getLastActivityNs() {
        return lastActivityNs;
    }

    public Long getScreenOffSinceNs() {
        return screenOffSinceNs;
    }

    public EndReason getLastEndReason() {
        return lastEndReason;
    }

    public Step getLastStep() {
        return lastStep;
    }

    public Step step(SessionEvent event) {
        return onEvent(event);
    }

    public Step onEvent(SessionEvent event) {
        if (event == null) {
            return lastStep != null ? lastStep : new Step(state, lastEndReason, Collections.emptyList());
        }

        if (state == Sm1State.ENDED) {
            lastStep = new Step(Sm1State.ENDED, lastEndReason, Collections.emptyList());
            return lastStep;
        }

        List<Effect> effects = Collections.emptyList();
        EndReason currentReason = null;

        switch (state) {
            case CREATED -> {
                switch (event.type()) {
                    case START_WARMUP -> {
                        state = Sm1State.WARMING_UP;
                        effects = List.of(
                                Effect.persistState(),
                                Effect.runtimeStart(),
                                Effect.screenFlag(true)
                        );
                    }
                    case END_SESSION -> {
                        state = Sm1State.ENDED;
                        lastEndReason = EndReason.END_SESSION;
                        currentReason = EndReason.END_SESSION;
                        effects = List.of(Effect.persistState());
                    }
                    default -> effects = Collections.emptyList();
                }
            }
            case WARMING_UP -> {
                switch (event.type()) {
                    case WARMUP_DONE -> {
                        state = Sm1State.READY;
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.persistState(),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case WARMUP_FAIL -> {
                        state = Sm1State.WARMUP_FAILED;
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.persistState(),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case END_SESSION -> {
                        state = Sm1State.ENDED;
                        lastEndReason = EndReason.END_SESSION;
                        currentReason = EndReason.END_SESSION;
                        effects = endEffects();
                    }
                    case SCREEN_OFF -> effects = handleScreenOff(event.tNs());
                    case SCREEN_ON -> effects = handleScreenOn(event.tNs());
                    default -> effects = Collections.emptyList();
                }
            }
            case WARMUP_FAILED -> {
                switch (event.type()) {
                    case RETRY_WARMUP -> {
                        state = Sm1State.WARMING_UP;
                        effects = List.of(Effect.persistState(), Effect.cancelIdle());
                    }
                    case CONTINUE_INDICATIVE -> {
                        state = Sm1State.READY;
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.persistState(),
                                Effect.markIndicative(),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case ACTIVITY -> {
                        lastActivityNs = event.tNs();
                        effects = List.of(Effect.scheduleIdle(event.tNs() + LIMIT));
                    }
                    case RESUME -> {
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.runtimeStart(),
                                Effect.screenFlag(true),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case IDLE_TICK -> {
                        if (event.tNs() - lastActivityNs < LIMIT) {
                            effects = List.of(Effect.scheduleIdle(lastActivityNs + LIMIT));
                        } else {
                            state = Sm1State.ENDED;
                            lastEndReason = EndReason.IDLE_10MIN;
                            currentReason = EndReason.IDLE_10MIN;
                            effects = endEffects();
                        }
                    }
                    case END_SESSION -> {
                        state = Sm1State.ENDED;
                        lastEndReason = EndReason.END_SESSION;
                        currentReason = EndReason.END_SESSION;
                        effects = endEffects();
                    }
                    case SCREEN_OFF -> effects = handleScreenOff(event.tNs());
                    case SCREEN_ON -> effects = handleScreenOn(event.tNs());
                    default -> effects = Collections.emptyList();
                }
            }
            case READY -> {
                switch (event.type()) {
                    case SURVEY_OPENED -> {
                        state = Sm1State.ACTIVE;
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.persistState(),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case ACTIVITY -> {
                        lastActivityNs = event.tNs();
                        effects = List.of(Effect.scheduleIdle(event.tNs() + LIMIT));
                    }
                    case RESUME -> {
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.runtimeStart(),
                                Effect.screenFlag(true),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case IDLE_TICK -> {
                        if (event.tNs() - lastActivityNs < LIMIT) {
                            effects = List.of(Effect.scheduleIdle(lastActivityNs + LIMIT));
                        } else {
                            state = Sm1State.ENDED;
                            lastEndReason = EndReason.IDLE_10MIN;
                            currentReason = EndReason.IDLE_10MIN;
                            effects = endEffects();
                        }
                    }
                    case END_SESSION -> {
                        state = Sm1State.ENDED;
                        lastEndReason = EndReason.END_SESSION;
                        currentReason = EndReason.END_SESSION;
                        effects = endEffects();
                    }
                    case SCREEN_OFF -> effects = handleScreenOff(event.tNs());
                    case SCREEN_ON -> effects = handleScreenOn(event.tNs());
                    default -> effects = Collections.emptyList();
                }
            }
            case ACTIVE -> {
                switch (event.type()) {
                    case ACTIVITY -> {
                        lastActivityNs = event.tNs();
                        effects = List.of(Effect.scheduleIdle(event.tNs() + LIMIT));
                    }
                    case RESUME -> {
                        lastActivityNs = event.tNs();
                        effects = List.of(
                                Effect.runtimeStart(),
                                Effect.screenFlag(true),
                                Effect.scheduleIdle(event.tNs() + LIMIT)
                        );
                    }
                    case IDLE_TICK -> {
                        if (event.tNs() - lastActivityNs < LIMIT) {
                            effects = List.of(Effect.scheduleIdle(lastActivityNs + LIMIT));
                        } else {
                            state = Sm1State.ENDED;
                            lastEndReason = EndReason.IDLE_10MIN;
                            currentReason = EndReason.IDLE_10MIN;
                            effects = endEffects();
                        }
                    }
                    case END_SESSION -> {
                        state = Sm1State.ENDED;
                        lastEndReason = EndReason.END_SESSION;
                        currentReason = EndReason.END_SESSION;
                        effects = endEffects();
                    }
                    case SCREEN_OFF -> effects = handleScreenOff(event.tNs());
                    case SCREEN_ON -> effects = handleScreenOn(event.tNs());
                    default -> effects = Collections.emptyList();
                }
            }
            case ENDED -> effects = Collections.emptyList();
        }

        lastStep = new Step(state, currentReason != null ? currentReason : lastEndReason, effects);
        return lastStep;
    }

    private List<Effect> handleScreenOff(long tNs) {
        if (screenOffSinceNs == null) {
            screenOffSinceNs = tNs;
            return List.of(Effect.logGap("SCREEN_OFF", tNs));
        }
        return Collections.emptyList();
    }

    private List<Effect> handleScreenOn(long tNs) {
        if (screenOffSinceNs != null) {
            screenOffSinceNs = null;
            return List.of(Effect.logGap("SCREEN_ON", tNs));
        }
        return Collections.emptyList();
    }

    private List<Effect> endEffects() {
        return List.of(
                Effect.persistState(),
                Effect.cancelIdle(),
                Effect.runtimeStop(),
                Effect.screenFlag(false)
        );
    }
}
