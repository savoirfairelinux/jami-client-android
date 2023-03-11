val kotlin_version: String by rootProject.extra
val hilt_version: String by rootProject.extra
val dokka_version: String by rootProject.extra

plugins {
    id("kotlin")
    id("java")
    kotlin("kapt")
}

dependencies {
    // VCard parsing
    implementation ("com.googlecode.ez-vcard:ez-vcard:0.12.0")
    // QRCode encoding
    implementation ("com.google.zxing:core:3.5.1")
    // dependency injection
    implementation( "javax.inject:javax.inject:1")
    // ORM
    implementation ("com.j256.ormlite:ormlite-core:5.7")

    // Required -- JUnit 4 framework
    testImplementation ("junit:junit:4.13.2")
    // RxJava
    implementation ("io.reactivex.rxjava3:rxjava:3.1.6")
    // gson
    implementation ("com.google.code.gson:gson:2.10")
    api("com.google.dagger:dagger:$hilt_version")
    kapt("com.google.dagger:dagger-compiler:$hilt_version")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

