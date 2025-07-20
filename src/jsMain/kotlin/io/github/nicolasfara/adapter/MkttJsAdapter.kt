package io.github.nicolasfara.adapter

import io.github.nicolasfara.MqttClientConfiguration
import io.github.nicolasfara.facade.IClientOptions
import io.github.nicolasfara.facade.IMessageIdProvider
import io.github.nicolasfara.facade.MqttClient
import io.github.nicolasfara.facade.Server

internal object MkttJsAdapter {
    fun MqttClientConfiguration.toMqttjs(): IClientOptions = object : IClientOptions {
        override var reschedulePings: Boolean? = null
        override var servers: Array<Server>? = null
        override var resubscribe: Boolean? = true
        override var transformWsUrl: ((String, IClientOptions, MqttClient) -> String)? = null
        override var createWebsocket: ((String, Array<String>, IClientOptions) -> Any)? = null
        override var messageIdProvider: IMessageIdProvider? = null
        override var browserBufferTimeout: Number? = null
        override var objectMode: Boolean? = null
        override var clientId: String? = this@toMqttjs.clientId
        override var protocolVersion: Number? = 5
        override var protocolId: String? = "MQTT"
        override var clean: Boolean? = cleanSession
        override var keepalive: Number? = keepAliveInterval
        override var username: String? = this@toMqttjs.username
        override var password: Any? = this@toMqttjs.password
        override var will: Any? = null // TODO: Implement will
        override var properties: Any? = null
        override var timerVariant: Any? = "auto"
        override var forceNativeWebSocket: Boolean? = null
    }
}
