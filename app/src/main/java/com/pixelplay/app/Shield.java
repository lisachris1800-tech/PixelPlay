package com.pixelplay.app;

import android.util.Log;

public class Shield {
    private static final String TAG = "Shield";
    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("shield");
            loaded = true;
            Log.d(TAG, "[+] native shield loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "[!] shield unavailable");
        }
    }

    public static native byte[] decrypt(byte[] data);

    public static boolean isAvailable() {
        return loaded;
    }
}
