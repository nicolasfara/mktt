package it.nicolasfarabegoli.mktt.message

import it.nicolasfarabegoli.mktt.topic.MqttTopic

data class MqttMessage<MessageType>(
    val message: MessageType,
    val topic: MqttTopic,
    val qos: QoS = ExactlyOnce,
    val retain: Boolean = false,
)
