package it.nicolasfarabegoli.mktt.facade

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration

external interface MqttProtocol
external interface IClientOptions {
    var host: String?
    var port: Number?
    var clientId: String?
    var username: String?
    var password: String?
    var clean: Boolean?
    var keepalive: Number?
    var connectTimeout: Number?
    var protocol: String?
}

fun MqttConfiguration.toClientOptions(): IClientOptions {
    return object : IClientOptions {
        override var host: String? = this@toClientOptions.hostname
        override var port: Number? = this@toClientOptions.port
        override var clientId: String? = this@toClientOptions.clientId
        override var username: String? = this@toClientOptions.username
        override var password: String? = this@toClientOptions.password
        override var clean: Boolean? = this@toClientOptions.cleanSession
        override var keepalive: Number? = this@toClientOptions.keepAliveInterval
        override var connectTimeout: Number? = this@toClientOptions.connectionTimeout
        override var protocol: String? = this@toClientOptions.protocol.representation
    }
}
