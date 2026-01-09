import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val hilt_version: String by rootProject.extra

plugins {
    id("kotlin")
    id("java")
    kotlin("kapt")
}

dependencies {
    // VCard parsing
    implementation ("com.googlecode.ez-vcard:ez-vcard:0.12.1"){
        exclude(group= "org.jsoup", module= "jsoup")
        exclude(group= "org.freemarker", module= "freemarker")
        exclude(group= "com.fasterxml.jackson.core", module= "jackson-core")
    }
    // QRCode encoding
    implementation ("com.google.zxing:core:3.5.4")
    // dependency injection
    implementation( "javax.inject:javax.inject:1")
    // ORM
    implementation ("com.j256.ormlite:ormlite-core:5.7")

    // Required -- JUnit 4 framework
    testImplementation ("junit:junit:4.13.2")
    // RxJava
    implementation ("io.reactivex.rxjava3:rxjava:3.1.12")
    // gson
    implementation ("com.google.code.gson:gson:2.13.2")
    implementation("com.google.dagger:dagger:$hilt_version")
    kapt("com.google.dagger:dagger-compiler:$hilt_version")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}