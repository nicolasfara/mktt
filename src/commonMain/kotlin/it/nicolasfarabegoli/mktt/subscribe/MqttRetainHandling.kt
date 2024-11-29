package it.nicolasfarabegoli.mktt.subscribe

/**
 * Represents the handling of retained messages.
 */
sealed interface MqttRetainHandling {
    /**
     * The byte code of the [MqttRetainHandling].
     */
    val code: Byte
}

/**
 * The retained message is sent to the new subscriber.
 */
data object Send : MqttRetainHandling {
    /**
     * The byte code of the [Send].
     */
    override val code: Byte = 0x00
}

/**
 * The retained message is sent to the new subscriber only if the subscription does not exist.
 */
data object SendIfSubscriptionDoesNotExist : MqttRetainHandling {
    /**
     * The byte code of the [SendIfSubscriptionDoesNotExist].
     */
    override val code: Byte = 0x01
}

/**
 * The retained message is not sent to the new subscriber.
 */
data object DoNotSend : MqttRetainHandling {
    /**
     * The byte code of the [DoNotSend].
     */
    override val code: Byte = 0x02
}
