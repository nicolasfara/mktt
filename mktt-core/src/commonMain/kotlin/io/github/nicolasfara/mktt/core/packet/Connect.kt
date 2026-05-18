package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.MaximumPacketSize
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReceiveMaximum
import io.github.nicolasfara.mktt.core.RequestProblemInformation
import io.github.nicolasfara.mktt.core.RequestResponseInformation
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.WillMessage
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.malformedWhen
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.readWillMessage
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.wellFormedWhen
import io.github.nicolasfara.mktt.core.write
import io.github.nicolasfara.mktt.core.writeProperties
import io.ktor.utils.io.core.writeFully
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort

private const val MQTT_PROTOCOL_NAME_LENGTH_MSB: Byte = 0
private const val MQTT_PROTOCOL_NAME_LENGTH_LSB: Byte = 4
private const val MQTT_PROTOCOL_CHAR_M: Byte = 77
private const val MQTT_PROTOCOL_CHAR_Q: Byte = 81
private const val MQTT_PROTOCOL_CHAR_T: Byte = 84
private const val MQTT_VERSION_5: Byte = 5

private const val CLEAN_START_FLAG_MASK = 0b0000_0010
private const val WILL_MESSAGE_FLAG_MASK = 0b0000_0100
private const val WILL_QOS_MASK = 0b0001_1000
private const val WILL_QOS_SHIFT = 3
private const val RETAIN_WILL_MESSAGE_FLAG_MASK = 0b0010_0000
private const val PASSWORD_FLAG_MASK = 0b0100_0000
private const val USERNAME_FLAG_MASK = 0b1000_0000

/**
 * MQTT CONNECT packet used by a client to initiate a session with a server.
 *
 * @property isCleanStart whether a new clean session is requested.
 * @property willMessage optional will message sent by the server on unexpected disconnect.
 * @property willQqS QoS used for the will message.
 * @property retainWillMessage whether the will message should be retained by the server.
 * @property keepAliveSeconds keep-alive interval requested by the client.
 * @property clientId client identifier used for this connection.
 * @property userName optional username for authentication.
 * @property password optional password for authentication.
 * @property sessionExpiryInterval optional session expiry interval.
 * @property receiveMaximum optional receive maximum setting.
 * @property maximumPacketSize optional maximum packet size accepted by the client.
 * @property topicAliasMaximum optional topic alias maximum accepted by the client.
 * @property requestResponseInformation optional response-information request flag.
 * @property requestProblemInformation optional problem-information request flag.
 * @property userProperties optional user properties attached to this packet.
 * @property authenticationMethod optional authentication method.
 * @property authenticationData optional authentication data.
 */
data class Connect(
    val isCleanStart: Boolean,
    val willMessage: WillMessage?,
    val willQqS: QoS,
    val retainWillMessage: Boolean,
    val keepAliveSeconds: UShort,
    val clientId: String,
    val userName: String? = null,
    val password: String? = null,
    val sessionExpiryInterval: SessionExpiryInterval? = null,
    val receiveMaximum: ReceiveMaximum? = null,
    val maximumPacketSize: MaximumPacketSize? = null,
    val topicAliasMaximum: TopicAliasMaximum? = null,
    val requestResponseInformation: RequestResponseInformation? = null,
    val requestProblemInformation: RequestProblemInformation? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
    val authenticationMethod: AuthenticationMethod? = null,
    val authenticationData: AuthenticationData? = null,
) : BasePacket(PacketType.CONNECT) {

    init {
        wellFormedWhen((willMessage != null) || (willQqS == QoS.AT_MOST_ONCE)) {
            "If the Will Flag is set to 0, then the Will QoS MUST be set to 0 (0x00) [MQTT-3.1.2-11]"
        }
        malformedWhen(willMessage == null && retainWillMessage) {
            "If the Will Flag is set to 0, then Will Retain MUST be set to 0 [MQTT-3.1.2-13]"
        }
    }

    override fun toString(): String {
        // Overwritten to prevent printing of password
        return "Connect(" +
            "isCleanStart=$isCleanStart, " +
            "willMessage=$willMessage, " +
            "willOqS=$willQqS, " +
            "retainWillMessage=$retainWillMessage, " +
            "keepAliveSeconds=$keepAliveSeconds, " +
            "clientId='$clientId', " +
            "userName=$userName, " +
            "password=********, " +
            "sessionExpiryInterval=$sessionExpiryInterval, " +
            "receiveMaximum=$receiveMaximum, " +
            "maximumPacketSize=$maximumPacketSize, " +
            "topicAliasMaximum=$topicAliasMaximum, " +
            "requestResponseInformation=$requestResponseInformation, " +
            "requestProblemInformation=$requestProblemInformation, " +
            "userProperties=$userProperties, " +
            "authenticationMethod=$authenticationMethod, " +
            "authenticationData=$authenticationData)"
    }
}

