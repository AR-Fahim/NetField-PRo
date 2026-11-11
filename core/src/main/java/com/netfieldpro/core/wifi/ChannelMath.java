package com.netfieldpro.core.wifi;

public final class ChannelMath {
    private ChannelMath() {}

    public static Band toBand(int freqMHz) {
        if (freqMHz >= 2400 && freqMHz <= 2500) {
            return Band.B24;
        } else if (freqMHz >= 5150 && freqMHz <= 5895) {
            return Band.B5;
        } else if (freqMHz >= 5925 && freqMHz <= 7125) {
            return Band.B6;
        } else {
            return Band.UNKNOWN;
        }
    }

    public static int toChannel(int freqMHz) {
        Band band = toBand(freqMHz);
        switch (band) {
            case B24:
                if (freqMHz == 2484) {
                    return 14;
                }
                if (freqMHz >= 2412 && freqMHz <= 2472 && (freqMHz - 2407) % 5 == 0) {
                    return (freqMHz - 2407) / 5;
                }
                return -1;
            case B5:
                if ((freqMHz - 5000) % 5 == 0) {
                    return (freqMHz - 5000) / 5;
                }
                return -1;
            case B6:
                if (freqMHz == 5935) {
                    return 2;
                }
                if (freqMHz >= 5955 && (freqMHz - 5950) % 5 == 0) {
                    return (freqMHz - 5950) / 5;
                }
                return -1;
            case UNKNOWN:
            default:
                return -1;
        }
    }
}
