package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.message.publish.MqttPublishResult
import it.nicolasfarabegoli.mktt.subscribe.MqttRetainHandling
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import it.nicolasfarabegoli.mktt.subscribe.Send
import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single

interface MqttClient {
    val defaultDispatcher: CoroutineDispatcher
    suspend fun connect(): MqttConnAck
    suspend fun disconnect()
    fun subscribe(
        filter: MqttTopicFilter,
        qoS: MqttQoS = MqttQoS.ExactlyOnce,
        noLocal: Boolean = true,
        retainHandling: MqttRetainHandling = Send,
        retainAsPublished: Boolean = false,
    ): Flow<MqttPublish> = subscribe(MqttSubscription(filter, qoS, noLocal, retainHandling, retainAsPublished))
    fun subscribe(subscription: MqttSubscription): Flow<MqttPublish>
    fun publish(messages: Flow<MqttPublish>): Flow<MqttPublishResult>
    suspend fun publish(message: MqttPublish): MqttPublishResult = publish(flowOf(message)).single()
    suspend fun publish(
        message: ByteArray,
        topic: MqttTopic,
        qoS: MqttQoS = MqttQoS.ExactlyOnce,
        retain: Boolean = false,
    ): MqttPublishResult = publish(MqttPublish(topic = topic, payload = message, qos = qoS, isRetain = retain))
}

expect fun MqttClient(
    configuration: MqttConfiguration,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): MqttClient
