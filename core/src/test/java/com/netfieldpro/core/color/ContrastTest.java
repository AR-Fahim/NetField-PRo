package com.netfieldpro.core.color;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContrastTest {

    private static final double TOLERANCE = 0.01;

    @Test
    public void testCT1() {
        int black = Contrast.parseHex("#000000");
        int white = Contrast.parseHex("#FFFFFF");
        assertEquals(21.00, Contrast.ratio(black, white), TOLERANCE);
    }

    @Test
    public void testCT2() {
        int white = Contrast.parseHex("#FFFFFF");
        assertEquals(1.00, Contrast.ratio(white, white), TOLERANCE);
    }

    @Test
    public void testCT3() {
        int gray = Contrast.parseHex("#767676");
        int white = Contrast.parseHex("#FFFFFF");
        assertEquals(4.54, Contrast.ratio(gray, white), TOLERANCE);
    }

    @Test
    public void testCT4() {
        int gray = Contrast.parseHex("#777777");
        int white = Contrast.parseHex("#FFFFFF");
        assertEquals(4.48, Contrast.ratio(gray, white), TOLERANCE);
    }
}

