package com.netfieldpro.core.heat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Palette {

    private static record Anchor(double dbm, int[] rgb) {
    }

    private static final Anchor[] TL_ANCHORS = new Anchor[]{
            new Anchor(-95.0, new int[]{0xD7, 0x30, 0x27}),
            new Anchor(-85.0, new int[]{0xFC, 0x8D, 0x59}),
            new Anchor(-75.0, new int[]{0xFE, 0xE0, 0x8B}),
            new Anchor(-67.0, new int[]{0x91, 0xCF, 0x60}),
            new Anchor(-55.0, new int[]{0x1A, 0x98, 0x50})
    };

    private static final Anchor[] VI_ANCHORS = new Anchor[]{
            new Anchor(-95.0, new int[]{0x44, 0x01, 0x54}),
            new Anchor(-85.0, new int[]{0x3B, 0x52, 0x8B}),
            new Anchor(-75.0, new int[]{0x21, 0x90, 0x8C}),
            new Anchor(-67.0, new int[]{0x5D, 0xC8, 0x63}),
            new Anchor(-55.0, new int[]{0xFD, 0xE7, 0x25})
    };

    private Palette() {
    }

    public static int[] rgb(PaletteId id, double dbm) {
        if (Double.isNaN(dbm)) {
            throw new IllegalArgumentException("dbm is NaN");
        }
        Anchor[] anchors = (id == PaletteId.VIRIDIS) ? VI_ANCHORS : TL_ANCHORS;

        if (dbm <= anchors[0].dbm) {
            return anchors[0].rgb.clone();
        }
        if (dbm >= anchors[anchors.length - 1].dbm) {
            return anchors[anchors.length - 1].rgb.clone();
        }

        for (int i = 0; i < anchors.length - 1; i++) {
            Anchor a0 = anchors[i];
            Anchor a1 = anchors[i + 1];
            if (a0.dbm <= dbm && dbm <= a1.dbm) {
                BigDecimal valBd = BigDecimal.valueOf(dbm);
                BigDecimal db0Bd = BigDecimal.valueOf(a0.dbm);
                BigDecimal spanBd = BigDecimal.valueOf(a1.dbm - a0.dbm);

                int[] res = new int[3];
                for (int ch = 0; ch < 3; ch++) {
                    BigDecimal c0Bd = BigDecimal.valueOf(a0.rgb[ch]);
                    BigDecimal diffBd = BigDecimal.valueOf(a1.rgb[ch] - a0.rgb[ch]);
                    BigDecimal term = valBd.subtract(db0Bd).multiply(diffBd).divide(spanBd, 9, RoundingMode.HALF_EVEN);
                    BigDecimal chVal = c0Bd.add(term);
                    res[ch] = chVal.setScale(0, RoundingMode.HALF_EVEN).intValueExact();
                }
                return res;
            }
        }
        return anchors[anchors.length - 1].rgb.clone();
    }

    public static String hex(int[] rgb) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
    }

    public static String hex(PaletteId id, double dbm) {
        return hex(rgb(id, dbm));
    }

    public static double relLum(int[] rgb) {
        return 0.2126 * chan(rgb[0]) + 0.7152 * chan(rgb[1]) + 0.0722 * chan(rgb[2]);
    }

    private static double chan(int c) {
        double v = c / 255.0;
        return (v <= 0.04045) ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    public static int textOn(int[] rgb) {
        double lBg = relLum(rgb);
        double crBlack = (lBg + 0.05) / 0.05;
        double crWhite = 1.05 / (lBg + 0.05);
        return (crWhite > crBlack) ? 0xFFFFFF : 0x000000;
    }

    public static int textOn(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return textOn(new int[]{r, g, b});
    }

    public static int textOn(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(clean.substring(0, 2), 16);
        int g = Integer.parseInt(clean.substring(2, 4), 16);
        int b = Integer.parseInt(clean.substring(4, 6), 16);
        return textOn(new int[]{r, g, b});
    }
}
