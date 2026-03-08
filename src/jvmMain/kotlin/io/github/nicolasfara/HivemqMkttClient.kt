package io.github.nicolasfara

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException
import com.hivemq.client.mqtt.mqtt5.message.Mqtt5MessageType
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe
import io.github.nicolasfara.adapter.MqttWillAdapter.toHivemq
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    private val client by lazy {
        val builder = Mqtt5Client.builder()
            .serverHost(configuration.brokerUrl)
            .serverPort(configuration.port)
            .identifier(configuration.clientId)
            .addConnectedListener { _connectionState.value = MqttConnectionState.Connected }
            .addDisconnectedListener { ctx ->
                _connectionState.value = if (ctx.reconnector.isReconnect) {
                    MqttConnectionState.Connecting
                } else {
                    MqttConnectionState.Disconnected
                }
            }
        val builderWithWill = configuration.will?.let { builder.willPublish(it.toHivemq()) } ?: builder
        val builderWithAuth = if (configuration.username != null) {
            val authBuilder = builderWithWill.simpleAuth().username(configuration.username)
            val authBuilderWithPassword = if (configuration.password != null) {
                authBuilder.password(configuration.password.encodeToByteArray())
            } else {
                authBuilder
            }
            authBuilderWithPassword.applySimpleAuth()
        } else {
            builderWithWill
        }
        val builderWithReconnect = if (configuration.automaticReconnect) {
            builderWithAuth.automaticReconnectWithDefaultConfig()
        } else {
            builderWithAuth
        }
        val builderWithSsl = if (configuration.ssl) {
            builderWithReconnect.sslWithDefaultConfig()
        } else {
            builderWithReconnect
        }
        builderWithSsl.buildRx()
    }
    private val messageFlows = mutableMapOf<String, Flow<MqttMessage>>()

    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Disconnected) {
            "Client is already connected or connecting"
        }
        _connectionState.value = MqttConnectionState.Connecting
        try {
            client.connect().await()
        } catch (e: ConnectionFailedException) {
            _connectionState.value = MqttConnectionState.ConnectionError(e)
            throw e
        } catch (e: Mqtt5ConnAckException) {
            _connectionState.value = MqttConnectionState.ConnectionError(e)
            throw e
        }
    }

    override suspend fun disconnect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Connected) {
            "Client is not connected"
        }
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
