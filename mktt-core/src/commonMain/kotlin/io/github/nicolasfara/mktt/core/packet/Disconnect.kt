package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source

public data class Disconnect(
    val reason: io.github.nicolasfara.mktt.core.ReasonCode,
    val sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval? = null,
    val reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    val serverReference: io.github.nicolasfara.mktt.core.ServerReference? = null,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.DISCONNECT,
) {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(
            reason == _root_ide_package_.io.github.nicolasfara.mktt.core.Success ||
                reason == _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS0,
        ) {
            "Only 'NormalDisconnection' is an allowed reason code for successful disconnection"
        }
    }
}

internal fun Sink.write(disconnect: io.github.nicolasfara.mktt.core.packet.Disconnect) {
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

internal fun Source.readDisconnect(remainingLength: Int): io.github.nicolasfara.mktt.core.packet.Disconnect =
    if (remainingLength ==
        0
    ) {
        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Disconnect(
            _root_ide_package_.io.github.nicolasfara.mktt.core.NormalDisconnection,
        )
    } else {
        val reason = _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode.Companion.from(
            readByte(),
            defaultSuccessReason = _root_ide_package_.io.github.nicolasfara.mktt.core.NormalDisconnection,
        )

        if (remainingLength == 1) {
            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Disconnect(reason)
        } else {
            val properties = readProperties()
            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Disconnect(
                reason = reason,
                sessionExpiryInterval = properties.singleOrNull<io.github.nicolasfara.mktt.core.SessionExpiryInterval>(),
                reasonString = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReasonString>(),
                userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(
                    properties,
                ),
                serverReference = properties.singleOrNull<io.github.nicolasfara.mktt.core.ServerReference>(),
            )
        }
    }
