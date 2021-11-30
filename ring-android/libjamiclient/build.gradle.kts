import java.net.URL

val kotlin_version: String by rootProject.extra
val hilt_version: String by rootProject.extra

plugins {
    id("kotlin")
    id("java")
    kotlin("kapt")
    id("org.jetbrains.dokka")
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        suppressInheritedMembers.set(true)
        suppressObviousFunctions.set(true)
        configureEach {
            //includes.from("Module.md")
            sourceLink {
                localDirectory.set(file("src/main/kotlin")) // Unix based directory relative path to the root of the project (where you execute gradle respectively).
                remoteUrl.set(URL("https://git.jami.net/savoirfairelinux/jami-client-android/-/blob/master/ring-android/libjamiclient/src/main/kotlin")) // URL showing where the source code can be accessed through the web browser
                remoteLineSuffix.set("#L") // Suffix which is used to append the line number to the URL. Use #L for GitHub
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation ("com.googlecode.ez-vcard:ez-vcard:0.11.3")      // VCard parsing
    implementation ("com.google.zxing:core:3.4.1")                  // QRCode encoding
    implementation( "javax.inject:javax.inject:1")                  // dependency injection
    implementation ("com.j256.ormlite:ormlite-core:5.6")            // ORM
    testImplementation ("junit:junit:4.13.2")                       // Required -- JUnit 4 framework
    implementation ("io.reactivex.rxjava3:rxjava:3.1.2")            // RxJava
    implementation ("com.google.code.gson:gson:2.8.9")              // gso
    api("com.google.dagger:dagger:$hilt_version")
    kapt("com.google.dagger:dagger-compiler:$hilt_version")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

