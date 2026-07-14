package com.pda.alertrelay.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.pda.alertrelay.R;
import com.pda.alertrelay.service.KeepAliveService;
import com.pda.alertrelay.util.PermissionHelper;
import com.pda.alertrelay.util.PreferenceHelper;

public class PermissionActivity extends Activity {

    private TextView statusNotification;
    private TextView statusBattery;
    private CheckBox checkAutostart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        statusNotification = findViewById(R.id.status_notification);
        statusBattery = findViewById(R.id.status_battery);
        checkAutostart = findViewById(R.id.check_autostart);

        Button btnNotification = findViewById(R.id.btn_open_notification);
        Button btnBattery = findViewById(R.id.btn_open_battery);
        Button btnContinue = findViewById(R.id.btn_continue);

        checkAutostart.setChecked(PreferenceHelper.isAutostartGuideDone(this));

        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionHelper.openNotificationListenerSettings(PermissionActivity.this);
            }
        });

        btnBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionHelper.requestIgnoreBatteryOptimizations(PermissionActivity.this);
            }
        });

        checkAutostart.setOnCheckedChangeListener((buttonView, isChecked) ->
                PreferenceHelper.setAutostartGuideDone(PermissionActivity.this, isChecked));

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.areAllPermissionsReady(PermissionActivity.this)) {
                    goMain();
                }
            }
        });

        KeepAliveService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        if (PermissionHelper.areAllPermissionsReady(this)) {
            goMain();
        }
    }

    private void refreshStatus() {
        boolean notificationOk = PermissionHelper.isNotificationListenerEnabled(this);
        boolean batteryOk = PermissionHelper.isIgnoringBatteryOptimizations(this);

        statusNotification.setText(notificationOk
                ? getString(R.string.status_ok)
                : getString(R.string.status_missing));
        statusBattery.setText(batteryOk
                ? getString(R.string.status_ok)
                : getString(R.string.status_missing));

        checkAutostart.setChecked(PreferenceHelper.isAutostartGuideDone(this));
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
