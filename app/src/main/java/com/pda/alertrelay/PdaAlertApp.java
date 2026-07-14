package com.pda.alertrelay;

import android.app.Application;

import com.pda.alertrelay.service.KeepAliveService;

public class PdaAlertApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KeepAliveService.start(this);
    }
}
