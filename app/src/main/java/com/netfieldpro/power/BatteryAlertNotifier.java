package com.netfieldpro.power;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.netfieldpro.MainActivity;
import com.netfieldpro.R;
import com.netfieldpro.core.power.BatteryAdvisor.Level;
import com.netfieldpro.core.power.BatteryAlertPolicy;
import com.netfieldpro.core.power.NotifyAction;
import com.netfieldpro.session.SurveyForegroundService;

public class BatteryAlertNotifier {

    public static final String CHANNEL_ID = "nf_battery_alert";
    public static final int ALERT_ID = 4102;

    private final Context context;
    private boolean channelCreated = false;

    public BatteryAlertNotifier(Context context) {
        this.context = context.getApplicationContext() != null ? context.getApplicationContext() : context;
    }

    public void onLevel(Level prev, Level next, int pct) {
        // Update ongoing notification text independently of alert notification
        if (next == Level.WARN) {
            String ongoingText = context.getString(R.string.survey_notif_text_batt_warn, pct);
            SurveyForegroundService.updateOngoingText(ongoingText);
        } else if (next == Level.CRITICAL) {
            String ongoingText = context.getString(R.string.survey_notif_text_batt_crit, pct);
            SurveyForegroundService.updateOngoingText(ongoingText);
        } else {
            SurveyForegroundService.updateOngoingText(null);
        }

        NotifyAction action = BatteryAlertPolicy.decide(prev, next);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }

        if (action == NotifyAction.CANCEL) {
            nm.cancel(ALERT_ID);
            return;
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return;
        }

        if (action == NotifyAction.POST_ALERT || action == NotifyAction.UPDATE_QUIET) {
            ensureChannel(nm);
            Notification notification = buildAlertNotification(next, pct);
            nm.notify(ALERT_ID, notification);
        }
    }

    private void ensureChannel(NotificationManager nm) {
        if (channelCreated) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.battery_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }
        channelCreated = true;
    }

    private Notification buildAlertNotification(Level level, int pct) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent endIntent = new Intent(context, SurveyForegroundService.class);
        endIntent.setAction(SurveyForegroundService.ACTION_END);
        PendingIntent endPendingIntent = PendingIntent.getService(
                context,
                1,
                endIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String title;
        String text;
        if (level == Level.CRITICAL) {
            title = context.getString(R.string.battery_alert_title_crit);
            text = context.getString(R.string.battery_alert_text_crit, pct);
        } else {
            title = context.getString(R.string.battery_alert_title_warn);
            text = context.getString(R.string.battery_alert_text_warn, pct);
        }

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        context.getString(R.string.survey_notif_end),
                        endPendingIntent
                )
                .build();
    }
}

