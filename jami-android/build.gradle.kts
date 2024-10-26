plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.26" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri( "https://maven.google.com") }
        mavenCentral()
    }

    val kotlin_version by extra { "2.0.21" }
    val hilt_version by extra { "2.52" }

    dependencies {
        classpath ("com.android.tools.build:gradle:8.5.2")
        classpath ("com.google.gms:google-services:4.4.2")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath ("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    }
}
allprojects {
    repositories {
        google()
        maven { url = uri("https://maven.google.com") }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
