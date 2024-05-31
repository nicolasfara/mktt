package it.nicolasfarabegoli.mktt.message

data class MqttMessage(val topic: String, val payload: ByteArray, val qos: QoS, val retain: Boolean = false) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MqttMessage

        if (topic != other.topic) return false
        if (!payload.contentEquals(other.payload)) return false
        if (qos != other.qos) return false
        if (retain != other.retain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + qos.hashCode()
        result = 31 * result + retain.hashCode()
        return result
    }
}