package com.netfieldpro.core.latency;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.netfieldpro.core.model.enums.ProbeMethod;
import com.netfieldpro.core.time.FakeClock;

public class LatencyCoreTest {

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // --- IC-1..4 ---

    @Test
    public void testIC1_echoRequestStandard() {
        byte[] packet = IcmpPacket.echoRequest(0x1234, 1, "abcd");
        String hex = bytesToHex(packet);
        assertEquals("08 00 21 04 12 34 00 01 61 62 63 64", hex);
    }

    @Test
    public void testIC2_payloadOddLengthChecksum() {
        byte[] packet = IcmpPacket.echoRequest(0x1234, 1, "abc");
        assertEquals((byte) 0x21, packet[2]);
        assertEquals((byte) 0x68, packet[3]);
    }

    @Test
    public void testIC3_payloadCarryFoldChecksum() {
        byte[] payload = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        byte[] packet = IcmpPacket.echoRequest(0x1234, 1, payload);
        assertEquals((byte) 0xe5, packet[2]);
        assertEquals((byte) 0xca, packet[3]);
    }

    @Test
    public void testIC4_parseEchoReply() {
        byte[] reply = new byte[]{0x00, 0x00, 0x00, 0x00, 0x12, 0x34, 0x00, 0x05};
        assertEquals(5, IcmpPacket.parseEchoReply(reply, reply.length));

        // Type 8 -> -1
        byte[] type8 = new byte[]{0x08, 0x00, 0x00, 0x00, 0x12, 0x34, 0x00, 0x05};
        assertEquals(-1, IcmpPacket.parseEchoReply(type8, type8.length));

        // Short buffer (7 bytes) -> -1
        byte[] shortBuf = new byte[]{0x00, 0x00, 0x00, 0x00, 0x12, 0x34, 0x00};
        assertEquals(-1, IcmpPacket.parseEchoReply(shortBuf, 7));
    }

    // --- LS-1..5, P95-1..4, JI-1..3, LO-1 ---

    @Test
    public void testLS1_summarizeAllReceived() {
        long[] us = new long[]{10_000L, 14_000L, 11_000L, 30_000L, 12_000L};
        LatencyStats.Summary s = LatencyStats.summarize(us);
        assertEquals(Double.valueOf(15.400), s.avgMs());
        assertEquals("15.400", s.avgMsFormatted());
        assertEquals(Double.valueOf(10.000), s.minMs());
        assertEquals(Double.valueOf(30.000), s.maxMs());
        assertEquals(Double.valueOf(11.000), s.jitterMs());
        assertEquals("11.000", s.jitterMsFormatted());
        assertEquals(5, s.sent());
        assertEquals(0, s.lost());
    }

    @Test
    public void testLS2_summarizeWithLost() {
        long[] us = new long[]{10_000L, LatencyStats.LOST, 14_000L, 11_000L, 30_000L};
        LatencyStats.Summary s = LatencyStats.summarize(us);
        assertEquals(Double.valueOf(16.250), s.avgMs());
        assertEquals("16.250", s.avgMsFormatted());
        assertEquals(Double.valueOf(10.000), s.minMs());
        assertEquals(Double.valueOf(30.000), s.maxMs());
        assertEquals(Double.valueOf(8.667), s.jitterMs());
        assertEquals("8.667", s.jitterMsFormatted());
        assertEquals(5, s.sent());
        assertEquals(1, s.lost());
    }

    @Test
    public void testLS3_allLost() {
        long[] us = new long[]{LatencyStats.LOST, LatencyStats.LOST, LatencyStats.LOST, LatencyStats.LOST, LatencyStats.LOST};
        LatencyStats.Summary s = LatencyStats.summarize(us);
        assertNull(s.avgMs());
        assertNull(s.minMs());
        assertNull(s.maxMs());
        assertNull(s.jitterMs());
        assertEquals(5, s.sent());
        assertEquals(5, s.lost());
    }

    @Test
    public void testLS4_oneReceived() {
        long[] us = new long[]{25_000L, LatencyStats.LOST, LatencyStats.LOST};
        LatencyStats.Summary s = LatencyStats.summarize(us);
        assertEquals(Double.valueOf(25.000), s.avgMs());
        assertNull(s.jitterMs());
    }

    @Test
    public void testLS5_halfEvenTie() {
        long[] us = new long[]{1L, 2L};
        LatencyStats.Summary s = LatencyStats.summarize(us);
        assertEquals(Double.valueOf(0.002), s.avgMs());
        assertEquals("0.002", s.avgMsFormatted());
    }

