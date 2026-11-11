package com.netfieldpro.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.db.entity.ProjectRow;
import com.netfieldpro.data.db.entity.RawSampleRow;
import com.netfieldpro.data.db.entity.ScanRow;
import com.netfieldpro.data.db.entity.SurveySessionRow;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BatchedWriterTest {

    private NetFieldDatabase db;
    private BatchedWriter writer;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                NetFieldDatabase.class
        ).addCallback(NetFieldDatabase.SEED_CALLBACK).allowMainThreadQueries().build();
        writer = new BatchedWriter(db);
    }

    @After
    public void tearDown() throws IOException {
        writer.close();
        db.close();
    }

    @Test
    public void testBW1_fifoOrder() {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        writer.submit(database -> order.add("A"), null);
        writer.submit(database -> order.add("B"), null);
        writer.submit(database -> order.add("C"), null);
        writer.flush();

        assertEquals(List.of("A", "B", "C"), order);
    }

    @Test
    public void testBW2_rollbackOnError() {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        ProjectRow p = new ProjectRow();
        p.id = "AAAAAAAAAAAAAAAAAAAAAA";
        p.name = "Test Project";
        p.createdAtMs = 1000L;

        writer.submit(database -> {
            database.netFieldDao().insertProject(p);
            throw new RuntimeException("boom");
        }, new WriteCallback() {
            @Override
            public void onSuccess() {
                fail("Should fail");
            }

            @Override
            public void onError(Throwable t) {
                captured.set(t);
            }
        });
        writer.flush();

        assertNotNull(captured.get());
        assertEquals("boom", captured.get().getMessage());
        assertEquals(0, db.netFieldDao().countProjects());
    }

    @Test
    public void testBW3_threadName() {
        AtomicReference<String> threadName = new AtomicReference<>();
        writer.submit(database -> threadName.set(Thread.currentThread().getName()), null);
        writer.flush();

        assertEquals("nf-db-writer", threadName.get());
    }

    @Test
    public void testBW4_flushAndClose() {
        AtomicBoolean finished = new AtomicBoolean(false);
        writer.submit(database -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            finished.set(true);
        }, null);
        writer.flush();
        assertTrue("flush must wait until batch finishes", finished.get());

        writer.close();
        try {
            writer.submit(database -> {}, null);
            fail("submit after close must throw IllegalStateException");
        } catch (IllegalStateException expected) {
            // Success
        }
    }

    @Test
    public void testBW5_batch1000Rollback() {
        ProjectRow p = new ProjectRow();
        p.id = "AAAAAAAAAAAAAAAAAAAAAA";
        p.name = "Project";
        p.createdAtMs = 1000L;
        db.netFieldDao().insertProject(p);

        FloorRow f = new FloorRow();
        f.id = "BBBBBBBBBBBBBBBBBBBBBB";
        f.projectId = p.id;
        f.levelIndex = 1L;
        f.planSha256 = "sha";
        f.planWpx = 100;
        f.planHpx = 100;
        f.metersPerPixel = 0.05;
        f.scaleMethod = "LINE";
        f.scaleUncertaintyPct = 1.0;
        f.scaleChecked = true;
        db.netFieldDao().insertFloor(f);

        SurveySessionRow s = new SurveySessionRow();
        s.id = "CCCCCCCCCCCCCCCCCCCCCC";
        s.floorId = f.id;
        s.name = "Session";
        s.state = "DRAFT";
        s.createdAtMs = 1000L;
        s.monoEpochNs = 1000L;
        s.schemaVersion = 1;
        s.appVersion = "1.0";
        s.algoVersion = "1.0";
        s.throttleAtStart = false;
        s.timingQuality = "GOOD";
        s.evidenceGrade = "VERDICT";
        s.sessionType = "STANDARD";
        s.intendedProfileId = "DDDDDDDDDDDDDDDDDDDDDD";
        s.intendedProfileChanges = 0L;
        s.rssiOffsetDb = 0.0;
        s.linkCadenceMs = 1000L;
        s.cadenceFlag = "DEFAULT";
        s.probeMethod = "ICMP";
        s.referenceFreqsMhz = "[]";
        s.receiverId = "rx";
        s.deviceInfoId = "EEEEEEEEEEEEEEEEEEEEEE";
        s.capabilityReport = "{}";
        db.netFieldDao().insertSurveySession(s);

        ScanRow scan = new ScanRow();
        scan.id = "FFFFFFFFFFFFFFFFFFFFFF";
        scan.sessionId = s.id;
        scan.sweepClass = "FULL";
        scan.startNs = 1000L;
        scan.endNs = 2000L;
        scan.arrivalNs = 2050L;
        scan.tsPlausible = true;
        scan.throttleOffAtScan = true;
        db.netFieldDao().insertScan(scan);

        AtomicReference<Throwable> captured = new AtomicReference<>();
        writer.submit(database -> {
            for (int i = 0; i < 1000; i++) {
                if (i == 999) {
                    throw new RuntimeException("fail at sample 1000");
                }
                RawSampleRow r = new RawSampleRow();
                r.scanId = scan.id;
                r.bssid = "aa:bb:cc:dd:ee:01";
                r.ssid = "TestSSID";
                r.freqMHz = 2412;
                r.rssi = -60;
                database.netFieldDao().insertRawSample(r);
            }
        }, new WriteCallback() {
            @Override
            public void onSuccess() {
                fail("Should fail");
            }

            @Override
            public void onError(Throwable t) {
                captured.set(t);
            }
        });
        writer.flush();

        assertNotNull(captured.get());
        assertEquals("fail at sample 1000", captured.get().getMessage());
        assertEquals(0, db.netFieldDao().countRawSamples());
    }
}

