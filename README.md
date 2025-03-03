<p align="center">
    <img alt="MKTT logo" src="mktt-logo.svg">
</p>

> A Kotlin Multiplatform, coroutine-based MQTT client library

# Dependencies

```kotlin
implementation("it.nicolasfarabegoli:mktt:<version>")
```

or using [Gradle catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html):

```kotlin
[versions]
mktt-version = "<version>"

[dependencies]
mktt = { module = "it.nicolasfarabegoli:mktt", version.ref = mktt-version }
```

# Supported Platforms

| Platform    | Target                                                      | Description                                  | Supported          |
|-------------|-------------------------------------------------------------|----------------------------------------------|--------------------|
| **JVM**     | `jvm()`                                                     | Java Virtual Machine (backend, desktop apps) | :white_check_mark: |
| **Android** | `android()`                                                 | Native Android development                   | :white_check_mark: |
| **iOS**     | `ios()`, `iosArm64()`, `iosX64()`, `iosSimulatorArm64()`    | Apple iOS devices (real & simulator)         | :x:                |
| **macOS**   | `macosX64()`, `macosArm64()`                                | macOS applications                           | :x:                |
| **watchOS** | `watchosX64()`, `watchosArm64()`, `watchosSimulatorArm64()` | Apple Watch apps                             | :x:                |
| **tvOS**    | `tvosX64()`, `tvosArm64()`, `tvosSimulatorArm64()`          | Apple TV apps                                | :x:                |
| **Linux**   | `linuxX64()`, `linuxArm64()`, `linuxArm32Hfp()`             | Linux native applications                    | :x:                |
| **Windows** | `mingwX64()`                                                | Windows native applications                  | :x:                |
| **Web**     | `js()`                                                      | JavaScript (both browser & Node.js)          | :white_check_mark: |
| **Wasm**    | `wasmJs()`, `wasmWasi()`                                    | WebAssembly (browser & standalone)           | :x:                |
