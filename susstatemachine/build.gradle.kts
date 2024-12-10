import com.vanniktech.maven.publish.SonatypeHost
import dev.mokkery.MockMode
import dev.mokkery.verify.VerifyMode

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.allopen)
}

kotlin {

    jvm()
    androidTarget()
    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    watchosDeviceArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()
    linuxArm64()
    js {
        browser()
        nodejs()
    }
    // WasmJs doesn't pass the tests yet, probably due to mocking limitations
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        browser()
//        nodejs()
//    }
    // WasmWasi is not yet supported by kotest assertions
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmWasi {
//        nodejs()
//    }


    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions)
        }
    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xexport-kdoc")
    }

}

mokkery {
    defaultVerifyMode.set(VerifyMode.exhaustiveOrder)
    defaultMockMode.set(MockMode.autoUnit)
}

allOpen {
    annotation("io.github.hylkeb.susstatemachine.sample.OpenForMocking")
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
// and here: https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-going-public-4a8k
// and here: https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#automating-sonatype-actions
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    val version = System.getenv("GITHUB_REF_NAME")?.drop(1) ?: "0.0.0" // remove v from vX.Y.Z
    println("Resolved version: $version")
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