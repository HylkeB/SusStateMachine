import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    id("convention.publication")
    alias(libs.plugins.mockmp)
}

group = "io.github.hylkeb"
version = "1.0"

kotlin {
//    androidTarget {
//        publishLibraryVariants("release")
//        compilations.all {
//            kotlinOptions {
//                jvmTarget = "17"
//            }
//        }
//    }



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
//    wasmWasi() // todo requires coroutine update

//    js {
//        browser {
//            webpackTask {
//                mainOutputFileName = "susstatemachine.js"
//            }
//        }
//        binaries.executable()
//        nodejs()
//    }
//
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "susstatemachine"
//            isStatic = true
//        }
//    }
//
//    listOf(
//        macosX64(),
//        macosArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "susstatemachine"
//            isStatic = true
//        }
//    }
//
//    linuxX64 {
//        binaries.staticLib {
//            baseName = "susstatemachine"
//        }
//    }
//
//
//    mingwX64 {
//        binaries.staticLib {
//            baseName = "susstatemachine"
//        }
//    }

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
//
//        androidMain.dependencies {
//            implementation(libs.kotlinx.coroutines.android)
//        }
//
//        jvmMain.dependencies {
//            implementation(libs.kotlinx.coroutines.swing)
//        }

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

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
