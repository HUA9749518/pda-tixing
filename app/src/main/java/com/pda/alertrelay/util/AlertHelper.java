package com.pda.alertrelay.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.pda.alertrelay.R;
import com.pda.alertrelay.model.AlertRecord;
import com.pda.alertrelay.ui.MainActivity;

public final class AlertHelper {

    private static final int FALLBACK_NOTIFY_ID_BASE = 90000;
    private static final long WAKE_LOCK_TIMEOUT_MS = 5000L;
    private static final long[] VIBRATE_PATTERN = new long[]{0, 400, 200, 400};

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static int sNotifyCounter;

    private AlertHelper() {
    }

    public static void showFallbackFromNotification(Context context, StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return;
        }
        CharSequence title = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        if (TextUtils.isEmpty(text)) {
            text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }
        if (TextUtils.isEmpty(text)) {
            text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        }
        showFallback(context,
                title == null ? "新消息" : title.toString(),
                text == null ? "" : text.toString(),
                true);
    }

    public static void showFallback(Context context, String title, String text, boolean saveHistory) {
        if (!PreferenceHelper.isAlertEnabled(context)) {
            return;
        }

        Context app = context.getApplicationContext();
        String displayTitle = TextUtils.isEmpty(title) ? "新消息提醒" : title;
        String displayText = text == null ? "" : text;

        if (PreferenceHelper.isWakeScreenEnabled(app)) {
            wakeScreen(app);
        }
        if (PreferenceHelper.isSoundEnabled(app)) {
            playSound(app);
        }
        if (PreferenceHelper.isVibrateEnabled(app)) {
            vibrate(app);
        }

        int notifyId = FALLBACK_NOTIFY_ID_BASE + (++sNotifyCounter % 1000);
        postFallbackNotification(app, displayTitle, displayText, notifyId);

        if (saveHistory) {
            PreferenceHelper.addHistoryRecord(app, new AlertRecord(System.currentTimeMillis(), displayTitle, displayText));
        }
    }

    private static void postFallbackNotification(Context context, String title, String text, int notifyId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notifyId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("[兜底] " + title)
                .setContentText(text)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (PreferenceHelper.isSoundEnabled(context)) {
            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(sound);
        }
        if (PreferenceHelper.isVibrateEnabled(context)) {
            builder.setDefaults(builder.getDefaults() | Notification.DEFAULT_VIBRATE);
            builder.setVibrate(VIBRATE_PATTERN);
        }

        int staySeconds = PreferenceHelper.getStayDuration(context);
        if (staySeconds == PreferenceHelper.STAY_MANUAL) {
            builder.setOngoing(true);
        }

        nm.notify(notifyId, builder.build());

        if (staySeconds > 0) {
            MAIN_HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    nm.cancel(notifyId);
                }
            }, staySeconds * 1000L);
        }
    }

    private static void wakeScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) {
            return;
        }
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "pda-alert:wake"
        );
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
    }

    private static void playSound(Context context) {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        vibrator.vibrate(VIBRATE_PATTERN, -1);
    }
}
