package com.netfieldpro.core.latency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.netfieldpro.core.time.Clock;

public final class ProbeSequence {

    public static final int PROBE_COUNT = 5;
    public static final long SPACING_MS = 200L;
    public static final long SPACING_NS = SPACING_MS * 1_000_000L;
    public static final long TIMEOUT_MS = 1000L;
    public static final long TIMEOUT_NS = TIMEOUT_MS * 1_000_000L;
    public static final int ICMP_PAYLOAD_SIZE = 32;

    public record SequenceResult(
            long[] rttUsOrLost,
            List<Long> sendTimesMs,
            boolean aborted,
            boolean cancelled,
            int sent,
            LatencyStats.Summary summary
    ) {
    }

    private ProbeSequence() {
    }

    public static byte[] createDefaultPayload() {
        byte[] payload = new byte[ICMP_PAYLOAD_SIZE];
        for (int i = 0; i < ICMP_PAYLOAD_SIZE; i++) {
            payload[i] = (byte) i;
        }
        return payload;
    }

    public static SequenceResult run(
            IcmpProbe probe,
            Clock clock,
            Sleeper sleeper,
            BooleanSupplier isCancelled
    ) throws InterruptedException {
        return run(probe, clock, sleeper, isCancelled, PROBE_COUNT, SPACING_NS, TIMEOUT_NS);
    }

    public static SequenceResult run(
            IcmpProbe probe,
            Clock clock,
            Sleeper sleeper,
            BooleanSupplier isCancelled,
            int count,
            long spacingNs,
            long timeoutNs
    ) throws InterruptedException {
        List<Long> results = new ArrayList<>();
        List<Long> sendTimesMs = new ArrayList<>();
        boolean aborted = false;
        boolean cancelled = false;

        long prevSendStartNs = 0L;

        for (int seq = 1; seq <= count; seq++) {
            if (isCancelled != null && isCancelled.getAsBoolean()) {
                cancelled = true;
                break;
            }

            if (seq > 1) {
                long nowNs = clock.monoNs();
                long targetSendNs = prevSendStartNs + spacingNs;
                if (targetSendNs > nowNs) {
                    sleeper.sleepNs(targetSendNs - nowNs);
                }
            }

            if (isCancelled != null && isCancelled.getAsBoolean()) {
                cancelled = true;
                break;
            }

            long sendStartNs = clock.monoNs();
            prevSendStartNs = sendStartNs;
            sendTimesMs.add(sendStartNs / 1_000_000L);

            ProbeResult result;
            try {
                result = probe.probe(seq, timeoutNs);
            } catch (Exception e) {
                result = ProbeResult.lost();
            }

            if (result.isAborted()) {
                aborted = true;
                break;
            }

            if (result.isOk()) {
                results.add(result.rttUs());
            } else {
                results.add(LatencyStats.LOST);
            }
        }

        int sent = results.size();
        long[] rttArr = new long[sent];
        for (int i = 0; i < sent; i++) {
            rttArr[i] = results.get(i);
        }
        LatencyStats.Summary summary = LatencyStats.summarize(rttArr);

        return new SequenceResult(
                rttArr,
                Collections.unmodifiableList(sendTimesMs),
                aborted,
                cancelled,
                sent,
                summary
        );
    }
}
