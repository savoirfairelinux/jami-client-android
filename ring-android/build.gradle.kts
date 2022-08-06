buildscript {
    repositories {
        google()
        maven { url = uri( "https://maven.google.com") }
        mavenCentral()
    }

    val kotlin_version by extra { "1.7.10" }
    val hilt_version by extra { "2.42" }

    dependencies {
        classpath ("com.android.tools.build:gradle:7.2.1")
        classpath ("com.google.gms:google-services:4.3.13")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath ("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    }
}
allprojects {
    repositories {
        google()
        maven { url = uri("https://maven.google.com") }
        mavenCentral()
    }
}
