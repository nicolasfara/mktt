package it.nicolasfarabegoli.mktt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Represents an MQTT client.
 *
 * The [MkttClient] interface provides a way to connect and disconnect from an MQTT broker, subscribe to topics, and
 * publish messages.
 */
interface MkttClient {
    /**
     * The [CoroutineDispatcher] used by the client to run the operations.
     */
    val dispatcher: CoroutineDispatcher

    /**
     * Observes the current connection state.
     * The [Flow] will emit the current connection state of the client.
     */
    val connectionState: Flow<MqttConnectionState>

    /**
     * Connects to the MQTT broker with the configuration built with [MqttClient] smart constructor.
     *
     * Throw an [Exception] if the connection fails.
     */
    suspend fun connect()

    /**
     * Disconnects from the MQTT broker.
     */
    suspend fun disconnect()

    /**
     * Publishes the given [message] to the given [topic] using the given [qos] level.
     */
    suspend fun publish(
        topic: String,
        message: ByteArray,
        qos: MqttQoS = MqttQoS.AtMostOnce,
    )

    /**
     * Subscribes to a topic with a specific [topic] and [qos] level.
     *
     * Returns a [Flow] that emits the received messages based on the subscription.
     */
    fun subscribe(
        topic: String,
        qos: MqttQoS = MqttQoS.AtMostOnce,
    ): Flow<MqttMessage>

    /**
     * Unsubscribes from a topic.
     */
    suspend fun unsubscribe(topic: String)
}

/**
 * Creates an MQTT client by optionally specifying a [dispatcher] to run the async operations.
 */
@Suppress("FunctionNaming")
fun MqttClient(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    clientConfig: MqttClientConfigurationScope.() -> Unit,
): MkttClient {
    val configuration = MqttClientConfigurationScope().apply(clientConfig).build()
    return createMqttClient(dispatcher, configuration)
}

internal expect fun createMqttClient(dispatcher: CoroutineDispatcher, clientConfig: MqttClientConfiguration): MkttClient
