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

public data class Suback(
    override val packetIdentifier: UShort,
    val reasons: List<io.github.nicolasfara.mktt.core.ReasonCode>,
    val reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.SUBACK,
),
    io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(reasons.isEmpty()) {
            "Reason codes must not be empty in SUBACK"
        }
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(
            reasons.contains(_root_ide_package_.io.github.nicolasfara.mktt.core.Success),
        ) {
            "Reason code 'Success' is not allowed for SUBACK"
        }
    }
}

/**
 * Returns `true` when this SUBACK packet contains a reason code which not indicates a success.
 */
public val io.github.nicolasfara.mktt.core.packet.Suback.hasFailure: Boolean
    get() = reasons.any { it.code > _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS2.code }

internal fun Sink.write(suback: io.github.nicolasfara.mktt.core.packet.Suback) {
    with(suback) {
        writeUShort(packetIdentifier)
        writeProperties(reasonString, *userProperties.asArray)

        // Payload
        reasons.forEach {
            writeByte(it.code.toByte())
        }
    }
}

internal fun Source.readSuback(): io.github.nicolasfara.mktt.core.packet.Suback {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val reasons = buildList {
        while (!exhausted()) {
            add(
                _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode.Companion.from(
                    readByte(),
                    defaultSuccessReason = _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS0,
                ),
            )
        }
    }

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Suback(
        packetIdentifier = packetIdentifier,
        reasonString = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReasonString>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
        reasons = reasons,
    )
}
