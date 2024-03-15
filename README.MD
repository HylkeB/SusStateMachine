# SusStateMachine

Kotlin Multiplatform Library

### Publish to MavenCentral

1) Registering a Sonatype account as described here: 
   https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-going-public-4a8k
2) Add developer id, name, email and the project url to
   `/convention-plugins/src/main/kotlin/convention.publication.gradle.kts`
3) Add the secrets to `local.properties`:

```
signing.keyId=...
signing.password=...
signing.secretKeyRingFile=...
ossrhUsername=...
ossrhPassword=...
```

4) Run `./gradlew :dodo:publishAllPublicationsToSonatypeRepository`

### Build platform artifacts

#### Android aar

- Run `./gradlew :susstatemachine:assembleRelease`
- Output: `/susstatemachine/build/outputs/aar/susstatemachine-release.aar`

#### JVM jar

- Run `./gradlew :susstatemachine:jvmJar`
- Output: `/susstatemachine/build/libs/susstatemachine-jvm-1.0.jar`

#### iOS Framework

- Run `./gradlew :susstatemachine:linkReleaseFrameworkIosArm64`
- Output: `/susstatemachine/build/bin/iosArm64/releaseFramework/susstatemachine.framework`

#### JS file

- Run `./gradlew :susstatemachine:jsBrowserProductionWebpack`
- Output: `/susstatemachine/build/dist/js/productionExecutable/susstatemachine.js`

#### macOS Framework

- Run `./gradlew :susstatemachine:linkReleaseFrameworkMacosArm64`
- Output: `/susstatemachine/build/bin/macosArm64/releaseFramework/susstatemachine.framework`

#### Linux static library

- Run `./gradlew :susstatemachine:linkReleaseStaticLinuxX64`
- Output: `/susstatemachine/build/bin/linuxX64/releaseStatic/libsusstatemachine.a`

#### Windows static library

- Run `./gradlew :susstatemachine:linkReleaseStaticMingwX64`
- Output: `/susstatemachine/build/bin/mingwX64/releaseStatic/libsusstatemachine.a`

#### Wasm binary file

- Run `./gradlew :susstatemachine:wasmJsBrowserDistribution`
- Output: `/susstatemachine/build/dist/wasmJs/productionExecutable/susstatemachine-wasm-js.wasm`
