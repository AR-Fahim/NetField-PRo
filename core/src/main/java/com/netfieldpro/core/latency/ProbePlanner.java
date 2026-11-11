package com.netfieldpro.core.latency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProbePlanner {

    private static final int[] TCP_PORTS = {443, 80, 53};

    private ProbePlanner() {
    }

    public static List<ProbeCandidate> plan(String gateway, String dns) {
        if (gateway == null || gateway.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<ProbeCandidate> candidates = new ArrayList<>();
        // 1. ICMP datagram to the gateway.
        candidates.add(ProbeCandidate.icmp(gateway));

        // 2. TCP to the gateway on 443, 80, 53.
        for (int port : TCP_PORTS) {
            candidates.add(ProbeCandidate.tcp(gateway, port));
        }

        // 3. TCP to the first IPv4 DNS server on 443, 80, 53 (skipped if it equals the gateway or is absent).
        if (dns != null && !dns.trim().isEmpty() && !dns.equals(gateway)) {
            for (int port : TCP_PORTS) {
                candidates.add(ProbeCandidate.tcp(dns, port));
            }
        }

        return Collections.unmodifiableList(candidates);
    }
}
