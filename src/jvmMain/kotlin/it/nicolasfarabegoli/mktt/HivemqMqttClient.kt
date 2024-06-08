package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.adapter.HivemqAdapter.fromHivemqMqttConnAck
import it.nicolasfarabegoli.mktt.adapter.HivemqAdapter.toHivemqMqtt
import it.nicolasfarabegoli.mktt.adapter.publish.HivemqPublishAdapter.toHivemqMqtt
import it.nicolasfarabegoli.mktt.adapter.publish.HivemqPublishAdapter.toMqtt
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.message.publish.MqttPublishResult
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import com.hivemq.client.mqtt.MqttClient as HiveMqClient

class HivemqMqttClient(configuration: MqttConfiguration, override val defaultDispatcher: CoroutineDispatcher) : MqttClient {
    private val hiveMqClient by lazy {
        HiveMqClient.builder()
            .serverHost(configuration.hostname)
            .serverPort(configuration.port)
            .identifier(configuration.clientId)
            .useMqttVersion5()
            .build()
            .toRx()
    }

    override suspend fun connect(): MqttConnAck = withContext(defaultDispatcher) {
        val reasonCode = hiveMqClient.connect().await()
        fromHivemqMqttConnAck(reasonCode)
    }

    override suspend fun disconnect(): Unit = withContext(defaultDispatcher) {
        hiveMqClient.disconnect().await()
    }

    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> = hiveMqClient
        .subscribePublishes(subscription.toHivemqMqtt())
        .asFlow()
        .map { it.toMqtt() }

    override fun publish(messages: Flow<MqttPublish>): Flow<MqttPublishResult> {
        val mappedMessages = messages.map { it.toHivemqMqtt() }
        return hiveMqClient.publish(mappedMessages.asFlowable()).asFlow().map { it.toMqtt() }
    }
}
