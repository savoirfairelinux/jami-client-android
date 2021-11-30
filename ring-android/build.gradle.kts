plugins {
    id("org.jetbrains.dokka") version ("1.5.30")
}

buildscript {
    repositories {
        google()
        maven { url = uri( "https://maven.google.com") }
        mavenCentral()
    }

    val kotlin_version by extra { "1.5.31" }
    val hilt_version by extra { "2.40" }

    dependencies {
        classpath ("com.android.tools.build:gradle:7.0.3")
        classpath ("com.google.gms:google-services:4.3.10")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath ("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    }
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(buildDir.resolve("dokkaCustomMultiModuleOutput"))
}

allprojects {
    repositories {
        google()
        maven { url = uri("https://maven.google.com") }
        mavenCentral()
    }
}
