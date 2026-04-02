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
 * TODO!
 */
fun MqttClient(config: MqttClientConfig): MqttClient = DefaultMqttClient(config)
