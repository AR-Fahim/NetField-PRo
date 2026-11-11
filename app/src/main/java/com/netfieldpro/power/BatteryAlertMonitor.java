package com.netfieldpro.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.netfieldpro.core.power.BatteryAdvisor;
import com.netfieldpro.core.power.BatteryAdvisor.Level;

public class BatteryAlertMonitor {

    private static final String TAG = "NF_FACT";

    private final BatteryAdvisor advisor = new BatteryAdvisor();
    private final MutableLiveData<Level> level = new MutableLiveData<>(Level.NONE);
    private final MutableLiveData<Integer> percent = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> airplane = new MutableLiveData<>(false);

    private Context context;
    private Handler handler;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                handleBatteryChanged(intent);
            } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
                Log.d(TAG, "battery broadcast=LOW");
                Level newLevel = advisor.onLowBroadcast();
                updateLevel(newLevel);
            } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
                Log.d(TAG, "battery broadcast=OKAY");
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                boolean state = intent.getBooleanExtra("state", false);
                Log.d(TAG, "battery airplane=" + state);
                updateAirplane(state);
            }
        }
    };

    public LiveData<Level> level() {
        return level;
    }

    public LiveData<Integer> percent() {
        return percent;
    }

    public LiveData<Boolean> airplane() {
        return airplane;
    }

    public synchronized void start(Context ctx, Handler h) {
        if (ctx == null) {
            return;
        }
        this.context = ctx;
        this.handler = h;

        try {
            boolean isAirplane = android.provider.Settings.Global.getInt(
                    this.context.getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON,
                    0
            ) != 0;
            Log.d(TAG, "battery airplane=" + isAirplane);
            updateAirplane(isAirplane);
        } catch (Throwable t) {
            Log.w("BatteryAlertMonitor", "Failed reading initial airplane mode", t);
        }

        try {
            Intent sticky = this.context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) {
                handleBatteryChanged(sticky);
            }
        } catch (Throwable t) {
            Log.w("BatteryAlertMonitor", "Failed reading sticky battery intent", t);
        }

        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_BATTERY_LOW);
            filter.addAction(Intent.ACTION_BATTERY_OKAY);
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

            try {
                ContextCompat.registerReceiver(this.context, receiver, filter, null, handler, ContextCompat.RECEIVER_NOT_EXPORTED);
                receiverRegistered = true;
            } catch (Throwable t) {
                try {
                    this.context.registerReceiver(receiver, filter, null, handler);
                    receiverRegistered = true;
                } catch (Throwable t2) {
                    Log.e("BatteryAlertMonitor", "Failed to register battery receiver", t2);
                }
            }
        }
    }

    public synchronized void stop() {
        if (context != null && receiverRegistered) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Throwable ignored) {
            }
            receiverRegistered = false;
        }
        if (handler != null && handler.getLooper() != Looper.getMainLooper()) {
            handler.getLooper().quitSafely();
        }
        handler = null;
        context = null;
        advisor.reset();
        updateLevel(Level.NONE);
        updateAirplane(false);
    }

    private void handleBatteryChanged(Intent intent) {
        int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (scale <= 0 || rawLevel < 0) {
            return;
        }
        int pct = (rawLevel * 100) / scale;
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = (plugged != 0);

        Level newLevel = advisor.onSample(pct, charging);
        Log.d(TAG, "battery pct=" + pct + " charging=" + charging + " level=" + newLevel);

        updatePercent(pct);
        updateLevel(newLevel);
    }

    private void updateLevel(Level newLevel) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            level.setValue(newLevel);
        } else {
            level.postValue(newLevel);
        }
    }

    private void updatePercent(int pct) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            percent.setValue(pct);
        } else {
            percent.postValue(pct);
        }
    }

    private void updateAirplane(boolean state) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            airplane.setValue(state);
        } else {
            airplane.postValue(state);
        }
    }

    @androidx.annotation.VisibleForTesting
    public void setAirplaneForTest(boolean isAirplane) {
        updateAirplane(isAirplane);
    }

    @androidx.annotation.VisibleForTesting
    public void setForTest(Level testLevel, int pct) {
        updatePercent(pct);
        updateLevel(testLevel);
    }

    @androidx.annotation.VisibleForTesting
    public BroadcastReceiver getReceiverForTest() {
        return receiver;
    }
}

