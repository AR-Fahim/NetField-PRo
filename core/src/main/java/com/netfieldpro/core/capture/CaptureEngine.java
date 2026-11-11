package com.netfieldpro.core.capture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netfieldpro.core.util.FixedDecimal;

public final class CaptureEngine {

    public static final int DEFAULT_K = 2;
    public static final long DEFAULT_TIMEOUT_NS = 15_000_000_000L;

    public enum State {
        IDLE,
        AWAIT_SCANS,
        PROBING,
        COMMITTING
    }

    private final int k;
    private final long timeoutNs;
    private final boolean probeEnabled;

    private State state = State.IDLE;
    private int nFull = 0;
    private boolean moved = false;
    private double x = 0.0;
    private double y = 0.0;
    private long tapMonoNs = 0L;
    private long deadlineNs = 0L;

    public CaptureEngine() {
        this(DEFAULT_K, DEFAULT_TIMEOUT_NS, true);
    }

    public CaptureEngine(boolean probeEnabled) {
        this(DEFAULT_K, DEFAULT_TIMEOUT_NS, probeEnabled);
    }

    public CaptureEngine(int k, long timeoutNs, boolean probeEnabled) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive: " + k);
        }
        if (timeoutNs <= 0) {
            throw new IllegalArgumentException("timeoutNs must be positive: " + timeoutNs);
        }
        this.k = k;
        this.timeoutNs = timeoutNs;
        this.probeEnabled = probeEnabled;
    }

    public State getState() {
        return state;
    }

    public int getNFull() {
        return nFull;
    }

    public boolean isMoved() {
        return moved;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getTapMonoNs() {
        return tapMonoNs;
    }

    public long getDeadlineNs() {
        return deadlineNs;
    }

    public boolean isProbeEnabled() {
        return probeEnabled;
    }

    public String computeQuality() {
        if (moved) {
            return "MOVED";
        }
        if (nFull < k) {
            return "LOW_CONFIDENCE";
        }
        return "OK";
    }

    public List<String> step(CaptureEvent event) {
        return onEvent(event);
    }

    public List<String> onEvent(CaptureEvent event) {
        if (event == null) {
            return Collections.emptyList();
        }

        switch (event.type()) {
            case TAP_START:
                return handleTapStart(event);
            case SCAN:
                return handleScan(event);
            case TIMEOUT:
                return handleTimeout(event);
            case PROBE_DONE:
                return handleProbeDone(event);
            case COMMIT_DONE:
                return handleCommitDone(event);
            case CANCEL:
                return handleCancel(event);
            case PIN_DRAG:
                return handlePinDrag(event);
            case MOTION:
                return handleMotion(event);
            default:
                return Collections.emptyList();
        }
    }

    private List<String> handleTapStart(CaptureEvent event) {
        if (state != State.IDLE) {
            return Collections.emptyList();
        }
        state = State.AWAIT_SCANS;
        x = event.x();
        y = event.y();
        tapMonoNs = event.t();
        deadlineNs = event.t() + timeoutNs;
        nFull = 0;
        moved = false;

        List<String> effects = new ArrayList<>(3);
        effects.add("START_SCANNING");
        effects.add("WAKE_AT@" + deadlineNs);
        effects.add("PIN_AT(" + FixedDecimal.format(x, 3) + "," + FixedDecimal.format(y, 3) + ")");
        return effects;
    }

    private List<String> handleScan(CaptureEvent event) {
        if (state != State.AWAIT_SCANS) {
            return Collections.emptyList();
        }
        if (!event.fresh() || !event.full()) {
            return Collections.emptyList();
        }
        nFull++;
        if (nFull == k) {
            List<String> effects = new ArrayList<>(3);
            effects.add("STOP_SCANNING");
            effects.add("CANCEL_WAKE");
            if (probeEnabled) {
                state = State.PROBING;
                effects.add("START_PROBE");
            } else {
                state = State.COMMITTING;
                effects.add("COMMIT(" + computeQuality() + ",nFull=" + nFull + ")");
            }
            return effects;
        }
        return Collections.emptyList();
    }

    private List<String> handleTimeout(CaptureEvent event) {
        if (state != State.AWAIT_SCANS) {
            return Collections.emptyList();
        }
        if (event.t() < deadlineNs) {
            return Collections.emptyList();
        }
        if (nFull >= 1) {
            List<String> effects = new ArrayList<>(2);
            effects.add("STOP_SCANNING");
            if (probeEnabled) {
                state = State.PROBING;
                effects.add("START_PROBE");
            } else {
                state = State.COMMITTING;
                effects.add("COMMIT(" + computeQuality() + ",nFull=" + nFull + ")");
            }
            return effects;
        } else {
            state = State.IDLE;
            List<String> effects = new ArrayList<>(2);
            effects.add("STOP_SCANNING");
            effects.add("DISCARD(NO_SCANS)");
            return effects;
        }
    }

    private List<String> handleProbeDone(CaptureEvent event) {
        if (state != State.PROBING) {
            return Collections.emptyList();
        }
        state = State.COMMITTING;
        List<String> effects = new ArrayList<>(1);
        effects.add("COMMIT(" + computeQuality() + ",nFull=" + nFull + ")");
        return effects;
    }

    private List<String> handleCommitDone(CaptureEvent event) {
        if (state != State.COMMITTING) {
            return Collections.emptyList();
        }
        state = State.IDLE;
        return Collections.emptyList();
    }

    private List<String> handleCancel(CaptureEvent event) {
        if (state == State.AWAIT_SCANS) {
            state = State.IDLE;
            List<String> effects = new ArrayList<>(3);
            effects.add("STOP_SCANNING");
            effects.add("CANCEL_WAKE");
            effects.add("DISCARD(CANCELLED)");
            return effects;
        } else if (state == State.PROBING) {
            state = State.IDLE;
            List<String> effects = new ArrayList<>(2);
            effects.add("ABORT_PROBE");
            effects.add("DISCARD(CANCELLED)");
            return effects;
        }
        return Collections.emptyList();
    }

    private List<String> handlePinDrag(CaptureEvent event) {
        if (state == State.AWAIT_SCANS || state == State.PROBING) {
            x = event.x();
            y = event.y();
            List<String> effects = new ArrayList<>(1);
            effects.add("PIN_AT(" + FixedDecimal.format(x, 3) + "," + FixedDecimal.format(y, 3) + ")");
            return effects;
        }
        return Collections.emptyList();
    }

    private List<String> handleMotion(CaptureEvent event) {
        if (state == State.AWAIT_SCANS || state == State.PROBING) {
            if (event.moved()) {
                moved = true;
            }
        }
        return Collections.emptyList();
    }
}
