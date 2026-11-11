package com.netfieldpro.time;

import android.os.SystemClock;
import com.netfieldpro.core.time.Clock;

public class AndroidClock implements Clock {
    @Override
    public long monoNs() {
        return SystemClock.elapsedRealtimeNanos();
    }

    @Override
    public long wallMs() {
        return System.currentTimeMillis();
    }
}

