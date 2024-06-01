package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.ExactlyOnce
import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.QoS
import it.nicolasfarabegoli.mktt.subscribe.MqttRetainHandling
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import it.nicolasfarabegoli.mktt.subscribe.Send
import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

interface MqttClient {
    val defaultDispatcher: CoroutineDispatcher
    suspend fun connect()
    suspend fun disconnect()
    fun <Message> subscribe(
        filter: MqttTopicFilter = MqttTopicFilter(),
        qoS: QoS = ExactlyOnce,
        noLocal: Boolean = true,
        retainHandling: MqttRetainHandling = Send,
        retainAsPublished: Boolean = false
    ): Flow<MqttMessage<Message>>
    fun <Message> subscribe(subscription: MqttSubscription): Flow<MqttMessage<Message>>
    suspend fun <Message> publish(message: MqttMessage<Message>)
    suspend fun <Message> publish(
        message: Message,
        topic: MqttTopic,
        qoS: QoS = ExactlyOnce,
        retain: Boolean = false,
    )
}

expect fun MqttClient(
    configuration: MqttConfiguration,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): MqttClient
