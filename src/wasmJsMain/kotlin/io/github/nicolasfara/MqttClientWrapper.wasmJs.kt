@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("MatchingDeclarationName")

package io.github.nicolasfara

import io.github.nicolasfara.facade.MqttClient
import io.github.nicolasfara.facade.connectAsync
import kotlinx.coroutines.await

@Suppress("TooManyFunctions")
internal actual class MqttClientWrapper actual constructor() {
    private var mqttClient: MqttClient? = null

    actual val isConnected: Boolean get() = mqttClient != null

    actual suspend fun connect(url: String, config: MqttClientConfiguration) {
        mqttClient = connectAsync(url, config.buildConnectOptions()).await()
    }

    actual fun setMessageCallback(callback: (topic: String, message: ByteArray, qos: Int, retain: Boolean) -> Unit) {
        mqttClient?.on("message") { topic, message, packet ->
            if (packet.cmd == "publish") {
                callback(topic, message.toByteArray(), packet.qos, packet.retain)
            }
        }
    }

    actual fun clearMessageCallback() {
        mqttClient?.off("message") { _: JsAny? -> }
    }

    actual suspend fun subscribe(topic: String) {
        mqttClient?.subscribeAsync(topic)?.await<JsAny?>()
    }

    actual suspend fun unsubscribe(topic: String) {
        mqttClient?.unsubscribeAsync(topic)?.await<JsAny?>()
    }

    actual suspend fun publish(topic: String, message: String, qos: Int) {
        mqttClient?.publishAsync(topic, message, jsCreatePublishOptionsRaw(qos)!!)?.await<JsAny?>()
    }

    actual suspend fun end() {
        mqttClient?.endAsync()?.await<JsAny?>()
        mqttClient = null
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
// body.  These functions return JsAny? so that callers can cast/convert
// the result without violating the restriction.
// Parameters are intentionally referenced inside the js("...") string literal
// so they appear unused to static analysis tools.
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
