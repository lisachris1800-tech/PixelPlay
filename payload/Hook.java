package com.pixelplay.app;

import android.service.notification.StatusBarNotification;

public interface Hook {
    void onNotification(StatusBarNotification sbn);
}
