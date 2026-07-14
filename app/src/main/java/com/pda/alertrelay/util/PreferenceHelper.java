package com.pda.alertrelay.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.pda.alertrelay.model.AlertRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class PreferenceHelper {

    private static final String PREFS = "pda_alert_prefs";

    public static final String KEY_TARGET_PACKAGE = "target_package";
    public static final String KEY_ALERT_ENABLED = "alert_enabled";
    public static final String KEY_SOUND_ENABLED = "sound_enabled";
    public static final String KEY_VIBRATE_ENABLED = "vibrate_enabled";
    public static final String KEY_WAKE_SCREEN_ENABLED = "wake_screen_enabled";
    public static final String KEY_STAY_DURATION = "stay_duration";
    public static final String KEY_AUTOSTART_GUIDE_DONE = "autostart_guide_done";
    public static final String KEY_BATTERY_SKIPPED = "battery_skipped";
    public static final String KEY_HISTORY = "history_json";

    public static final int STAY_MANUAL = 0;
    public static final int STAY_30S = 30;
    public static final int STAY_60S = 60;

    private static final int MAX_HISTORY = 20;

    private PreferenceHelper() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getTargetPackage(Context context) {
        return prefs(context).getString(KEY_TARGET_PACKAGE, "");
    }

    public static void setTargetPackage(Context context, String pkg) {
        prefs(context).edit().putString(KEY_TARGET_PACKAGE, pkg == null ? "" : pkg.trim()).apply();
    }

    public static boolean isAlertEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ALERT_ENABLED, true);
    }

    public static void setAlertEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ALERT_ENABLED, enabled).apply();
    }

    public static boolean isSoundEnabled(Context context) {
        return prefs(context).getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public static boolean isVibrateEnabled(Context context) {
        return prefs(context).getBoolean(KEY_VIBRATE_ENABLED, true);
    }

    public static void setVibrateEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply();
    }

    public static boolean isWakeScreenEnabled(Context context) {
        return prefs(context).getBoolean(KEY_WAKE_SCREEN_ENABLED, true);
    }

    public static void setWakeScreenEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_WAKE_SCREEN_ENABLED, enabled).apply();
    }

    public static int getStayDuration(Context context) {
        return prefs(context).getInt(KEY_STAY_DURATION, STAY_60S);
    }

    public static void setStayDuration(Context context, int seconds) {
        prefs(context).edit().putInt(KEY_STAY_DURATION, seconds).apply();
    }

    public static boolean isAutostartGuideDone(Context context) {
        return prefs(context).getBoolean(KEY_AUTOSTART_GUIDE_DONE, false);
    }

    public static void setAutostartGuideDone(Context context, boolean done) {
        prefs(context).edit().putBoolean(KEY_AUTOSTART_GUIDE_DONE, done).apply();
    }

    public static boolean isBatterySkipped(Context context) {
        return prefs(context).getBoolean(KEY_BATTERY_SKIPPED, false);
    }

    public static void setBatterySkipped(Context context, boolean skipped) {
        prefs(context).edit().putBoolean(KEY_BATTERY_SKIPPED, skipped).apply();
    }

    public static void addHistoryRecord(Context context, AlertRecord record) {
        List<AlertRecord> records = getHistory(context);
        records.add(0, record);
        while (records.size() > MAX_HISTORY) {
            records.remove(records.size() - 1);
        }
        saveHistory(context, records);
    }

    public static List<AlertRecord> getHistory(Context context) {
        String json = prefs(context).getString(KEY_HISTORY, "[]");
        List<AlertRecord> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new AlertRecord(
                        obj.getLong("timestamp"),
                        obj.optString("title", ""),
                        obj.optString("text", "")
                ));
            }
        } catch (JSONException ignored) {
            // 损坏的历史数据忽略
        }
        return list;
    }

    public static void clearHistory(Context context) {
        prefs(context).edit().putString(KEY_HISTORY, "[]").apply();
    }

    private static void saveHistory(Context context, List<AlertRecord> records) {
        JSONArray array = new JSONArray();
        for (AlertRecord record : records) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("timestamp", record.timestamp);
                obj.put("title", record.title);
                obj.put("text", record.text);
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(KEY_HISTORY, array.toString()).apply();
    }
}
