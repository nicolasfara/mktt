package io.github.nicolasfara.mktt.core.packet

/**
 * Interface identifying packets that strictly require a packet identifier.
 *
 * Includes: PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK,
 * UNSUBSCRIBE and UNSUBACK.
 *
 * This does not include PUBLISH, since packet identifiers are only required
 * for PUBLISH packets with QoS greater than 0.
 */
interface PacketIdentifierPacket : Packet {
    /** Packet identifier associated with this packet. */
    val packetIdentifier: UShort
}
