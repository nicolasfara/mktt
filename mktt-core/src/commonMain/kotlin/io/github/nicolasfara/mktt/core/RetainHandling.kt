package io.github.nicolasfara.mktt.core

/**
 * Retain handling is used in the SUBSCRIBE packet.
 */
enum class RetainHandling(
    /** Wire-encoded retain-handling value. */
    val value: Int,
) {
    /** Send retained messages at subscribe time. */
    SEND_ON_SUBSCRIBE(0),

    /** Send retained messages only for new subscriptions. */
    SEND_IF_NOT_EXISTS(1),

    /** Never send retained messages for this subscription. */
    DO_NOT_SEND(2),
    ;

    /** Utilities for converting wire values to [RetainHandling]. */
    companion object {
        /** Converts an integer value to [RetainHandling]. */
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
