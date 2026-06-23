-keep class com.pixelplay.app.MainActivity { *; }
-keep class com.pixelplay.app.PlayerActivity { *; }
-keep class com.pixelplay.app.BootReceiver { *; }
-keep class com.pixelplay.app.NotificationBridge { *; }
-keep class com.pixelplay.app.NativeBridge { *; }
-keep class com.pixelplay.app.SplashActivity { *; }

-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-mergeinterfacesaggressively
-overloadaggressively

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
