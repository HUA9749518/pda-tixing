package com.pda.alertrelay.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pda.alertrelay.R;
import com.pda.alertrelay.service.KeepAliveService;
import com.pda.alertrelay.util.PermissionHelper;
import com.pda.alertrelay.util.PreferenceHelper;

public class PermissionActivity extends Activity {

    private TextView statusNotification;
    private TextView statusBattery;
    private CheckBox checkBatterySkip;
    private CheckBox checkAutostart;
    private boolean syncingChecks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        statusNotification = findViewById(R.id.status_notification);
        statusBattery = findViewById(R.id.status_battery);
        checkBatterySkip = findViewById(R.id.check_battery_skip);
        checkAutostart = findViewById(R.id.check_autostart);

        Button btnNotification = findViewById(R.id.btn_open_notification);
        Button btnBattery = findViewById(R.id.btn_open_battery);
        Button btnAutostart = findViewById(R.id.btn_open_autostart);
        Button btnAppDetails = findViewById(R.id.btn_open_app_details);
        Button btnContinue = findViewById(R.id.btn_continue);

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

        btnAutostart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionHelper.tryOpenAutostartSettings(PermissionActivity.this);
                Toast.makeText(PermissionActivity.this, R.string.toast_autostart_opened, Toast.LENGTH_LONG).show();
            }
        });

        btnAppDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionHelper.openAppDetails(PermissionActivity.this);
                Toast.makeText(PermissionActivity.this, R.string.toast_lock_recent, Toast.LENGTH_LONG).show();
            }
        });

        checkBatterySkip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (syncingChecks) {
                    return;
                }
                PreferenceHelper.setBatterySkipped(PermissionActivity.this, isChecked);
                refreshStatus();
            }
        });

        checkAutostart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (syncingChecks) {
                    return;
                }
                PreferenceHelper.setAutostartGuideDone(PermissionActivity.this, isChecked);
            }
        });

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.areAllPermissionsReady(PermissionActivity.this)) {
                    goMain();
                } else {
                    Toast.makeText(
                            PermissionActivity.this,
                            PermissionHelper.getMissingHint(PermissionActivity.this),
                            Toast.LENGTH_LONG
                    ).show();
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
        boolean batteryIgnored = PermissionHelper.isIgnoringBatteryOptimizations(this);
        boolean batterySkipped = PreferenceHelper.isBatterySkipped(this);

        statusNotification.setText(notificationOk
                ? getString(R.string.status_ok)
                : getString(R.string.status_missing));

        syncingChecks = true;
        if (batteryIgnored) {
            statusBattery.setText(getString(R.string.status_ok));
            PreferenceHelper.setBatterySkipped(this, false);
            checkBatterySkip.setChecked(false);
        } else if (batterySkipped) {
            statusBattery.setText(getString(R.string.status_skipped));
            checkBatterySkip.setChecked(true);
        } else {
            statusBattery.setText(getString(R.string.status_missing));
            checkBatterySkip.setChecked(false);
        }
        checkAutostart.setChecked(PreferenceHelper.isAutostartGuideDone(this));
        syncingChecks = false;
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
