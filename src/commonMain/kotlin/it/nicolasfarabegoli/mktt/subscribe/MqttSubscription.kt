package it.nicolasfarabegoli.mktt.subscribe

import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter

data class MqttSubscription(
    val topicFilter: MqttTopicFilter,
    val qoS: MqttQoS = MqttQoS.ExactlyOnce,
    val noLocal: Boolean = false,
    val retainHandling: MqttRetainHandling = Send,
    val retainAsPublished: Boolean = false,
)
