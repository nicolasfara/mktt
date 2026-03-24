package io.github.nicolasfara.mktt.core.packet

public object Pingreq : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGREQ,
) {

    override fun toString(): String = "Pingreq"
}

// PINGREQ consists only of the fixed header, hence, nothing else to do here!
