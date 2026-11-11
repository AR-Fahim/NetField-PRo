package com.netfieldpro.core.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.netfieldpro.core.model.enums.SweepClass;

public class WifiCoreTest {

    private static List<String> stringify(List<ScanScheduler.Action> actions) {
        List<String> res = new ArrayList<>();
        for (ScanScheduler.Action a : actions) {
            res.add(a.toString());
        }
        return res;
    }

    // --- CH-1..4 ChannelMath vectors ---

    @Test
    public void testCH1() {
        assertEquals(Band.B24, ChannelMath.toBand(2412));
        assertEquals(1, ChannelMath.toChannel(2412));

        assertEquals(Band.B24, ChannelMath.toBand(2437));
        assertEquals(6, ChannelMath.toChannel(2437));

        assertEquals(Band.B24, ChannelMath.toBand(2462));
        assertEquals(11, ChannelMath.toChannel(2462));

        assertEquals(Band.B24, ChannelMath.toBand(2484));
        assertEquals(14, ChannelMath.toChannel(2484));
    }

    @Test
    public void testCH2() {
        assertEquals(Band.B5, ChannelMath.toBand(5180));
        assertEquals(36, ChannelMath.toChannel(5180));

        assertEquals(Band.B5, ChannelMath.toBand(5745));
        assertEquals(149, ChannelMath.toChannel(5745));

        assertEquals(Band.B5, ChannelMath.toBand(5825));
        assertEquals(165, ChannelMath.toChannel(5825));

        assertEquals(Band.B5, ChannelMath.toBand(5890));
        assertEquals(178, ChannelMath.toChannel(5890));

        assertEquals(Band.B5, ChannelMath.toBand(5895));
        assertEquals(179, ChannelMath.toChannel(5895));
    }

    @Test
    public void testCH3() {
        assertEquals(Band.B6, ChannelMath.toBand(5955));
        assertEquals(1, ChannelMath.toChannel(5955));

        assertEquals(Band.B6, ChannelMath.toBand(5935));
        assertEquals(2, ChannelMath.toChannel(5935));

        assertEquals(Band.B6, ChannelMath.toBand(6415));
        assertEquals(93, ChannelMath.toChannel(6415));
    }

    @Test
    public void testCH4() {
        assertEquals(Band.UNKNOWN, ChannelMath.toBand(2399));
        assertEquals(-1, ChannelMath.toChannel(2399));

        assertEquals(Band.UNKNOWN, ChannelMath.toBand(5900));
        assertEquals(-1, ChannelMath.toChannel(5900));

        assertEquals(Band.B6, ChannelMath.toBand(5925));
        assertEquals(-1, ChannelMath.toChannel(5925));

        assertEquals(Band.B5, ChannelMath.toBand(5182));
        assertEquals(-1, ChannelMath.toChannel(5182));

        assertEquals(Band.B24, ChannelMath.toBand(2413));
        assertEquals(-1, ChannelMath.toChannel(2413));
    }

    // --- NM-1..3 Normalization vectors ---

    @Test
    public void testNM1() {
        assertEquals("aa:bb:cc:dd:ee:0f", Normalize.bssid("AA:BB:CC:DD:EE:0F"));
        assertNull(Normalize.bssid("aa-bb-cc-dd-ee-0f"));
        assertNull(Normalize.bssid("aa:bb:cc:dd:ee"));
        assertNull(Normalize.bssid("02:00:00:00:00:00"));
        assertNull(Normalize.bssid("zz:bb:cc:dd:ee:0f"));
        assertNull(Normalize.bssid(null));
    }

    @Test
    public void testNM2() {
        assertEquals("", Normalize.scanSsid(null));
        assertEquals("eduroam", Normalize.scanSsid("eduroam"));

        assertEquals("eduroam", Normalize.connectedSsid("\"eduroam\""));
        assertNull(Normalize.connectedSsid("<unknown ssid>"));
        assertNull(Normalize.connectedSsid(""));
        assertNull(Normalize.connectedSsid("\"\""));
        assertNull(Normalize.connectedSsid(null));
    }

    @Test
    public void testNM3() {
        assertEquals(Long.valueOf(123456000L), Normalize.scanResultTsToNs(123456L));
        assertNull(Normalize.scanResultTsToNs(0L));
        assertNull(Normalize.scanResultTsToNs(-100L));
        assertFalse(TimestampPlausibility.isPlausible(0L, 100_000_000L));
        assertFalse(TimestampPlausibility.isPlausible(-100L, 100_000_000L));
    }

    // --- TSP-1..4 Timestamp Plausibility vectors ---

    @Test
    public void testTSP1() {
        long arrival = 100_000_000_000L;
        assertTrue(TimestampPlausibility.isPlausible(arrival, arrival));
    }

