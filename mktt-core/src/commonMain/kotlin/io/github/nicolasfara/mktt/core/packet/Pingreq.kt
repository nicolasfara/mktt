package io.github.nicolasfara.mktt.core.packet

object Pingreq : AbstractPacket(PacketType.PINGREQ) {
    override fun toString(): String = "Pingreq"
}

// PINGREQ consists only of the fixed header, hence, nothing else to do here!
