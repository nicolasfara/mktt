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
 * Configures the websocket passing the [url] at which the client should connect to.
 */
class WebSocketEngineConfig(val url: Url) : MqttEngineConfig() {
    /**
     * Configures the http client to be used.
     */
    var http: () -> HttpClient = {
        HttpClient {
            install(WebSockets)
        }
    }
}
