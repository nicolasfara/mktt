@file:JsModule("mqtt")
@file:JsNonModule

package it.nicolasfarabegoli.mktt.facade

import kotlin.js.Promise

external fun connect(brokerUrl: String): MqttClient
external fun connectAsync(brokerUrl: String): Promise<MqttClient>
external fun connectAsync(brokerUrl: String, opts: IClientOptions?, allowRetries: Boolean): Promise<MqttClient>
