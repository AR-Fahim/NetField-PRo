package com.netfieldpro.session;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;

import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.power.BatteryAdvisor.Level;
import com.netfieldpro.core.session.SessionEvent;
import com.netfieldpro.power.BatteryAlertMonitor;
import com.netfieldpro.power.BatteryAlertNotifier;
import com.netfieldpro.time.AndroidClock;

public class SurveyForegroundService extends Service {

    public static final String ACTION_START = "com.netfieldpro.session.action.START";
    public static final String ACTION_END = "com.netfieldpro.session.action.END";

    public static final String CHANNEL_ID = "nf_survey";
    public static final int NOTIFICATION_ID = 2001;

    private static volatile SurveyForegroundService sInstance;
    private WifiManager.WifiLock wifiLock;
    private String ongoingTextOverride = null;

    private HandlerThread batteryThread;
    private BatteryAlertMonitor batteryMonitor;
    private BatteryAlertNotifier batteryNotifier;
    private Observer<Level> batteryObserver;
    private Level previousBatteryLevel = Level.NONE;

    public static void updateOngoingText(String text) {
        if (sInstance != null) {
            sInstance.updateOngoingTextInternal(text);
        }
    }

    private void updateOngoingTextInternal(String text) {
        this.ongoingTextOverride = text;
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.notify(NOTIFICATION_ID, buildNotification());
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        try {
            batteryThread = new HandlerThread("nf-battery");
            batteryThread.start();
            Handler handlerOfNfBattery = new Handler(batteryThread.getLooper());

            batteryMonitor = NetFieldApp.batteryAlerts();
            batteryNotifier = new BatteryAlertNotifier(this);
            batteryObserver = next -> {
                try {
                    int pct = (batteryMonitor != null && batteryMonitor.percent().getValue() != null)
                            ? batteryMonitor.percent().getValue() : 100;
                    Level current = (next != null) ? next : Level.NONE;
                    batteryNotifier.onLevel(previousBatteryLevel, current, pct);
                    previousBatteryLevel = current;
                } catch (RuntimeException re) {
                    Log.e("NF_FACT", "battery ERROR", re);
                }
            };
            batteryMonitor.start(this, handlerOfNfBattery);
            batteryMonitor.level().observeForever(batteryObserver);
        } catch (RuntimeException re) {
            Log.e("NF_FACT", "battery ERROR", re);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null && intent.getAction() != null) ? intent.getAction() : ACTION_START;

        if (ACTION_END.equals(action)) {
            try {
                SessionExecutor executor = NetFieldApp.sessionExecutor();
                if (executor != null) {
                    executor.post(new SessionEvent(SessionEvent.Type.END_SESSION, new AndroidClock().monoNs()));
                }
            } catch (Throwable ignored) {
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        Notification notification = buildNotification();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            SurveyRuntime.get().setRuntimeStatus(SurveyRuntime.RuntimeStatus.running());
        } catch (SecurityException se) {
            SurveyRuntime.get().setRuntimeStatus(SurveyRuntime.RuntimeStatus.blocked(se.getClass().getSimpleName()));
        } catch (Throwable t) {
            SurveyRuntime.get().setRuntimeStatus(SurveyRuntime.RuntimeStatus.blocked(t.getClass().getSimpleName()));
        }

        acquireWifiLock();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.survey_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                );
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent endIntent = new Intent(this, SurveyForegroundService.class);
        endIntent.setAction(ACTION_END);
        PendingIntent endPendingIntent = PendingIntent.getService(
                this,
                1,
                endIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String contentText = (ongoingTextOverride != null) ? ongoingTextOverride : getString(R.string.survey_notif_text);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(getString(R.string.survey_notif_title))
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.survey_notif_end),
                        endPendingIntent
                )
                .build();
    }

    private void acquireWifiLock() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "nf-survey");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
                SurveyRuntime.get().setLockStatus(SurveyRuntime.LockStatus.held());
            } else {
                SurveyRuntime.get().setLockStatus(SurveyRuntime.LockStatus.unavailable("WifiManagerNull"));
            }
        } catch (Throwable t) {
            SurveyRuntime.get().setLockStatus(SurveyRuntime.LockStatus.unavailable(t.getClass().getSimpleName()));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sInstance == this) {
            sInstance = null;
        }

        try {
            if (batteryMonitor != null) {
                if (batteryObserver != null) {
                    batteryMonitor.level().removeObserver(batteryObserver);
                    batteryObserver = null;
                }
                batteryMonitor.stop();
                batteryMonitor = null;
            }
        } catch (RuntimeException re) {
            Log.e("NF_FACT", "battery ERROR", re);
        }
        if (batteryThread != null) {
            batteryThread.quitSafely();
            batteryThread = null;
        }

        if (wifiLock != null) {
            try {
                if (wifiLock.isHeld()) {
                    wifiLock.release();
                }
            } catch (Throwable ignored) {
            }
            wifiLock = null;
        }
        SurveyRuntime.get().setLockStatus(SurveyRuntime.LockStatus.none());
        SurveyRuntime.get().setRuntimeStatus(SurveyRuntime.RuntimeStatus.stopped());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
