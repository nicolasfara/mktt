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
 *
 * @param host broker host name or IP address.
 * @param port broker TCP port.
 * @param dispatcher coroutine dispatcher used by the transport engine.
 * @param init configuration block applied before the client is created.
 * @return a client configured to use the default TCP transport engine.
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
 *
 * @param config immutable client configuration.
 * @return a client using [config].
 */
fun MqttClient(config: MqttClientConfig): MqttClient = DefaultMqttClient(config)
