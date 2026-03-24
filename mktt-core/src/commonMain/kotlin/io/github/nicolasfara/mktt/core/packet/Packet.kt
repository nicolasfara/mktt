package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.packet.readAuth
import io.github.nicolasfara.mktt.core.packet.readConnack
import io.github.nicolasfara.mktt.core.packet.readConnect
import io.github.nicolasfara.mktt.core.packet.readDisconnect
import io.github.nicolasfara.mktt.core.packet.readPublish
import io.github.nicolasfara.mktt.core.packet.readPublishResponse
import io.github.nicolasfara.mktt.core.packet.readSuback
import io.github.nicolasfara.mktt.core.packet.readSubscribe
import io.github.nicolasfara.mktt.core.packet.readUnsuback
import io.github.nicolasfara.mktt.core.packet.readUnsubscribe
import io.github.nicolasfara.mktt.core.packet.write
import io.github.nicolasfara.mktt.core.packet.writeBody
import io.github.nicolasfara.mktt.core.packet.writeFixedHeader
import io.github.nicolasfara.mktt.core.util.Logger
import io.github.nicolasfara.mktt.core.util.readVariableByteInt
import io.github.nicolasfara.mktt.core.util.writeVariableByteInt
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.io.Sink

public interface Packet {

    public val type: io.github.nicolasfara.mktt.core.packet.PacketType

    public val headerFlags: Int
}

/**
 * Determines whether this packet is of the specified type and its packet identifier is the same as the one of `packet`.
 */
public inline fun <reified T : io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket> io.github.nicolasfara.mktt.core.packet.Packet.isResponseFor(
    packet: io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket,
): Boolean = T::class.isInstance(this) &&
    packet.packetIdentifier ==
    (this as io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket).packetIdentifier

/**
 * Determines whether this packet is of the specified type and its packet identifier is the same as the one of `publish`.
 */
public inline fun <reified T : io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket> io.github.nicolasfara.mktt.core.packet.Packet.isResponseFor(
    publish: io.github.nicolasfara.mktt.core.packet.Publish,
): Boolean = T::class.isInstance(this) &&
    publish.packetIdentifier ==
    (this as io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket).packetIdentifier

/**
 * Reads a packet from this byte read channel. Blocks until the packet has been read completely
 *
 * @throws io.github.nicolasfara.mktt.core.MalformedPacketException when the packet cannot be parsed
 */
public suspend fun ByteReadChannel.readPacket(): io.github.nicolasfara.mktt.core.packet.Packet {
    val header = readByte()
    val type = _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.Companion.from(header)
    val length = readVariableByteInt()

    _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.v {
        "New MQTT header of type $type received, remaining packet has $length bytes"
    }

    return with(readPacket(length)) {
        when (type) {
            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNACK -> readConnack()

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNECT -> readConnect()

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBLISH -> readPublish(header.toInt())

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBACK -> readPublishResponse(
                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PubackFactory,
            )

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBREC -> readPublishResponse(
                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PubrecFactory,
            )

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBREL -> readPublishResponse(
                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PubrelFactory,
            )

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBCOMP -> readPublishResponse(
                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PubcompFactory,
            )

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.SUBSCRIBE -> readSubscribe()

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.SUBACK -> readSuback()

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.UNSUBSCRIBE -> readUnsubscribe()

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.UNSUBACK -> readUnsuback()

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGREQ -> _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pingreq

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGRESP -> _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pingresp

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.DISCONNECT -> readDisconnect(length)

            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.AUTH -> readAuth()
        }
    }
}

/**
 * Writes the bytes of the specified packet to this byte write channel.
 */
public suspend fun ByteWriteChannel.write(packet: io.github.nicolasfara.mktt.core.packet.Packet) {
    val bytes = buildPacket {
        writeBody(packet)
    }
    writeFixedHeader(packet, bytes.remaining.toInt())
    writePacket(bytes)
}

public fun Sink.write(packet: io.github.nicolasfara.mktt.core.packet.Packet) {
    val bytes = buildPacket {
        writeBody(packet)
    }
    writeFixedHeader(packet, bytes.remaining.toInt())
    writePacket(bytes)
}

/**
 * Write the bytes of this packet, **excluding** the variable header part.
 */
private fun Sink.writeBody(packet: io.github.nicolasfara.mktt.core.packet.Packet) {
    when (packet.type) {
        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNACK -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Connack,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNECT -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Connect,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBLISH -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Publish,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBACK -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Puback,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBREC -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Pubrec,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBREL -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Pubrel,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBCOMP -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Pubcomp,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.SUBSCRIBE -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Subscribe,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.SUBACK -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Suback,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.UNSUBSCRIBE -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Unsubscribe,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.UNSUBACK -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Unsuback,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGREQ -> Unit

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGRESP -> Unit

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.DISCONNECT -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Disconnect,
        )

        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.AUTH -> write(
            packet as io.github.nicolasfara.mktt.core.packet.Auth,
        )
    }
}

private fun Sink.writeFixedHeader(packet: io.github.nicolasfara.mktt.core.packet.Packet, remainingLength: Int) {
    check(packet.headerFlags < 16) { "Header flags may only contain 4 bits: ${packet.headerFlags}" }
    writeByte(((packet.type.value shl 4) or packet.headerFlags).toByte())
    writeVariableByteInt(remainingLength)
}

private suspend fun ByteWriteChannel.writeFixedHeader(
    packet: io.github.nicolasfara.mktt.core.packet.Packet,
    remainingLength: Int,
) {
    check(packet.headerFlags < 16) { "Header flags may only contain 4 bits: ${packet.headerFlags}" }
    writeByte(((packet.type.value shl 4) or packet.headerFlags).toByte())
    writeVariableByteInt(remainingLength)
}
