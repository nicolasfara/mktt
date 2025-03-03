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

# Examples

## Publishing

```kotlin
val mqttClient = MkttClient(Diaptchers.IO) {
    brokerUrl = "localhost"
    port = 1883
    clientId = "mktt-client"
}
mqttClient.connect()
mqttClient.publish(
    topic = "test/topic",
    qos = MqttQoS.ExactlyOnce,
    "hello world".encodeToByteArray(),
)
mqttClient.disconnect()
```

## Subscribing

```kotlin
val mqttClient = MkttClient(Diaptchers.IO) {
    brokerUrl = "localhost"
    port = 1883
    clientId = "mktt-client"
}
mqttClient.connect()
mqttClient.subscribe("test/topic", MqttQoS.ExactlyOnce).collect {
    println("Received message: ${it.payload.decodeToString()}")
}
mqttClient.disconnect()
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

# License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

See the [LICENSE](LICENSE) file for more details.
