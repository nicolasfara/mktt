package it.nicolasfarabegoli.mktt.message

sealed interface MqttQoS {
    val code: Int
    companion object {
        fun fromCode(code: Int): MqttQoS {
            return when (code) {
                0 -> AtMostOnce
                1 -> AtLeastOnce
                2 -> ExactlyOnce
                else -> throw IllegalArgumentException("Invalid QoS code: $code")
            }
        }
    }
}

data object AtMostOnce : MqttQoS {
    override val code: Int = 0
}

data object AtLeastOnce : MqttQoS {
    override val code: Int = 1
}

data object ExactlyOnce : MqttQoS {
    override val code: Int = 2
}
