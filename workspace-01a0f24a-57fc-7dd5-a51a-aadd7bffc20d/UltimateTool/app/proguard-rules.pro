# LuaJ: интерпретатор активно использует рефлексию внутри себя.
-keep class org.luaj.** { *; }

# Retrofit/Gson DTO: сериализация через рефлексию.
-keep class com.ultimatetool.app.data.remote.** { *; }
-keepclassmembers class com.ultimatetool.app.data.remote.** { *; }

# Markwon
-keep class io.noties.markwon.** { *; }

-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
