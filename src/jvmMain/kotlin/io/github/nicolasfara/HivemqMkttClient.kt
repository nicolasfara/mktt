package io.github.nicolasfara

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.Mqtt5MessageType
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe
import io.github.nicolasfara.adapter.MqttWillAdapter.toHivemq
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

/**
 * An implementation of [MkttClient] using HiveMQ MQTT Client.
 *
 * Takes a [configuration] and a [dispatcher] as parameters.
 */
internal class HivemqMkttClient(override val dispatcher: CoroutineDispatcher, configuration: MqttClientConfiguration) :
    MkttClient {
    private val client by lazy {
        @Suppress("IgnoredReturnValue")
        Mqtt5Client
            .builder()
            .apply {
                serverHost(configuration.brokerUrl)
                serverPort(configuration.port)
                identifier(configuration.clientId)
                willPublish(configuration.will?.toHivemq())
                if (configuration.automaticReconnect) {
                    automaticReconnectWithDefaultConfig()
                }
            }.buildRx()
    }
    private val messageFlows = mutableMapOf<String, Flow<MqttMessage>>()

    override val connectionState: Flow<MqttConnectionState>
        get() = TODO("Not yet implemented")

    override suspend fun connect(): Unit = withContext(dispatcher) {
        client.connect().await()
    }

    override suspend fun disconnect(): Unit = withContext(dispatcher) {
        client.disconnect().await()
    }

    override suspend fun publish(topic: String, message: ByteArray, qos: MqttQoS): Unit = withContext(dispatcher) {
        val publishMessage =
            Mqtt5Publish
                .builder()
                .topic(topic)
                .qos(MqttQos.fromCode(qos.code) ?: error("Invalid QoS"))
                .retain(true)
                .payload(message)
                .build()
        client.publish(flowOf(publishMessage).asFlowable()).awaitFirst()
    }

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = messageFlows.getOrPut(topic) {
        flow {
            val subscription =
                Mqtt5Subscribe
                    .builder()
                    .topicFilter(topic)
                    .qos(MqttQos.fromCode(qos.code) ?: error("Invalid QoS"))
                    .build()
            val result = client.subscribePublishes(subscription)
            val subAckResult = result.subscribeSingleFuture()
                .await()
            require(subAckResult.type == Mqtt5MessageType.SUBACK) {
                "Subscription failed: $subAckResult"
            }
            emitAll(
                result
                    .asFlow()
                    .map {
                        MqttMessage(
                            topic = it.topic.toString(),
                            payload = it.payloadAsBytes,
                            qos = MqttQoS.from(it.qos.code),
                            retained = it.isRetain,
                        )
                    },
            )
        }.flowOn(dispatcher)
    }

    override suspend fun unsubscribe(topic: String): Unit = withContext(dispatcher) {
        val unsubscribe =
            Mqtt5Unsubscribe
                .builder()
                .topicFilter(topic)
                .build()
        val result = client.unsubscribe(unsubscribe).await()
        require(result.type == Mqtt5MessageType.UNSUBACK) { "Unsubscription failed: $result" }
        messageFlows.remove(topic) ?: error("Topic not subscribed")
    }
}
