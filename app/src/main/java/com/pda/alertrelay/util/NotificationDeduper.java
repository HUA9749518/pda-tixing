package com.pda.alertrelay.util;

import android.service.notification.StatusBarNotification;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 防止系统对同一通知多次回调导致重复兜底。
 */
public final class NotificationDeduper {

    private static final long WINDOW_MS = 5000L;
    private static final int MAX_ENTRIES = 64;

    private static final Map<String, Long> SEEN = new LinkedHashMap<String, Long>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private NotificationDeduper() {
    }

    public static synchronized boolean isDuplicate(StatusBarNotification sbn) {
        if (sbn == null) {
            return true;
        }
        String key = buildKey(sbn);
        long now = System.currentTimeMillis();
        purgeExpired(now);
        Long last = SEEN.get(key);
        if (last != null && now - last < WINDOW_MS) {
            return true;
        }
        SEEN.put(key, now);
        return false;
    }

    private static String buildKey(StatusBarNotification sbn) {
        if (sbn.getTag() != null) {
            return sbn.getPackageName() + "|" + sbn.getTag() + "|" + sbn.getId();
        }
        return sbn.getKey();
    }

    private static void purgeExpired(long now) {
        Iterator<Map.Entry<String, Long>> it = SEEN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > WINDOW_MS) {
                it.remove();
            }
        }
    }
}
