@file:JsModule("mqtt")
@file:JsNonModule
package it.nicolasfarabegoli.mktt.facade

import kotlin.js.Promise

external class MqttClient {
    fun endAsync(): Promise<dynamic>
    fun on(event: dynamic, callback: (dynamic) -> dynamic)
}