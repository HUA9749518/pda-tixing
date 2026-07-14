package com.pda.alertrelay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pda.alertrelay.service.KeepAliveService;
import com.pda.alertrelay.util.PermissionHelper;
import com.pda.alertrelay.util.PreferenceHelper;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        // APP 内开关关闭则不在开机时拉起（与 Screen Stream 同类逻辑）
        if (!PreferenceHelper.isBootStartEnabled(context)) {
            return;
        }

        KeepAliveService.start(context);
        PermissionHelper.requestNotificationListenerRebind(context);
    }
}
