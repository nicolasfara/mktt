package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.engine.DefaultEngineConfig
import io.github.nicolasfara.mktt.engine.DefaultEngineFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Creates a new MQTT client, connecting to the specified host on the specified port.
 *
 * To enable TLS on the connection, use the following code snippet:
 * ```
 * MqttClient("test.mosquitto.org", 8886) {
 *     connection {
 *         tls { }
 *     }
 *     ...
 * }
 * ```
 */
fun MqttClient(
    host: String,
    port: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    init: MqttClientConfigBuilder<DefaultEngineConfig>.() -> Unit,
): MqttClient =
    DefaultMqttClient(MqttClientConfigBuilder(DefaultEngineFactory(host, port, dispatcher)).apply(init).build())

/**
 * Creates a new MQTT client from a fully configured [MqttClientConfig].
 *
 * Use this overload when you want to construct the configuration separately
 * (for example via [buildConfig]) and then create the client instance from it.
 */
fun MqttClient(config: MqttClientConfig): MqttClient = DefaultMqttClient(config)
