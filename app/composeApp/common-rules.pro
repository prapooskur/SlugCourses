# needed for ktor - see https://youtrack.jetbrains.com/issue/KTOR-5528
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
-dontwarn java.awt.event.ActionListener
-dontwarn javax.swing.SwingUtilities
-dontwarn javax.swing.Timer

# the app is open-source anyway
-dontobfuscate

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable