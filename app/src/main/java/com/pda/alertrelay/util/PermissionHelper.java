package com.pda.alertrelay.util;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;

public final class PermissionHelper {

    private PermissionHelper() {
    }

    public static boolean isNotificationListenerEnabled(Context context) {
        String pkg = context.getPackageName();
        String flat = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        if (TextUtils.isEmpty(flat)) {
            return false;
        }
        String[] names = flat.split(":");
        for (String name : names) {
            ComponentName componentName = ComponentName.unflattenFromString(name);
            if (componentName != null && TextUtils.equals(pkg, componentName.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static void openNotificationListenerSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception ignored) {
            openBatterySettings(context);
        }
    }

    public static void openBatterySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    public static boolean areAllPermissionsReady(Context context) {
        return isNotificationListenerEnabled(context)
                && isIgnoringBatteryOptimizations(context)
                && PreferenceHelper.isAutostartGuideDone(context);
    }

    public static void requestNotificationListenerRebind(Context context) {
        ComponentName component = new ComponentName(context, com.pda.alertrelay.service.AlertNotificationListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(component);
        }
    }
}
