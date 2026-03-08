package io.github.nicolasfara

import io.github.nicolasfara.adapter.MkttJsAdapter.toMqttjs
import io.github.nicolasfara.facade.Error
import io.github.nicolasfara.facade.IClientPublishOptions
import io.github.nicolasfara.facade.MqttClient
import io.github.nicolasfara.facade.connectAsync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class MqttJsClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
) : MkttClient {
    private lateinit var client: MqttClient
    private lateinit var messageFlow: Flow<MqttMessage>
    private val subscribedTopics = mutableMapOf<String, Flow<MqttMessage>>()
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)

    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Disconnected) {
            "Client is already connected or connecting"
        }
        _connectionState.value = MqttConnectionState.Connecting
        val scheme = if (configuration.ssl) "mqtts" else "mqtt"
        val brokerString = "$scheme://${configuration.brokerUrl}:${configuration.port}"
        try {
            client = connectAsync(brokerString, configuration.toMqttjs()).await()
            _connectionState.value = MqttConnectionState.Connected
            messageFlow = callbackFlow {
                client.on("message") { topic, message, packet -> onMessageCallback(topic, message, packet, this) }
                awaitClose()
            }
        } finally {
            if (_connectionState.value is MqttConnectionState.Connecting) {
                _connectionState.value = MqttConnectionState.Disconnected
            }
        }
    }

    override suspend fun disconnect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Connected) {
            "Client is not connected"
        }
        client.endAsync().await()
        client.off("message") { }
        _connectionState.value = MqttConnectionState.Disconnected
    }

    override suspend fun publish(topic: String, message: ByteArray, qos: MqttQoS) = withContext(dispatcher) {
        val publishOption =
            object : IClientPublishOptions {
                override var qos: Number? = qos.code
                override var retain: Boolean? = true
                override var dup: Boolean? = null
                override var properties: Any? = null
                override var cbStorePut: ((Error?) -> Unit)? = null
            }
        client.publishAsync(topic, message.decodeToString(), publishOption).await()
    }

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = subscribedTopics.getOrPut(topic) {
        flow {
            require(::client.isInitialized) { "Client not initialized" }
            client.subscribeAsync(topic).await()
            emitAll(messageFlow.filter { matchesTopicFilter(it.topic, topic) })
        }.cancellable().flowOn(dispatcher)
    }

    override suspend fun unsubscribe(topic: String): Unit = withContext(dispatcher) {
        client.unsubscribeAsync(topic).await()
        subscribedTopics.remove(topic)
    }

    private fun matchesTopicFilter(topic: String, filter: String): Boolean {
        val regexPattern =
            filter
                .replace("+", "[^/]+")
                .replace("#", ".*")
                .let { "^$it$" }
        return Regex(regexPattern).matches(topic)
    }

    private fun onMessageCallback(
        topic: String,
        message: ByteArray,
        packet: dynamic,
        flowScope: ProducerScope<MqttMessage>,
    ) = with(flowScope) {
        if (packet.cmd == "publish") {
            val msg = MqttMessage(topic, message, MqttQoS.from(packet.qos), packet.retain)
            trySend(msg).onFailure { error -> close(error) }
        }
    }
}
