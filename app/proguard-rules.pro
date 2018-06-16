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
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-keep class android.support.design.widget.** { *; }
-keep interface android.support.design.widget.** { *; }
-dontwarn android.support.design.**
-keep public class * extends android.support.design.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Grammar
-keep class com.tomclaw.kvassword.Unobfuscatable
-keep class * implements com.tomclaw.kvassword.Unobfuscatable
-keepclassmembernames class * implements com.tomclaw.kvassword.Unobfuscatable {
  !transient <fields>;
}
-keepnames class * implements com.tomclaw.kvassword.Unobfuscatable {
  !transient <fields>;
}
-keepclassmembers class * implements com.tomclaw.kvassword.Unobfuscatable {
  <init>(...);
}

-keepattributes *Annotation*,EnclosingMethod
-keepclasseswithmembers class * {
	public <init>(android.content.Context, android.util.AttributeSet, int);
}