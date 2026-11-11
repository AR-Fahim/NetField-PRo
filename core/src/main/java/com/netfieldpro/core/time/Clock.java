package com.netfieldpro.core.time;

public interface Clock {
    long monoNs();
    long wallMs();
}

