-keepclassmembers class * {
  public <init>(android.content.Context);
}

-keepattributes InnerClasses

-keep class android.support.v7.widget.LinearLayoutManager { *; }
-keep,includedescriptorclasses class androidx.core.content.FileProvider { *; }

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keep,includedescriptorclasses class cx.ring.** { *; }
-keepclassmembers class cx.ring.** { *; }

# Firebase
-keep class com.firebase.** { *; }
-keep class com.google.firebase.** { *; }

# ORMLite
-keep class com.j256.**
-keepclassmembers class com.j256.** { *; }
-keep enum com.j256.**
-keepclassmembers enum com.j256.** { *; }
-keep interface com.j256.**
-keepclassmembers interface com.j256.** { *; }

-keep class * extends com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper

-keep @com.j256.ormlite.table.DatabaseTable class * {
    @com.j256.ormlite.field.DatabaseField <fields>;
    @com.j256.ormlite.field.ForeignCollectionField <fields>;
    <init>();
}

# other
-dontwarn com.fasterxml.jackson.**
-dontwarn org.jsoup.**
-dontwarn freemarker.**

# EZVcard
-keep,includedescriptorclasses class ezvcard.** { *; }
-keepclassmembers class ezvcard.** { *; }

# barcodescanner
-keep,includedescriptorclasses class com.journeyapps.barcodescanner.** { *; }
-keepclassmembers class com.journeyapps.barcodescanner.** { *; }

# stickylistheaders
-keep,includedescriptorclasses class se.emilsjolander.stickylistheaders.** { *; }
-keepclassmembers class se.emilsjolander.stickylistheaders.** { *; }

# Butterknife
-keep class butterknife.** { *; }
-dontwarn butterknife.**
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}