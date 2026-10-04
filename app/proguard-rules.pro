# Navigation type-safe routes use kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.focusgrowing.app.**$$serializer { *; }

# Keep enum names: Room and DataStore persist enums by name.
-keepclassmembers enum com.focusgrowing.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
