package com.netfieldpro.core.heat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class HeatCoreTest {

    // --- MD-1, MD-2, IM-1, MD-X1, MD-X2 vectors ---

    @Test
    public void testMD1() {
        int[] samples = new int[]{-60, -62, -61, -65};
        assertEquals(-615, Median.tenths(samples));
    }

    @Test
    public void testMD2() {
        int[] v1 = new int[]{-60, -62};
        assertEquals(-610, Median.tenths(v1));
    }

    @Test
    public void testMD_X1() {
        int[] single = new int[]{-70};
        assertEquals(-700, Median.tenths(single));
    }

    @Test
    public void testMD_X2() {
        int[] three = new int[]{-61, -60, -65};
        assertEquals(-610, Median.tenths(three));
    }

    @Test
    public void testMedian_emptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> Median.tenths(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.tenths(null));
    }

    @Test
    public void testIM1() {
        Map<String, Integer> measured = new LinkedHashMap<>();
        measured.put("bssid_a", -610);
        measured.put("bssid_b", -700);

        List<String> universe = List.of("bssid_a", "bssid_b", "bssid_c");
        List<Imputer.Entry> resolved = Imputer.resolve(measured, universe);

        assertEquals(3, resolved.size());

        assertEquals("bssid_a", resolved.get(0).bssid());
        assertEquals(-610, resolved.get(0).tenths());
        assertFalse(resolved.get(0).imputed());

        assertEquals("bssid_b", resolved.get(1).bssid());
        assertEquals(-700, resolved.get(1).tenths());
        assertFalse(resolved.get(1).imputed());

        assertEquals("bssid_c", resolved.get(2).bssid());
        assertEquals(-950, resolved.get(2).tenths());
        assertTrue(resolved.get(2).imputed());
    }

    // --- RR-1..3 RadiusPolicy vectors ---

    @Test
    public void testRR1() {
        double r = RadiusPolicy.rFromNearestNeighbors(new double[]{6.0, 6.0, 8.0});
        assertEquals(12.0, r, 1e-9);
        assertEquals(10.0, RadiusPolicy.rEff(r, 10.0), 1e-9);
        assertEquals(12.0, RadiusPolicy.rEff(r, 12.0), 1e-9);
    }

    @Test
    public void testRR2() {
        assertEquals(4.0, RadiusPolicy.rFromNearestNeighbors(new double[]{1.0, 1.0}), 1e-9);
        assertEquals(25.0, RadiusPolicy.rFromNearestNeighbors(new double[]{20.0, 20.0}), 1e-9);
        assertEquals(25.0, RadiusPolicy.r(List.of(new Sample("s1", 0, 0, -50, 1.0))), 1e-9);
    }

    @Test
    public void testRR3() {
        double r = RadiusPolicy.rFromNearestNeighbors(new double[]{3.0, 3.0, 5.0, 9.0});
        assertEquals(8.0, r, 1e-9);
    }

    @Test
    public void testRadiusPolicy_sampleGeometry() {
        // Build 3 samples where NN are 6, 6, 8:
        // s1 at (0, 0) -> nearest is s2 at (6, 0), d = 6
        // s2 at (6, 0) -> nearest is s1 at (0, 0), d = 6
        // s3 at (14, 0) -> nearest is s2 at (6, 0), d = 8
        List<Sample> samples = List.of(
                new Sample("s1", 0.0, 0.0, -50.0, 1.0),
                new Sample("s2", 6.0, 0.0, -60.0, 1.0),
                new Sample("s3", 14.0, 0.0, -70.0, 1.0)
        );
        double r = RadiusPolicy.r(samples);
        assertEquals(12.0, r, 1e-9);
    }

    // --- GR-1..4 Grid and Boundary vectors ---

    @Test
    public void testGR1() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(40.0, 0.0),
                new Vertex(40.0, 25.0),
                new Vertex(0.0, 25.0)
        ));
        Grid grid = GridBuilder.build(poly);
        assertEquals(0.5, grid.g(), 1e-9);
        assertEquals(80, grid.cols());
        assertEquals(50, grid.rows());
        assertEquals(4000, grid.totalCells());
    }

    @Test
    public void testGR2() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(200.0, 0.0),
                new Vertex(200.0, 100.0),
                new Vertex(0.0, 100.0)
        ));
        Grid grid = GridBuilder.build(poly);
        assertEquals(0.903371, grid.g(), 1e-6);
        assertEquals(222, grid.cols());
        assertEquals(111, grid.rows());
        assertEquals(24642, grid.totalCells());
    }

    @Test
    public void testGR3() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(2.5, 0.0),
                new Vertex(2.5, 2.0),
                new Vertex(0.0, 2.0)
        ));
        Grid grid = GridBuilder.build(poly);
        assertEquals(5, grid.cols());
        assertEquals(4, grid.rows());
        assertEquals(20, grid.totalCells());
    }

    @Test
    public void testGR4() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(10.0, 0.0),
                new Vertex(10.0, 10.0),
                new Vertex(0.0, 10.0)
        ));
        assertTrue("p1 (10, 5) on edge should be inside", poly.contains(10.0, 5.0));
        assertTrue("p2 (10, 10) vertex should be inside", poly.contains(10.0, 10.0));
        assertFalse("p3 (10.0000001, 5) just outside should be outside", poly.contains(10.0000001, 5.0));
    }

    // --- WL-1..10 WallKernel vectors ---

    private static final Map<String, Double> TEST_MATS = Map.of(
            "DRYWALL", 3.0,
            "GLASS", 2.5,
            "BRICK", 8.0,
            "CONCRETE", 15.0
    );

    @Test
    public void testWL1() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.0, -1.0), new Vertex(2.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(1, hit.count());
        assertEquals(3.0, hit.penaltyM(), 1e-9);
        assertFalse(hit.nearWallAtP());
    }

    @Test
    public void testWL2() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.0, 0.0), new Vertex(2.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(0, hit.count());
        assertEquals(0.0, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL3() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "BRICK", List.of(new Vertex(2.0, -1.0), new Vertex(2.0, 0.0), new Vertex(2.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(1, hit.count());
        assertEquals(8.0, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL4() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.0, 1.0), new Vertex(2.0, 0.0), new Vertex(3.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(0, hit.count());
        assertEquals(0.0, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL5() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(1.0, 0.0), new Vertex(3.0, 0.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(0, hit.count());
        assertEquals(0.0, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL6() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.00, -1.0), new Vertex(2.00, 1.0))),
                new WallChain("w2", "GLASS", List.of(new Vertex(2.04, -1.0), new Vertex(2.04, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(1, hit.count());
        assertEquals(3.0, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL7() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.00, -1.0), new Vertex(2.00, 1.0))),
                new WallChain("w2", "GLASS", List.of(new Vertex(2.06, -1.0), new Vertex(2.06, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(2, hit.count());
        assertEquals(5.5, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL8() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.0, -1.0), new Vertex(2.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(1.9, 0.0, 4.0, 0.0);
        assertEquals(1, hit.count());
        assertTrue(hit.nearWallAtP());
    }

    @Test
    public void testWL9() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(5.0, -1.0), new Vertex(5.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(0.0, 0.0, 4.0, 0.0);
        assertEquals(0, hit.count());
        assertEquals(0.0, hit.penaltyM(), 1e-9);
    }

    @Test
    public void testWL10() {
        WallKernel kernel = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.0, -1.0), new Vertex(2.0, 1.0)))
        ), TEST_MATS);
        WallHit hit = kernel.evaluate(2.0, 0.0, 4.0, 0.0);
        assertEquals(0, hit.count());
        assertEquals(0.0, hit.penaltyM(), 1e-9);
    }

    // --- ID-1..5 and IDW-X1..2 IdwEngine vectors ---

    @Test
    public void testID1() {
        List<Sample> samples = List.of(
                new Sample("S1", 0.0, 0.0, -50.0, 1.0),
                new Sample("S2", 10.0, 0.0, -70.0, 1.0),
                new Sample("S3", 0.0, 10.0, -60.0, 0.5)
        );
        IdwEngine engine = new IdwEngine(samples, null, 10.0);
        IdwResult res = engine.estimate(4.0, 0.0);
        assertFalse(res.masked());
        assertEquals(-56.1538, res.dbm(), 5e-5);
        assertEquals(2, res.candidates());
    }

    @Test
    public void testID2() {
        List<Sample> samples = List.of(
                new Sample("S1", 0.0, 0.0, -50.0, 1.0),
                new Sample("S2", 10.0, 0.0, -70.0, 0.5),
                new Sample("S3", 0.0, 10.0, -60.0, 0.5)
        );
        IdwEngine engine = new IdwEngine(samples, null, 10.0);
        IdwResult res = engine.estimate(4.0, 0.0);
        assertFalse(res.masked());
        assertEquals(-53.6364, res.dbm(), 5e-5);
        assertEquals(2, res.candidates());
    }

    @Test
    public void testID3() {
        List<Sample> samples = List.of(
                new Sample("S1", 0.0, 0.0, -50.0, 1.0),
                new Sample("S2", 10.0, 0.0, -70.0, 1.0),
                new Sample("S3", 0.0, 10.0, -60.0, 0.5)
        );
        WallKernel walls = new WallKernel(List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(2.0, -10.0), new Vertex(2.0, 10.0)))
        ), TEST_MATS);
        IdwEngine engine = new IdwEngine(samples, walls, 10.0);
        IdwResult res = engine.estimate(4.0, 0.0);
        assertFalse(res.masked());
        assertEquals(-61.5294, res.dbm(), 5e-5);
        assertEquals(2, res.candidates());
    }

    @Test
    public void testID4() {
        List<Sample> samples = List.of(
                new Sample("S1", 0.0, 0.0, -50.0, 1.0)
        );
        WallKernel walls = new WallKernel(List.of(
                new WallChain("w1", "CONCRETE", List.of(new Vertex(2.0, -10.0), new Vertex(2.0, 10.0)))
        ), TEST_MATS);
        IdwEngine engine = new IdwEngine(samples, walls, 10.0);
        IdwResult res = engine.estimate(4.0, 0.0);
        assertTrue(res.masked());
        assertTrue(Double.isNaN(res.dbm()));
        assertEquals(0, res.candidates());
    }

    @Test
    public void testID5() {
        List<Sample> samples = List.of(
                new Sample("S1", 0.0, 0.0, -50.0, 1.0),
                new Sample("S2", 10.0, 0.0, -70.0, 1.0),
                new Sample("S3", 0.0, 10.0, -60.0, 0.5)
        );
        IdwEngine engine = new IdwEngine(samples, null, 10.0);
        IdwResult res = engine.estimate(10.0, 0.0);
        assertFalse(res.masked());
        assertEquals(-70.0, res.dbm(), 1e-9);
    }

    @Test
    public void testIDW_X1_cap12() {
        // 20 samples arranged at distance 2m around (0, 0)
        List<Sample> samples = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double angle = (2.0 * Math.PI * i) / 20.0;
            double sx = 2.0 * Math.cos(angle);
            double sy = 2.0 * Math.sin(angle);
            samples.add(new Sample("s_" + i, sx, sy, -50.0 - i, 1.0));
        }
        IdwEngine engine = new IdwEngine(samples, null, 10.0);
        IdwResult res = engine.estimate(0.0, 0.0);
        assertFalse(res.masked());
        assertEquals(12, res.candidates());
    }

    @Test
    public void testIDW_X2_tieBreak() {
        // 11 samples at d=1.0, and 2 samples at d=2.0 ("s_tieA" with -40 dBm, "s_tieB" with -80 dBm)
        List<Sample> samples = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            samples.add(new Sample("s_fixed_" + i, 1.0, (double) i * 0.01, -50.0, 1.0));
        }
        // At d=2.0
        samples.add(new Sample("s_tieB", 2.0, 0.0, -80.0, 1.0));
        samples.add(new Sample("s_tieA", 2.0, 0.0, -40.0, 1.0));

        IdwEngine engine = new IdwEngine(samples, null, 10.0);
        IdwResult res = engine.estimate(0.0, 0.0);
        assertEquals(12, res.candidates());
        // If s_tieA was kept (-40), value will differ from if s_tieB was kept (-80)
        // With s_tieA: 11 samples at d=1.0 (w=1, v=-50 -> sum=-550, den=11)
        // 1 sample at d=2.0 (w=0.25, v=-40 -> sum=-10, den=0.25)
        // dbm = -560 / 11.25 = -49.77777...
        // With s_tieB (-80): w*v = -20 -> sum=-570, dbm = -570 / 11.25 = -50.6666...
        assertEquals(-49.7777, res.dbm(), 1e-3);
    }

    // --- IDX-1..3 and PERF-1 IdwSurface vectors ---

    private static record OracleCandidate(Sample sample, double dEff) {}

    private static IdwResult oracleEstimate(List<Sample> samples, WallKernel walls, double rEff, double cx, double cy) {
        if (samples.isEmpty() || rEff <= 0.0) {
            return new IdwResult(true, Double.NaN, 0);
        }
        Sample bestCoincident = null;
        for (Sample s : samples) {
            double dx = s.x() - cx;
            double dy = s.y() - cy;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < 1e-6) {
                if (bestCoincident == null || s.id().compareTo(bestCoincident.id()) < 0) {
                    bestCoincident = s;
                }
            }
        }
        if (bestCoincident != null) {
            return new IdwResult(false, bestCoincident.dbm(), 1);
        }
        List<OracleCandidate> candidates = new java.util.ArrayList<>();
        for (Sample s : samples) {
            double dx = s.x() - cx;
            double dy = s.y() - cy;
            double d = Math.sqrt(dx * dx + dy * dy);
            double penalty = (walls != null) ? walls.evaluate(s.x(), s.y(), cx, cy).penaltyM() : 0.0;
            double dEff = d + penalty;
            if (dEff <= rEff) {
                candidates.add(new OracleCandidate(s, dEff));
            }
        }
        if (candidates.isEmpty()) {
            return new IdwResult(true, Double.NaN, 0);
        }
        candidates.sort((c1, c2) -> {
            int cmp = Double.compare(c1.dEff, c2.dEff);
            if (cmp != 0) {
                return cmp;
            }
            return c1.sample.id().compareTo(c2.sample.id());
        });
        int k = Math.min(12, candidates.size());
        double num = 0.0;
        double den = 0.0;
        for (int i = 0; i < k; i++) {
            OracleCandidate c = candidates.get(i);
            double w = c.sample.weight() / (c.dEff * c.dEff);
            num += w * c.sample.dbm();
            den += w;
        }
        return new IdwResult(false, num / den, k);
    }

    @Test
    public void testIDX1_oracleEqualBitForBit() {
        java.util.Random rng = new java.util.Random(12345);
        List<Sample> samples = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double sx = rng.nextDouble() * 60.0;
            double sy = rng.nextDouble() * 40.0;
            double dbm = -40.0 - rng.nextDouble() * 50.0;
            double weight = 0.5 + rng.nextDouble() * 0.5;
            samples.add(new Sample("s_" + i, sx, sy, dbm, weight));
        }

        List<WallChain> chains = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double x1 = rng.nextDouble() * 60.0;
            double y1 = rng.nextDouble() * 40.0;
            double x2 = rng.nextDouble() * 60.0;
            double y2 = rng.nextDouble() * 40.0;
            chains.add(new WallChain("w_" + i, "DRYWALL", List.of(new Vertex(x1, y1), new Vertex(x2, y2))));
        }
        WallKernel walls = new WallKernel(chains, TEST_MATS);
        double rEff = 10.0;
        IdwEngine engine = new IdwEngine(samples, walls, rEff);

        for (int q = 0; q < 200; q++) {
            double qx = rng.nextDouble() * 60.0;
            double qy = rng.nextDouble() * 40.0;
            IdwResult oracle = oracleEstimate(samples, walls, rEff, qx, qy);
            IdwResult actual = engine.estimate(qx, qy);

            assertEquals("Query " + q + " masked mismatch", oracle.masked(), actual.masked());
            assertEquals("Query " + q + " candidates mismatch", oracle.candidates(), actual.candidates());
            if (!oracle.masked()) {
                assertEquals("Query " + q + " bit-for-bit dbm mismatch",
                        Double.doubleToRawLongBits(oracle.dbm()),
                        Double.doubleToRawLongBits(actual.dbm()));
            }
        }
    }

    @Test
    public void testIDX2_gridInside4000Cells() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(40.0, 0.0),
                new Vertex(40.0, 25.0),
                new Vertex(0.0, 25.0)
        ));
        Grid grid = GridBuilder.build(poly);
        assertEquals(4000, grid.totalCells());

        List<Sample> samples = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            samples.add(new Sample("s_" + i, 5.0 + i * 3.0, 5.0 + i * 1.5, -60.0 - i, 1.0));
        }
        IdwEngine engine = new IdwEngine(samples, null, 15.0);
        IdwSurface surface = IdwSurface.compute(grid, poly, engine, null);
        assertNotNull(surface);
        assertEquals(4000, surface.value() + surface.masked());
    }

    @Test
    public void testIDX3_cancelAfterRow3() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(40.0, 0.0),
                new Vertex(40.0, 25.0),
                new Vertex(0.0, 25.0)
        ));
        Grid grid = GridBuilder.build(poly);
        List<Sample> samples = List.of(new Sample("s1", 5.0, 5.0, -60.0, 1.0));
        IdwEngine engine = new IdwEngine(samples, null, 15.0);

        int[] rowCount = new int[1];
        IdwSurface surface = IdwSurface.compute(grid, poly, engine, () -> {
            rowCount[0]++;
            return rowCount[0] > 3;
        });
        assertNull(surface);
    }

    @Test
    public void testPERF1_benchmarkInformational() {
        Polygon poly = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(200.0, 0.0),
                new Vertex(200.0, 100.0),
                new Vertex(0.0, 100.0)
        ));
        Grid grid = GridBuilder.build(poly);
        assertTrue(grid.totalCells() <= 25000);
        assertTrue(grid.totalCells() >= 20000);

        java.util.Random rng = new java.util.Random(12345);
        List<Sample> samples = new java.util.ArrayList<>();
        for (int i = 0; i < 300; i++) {
            samples.add(new Sample("s_" + i, rng.nextDouble() * 200.0, rng.nextDouble() * 100.0, -50.0 - rng.nextDouble() * 40.0, 1.0));
        }

        List<WallChain> chains = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            double x1 = rng.nextDouble() * 200.0;
            double y1 = rng.nextDouble() * 100.0;
            double x2 = rng.nextDouble() * 200.0;
            double y2 = rng.nextDouble() * 100.0;
            chains.add(new WallChain("w_" + i, "DRYWALL", List.of(new Vertex(x1, y1), new Vertex(x2, y2))));
        }
        WallKernel walls = new WallKernel(chains, TEST_MATS);
        IdwEngine engine = new IdwEngine(samples, walls, 12.0);

        long startNs = System.nanoTime();
        IdwSurface surface = IdwSurface.compute(grid, poly, engine, null);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertNotNull(surface);
        System.out.println("PERF-1: " + grid.totalCells() + " cells, elapsed: " + elapsedMs + " ms");
        assertTrue("PERF-1 took " + elapsedMs + " ms, must be <= 5000 ms", elapsedMs <= 5000L);
    }

    // --- PL-1..5 and PL-X1..6 Palette vectors ---

    @Test
    public void testPL1() {
        assertEquals("#D73027", Palette.hex(PaletteId.TRAFFIC_LIGHT, -100.0));
        assertEquals("#1A9850", Palette.hex(PaletteId.TRAFFIC_LIGHT, -55.0));
        assertEquals("#FEE08B", Palette.hex(PaletteId.TRAFFIC_LIGHT, -75.0));
    }

    @Test
    public void testPL2() {
        int[] rgb = Palette.rgb(PaletteId.TRAFFIC_LIGHT, -71.0);
        assertEquals(200, rgb[0]);
        assertEquals(216, rgb[1]);
        assertEquals(118, rgb[2]);
        assertEquals("#C8D876", Palette.hex(rgb));
    }

    @Test
    public void testPL3() {
        int[] rgb = Palette.rgb(PaletteId.TRAFFIC_LIGHT, -60.0);
        assertEquals(76, rgb[0]);
        assertEquals(175, rgb[1]);
        assertEquals(87, rgb[2]);
        assertEquals("#4CAF57", Palette.hex(rgb));
    }

    @Test
    public void testPL4() {
        int[] rgb = Palette.rgb(PaletteId.VIRIDIS, -71.0);
        assertEquals(63, rgb[0]);
        assertEquals(172, rgb[1]);
        assertEquals(120, rgb[2]);
        assertEquals("#3FAC78", Palette.hex(rgb));
    }

    @Test
    public void testPL5() {
        assertEquals(0x000000, Palette.textOn("#FEE08B"));
        assertEquals(0xFFFFFF, Palette.textOn("#D73027"));
    }

    @Test
    public void testPL_X1_to_X5() {
        assertEquals("#1A9850", Palette.hex(PaletteId.TRAFFIC_LIGHT, -40.0));
        assertEquals("#D73027", Palette.hex(PaletteId.TRAFFIC_LIGHT, -100.0));
        assertEquals("#FDE725", Palette.hex(PaletteId.VIRIDIS, -40.0));
        assertEquals("#440154", Palette.hex(PaletteId.VIRIDIS, -100.0));
        assertEquals("#FC8D59", Palette.hex(PaletteId.TRAFFIC_LIGHT, -85.0));
    }

    @Test
    public void testPL_X6_textOn() {
        assertEquals(0x000000, Palette.textOn("#FFFFFF"));
        assertEquals(0xFFFFFF, Palette.textOn("#000000"));
        assertEquals(0xFFFFFF, Palette.textOn("#036434"));
    }

    @Test
    public void testPalette_nanThrows() {
        assertThrows(IllegalArgumentException.class, () -> Palette.rgb(PaletteId.TRAFFIC_LIGHT, Double.NaN));
    }

    // --- LG-1.. Legend vectors ---

    @Test
    public void testLegend_buckets() {
        // Latency: <=10000->0, <=25000->1, <=50000->2, <=100000->3, else 4
        assertEquals(0, Legend.latencyBucket(10000L));
        assertEquals(1, Legend.latencyBucket(10001L));
        assertEquals(3, Legend.latencyBucket(100000L));
        assertEquals(4, Legend.latencyBucket(100001L));

        // Jitter: <=5000, <=10000, <=15000, <=30000, else
        assertEquals(0, Legend.jitterBucket(5000L));
        assertEquals(4, Legend.jitterBucket(30001L));

        // Loss: 0->0, 1->1, >=2->2
        assertEquals(0, Legend.lossBucket(0));
        assertEquals(1, Legend.lossBucket(1));
        assertEquals(2, Legend.lossBucket(2));
        assertEquals(2, Legend.lossBucket(5));

        // PHY: >=600->0, >=300->1, >=150->2, >=50->3, else 4
        assertEquals(0, Legend.phyBucket(600));
        assertEquals(1, Legend.phyBucket(599));
        assertEquals(3, Legend.phyBucket(50));
        assertEquals(4, Legend.phyBucket(49));
    }

    @Test
    public void testLegend_colors() {
        // 3 buckets (loss)
        assertEquals("#1A9850", Legend.color(PaletteId.TRAFFIC_LIGHT, 0, 3));
        assertEquals("#FEE08B", Legend.color(PaletteId.TRAFFIC_LIGHT, 1, 3));
        assertEquals("#D73027", Legend.color(PaletteId.TRAFFIC_LIGHT, 2, 3));

        assertEquals("#FDE725", Legend.color(PaletteId.VIRIDIS, 0, 3));
        assertEquals("#21908C", Legend.color(PaletteId.VIRIDIS, 1, 3));
        assertEquals("#440154", Legend.color(PaletteId.VIRIDIS, 2, 3));
    }

    // --- SO-1..2 and SO-X1..2 SecondaryOverlap vectors ---

    @Test
    public void testSO1() {
        assertEquals(-71.0, SecondaryOverlap.second(new double[]{-60.0, -71.0, -80.0}), 1e-9);
    }

    @Test
    public void testSO2() {
        assertEquals(-95.0, SecondaryOverlap.second(new double[]{-60.0}), 1e-9);
    }

    @Test
    public void testSO_X1() {
        assertEquals(-71.0, SecondaryOverlap.second(new double[]{-60.0, -71.0, -80.0}), 1e-9);
    }

    @Test
    public void testSO_X2_surfaceAllNaN() {
        double[] g1 = new double[]{Double.NaN, Double.NaN};
        double[] g2 = new double[]{Double.NaN, Double.NaN};
        double[] surf = SecondaryOverlap.surface(List.of(g1, g2));
        assertEquals(2, surf.length);
        assertTrue(Double.isNaN(surf[0]));
        assertTrue(Double.isNaN(surf[1]));
    }

    @Test
    public void testSecondaryOverlap_surfaceMixed() {
        double[] g1 = new double[]{-60.0, -65.0, Double.NaN};
        double[] g2 = new double[]{-70.0, Double.NaN, Double.NaN};
        double[] surf = SecondaryOverlap.surface(List.of(g1, g2));
        assertEquals(3, surf.length);
        assertEquals(-70.0, surf[0], 1e-9);
        assertEquals(-95.0, surf[1], 1e-9);
        assertTrue(Double.isNaN(surf[2]));
    }

    // --- HP-1..3 HeatPipeline vectors ---

    @Test
    public void testHP1() {
        Polygon boundary = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(40.0, 0.0),
                new Vertex(40.0, 25.0),
                new Vertex(0.0, 25.0)
        ));

        List<Sample> samples = List.of(
                new Sample("s1", 5.0, 5.0, -50.0, 1.0),
                new Sample("s2", 35.0, 5.0, -60.0, 1.0),
                new Sample("s3", 5.0, 20.0, -70.0, 1.0),
                new Sample("s4", 35.0, 20.0, -80.0, 1.0),
                new Sample("s5", 20.0, 12.5, -65.0, 1.0),
                new Sample("s6", 12.0, 12.0, -58.0, 0.5)
        );

        HeatPipeline.Request req = new HeatPipeline.Request(
                boundary,
                null,
                samples,
                10.0
        );

        HeatPipeline.Result res = HeatPipeline.compute(req, 42L);
        assertNotNull(res);
        assertEquals(80, res.cols());
        assertEquals(50, res.rows());
        assertEquals(4000, res.inside());
        assertEquals(42L, res.computeMs());
        assertTrue(res.meeting(-70.0) > 0);
    }

    @Test
    public void testHP2() {
        Polygon triangle = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(20.0, 0.0),
                new Vertex(0.0, 20.0)
        ));

        List<Sample> samples = List.of(
                new Sample("s1", 5.0, 5.0, -50.0, 1.0),
                new Sample("s2", 10.0, 5.0, -60.0, 1.0)
        );

        HeatPipeline.Request req = new HeatPipeline.Request(
                triangle,
                null,
                samples,
                10.0
        );

        HeatPipeline.Result res = HeatPipeline.compute(req);
        assertNotNull(res);
        assertTrue(res.cols() > 0);
        assertTrue(res.rows() > 0);
        assertTrue(res.inside() > 0);
    }

    @Test
    public void testHP3() {
        Polygon boundary = new Polygon(List.of(
                new Vertex(0.0, 0.0),
                new Vertex(40.0, 0.0),
                new Vertex(40.0, 25.0),
                new Vertex(0.0, 25.0)
        ));

        List<Sample> samples = List.of(
                new Sample("s1", 5.0, 5.0, -50.0, 1.0),
                new Sample("s2", 35.0, 5.0, -60.0, 1.0),
                new Sample("s3", 5.0, 20.0, -70.0, 1.0),
                new Sample("s4", 35.0, 20.0, -80.0, 1.0),
                new Sample("s5", 20.0, 12.5, -65.0, 1.0),
                new Sample("s6", 12.0, 12.0, -58.0, 0.5)
        );

        HeatPipeline.Request reqNoWalls = new HeatPipeline.Request(
                boundary,
                null,
                samples,
                10.0
        );
        HeatPipeline.Result resNoWalls = HeatPipeline.compute(reqNoWalls);
        assertNotNull(resNoWalls);

        List<WallChain> walls = List.of(
                new WallChain("w1", "DRYWALL", List.of(new Vertex(20.0, 0.0), new Vertex(20.0, 25.0)))
        );
        HeatPipeline.Request reqWithWalls = new HeatPipeline.Request(
                boundary,
                walls,
                samples,
                10.0
        );
        HeatPipeline.Result resWithWalls = HeatPipeline.compute(reqWithWalls);
        assertNotNull(resWithWalls);

        // Cell centers (19.75, 12.25) -> row 24, col 39 -> idx 24*80+39 = 1959
        // Cell centers (20.25, 12.25) -> row 24, col 40 -> idx 24*80+40 = 1960
        int idxLeft = 24 * 80 + 39;
        int idxRight = 24 * 80 + 40;

        double dbmLeftNoWall = resNoWalls.surface().dbm()[idxLeft];
        double dbmRightNoWall = resNoWalls.surface().dbm()[idxRight];
        double diffNoWall = Math.abs(dbmLeftNoWall - dbmRightNoWall);

        double dbmLeftWall = resWithWalls.surface().dbm()[idxLeft];
        double dbmRightWall = resWithWalls.surface().dbm()[idxRight];
        double diffWithWall = Math.abs(dbmLeftWall - dbmRightWall);

        assertTrue("Expected diff with walls (" + diffWithWall + ") > diff without walls (" + diffNoWall + ")",
                diffWithWall > diffNoWall);
    }
}
