package com.netfieldpro.latency;

import com.netfieldpro.core.latency.Sleeper;
import com.netfieldpro.core.time.Clock;

import java.util.concurrent.locks.LockSupport;

public class AndroidSleeper implements Sleeper {
    private final Clock clock;

    public AndroidSleeper(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void sleepMs(long ms) throws InterruptedException {
        sleepNs(ms * 1_000_000L);
    }

    @Override
    public void sleepNs(long ns) throws InterruptedException {
        if (ns <= 0) {
            return;
        }
        long startNs = clock.monoNs();
        long endNs = startNs + ns;
        while (clock.monoNs() < endNs) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            long remaining = endNs - clock.monoNs();
            if (remaining <= 0) {
                break;
            }
            LockSupport.parkNanos(remaining);
        }
    }
}
