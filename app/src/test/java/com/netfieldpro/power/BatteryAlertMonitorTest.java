package com.netfieldpro.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.core.power.BatteryAdvisor.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class BatteryAlertMonitorTest {

    private BatteryAlertMonitor monitor;
    private Context context;
    private Handler handler;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        handler = new Handler(Looper.getMainLooper());
        monitor = new BatteryAlertMonitor();
    }

    @After
    public void tearDown() {
        monitor.stop();
    }

    @Test
    public void testBM1_batteryTransitions() {
        monitor.start(context, handler);
        ShadowLooper.idleMainLooper();

        // Broadcast level 15, scale 100, plugged 0 -> WARN, 15
        Intent intent1 = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent1.putExtra(BatteryManager.EXTRA_LEVEL, 15);
        intent1.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent1.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        context.sendBroadcast(intent1);
        ShadowLooper.idleMainLooper();

        assertEquals(Level.WARN, monitor.level().getValue());
        assertEquals(Integer.valueOf(15), monitor.percent().getValue());

        // Level 9 -> CRITICAL
        Intent intent2 = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent2.putExtra(BatteryManager.EXTRA_LEVEL, 9);
        intent2.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent2.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        context.sendBroadcast(intent2);
        ShadowLooper.idleMainLooper();

        assertEquals(Level.CRITICAL, monitor.level().getValue());
        assertEquals(Integer.valueOf(9), monitor.percent().getValue());

        // Plugged 2 -> NONE
        Intent intent3 = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent3.putExtra(BatteryManager.EXTRA_LEVEL, 9);
        intent3.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent3.putExtra(BatteryManager.EXTRA_PLUGGED, 2);
        context.sendBroadcast(intent3);
        ShadowLooper.idleMainLooper();

        assertEquals(Level.NONE, monitor.level().getValue());
    }

    @Test
    public void testBM2_stopUnregistersAndDoesNotUpdate() {
        monitor.start(context, handler);
        ShadowLooper.idleMainLooper();

        boolean foundBeforeStop = false;
        for (ShadowApplication.Wrapper wrapper : ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (wrapper.broadcastReceiver == monitor.getReceiverForTest()) {
                foundBeforeStop = true;
                break;
            }
        }
        assertTrue("Monitor receiver should be registered", foundBeforeStop);

        monitor.stop();
        ShadowLooper.idleMainLooper();

        for (ShadowApplication.Wrapper wrapper : ShadowApplication.getInstance().getRegisteredReceivers()) {
            assertNotSame("Monitor receiver should not be in registered receivers",
                    monitor.getReceiverForTest(), wrapper.broadcastReceiver);
        }

        // A CHANGED broadcast after stop does not change the LiveData
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, 5);
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        context.sendBroadcast(intent);
        ShadowLooper.idleMainLooper();

        assertEquals(Level.NONE, monitor.level().getValue());
    }

    @Test
    public void testBM3_airplaneModeBroadcast() {
        monitor.start(context, handler);
        ShadowLooper.idleMainLooper();

        // Broadcast with state=true -> airplane() true
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", true);
        context.sendBroadcast(intent);
        ShadowLooper.idleMainLooper();

        assertEquals(Boolean.TRUE, monitor.airplane().getValue());

        // Broadcast with state=false -> airplane() false
        Intent intentOff = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentOff.putExtra("state", false);
        context.sendBroadcast(intentOff);
        ShadowLooper.idleMainLooper();

        assertEquals(Boolean.FALSE, monitor.airplane().getValue());
    }
}

