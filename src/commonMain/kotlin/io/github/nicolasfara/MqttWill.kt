package io.github.nicolasfara

/**
 * Represents the MQTT Last Will and Testament.
 *
 * If the client disconnects unexpectedly, the broker will publish this message containing the [message] to the [topic].
 * The [qos] level and the [retained] flag can be set.
 */
data class MqttWill(val topic: String, val message: ByteArray, val qos: MqttQoS, val retained: Boolean = false) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MqttWill

        if (retained != other.retained) return false
        if (topic != other.topic) return false
        if (!message.contentEquals(other.message)) return false
        if (qos != other.qos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = retained.hashCode()
        result = 31 * result + topic.hashCode()
        result = 31 * result + message.contentHashCode()
        result = 31 * result + qos.hashCode()
        return result
    }
}
