# needed for sqldelight
-keep class org.sqlite.** { *; }

# needed for ktor?
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.netty.** {*; }
-keep class kotlin.reflect.jvm.internal.** { *; }