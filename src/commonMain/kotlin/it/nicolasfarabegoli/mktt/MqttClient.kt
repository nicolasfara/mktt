package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.QoS
import kotlinx.coroutines.flow.Flow

interface MqttClient {
    suspend fun connect()
    suspend fun disconnect()
    fun subscribe(topic: String, qoS: QoS, noLocal: Boolean = true): Flow<MqttMessage>
    suspend fun publish(message: MqttMessage)
}

// expect fun createMqttClient(): MqttClient
