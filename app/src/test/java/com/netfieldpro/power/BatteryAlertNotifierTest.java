package com.netfieldpro.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;

import com.netfieldpro.R;
import com.netfieldpro.core.power.BatteryAdvisor.Level;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatteryAlertNotifierTest {

    private Context context;
    private BatteryAlertNotifier notifier;
    private NotificationManager nm;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        notifier = new BatteryAlertNotifier(context);
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    @Test
    public void testBN1_alertNotificationLifecycle() {
        // WARN -> one active alert notification with title battery_alert_title_warn
        notifier.onLevel(Level.NONE, Level.WARN, 18);

        StatusBarNotification warnNotif = findAlertNotification();
        assertNotNull("Alert notification should be active for WARN", warnNotif);
        assertEquals(context.getString(R.string.battery_alert_title_warn),
                warnNotif.getNotification().extras.getString(Notification.EXTRA_TITLE));

        // CRITICAL -> the same id with the critical title
        notifier.onLevel(Level.WARN, Level.CRITICAL, 9);

        StatusBarNotification critNotif = findAlertNotification();
        assertNotNull("Alert notification should be active for CRITICAL", critNotif);
        assertEquals(context.getString(R.string.battery_alert_title_crit),
                critNotif.getNotification().extras.getString(Notification.EXTRA_TITLE));

        // NONE -> no alert from this id
        notifier.onLevel(Level.CRITICAL, Level.NONE, 25);

        StatusBarNotification clearedNotif = findAlertNotification();
        assertFalse("Alert notification should be cancelled for NONE", clearedNotif != null);
    }

    private StatusBarNotification findAlertNotification() {
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
            if (sbn.getId() == BatteryAlertNotifier.ALERT_ID) {
                return sbn;
            }
        }
        return null;
    }
}

