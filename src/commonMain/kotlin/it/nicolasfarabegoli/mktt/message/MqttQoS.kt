package it.nicolasfarabegoli.mktt.message

/**
 * Represents the Quality of Service (QoS) level of an MQTT message with its [code].
 */
enum class MqttQoS(
    val code: Int,
) {
    /**
     * TODO.
     */
    AtMostOnce(0),

    /**
     * TODO.
     */
    AtLeastOnce(1),

    /**
     * TODO.
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
        fun from(code: Int): MqttQoS = MqttQoS.entries.first { it.code == code }
    }
}
