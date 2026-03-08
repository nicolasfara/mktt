package io.github.nicolasfara

import kotlinx.coroutines.CoroutineDispatcher
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
    private val wrapper = MqttClientWrapper()
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
            wrapper.connect(brokerString, configuration)
            _connectionState.value = MqttConnectionState.Connected
            messageFlow = callbackFlow {
                wrapper.setMessageCallback { topic, message, qos, retain ->
                    val msg = MqttMessage(topic, message, MqttQoS.from(qos), retain)
                    trySend(msg).onFailure { error -> close(error) }
                }
                awaitClose { wrapper.clearMessageCallback() }
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
        wrapper.end()
        _connectionState.value = MqttConnectionState.Disconnected
    }

    override suspend fun publish(topic: String, message: ByteArray, qos: MqttQoS) = withContext(dispatcher) {
        wrapper.publish(topic, message.decodeToString(), qos.code)
    }

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = subscribedTopics.getOrPut(topic) {
        flow {
            require(wrapper.isConnected) { "Client not initialized" }
            wrapper.subscribe(topic)
            emitAll(messageFlow.filter { matchesTopicFilter(it.topic, topic) })
        }.cancellable().flowOn(dispatcher)
    }

    override suspend fun unsubscribe(topic: String): Unit = withContext(dispatcher) {
        wrapper.unsubscribe(topic)
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
}
