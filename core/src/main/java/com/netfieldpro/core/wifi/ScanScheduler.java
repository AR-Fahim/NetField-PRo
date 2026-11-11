package com.netfieldpro.core.wifi;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ScanScheduler {

    public enum Mode {
        ONE_SHOT,
        CONTINUOUS
    }

    public enum State {
        IDLE,
        REQUESTING,
        WAITING,
        COOLDOWN
    }

    public sealed interface Action {
        record RequestScan() implements Action {
            @Override
            public String toString() {
                return "REQUEST_SCAN";
            }
        }

        record WakeAt(long timeMs) implements Action {
            @Override
            public String toString() {
                return "WAKE_AT(" + timeMs + ")";
            }
        }

        record StatusRefused() implements Action {
            @Override
            public String toString() {
                return "STATUS_REFUSED";
            }
        }

        record Deliver(long id) implements Action {
            @Override
            public String toString() {
                return "DELIVER#" + id;
            }
        }

        record StatusTimeout() implements Action {
            @Override
            public String toString() {
                return "STATUS_TIMEOUT";
            }
        }
    }

    private final Mode mode;
    private State state = State.IDLE;
    private long deadlineMs = -1L;
    private long nextMs = -1L;

    public ScanScheduler(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode cannot be null");
    }

    public State getState() {
        return state;
    }

    public Mode getMode() {
        return mode;
    }

    public List<Action> onStart(long t) {
        if (state == State.IDLE) {
            state = State.REQUESTING;
            return List.of(new Action.RequestScan());
        }
        return Collections.emptyList();
    }

    public List<Action> onIssued(long t, boolean ok) {
        if (state != State.REQUESTING) {
            return Collections.emptyList();
        }
        if (ok) {
            state = State.WAITING;
            deadlineMs = t + 15000L;
            return List.of(new Action.WakeAt(deadlineMs));
        } else {
            if (mode == Mode.CONTINUOUS) {
                state = State.COOLDOWN;
                nextMs = t + 3000L;
                return List.of(new Action.StatusRefused(), new Action.WakeAt(nextMs));
            } else {
                state = State.IDLE;
                return List.of(new Action.StatusRefused());
            }
        }
    }

    public List<Action> onResult(long t, long id) {
        if (state == State.WAITING || state == State.REQUESTING) {
            if (mode == Mode.ONE_SHOT) {
                state = State.IDLE;
                return List.of(new Action.Deliver(id));
            } else {
                state = State.COOLDOWN;
                nextMs = t + 500L;
                return List.of(new Action.Deliver(id), new Action.WakeAt(nextMs));
            }
        } else if (state == State.COOLDOWN) {
            return List.of(new Action.Deliver(id));
        }
        return Collections.emptyList();
    }

    public List<Action> onWake(long t) {
        if (state == State.COOLDOWN) {
            if (t >= nextMs) {
                state = State.REQUESTING;
                return List.of(new Action.RequestScan());
            }
            return Collections.emptyList();
        } else if (state == State.WAITING) {
            if (t >= deadlineMs) {
                if (mode == Mode.CONTINUOUS) {
                    state = State.REQUESTING;
                    return List.of(new Action.StatusTimeout(), new Action.RequestScan());
                } else {
                    state = State.IDLE;
                    return List.of(new Action.StatusTimeout());
                }
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public List<Action> onStop() {
        state = State.IDLE;
        return Collections.emptyList();
    }
}
