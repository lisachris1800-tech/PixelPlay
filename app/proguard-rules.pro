-keep class com.pixelplay.app.MainActivity { *; }
-keep class com.pixelplay.app.PlayerActivity { *; }
-keep class com.pixelplay.app.MediaService { *; }
-keep class com.pixelplay.app.BootReceiver { *; }
-keep class com.pixelplay.app.MyNotifListener { *; }

-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-mergeinterfacesaggressively
-overloadaggressively

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
