-keepclassmembers class * {
  public <init>(android.content.Context);
}

-keepattributes InnerClasses

# To be able to see line numbers in stack traces
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keep,includedescriptorclasses class androidx.core.content.FileProvider { *; }
-keep,includedescriptorclasses class androidx.sharetarget.* { *; }

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keep,includedescriptorclasses class net.jami.daemon.** { *; }
-keepclassmembers class net.jami.daemon.** { *; }
-keep class cx.ring.tv.settings.TVAboutFragment

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
-keep,includedescriptorclasses class com.github.mangstadt.vinnie.** { *; }
-keepclassmembers class com.github.mangstadt.vinnie.** { *; }

# barcodescanner
-keep,includedescriptorclasses class com.journeyapps.barcodescanner.** { *; }
-keepclassmembers class com.journeyapps.barcodescanner.** { *; }
-keep,includedescriptorclasses class com.google.zxing.** { *; }
-keepclassmembers class com.google.zxing.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector
-dontwarn javax.persistence.Basic
-dontwarn javax.persistence.Column
-dontwarn javax.persistence.Entity
-dontwarn javax.persistence.EnumType
-dontwarn javax.persistence.Enumerated
-dontwarn javax.persistence.FetchType
-dontwarn javax.persistence.GeneratedValue
-dontwarn javax.persistence.Id
-dontwarn javax.persistence.JoinColumn
-dontwarn javax.persistence.ManyToOne
-dontwarn javax.persistence.OneToMany
-dontwarn javax.persistence.OneToOne
-dontwarn javax.persistence.Table
-dontwarn javax.persistence.Version
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.logging.log4j.LogManager
-dontwarn org.apache.logging.log4j.Logger
-dontwarn org.slf4j.ILoggerFactory
-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder
