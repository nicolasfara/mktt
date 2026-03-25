@file:OptIn(ExperimentalStdlibApi::class)

package io.github.nicolasfara.mktt.core.packet

private const val PACKET_TYPE_CONNECT = 1
private const val PACKET_TYPE_CONNACK = 2
private const val PACKET_TYPE_PUBLISH = 3
private const val PACKET_TYPE_PUBACK = 4
private const val PACKET_TYPE_PUBREC = 5
private const val PACKET_TYPE_PUBREL = 6
private const val PACKET_TYPE_PUBCOMP = 7
private const val PACKET_TYPE_SUBSCRIBE = 8
private const val PACKET_TYPE_SUBACK = 9
private const val PACKET_TYPE_UNSUBSCRIBE = 10
private const val PACKET_TYPE_UNSUBACK = 11
private const val PACKET_TYPE_PINGREQ = 12
private const val PACKET_TYPE_PINGRESP = 13
private const val PACKET_TYPE_DISCONNECT = 14
private const val PACKET_TYPE_AUTH = 15

private const val MQTT_HEADER_BYTE_MASK = 0xFF
private const val MQTT_PACKET_TYPE_SHIFT = 4

/** MQTT control packet type. */
enum class PacketType(internal val value: Int) {
    /** CONNECT packet. */
    CONNECT(PACKET_TYPE_CONNECT),

    /** CONNACK packet. */
    CONNACK(PACKET_TYPE_CONNACK),

    /** PUBLISH packet. */
    PUBLISH(PACKET_TYPE_PUBLISH),

    /** PUBACK packet. */
    PUBACK(PACKET_TYPE_PUBACK),

    /** PUBREC packet. */
    PUBREC(PACKET_TYPE_PUBREC),

    /** PUBREL packet. */
    PUBREL(PACKET_TYPE_PUBREL),

    /** PUBCOMP packet. */
    PUBCOMP(PACKET_TYPE_PUBCOMP),

    /** SUBSCRIBE packet. */
    SUBSCRIBE(PACKET_TYPE_SUBSCRIBE),

    /** SUBACK packet. */
    SUBACK(PACKET_TYPE_SUBACK),

    /** UNSUBSCRIBE packet. */
    UNSUBSCRIBE(PACKET_TYPE_UNSUBSCRIBE),

    /** UNSUBACK packet. */
    UNSUBACK(PACKET_TYPE_UNSUBACK),

    /** PINGREQ packet. */
    PINGREQ(PACKET_TYPE_PINGREQ),

    /** PINGRESP packet. */
    PINGRESP(PACKET_TYPE_PINGRESP),

    /** DISCONNECT packet. */
    DISCONNECT(PACKET_TYPE_DISCONNECT),

    /** AUTH packet. */
    AUTH(PACKET_TYPE_AUTH),
    ;

    /** Utilities for converting wire-encoded packet headers to [PacketType]. */
    companion object {
        private val HeaderFormat = HexFormat { number.prefix = "0x" }

        /**
         * Converts the upper 4 bits of the specified MQTT header field into an instance of this.
         */
        fun from(header: Byte): PacketType {
            val value = (header.toInt() and MQTT_HEADER_BYTE_MASK) shr MQTT_PACKET_TYPE_SHIFT
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
