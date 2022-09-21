buildscript {
    repositories {
        google()
        maven { url = uri( "https://maven.google.com") }
        mavenCentral()
    }

    val kotlin_version by extra { "1.7.10" }
    val hilt_version by extra { "2.43.2" }

    dependencies {
        classpath ("com.android.tools.build:gradle:8.0.0-alpha01")
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
