# 保留 kotlinx.serialization 的序列化器
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.a2heng.opencode.mobile.**$$serializer { *; }
-keepclassmembers class com.a2heng.opencode.mobile.** { *** Companion; }
