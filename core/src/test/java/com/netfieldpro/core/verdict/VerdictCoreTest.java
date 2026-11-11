package com.netfieldpro.core.verdict;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.netfieldpro.core.json.JsonInt;
import com.netfieldpro.core.json.JsonObject;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.model.enums.EvidenceGrade;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.util.FixedDecimal;

public class VerdictCoreTest {

    @Test
    public void testVD1_officeStandardFailOnWorstPoint() {
        Inputs inputs = new Inputs(
                EvidenceGrade.VERDICT,
                200L,
                185L,
                0L,
                null,
                0L,
                0L,
                48000L,
                15000L,
                3L,
                300L,
                2L,
                5L,
                null
        );

        Result result = VerdictEngine.evaluate(PresetProfiles.OFFICE_STANDARD, inputs);

        assertEquals(Verdict.FAIL, result.verdict());
        assertEquals(CritStatus.MET, result.rows().get(CritId.COVERAGE.ordinal()).status());
        assertEquals(CritStatus.NOT_REQUIRED, result.rows().get(CritId.SECONDARY.ordinal()).status());
        assertEquals(CritStatus.MET, result.rows().get(CritId.P95_RTT.ordinal()).status());
        assertEquals(CritStatus.MET, result.rows().get(CritId.MEAN_JITTER.ordinal()).status());
        assertEquals(CritStatus.MET, result.rows().get(CritId.POOLED_LOSS.ordinal()).status());
        assertEquals(CritStatus.NOT_MET, result.rows().get(CritId.WORST_POINT_LOSS.ordinal()).status());
        assertEquals(CritStatus.NOT_REQUIRED, result.rows().get(CritId.STICKY.ordinal()).status());
        assertEquals(CritStatus.MET, result.rows().get(CritId.MASKED.ordinal()).status());

        long[] expectedC = new long[]{925L, -1L, 480L, 150L, 10L, 400L, -1L, 0L};
        assertArrayEquals(expectedC, VerdictEngine.cArray(result));
        assertArrayEquals(expectedC, result.cArray());
    }

    @Test
    public void testVD2_maskedIncomplete() {
        Inputs inputsIncomplete = new Inputs(
                EvidenceGrade.VERDICT,
                200L,
                185L,
                11L,
                null,
                0L,
                0L,
                48000L,
                15000L,
                3L,
                300L,
                2L,
                5L,
                null
        );

        Result result = VerdictEngine.evaluate(PresetProfiles.OFFICE_STANDARD, inputsIncomplete);
        assertEquals(Verdict.INCOMPLETE, result.verdict());
        assertEquals(CritStatus.NOT_EVALUATED, result.rows().get(CritId.MASKED.ordinal()).status());
        assertEquals(55L, result.rows().get(CritId.MASKED.ordinal()).c());

        // At exactly 5% (10 of 200), masked is MET and verdict is FAIL
        Inputs inputsBorderline = new Inputs(
                EvidenceGrade.VERDICT,
                200L,
                185L,
                10L,
                null,
                0L,
                0L,
                48000L,
                15000L,
                3L,
                300L,
                2L,
                5L,
                null
        );
        Result resBorderline = VerdictEngine.evaluate(PresetProfiles.OFFICE_STANDARD, inputsBorderline);
        assertEquals(Verdict.FAIL, resBorderline.verdict());
        assertEquals(CritStatus.MET, resBorderline.rows().get(CritId.MASKED.ordinal()).status());
        assertEquals(50L, resBorderline.rows().get(CritId.MASKED.ordinal()).c());
    }

    @Test
    public void testVD3_coveragePass() {
        Inputs inputsPass = new Inputs(
                EvidenceGrade.VERDICT,
                200L,
                180L,
                0L,
                null,
                0L,
                0L,
                48000L,
                15000L,
                3L,
                300L,
                1L,
                5L,
                null
        );

        Result result = VerdictEngine.evaluate(PresetProfiles.OFFICE_STANDARD, inputsPass);
        assertEquals(Verdict.PASS, result.verdict());
        assertEquals(CritStatus.MET, result.rows().get(CritId.COVERAGE.ordinal()).status());
        assertEquals(900L, result.rows().get(CritId.COVERAGE.ordinal()).c());
    }

