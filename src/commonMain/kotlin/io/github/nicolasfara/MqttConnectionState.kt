package io.github.nicolasfara

/**
 * Represents the current connection state of an MQTT client.
 */
sealed interface MqttConnectionState {
    /**
     * The client is currently connecting or reconnecting to the MQTT broker.
     * This state is entered both during the initial connection attempt and
     * during automatic reconnection after a disconnection.
     */
    object Connecting : MqttConnectionState

    /**
     * The client is currently connected to the MQTT broker.
     */
    object Connected : MqttConnectionState

    /**
     * The client is currently disconnected from the MQTT broker.
     */
    object Disconnected : MqttConnectionState

    /**
     * The client encountered an [error] while is connected or is connecting to the MQTT broker.
     */
    data class ConnectionError(val error: Throwable) : MqttConnectionState
}
