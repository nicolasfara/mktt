package io.github.nicolasfara.mktt.core.packet

public abstract class AbstractPacket(final override val type: io.github.nicolasfara.mktt.core.packet.PacketType) :
    io.github.nicolasfara.mktt.core.packet.Packet {

    override val headerFlags: Int = 0
}
