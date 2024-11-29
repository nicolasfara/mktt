package it.nicolasfarabegoli.mktt

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

/**
 * An implementation of [MqttClient] using HiveMQ MQTT Client.
 *
 * Takes a [configuration] and a [defaultDispatcher] as parameters.
 */
class HivemqMqttClient(
    configuration: MqttConfiguration,
    override val defaultDispatcher: CoroutineDispatcher,
) : MqttClient {
    private val hiveMqClient by lazy {
        Mqtt5Client.builder()
            .serverHost(configuration.hostname)
            .serverPort(configuration.port)
            .identifier(configuration.clientId)
            .buildRx()
    }

    override suspend fun connect(): MqttConnAck = withContext(defaultDispatcher) {
        val reasonCode = hiveMqClient.connect()
            .doOnSuccess { println("Connected to the broker") }
            .await()
        fromHivemqMqttConnAck(reasonCode)
    }

    override suspend fun disconnect(): Unit = withContext(defaultDispatcher) {
        hiveMqClient.disconnect().await()
    }

    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> = hiveMqClient
        .subscribePublishes(subscription.toHivemqMqtt())
        .doOnNext { println("Received message: $it") }
        .asFlow()
        .map { it.toMqtt() }
        .flowOn(defaultDispatcher)

    override fun publish(messages: Flow<MqttPublish>): Flow<MqttPublishResult> {
        val mappedMessages = messages.map { it.toHivemqMqtt() }
        return hiveMqClient.publish(mappedMessages.asFlowable())
            .doOnNext { println("Published message: $it") }
            .asFlow()
            .map { it.toMqtt() }
            .flowOn(defaultDispatcher)
    }
}
