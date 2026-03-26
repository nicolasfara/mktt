# MKTT

MQTT 5 for Kotlin Multiplatform, split into:

- `mktt-core`: protocol types, properties, packets, codecs
- `mktt-client`: coroutine and Flow based client built on top of `mktt-core`

## Dependencies

```kotlin
dependencies {
    implementation("io.github.nicolasfara:mktt-core:<version>")
    implementation("io.github.nicolasfara:mktt-client:<version>")
}
```

## Example

```kotlin
import io.github.nicolasfara.mktt.client.MqttClient
import io.github.nicolasfara.mktt.client.PublishRequest
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import kotlinx.coroutines.flow.first

suspend fun sample() {
    val client = MqttClient("localhost", 1883) {
        clientId = "mktt-sample"
    }

    client.connect()
    client.subscribe(listOf(TopicFilter(Topic("sample/topic"))))

    client.publish(
        PublishRequest("sample/topic") {
            desiredQoS = QoS.AT_LEAST_ONCE
            payload("hello")
        },
    )

    val message = client.messages(TopicFilter(Topic("sample/topic"))).first()
    println(message.payloadAsString())

    client.disconnect()
    client.close()
}
```

## Modules

`mktt-core` exposes:

- MQTT 5 reason codes and properties
- topic and subscription models
- packet definitions for CONNECT, CONNACK, PUBLISH, PUBACK/PUBREC/PUBREL/PUBCOMP, SUBSCRIBE, UNSUBSCRIBE, PING and DISCONNECT
- packet encode/decode utilities over Ktor channels and `kotlinx-io`

`mktt-client` exposes:

- `MqttClient`
- `StateFlow<MqttConnectionState>`
- `SharedFlow<MqttPublishMessage>`
- `messages(filter)` local filtering helper
- coroutine-based `connect`, `publish`, `subscribe`, `unsubscribe`, and `disconnect`

## Testing

- `mktt-core` is covered by deterministic packet/property round-trip tests.
- `mktt-client` keeps fast unit tests in `commonTest`/`jvmTest` and runs broker-based checks in `jvmIntegrationTest`.

Run unit tests:

```bash
./gradlew unitTest
```

Run integration tests against the local Mosquitto container scaffold (`mktt-client/src/jvmIntegrationTest/resources`):

```bash
MKTT_RUN_INTEGRATION_TESTS=true ./gradlew integrationTest
```

Optional public broker test (opt-in only):

```bash
MKTT_RUN_INTEGRATION_TESTS=true MKTT_RUN_REMOTE_BROKER_TESTS=true ./gradlew integrationTest
```