// The MQTT protocol name: "04MQTT" encoded as an MQTT string
private val ProtocolName = byteArrayOf(
    MQTT_PROTOCOL_NAME_LENGTH_MSB,
    MQTT_PROTOCOL_NAME_LENGTH_LSB,
    MQTT_PROTOCOL_CHAR_M,
    MQTT_PROTOCOL_CHAR_Q,
    MQTT_PROTOCOL_CHAR_T,
    MQTT_PROTOCOL_CHAR_T,
)

internal fun Sink.write(connect: Connect) {
    writeFully(ProtocolName)
    writeByte(MQTT_VERSION_5)

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

internal fun Source.readConnect(): Connect {
    val protocolName = readMqttString()
    wellFormedWhen(protocolName == "MQTT") {
        "Connect packet must start with 'MQTT', but is: '$protocolName'"
    }

    val version = readByte()
    wellFormedWhen(version == MQTT_VERSION_5) {
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

    return Connect(
        isCleanStart = bits.isCleanStartFlagSet,
        willMessage = willMessage,
        willQqS = bits.willQoS,
        retainWillMessage = bits.isRetainWillMessageFlagSet,
        keepAliveSeconds = keepAliveSeconds,
        clientId = clientId,
        userName = userName,
        password = password,
        sessionExpiryInterval = properties.singleOrNull<SessionExpiryInterval>(),
        receiveMaximum = properties.singleOrNull(),
        maximumPacketSize = properties.singleOrNull(),
        topicAliasMaximum = properties.singleOrNull(),
        requestResponseInformation = properties.singleOrNull<RequestResponseInformation>(),
        requestProblemInformation = properties.singleOrNull<RequestProblemInformation>(),
        userProperties = UserProperties.from(properties),
        authenticationMethod = properties.singleOrNull<AuthenticationMethod>(),
        authenticationData = properties.singleOrNull<AuthenticationData>(),
    )
}

private val Connect.bits: Byte
    get() {
        var bits = if (isCleanStart) CLEAN_START_FLAG_MASK else 0
        if (willMessage != null) {
            // When there is not will message, the QoS and retain flags must be zero, hence evaluate them here:
            bits = (bits or WILL_MESSAGE_FLAG_MASK) or (willQqS.value shl WILL_QOS_SHIFT)
            if (retainWillMessage) bits = bits or RETAIN_WILL_MESSAGE_FLAG_MASK
        }
        if (password != null) bits = bits or PASSWORD_FLAG_MASK
        if (userName != null) bits = bits or USERNAME_FLAG_MASK

        return bits.toByte()
    }

private val Byte.willQoS: QoS
    get() = QoS.from((toInt() and WILL_QOS_MASK) shr WILL_QOS_SHIFT)

private val Byte.isCleanStartFlagSet: Boolean
    get() = (toInt() and CLEAN_START_FLAG_MASK) != 0

private val Byte.isWillMessageFlagSet: Boolean
    get() = (toInt() and WILL_MESSAGE_FLAG_MASK) != 0

private val Byte.isRetainWillMessageFlagSet: Boolean
    get() = (toInt() and RETAIN_WILL_MESSAGE_FLAG_MASK) != 0

private val Byte.isPasswordFlagSet: Boolean
    get() = (toInt() and PASSWORD_FLAG_MASK) != 0

private val Byte.isUserNameFlagSet: Boolean
    get() = (toInt() and USERNAME_FLAG_MASK) != 0
