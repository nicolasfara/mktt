package it.nicolasfarabegoli.mktt.message

/**
 * Represents a generic MQTT message with a specific [type].
 */
interface MqttMessage {
    /**
     * The type of the message.
     */
    val type: MqttMessageType
}