    @Test
    public void testP95_1_to_4() {
        long[] p95_1 = new long[]{12, 15, 11, 40, 13, 14, 16, 12, 18, 90};
        assertEquals(90L, LatencyStats.p95(p95_1));

        long[] p95_2 = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 100};
        assertEquals(19L, LatencyStats.p95(p95_2));

        long[] p95_3 = new long[21];
        for (int i = 0; i < 21; i++) {
            p95_3[i] = i + 1;
        }
        assertEquals(20L, LatencyStats.p95(p95_3));

        long[] p95_4_single = new long[]{42L};
        assertEquals(42L, LatencyStats.p95(p95_4_single));

        assertThrows(IllegalArgumentException.class, () -> LatencyStats.p95(new long[0]));
    }

    @Test
    public void testJI_1_to_3() {
        long[] ji1 = new long[]{10_000L, 14_000L, 11_000L, 30_000L, 12_000L};
        assertEquals(Double.valueOf(11.000), LatencyStats.summarize(ji1).jitterMs());

        long[] ji2 = new long[]{10_000L, LatencyStats.LOST, 14_000L, 11_000L, 30_000L};
        assertEquals(Double.valueOf(8.667), LatencyStats.summarize(ji2).jitterMs());

        long[] ji3 = new long[]{10_000L};
        assertNull(LatencyStats.summarize(ji3).jitterMs());
    }

    @Test
    public void testLO_1() {
        assertTrue(LatencyStats.lossAtMost(3, 300, 10)); // 1.0% = 10 permille -> 3*1000 <= 10*300 (3000 <= 3000)
        assertFalse(LatencyStats.lossAtMost(4, 300, 10)); // 4*1000 > 10*300
        assertThrows(IllegalArgumentException.class, () -> LatencyStats.lossAtMost(0, 0, 10));
    }

    // --- TC-1..4 ---

    @Test
    public void testTC1_socketTimeoutException() {
        assertEquals(TcpOutcome.LOST, TcpOutcome.classify(new SocketTimeoutException("timed out")));
        assertFalse(TcpOutcome.LOST.isCountedAsOk());
    }

    @Test
    public void testTC2_connectExceptionRefused() {
        TcpOutcome outcome = TcpOutcome.classify(new ConnectException("failed: ECONNREFUSED (Connection refused)"));
        assertEquals(TcpOutcome.REFUSED, outcome);
        assertTrue(outcome.isCountedAsOk());
    }

    @Test
    public void testTC3_connectExceptionUnreachable() {
        TcpOutcome outcome = TcpOutcome.classify(new ConnectException("Network is unreachable"));
        assertEquals(TcpOutcome.LOST, outcome);
    }

    @Test
    public void testTC4_genericIOException() {
        TcpOutcome outcome = TcpOutcome.classify(new IOException("x"));
        assertEquals(TcpOutcome.LOST, outcome);
    }

    // --- TG-1..4 ---

    @Test
    public void testTG1_planGatewayAndDns() {
        List<ProbeCandidate> plan = ProbePlanner.plan("10.0.0.1", "10.0.0.2");
        assertEquals(7, plan.size());
        assertEquals(new ProbeCandidate(ProbeMethod.ICMP, "10.0.0.1", 0), plan.get(0));
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.1", 443), plan.get(1));
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.1", 80), plan.get(2));
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.1", 53), plan.get(3));
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.2", 443), plan.get(4));
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.2", 80), plan.get(5));
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.2", 53), plan.get(6));
    }

    @Test
    public void testTG2_planDnsEqualsGateway() {
        List<ProbeCandidate> plan = ProbePlanner.plan("10.0.0.1", "10.0.0.1");
        assertEquals(4, plan.size());
    }

    @Test
    public void testTG3_planDnsNull() {
        List<ProbeCandidate> plan = ProbePlanner.plan("10.0.0.1", null);
        assertEquals(4, plan.size());
    }

    @Test
    public void testTG4_planGatewayNull() {
        List<ProbeCandidate> plan = ProbePlanner.plan(null, "10.0.0.2");
        assertTrue(plan.isEmpty());
    }

    // --- PS-1..4 ---

    @Test
    public void testPS1_icmpTrialOk() {
        List<ProbeCandidate> plan = ProbePlanner.plan("10.0.0.1", "10.0.0.2");
        ProbeMethodSelector.SelectionResult res = ProbeMethodSelector.select(plan, candidate -> (seq, timeoutNs) -> {
            if (candidate.method() == ProbeMethod.ICMP) {
                return ProbeResult.ok(5000);
            }
            return ProbeResult.lost();
        });
        assertFalse(res.unreachable());
        assertEquals(ProbeMethod.ICMP, res.selected().method());
        assertEquals(1, res.trialsRun());
    }

    @Test
    public void testPS2_icmpLostTcp443Ok() {
        List<ProbeCandidate> plan = ProbePlanner.plan("10.0.0.1", "10.0.0.2");
        ProbeMethodSelector.SelectionResult res = ProbeMethodSelector.select(plan, candidate -> (seq, timeoutNs) -> {
            if (candidate.method() == ProbeMethod.TCP && candidate.port() == 443) {
                return ProbeResult.ok(12000);
            }
            return ProbeResult.lost();
        });
        assertFalse(res.unreachable());
        assertEquals(new ProbeCandidate(ProbeMethod.TCP, "10.0.0.1", 443), res.selected());
        assertEquals(2, res.trialsRun());
    }

    @Test
    public void testPS3_allLostUnreachable() {
        List<ProbeCandidate> plan = ProbePlanner.plan("10.0.0.1", "10.0.0.2");
        ProbeMethodSelector.SelectionResult res = ProbeMethodSelector.select(plan, candidate -> (seq, timeoutNs) -> ProbeResult.lost());
        assertTrue(res.unreachable());
        assertNull(res.selected());
        assertEquals(7, res.trialsRun());
    }

    @Test
    public void testPS4_emptyPlanUnreachable() {
        ProbeMethodSelector.SelectionResult res = ProbeMethodSelector.select(List.of(), candidate -> (seq, timeoutNs) -> ProbeResult.ok(1000));
        assertTrue(res.unreachable());
        assertNull(res.selected());
        assertEquals(0, res.trialsRun());
    }

    // --- PR-1..4 ---

    @Test
    public void testPR1_allProbes10ms() throws Exception {
        FakeClock clock = new FakeClock(0L, 1000L);
        FakeSleeper sleeper = new FakeSleeper(clock);

        IcmpProbe probe = (seq, timeoutNs) -> {
            clock.advanceMs(10);
            return ProbeResult.ok(10_000L); // 10000 us = 10 ms
        };

        ProbeSequence.SequenceResult res = ProbeSequence.run(probe, clock, sleeper, () -> false);
        assertEquals(5, res.sent());
        assertFalse(res.aborted());
        assertFalse(res.cancelled());
        assertEquals(List.of(0L, 200L, 400L, 600L, 800L), res.sendTimesMs());
        assertArrayEquals(new long[]{10_000L, 10_000L, 10_000L, 10_000L, 10_000L}, res.rttUsOrLost());
        assertEquals(810_000_000L, clock.monoNs()); // 810 ms
    }

    @Test
    public void testPR2_thirdProbeLost() throws Exception {
        FakeClock clock = new FakeClock(0L, 1000L);
        FakeSleeper sleeper = new FakeSleeper(clock);

        IcmpProbe probe = (seq, timeoutNs) -> {
            if (seq == 3) {
                clock.advanceMs(1000); // timeout 1000ms
                return ProbeResult.lost();
            }
            clock.advanceMs(10);
            return ProbeResult.ok(10_000L);
        };

        ProbeSequence.SequenceResult res = ProbeSequence.run(probe, clock, sleeper, () -> false);
        assertEquals(5, res.sent());
        assertEquals(List.of(0L, 200L, 400L, 1400L, 1600L), res.sendTimesMs());
    }

    @Test
    public void testPR3_thirdProbeAborted() throws Exception {
        FakeClock clock = new FakeClock(0L, 1000L);
        FakeSleeper sleeper = new FakeSleeper(clock);

        IcmpProbe probe = (seq, timeoutNs) -> {
            if (seq == 3) {
                return ProbeResult.aborted();
            }
            clock.advanceMs(10);
            return ProbeResult.ok(10_000L);
        };

        ProbeSequence.SequenceResult res = ProbeSequence.run(probe, clock, sleeper, () -> false);
        assertTrue(res.aborted());
        assertEquals(2, res.sent());
        assertEquals(2, res.rttUsOrLost().length);
    }

    @Test
    public void testPR4_cancelAfterSecondProbe() throws Exception {
        FakeClock clock = new FakeClock(0L, 1000L);
        FakeSleeper sleeper = new FakeSleeper(clock);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        IcmpProbe probe = (seq, timeoutNs) -> {
            clock.advanceMs(10);
            if (seq == 2) {
                cancelled.set(true);
            }
            return ProbeResult.ok(10_000L);
        };

        ProbeSequence.SequenceResult res = ProbeSequence.run(probe, clock, sleeper, cancelled::get);
        assertTrue(res.cancelled());
        assertEquals(2, res.sent());
        assertEquals(2, res.rttUsOrLost().length);
    }
}
