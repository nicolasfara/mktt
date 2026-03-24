package io.github.nicolasfara.mktt.core.packet

public object Pingresp : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGRESP,
) {

    override fun toString(): String = "Pingresp"
}

// PINGRESP consists only of the fixed header, hence, nothing else to do here!
