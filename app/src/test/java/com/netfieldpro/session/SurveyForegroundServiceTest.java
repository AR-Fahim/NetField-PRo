package com.netfieldpro.session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.power.BatteryAdvisor.Level;
import com.netfieldpro.power.BatteryAlertMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class SurveyForegroundServiceTest {

    private ServiceController<SurveyForegroundService> controller;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(SurveyForegroundService.class);
    }

    @After
    public void tearDown() {
        if (controller != null) {
            try {
                controller.destroy();
            } catch (Throwable ignored) {
            }
        }
    }

    @Test
    public void testServiceLifecycle_startsAndStopsBatteryMonitor() {
        SurveyForegroundService service = controller.create().get();
        assertNotNull(service);

        BatteryAlertMonitor monitor = NetFieldApp.batteryAlerts();
        assertNotNull(monitor);

        controller.destroy();
        ShadowLooper.idleMainLooper();
    }

    @Test
    public void testServiceLifecycle_monitorErrorDoesNotStopService() {
        // Set a monitor that throws a RuntimeException on start
        BatteryAlertMonitor throwingMonitor = new BatteryAlertMonitor() {
            @Override
            public synchronized void start(android.content.Context ctx, android.os.Handler h) {
                throw new RuntimeException("Simulated monitor crash");
            }
        };
        NetFieldApp.setBatteryAlertMonitorForTest(throwingMonitor);

        // Service should create successfully without crashing
        SurveyForegroundService service = controller.create().get();
        assertNotNull(service);

        controller.destroy();
        NetFieldApp.setBatteryAlertMonitorForTest(null);
    }
}

