package it.nicolasfarabegoli.mktt

import com.hivemq.client.mqtt.MqttClient as HiveMqClient
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.QoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.subscribe.MqttRetainHandling
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

class HivemqMqttClient(
    configuration: MqttConfiguration,
    override val defaultDispatcher: CoroutineDispatcher
) : MqttClient {
    private val hiveMqClient by lazy {
        HiveMqClient.builder()
            .serverHost(configuration.hostname)
            .serverPort(configuration.port)
            .identifier(configuration.clientId)
            .useMqttVersion5()
            .build()
            .toRx()
    }

    override suspend fun connect(): MqttConnAckReasonCode = withContext(defaultDispatcher) {
        val reasonCode = hiveMqClient.connect().await().reasonCode
        MqttConnAckReasonCode.fromCode(reasonCode.code.toByte())
    }

    override suspend fun disconnect(): Unit = withContext(defaultDispatcher) {
        hiveMqClient.disconnect().await()
    }

    override fun <Message> subscribe(
        filter: MqttTopicFilter,
        qoS: QoS,
        noLocal: Boolean,
        retainHandling: MqttRetainHandling,
        retainAsPublished: Boolean
    ): Flow<MqttMessage<Message>> {
        TODO("Not yet implemented")
    }

    override fun <Message> subscribe(subscription: MqttSubscription): Flow<MqttMessage<Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun <Message> publish(message: MqttMessage<Message>) {
        TODO("Not yet implemented")
    }

    override suspend fun <Message> publish(
        message: Message,
        topic: MqttTopic,
        qoS: QoS,
        retain: Boolean,
    ) {
        TODO("Not yet implemented")
    }
}