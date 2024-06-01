package it.nicolasfarabegoli.mktt.subscribe

import it.nicolasfarabegoli.mktt.message.ExactlyOnce
import it.nicolasfarabegoli.mktt.message.QoS
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter

data class MqttSubscription(
    val topicFilter: MqttTopicFilter = MqttTopicFilter(),
    val qoS: QoS = ExactlyOnce,
    val noLocal: Boolean = false,
    val retainHandling: MqttRetainHandling = Send,
    val retainAsPublished: Boolean = false,
)
