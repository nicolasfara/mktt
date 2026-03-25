package io.github.nicolasfara.mktt.core.packet

object Pingresp : AbstractPacket(PacketType.PINGRESP) {
    override fun toString(): String = "Pingresp"
}

// PINGRESP consists only of the fixed header, hence, nothing else to do here!
