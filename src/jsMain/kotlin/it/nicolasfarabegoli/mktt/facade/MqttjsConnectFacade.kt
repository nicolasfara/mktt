@file:JsModule("mqtt")
@file:JsNonModule
@file:JsQualifier("connect")
package it.nicolasfarabegoli.mktt.facade

import kotlin.js.Promise

// external fun connectAsync(brokerUrl: String): Promise<MqttClient>
external fun connectAsync(brokerUrl: String, opts: IClientOptions?, allowRetries: Boolean): Promise<MqttClient>