    @Test
    public void testTSP2() {
        long arrival = 100_000_000_000L;
        assertTrue(TimestampPlausibility.isPlausible(arrival + 1_000_000_000L, arrival));
        assertFalse(TimestampPlausibility.isPlausible(arrival + 1_000_000_001L, arrival));
    }

    @Test
    public void testTSP3() {
        long arrival = 100_000_000_000L;
        assertTrue(TimestampPlausibility.isPlausible(arrival - 30_000_000_000L, arrival));
        assertFalse(TimestampPlausibility.isPlausible(arrival - 30_000_000_001L, arrival));
    }

    @Test
    public void testTSP4() {
        long arrival = 100_000_000_000L;
        assertTrue(TimestampPlausibility.isBatchPlausible(Collections.emptyList(), arrival));
        long rowTs = arrival + 500_000_000L;
        assertEquals(rowTs, TimestampPlausibility.effective(rowTs, arrival, true));
        assertEquals(arrival, TimestampPlausibility.effective(rowTs, arrival, false));
        assertEquals(arrival, TimestampPlausibility.effective(null, arrival, true));
    }

    // --- RS-1..3 Reference Set vectors ---

    @Test
    public void testRS1() {
        Set<Integer> a = Set.of(2412, 2437, 5180);
        Set<Integer> b = Set.of(2412, 5180, 5200);
        Set<Integer> c = Set.of(2437, 5180);
        List<Integer> expected = List.of(2412, 2437, 5180);
        assertEquals(expected, SweepClassifier.referenceSet(List.of(a, b, c)));
    }

    @Test
    public void testRS2() {
        Set<Integer> a = Set.of(2412);
        Set<Integer> b = Set.of(2437);
        Set<Integer> c = Set.of(5180);
        assertEquals(Collections.emptyList(), SweepClassifier.referenceSet(List.of(a, b, c)));
    }

    @Test
    public void testRS3() {
        Set<Integer> a = Set.of(2412);
        Set<Integer> b = Set.of(2437);
        assertThrows(IllegalArgumentException.class, () -> SweepClassifier.referenceSet(List.of(a, b)));
        assertThrows(IllegalArgumentException.class, () -> SweepClassifier.classify(Collections.emptySet(), Set.of(2412)));
    }

    // --- SW-1..3 and FR-1 vectors ---

    @Test
    public void testSW1() {
        Set<Integer> s1 = Set.of(2412, 2437, 2462, 5180, 5200);
        Set<Integer> s2 = Set.of(2412, 2437, 5180, 5200, 5220, 5745);
        Set<Integer> s3 = Set.of(2412, 2462, 5180, 5745, 5765);
        List<Integer> expected = List.of(2412, 2437, 2462, 5180, 5200, 5745);
        assertEquals(expected, SweepClassifier.referenceSet(List.of(s1, s2, s3)));
    }

    @Test
    public void testSW2() {
        Set<Integer> ref = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Set<Integer> sweep6 = Set.of(1, 2, 3, 4, 5, 6);
        Set<Integer> sweep5 = Set.of(1, 2, 3, 4, 5, 101, 102, 103, 104, 105);

        assertEquals(SweepClass.FULL, SweepClassifier.classify(ref, sweep6));
        assertEquals(SweepClass.PARTIAL, SweepClassifier.classify(ref, sweep5));
    }

    @Test
    public void testSW3() {
        List<Integer> refList = List.of(2412, 2437, 2462, 5180, 5200, 5745);
        Set<Integer> sweepA = Set.of(2412, 2437, 5180, 5745);
        Set<Integer> sweepB = Set.of(2412, 2437, 5180, 5220);

        assertEquals(SweepClass.FULL, SweepClassifier.classify(refList, sweepA));
        assertEquals(SweepClass.PARTIAL, SweepClassifier.classify(refList, sweepB));
    }

    @Test
    public void testFR1() {
        long tapMono = 100_000_000L;
        assertFalse(Freshness.isFresh(99_999_999L, tapMono));
        assertTrue(Freshness.isFresh(100_000_000L, tapMono));
    }

    // --- SL-1 ScanListModel vector ---

    @Test
    public void testSL1() {
        WifiObservation obs1 = new WifiObservation("aa:00:00:00:00:01", "eduroam", 5180, -60, 1000L);
        WifiObservation obs2 = new WifiObservation("aa:00:00:00:00:02", "eduroam", 2437, -60, 1000L);
        WifiObservation obs3 = new WifiObservation("bb:00:00:00:00:03", "", 2412, -75, 1000L);
        ScanBatch batch = new ScanBatch(List.of(obs3, obs2, obs1), 1000L, Tier.CALLBACK, true, false, 0);

        List<ScanListModel.Row> rows = ScanListModel.build(batch, "bb:00:00:00:00:03");
        assertEquals(3, rows.size());

        assertEquals("aa:00:00:00:00:01", rows.get(0).bssid());
        assertEquals("aa:00:00:00:00:02", rows.get(1).bssid());
        assertEquals("bb:00:00:00:00:03", rows.get(2).bssid());

        assertFalse(rows.get(0).isConnected());
        assertFalse(rows.get(1).isConnected());
        assertTrue(rows.get(2).isConnected());

        assertEquals("eduroam", rows.get(0).ssidDisplay());
        assertEquals("eduroam", rows.get(1).ssidDisplay());
        assertNull(rows.get(2).ssidDisplay());
    }

