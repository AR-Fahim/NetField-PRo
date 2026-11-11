package com.netfieldpro.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.ConnectionEvent;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.entity.LinkSample;
import com.netfieldpro.core.model.entity.Measurement;
import com.netfieldpro.core.model.entity.PathSegment;
import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.core.model.entity.Scan;
import com.netfieldpro.core.model.entity.SurveyPoint;
import com.netfieldpro.core.model.enums.ConnectionEventType;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.model.enums.MeasurementSource;
import com.netfieldpro.core.model.enums.PathMode;
import com.netfieldpro.core.model.enums.PathState;
import com.netfieldpro.core.model.enums.PointQuality;
import com.netfieldpro.core.model.enums.PointSource;
import com.netfieldpro.core.model.enums.Protocol;
import com.netfieldpro.core.model.enums.SweepClass;
import com.netfieldpro.core.model.serial.ConnectionEventSerializer;
import com.netfieldpro.core.model.serial.LatencyResultSerializer;
import com.netfieldpro.core.model.serial.LinkSampleSerializer;
import com.netfieldpro.core.model.serial.MeasurementSerializer;
import com.netfieldpro.core.model.serial.PathSegmentSerializer;
import com.netfieldpro.core.model.serial.RawSampleSerializer;
import com.netfieldpro.core.model.serial.ScanSerializer;
import com.netfieldpro.core.model.serial.SurveyPointSerializer;

public class GroupBTest {

    private static final String ID_B = "BBBBBBBBBBBBBBBBBBBBBB";
    private static final String ID_C = "CCCCCCCCCCCCCCCCCCCCCC";
    private static final String ID_D = "DDDDDDDDDDDDDDDDDDDDDD";
    private static final String ID_E = "EEEEEEEEEEEEEEEEEEEEEE";

