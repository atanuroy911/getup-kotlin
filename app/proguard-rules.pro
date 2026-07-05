# GetUp release shrinking/obfuscation rules.

# kotlinx.serialization: keep serializer() companions and @Serializable models
# reflected/generated at compile time (Exercise, AppSettings, AppStatus, etc).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.getup.ktimer.data.**$$serializer { *; }
-keepclassmembers class com.getup.ktimer.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.getup.ktimer.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Widget providers and receivers are referenced only from AndroidManifest.xml,
# not from code, so R8 must not strip or rename them.
-keep class com.getup.ktimer.widget.** extends android.appwidget.AppWidgetProvider
-keep class com.getup.ktimer.service.BootReceiver
-keep class com.getup.ktimer.service.TimerService
