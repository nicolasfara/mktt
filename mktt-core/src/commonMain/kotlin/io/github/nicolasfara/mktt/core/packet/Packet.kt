package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.util.Logger
import io.github.nicolasfara.mktt.core.util.readVariableByteInt
import io.github.nicolasfara.mktt.core.util.writeVariableByteInt
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.core.writePacket
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writePacket
import kotlinx.io.Sink

interface Packet {
    val type: PacketType
    val headerFlags: Int
}

/**
 * Determines whether this packet is of the specified type and its packet identifier is the same as the one of `packet`.
 */
inline fun <reified T : PacketIdentifierPacket> Packet.isResponseFor(packet: PacketIdentifierPacket): Boolean =
    T::class.isInstance(this) &&
        packet.packetIdentifier ==
        (this as PacketIdentifierPacket).packetIdentifier

/**
 * Determines whether this packet is of the specified type and its packet identifier is the same as the one of `publish`.
 */
inline fun <reified T : PacketIdentifierPacket> Packet.isResponseFor(publish: Publish): Boolean =
    T::class.isInstance(this) &&
        publish.packetIdentifier ==
        (this as PacketIdentifierPacket).packetIdentifier

/**
 * Reads a packet from this byte read channel. Blocks until the packet has been read completely
 *
 * @throws io.github.nicolasfara.mktt.core.MalformedPacketException when the packet cannot be parsed
 */
suspend fun ByteReadChannel.readPacket(): Packet {
    val header = readByte()
    val type = PacketType.from(header)
    val length = readVariableByteInt()

    Logger.v {
        "New MQTT header of type $type received, remaining packet has $length bytes"
    }

    return with(readPacket(length)) {
        when (type) {
            PacketType.CONNACK -> readConnack()

            PacketType.CONNECT -> readConnect()

            PacketType.PUBLISH -> readPublish(header.toInt())

            PacketType.PUBACK -> readPublishResponse(
                PubackFactory,
            )

            PacketType.PUBREC -> readPublishResponse(
                PubrecFactory,
            )

            PacketType.PUBREL -> readPublishResponse(
                PubrelFactory,
            )

            PacketType.PUBCOMP -> readPublishResponse(
                PubcompFactory,
            )

            PacketType.SUBSCRIBE -> readSubscribe()

            PacketType.SUBACK -> readSuback()

            PacketType.UNSUBSCRIBE -> readUnsubscribe()

            PacketType.UNSUBACK -> readUnsuback()

            PacketType.PINGREQ -> Pingreq

            PacketType.PINGRESP -> Pingresp

            PacketType.DISCONNECT -> readDisconnect(length)

            PacketType.AUTH -> readAuth()
        }
    }
}

/**
 * Writes the bytes of the specified packet to this byte write channel.
 */
suspend fun ByteWriteChannel.write(packet: Packet) {
    val bytes = buildPacket {
        writeBody(packet)
    }
    writeFixedHeader(packet, bytes.remaining.toInt())
    writePacket(bytes)
}

fun Sink.write(packet: Packet) {
    val bytes = buildPacket {
        writeBody(packet)
    }
    writeFixedHeader(packet, bytes.remaining.toInt())
    writePacket(bytes)
}

/**
 * Write the bytes of this packet, **excluding** the variable header part.
 */
private fun Sink.writeBody(packet: Packet) {
    when (packet.type) {
        PacketType.CONNACK -> write(packet as Connack)
        PacketType.CONNECT -> write(packet as Connect)
        PacketType.PUBLISH -> write(packet as Publish)
        PacketType.PUBACK -> write(packet as Puback)
        PacketType.PUBREC -> write(packet as Pubrec)
        PacketType.PUBREL -> write(packet as Pubrel)
        PacketType.PUBCOMP -> write(packet as Pubcomp)
        PacketType.SUBSCRIBE -> write(packet as Subscribe)
        PacketType.SUBACK -> write(packet as Suback)
        PacketType.UNSUBSCRIBE -> write(packet as Unsubscribe)
        PacketType.UNSUBACK -> write(packet as Unsuback)
        PacketType.PINGREQ -> Unit
        PacketType.PINGRESP -> Unit
        PacketType.DISCONNECT -> write(packet as Disconnect)
        PacketType.AUTH -> write(packet as Auth)
    }
}

private fun Sink.writeFixedHeader(packet: Packet, remainingLength: Int) {
    check(packet.headerFlags < 16) { "Header flags may only contain 4 bits: ${packet.headerFlags}" }
    writeByte(((packet.type.value shl 4) or packet.headerFlags).toByte())
    writeVariableByteInt(remainingLength)
}

private suspend fun ByteWriteChannel.writeFixedHeader(packet: Packet, remainingLength: Int) {
    check(packet.headerFlags < 16) { "Header flags may only contain 4 bits: ${packet.headerFlags}" }
    writeByte(((packet.type.value shl 4) or packet.headerFlags).toByte())
    writeVariableByteInt(remainingLength)
}
