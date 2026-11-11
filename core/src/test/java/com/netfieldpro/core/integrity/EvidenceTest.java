package com.netfieldpro.core.integrity;

import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.entity.RawSample;
import com.netfieldpro.core.model.enums.Verdict;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class EvidenceTest {

    @Test
    public void testEv1ManifestDeterminism() {
        Evidence.Part part1 = new Evidence.Part("core.json", "sha256_core_123");
        Evidence.Part part2 = new Evidence.Part("neighbors.json", "sha256_neighbors_456");
        Evidence.Part part3 = new Evidence.Part("floor.png", "sha256_floor_789");

        byte[] m1 = Evidence.partsManifest(1700000000000L, List.of(part1, part2, part3));
        byte[] m2 = Evidence.partsManifest(1700000000000L, List.of(part3, part1, part2));

        String sha1 = Evidence.evidenceSha256(m1);
        String sha2 = Evidence.evidenceSha256(m2);

        assertEquals(new String(m1), new String(m2));
        assertEquals(sha1, sha2);

        // One byte changed gives a different hash
        Evidence.Part part1Modified = new Evidence.Part("core.json", "sha256_core_124");
        byte[] m3 = Evidence.partsManifest(1700000000000L, List.of(part1Modified, part2, part3));
        String sha3 = Evidence.evidenceSha256(m3);
        assertNotEquals(sha1, sha3);

        // Timestamp changed gives a different hash
        byte[] m4 = Evidence.partsManifest(1700000000001L, List.of(part1, part2, part3));
        String sha4 = Evidence.evidenceSha256(m4);
        assertNotEquals(sha1, sha4);
    }

    private static String strOf(char ch, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ch);
        return sb.toString();
    }

    @Test
    public void testAssessmentBytesExcludesSig() {
        JsonObject profileSnapshot = new JsonObject();
        JsonObject thresholds = new JsonObject();
        JsonObject computedMetrics = new JsonObject();
        JsonArray criteria = new JsonArray();

        String aId = com.netfieldpro.core.util.IdGenerator.generate();
        String pId = com.netfieldpro.core.util.IdGenerator.generate();
        String ipId = com.netfieldpro.core.util.IdGenerator.generate();

        Assessment a1 = new Assessment(
                aId, "evidence-sha-1", pId, profileSnapshot,
                ipId, false, thresholds, computedMetrics, criteria,
                Verdict.PASS, 1700000000000L, "algo-1.0", 0.5,
                strOf('A', 44), strOf('B', 86)
        );

        Assessment a2 = new Assessment(
                aId, "evidence-sha-1", pId, profileSnapshot,
                ipId, false, thresholds, computedMetrics, criteria,
                Verdict.PASS, 1700000000000L, "algo-1.0", 0.5,
                strOf('A', 44), strOf('C', 86)
        );

        byte[] b1 = Evidence.assessmentBytes(a1);
        byte[] b2 = Evidence.assessmentBytes(a2);

        assertEquals(new String(b1), new String(b2));
        assertEquals(Evidence.assessmentHash(b1), Evidence.assessmentHash(b2));
    }

    @Test
    public void testNeighborsJsonRowIdOrdering() {
        String scanId = com.netfieldpro.core.util.IdGenerator.generate();
        RawSample s1 = new RawSample(10L, scanId, "02:00:00:00:00:01", "SSID1", 2412, -60, null);
        RawSample s2 = new RawSample(2L, scanId, "02:00:00:00:00:02", "SSID2", 2437, -65, null);
        RawSample s3 = new RawSample(5L, scanId, "02:00:00:00:00:03", "SSID3", 5180, -70, null);

        byte[] n1 = Evidence.neighborsJson(List.of(s1, s2, s3));
        byte[] n2 = Evidence.neighborsJson(List.of(s3, s1, s2));

        assertEquals(new String(n1), new String(n2));
    }
}
