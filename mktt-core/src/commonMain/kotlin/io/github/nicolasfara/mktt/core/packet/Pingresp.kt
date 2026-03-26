package io.github.nicolasfara.mktt.core.packet

/** MQTT PINGRESP packet sent by the peer in response to [Pingreq]. */
object Pingresp : BasePacket(PacketType.PINGRESP) {
    override fun toString(): String = "Pingresp"
}

// PINGRESP consists only of the fixed header, hence, nothing else to do here!
