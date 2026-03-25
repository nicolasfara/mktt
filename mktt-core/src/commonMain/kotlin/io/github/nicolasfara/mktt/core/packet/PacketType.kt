@file:OptIn(ExperimentalStdlibApi::class)

package io.github.nicolasfara.mktt.core.packet

enum class PacketType(internal val value: Int) {
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14),
    AUTH(15),
    ;

    companion object {
        private val HeaderFormat = HexFormat { number.prefix = "0x" }

        /**
         * Converts the upper 4 bits of the specified MQTT header field into an instance of this.
         */
        fun from(header: Byte): PacketType {
            val value = (header.toInt() and 0xFF) shr 4
            return entries.firstOrNull { it.value == value }
                ?: throw _root_ide_package_.io.github.nicolasfara.mktt.core.MalformedPacketException(
                    "Unknown header type: ${
                        header.toHexString(
                            HeaderFormat,
                        )
                    }",
                )
        }
    }
}
