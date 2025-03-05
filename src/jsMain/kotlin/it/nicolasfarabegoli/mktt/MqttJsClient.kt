package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.facade.Error
import it.nicolasfarabegoli.mktt.facade.IClientPublishOptions
import it.nicolasfarabegoli.mktt.facade.MqttClient
import it.nicolasfarabegoli.mktt.facade.connectAsync
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

internal class MqttJsClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
) : MkttClient {
    private lateinit var client: MqttClient
    private val messageFlow by lazy {
        callbackFlow {
            client.on("message") { topic, message ->
                val msg =
                    MqttMessage(
                        topic = topic,
                        payload = message,
                        qos = MqttQoS.ExactlyOnce,
                        retained = false,
                    )
                trySend(msg).onFailure { error ->
                    close(error)
                }
            }
            client.on("error") { error: Error ->
                close(Throwable(error.message))
            }
            awaitClose {
            }
        }
    }

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
        client = connectAsync(brokerString).await()
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

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = flow {
        require(::client.isInitialized) { "Client not initialized" }
        client.subscribeAsync(topic).await()
        emitAll(messageFlow.filter { matchesTopicFilter(it.topic, topic) })
    }

    override suspend fun unsubscribe(topic: String) = withContext(dispatcher) {
        client.unsubscribeAsync(topic).await()
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
}
