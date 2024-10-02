# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class dev.brahmkshatriya.echo.extension.LastFM {
    *;
}
-keep class dev.brahmkshatriya.echo.common.** { *; }
-keep @interface * { *; }
-keepnames class dev.brahmkshatriya.echo.common.** { *; }
-keep interface dev.brahmkshatriya.echo.common.clients.TrackerClient
-keep interface dev.brahmkshatriya.echo.common.clients.LoginClient
-keep interface dev.brahmkshatriya.echo.common.clients.ExtensionClient
-keep class dev.brahmkshatriya.echo.common.models.** { *; }
-keepclassmembers class dev.brahmkshatriya.echo.common.** {
    @dev.brahmkshatriya.echo.common.* <fields>;
}
-keep interface dev.brahmkshatriya.echo.common.clients.TrackerClient {
    <methods>;
}
-keep class dev.brahmkshatriya.echo.common.models.EchoMediaItem {
    <methods>;
}
-keep class dev.brahmkshatriya.echo.common.models.Track {
    <methods>;
}
-keepnames class kotlin.coroutines.Continuation { *; }
-dontwarn org.jspecify.annotations.NullMarked