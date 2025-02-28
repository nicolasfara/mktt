package it.nicolasfarabegoli.mktt

/**
 * An MQTT message with its [topic], [payload], [qos] and [retained] flag.
 */
data class MqttMessage(
    val topic: String,
    val payload: ByteArray,
    val qos: MqttQoS,
    val retained: Boolean,
) {
    /**
     * Returns the payload as a string.
     */
    fun payloadAsString(): String = payload.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MqttMessage

        if (retained != other.retained) return false
        if (topic != other.topic) return false
        if (!payload.contentEquals(other.payload)) return false
        if (qos != other.qos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = retained.hashCode()
        result = 31 * result + topic.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + qos.hashCode()
        return result
    }
}
