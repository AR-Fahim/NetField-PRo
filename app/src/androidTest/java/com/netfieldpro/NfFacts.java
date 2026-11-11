package com.netfieldpro;

import android.util.Log;

public final class NfFacts {
    private static final String TAG = "NF_FACT";

    private NfFacts() {}

    public static void log(String key, String value) {
        Log.i(TAG, key + "=" + value);
    }
}
