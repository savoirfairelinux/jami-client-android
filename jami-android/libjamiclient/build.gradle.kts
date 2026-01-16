import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kotlin")
    id("java")
    kotlin("kapt")
}

dependencies {
    // VCard parsing
    implementation(libs.ez.vcard){
        exclude(group= "org.jsoup", module= "jsoup")
        exclude(group= "org.freemarker", module= "freemarker")
        exclude(group= "com.fasterxml.jackson.core", module= "jackson-core")
    }
    // QRCode encoding
    implementation(libs.zxing.core)
    // dependency injection
    implementation(libs.javax.inject)
    // ORM
    implementation(libs.ormlite.core)

    // Required -- JUnit 4 framework
    testImplementation(libs.junit)
    // RxJava
    implementation(libs.rxjava)
    // gson
    implementation(libs.gson)
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)
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