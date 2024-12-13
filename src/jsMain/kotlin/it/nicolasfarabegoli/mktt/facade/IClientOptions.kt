package it.nicolasfarabegoli.mktt.facade

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration

external interface IClientOptions {
    val host: String?
    val port: Number?
    val clientId: String?
    val username: String?
    val password: String?
    val clean: Boolean?
    val keepalive: Number?
    val connectTimeout: Number?
}

fun MqttConfiguration.toClientOptions(): IClientOptions {
    return object : IClientOptions {
        override val host: String? = this@toClientOptions.hostname
        override val port: Number? = this@toClientOptions.port
        override val clientId: String? = this@toClientOptions.clientId
        override val username: String? = this@toClientOptions.username
        override val password: String? = this@toClientOptions.password
        override val clean: Boolean? = this@toClientOptions.cleanSession
        override val keepalive: Number? = this@toClientOptions.keepAliveInterval
        override val connectTimeout: Number? = this@toClientOptions.connectionTimeout
    }
}