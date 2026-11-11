package com.netfieldpro.core.latency;

import java.io.Closeable;

public interface IcmpProbe extends Closeable {
    ProbeResult probe(int seq, long timeoutNs);

    @Override
    default void close() {
    }
}