    // --- EV-1..5 EnvironmentAdvisor vectors ---

    @Test
    public void testEV1() {
        EnvironmentAdvisor.Advice advice = EnvironmentAdvisor.advise(
                true, true, true, EnvironmentAdvisor.ThrottleState.OFF);
        assertEquals(Collections.emptyList(), advice.issues());
        assertTrue(advice.isReady());
    }

    @Test
    public void testEV2() {
        EnvironmentAdvisor.Advice advice = EnvironmentAdvisor.advise(
                false, false, false, EnvironmentAdvisor.ThrottleState.ON);
        List<EnvironmentAdvisor.Issue> expected = List.of(
                EnvironmentAdvisor.Issue.PERMISSION_MISSING,
                EnvironmentAdvisor.Issue.LOCATION_SERVICES_OFF,
                EnvironmentAdvisor.Issue.WIFI_OFF,
                EnvironmentAdvisor.Issue.THROTTLE_ON
        );
        assertEquals(expected, advice.issues());
        assertFalse(advice.isReady());
    }

    @Test
    public void testEV3() {
        EnvironmentAdvisor.Advice advice = EnvironmentAdvisor.advise(
                true, true, true, EnvironmentAdvisor.ThrottleState.UNKNOWN);
        assertEquals(List.of(EnvironmentAdvisor.Issue.THROTTLE_UNKNOWN), advice.issues());
        assertTrue(advice.isReady());
    }

    @Test
    public void testEV4() {
        EnvironmentAdvisor.Advice advice = EnvironmentAdvisor.advise(
                true, true, true, EnvironmentAdvisor.ThrottleState.ON);
        assertEquals(List.of(EnvironmentAdvisor.Issue.THROTTLE_ON), advice.issues());
        assertTrue(advice.isReady());
    }

    @Test
    public void testEV5() {
        EnvironmentAdvisor.Advice advice = EnvironmentAdvisor.advise(
                true, false, true, EnvironmentAdvisor.ThrottleState.OFF);
        assertEquals(List.of(EnvironmentAdvisor.Issue.LOCATION_SERVICES_OFF), advice.issues());
        assertFalse(advice.isReady());
    }

    // --- TIER-1..6 ScanTierSelector vectors ---

    @Test
    public void testTIER1() {
        ScanTierSelector selector = new ScanTierSelector();
        selector.requestOk(0.0);
        selector.tierBatch(3.0);
        assertEquals(Tier.CALLBACK, selector.getTier());
        assertTrue(selector.isConfirmed());
    }

    @Test
    public void testTIER2() {
        ScanTierSelector selector = new ScanTierSelector();
        selector.requestOk(0.0);
        selector.pollAdvanced(11.9);
        assertEquals(Tier.CALLBACK, selector.getTier());
        selector.pollAdvanced(12.0);
        assertEquals(Tier.RECEIVER, selector.getTier());
    }

    @Test
    public void testTIER3() {
        ScanTierSelector selector = new ScanTierSelector();
        selector.requestOk(0.0);
        selector.pollAdvanced(12.0);
        assertEquals(Tier.RECEIVER, selector.getTier());
        selector.pollAdvanced(23.9);
        assertEquals(Tier.RECEIVER, selector.getTier());
        selector.pollAdvanced(24.0);
        assertEquals(Tier.POLL, selector.getTier());
    }

    @Test
    public void testTIER4() {
        ScanTierSelector selector = new ScanTierSelector();
        selector.registerFailed();
        assertEquals(Tier.RECEIVER, selector.getTier());
        selector.registerFailed();
        assertEquals(Tier.POLL, selector.getTier());
        selector.registerFailed();
        assertEquals(Tier.POLL, selector.getTier());
    }

    @Test
    public void testTIER5() {
        ScanTierSelector selector = new ScanTierSelector();
        selector.requestOk(0.0);
        selector.tierBatch(2.0);
        selector.pollAdvanced(100.0);
        assertEquals(Tier.CALLBACK, selector.getTier());
        assertTrue(selector.isConfirmed());
    }

