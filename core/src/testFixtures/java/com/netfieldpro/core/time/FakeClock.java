package com.netfieldpro.core.time;

public class FakeClock implements Clock {
    private long monoNs;
    private long wallMs;

    public FakeClock(long monoNs, long wallMs) {
        this.monoNs = monoNs;
        this.wallMs = wallMs;
    }

    public FakeClock advanceMs(long ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("advanceMs requires non-negative ms: " + ms);
        }
        this.monoNs += ms * 1_000_000L;
        this.wallMs += ms;
        return this;
    }

    public FakeClock advanceNs(long ns) {
        if (ns < 0) {
            throw new IllegalArgumentException("advanceNs requires non-negative ns: " + ns);
        }
        this.monoNs += ns;
        return this;
    }

    public FakeClock jumpWallMs(long delta) {
        this.wallMs += delta;
        return this;
    }

    @Override
    public long monoNs() {
        return monoNs;
    }

    @Override
    public long wallMs() {
        return wallMs;
    }
}

