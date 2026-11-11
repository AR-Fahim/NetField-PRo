package com.netfieldpro.core.wifi;

public final class Freshness {
    private Freshness() {}

    public static boolean isFresh(long effectiveTs, long tapMono) {
        return effectiveTs >= tapMono;
    }
}
