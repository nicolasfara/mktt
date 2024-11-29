package it.nicolasfarabegoli.mktt.message

/**
 * Represents the Quality of Service (QoS) level of an MQTT message.
 */
sealed interface MqttQoS {
    /**
     * The code of the QoS level.
     */
    val code: Int

    /**
     * The at most once QoS level.
     */
    companion object {
        /**
         * Returns the [MqttQoS] from the given [code].
         */
        fun fromCode(code: Int): MqttQoS {
            return when (code) {
                0 -> AtMostOnce
                1 -> AtLeastOnce
                2 -> ExactlyOnce
                else -> throw IllegalArgumentException("Invalid QoS code: $code")
            }
        }
    }

    /**
     * The at most once QoS level.
     */
    data object AtMostOnce : MqttQoS {
        override val code: Int = 0
    }

    /**
     * The at least once QoS level.
     */
    data object AtLeastOnce : MqttQoS {
        override val code: Int = 1
    }

    /**
     * The exactly once QoS level.
     */
    data object ExactlyOnce : MqttQoS {
        override val code: Int = 2
    }
}
