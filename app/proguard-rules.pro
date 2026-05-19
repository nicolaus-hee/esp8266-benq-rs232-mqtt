# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class de.nhe.podcastwear.network.**$$serializer { *; }
-keepclassmembers class de.nhe.podcastwear.network.** { *** Companion; }
-keepclasseswithmembers class de.nhe.podcastwear.network.** { kotlinx.serialization.KSerializer serializer(...); }

# Media3
-keep class androidx.media3.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
