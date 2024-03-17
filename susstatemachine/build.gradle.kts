import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.mockmp)
    alias(libs.plugins.maven.publish)
}

kotlin {

    jvm()
    androidTarget()
//    androidNativeX64() // todo requires kotest 5.9.0
//    androidNativeX86() // todo requires kotest 5.9.0
//    androidNativeArm32() // todo requires kotest 5.9.0
//    androidNativeArm64() // todo requires kotest 5.9.0
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
//    watchosDeviceArm64() // todo requires kotest 5.9.0
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    linuxX64()
//    mingwX64() // todo requires kotest 5.9.0
    macosX64()
    macosArm64()
    linuxArm64()
    js {
        browser()
        nodejs()
    }
//    wasmJs { // todo requires kotest 5.9.0
//        browser()
//        nodejs()
//    }
//    wasmWasi() // todo requires coroutines update


    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions)
            implementation(libs.kodein.di)
        }
    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xexport-kdoc")
    }

}

mockmp {
    usesHelper = true
    installWorkaround()
}

android {
    namespace = "io.github.hylkeb"
    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Help found here: https://github.com/vanniktech/gradle-maven-publish-plugin/?tab=readme-ov-file
// and here: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false) // might want to set this to true later
    signAllPublications()
    val version = System.getenv("GITHUB_REF_NAME")?.drop(1) ?: "0.0.0" // remove v from vX.Y.Z
    coordinates("io.github.hylkeb", "susstatemachine", version)

    pom {
        name.set("SusStateMachine")
        description.set("Kotlin Multiplatform library for a simple suspending state machine")
        inceptionYear.set("2024")
        url.set("https://github.com/HylkeB/SusStateMachine")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://opensource.org/licenses/Apache-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("HylkeB")
                name.set("Hylke Bron")
                email.set("hylke.bron@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/HylkeB/SusStateMachine")
            connection.set("scm:git:git://github.com/HylkeB/SusStateMachine.git")
            developerConnection.set("scm:git:ssh://git@github.com/HylkeB/SusStateMachine.git")
        }
    }
}