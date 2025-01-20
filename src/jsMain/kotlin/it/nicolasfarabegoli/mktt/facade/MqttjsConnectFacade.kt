@file:JsModule("mqtt")
@file:JsNonModule

package it.nicolasfarabegoli.mktt.facade

import kotlin.js.Promise

external fun connect(opts: IClientOptions?): MqttClient
external fun connect(host: String, opts: IClientOptions?): MqttClient
external fun connectAsync(brokerUrl: String): Promise<MqttClient>
external fun connectAsync(opts: IClientOptions): Promise<MqttClient>
