package com.netfieldpro.core.trace;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;

public class TraceCodecTest {

    @Test
    public void testTR1_and_TR2_roundTripFixture() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("traces/sample_v1.nftrace");
        assertNotNull("Could not find sample_v1.nftrace", stream);
        byte[] originalBytes = stream.readAllBytes();
        stream.close();

        TraceReader reader = new TraceReader(new ByteArrayInputStream(originalBytes));
        TraceHeader header = reader.header();
        assertEquals(1, header.version());
        assertEquals("Pixel 7", header.deviceModel());
        assertEquals(34, header.sdk());
        assertEquals(5000000000L, header.monoEpochNs());
        assertEquals(1700000000000L, header.createdAtMs());

        List<TraceEvent> events = new ArrayList<>();
        TraceEvent e1 = reader.next();
        assertNotNull(e1);
        assertEquals(0L, e1.tNs());
        assertEquals("USER", e1.kind());
        assertEquals(1L, ((JsonInt) e1.data().get("seq")).value());
        events.add(e1);

        TraceEvent e2 = reader.next();
        assertNotNull(e2);
        assertEquals(1500000000L, e2.tNs());
        assertEquals("USER", e2.kind());
        assertEquals(2L, ((JsonInt) e2.data().get("seq")).value());
        events.add(e2);

        TraceEvent e3 = reader.next();
        assertNull(e3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TraceWriter writer = new TraceWriter(baos, header)) {
            for (TraceEvent event : events) {
                writer.write(event);
            }
        }

        assertArrayEquals(originalBytes, baos.toByteArray());
    }

    @Test
    public void testTR3_timeRegression() throws Exception {
        String input = """
                {"createdAtMs":1700000000000,"deviceModel":"Pixel 7","monoEpochNs":5000000000,"nftrace":1,"sdk":34}
                {"d":{"seq":1},"k":"USER","t":5}
                {"d":{"seq":2},"k":"USER","t":4}
                """;
        TraceReader reader = new TraceReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(reader.header());
        assertNotNull(reader.next());
        try {
            reader.next();
            fail("Expected TIME_REGRESSION");
        } catch (TraceFormatException e) {
            assertEquals(TraceFormatException.TIME_REGRESSION, e.getCode());
            assertEquals(3, e.getLineNo());
        }
    }

    @Test
    public void testTR4_unsupportedVersion() throws IOException {
        String input = "{\"createdAtMs\":1700000000000,\"deviceModel\":\"Pixel 7\",\"monoEpochNs\":5000000000,\"nftrace\":2,\"sdk\":34}\n";
        try {
            new TraceReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            fail("Expected UNSUPPORTED_VERSION");
        } catch (TraceFormatException e) {
            assertEquals(TraceFormatException.UNSUPPORTED_VERSION, e.getCode());
            assertEquals(1, e.getLineNo());
        }
    }

    @Test
    public void testTR5_missingHeader() throws IOException {
        String input = "{\"d\":{\"seq\":1},\"k\":\"USER\",\"t\":0}\n";
        try {
            new TraceReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            fail("Expected MISSING_HEADER");
        } catch (TraceFormatException e) {
            assertEquals(TraceFormatException.MISSING_HEADER, e.getCode());
            assertEquals(1, e.getLineNo());
        }
    }

    @Test
    public void testTR6_badEventWithoutK() throws Exception {
        String input = """
                {"createdAtMs":1700000000000,"deviceModel":"Pixel 7","monoEpochNs":5000000000,"nftrace":1,"sdk":34}
                {"d":{"seq":1},"t":0}
                """;
        TraceReader reader = new TraceReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(reader.header());
        try {
            reader.next();
            fail("Expected BAD_EVENT");
        } catch (TraceFormatException e) {
            assertEquals(TraceFormatException.BAD_EVENT, e.getCode());
            assertEquals(2, e.getLineNo());
        }
    }

    @Test
    public void testTR7_lineTooLong() throws IOException {
        int length = 1048577;
        byte[] bigLine = new byte[length + 1];
        Arrays.fill(bigLine, (byte) 'a');
        bigLine[length] = '\n';
        try {
            new TraceReader(new ByteArrayInputStream(bigLine));
            fail("Expected LINE_TOO_LONG");
        } catch (TraceFormatException e) {
            assertEquals(TraceFormatException.LINE_TOO_LONG, e.getCode());
            assertEquals(1, e.getLineNo());
        }
    }
}
