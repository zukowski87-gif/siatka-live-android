# Zachowanie serializatorów Kotlinx Serialization w wersji produkcyjnej R8
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pl.siatkalive.tlk.**$$serializer { *; }
-keepclassmembers class pl.siatkalive.tlk.** {
    *** Companion;
}
-keepclasseswithmembers class pl.siatkalive.tlk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit 2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions
