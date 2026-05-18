package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.NoSubscriptionExisted
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.wellFormedWhen
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

/**
 * MQTT UNSUBACK packet sent by the server in response to an UNSUBSCRIBE.
 *
 * @property packetIdentifier packet identifier matching the corresponding UNSUBSCRIBE request.
 * @property reasons result reason code for each requested topic filter.
 * @property reasonString optional human-readable diagnostic reason provided by the server.
 * @property userProperties optional user properties attached to this packet.
 */
data class Unsuback(
    override val packetIdentifier: UShort,
    val reasons: List<ReasonCode>,
    val reasonString: ReasonString? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) : BasePacket(PacketType.UNSUBACK),
    PacketIdentifierPacket {

    init {
        wellFormedWhen(reasons.isNotEmpty()) {
            "Reason codes must not be empty in UNSUBACK"
        }
    }
}

/**
 * Returns `true` when all reason codes of this UNSUBACK are either
 * [io.github.nicolasfara.mktt.core.Success] or
 * [io.github.nicolasfara.mktt.core.NoSubscriptionExisted].
 */
val Unsuback.isUnsubscribed: Boolean
    get() = reasons.all {
        it.code == Success.code ||
            it.code == NoSubscriptionExisted.code
    }

internal fun Sink.write(unsuback: Unsuback) {
    with(unsuback) {
        writeUShort(packetIdentifier)
        writeProperties(reasonString, *userProperties.asArray)

        // Payload
        reasons.forEach {
            writeByte(it.code.toByte())
        }
    }
}

internal fun Source.readUnsuback(): Unsuback {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val reasons = buildList {
        while (!exhausted()) {
            add(ReasonCode.from(readByte()))
        }
    }

    return Unsuback(
        packetIdentifier = packetIdentifier,
        reasonString = properties.singleOrNull<ReasonString>(),
        userProperties = UserProperties.from(properties),
        reasons = reasons,
    )
}
