package com.netfieldpro.core.session;

public record SessionEvent(Type type, long tNs) {

    public enum Type {
        START_WARMUP,
        WARMUP_DONE,
        WARMUP_FAIL,
        RETRY_WARMUP,
        CONTINUE_INDICATIVE,
        SURVEY_OPENED,
        ACTIVITY,
        SCREEN_OFF,
        SCREEN_ON,
        IDLE_TICK,
        END_SESSION,
        RESUME
    }
}
