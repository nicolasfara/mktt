package io.github.nicolasfara

/**
 * Platform-specific wrapper around the native MQTT.js client.
 *
 * Each platform provides an [actual] implementation that bridges to its
 * respective facade and handles type conversion (e.g. Buffer → ByteArray for
 * the WASM target).
 */
internal expect class MqttClientWrapper() {
    /** `true` once [connect] has completed successfully. */
    val isConnected: Boolean

    /** Opens the connection to the broker using the given [config]. */
    suspend fun connect(url: String, config: MqttClientConfiguration)

    /**
     * Registers a [callback] that is invoked for every incoming PUBLISH
     * message.  Parameters: topic, payload, QoS code, retained flag.
     */
    fun setMessageCallback(callback: (topic: String, message: ByteArray, qos: Int, retain: Boolean) -> Unit)

    /** Removes the previously registered message callback. */
    fun clearMessageCallback()

    /** Subscribes to [topic] on the broker. */
    suspend fun subscribe(topic: String)

    /** Unsubscribes from [topic] on the broker. */
    suspend fun unsubscribe(topic: String)

    /**
     * Publishes [message] (already encoded as a UTF-8 string) to [topic]
     * with the given [qos] code.
     */
    suspend fun publish(topic: String, message: String, qos: Int)

    /** Closes the connection to the broker. */
    suspend fun end()
}