    @Test
    public void testVD4_coverageNotMetAndDisplay() {
        Inputs inputs = new Inputs(
                EvidenceGrade.VERDICT,
                2000L,
                1799L,
                0L,
                null,
                0L,
                0L,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Result result = VerdictEngine.evaluate(PresetProfiles.OFFICE_STANDARD, inputs);
        CritResult cov = result.rows().get(CritId.COVERAGE.ordinal());
        assertEquals(CritStatus.NOT_MET, cov.status());
        assertEquals(899L, cov.c());

        String display = FixedDecimal.format(cov.c() / 10.0, 1);
        assertEquals("89.9", display);
    }

    @Test
    public void testVD5_indicativeGradeNotEligible() {
        Inputs inputs = new Inputs(
                EvidenceGrade.INDICATIVE,
                200L,
                185L,
                0L,
                null,
                0L,
                0L,
                48000L,
                15000L,
                3L,
                300L,
                1L,
                5L,
                null
        );

        Result result = VerdictEngine.evaluate(PresetProfiles.OFFICE_STANDARD, inputs);
        assertEquals(Verdict.NOT_ELIGIBLE, result.verdict());
    }

    @Test
    public void testVD6_warehouseLossThreshold() {
        // Case 1: 1 lost of 500 with lossPermille 2: 1 * 1000 <= 2 * 500 (1000 <= 1000) -> MET
        Inputs case1 = new Inputs(
                EvidenceGrade.VERDICT,
                100L,
                98L,
                0L,
                90L,
                0L,
                0L,
                25000L,
                8000L,
                1L,
                500L,
                1L,
                500L,
                0
        );
        Result res1 = VerdictEngine.evaluate(PresetProfiles.WAREHOUSE_ROBOTICS, case1);
        boolean case1Met = (res1.rows().get(CritId.POOLED_LOSS.ordinal()).status() == CritStatus.MET);
        assertTrue(case1Met);
        assertEquals(2L, res1.rows().get(CritId.POOLED_LOSS.ordinal()).c());

        // Case 2: 1 lost of 495 with lossPermille 2: 1 * 1000 <= 2 * 495 (1000 <= 990) -> NOT_MET
        Inputs case2 = new Inputs(
                EvidenceGrade.VERDICT,
                100L,
                98L,
                0L,
                90L,
                0L,
                0L,
                25000L,
                8000L,
                1L,
                495L,
                1L,
                495L,
                0
        );
        Result res2 = VerdictEngine.evaluate(PresetProfiles.WAREHOUSE_ROBOTICS, case2);
        boolean case2Met = (res2.rows().get(CritId.POOLED_LOSS.ordinal()).status() == CritStatus.MET);
        assertFalse(case2Met);
        assertEquals(3L, res2.rows().get(CritId.POOLED_LOSS.ordinal()).c());
    }

    @Test
    public void testVD7_cctvTapPinsCoverage() {
        // CCTV: threshold -65 dBm, 100% of TAP_PINS required.
        // Case 1: Pin best is -64 (>= -65) -> meeting = 1, total = 1 -> MET
        Inputs case1 = new Inputs(
                EvidenceGrade.VERDICT,
                100L,
                90L,
                0L,
                null,
                1L,
                1L,
                20000L,
                5000L,
                0L,
                100L,
                0L,
                100L,
                null
        );
        Result res1 = VerdictEngine.evaluate(PresetProfiles.CCTV_SURVEILLANCE, case1);
        boolean case1Met = (res1.rows().get(CritId.COVERAGE.ordinal()).status() == CritStatus.MET);
        assertTrue(case1Met);
        assertEquals(1000L, res1.rows().get(CritId.COVERAGE.ordinal()).c());

        // Case 2: Pin best is -66 (< -65) -> meeting = 0, total = 1 -> NOT_MET
        Inputs case2 = new Inputs(
                EvidenceGrade.VERDICT,
                100L,
                90L,
                0L,
                null,
                1L,
                0L,
                20000L,
                5000L,
                0L,
                100L,
                0L,
                100L,
                null
        );
        Result res2 = VerdictEngine.evaluate(PresetProfiles.CCTV_SURVEILLANCE, case2);
        boolean case2Met = (res2.rows().get(CritId.COVERAGE.ordinal()).status() == CritStatus.MET);
        assertFalse(case2Met);
        assertEquals(0L, res2.rows().get(CritId.COVERAGE.ordinal()).c());
    }

    @Test
    public void testPR1_idForLength() {
        assertEquals(22, PresetProfiles.idFor("OFFICE_STANDARD").length());
        assertEquals(22, PresetProfiles.idFor("WAREHOUSE_ROBOTICS").length());
        assertEquals(22, PresetProfiles.idFor("CCTV_SURVEILLANCE").length());
        assertEquals(22, PresetProfiles.idFor("SMART_HOME_IOT").length());
    }

    @Test
    public void testPR2_idForStability() {
        String id1 = PresetProfiles.idFor("OFFICE_STANDARD");
        String id2 = PresetProfiles.idFor("OFFICE_STANDARD");
        assertEquals(id1, id2);
    }

    @Test
    public void testPR3_idsDistinctAndLookup() {
        List<ProfileSpec> all = PresetProfiles.all();
        assertEquals(4, all.size());
        assertEquals("OFFICE_STANDARD", all.get(0).key());
        assertEquals("WAREHOUSE_ROBOTICS", all.get(1).key());
        assertEquals("CCTV_SURVEILLANCE", all.get(2).key());
        assertEquals("SMART_HOME_IOT", all.get(3).key());

        Set<String> ids = new HashSet<>();
        for (ProfileSpec spec : all) {
            String id = PresetProfiles.idFor(spec.key());
            assertEquals(22, id.length());
            assertTrue(ids.add(id));

            assertEquals(spec, PresetProfiles.byId(id));
            assertEquals(spec, PresetProfiles.byId(spec.key()));
        }
        assertEquals(4, ids.size());

        assertNull(PresetProfiles.byId("non_existent"));
        assertNull(PresetProfiles.byId(null));
    }

    @Test
    public void testThresholdsJson() {
        for (ProfileSpec spec : PresetProfiles.all()) {
            JsonObject json = PresetProfiles.thresholdsJson(spec);
            assertEquals(spec.covDbm(), ((JsonInt) json.get("covDbm")).value());
            assertEquals(spec.covPct(), ((JsonInt) json.get("covPct")).value());
            assertEquals(spec.covBasisTapPins() ? "TAP_PINS" : "AREA", ((JsonString) json.get("covBasis")).value());

            if (spec.secDbm() != null) {
                assertEquals((long) spec.secDbm(), ((JsonInt) json.get("secDbm")).value());
                assertEquals((long) spec.secPct(), ((JsonInt) json.get("secPct")).value());
            } else {
                assertNull(json.get("secDbm"));
                assertNull(json.get("secPct"));
            }

            assertEquals(spec.p95Ms(), ((JsonInt) json.get("p95Ms")).value());
            assertEquals(spec.jitMs(), ((JsonInt) json.get("jitMs")).value());
            assertEquals(spec.lossPermille(), ((JsonInt) json.get("lossPermille")).value());
            assertEquals(spec.worstLossPct(), ((JsonInt) json.get("worstLossPct")).value());
            assertEquals(spec.worstLossInclusive() ? "LE" : "LT", ((JsonString) json.get("worstLossOp")).value());
            assertEquals(spec.sticky(), ((JsonInt) json.get("sticky")).value());
            assertEquals(spec.maxInfluenceM(), ((JsonInt) json.get("maxInfluenceM")).value());
            assertEquals("BEST_ANY", ((JsonString) json.get("bandPolicy")).value());
        }
    }
}
