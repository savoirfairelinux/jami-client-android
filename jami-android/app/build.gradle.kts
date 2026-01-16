import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val buildFirebase = project.hasProperty("buildFirebase") || gradle.startParameter.taskRequests.toString().contains("Firebase")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp)
}

android {
    namespace = "cx.ring"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.14206865"
    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 481
        versionName = "20260114-01"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                version = "4.1.2"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DBUILD_CONTRIB=ON",
                    "-DBUILD_EXTRA_TOOLS=OFF",
                    "-DBUILD_TESTING=OFF",
                    "-DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON",
                    "-DJAMI_JNI=ON",
                    "-DJAMI_JNI_PACKAGEDIR="+rootProject.projectDir.resolve("libjamiclient/src/main/java"),
                    "-DJAMI_DATADIR=/data/data/$namespace/files",
                )
            }
            ndk {
                debugSymbolLevel = "FULL"
                abiFilters += properties["archs"]?.toString()?.split(",") ?: listOf("arm64-v8a", "x86_64", "armeabi-v7a")
                println ("Building for ABIs $abiFilters")
            }
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    flavorDimensions += "push"
    productFlavors {
        create("noPush") {
            dimension = "push"
        }
        create("withFirebase") {
            dimension = "push"
        }
        create("withUnifiedPush") {
            dimension = "push"
        }
    }
    signingConfigs {
        create("config") {
            keyAlias = "ring"
            storeFile = file("../keystore.bin")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    externalNativeBuild {
        cmake {
            path = file("../../daemon/CMakeLists.txt")
            version = "4.1.2"
        }
    }
}

dependencies {
    implementation(project(":libjamiclient"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.leanback.preference)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.tvprovider)
    implementation(libs.androidx.media)
    implementation(libs.androidx.sharetarget)
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.window)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.material)
    implementation(libs.androidx.biometric)
    implementation(libs.flexbox)
    implementation(libs.protobuf.javalite)
    implementation(libs.androidx.annotation.jvm)

    // ORM
    implementation(libs.ormlite.android)

    // Barcode scanning
    implementation(libs.zxing.android.embedded) { isTransitive = false }
    implementation(libs.zxing.core)

    // Dagger dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Espresso Unit Tests
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.core)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)
    // Android SVG
    implementation(libs.androidsvg)

    // RxAndroid
    implementation(libs.rxandroid)
    implementation(libs.rxjava)

    // OpenStreetMap
    implementation(libs.osmdroid)

    // Markwon (Markdown support)
    implementation(libs.markwon.core)
    implementation(libs.markwon.linkify)

    implementation(libs.zoomage)
    implementation(libs.ez.vcard) {
        exclude(group= "org.freemarker", module= "freemarker")
        exclude(group= "com.fasterxml.jackson.core", module= "jackson-core")
    }

    "withFirebaseImplementation"(libs.firebase.messaging) {
        exclude(group= "com.google.firebase", module= "firebase-core")
        exclude(group= "com.google.firebase", module= "firebase-analytics")
        exclude(group= "com.google.firebase", module= "firebase-measurement-connector")
    }
    "withUnifiedPushImplementation"(libs.unifiedpush.connector)  {
        exclude(group= "com.google.protobuf", module= "protobuf-java")
    }
    "withUnifiedPushImplementation"(libs.unifiedpush.connector.ui)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

if (buildFirebase) {
    println ("apply plugin $buildFirebase")
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Make sure the native build runs before the Kotlin/Java build
afterEvaluate {
    val cmakeTasks = tasks.matching { it.name.startsWith("buildCMake") }
    tasks.withType<KotlinCompile>().configureEach { dependsOn(cmakeTasks) }
}
