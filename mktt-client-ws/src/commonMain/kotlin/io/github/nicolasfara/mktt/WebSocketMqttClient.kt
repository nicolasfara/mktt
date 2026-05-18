package io.github.nicolasfara.mktt

import io.github.nicolasfara.mktt.client.DefaultMqttClient
import io.github.nicolasfara.mktt.client.MqttClient
import io.github.nicolasfara.mktt.client.MqttClientConfigBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Creates a new MQTT client using web sockets for connecting to the server.
 *
 * When not configured, this uses the
 * [default Ktor HTTP client engine](https://ktor.io/docs/client-engines.html#default). To specify or configure an
 * engine, for example CIO, use:
 *
 * ```
 * MqttClient("https://test.mosquitto.org:8091") {
 *     connection {
 *         http = {
 *             HttpClient(CIO) {
 *                 install(WebSockets)
 *             }
 *         }
 *     }
 *     ...
 * }
 * ```
 *
 * @param url URL to connect to. Supported schemes are `http`, `https`, `ws`, and `wss`.
 * @param dispatcher coroutine dispatcher used by the WebSocket transport engine.
 * @param init configuration block applied before the client is created.
 * @return a client configured to use the WebSocket transport engine.
 */
fun MqttClient(
    url: String,
    dispatcher: CoroutineDispatcher,
    init: MqttClientConfigBuilder<WebSocketEngineConfig>.() -> Unit,
): MqttClient = MqttClient(Url(url), dispatcher, init)

/**
 * Creates a new MQTT client using web sockets for connecting to the server.
 *
 * When not configured, this uses the
 * [default Ktor HTTP client engine](https://ktor.io/docs/client-engines.html#default). To specify or configure an
 * engine, for example CIO, use:
 *
 * ```
 * MqttClient("https://test.mosquitto.org:8091") {
 *     connection {
 *         http = {
 *             HttpClient(CIO) {
 *                 install(WebSockets)
 *             }
 *         }
 *     }
 *     ...
 * }
 * ```
 *
 * @param url URL to connect to. Supported schemes are `http`, `https`, `ws`, and `wss`.
 * @param dispatcher coroutine dispatcher used by the WebSocket transport engine.
 * @param init configuration block applied before the client is created.
 * @return a client configured to use the WebSocket transport engine.
 */
fun MqttClient(
    url: Url,
    dispatcher: CoroutineDispatcher,
    init: MqttClientConfigBuilder<WebSocketEngineConfig>.() -> Unit,
): MqttClient = DefaultMqttClient(MqttClientConfigBuilder(WebSocketEngineFactory(url, dispatcher)).also(init).build())
