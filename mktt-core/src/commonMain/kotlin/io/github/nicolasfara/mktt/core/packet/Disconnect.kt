package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.NormalDisconnection
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.ServerReference
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.malformedWhen
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * MQTT DISCONNECT packet used to close a connection gracefully.
 *
 * @property reason disconnection reason code.
 * @property sessionExpiryInterval optional session expiry interval override.
 * @property reasonString optional human-readable disconnection reason.
 * @property userProperties optional user properties attached to this packet.
 * @property serverReference optional server reference for reconnect guidance.
 */
data class Disconnect(
    val reason: ReasonCode,
    val sessionExpiryInterval: SessionExpiryInterval? = null,
    val reasonString: ReasonString? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
    val serverReference: ServerReference? = null,
) : BasePacket(PacketType.DISCONNECT) {

    init {
        malformedWhen(reason == Success || reason == GrantedQoS0) {
            "Only 'NormalDisconnection' is an allowed reason code for successful disconnection"
        }
    }
}

internal fun Sink.write(disconnect: Disconnect) {
    with(disconnect) {
        writeByte(reason.code.toByte())
        // For Disconnect, there is no need to write the properties length bytes, in case there are no properties:
        if (sessionExpiryInterval != null || reasonString != null || serverReference != null ||
            userProperties.isNotEmpty()
        ) {
            writeProperties(
                sessionExpiryInterval,
                reasonString,
                serverReference,
                *userProperties.asArray,
            )
        }
    }
}

internal fun Source.readDisconnect(remainingLength: Int): Disconnect = if (remainingLength == 0) {
    Disconnect(NormalDisconnection)
} else {
    val reason = ReasonCode.from(
        readByte(),
        defaultSuccessReason = NormalDisconnection,
    )

    if (remainingLength == 1) {
        Disconnect(reason)
    } else {
        val properties = readProperties()
        Disconnect(
            reason = reason,
            sessionExpiryInterval = properties.singleOrNull<SessionExpiryInterval>(),
            reasonString = properties.singleOrNull<ReasonString>(),
            userProperties = UserProperties.from(
                properties,
            ),
            serverReference = properties.singleOrNull<ServerReference>(),
        )
    }
}
