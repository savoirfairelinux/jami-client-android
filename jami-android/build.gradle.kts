plugins {
    alias(libs.plugins.android.application) apply false

    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.google.services) apply false
}

allprojects {
    repositories {
        google()
        maven { url = uri("https://maven.google.com") }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Huawei Mobile Services, only resolved by the withHmsPush flavor.
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
