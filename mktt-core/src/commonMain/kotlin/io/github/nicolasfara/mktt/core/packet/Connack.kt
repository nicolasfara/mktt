package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.AssignedClientIdentifier
import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.MaximumPacketSize
import io.github.nicolasfara.mktt.core.MaximumQoS
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.ReceiveMaximum
import io.github.nicolasfara.mktt.core.ResponseInformation
import io.github.nicolasfara.mktt.core.RetainAvailable
import io.github.nicolasfara.mktt.core.ServerKeepAlive
import io.github.nicolasfara.mktt.core.ServerReference
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SharedSubscriptionAvailable
import io.github.nicolasfara.mktt.core.SubscriptionIdentifierAvailable
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.WildcardSubscriptionAvailable
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * MQTT CONNACK packet sent by the server in response to CONNECT.
 *
 * @property isSessionPresent whether an existing session is present on the server.
 * @property reason connection result reason code.
 * @property sessionExpiryInterval optional session expiry interval assigned by the server.
 * @property receiveMaximum optional receive maximum assigned by the server.
 * @property maximumQoS optional maximum QoS accepted by the server.
 * @property retainAvailable optional retain support flag from the server.
 * @property maximumPacketSize optional maximum packet size accepted by the server.
 * @property assignedClientIdentifier optional server-assigned client identifier.
 * @property topicAliasMaximum optional topic alias maximum accepted by the server.
 * @property reasonString optional human-readable reason string.
 * @property userProperties optional user properties attached to this packet.
 * @property wildcardSubscriptionAvailable optional wildcard-subscription support flag.
 * @property subscriptionIdentifierAvailable optional subscription-identifier support flag.
 * @property sharedSubscriptionAvailable optional shared-subscription support flag.
 * @property serverKeepAlive optional keep-alive value requested by the server.
 * @property responseInformation optional response information provided by the server.
 * @property serverReference optional server reference for reconnect guidance.
 * @property authenticationMethod optional authentication method.
 * @property authenticationData optional authentication data.
 */
data class Connack(
    val isSessionPresent: Boolean,
    val reason: ReasonCode,
    val sessionExpiryInterval: SessionExpiryInterval? = null,
    val receiveMaximum: ReceiveMaximum? = null,
    val maximumQoS: MaximumQoS? = null,
    val retainAvailable: RetainAvailable? = null,
    val maximumPacketSize: MaximumPacketSize? = null,
    val assignedClientIdentifier: AssignedClientIdentifier? = null,
    val topicAliasMaximum: TopicAliasMaximum? = null,
    val reasonString: ReasonString? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
    val wildcardSubscriptionAvailable: WildcardSubscriptionAvailable? = null,
    val subscriptionIdentifierAvailable: SubscriptionIdentifierAvailable? = null,
    val sharedSubscriptionAvailable: SharedSubscriptionAvailable? = null,
    val serverKeepAlive: ServerKeepAlive? = null,
    val responseInformation: ResponseInformation? = null,
    val serverReference: ServerReference? = null,
    val authenticationMethod: AuthenticationMethod? = null,
    val authenticationData: AuthenticationData? = null,
) : BasePacket(PacketType.CONNACK) {

    /**
     * Determines whether this CONNACK represents a successful connection attempt.
     */
    val isSuccess: Boolean
        get() = reason.code == 0
}

internal fun Sink.write(connack: Connack) {
    with(connack) {
        writeByte(if (isSessionPresent) 1 else 0)
        writeByte(reason.code.toByte())
        writeProperties(
            sessionExpiryInterval,
            receiveMaximum,
            maximumQoS,
            retainAvailable,
            maximumPacketSize,
            assignedClientIdentifier,
            topicAliasMaximum,
            reasonString,
            wildcardSubscriptionAvailable,
            subscriptionIdentifierAvailable,
            sharedSubscriptionAvailable,
            serverKeepAlive,
            responseInformation,
            serverReference,
            authenticationMethod,
            authenticationData,
            *userProperties.asArray,
        )
    }
}

/**
 * Constructs a Connack packet from this byte read packet. Expects the packet to start at the remaining length (byte 2)
 * of the fixed header of the Connack packet.
 */
internal fun Source.readConnack(): Connack {
    val isSessionPresent = readByte() == 1.toByte()
    val reason = ReasonCode.from(readByte())
    val properties = readProperties()

    return Connack(
        isSessionPresent = isSessionPresent,
        reason = reason,
        sessionExpiryInterval = properties.singleOrNull<SessionExpiryInterval>(),
        receiveMaximum = properties.singleOrNull<ReceiveMaximum>(),
        maximumQoS = properties.singleOrNull<MaximumQoS>(),
        retainAvailable = properties.singleOrNull<RetainAvailable>(),
        maximumPacketSize = properties.singleOrNull<MaximumPacketSize>(),
        assignedClientIdentifier = properties.singleOrNull<AssignedClientIdentifier>(),
        topicAliasMaximum = properties.singleOrNull<TopicAliasMaximum>(),
        reasonString = properties.singleOrNull<ReasonString>(),
        userProperties = UserProperties.from(properties),
        wildcardSubscriptionAvailable = properties.singleOrNull<WildcardSubscriptionAvailable>(),
        subscriptionIdentifierAvailable = properties.singleOrNull<SubscriptionIdentifierAvailable>(),
        sharedSubscriptionAvailable = properties.singleOrNull<SharedSubscriptionAvailable>(),
        serverKeepAlive = properties.singleOrNull<ServerKeepAlive>(),
        responseInformation = properties.singleOrNull<ResponseInformation>(),
        serverReference = properties.singleOrNull<ServerReference>(),
        authenticationMethod = properties.singleOrNull<AuthenticationMethod>(),
        authenticationData = properties.singleOrNull<AuthenticationData>(),
    )
}
