package it.nicolasfarabegoli.mktt.subscribe

import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter

/**
 * Represents a subscription to a topic.
 *
 * Leveraging the [MqttSubscription] class, you can subscribe to a topic with a [topicFilter] with a specific [qos]
 * level, [noLocal] flag, [retainHandling] and [retainAsPublished] flags.
 */
data class MqttSubscription(
    val topicFilter: MqttTopicFilter,
    val qos: MqttQoS = MqttQoS.ExactlyOnce,
    val noLocal: Boolean = false,
    val retainHandling: MqttRetainHandling = MqttRetainHandling.SEND,
    val retainAsPublished: Boolean = false,
)
