package com.netfieldpro.core.session;

import java.util.Objects;

public final class Effect {

    public enum Kind {
        PERSIST_STATE,
        RUNTIME_START,
        RUNTIME_STOP,
        SCREEN_FLAG,
        SCHEDULE_IDLE,
        CANCEL_IDLE,
        LOG_GAP,
        MARK_INDICATIVE
    }

    private final Kind kind;
    private final boolean screenFlag;
    private final long atNs;
    private final String gapKind;

    private Effect(Kind kind, boolean screenFlag, long atNs, String gapKind) {
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
        this.screenFlag = screenFlag;
        this.atNs = atNs;
        this.gapKind = gapKind;
    }

    public static Effect persistState() {
        return new Effect(Kind.PERSIST_STATE, false, 0L, null);
    }

    public static Effect runtimeStart() {
        return new Effect(Kind.RUNTIME_START, false, 0L, null);
    }

    public static Effect runtimeStop() {
        return new Effect(Kind.RUNTIME_STOP, false, 0L, null);
    }

    public static Effect screenFlag(boolean on) {
        return new Effect(Kind.SCREEN_FLAG, on, 0L, null);
    }

    public static Effect scheduleIdle(long atNs) {
        return new Effect(Kind.SCHEDULE_IDLE, false, atNs, null);
    }

    public static Effect cancelIdle() {
        return new Effect(Kind.CANCEL_IDLE, false, 0L, null);
    }

    public static Effect logGap(String gapKind, long atNs) {
        return new Effect(Kind.LOG_GAP, false, atNs, gapKind);
    }

    public static Effect markIndicative() {
        return new Effect(Kind.MARK_INDICATIVE, false, 0L, null);
    }

    public Kind kind() {
        return kind;
    }

    public boolean screenFlag() {
        return screenFlag;
    }

    public long atNs() {
        return atNs;
    }

    public String gapKind() {
        return gapKind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Effect effect)) return false;
        return screenFlag == effect.screenFlag
                && atNs == effect.atNs
                && kind == effect.kind
                && Objects.equals(gapKind, effect.gapKind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, screenFlag, atNs, gapKind);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case PERSIST_STATE -> "PERSIST_STATE";
            case RUNTIME_START -> "RUNTIME_START";
            case RUNTIME_STOP -> "RUNTIME_STOP";
            case SCREEN_FLAG -> "SCREEN_FLAG(" + screenFlag + ")";
            case SCHEDULE_IDLE -> "SCHEDULE_IDLE@" + atNs;
            case CANCEL_IDLE -> "CANCEL_IDLE";
            case LOG_GAP -> "LOG_GAP(" + gapKind + ")@" + atNs;
            case MARK_INDICATIVE -> "MARK_INDICATIVE";
        };
    }
}
