package it.nicolasfarabegoli.mktt

/**
 * Represents the Quality of Service (QoS) level of an MQTT message with its [code].
 */
enum class MqttQoS(
    val code: Int,
) {
    /**
     * Delivers the message at most once.
     */
    AtMostOnce(0),

    /**
     * Delivers the message at least once.
     */
    AtLeastOnce(1),

    /**
     * Delivers the message exactly once.
     */
    ExactlyOnce(2),
    ;

    /**
     * Companion object for [MqttQoS].
     */
    companion object {
        /**
         * Returns the [MqttQoS] from the given [code].
         *
         * Trows an [IllegalArgumentException] if the [code] is invalid.
         */
        fun from(code: Int): MqttQoS = entries.first { it.code == code }
    }
}
