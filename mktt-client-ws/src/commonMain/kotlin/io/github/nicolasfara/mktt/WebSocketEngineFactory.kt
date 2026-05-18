package io.github.nicolasfara.mktt

import io.github.nicolasfara.mktt.engine.MqttEngine
import io.github.nicolasfara.mktt.engine.MqttEngineConfig
import io.github.nicolasfara.mktt.engine.MqttEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineDispatcher

internal class WebSocketEngineFactory(private val url: Url, val dispatcher: CoroutineDispatcher) :
    MqttEngineFactory<WebSocketEngineConfig> {
    override fun create(block: WebSocketEngineConfig.() -> Unit): MqttEngine =
        WebSocketEngine(WebSocketEngineConfig(url).apply(block), dispatcher)
}

/**
 * WebSocket transport configuration.
 *
 * @property url broker URL used by the WebSocket engine.
 */
class WebSocketEngineConfig(val url: Url) : MqttEngineConfig() {
    /**
     * Factory for the Ktor [HttpClient] used by the WebSocket engine.
     *
     * The default client installs the Ktor [WebSockets] plugin.
     */
    var http: () -> HttpClient = {
        HttpClient {
            install(WebSockets)
        }
    }
}
