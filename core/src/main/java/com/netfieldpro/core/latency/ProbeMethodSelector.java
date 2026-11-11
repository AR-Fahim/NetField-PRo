package com.netfieldpro.core.latency;

import java.util.List;

public final class ProbeMethodSelector {

    public static final long TRIAL_TIMEOUT_NS = 1_000_000_000L;

    public record SelectionResult(
            ProbeCandidate selected,
            int trialsRun,
            boolean unreachable
    ) {
    }

    @FunctionalInterface
    public interface ProbeFactory {
        IcmpProbe createProbe(ProbeCandidate candidate) throws Exception;
    }

    private ProbeMethodSelector() {
    }

    public static SelectionResult select(List<ProbeCandidate> candidates, ProbeFactory probeFactory) {
        return select(candidates, probeFactory, TRIAL_TIMEOUT_NS);
    }

    public static SelectionResult select(List<ProbeCandidate> candidates, ProbeFactory probeFactory, long timeoutNs) {
        if (candidates == null || candidates.isEmpty()) {
            return new SelectionResult(null, 0, true);
        }

        int trialsRun = 0;
        for (ProbeCandidate candidate : candidates) {
            trialsRun++;
            IcmpProbe probe = null;
            try {
                probe = probeFactory.createProbe(candidate);
                ProbeResult result = probe.probe(0, timeoutNs);
                if (result.isOk()) {
                    return new SelectionResult(candidate, trialsRun, false);
                }
            } catch (Exception e) {
                // Failure to create socket or probe -> lost for the trial, continue waterfall
            } finally {
                if (probe != null) {
                    try {
                        probe.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return new SelectionResult(null, trialsRun, true);
    }
}
