package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.subscribe.MqttRetainHandling
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Represents an MQTT client.
 *
 * The [MkttClient] interface provides a way to connect and disconnect from an MQTT broker, subscribe to topics, and
 * publish messages.
 */
interface MkttClient {
    /**
     * The default [CoroutineDispatcher] used by the client.
     */
    val defaultDispatcher: CoroutineDispatcher

    /**
     * Connects to the MQTT broker.
     */
    suspend fun connect(): MqttConnAck

    /**
     * Disconnects from the MQTT broker.
     */
    suspend fun disconnect()

    /**
     * Subscribes to a topic with a specific [filter], [qoS] level, [noLocal] flag, [retainHandling] and
     * [retainAsPublished] flags.
     */
    fun subscribe(
        filter: MqttTopicFilter,
        qoS: MqttQoS = MqttQoS.ExactlyOnce,
        noLocal: Boolean = true,
        retainHandling: MqttRetainHandling = MqttRetainHandling.SEND,
        retainAsPublished: Boolean = false,
    ): Flow<MqttPublish> = subscribe(MqttSubscription(filter, qoS, noLocal, retainHandling, retainAsPublished))

    /**
     * Subscribes to a topic with a specific [subscription].
     */
    fun subscribe(subscription: MqttSubscription): Flow<MqttPublish>

    /**
     * Publishes [messages] to the MQTT broker.
     */
    suspend fun publish(messages: Flow<MqttPublish>)

    /**
     * Publishes a single [message] to the MQTT broker.
     */
    suspend fun publish(message: MqttPublish) = publish(flowOf(message))

    /**
     * Publishes a single message to the MQTT broker.
     */
    suspend fun publish(
        message: ByteArray,
        topic: MqttTopic,
        qoS: MqttQoS = MqttQoS.ExactlyOnce,
        retain: Boolean = false,
    ): Unit = publish(MqttPublish(topic = topic, payload = message, qos = qoS, isRetain = retain))
}

/**
 * Creates an MQTT client with the given [configuration] and [dispatcher].
 */
expect fun MqttClient(
    configuration: MqttConfiguration,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): MkttClient
