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

data class Unsubscribe(
    override val packetIdentifier: UShort,
    val topics: List<Topic>,
    val userProperties: UserProperties = UserProperties.EMPTY,
) : AbstractPacket(PacketType.UNSUBSCRIBE),
    PacketIdentifierPacket {

    init {
        wellFormedWhen(topics.isNotEmpty()) {
            "Empty topic list in UNSUBSCRIBE"
        }
    }

    override val headerFlags: Int = 2
}

internal fun Sink.write(unsubscribe: Unsubscribe) {
    with(unsubscribe) {
        writeUShort(packetIdentifier)
        writeProperties(*userProperties.asArray)

        // Payload
        topics.forEach {
            writeMqttString(it.name)
        }
    }
}

internal fun Source.readUnsubscribe(): Unsubscribe {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val topics = buildList {
        while (!exhausted()) {
            add(Topic(readMqttString()))
        }
    }
    return Unsubscribe(
        packetIdentifier,
        topics,
        UserProperties.from(properties),
    )
}
