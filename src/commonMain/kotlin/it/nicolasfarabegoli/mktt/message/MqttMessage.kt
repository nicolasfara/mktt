package it.nicolasfarabegoli.mktt.message

interface MqttMessage {
    val type: MqttMessageType
}