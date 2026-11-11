package com.netfieldpro.core.color;

public final class Contrast {

    private Contrast() {}

    public static int parseHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Hex string cannot be null");
        }
        String clean = hex.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (clean.length() == 6) {
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        } else if (clean.length() == 8) {
            return (int) Long.parseLong(clean, 16);
        } else {
            throw new IllegalArgumentException("Unsupported hex format: " + hex);
        }
    }

    public static double luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        double rLin = channelLinear(r / 255.0);
        double gLin = channelLinear(g / 255.0);
        double bLin = channelLinear(b / 255.0);

        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin;
    }

    private static double channelLinear(double c) {
        if (c <= 0.04045) {
            return c / 12.92;
        } else {
            return Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    public static double ratio(int a, int b) {
        double l1 = luminance(a);
        double l2 = luminance(b);
        double bright = Math.max(l1, l2);
        double dark = Math.min(l1, l2);
        return (bright + 0.05) / (dark + 0.05);
    }
}

