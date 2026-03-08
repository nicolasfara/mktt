package io.github.nicolasfara

import io.github.nicolasfara.adapter.MkttJsAdapter.toMqttjs
import io.github.nicolasfara.facade.Error
import io.github.nicolasfara.facade.IClientPublishOptions
import io.github.nicolasfara.facade.MqttClient
import io.github.nicolasfara.facade.connectAsync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MqttJsClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
) : MkttClient {
    private lateinit var client: MqttClient
    private val subscribedTopics = mutableMapOf<String, Flow<MqttMessage>>()
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    private val messageSharedFlow = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 64)

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
            // Register message callback eagerly so no messages are missed due to the
            // brief window between SUBACK reception and the subscriber's flow collection.
            client.on("message") { topic: String, message: ByteArray, packet: dynamic ->
                if (packet.cmd == "publish") {
                    messageSharedFlow.tryEmit(
                        MqttMessage(topic, message, MqttQoS.from(packet.qos), packet.retain),
                    )
                }
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
        channelFlow {
            require(::client.isInitialized) { "Client not initialized" }
            // Start collecting incoming messages BEFORE subscribing to avoid missing
            // messages that arrive in the brief window between the broker recording the
            // subscription and our subscriber's flow collection starting.
            val job = launch {
                messageSharedFlow.filter { matchesTopicFilter(it.topic, topic) }.collect { send(it) }
            }
            client.subscribeAsync(topic).await()
            awaitClose { job.cancel() }
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
}
