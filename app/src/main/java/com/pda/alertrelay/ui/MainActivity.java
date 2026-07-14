package com.pda.alertrelay.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pda.alertrelay.R;
import com.pda.alertrelay.model.AlertRecord;
import com.pda.alertrelay.service.KeepAliveService;
import com.pda.alertrelay.util.AlertHelper;
import com.pda.alertrelay.util.PermissionHelper;
import com.pda.alertrelay.util.PreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText editPackage;
    private CheckBox checkBootStart;
    private CheckBox checkAlertEnabled;
    private CheckBox checkSound;
    private CheckBox checkVibrate;
    private CheckBox checkWakeScreen;
    private Spinner spinnerStay;
    private TextView statusKeepAlive;
    private TextView statusListener;
    private ListView listHistory;
    private ArrayAdapter<String> historyAdapter;
    private final List<String> historyLines = new ArrayList<>();

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PermissionHelper.areAllPermissionsReady(this)) {
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root_main);
        if (root != null) {
            root.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        bindViews();
        loadSettings();
        bindActions();
        KeepAliveService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!PermissionHelper.areAllPermissionsReady(this)) {
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
            return;
        }
        refreshStatus();
        refreshHistory();
    }

    private void bindViews() {
        editPackage = findViewById(R.id.edit_package);
        checkBootStart = findViewById(R.id.check_boot_start);
        checkAlertEnabled = findViewById(R.id.check_alert_enabled);
        checkSound = findViewById(R.id.check_sound);
        checkVibrate = findViewById(R.id.check_vibrate);
        checkWakeScreen = findViewById(R.id.check_wake_screen);
        spinnerStay = findViewById(R.id.spinner_stay);
        statusKeepAlive = findViewById(R.id.status_keepalive);
        statusListener = findViewById(R.id.status_listener);
        listHistory = findViewById(R.id.list_history);

        historyAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_history,
                historyLines
        );
        listHistory.setAdapter(historyAdapter);
        listHistory.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private void loadSettings() {
        editPackage.setText(PreferenceHelper.getTargetPackage(this));
        checkBootStart.setChecked(PreferenceHelper.isBootStartEnabled(this));
        checkAlertEnabled.setChecked(PreferenceHelper.isAlertEnabled(this));
        checkSound.setChecked(PreferenceHelper.isSoundEnabled(this));
        checkVibrate.setChecked(PreferenceHelper.isVibrateEnabled(this));
        checkWakeScreen.setChecked(PreferenceHelper.isWakeScreenEnabled(this));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.stay_duration_labels)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStay.setAdapter(adapter);

        int stay = PreferenceHelper.getStayDuration(this);
        if (stay == PreferenceHelper.STAY_30S) {
            spinnerStay.setSelection(0);
        } else if (stay == PreferenceHelper.STAY_60S) {
            spinnerStay.setSelection(1);
        } else {
            spinnerStay.setSelection(2);
        }
    }

    private void bindActions() {
        Button btnSave = findViewById(R.id.btn_save);
        Button btnTest = findViewById(R.id.btn_test);
        Button btnClearHistory = findViewById(R.id.btn_clear_history);
        Button btnPermissions = findViewById(R.id.btn_permissions);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(MainActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
            }
        });

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                AlertHelper.showFallback(
                        MainActivity.this,
                        "测试兜底通知",
                        "若看到本条且听到铃声，说明兜底功能正常",
                        true
                );
                refreshHistory();
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceHelper.clearHistory(MainActivity.this);
                refreshHistory();
            }
        });

        btnPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PermissionActivity.class));
            }
        });
    }

    private void saveSettings() {
        PreferenceHelper.setTargetPackage(this, editPackage.getText().toString());
        PreferenceHelper.setBootStartEnabled(this, checkBootStart.isChecked());
        PreferenceHelper.setAlertEnabled(this, checkAlertEnabled.isChecked());
        PreferenceHelper.setSoundEnabled(this, checkSound.isChecked());
        PreferenceHelper.setVibrateEnabled(this, checkVibrate.isChecked());
        PreferenceHelper.setWakeScreenEnabled(this, checkWakeScreen.isChecked());

        int position = spinnerStay.getSelectedItemPosition();
        if (position == 0) {
            PreferenceHelper.setStayDuration(this, PreferenceHelper.STAY_30S);
        } else if (position == 1) {
            PreferenceHelper.setStayDuration(this, PreferenceHelper.STAY_60S);
        } else {
            PreferenceHelper.setStayDuration(this, PreferenceHelper.STAY_MANUAL);
        }
    }

    private void refreshStatus() {
        statusKeepAlive.setText(getString(R.string.status_keepalive_on));

        if (!PermissionHelper.isNotificationListenerEnabled(this)) {
            statusListener.setText(getString(R.string.status_listener_no_permission));
        } else if (KeepAliveService.isListenerConnected()) {
            statusListener.setText(getString(R.string.status_listener_on));
        } else {
            statusListener.setText(getString(R.string.status_listener_waiting));
            PermissionHelper.requestNotificationListenerRebind(this);
        }
    }

    private void refreshHistory() {
        historyLines.clear();
        List<AlertRecord> records = PreferenceHelper.getHistory(this);
        if (records.isEmpty()) {
            historyLines.add(getString(R.string.history_empty));
        } else {
            for (AlertRecord record : records) {
                historyLines.add(
                        timeFormat.format(new Date(record.timestamp))
                                + "  "
                                + record.title
                                + "\n"
                                + record.text
                );
            }
        }
        historyAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        String pkg = editPackage.getText().toString().trim();
        if (checkAlertEnabled.isChecked() && TextUtils.isEmpty(pkg)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.warning_empty_package)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        saveSettings();
        super.onBackPressed();
    }
}
