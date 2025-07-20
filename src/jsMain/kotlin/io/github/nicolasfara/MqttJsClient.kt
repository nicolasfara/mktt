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

    override val connectionState: Flow<MqttConnectionState> =
        callbackFlow {
            val connectCallback: (dynamic) -> Unit = { _: dynamic -> trySend(MqttConnectionState.Connected) }
            val disconnectCallback: (dynamic) -> Unit = { _: dynamic -> trySend(MqttConnectionState.Disconnected) }
            val errorCallback: (Error) -> Unit =
                { error: Error -> trySend(MqttConnectionState.ConnectionError(Throwable(error.message))) }

            client.on("connect", connectCallback)
            client.on("disconnect", disconnectCallback)
            client.on("error", errorCallback)

            awaitClose {
                client.off("connect", connectCallback)
                client.off("disconnect", disconnectCallback)
                client.off("error", errorCallback)
            }
        }

    override suspend fun connect(): Unit = withContext(dispatcher) {
        val brokerString = "mqtt://${configuration.brokerUrl}:${configuration.port}"
        client = connectAsync(brokerString, configuration.toMqttjs()).await()
        messageFlow = callbackFlow {
            client.on("message") { topic, message, packet -> onMessageCallback(topic, message, packet, this) }
            awaitClose()
        }
    }

    override suspend fun disconnect() = withContext(dispatcher) {
        if (::client.isInitialized) {
            client.endAsync().await()
            client.off("message") { }
        } else {
            error("Client not initialized")
        }
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
        // Convert the topic filter into a regex pattern
        val regexPattern =
            filter
                .replace("+", "[^/]+") // `+` matches a single level (anything except `/`)
                .replace("#", ".*") // `#` matches everything beyond this point
                .let { "^$it$" } // Ensure the pattern matches the whole topic

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
