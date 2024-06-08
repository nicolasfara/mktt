package it.nicolasfarabegoli.mktt.message

sealed interface QoS {
    val code: Int
    companion object {
        fun fromCode(code: Int): QoS {
            return when (code) {
                0 -> AtMostOnce
                1 -> AtLeastOnce
                2 -> ExactlyOnce
                else -> throw IllegalArgumentException("Invalid QoS code: $code")
            }
        }
    }
}

data object AtMostOnce : QoS {
    override val code: Int = 0
}

data object AtLeastOnce : QoS {
    override val code: Int = 1
}

data object ExactlyOnce : QoS {
    override val code: Int = 2
}
