package com.netfieldpro.session;

import com.netfieldpro.core.session.Sm1State;

public interface EffectSink {
    void onPersistState(Sm1State state) throws Exception;
    void onRuntimeStart() throws Exception;
    void onRuntimeStop() throws Exception;
    void onScreenFlag(boolean keepScreenOn) throws Exception;
    void onScheduleIdle(long atNs, Runnable tickRunnable, long delayMs) throws Exception;
    void onCancelIdle() throws Exception;
    void onLogGap(String gapKind, long atNs) throws Exception;
    void onMarkIndicative() throws Exception;
}
