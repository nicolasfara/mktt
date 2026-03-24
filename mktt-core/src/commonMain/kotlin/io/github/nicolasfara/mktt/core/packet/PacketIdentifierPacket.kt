package io.github.nicolasfara.mktt.core.packet

/**
 * Interface identifying packets that strictly require a packet identifier. Hence, for: PUBACK, PUBREC, PUBREL, PUBCOMP,
 * SUBSCRIBE, SUBACK, UNSUBSCRIBE and UNSUBACK. (Does not include PUBLISH, as the packet identifier is not always
 * required for PUBLISH packets.)
 */
public interface PacketIdentifierPacket : io.github.nicolasfara.mktt.core.packet.Packet {

    public val packetIdentifier: UShort
}
