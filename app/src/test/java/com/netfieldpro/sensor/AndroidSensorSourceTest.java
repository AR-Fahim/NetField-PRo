package com.netfieldpro.sensor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AndroidSensorSourceTest {

    @Test
    public void testStartAndStopLifecycle() {
        Context context = ApplicationProvider.getApplicationContext();
        AndroidSensorSource source = new AndroidSensorSource(context);

        assertFalse(source.isRunning());
        source.start((tNs, x, y, z) -> {});
        assertTrue(source.isRunning());

        source.stop();
        assertFalse(source.isRunning());

        // Repeated stop does not crash
        source.stop();
        assertFalse(source.isRunning());
    }
}
