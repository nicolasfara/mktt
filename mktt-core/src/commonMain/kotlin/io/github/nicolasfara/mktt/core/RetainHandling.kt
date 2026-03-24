package io.github.nicolasfara.mktt.core

/**
 * Retain handling is used in the SUBSCRIBE packet.
 */
public enum class RetainHandling(public val value: Int) {

    SEND_ON_SUBSCRIBE(0),
    SEND_IF_NOT_EXISTS(1),
    DO_NOT_SEND(2),
    ;

    public companion object {
        public fun from(value: Int): io.github.nicolasfara.mktt.core.RetainHandling = when (value) {
            0 -> SEND_ON_SUBSCRIBE

            1 -> SEND_IF_NOT_EXISTS

            2 -> DO_NOT_SEND

            else -> throw _root_ide_package_.io.github.nicolasfara.mktt.core.MalformedPacketException(
                "Unknown RetainHandling value: $value",
            )
        }
    }
}
