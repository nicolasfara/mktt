package it.nicolasfarabegoli.mktt.facade

import kotlin.js.Promise

external class MqttClient {
    fun endAsync(): Promise<dynamic>
}