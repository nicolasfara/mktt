package io.github.nicolasfara.mktt.core.packet

/** MQTT PINGREQ packet used by a client to keep the connection alive. */
object Pingreq : BasePacket(PacketType.PINGREQ) {
    override fun toString(): String = "Pingreq"
}

// PINGREQ consists only of the fixed header, hence, nothing else to do here!
