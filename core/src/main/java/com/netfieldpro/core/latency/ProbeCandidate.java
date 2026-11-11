package com.netfieldpro.core.latency;

import com.netfieldpro.core.model.enums.ProbeMethod;

public record ProbeCandidate(ProbeMethod method, String target, int port) {

    public static ProbeCandidate icmp(String target) {
        return new ProbeCandidate(ProbeMethod.ICMP, target, 0);
    }

    public static ProbeCandidate tcp(String target, int port) {
        return new ProbeCandidate(ProbeMethod.TCP, target, port);
    }

    @Override
    public String toString() {
        if (method == ProbeMethod.ICMP) {
            return "ICMP " + target;
        }
        return "TCP " + target + ":" + port;
    }
}
