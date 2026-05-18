package io.github.nicolasfara.mktt.core.packet

/**
 * Base implementation of [Packet] that stores the packet type and default header flags.
 */
open class BasePacket(final override val type: PacketType) : Packet {
    override val headerFlags: Int = 0
}
