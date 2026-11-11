package com.netfieldpro.core.latency;

import com.netfieldpro.core.time.FakeClock;

public class FakeSleeper implements Sleeper {
    private final FakeClock clock;

    public FakeSleeper(FakeClock clock) {
        this.clock = clock;
    }

    @Override
    public void sleepMs(long ms) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        if (ms > 0) {
            clock.advanceMs(ms);
        }
    }

    @Override
    public void sleepNs(long ns) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        if (ns > 0) {
            clock.advanceNs(ns);
        }
    }
}
