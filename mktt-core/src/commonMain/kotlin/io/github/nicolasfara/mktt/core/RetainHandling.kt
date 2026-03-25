package io.github.nicolasfara.mktt.core

/**
 * Retain handling is used in the SUBSCRIBE packet.
 */
enum class RetainHandling(val value: Int) {
    SEND_ON_SUBSCRIBE(0),
    SEND_IF_NOT_EXISTS(1),
    DO_NOT_SEND(2),
    ;

    companion object {
        fun from(value: Int): RetainHandling = when (value) {
            0 -> SEND_ON_SUBSCRIBE

            1 -> SEND_IF_NOT_EXISTS

            2 -> DO_NOT_SEND

            else -> throw MalformedPacketException(
                "Unknown RetainHandling value: $value",
            )
        }
    }
}
