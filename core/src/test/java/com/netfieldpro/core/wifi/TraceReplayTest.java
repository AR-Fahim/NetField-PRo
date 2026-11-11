package com.netfieldpro.core.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.netfieldpro.core.trace.TraceHeader;
import com.netfieldpro.core.trace.TraceReader;
import com.netfieldpro.core.trace.TraceWriter;

public class TraceReplayTest {

    public static final String EXPECTED_FIXTURE_SHA256 =
            "c7628965e289b1bab4839b5e71ade0c75eb5fd7923cf86259dd1531151aa41f2";

    public static List<ScanBatch> canonicalBatches() {
        WifiObservation b1o1 = new WifiObservation("aa:bb:cc:dd:ee:01", "eduroam", 5180, -58, 999900000L);
        WifiObservation b1o2 = new WifiObservation("aa:bb:cc:dd:ee:02", "eduroam", 2412, -65, 999850000L);
        WifiObservation b1o3 = new WifiObservation("bb:cc:dd:ee:ff:01", "", 2437, -72, 999800000L);
        ScanBatch batch1 = new ScanBatch(List.of(b1o1, b1o2, b1o3), 1000000000L, Tier.CALLBACK, true, false, 0);

        WifiObservation b2o1 = new WifiObservation("aa:bb:cc:dd:ee:01", "eduroam", 5180, -55, 1999900000L);
        WifiObservation b2o2 = new WifiObservation("aa:bb:cc:dd:ee:02", "eduroam", 2412, -68, 1999850000L);
        WifiObservation b2o3 = new WifiObservation("cc:dd:ee:ff:00:01", "Guest", 5745, -80, 1999800000L);
        ScanBatch batch2 = new ScanBatch(List.of(b2o1, b2o2, b2o3), 2000000000L, Tier.CALLBACK, true, false, 0);

        WifiObservation b3o1 = new WifiObservation("aa:bb:cc:dd:ee:01", "eduroam", 5180, -60, 2999900000L);
        WifiObservation b3o2 = new WifiObservation("aa:bb:cc:dd:ee:03", "eduroam", 5955, -70, 2999850000L);
        ScanBatch batch3 = new ScanBatch(List.of(b3o1, b3o2), 3000000000L, Tier.RECEIVER, true, false, 0);

        WifiObservation b4o1 = new WifiObservation("aa:bb:cc:dd:ee:01", "eduroam", 5180, -62, 3999900000L);
        WifiObservation b4o2 = new WifiObservation("bb:cc:dd:ee:ff:01", "", 2437, -75, 3999800000L);
        ScanBatch batch4 = new ScanBatch(List.of(b4o1, b4o2), 4000000000L, Tier.POLL, true, false, 0);

        return List.of(batch1, batch2, batch3, batch4);
    }

    @Test
    public void testRT1_recordWriteReadReplayYieldsEqualBatches() throws Exception {
        List<ScanBatch> expected = canonicalBatches();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TraceHeader header = new TraceHeader(1, "Pixel 7", 34, 1700000000000L, 5000000000L);
        try (TraceWriter writer = new TraceWriter(baos, header)) {
            FakeWifiScanSource fake = new FakeWifiScanSource(expected);
            RecordingWifiScanSource recording = new RecordingWifiScanSource(fake, writer);
            recording.start(batch -> {});
            for (int i = 0; i < expected.size(); i++) {
                fake.emitNext();
            }
            recording.stop();
        }

        byte[] traceBytes = baos.toByteArray();
        TraceReader reader = new TraceReader(new ByteArrayInputStream(traceBytes));
        TraceReplayWifiScanSource replay = TraceReplayWifiScanSource.fromTraceReader(reader);

        List<ScanBatch> replayed = new ArrayList<>();
        replay.start(replayed::add);
        replay.playAll();
        replay.stop();

        assertEquals(expected, replayed);
    }

    @Test
    public void testRT2_fixtureSha256MatchesReplay() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("traces/wifi_sample_v1.nftrace");
        assertNotNull("Could not find traces/wifi_sample_v1.nftrace on classpath", stream);

        byte[] bytes = stream.readAllBytes();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String sha256 = sb.toString();
        assertEquals(EXPECTED_FIXTURE_SHA256, sha256);

        TraceReader reader = new TraceReader(new ByteArrayInputStream(bytes));
        TraceReplayWifiScanSource replay = TraceReplayWifiScanSource.fromTraceReader(reader);

        List<ScanBatch> replayed = new ArrayList<>();
        replay.start(replayed::add);
        replay.playAll();
        replay.stop();

        assertEquals(canonicalBatches(), replayed);
    }
}
