@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicProperty",
    "UndocumentedPublicFunction",
    "TooManyFunctions",
)
@file:JsModule("mqtt")
@file:JsNonModule

package it.nicolasfarabegoli.mktt.facade

import kotlin.js.Promise

external interface IClientOptions {
    var reschedulePings: Boolean?
    var servers: Array<Server>?
    var resubscribe: Boolean?
    var transformWsUrl: ((url: String, options: IClientOptions, client: MqttClient) -> String)?
    var createWebsocket: ((url: String, websocketSubProtocols: Array<String>, options: IClientOptions) -> Any)?
    var messageIdProvider: IMessageIdProvider?
    var browserBufferTimeout: Number?
    var objectMode: Boolean?
    var clientId: String?
    var protocolVersion: Number?
    var protocolId: String?
    var clean: Boolean?
    var keepalive: Number?
    var username: String?
    var password: dynamic // Buffer | String
    var will: dynamic
    var properties: dynamic
    var timerVariant: dynamic
    var forceNativeWebSocket: Boolean?
}

external interface Server {
    var host: String
    var port: Number
    var protocol: String?
}

external interface IMessageIdProvider

external interface IClientPublishOptions {
    var qos: Number?
    var retain: Boolean?
    var dup: Boolean?
    var properties: dynamic
    var cbStorePut: ((error: Error?) -> Unit)?
}

external interface IClientSubscribeOptions {
    var qos: Number
    var nl: Boolean?
    var rap: Boolean?
    var rh: Number?
    var properties: dynamic
}

external interface ISubscriptionRequest : IClientSubscribeOptions {
    var topic: String
}

external interface ISubscriptionGrant : ISubscriptionRequest {
    override var qos: Number
}

external interface IClientUnsubscribeProperties {
    var properties: dynamic
}

external interface MqttClient {
    fun connect()

    fun publish(
        topic: String,
        message: String,
        opts: IClientPublishOptions? = definedExternally,
        callback: ((error: Error?, packet: dynamic) -> Unit)? = definedExternally,
    )

    fun subscribe(
        topic: String,
        opts: IClientSubscribeOptions? = definedExternally,
        callback: ((error: Error?, granted: Array<ISubscriptionGrant>?) -> Unit)? = definedExternally,
    )

    fun unsubscribe(
        topic: String,
        opts: IClientUnsubscribeProperties? = definedExternally,
        callback: ((error: Error?, packet: dynamic) -> Unit)? = definedExternally,
    )

    fun end(
        force: Boolean? = definedExternally,
        opts: dynamic = definedExternally,
        callback: ((error: Error?) -> Unit)? = definedExternally,
    )

    fun on(event: String, callback: (topic: String, message: ByteArray) -> Unit)

    fun on(event: String, callback: (topic: String, message: ByteArray, packet: dynamic) -> Unit)

    fun on(event: String, callback: (dynamic) -> Unit)

    fun off(event: String, callback: (dynamic) -> Unit)

    fun removeListener(event: String, callback: (dynamic) -> Unit)

    fun publishAsync(topic: String, message: String): Promise<dynamic>

    fun publishAsync(topic: String, message: String, options: IClientPublishOptions): Promise<dynamic>

    fun subscribeAsync(topicObject: String): Promise<Array<ISubscriptionGrant>>

    fun subscribeAsync(topicObject: Array<String>): Promise<Array<ISubscriptionGrant>>

    fun subscribeAsync(topic: String, options: IClientSubscribeOptions?): Promise<Array<ISubscriptionGrant>>

    fun unsubscribeAsync(topic: String): Promise<dynamic>

    fun unsubscribeAsync(topic: Array<String>): Promise<dynamic>

    fun unsubscribeAsync(topic: String, options: IClientUnsubscribeProperties): Promise<dynamic>

    fun endAsync(): Promise<Unit>

    fun endAsync(force: Boolean?): Promise<Unit>

    fun endAsync(options: dynamic): Promise<Unit>

    fun endAsync(force: Boolean, options: dynamic): Promise<Unit>
}

external interface Error {
    var message: String
}

external fun connectAsync(brokerUrl: String, options: dynamic = definedExternally): Promise<MqttClient>
