# Optimized build: keep keyboard core, allow R8 to shrink
-keep class expected.keyboard2.** { *; }
-keepclassmembers class expected.keyboard2.** { *; }
# Keep InputMethodService entry
-keep class expected.keyboard2.Keyboard2 { *; }
-keep class expected.keyboard2.Keyboard2$Receiver { *; }
# Keep resources referenced via reflection
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn androidx.compose.**
-dontwarn androidx.room.**
# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
