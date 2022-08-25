val kotlin_version: String by rootProject.extra
val hilt_version: String by rootProject.extra
val dokka_version: String by rootProject.extra
val archs: CharSequence by project
val buildFirebase = project.hasProperty("buildFirebase") || gradle.startParameter.taskRequests.toString().contains("Firebase")

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdk = 32
    buildToolsVersion = "32.0.0"
    ndkVersion = "25.0.8528842"
    defaultConfig {
        minSdk = 24
        targetSdk = 32
        versionCode = 340
        versionName = "20220825-01"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDir( "src/main/libs")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            packagingOptions {
                jniLibs.keepDebugSymbols += "**/*.so"
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    flavorDimensions += "push"
    productFlavors {
        create("noPush") {
            dimension = "push"
        }
        create("withFirebase") {
            dimension = "push"
        }
    }
    signingConfigs {
        create("config") {
            keyAlias = "ring"
            storeFile = file("../keystore.bin")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            val sp = archs.split(",")
            include("armeabi-v7a, arm64-v8a, x86_64")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    namespace = "cx.ring"
}

val markwon_version = "4.6.2"

dependencies {
    implementation (project(":libjamiclient"))
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation ("androidx.core:core-ktx:1.8.0")
    implementation ("androidx.appcompat:appcompat:1.4.2")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.legacy:legacy-support-core-utils:1.0.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.preference:preference-ktx:1.2.0")
    implementation ( "androidx.recyclerview:recyclerview:1.2.1")
    implementation ( "androidx.leanback:leanback:1.2.0-alpha02")
    implementation ("androidx.leanback:leanback-preference:1.2.0-alpha02")
    implementation ("androidx.tvprovider:tvprovider:1.1.0-alpha01")
    implementation ("androidx.media:media:1.6.0")
    implementation ("androidx.sharetarget:sharetarget:1.2.0-rc02")
    implementation ("androidx.percentlayout:percentlayout:1.0.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation ("com.google.android.material:material:1.6.1")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("org.osmdroid:osmdroid-android:6.1.13")
    implementation ("io.noties.markwon:core:$markwon_version")
    implementation ("io.noties.markwon:linkify:$markwon_version")

    // ORM
    implementation ("com.j256.ormlite:ormlite-android:5.6")

    // Barcode scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
    implementation ("com.google.zxing:core:3.5.0")

    // Dagger dependency injection
    implementation("com.google.dagger:hilt-android:$hilt_version")
    kapt("com.google.dagger:hilt-android-compiler:$hilt_version")

    // Espresso Unit Tests
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")

    // Glide
    implementation ("com.github.bumptech.glide:glide:4.13.2")
    kapt ("com.github.bumptech.glide:compiler:4.13.2")

    // RxAndroid
    implementation ("io.reactivex.rxjava3:rxandroid:3.0.0")
    implementation ("io.reactivex.rxjava3:rxjava:3.1.4")

    implementation ("com.jsibbold:zoomage:1.3.1")
    implementation ("com.googlecode.ez-vcard:ez-vcard:0.11.3")

    "withFirebaseImplementation"("com.google.firebase:firebase-messaging:23.0.7") {
        exclude(group= "com.google.firebase", module= "firebase-core")
        exclude(group= "com.google.firebase", module= "firebase-analytics")
        exclude(group= "com.google.firebase", module= "firebase-measurement-connector")
    }
}

kapt {
    correctErrorTypes = true
}

if (buildFirebase) {
    println ("apply plugin $buildFirebase")
    apply(plugin = "com.google.gms.google-services")
}
