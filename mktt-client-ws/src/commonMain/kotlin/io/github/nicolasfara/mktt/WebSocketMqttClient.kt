package io.github.nicolasfara.mktt

import io.github.nicolasfara.mktt.client.DefaultMqttClient
import io.github.nicolasfara.mktt.client.MqttClient
import io.github.nicolasfara.mktt.client.MqttClientConfigBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Creates a new MQTT client using web sockets for connecting to the server.
 *
 * When not configured, this will use the default ktor http client as
 * described [here](https://ktor.io/docs/client-engines.html#default).
 * If you want to specify or configure an engine, for example CIO, use the following code snippet:
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
 * @param url the URL to connect to, this may contain a 'http', 'https', 'ws' or 'wss' protocol
 */
fun MqttClient(
    url: String,
    dispatcher: CoroutineDispatcher,
    init: MqttClientConfigBuilder<WebSocketEngineConfig>.() -> Unit,
): MqttClient = MqttClient(Url(url), dispatcher, init)

/**
 * Creates a new MQTT client using web sockets for connecting to the server.
 *
 * When not configured, this will use the default ktor http client
 * as described [here](https://ktor.io/docs/client-engines.html#default).
 * If you want to specify or configure an engine, for example CIO, use the following code snippet:
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
 * @param url the URL to connect to, this may contain a 'http', 'https', 'ws' or 'wss' protocol
 */
fun MqttClient(
    url: Url,
    dispatcher: CoroutineDispatcher,
    init: MqttClientConfigBuilder<WebSocketEngineConfig>.() -> Unit,
): MqttClient = DefaultMqttClient(MqttClientConfigBuilder(WebSocketEngineFactory(url, dispatcher)).also(init).build())
