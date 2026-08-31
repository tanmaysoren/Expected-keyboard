# Allow R8 to shrink/optimize app code - keep only entry points accessed via reflection/Android framework
-keep class expected.keyboard2.Keyboard2 { *; }
-keep class expected.keyboard2.Keyboard2$Receiver { *; }
-keep class expected.keyboard2.Config { *; }
-keep class expected.keyboard2.KeyboardData { *; }
-keep class expected.keyboard2.KeyboardData$* { *; }
# Keep preference Activities (declared in manifest)
-keep class expected.keyboard2.SettingsActivity { *; }
-keep class expected.keyboard2.SectionSettingsActivity { *; }
-keep class expected.keyboard2.LauncherActivity { *; }
-keep class expected.keyboard2.dict.DictionariesActivity { *; }
# Keep enum values for XML layout parsing via reflection
-keepclassmembers enum expected.keyboard2.** { *; }
# Keep native methods and resource R class (referenced via aapt)
-keep class expected.keyboard2.R { *; }
-keep class expected.keyboard2.R$* { *; }
-keep class expected.keyboard2.BuildConfig { *; }
# Keep attributes needed for debugging but allow obfuscation
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,*Annotation*
-renamesourcefileattribute SourceFile
-dontwarn androidx.compose.**
-dontwarn androidx.room.**
# Optimize: allow aggressive shrinking
-optimizationpasses 5
-allowaccessmodification
-repackageclasses
# APK compression - strip logs, debug, and unused code
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
-assumenosideeffects class expected.keyboard2.Logs {
    public static *** d(...);
    public static *** debug*(...);
}
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 7
# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
