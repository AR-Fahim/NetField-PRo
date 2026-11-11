package com.netfieldpro.core.latency;

public interface Sleeper {
    void sleepMs(long ms) throws InterruptedException;
    void sleepNs(long ns) throws InterruptedException;
}
