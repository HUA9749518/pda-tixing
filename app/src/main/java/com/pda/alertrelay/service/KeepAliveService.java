package com.pda.alertrelay.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.pda.alertrelay.R;
import com.pda.alertrelay.ui.MainActivity;
import com.pda.alertrelay.util.PermissionHelper;

public class KeepAliveService extends Service {

    public static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_CHECK = "com.pda.alertrelay.action.CHECK";
    private static final String ACTION_REFRESH = "com.pda.alertrelay.action.REFRESH";
    private static final long WATCHDOG_INTERVAL_MS = 3 * 60 * 1000L;

    private static volatile boolean sListenerConnected;

    private final Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private final Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            runWatchdogCheck();
            watchdogHandler.postDelayed(this, WATCHDOG_INTERVAL_MS);
        }
    };

    public static boolean isListenerConnected() {
        return sListenerConnected;
    }

    public static void setListenerConnected(boolean connected) {
        sListenerConnected = connected;
    }

    public static void refresh(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.setAction(ACTION_REFRESH);
        context.getApplicationContext().startService(intent);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        context.getApplicationContext().startService(intent);
    }

    public static void checkAndRestart(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.setAction(ACTION_CHECK);
        context.getApplicationContext().startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildForegroundNotification());
        watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildForegroundNotification());

        String action = intent != null ? intent.getAction() : null;
        if (ACTION_CHECK.equals(action) || ACTION_REFRESH.equals(action)) {
            runWatchdogCheck();
        } else if (PermissionHelper.isNotificationListenerEnabled(this) && !sListenerConnected) {
            PermissionHelper.requestNotificationListenerRebind(this);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        watchdogHandler.removeCallbacks(watchdogRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runWatchdogCheck() {
        if (PermissionHelper.isNotificationListenerEnabled(this) && !sListenerConnected) {
            PermissionHelper.requestNotificationListenerRebind(this);
        }
        startForeground(NOTIFICATION_ID, buildForegroundNotification());
    }

    private Notification buildForegroundNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        String status;
        if (!PermissionHelper.isNotificationListenerEnabled(this)) {
            status = getString(R.string.status_listener_no_permission);
        } else if (sListenerConnected) {
            status = getString(R.string.status_listener_on);
        } else {
            status = getString(R.string.status_listener_waiting);
        }

        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.foreground_title))
                .setContentText(status)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
    }
}
