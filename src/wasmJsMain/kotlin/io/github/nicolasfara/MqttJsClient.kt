@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("TooManyFunctions")

package io.github.nicolasfara

import io.github.nicolasfara.facade.MqttClient
import io.github.nicolasfara.facade.PacketInfo
import io.github.nicolasfara.facade.connectAsync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class MqttJsClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
) : MkttClient {
    private var mqttClient: MqttClient? = null
    private lateinit var messageFlow: Flow<MqttMessage>
    private val subscribedTopics = mutableMapOf<String, Flow<MqttMessage>>()
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)

    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Disconnected) {
            "Client is already connected or connecting"
        }
        _connectionState.value = MqttConnectionState.Connecting
        val scheme = if (configuration.ssl) "mqtts" else "mqtt"
        val brokerString = "$scheme://${configuration.brokerUrl}:${configuration.port}"
        try {
            mqttClient = connectAsync(brokerString, configuration.buildConnectOptions()).await()
            _connectionState.value = MqttConnectionState.Connected
            messageFlow = callbackFlow {
                mqttClient?.on("message") { topic: String, message: JsAny, packet: PacketInfo ->
                    if (packet.cmd == "publish") {
                        val msg = MqttMessage(topic, message.toByteArray(), MqttQoS.from(packet.qos), packet.retain)
                        trySend(msg).onFailure { error -> close(error) }
                    }
                }
                awaitClose {
                    mqttClient?.off("message") { _: JsAny? -> }
                }
            }
        } finally {
            if (_connectionState.value is MqttConnectionState.Connecting) {
                _connectionState.value = MqttConnectionState.Disconnected
            }
        }
    }

    override suspend fun disconnect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Connected) {
            "Client is not connected"
        }
        mqttClient?.endAsync()?.await<JsAny?>()
        mqttClient = null
        _connectionState.value = MqttConnectionState.Disconnected
    }

    override suspend fun publish(topic: String, message: ByteArray, qos: MqttQoS): Unit = withContext(dispatcher) {
        val opts = jsCreatePublishOptionsRaw(qos.code)!!
        mqttClient?.publishAsync(topic, message.decodeToString(), opts)?.await<JsAny?>()
        Unit
    }

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = subscribedTopics.getOrPut(topic) {
        flow {
            require(mqttClient != null) { "Client not initialized" }
            mqttClient?.subscribeAsync(topic)?.await<JsAny?>()
            emitAll(messageFlow.filter { matchesTopicFilter(it.topic, topic) })
        }.cancellable().flowOn(dispatcher)
    }

    override suspend fun unsubscribe(topic: String): Unit = withContext(dispatcher) {
        mqttClient?.unsubscribeAsync(topic)?.await<JsAny?>()
        subscribedTopics.remove(topic)
    }

    private fun matchesTopicFilter(topic: String, filter: String): Boolean {
        val regexPattern =
            filter
                .replace("+", "[^/]+")
                .replace("#", ".*")
                .let { "^$it$" }
        return Regex(regexPattern).matches(topic)
    }

    private fun MqttClientConfiguration.buildConnectOptions(): JsAny {
        val cidJs = clientId.toJsString()
        val cleanVal = cleanSession
        val keepAliveVal = keepAliveInterval.toInt()
        val userJs = username?.toJsString()
        val passJs = password?.toJsString()
        val willJs = will?.buildJsWill()
        return jsCreateConnectOptionsRaw(cidJs, cleanVal, keepAliveVal, userJs, passJs, willJs)!!
    }

    private fun MqttWill.buildJsWill(): JsAny {
        val topicJs = topic.toJsString()
        val payloadJs = message.decodeToString().toJsString()
        val qosVal = qos.code
        val retainVal = retained
        return jsCreateWillRaw(topicJs, payloadJs, qosVal, retainVal)!!
    }
}

/** Converts a JS Buffer / Uint8Array to a Kotlin [ByteArray]. */
private fun JsAny.toByteArray(): ByteArray {
    val len = jsBufferLengthInt(this)
    return ByteArray(len) { i -> jsBufferAtByte(this, i) }
}

private fun jsBufferLengthInt(buf: JsAny): Int = (jsBufferLengthRaw(buf) as JsNumber).toInt()

private fun jsBufferAtByte(buf: JsAny, index: Int): Byte =
    (jsBufferAtRaw(buf, index) as JsNumber).toDouble().toInt().toByte()

// ---------------------------------------------------------------------------
// Top-level single-expression functions wrapping js() calls.
// In Kotlin/Wasm js() must be the SOLE expression in a top-level function
// body.
// ---------------------------------------------------------------------------

@Suppress("UnusedParameter")
private fun jsBufferLengthRaw(buf: JsAny): JsAny? = js("buf.length")

@Suppress("UnusedParameter")
private fun jsBufferAtRaw(buf: JsAny, index: Int): JsAny? = js("buf[index]")

@Suppress("LongParameterList", "UnusedParameter", "MaxLineLength")
private fun jsCreateConnectOptionsRaw(
    clientId: JsString,
    clean: Boolean,
    keepAlive: Int,
    username: JsString?,
    password: JsString?,
    will: JsAny?,
): JsAny? = js("({ clientId: clientId, protocolVersion: 5, protocolId: 'MQTT', clean: clean, keepalive: keepAlive, username: username, password: password, will: will, timerVariant: 'auto', resubscribe: true })")

@Suppress("UnusedParameter")
private fun jsCreateWillRaw(topic: JsString, payload: JsString, qos: Int, retain: Boolean): JsAny? =
    js("({ topic: topic, payload: payload, qos: qos, retain: retain })")

@Suppress("UnusedParameter")
private fun jsCreatePublishOptionsRaw(qos: Int): JsAny? =
    js("({ qos: qos, retain: true, dup: false, properties: null })")
