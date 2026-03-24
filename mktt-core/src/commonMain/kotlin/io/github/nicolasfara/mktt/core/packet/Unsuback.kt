package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

public data class Unsuback(
    override val packetIdentifier: UShort,
    val reasons: List<io.github.nicolasfara.mktt.core.ReasonCode>,
    val reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.UNSUBACK,
),
    io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(reasons.isNotEmpty()) {
            "Reason codes must not be empty in UNSUBACK"
        }
    }
}

/**
 * Returns `true` when all reason codes of this UNSUBACK are either [io.github.nicolasfara.mktt.core.Success] or [io.github.nicolasfara.mktt.core.NoSubscriptionExisted].
 */
public val io.github.nicolasfara.mktt.core.packet.Unsuback.isUnsubscribed: Boolean
    get() = reasons.all {
        it.code == _root_ide_package_.io.github.nicolasfara.mktt.core.Success.code ||
            it.code == _root_ide_package_.io.github.nicolasfara.mktt.core.NoSubscriptionExisted.code
    }

internal fun Sink.write(unsuback: io.github.nicolasfara.mktt.core.packet.Unsuback) {
    with(unsuback) {
        writeUShort(packetIdentifier)
        writeProperties(reasonString, *userProperties.asArray)

        // Payload
        reasons.forEach {
            writeByte(it.code.toByte())
        }
    }
}

internal fun Source.readUnsuback(): io.github.nicolasfara.mktt.core.packet.Unsuback {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val reasons = buildList {
        while (!exhausted()) {
            add(_root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode.Companion.from(readByte()))
        }
    }

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Unsuback(
        packetIdentifier = packetIdentifier,
        reasonString = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReasonString>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
        reasons = reasons,
    )
}
