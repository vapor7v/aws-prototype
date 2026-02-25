# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Retrofit
-keepattributes Signature
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.arinterior.engine.data.model.** { *; }
-keep class com.arinterior.engine.data.api.** { *; }

# ARCore
-keep class com.google.ar.** { *; }

# SceneView
-keep class io.github.sceneview.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
