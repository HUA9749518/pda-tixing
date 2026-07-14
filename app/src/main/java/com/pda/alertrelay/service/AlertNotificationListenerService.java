package com.pda.alertrelay.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.pda.alertrelay.util.AlertHelper;
import com.pda.alertrelay.util.NotificationDeduper;
import com.pda.alertrelay.util.PreferenceHelper;

public class AlertNotificationListenerService extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        KeepAliveService.setListenerConnected(true);
        KeepAliveService.refresh(this);
    }

    @Override
    public void onListenerDisconnected() {
        KeepAliveService.setListenerConnected(false);
        KeepAliveService.checkAndRestart(this);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            requestRebind(new android.content.ComponentName(this, AlertNotificationListenerService.class));
        }
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        String pkg = sbn.getPackageName();
        if (TextUtils.equals(pkg, getPackageName())) {
            return;
        }

        if (!PreferenceHelper.isAlertEnabled(this)) {
            return;
        }

        String target = PreferenceHelper.getTargetPackage(this);
        if (TextUtils.isEmpty(target) || !TextUtils.equals(pkg, target)) {
            return;
        }

        if (NotificationDeduper.isDuplicate(sbn)) {
            return;
        }

        AlertHelper.showFallbackFromNotification(this, sbn);
    }
}
