package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.packet.bits
import io.github.nicolasfara.mktt.core.packet.isCleanStartFlagSet
import io.github.nicolasfara.mktt.core.packet.isPasswordFlagSet
import io.github.nicolasfara.mktt.core.packet.isRetainWillMessageFlagSet
import io.github.nicolasfara.mktt.core.packet.isUserNameFlagSet
import io.github.nicolasfara.mktt.core.packet.isWillMessageFlagSet
import io.github.nicolasfara.mktt.core.packet.willQoS
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.readWillMessage
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.write
import io.github.nicolasfara.mktt.core.writeProperties
import io.ktor.utils.io.core.*
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort

public data class Connect(
    val isCleanStart: Boolean,
    val willMessage: io.github.nicolasfara.mktt.core.WillMessage?,
    val willQqS: io.github.nicolasfara.mktt.core.QoS,
    val retainWillMessage: Boolean,
    val keepAliveSeconds: UShort,
    val clientId: String,
    val userName: String? = null,
    val password: String? = null,
    val sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval? = null,
    val receiveMaximum: io.github.nicolasfara.mktt.core.ReceiveMaximum? = null,
    val maximumPacketSize: io.github.nicolasfara.mktt.core.MaximumPacketSize? = null,
    val topicAliasMaximum: io.github.nicolasfara.mktt.core.TopicAliasMaximum? = null,
    val requestResponseInformation: io.github.nicolasfara.mktt.core.RequestResponseInformation? = null,
    val requestProblemInformation: io.github.nicolasfara.mktt.core.RequestProblemInformation? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    val authenticationMethod: io.github.nicolasfara.mktt.core.AuthenticationMethod? = null,
    val authenticationData: io.github.nicolasfara.mktt.core.AuthenticationData? = null,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNECT,
) {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(
            (willMessage != null) || (willQqS == _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE),
        ) {
            "If the Will Flag is set to 0, then the Will QoS MUST be set to 0 (0x00) [MQTT-3.1.2-11]"
        }
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(willMessage == null && retainWillMessage) {
            "If the Will Flag is set to 0, then Will Retain MUST be set to 0 [MQTT-3.1.2-13]"
        }
    }

    override fun toString(): String {
        // Overwritten to prevent printing of password
        return "Connect(isCleanStart=$isCleanStart, willMessage=$willMessage, willOqS=$willQqS, retainWillMessage=$retainWillMessage, " +
            "keepAliveSeconds=$keepAliveSeconds, clientId='$clientId', userName=$userName, password=********, " +
            "sessionExpiryInterval=$sessionExpiryInterval, receiveMaximum=$receiveMaximum, maximumPacketSize=$maximumPacketSize, " +
            "topicAliasMaximum=$topicAliasMaximum, requestResponseInformation=$requestResponseInformation, " +
            "requestProblemInformation=$requestProblemInformation, userProperties=$userProperties, " +
            "authenticationMethod=$authenticationMethod, authenticationData=$authenticationData)"
    }
}

// The MQTT protocol name: "04MQTT" encoded as an MQTT string
private val ProtocolName = byteArrayOf(0, 4, 77, 81, 84, 84)

internal fun Sink.write(connect: io.github.nicolasfara.mktt.core.packet.Connect) {
    writeFully(_root_ide_package_.io.github.nicolasfara.mktt.core.packet.ProtocolName)
    writeByte(5) // MQTT version 5

    with(connect) {
        writeByte(bits)
        writeShort(keepAliveSeconds.toShort())
        writeProperties(
            sessionExpiryInterval,
            receiveMaximum,
            maximumPacketSize,
            topicAliasMaximum,
            requestResponseInformation,
            requestProblemInformation,
            authenticationMethod,
            authenticationData,
            *userProperties.asArray,
        )

        // Write the payload
        writeMqttString(clientId) // Must always be present!
        if (willMessage != null) {
            write(willMessage)
        }
        if (userName != null) {
            writeMqttString(userName)
        }
        if (password != null) {
            writeMqttString(password)
        }
    }
}

internal fun Source.readConnect(): io.github.nicolasfara.mktt.core.packet.Connect {
    val protocolName = readMqttString()
    _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(protocolName == "MQTT") {
        "Connect packet must start with 'MQTT', but is: '$protocolName'"
    }

    val version = readByte()
    _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(version == 5.toByte()) {
        "Connect packet version must be 5, but is: $version"
    }

    val bits = readByte()
    val keepAliveSeconds = readUShort()
    val properties = readProperties()
    val clientId = readMqttString()
    val willMessage = if (bits.isWillMessageFlagSet) {
        readWillMessage()
    } else {
        null
    }
    val userName = if (bits.isUserNameFlagSet) {
        readMqttString()
    } else {
        null
    }
    val password = if (bits.isPasswordFlagSet) {
        readMqttString()
    } else {
        null
    }

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Connect(
        isCleanStart = bits.isCleanStartFlagSet,
        willMessage = willMessage,
        willQqS = bits.willQoS,
        retainWillMessage = bits.isRetainWillMessageFlagSet,
        keepAliveSeconds = keepAliveSeconds,
        clientId = clientId,
        userName = userName,
        password = password,
        sessionExpiryInterval = properties.singleOrNull<io.github.nicolasfara.mktt.core.SessionExpiryInterval>(),
        receiveMaximum = properties.singleOrNull(),
        maximumPacketSize = properties.singleOrNull(),
        topicAliasMaximum = properties.singleOrNull(),
        requestResponseInformation = properties.singleOrNull<io.github.nicolasfara.mktt.core.RequestResponseInformation>(),
        requestProblemInformation = properties.singleOrNull<io.github.nicolasfara.mktt.core.RequestProblemInformation>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
        authenticationMethod = properties.singleOrNull<io.github.nicolasfara.mktt.core.AuthenticationMethod>(),
        authenticationData = properties.singleOrNull<io.github.nicolasfara.mktt.core.AuthenticationData>(),
    )
}

private val io.github.nicolasfara.mktt.core.packet.Connect.bits: Byte
    get() {
        var bits = if (isCleanStart) 2 else 0
        if (willMessage != null) {
            // When there is not will message, the QoS and retain flags must be zero, hence evaluate them here:
            bits = (bits or 4) or (willQqS.value shl 3)
            if (retainWillMessage) bits = bits or (1 shl 5)
        }
        if (password != null) bits = bits or (1 shl 6)
        if (userName != null) bits = bits or (1 shl 7)

        return bits.toByte()
    }

private val Byte.willQoS: io.github.nicolasfara.mktt.core.QoS
    get() = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.Companion.from(((toInt() and 24) shr 3))

private val Byte.isCleanStartFlagSet: Boolean
    get() = (toInt() and 2) != 0

private val Byte.isWillMessageFlagSet: Boolean
    get() = (toInt() and 4) != 0

private val Byte.isRetainWillMessageFlagSet: Boolean
    get() = (toInt() and (1 shl 5)) != 0

private val Byte.isPasswordFlagSet: Boolean
    get() = (toInt() and (1 shl 6)) != 0

private val Byte.isUserNameFlagSet: Boolean
    get() = (toInt() and (1 shl 7)) != 0
