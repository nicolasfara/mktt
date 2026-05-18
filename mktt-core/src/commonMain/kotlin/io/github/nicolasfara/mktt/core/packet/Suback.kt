package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.GrantedQoS2
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.malformedWhen
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

/**
 * MQTT SUBACK packet sent by the server in response to a SUBSCRIBE request.
 *
 * @property packetIdentifier packet identifier matching the corresponding SUBSCRIBE request.
 * @property reasons result reason code for each requested topic filter.
 * @property reasonString optional human-readable diagnostic reason provided by the server.
 * @property userProperties optional user properties attached to this packet.
 */
data class Suback(
    override val packetIdentifier: UShort,
    val reasons: List<ReasonCode>,
    val reasonString: ReasonString? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) : BasePacket(PacketType.SUBACK),
    PacketIdentifierPacket {

    init {
        malformedWhen(reasons.isEmpty()) {
            "Reason codes must not be empty in SUBACK"
        }
        malformedWhen(reasons.contains(Success)) {
            "Reason code 'Success' is not allowed for SUBACK"
        }
    }
}

/**
 * Returns `true` when this SUBACK packet contains a reason code which not indicates a success.
 */
val Suback.hasFailure: Boolean
    get() = reasons.any { it.code > GrantedQoS2.code }

internal fun Sink.write(suback: Suback) {
    with(suback) {
        writeUShort(packetIdentifier)
        writeProperties(reasonString, *userProperties.asArray)

        // Payload
        reasons.forEach {
            writeByte(it.code.toByte())
        }
    }
}

internal fun Source.readSuback(): Suback {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val reasons = buildList {
        while (!exhausted()) {
            add(
                ReasonCode.from(
                    readByte(),
                    defaultSuccessReason = GrantedQoS0,
                ),
            )
        }
    }

    return Suback(
        packetIdentifier = packetIdentifier,
        reasonString = properties.singleOrNull<ReasonString>(),
        userProperties = UserProperties.from(properties),
        reasons = reasons,
    )
}
