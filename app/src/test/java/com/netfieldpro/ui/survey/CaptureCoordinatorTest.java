package com.netfieldpro.ui.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.net.Network;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.core.latency.LatencyStats;
import com.netfieldpro.core.latency.ProbeCandidate;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.sensor.FakeSensorSource;
import com.netfieldpro.core.time.FakeClock;
import com.netfieldpro.core.wifi.FakeWifiScanSource;
import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiObservation;
import com.netfieldpro.data.db.BatchedWriter;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.MeasurementRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveyPointRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;
import com.netfieldpro.data.db.entity.TargetNetworkRow;
import com.netfieldpro.latency.LatencyRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CaptureCoordinatorTest {

    private static final String PROJ_ID = "p1";
    private static final String FLOOR_ID = "f1";
    private static final String SESSION_ID = "s1";
    private static final String TARGET_SSID = "OfficeWiFi";

    private NetFieldDatabase db;
    private BatchedWriter writer;
    private FakeClock clock;
    private FakeWifiScanSource scanSource;
    private FakeSensorSource sensorSource;
    private TestTimeoutScheduler timeoutScheduler;
    private FakeLatencyRunner latencyRunner;
    private CaptureCoordinator coordinator;

    static class TestTimeoutScheduler implements CaptureCoordinator.TimeoutScheduler {
        Runnable scheduledRunnable;
        long delayMs;

        @Override
        public void schedule(Runnable runnable, long delayMs) {
            this.scheduledRunnable = runnable;
            this.delayMs = delayMs;
        }

        @Override
        public void cancel() {
            this.scheduledRunnable = null;
        }

        public void trigger() {
            if (scheduledRunnable != null) {
                Runnable r = scheduledRunnable;
                scheduledRunnable = null;
                r.run();
            }
        }
    }

    static class FakeLatencyRunner implements LatencyRunner {
        boolean running = false;

        @Override
        public void start(Network network, String gateway, String dns, Listener listener) {
            running = true;
            ProbeCandidate cand = ProbeCandidate.icmp("192.168.1.1");
            ProbeSequence.SequenceResult result = new ProbeSequence.SequenceResult(
                    new long[]{10_000L, 12_000L},
                    List.of(100L, 300L),
                    false,
                    false,
                    2,
                    new LatencyStats.Summary(11.0, 10.0, 12.0, 2.0, 2, 0)
            );
            listener.onCompleted(cand, result);
        }

        @Override
        public void cancel() {
            running = false;
        }

        @Override
        public void onNetworkLost() {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).allowMainThreadQueries().build();
        writer = new BatchedWriter(db);

        clock = new FakeClock(1_000_000_000L, 1_700_000_000_000L);
        scanSource = new FakeWifiScanSource();
        sensorSource = new FakeSensorSource();
        timeoutScheduler = new TestTimeoutScheduler();
        latencyRunner = new FakeLatencyRunner();

        // Seed project, floor, session, target network
        ProjectRow proj = new ProjectRow();
        proj.id = PROJ_ID;
        proj.name = "Test Project";
        proj.createdAtMs = clock.wallMs();
        db.netFieldDao().insertProject(proj);

        FloorRow floor = new FloorRow();
        floor.id = FLOOR_ID;
        floor.projectId = PROJ_ID;
        floor.metersPerPixel = 0.05;
        db.netFieldDao().insertFloor(floor);

        SurveySessionRow session = new SurveySessionRow();
        session.id = SESSION_ID;
        session.floorId = FLOOR_ID;
        session.name = "Survey 1";
        session.state = "DRAFT";
        session.createdAtMs = clock.wallMs();
        session.monoEpochNs = clock.monoNs();
        session.timingQuality = "GOOD";
        session.evidenceGrade = "VERDICT";
        session.probeMethod = "ICMP";
        session.referenceFreqsMhz = "[2412, 5180]";
        db.netFieldDao().insertSurveySession(session);

        TargetNetworkRow target = new TargetNetworkRow();
        target.id = "tn1";
        target.sessionId = SESSION_ID;
        target.ssid = TARGET_SSID;
        db.netFieldDao().insertTargetNetwork(target);

        coordinator = new CaptureCoordinator(
                SESSION_ID,
                0.05,
                List.of(2412, 5180),
                Set.of(TARGET_SSID),
                "ICMP",
                null,
                scanSource,
                sensorSource,
                latencyRunner,
                null, // probe null -> probeEnabled false in coordinator constructor or default
                writer,
                db,
                clock,
                timeoutScheduler
        );
    }

    @After
    public void tearDown() throws IOException {
        writer.close();
        db.close();
    }

    @Test
    public void testCC1_twoScansWithProbe_writesPointAndMeasurementsWithMedian() {
        coordinator.startCapture(100.0, 200.0);
        assertTrue(coordinator.isCapturing());

        // Emit first fresh full scan
        WifiObservation obs1a = new WifiObservation("AA:BB:CC:DD:EE:01", TARGET_SSID, 2412, -50, clock.monoNs());
        WifiObservation obs1b = new WifiObservation("AA:BB:CC:DD:EE:02", TARGET_SSID, 5180, -60, clock.monoNs());
        ScanBatch batch1 = new ScanBatch(List.of(obs1a, obs1b), clock.monoNs(), Tier.CALLBACK, true, false, 0);
        scanSource.emitBatch(batch1);

        // Advance clock and emit second fresh full scan
        clock.advanceMs(4000L);
        WifiObservation obs2a = new WifiObservation("AA:BB:CC:DD:EE:01", TARGET_SSID, 2412, -52, clock.monoNs());
        WifiObservation obs2b = new WifiObservation("AA:BB:CC:DD:EE:02", TARGET_SSID, 5180, -64, clock.monoNs());
        ScanBatch batch2 = new ScanBatch(List.of(obs2a, obs2b), clock.monoNs(), Tier.CALLBACK, true, false, 0);
        scanSource.emitBatch(batch2);

        // Flush writer transaction
        writer.flush();

        // Exactly 1 survey point
        assertEquals(1, db.netFieldDao().countSurveyPoints());
        List<SurveyPointRow> points = db.netFieldDao().getSurveyPointsForSession(SESSION_ID);
        assertEquals(1, points.size());
        SurveyPointRow pt = points.get(0);
        assertEquals(5.0, pt.x, 1e-4); // 100 * 0.05
        assertEquals(10.0, pt.y, 1e-4); // 200 * 0.05
        assertEquals("OK", pt.quality);
        assertEquals(2, pt.nScans);

        // 2 Scan rows
        List<ScanRow> scans = db.netFieldDao().getScansForSession(SESSION_ID);
        assertEquals(2, scans.size());

        // Correct Measurement medians
        List<MeasurementRow> measurements = db.netFieldDao().getMeasurementsForPoint(pt.id);
        assertEquals(2, measurements.size());

        MeasurementRow m1 = measurements.stream()
                .filter(m -> "AA:BB:CC:DD:EE:01".equals(m.bssidOrId))
                .findFirst()
                .orElse(null);
        assertNotNull(m1);
        assertEquals(-51.0, m1.rssiMedian, 1e-4); // median of -50 and -52 is -51.0
        assertEquals(-52, m1.rssiMin);
        assertEquals(-50, m1.rssiMax);
        assertEquals(2, m1.n);

        MeasurementRow m2 = measurements.stream()
                .filter(m -> "AA:BB:CC:DD:EE:02".equals(m.bssidOrId))
                .findFirst()
                .orElse(null);
        assertNotNull(m2);
        assertEquals(-62.0, m2.rssiMedian, 1e-4); // median of -60 and -64 is -62.0
    }

    @Test
    public void testCC2_timeoutWithOneScan_writesLowConfidencePoint() {
        coordinator.startCapture(50.0, 50.0);
        assertTrue(coordinator.isCapturing());

        // Emit one fresh full scan
        WifiObservation obs1 = new WifiObservation("AA:BB:CC:DD:EE:01", TARGET_SSID, 2412, -55, clock.monoNs());
        WifiObservation obs2 = new WifiObservation("AA:BB:CC:DD:EE:02", TARGET_SSID, 5180, -65, clock.monoNs());
        ScanBatch batch = new ScanBatch(List.of(obs1, obs2), clock.monoNs(), Tier.CALLBACK, true, false, 0);
        scanSource.emitBatch(batch);

        // Trigger timeout at 15 s
        clock.advanceMs(15000L);
        timeoutScheduler.trigger();

        writer.flush();

        assertEquals(1, db.netFieldDao().countSurveyPoints());
        List<SurveyPointRow> points = db.netFieldDao().getSurveyPointsForSession(SESSION_ID);
        assertEquals(1, points.size());
        assertEquals("LOW_CONFIDENCE", points.get(0).quality);
        assertEquals(1, points.get(0).nScans);
    }

    @Test
    public void testCC3_timeoutWithNoScans_noRowsWritten() {
        coordinator.startCapture(50.0, 50.0);
        assertTrue(coordinator.isCapturing());

        // No scans arrive; trigger timeout at 15 s
        clock.advanceMs(15000L);
        timeoutScheduler.trigger();

        writer.flush();

        assertEquals(0, db.netFieldDao().countSurveyPoints());
        assertEquals(0, db.netFieldDao().countScans());
        assertEquals(0, db.netFieldDao().countMeasurements());
    }
}
