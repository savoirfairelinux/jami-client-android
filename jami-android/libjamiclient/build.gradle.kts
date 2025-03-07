import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val kotlin_version: String by rootProject.extra
val hilt_version: String by rootProject.extra
val dokka_version: String by rootProject.extra

plugins {
    kotlin("kapt")
    id("com.android.library")
    kotlin("android")
}

dependencies {
    // VCard parsing
    implementation ("com.googlecode.ez-vcard:ez-vcard:0.11.3"){
        exclude(group= "org.jsoup", module= "jsoup")
        exclude(group= "org.freemarker", module= "freemarker")
        exclude(group= "com.fasterxml.jackson.core", module= "jackson-core")
    }
    // QRCode encoding
    implementation ("com.google.zxing:core:3.5.3")
    // dependency injection
    implementation( "javax.inject:javax.inject:1")
    // ORM
    implementation ("com.j256.ormlite:ormlite-core:5.7")

    // Required -- JUnit 4 framework
    testImplementation ("junit:junit:4.13.2")
    // RxJava
    implementation ("io.reactivex.rxjava3:rxjava:3.1.9")
    // gson
    implementation ("com.google.code.gson:gson:2.11.0")
    api("com.google.dagger:dagger:$hilt_version")
    kapt("com.google.dagger:dagger-compiler:$hilt_version")

    implementation ("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "28.0.13004108"
    namespace = "libjamiclient"

    defaultConfig {
        minSdk = 24
        externalNativeBuild {
            cmake {
                version = "3.22.1"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DBUILD_CONTRIB=ON",
                    "-DBUILD_EXTRA_TOOLS=OFF",
                    "-DJAMI_TESTS=OFF",
                    "-DBUILD_TESTING=OFF",
                    "-DJAMI_JNI=ON",
                    "-DJAMI_JNI_PACKAGEDIR="+rootProject.projectDir.resolve("libjamiclient/src/main/java"),
                    "-DJAMI_DATADIR=/data/data/$namespace/files",
                    "-DJAMI_NATPMP=Off"
                )
            }
            ndk {
                debugSymbolLevel = "FULL"
                abiFilters += properties["archs"]?.toString()?.split(",") ?: listOf("arm64-v8a", "x86_64", "armeabi-v7a")
                println ("Building for ABIs $abiFilters")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    externalNativeBuild {
        cmake {
            path = file("../../daemon/CMakeLists.txt")
            version = "3.22.1"
        }
    }
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