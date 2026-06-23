package com.pixelplay.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationBridge extends NotificationListenerService {
    private static Hook hook;

    public static void setHook(Hook h) {
        hook = h;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (hook != null) hook.onNotification(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
