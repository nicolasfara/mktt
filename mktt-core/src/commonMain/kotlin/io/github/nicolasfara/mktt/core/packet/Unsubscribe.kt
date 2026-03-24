package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

public data class Unsubscribe(
    public override val packetIdentifier: UShort,
    public val topics: List<io.github.nicolasfara.mktt.core.Topic>,
    public val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.UNSUBSCRIBE,
),
    io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(topics.isNotEmpty()) {
            "Empty topic list in UNSUBSCRIBE"
        }
    }

    override val headerFlags: Int = 2
}

internal fun Sink.write(unsubscribe: io.github.nicolasfara.mktt.core.packet.Unsubscribe) {
    with(unsubscribe) {
        writeUShort(packetIdentifier)
        writeProperties(*userProperties.asArray)

        // Payload
        topics.forEach {
            writeMqttString(it.name)
        }
    }
}

internal fun Source.readUnsubscribe(): io.github.nicolasfara.mktt.core.packet.Unsubscribe {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val topics = buildList {
        while (!exhausted()) {
            add(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic(readMqttString()))
        }
    }
    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Unsubscribe(
        packetIdentifier,
        topics,
        _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
    )
}