    @Test
    public void testTIER6() {
        ScanTierSelector selector = new ScanTierSelector();
        selector.pollAdvanced(50.0);
        assertEquals(Tier.CALLBACK, selector.getTier());
        selector.requestOk(60.0);
        selector.pollAdvanced(71.9);
        assertEquals(Tier.CALLBACK, selector.getTier());
    }

    @Test
    public void testTIERF1() {
        ScanTierSelector selector = ScanTierSelector.startingAt(Tier.POLL);
        assertEquals(Tier.POLL, selector.getTier());
        assertTrue(selector.isConfirmed());
        selector.registerFailed();
        selector.pollAdvanced(100.0);
        assertEquals(Tier.POLL, selector.getTier());
    }

    // --- SCH-1..8 ScanScheduler vectors ---

    @Test
    public void testSCH1() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        assertEquals(List.of("REQUEST_SCAN"), stringify(s.onStart(0)));
        assertEquals(List.of("WAKE_AT(15010)"), stringify(s.onIssued(10, true)));
        assertEquals(List.of("DELIVER#1", "WAKE_AT(3500)"), stringify(s.onResult(3000, 1)));
        assertEquals(List.of("REQUEST_SCAN"), stringify(s.onWake(3500)));
    }

    @Test
    public void testSCH2() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        s.onStart(0);
        s.onIssued(10, true);
        s.onResult(3000, 1);
        s.onWake(3500);

        assertEquals(List.of("WAKE_AT(18510)"), stringify(s.onIssued(3510, true)));
        assertEquals(Collections.emptyList(), s.onWake(15010));
    }

    @Test
    public void testSCH3() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        assertEquals(List.of("REQUEST_SCAN"), stringify(s.onStart(0)));
        assertEquals(List.of("WAKE_AT(15000)"), stringify(s.onIssued(0, true)));
        assertEquals(Collections.emptyList(), s.onWake(14999));
        assertEquals(List.of("STATUS_TIMEOUT", "REQUEST_SCAN"), stringify(s.onWake(15000)));
    }

    @Test
    public void testSCH4() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        assertEquals(List.of("REQUEST_SCAN"), stringify(s.onStart(0)));
        assertEquals(List.of("STATUS_REFUSED", "WAKE_AT(3005)"), stringify(s.onIssued(5, false)));
        assertEquals(Collections.emptyList(), s.onWake(3004));
        assertEquals(List.of("REQUEST_SCAN"), stringify(s.onWake(3005)));
    }

    @Test
    public void testSCH5() {
        ScanScheduler s1 = new ScanScheduler(ScanScheduler.Mode.ONE_SHOT);
        s1.onStart(0);
        s1.onIssued(0, true);
        assertEquals(List.of("DELIVER#1"), stringify(s1.onResult(2000, 1)));
        assertEquals(ScanScheduler.State.IDLE, s1.getState());
        assertEquals(Collections.emptyList(), s1.onResult(2100, 2));

        ScanScheduler s2 = new ScanScheduler(ScanScheduler.Mode.ONE_SHOT);
        s2.onStart(0);
        s2.onIssued(0, true);
        assertEquals(List.of("STATUS_TIMEOUT"), stringify(s2.onWake(15000)));
        assertEquals(ScanScheduler.State.IDLE, s2.getState());

        ScanScheduler s3 = new ScanScheduler(ScanScheduler.Mode.ONE_SHOT);
        s3.onStart(0);
        assertEquals(List.of("STATUS_REFUSED"), stringify(s3.onIssued(5, false)));
        assertEquals(ScanScheduler.State.IDLE, s3.getState());
    }

    @Test
    public void testSCH6() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        s.onStart(0);
        s.onIssued(10, true);
        s.onResult(3000, 1);
        assertEquals(ScanScheduler.State.COOLDOWN, s.getState());
        assertEquals(List.of("DELIVER#2"), stringify(s.onResult(3200, 2)));
        assertEquals(ScanScheduler.State.COOLDOWN, s.getState());
    }

    @Test
    public void testSCH7() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.CONTINUOUS);
        s.onStart(0);
        s.onIssued(0, true);
        assertEquals(ScanScheduler.State.WAITING, s.getState());
        assertEquals(Collections.emptyList(), s.onStop());
        assertEquals(ScanScheduler.State.IDLE, s.getState());
        assertEquals(Collections.emptyList(), s.onWake(15000));
    }

    @Test
    public void testSCH8() {
        ScanScheduler s = new ScanScheduler(ScanScheduler.Mode.ONE_SHOT);
        s.onStart(0);
        assertEquals(ScanScheduler.State.REQUESTING, s.getState());
        assertEquals(List.of("DELIVER#1"), stringify(s.onResult(100, 1)));
        assertEquals(ScanScheduler.State.IDLE, s.getState());
        assertEquals(Collections.emptyList(), s.onIssued(110, true));
    }
}
