package it.nicolasfarabegoli.mktt.subscribe

/**
 * Retain handling options and its respective [code].
 */
enum class MqttRetainHandling(val code: Byte) {
    /**
     * TODO.
     */
    SEND(0x00),

    /**
     * TODO.
     */
    SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST(0x01),

    /**
     * TODO.
     */
    DO_NOT_SEND(0x02),
}
