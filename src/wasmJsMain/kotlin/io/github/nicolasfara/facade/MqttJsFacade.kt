@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicProperty",
    "UndocumentedPublicFunction",
    "TooManyFunctions",
)
@file:JsModule("mqtt")
@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.nicolasfara.facade

import kotlin.js.Promise

external interface IClientOptions : JsAny {
    var reschedulePings: Boolean?
    var resubscribe: Boolean?
    var clientId: String?
    var protocolVersion: Int?
    var protocolId: String?
    var clean: Boolean?
    var keepalive: Int?
    var username: String?
    var password: JsAny?
    var will: JsAny?
    var properties: JsAny?
    var timerVariant: JsAny?
    var forceNativeWebSocket: Boolean?
}

/**
 * Represents the MQTT packet metadata delivered alongside each message.
 */
external interface PacketInfo : JsAny {
    val cmd: String
    val qos: Int
    val retain: Boolean
}

external interface IClientPublishOptions : JsAny {
    var qos: Int?
    var retain: Boolean?
    var dup: Boolean?
    var properties: JsAny?
}

external interface IClientSubscribeOptions : JsAny {
    var qos: Int
}

external interface ISubscriptionGrant : JsAny {
    var topic: String
    var qos: Int
}

external interface MqttClient : JsAny {
    fun on(event: String, callback: (topic: String, message: JsAny, packet: PacketInfo) -> Unit)

    fun on(event: String, callback: (event: JsAny?) -> Unit)

    fun off(event: String, callback: (JsAny?) -> Unit)

    fun removeListener(event: String, callback: (JsAny?) -> Unit)

    fun publishAsync(topic: String, message: String): Promise<JsAny?>

    fun publishAsync(topic: String, message: String, options: JsAny): Promise<JsAny?>

    fun subscribeAsync(topicObject: String): Promise<JsAny?>

    fun subscribeAsync(topic: String, options: IClientSubscribeOptions?): Promise<JsAny?>

    fun unsubscribeAsync(topic: String): Promise<JsAny?>

    fun endAsync(): Promise<JsAny?>

    fun endAsync(force: Boolean?): Promise<JsAny?>
}

external fun connectAsync(brokerUrl: String, options: JsAny = definedExternally): Promise<MqttClient>
