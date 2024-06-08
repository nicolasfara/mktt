package it.nicolasfarabegoli.mktt.subscribe

import it.nicolasfarabegoli.mktt.message.ExactlyOnce
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter

data class MqttSubscription(
    val topicFilter: MqttTopicFilter,
    val qoS: MqttQoS = ExactlyOnce,
    val noLocal: Boolean = false,
    val retainHandling: MqttRetainHandling = Send,
    val retainAsPublished: Boolean = false,
)
