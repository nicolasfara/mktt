package io.github.nicolasfara

import io.github.nicolasfara.facade.Error
import io.github.nicolasfara.facade.IClientOptions
import io.github.nicolasfara.facade.IClientPublishOptions
import io.github.nicolasfara.facade.IMessageIdProvider
import io.github.nicolasfara.facade.MqttClient
import io.github.nicolasfara.facade.Server
import io.github.nicolasfara.facade.connectAsync
import kotlinx.coroutines.await

@Suppress("TooManyFunctions")
internal actual class MqttClientWrapper actual constructor() {
    private var mqttClient: MqttClient? = null

    actual val isConnected: Boolean get() = mqttClient != null

    actual suspend fun connect(url: String, config: MqttClientConfiguration) {
        mqttClient = connectAsync(url, config.toMqttJs()).await()
    }

    actual fun setMessageCallback(callback: (topic: String, message: ByteArray, qos: Int, retain: Boolean) -> Unit) {
        mqttClient?.on("message") { topic, message, packet ->
            if (packet.cmd == "publish") {
                @Suppress("UNCHECKED_CAST")
                callback(topic, message, packet.qos as Int, packet.retain as Boolean)
            }
        }
    }

    actual fun clearMessageCallback() {
        mqttClient?.off("message") { }
    }

    actual suspend fun subscribe(topic: String) {
        mqttClient?.subscribeAsync(topic)?.await()
    }

    actual suspend fun unsubscribe(topic: String) {
        mqttClient?.unsubscribeAsync(topic)?.await()
    }

    actual suspend fun publish(topic: String, message: String, qos: Int) {
        val publishOption =
            object : IClientPublishOptions {
                override var qos: Number? = qos
                override var retain: Boolean? = true
                override var dup: Boolean? = null
                override var properties: dynamic = null
                override var cbStorePut: ((Error?) -> Unit)? = null
            }
        mqttClient?.publishAsync(topic, message, publishOption)?.await()
    }

    actual suspend fun end() {
        mqttClient?.endAsync()?.await()
        mqttClient = null
    }

    private fun MqttClientConfiguration.toMqttJs(): IClientOptions =
        object : IClientOptions {
            override var reschedulePings: Boolean? = null
            override var servers: Array<Server>? = null
            override var resubscribe: Boolean? = true
            override var transformWsUrl: ((String, IClientOptions, MqttClient) -> String)? = null
            override var createWebsocket: ((String, Array<String>, IClientOptions) -> Any)? = null
            override var messageIdProvider: IMessageIdProvider? = null
            override var browserBufferTimeout: Number? = null
            override var objectMode: Boolean? = null
            override var clientId: String? = this@toMqttJs.clientId
            override var protocolVersion: Number? = 5
            override var protocolId: String? = "MQTT"
            override var clean: Boolean? = cleanSession
            override var keepalive: Number? = keepAliveInterval
            override var username: String? = this@toMqttJs.username
            override var password: Any? = this@toMqttJs.password
            override var will: Any? =
                this@toMqttJs.will?.let { willConfig ->
                    val willObj: dynamic = js("{}")
                    willObj.topic = willConfig.topic
                    willObj.payload = willConfig.message
                    willObj.qos = willConfig.qos.code
                    willObj.retain = willConfig.retained
                    willObj
                }
            override var properties: Any? = null
            override var timerVariant: Any? = "auto"
            override var forceNativeWebSocket: Boolean? = null
        }
}