    @Test
    public void testGE2_LinkSample() {
        LinkSample sample = new LinkSample(
                ID_B, ID_C, 1500000000L, "aa:bb:cc:dd:ee:01", "Eduroam", -67, 5180, null, 433
        );
        JsonObject json = LinkSampleSerializer.toJson(sample);
        byte[] bytes = CanonicalJsonWriter.toBytes(json);
        assertEquals("{\"bssid\":\"aa:bb:cc:dd:ee:01\",\"freqMHz\":5180,\"id\":\"BBBBBBBBBBBBBBBBBBBBBB\",\"observedAtNs\":1500000000,\"rssi\":-67,\"rxMbps\":433,\"sessionId\":\"CCCCCCCCCCCCCCCCCCCCCC\",\"ssid\":\"Eduroam\"}",
                new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testGE3_Measurement() {
        Measurement m = new Measurement(
                ID_D, ID_E, Protocol.WIFI, "aa:bb:cc:dd:ee:01", "Eduroam", 5180, 36, null,
                MeasurementSource.SCAN, "wifi-builtin", 4, -61.5, -65, -60, null, null
        );
        JsonObject json = MeasurementSerializer.toJson(m);
        byte[] bytes = CanonicalJsonWriter.toBytes(json);
        assertEquals("{\"bssidOrId\":\"aa:bb:cc:dd:ee:01\",\"channel\":36,\"freqMHz\":5180,\"id\":\"DDDDDDDDDDDDDDDDDDDDDD\",\"n\":4,\"pointId\":\"EEEEEEEEEEEEEEEEEEEEEE\",\"protocol\":\"WIFI\",\"receiverId\":\"wifi-builtin\",\"rssiMax\":-60,\"rssiMedian\":\"-61.5\",\"rssiMin\":-65,\"source\":\"SCAN\",\"ssid\":\"Eduroam\"}",
                new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testSortTieBreak_LinkSample() {
        LinkSample s1 = new LinkSample("BBBBBBBBBBBBBBBBBBBBBB", ID_C, 1000L, "b1", "s1", -60, 2412, null, null);
        LinkSample s2 = new LinkSample("AAAAAAAAAAAAAAAAAAAAAA", ID_C, 1000L, "b2", "s2", -70, 2412, null, null);
        List<LinkSample> list = new ArrayList<>(List.of(s1, s2));
        list.sort(EntityRegistry.LINK_SAMPLE.sortComparator());
        assertEquals("AAAAAAAAAAAAAAAAAAAAAA", list.get(0).id());
        assertEquals("BBBBBBBBBBBBBBBBBBBBBB", list.get(1).id());
    }

    @Test
    public void testMeasurementSort() {
        // Measurement's key is (pointId, bssidOrId, source, receiverId, id)
        Measurement m1 = new Measurement(ID_D, ID_E, Protocol.WIFI, "b1", null, 2412, 1, null, MeasurementSource.SCAN, "rx", 1, -60.0, -60, -60, null, null);
        Measurement m2 = new Measurement(ID_C, ID_E, Protocol.WIFI, "b1", null, 2412, 1, null, MeasurementSource.ACTIVE_LINK, "rx", 1, -60.0, -60, -60, null, null);
        List<Measurement> list = new ArrayList<>(List.of(m1, m2));
        list.sort(EntityRegistry.MEASUREMENT.sortComparator());
        // ACTIVE_LINK comes before SCAN
        assertEquals(MeasurementSource.ACTIVE_LINK, list.get(0).source());
        assertEquals(MeasurementSource.SCAN, list.get(1).source());
    }

    @Test
    public void testLatencyResult_Nullability() {
        // recv = 0 omits min, avg, max, jitter
        LatencyResult r0 = new LatencyResult(ID_B, ID_E, LatencyMethod.ICMP, "target", "wlan0", LatencyScope.GATEWAY, 5, 0, null, null, null, null, 5, false, "b1", "s1", List.of());
        JsonObject j0 = LatencyResultSerializer.toJson(r0);
        assertNull(j0.get("minMs"));
        assertNull(j0.get("avgMs"));
        assertNull(j0.get("maxMs"));
        assertNull(j0.get("jitterMs"));

        // recv = 1 omits jitter only
        LatencyResult r1 = new LatencyResult(ID_B, ID_E, LatencyMethod.ICMP, "target", "wlan0", LatencyScope.GATEWAY, 5, 1, 10.0, 10.0, 10.0, null, 4, false, "b1", "s1", List.of(10.0));
        JsonObject j1 = LatencyResultSerializer.toJson(r1);
        assertEquals("10.000", ((com.netfieldpro.core.json.JsonString) j1.get("minMs")).value());
        assertNull(j1.get("jitterMs"));
    }

    @Test
    public void testInvariants_GroupB() {
        // Measurement n >= 1
        assertThrowsIllegalArgument(() -> new Measurement(ID_D, ID_E, Protocol.WIFI, "b1", null, 2412, 1, null, MeasurementSource.SCAN, "rx", 0, -60.0, -60, -60, null, null));
        // Measurement rssiMin <= rssiMedian <= rssiMax
        assertThrowsIllegalArgument(() -> new Measurement(ID_D, ID_E, Protocol.WIFI, "b1", null, 2412, 1, null, MeasurementSource.SCAN, "rx", 2, -50.0, -60, -55, null, null));

        // LatencyResult sent == recv + lossCount
        assertThrowsIllegalArgument(() -> new LatencyResult(ID_B, ID_E, LatencyMethod.ICMP, "target", "wlan0", LatencyScope.GATEWAY, 5, 2, 10.0, 10.0, 10.0, 0.0, 2, false, "b1", "s1", List.of(10.0, 10.0)));
        // LatencyResult rtts.size() == recv
        assertThrowsIllegalArgument(() -> new LatencyResult(ID_B, ID_E, LatencyMethod.ICMP, "target", "wlan0", LatencyScope.GATEWAY, 5, 2, 10.0, 10.0, 10.0, 0.0, 3, false, "b1", "s1", List.of(10.0)));

        // PathSegment >= 2 waypoints
        assertThrowsIllegalArgument(() -> new PathSegment(ID_B, ID_C, PathState.DONE, 0L, 10L, 0L, 10L, 1.0, List.of(new PathSegment.Waypoint(0, 0, 0L)), List.of(), PathMode.ROUTE));
        // PathSegment rawStart <= trimStart <= trimEnd <= rawEnd
        assertThrowsIllegalArgument(() -> new PathSegment(ID_B, ID_C, PathState.DONE, 5L, 10L, 0L, 10L, 1.0, List.of(new PathSegment.Waypoint(0, 0, 0L), new PathSegment.Waypoint(1, 1, 1L)), List.of(), PathMode.ROUTE));
    }

    @Test
    public void testKeyOrder_and_JsonRoundTrip_GroupB() throws Exception {
        Scan scan = EntityRegistry.SCAN.sampleFactory().get();
        JsonObject jScan = ScanSerializer.toJson(scan);
        assertStrictKeyOrder(jScan);
        assertEquals(scan, ScanSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jScan))));

        RawSample raw = EntityRegistry.RAW_SAMPLE.sampleFactory().get();
        JsonObject jRaw = RawSampleSerializer.toJson(raw);
        assertStrictKeyOrder(jRaw);
        RawSample fromJsonRaw = RawSampleSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jRaw)));
        assertEquals(raw.scanId(), fromJsonRaw.scanId());
        assertEquals(raw.bssid(), fromJsonRaw.bssid());

        SurveyPoint pt = EntityRegistry.SURVEY_POINT.sampleFactory().get();
        JsonObject jPt = SurveyPointSerializer.toJson(pt);
        assertStrictKeyOrder(jPt);
        assertEquals(pt, SurveyPointSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jPt))));

        Measurement m = EntityRegistry.MEASUREMENT.sampleFactory().get();
        JsonObject jM = MeasurementSerializer.toJson(m);
        assertStrictKeyOrder(jM);
        assertEquals(m, MeasurementSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jM))));

        LatencyResult lr = EntityRegistry.LATENCY_RESULT.sampleFactory().get();
        JsonObject jLr = LatencyResultSerializer.toJson(lr);
        assertStrictKeyOrder(jLr);
        assertEquals(lr, LatencyResultSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jLr))));

        LinkSample ls = EntityRegistry.LINK_SAMPLE.sampleFactory().get();
        JsonObject jLs = LinkSampleSerializer.toJson(ls);
        assertStrictKeyOrder(jLs);
        assertEquals(ls, LinkSampleSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jLs))));

        ConnectionEvent ce = EntityRegistry.CONNECTION_EVENT.sampleFactory().get();
        JsonObject jCe = ConnectionEventSerializer.toJson(ce);
        assertStrictKeyOrder(jCe);
        assertEquals(ce, ConnectionEventSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jCe))));

        PathSegment ps = EntityRegistry.PATH_SEGMENT.sampleFactory().get();
        JsonObject jPs = PathSegmentSerializer.toJson(ps);
        assertStrictKeyOrder(jPs);
        assertEquals(ps, PathSegmentSerializer.fromJson((JsonObject) JsonParser.parse(CanonicalJsonWriter.toBytes(jPs))));
    }

    private static void assertStrictKeyOrder(JsonObject obj) {
        String prev = null;
        for (Map.Entry<String, JsonValue> entry : obj.properties().entrySet()) {
            if (prev != null) {
                assertTrue("Keys not in strict UTF-16 ascending order: " + prev + " >= " + entry.getKey(),
                        prev.compareTo(entry.getKey()) < 0);
            }
            prev = entry.getKey();
            if (entry.getValue() instanceof JsonObject nested) {
                assertStrictKeyOrder(nested);
            }
        }
    }

    private static void assertThrowsIllegalArgument(Runnable r) {
        try {
            r.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(": "));
        }
    }
}
