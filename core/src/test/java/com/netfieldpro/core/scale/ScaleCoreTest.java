package com.netfieldpro.core.scale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScaleCoreTest {

    @Test
    public void testVectorSC1() {
        // Frozen values: SC-1 mpp 0.016641, uncertainty 5.588
        ScaleCalculator.ScaleResult res = ScaleCalculator.calculateLine(100, 100, 700, 500, 12.0, 2000, 1000);
        assertEquals("0.016641", res.mppFormatted());
        assertEquals("5.588", res.uncertaintyFormatted());
        assertTrue(res.spanOk());
    }

    @Test
    public void testVectorSC2() {
        // Frozen values: SC-2 mpp 0.020000, uncertainty 15.212
        ScaleCalculator.ScaleResult res = ScaleCalculator.calculateRoomWidth(40.0, 2000);
        assertEquals("0.020000", res.mppFormatted());
        assertEquals("15.212", res.uncertaintyFormatted());
        assertTrue(res.spanOk());
    }

    @Test
    public void testVectorSC3() {
        // Image 2000×1000: a line of length 299.9 px → TOO_SHORT. (0,0)→(300,0) → valid (inclusive)
        ScaleCalculator.ValidationStatus shortStatus = ScaleCalculator.validateLineSpan(0, 0, 299.9, 0, 2000, 1000);
        assertEquals(ScaleCalculator.ValidationStatus.TOO_SHORT, shortStatus);

        ScaleCalculator.ValidationStatus validStatus = ScaleCalculator.validateLineSpan(0, 0, 300.0, 0, 2000, 1000);
        assertEquals(ScaleCalculator.ValidationStatus.OK, validStatus);
    }

    @Test
    public void testVectorSC4() {
        // 0.49 m and 500.01 m → INVALID_DISTANCE. 0.5 and 500 → ok. NaN, 0 and negative → INVALID_DISTANCE
        assertEquals(ScaleCalculator.ValidationStatus.INVALID_DISTANCE, ScaleCalculator.validateDistance(0.49));
        assertEquals(ScaleCalculator.ValidationStatus.INVALID_DISTANCE, ScaleCalculator.validateDistance(500.01));
        assertEquals(ScaleCalculator.ValidationStatus.OK, ScaleCalculator.validateDistance(0.5));
        assertEquals(ScaleCalculator.ValidationStatus.OK, ScaleCalculator.validateDistance(500.0));
        assertEquals(ScaleCalculator.ValidationStatus.INVALID_DISTANCE, ScaleCalculator.validateDistance(Double.NaN));
        assertEquals(ScaleCalculator.ValidationStatus.INVALID_DISTANCE, ScaleCalculator.validateDistance(0.0));
        assertEquals(ScaleCalculator.ValidationStatus.INVALID_DISTANCE, ScaleCalculator.validateDistance(-5.0));
    }

    @Test
    public void testVectorSC5() {
        // Coincident points → TOO_SHORT
        assertEquals(ScaleCalculator.ValidationStatus.TOO_SHORT, ScaleCalculator.validateLineSpan(100, 100, 100, 100, 2000, 1000));
    }

    @Test
    public void testVectorSC6() {
        // metersBetween((0,0),(300,400), mpp 0.02) → 10.000
        assertEquals("10.000", ScaleCalculator.metersBetweenFormatted(0, 0, 300, 400, 0.02));
        assertEquals(10.0, ScaleCalculator.metersBetween(0, 0, 300, 400, 0.02), 1e-9);
    }

    @Test
    public void testAffine3x3() {
        Affine3x3 ident = Affine3x3.identity();
        double[] p = ident.transform(10.0, 20.0);
        assertEquals(10.0, p[0], 1e-9);
        assertEquals(20.0, p[1], 1e-9);

        Affine3x3 trans = Affine3x3.translate(5.0, -3.0);
        double[] pt = trans.transform(10.0, 20.0);
        assertEquals(15.0, pt[0], 1e-9);
        assertEquals(17.0, pt[1], 1e-9);

        double[] pInv = trans.inverseTransform(15.0, 17.0);
        assertEquals(10.0, pInv[0], 1e-9);
        assertEquals(20.0, pInv[1], 1e-9);

        // fromTwoPoints mapping (0,0)->(100,100) and (10,0)->(120,100) (scale 2, translate 100,100)
        Affine3x3 fit = Affine3x3.fromTwoPoints(0, 0, 10, 0, 100, 100, 120, 100);
        double[] mapped1 = fit.transform(0, 0);
        double[] mapped2 = fit.transform(10, 0);
        assertEquals(100.0, mapped1[0], 1e-6);
        assertEquals(100.0, mapped1[1], 1e-6);
        assertEquals(120.0, mapped2[0], 1e-6);
        assertEquals(100.0, mapped2[1], 1e-6);
    }
}
