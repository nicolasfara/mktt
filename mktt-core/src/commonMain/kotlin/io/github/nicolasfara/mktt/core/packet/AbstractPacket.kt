package io.github.nicolasfara.mktt.core.packet

abstract class AbstractPacket(final override val type: PacketType) : Packet {
    override val headerFlags: Int = 0
}
