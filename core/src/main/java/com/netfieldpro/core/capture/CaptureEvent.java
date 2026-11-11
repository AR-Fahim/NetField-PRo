package com.netfieldpro.core.capture;

import java.util.Objects;

public final class CaptureEvent {

    public enum Type {
        TAP_START,
        SCAN,
        TIMEOUT,
        PROBE_DONE,
        COMMIT_DONE,
        CANCEL,
        PIN_DRAG,
        MOTION
    }

    private final Type type;
    private final long t;
    private final double x;
    private final double y;
    private final boolean fresh;
    private final boolean full;
    private final boolean moved;

    private CaptureEvent(Type type, long t, double x, double y, boolean fresh, boolean full, boolean moved) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.t = t;
        this.x = x;
        this.y = y;
        this.fresh = fresh;
        this.full = full;
        this.moved = moved;
    }

    public static CaptureEvent tapStart(long t, double x, double y) {
        return new CaptureEvent(Type.TAP_START, t, x, y, false, false, false);
    }

    public static CaptureEvent scan(long t, boolean fresh, boolean full) {
        return new CaptureEvent(Type.SCAN, t, 0.0, 0.0, fresh, full, false);
    }

    public static CaptureEvent timeout(long t) {
        return new CaptureEvent(Type.TIMEOUT, t, 0.0, 0.0, false, false, false);
    }

    public static CaptureEvent probeDone(long t) {
        return new CaptureEvent(Type.PROBE_DONE, t, 0.0, 0.0, false, false, false);
    }

    public static CaptureEvent commitDone(long t) {
        return new CaptureEvent(Type.COMMIT_DONE, t, 0.0, 0.0, false, false, false);
    }

    public static CaptureEvent cancel(long t) {
        return new CaptureEvent(Type.CANCEL, t, 0.0, 0.0, false, false, false);
    }

    public static CaptureEvent pinDrag(long t, double x, double y) {
        return new CaptureEvent(Type.PIN_DRAG, t, x, y, false, false, false);
    }

    public static CaptureEvent motion(long t, boolean moved) {
        return new CaptureEvent(Type.MOTION, t, 0.0, 0.0, false, false, moved);
    }

    public Type type() {
        return type;
    }

    public long t() {
        return t;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public boolean fresh() {
        return fresh;
    }

    public boolean full() {
        return full;
    }

    public boolean moved() {
        return moved;
    }
}
