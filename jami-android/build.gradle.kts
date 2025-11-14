plugins {
    id("com.google.devtools.ksp") version "2.3.2" apply false
}

buildscript {
    repositories {
        google()
        maven { url = uri( "https://maven.google.com") }
        mavenCentral()
    }

    val kotlin_version by extra { "2.2.21" }
    val hilt_version by extra { "2.57.2" }

    dependencies {
        classpath ("com.android.tools.build:gradle:8.13.1")
        classpath ("com.google.gms:google-services:4.4.4")
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
