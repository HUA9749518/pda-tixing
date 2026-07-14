package com.pda.alertrelay.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
        try {
            context.startActivity(intent);
        } catch (Exception ignored) {
            openAppDetails(context);
        }
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean isBatteryReady(Context context) {
        return isIgnoringBatteryOptimizations(context)
                || PreferenceHelper.isBatterySkipped(context);
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
            openAppDetails(context);
        }
    }

    public static void openAppDetails(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception ignored) {
            Intent fallback = new Intent(Settings.ACTION_SETTINGS);
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(fallback);
            } catch (Exception ignored2) {
            }
        }
    }

    /**
     * 尝试打开各厂商自启动管理页；全部失败则打开应用详情页。
     *
     * @return true 表示至少成功启动了一个界面
     */
    public static boolean tryOpenAutostartSettings(Context context) {
        String pkg = context.getPackageName();
        Intent[] candidates = new Intent[]{
                componentIntent("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                componentIntent("com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                componentIntent("com.huawei.systemmanager",
                        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
                componentIntent("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                componentIntent("com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"),
                componentIntent("com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                componentIntent("com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddMyPhoneList"),
                componentIntent("com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"),
                componentIntent("com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"),
                componentIntent("com.meizu.safe",
                        "com.meizu.safe.permission.SmartBGActivity"),
                componentIntent("com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                new Intent().setAction("miui.intent.action.OP_AUTO_START")
                        .addCategory(Intent.CATEGORY_DEFAULT),
                new Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + pkg))
        };

        for (Intent intent : candidates) {
            if (intent == null) {
                continue;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (canStart(context, intent)) {
                try {
                    context.startActivity(intent);
                    return true;
                } catch (Exception ignored) {
                }
            }
        }
        openAppDetails(context);
        return true;
    }

    private static Intent componentIntent(String packageName, String className) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, className));
        return intent;
    }

    private static boolean canStart(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        return intent.resolveActivity(pm) != null;
    }

    /** 仅通知使用权为硬性要求；电池/自启动可检测通过或用户勾选跳过/确认。 */
    public static boolean areAllPermissionsReady(Context context) {
        return isNotificationListenerEnabled(context)
                && isBatteryReady(context)
                && PreferenceHelper.isAutostartGuideDone(context);
    }

    public static String getMissingHint(Context context) {
        if (!isNotificationListenerEnabled(context)) {
            return "请先开启「通知使用权」（必须项）";
        }
        if (!isBatteryReady(context)) {
            return "请设置电池优化，或勾选「本机无此项，跳过」";
        }
        if (!PreferenceHelper.isAutostartGuideDone(context)) {
            return "请尝试打开自启动页，或勾选「已完成/跳过」";
        }
        return "请完成权限配置";
    }

    public static void requestNotificationListenerRebind(Context context) {
        ComponentName component = new ComponentName(
                context,
                com.pda.alertrelay.service.AlertNotificationListenerService.class
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(component);
        }
    }
}
