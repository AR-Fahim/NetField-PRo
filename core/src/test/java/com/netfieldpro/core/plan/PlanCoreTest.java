package com.netfieldpro.core.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class PlanCoreTest {

    @Test
    public void testVectorOR1() {
        // Files {AAAAAAAAAAAAAAAAAAAAAA.png, BBBBBBBBBBBBBBBBBBBBBB.jpg}, floors {AAAAAAAAAAAAAAAAAAAAAA} -> orphans [BBBBBBBBBBBBBBBBBBBBBB.jpg]
        List<String> files = Arrays.asList("AAAAAAAAAAAAAAAAAAAAAA.png", "BBBBBBBBBBBBBBBBBBBBBB.jpg");
        Set<String> floors = Collections.singleton("AAAAAAAAAAAAAAAAAAAAAA");
        List<String> orphans = OrphanSweeper.findOrphans(files, floors);
        assertEquals(1, orphans.size());
        assertEquals("BBBBBBBBBBBBBBBBBBBBBB.jpg", orphans.get(0));
    }

    @Test
    public void testVectorOR2() {
        // X…X.tmp (22 chars) -> orphan. notes.txt and a 21-char name -> ignored
        String tmp22 = "XXXXXXXXXXXXXXXXXXXXXX.tmp";
        String notes = "notes.txt";
        String png21 = "AAAAAAAAAAAAAAAAAAAAA.png";
        List<String> files = Arrays.asList(tmp22, notes, png21);
        Set<String> floors = Collections.singleton("XXXXXXXXXXXXXXXXXXXXXX");

        List<String> orphans = OrphanSweeper.findOrphans(files, floors);
        assertEquals(1, orphans.size());
        assertEquals(tmp22, orphans.get(0));
    }

    @Test
    public void testVectorIMP1() {
        // Size 52,428,800 -> OK. 52,428,801 -> TOO_LARGE
        assertEquals(PlanImportValidator.Status.OK, PlanImportValidator.validateFileSize(52428800L));
        assertEquals(PlanImportValidator.Status.TOO_LARGE, PlanImportValidator.validateFileSize(52428801L));
    }

    @Test
    public void testVectorIMP2() {
        // image/gif -> UNSUPPORTED. image/png, image/jpeg and application/pdf -> ok
        assertEquals(PlanImportValidator.Status.UNSUPPORTED, PlanImportValidator.validateMimeType("image/gif"));
        assertEquals(PlanImportValidator.Status.OK, PlanImportValidator.validateMimeType("image/png"));
        assertEquals(PlanImportValidator.Status.OK, PlanImportValidator.validateMimeType("image/jpeg"));
        assertEquals(PlanImportValidator.Status.OK, PlanImportValidator.validateMimeType("application/pdf"));
    }

    @Test
    public void testVectorIMP3() {
        // Decoded 199×1000 -> TOO_SMALL. 200×200 -> OK. 0×0 -> DECODE_FAILED
        assertEquals(PlanImportValidator.Status.TOO_SMALL, PlanImportValidator.validateDimensions(199, 1000));
        assertEquals(PlanImportValidator.Status.OK, PlanImportValidator.validateDimensions(200, 200));
        assertEquals(PlanImportValidator.Status.DECODE_FAILED, PlanImportValidator.validateDimensions(0, 0));
        assertEquals(PlanImportValidator.Status.DECODE_FAILED, PlanImportValidator.validateDimensions(-10, 100));
    }

    @Test
    public void testVectorSP1() {
        // PNG 3000×2000, orient 1 -> STORE_ORIGINAL
        PlanStoragePolicy.Decision d = PlanStoragePolicy.decide("image/png", 3000, 2000, 1);
        assertEquals(PlanStoragePolicy.Action.STORE_ORIGINAL, d.action());
        assertEquals(3000, d.targetWidth());
        assertEquals(2000, d.targetHeight());
        assertEquals("png", d.targetFormat());
    }

    @Test
    public void testVectorSP2() {
        // PNG 5000×2000 -> REENCODE, target 4096×1638
        PlanStoragePolicy.Decision d = PlanStoragePolicy.decide("image/png", 5000, 2000, 1);
        assertEquals(PlanStoragePolicy.Action.REENCODE, d.action());
        assertEquals(4096, d.targetWidth());
        assertEquals(1638, d.targetHeight());
        assertEquals("png", d.targetFormat());
    }

    @Test
    public void testVectorSP3() {
        // JPG 3000×2000, orient 6 -> REENCODE_ROTATE, target 2000×3000
        PlanStoragePolicy.Decision d = PlanStoragePolicy.decide("image/jpeg", 3000, 2000, 6);
        assertEquals(PlanStoragePolicy.Action.REENCODE_ROTATE, d.action());
        assertEquals(2000, d.targetWidth());
        assertEquals(3000, d.targetHeight());
        assertEquals("png", d.targetFormat());
    }

    @Test
    public void testVectorSP4() {
        // JPG 3000×2000, orient 1 -> STORE_ORIGINAL
        PlanStoragePolicy.Decision d = PlanStoragePolicy.decide("image/jpeg", 3000, 2000, 1);
        assertEquals(PlanStoragePolicy.Action.STORE_ORIGINAL, d.action());
        assertEquals(3000, d.targetWidth());
        assertEquals(2000, d.targetHeight());
        assertEquals("jpg", d.targetFormat());
    }

    @Test
    public void testVectorSP5() {
        // PDF page 612×792 pt -> RASTERIZE, target 2374×3072
        PlanStoragePolicy.Decision d = PlanStoragePolicy.decide("application/pdf", 612, 792, 1);
        assertEquals(PlanStoragePolicy.Action.RASTERIZE, d.action());
        assertEquals(2374, d.targetWidth());
        assertEquals(3072, d.targetHeight());
        assertEquals("png", d.targetFormat());
    }

    @Test
    public void testVectorTP1() {
        // Image 4096×2048, overview long 2048, viewport whole image (0,0,4096,2048), screen long 1080 -> no tile
        TilePlanner.TilePlan plan = TilePlanner.plan(4096, 2048, 2048, 0, 0, 4096, 2048, 1080);
        assertFalse(plan.needsTile());
    }

    @Test
    public void testVectorTP2() {
        // Viewport (1000,500,2024,1012), screen long 1080 -> tile region (1000,500,2024,1012), sample 1
        TilePlanner.TilePlan plan = TilePlanner.plan(4096, 2048, 2048, 1000, 500, 2024, 1012, 1080);
        assertTrue(plan.needsTile());
        assertEquals(1000, plan.left());
        assertEquals(500, plan.top());
        assertEquals(2024, plan.right());
        assertEquals(1012, plan.bottom());
        assertEquals(1, plan.sampleSize());
    }

    @Test
    public void testVectorTP3() {
        // Viewport (−100,−50,924,462), screen long 1080 -> region clamped to (0,0,924,462)
        TilePlanner.TilePlan plan = TilePlanner.plan(4096, 2048, 2048, -100, -50, 924, 462, 1080);
        assertTrue(plan.needsTile());
        assertEquals(0, plan.left());
        assertEquals(0, plan.top());
        assertEquals(924, plan.right());
        assertEquals(462, plan.bottom());
    }

    @Test
    public void testVectorTP4() {
        // Viewport (0,0,3000,1500), screen long 2400 -> tile, region (0,0,3000,1500), sample 2
        TilePlanner.TilePlan plan = TilePlanner.plan(4096, 2048, 2048, 0, 0, 3000, 1500, 2400);
        assertTrue(plan.needsTile());
        assertEquals(0, plan.left());
        assertEquals(0, plan.top());
        assertEquals(3000, plan.right());
        assertEquals(1500, plan.bottom());
        assertEquals(2, plan.sampleSize());
    }

    @Test
    public void testVectorTP5() {
        // Viewport long N=2048, screen long 1024 (the ratio exactly equals the overview ratio 0.5) -> no tile (strict >)
        TilePlanner.TilePlan plan = TilePlanner.plan(4096, 2048, 2048, 0, 0, 2048, 1024, 1024);
        assertFalse(plan.needsTile());
    }

    @Test
    public void testVectorVP1() {
        // Image 2000×1000, view 1000×1000: fit scale 0.5, tx 0, ty 250. (0,0)→(0,250), (2000,1000)→(1000,750)
        ViewportController vp = new ViewportController(2000, 1000, 1000, 1000);
        assertEquals(0.5, vp.getFitScale(), 1e-9);
        assertEquals(0.5, vp.getScale(), 1e-9);
        assertEquals(0.0, vp.getTx(), 1e-9);
        assertEquals(250.0, vp.getTy(), 1e-9);

        assertEquals(0.0, vp.imageToViewX(0.0), 1e-9);
        assertEquals(250.0, vp.imageToViewY(0.0), 1e-9);
        assertEquals(1000.0, vp.imageToViewX(2000.0), 1e-9);
        assertEquals(750.0, vp.imageToViewY(1000.0), 1e-9);
    }

    @Test
    public void testVectorVP2() {
        // zoomAt(2.0, (500,500)) → scale 1.0, tx −500, ty 0
        ViewportController vp = new ViewportController(2000, 1000, 1000, 1000);
        vp.zoomAt(2.0, 500.0, 500.0);
        assertEquals(1.0, vp.getScale(), 1e-9);
        assertEquals(-500.0, vp.getTx(), 1e-9);
        assertEquals(0.0, vp.getTy(), 1e-9);
    }

    @Test
    public void testVectorVP3() {
        // zoomAt(100) → scale clamps to 8.0 (16× fit)
        ViewportController vp = new ViewportController(2000, 1000, 1000, 1000);
        vp.zoomAt(100.0);
        assertEquals(8.0, vp.getScale(), 1e-9);
    }

    @Test
    public void testVectorVP4() {
        // zoomAt(0.1) → clamps to 0.5 (fit)
        ViewportController vp = new ViewportController(2000, 1000, 1000, 1000);
        vp.zoomAt(0.1);
        assertEquals(0.5, vp.getScale(), 1e-9);
    }

    @Test
    public void testVectorVP5() {
        // At scale 1.0, pan(+10000, 0) → tx = 0; pan(−10000, 0) → tx = −1000; ty stays 0
        ViewportController vp = new ViewportController(2000, 1000, 1000, 1000);
        vp.zoomAt(2.0, 500.0, 500.0);
        assertEquals(1.0, vp.getScale(), 1e-9);

        vp.pan(10000.0, 0.0);
        assertEquals(0.0, vp.getTx(), 1e-9);
        assertEquals(0.0, vp.getTy(), 1e-9);

        vp.pan(-10000.0, 0.0);
        assertEquals(-1000.0, vp.getTx(), 1e-9);
        assertEquals(0.0, vp.getTy(), 1e-9);
    }
}
