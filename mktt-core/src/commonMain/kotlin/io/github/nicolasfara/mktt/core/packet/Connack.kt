package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source

public data class Connack(
    val isSessionPresent: Boolean,
    val reason: io.github.nicolasfara.mktt.core.ReasonCode,
    val sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval? = null,
    val receiveMaximum: io.github.nicolasfara.mktt.core.ReceiveMaximum? = null,
    val maximumQoS: io.github.nicolasfara.mktt.core.MaximumQoS? = null,
    val retainAvailable: io.github.nicolasfara.mktt.core.RetainAvailable? = null,
    val maximumPacketSize: io.github.nicolasfara.mktt.core.MaximumPacketSize? = null,
    val assignedClientIdentifier: io.github.nicolasfara.mktt.core.AssignedClientIdentifier? = null,
    val topicAliasMaximum: io.github.nicolasfara.mktt.core.TopicAliasMaximum? = null,
    val reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    val wildcardSubscriptionAvailable: io.github.nicolasfara.mktt.core.WildcardSubscriptionAvailable? = null,
    val subscriptionIdentifierAvailable: io.github.nicolasfara.mktt.core.SubscriptionIdentifierAvailable? = null,
    val sharedSubscriptionAvailable: io.github.nicolasfara.mktt.core.SharedSubscriptionAvailable? = null,
    val serverKeepAlive: io.github.nicolasfara.mktt.core.ServerKeepAlive? = null,
    val responseInformation: io.github.nicolasfara.mktt.core.ResponseInformation? = null,
    val serverReference: io.github.nicolasfara.mktt.core.ServerReference? = null,
    val authenticationMethod: io.github.nicolasfara.mktt.core.AuthenticationMethod? = null,
    val authenticationData: io.github.nicolasfara.mktt.core.AuthenticationData? = null,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNACK,
) {

    /**
     * Determines whether this CONNACK represents a successful connection attempt.
     */
    public val isSuccess: Boolean
        get() = reason.code == 0
}

internal fun Sink.write(connack: io.github.nicolasfara.mktt.core.packet.Connack) {
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
internal fun Source.readConnack(): io.github.nicolasfara.mktt.core.packet.Connack {
    val isSessionPresent = readByte() == 1.toByte()
    val reason = _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode.Companion.from(readByte())
    val properties = readProperties()

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Connack(
        isSessionPresent = isSessionPresent,
        reason = reason,
        sessionExpiryInterval = properties.singleOrNull<io.github.nicolasfara.mktt.core.SessionExpiryInterval>(),
        receiveMaximum = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReceiveMaximum>(),
        maximumQoS = properties.singleOrNull<io.github.nicolasfara.mktt.core.MaximumQoS>(),
        retainAvailable = properties.singleOrNull<io.github.nicolasfara.mktt.core.RetainAvailable>(),
        maximumPacketSize = properties.singleOrNull<io.github.nicolasfara.mktt.core.MaximumPacketSize>(),
        assignedClientIdentifier = properties.singleOrNull<io.github.nicolasfara.mktt.core.AssignedClientIdentifier>(),
        topicAliasMaximum = properties.singleOrNull<io.github.nicolasfara.mktt.core.TopicAliasMaximum>(),
        reasonString = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReasonString>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
        wildcardSubscriptionAvailable = properties.singleOrNull<io.github.nicolasfara.mktt.core.WildcardSubscriptionAvailable>(),
        subscriptionIdentifierAvailable = properties.singleOrNull<io.github.nicolasfara.mktt.core.SubscriptionIdentifierAvailable>(),
        sharedSubscriptionAvailable = properties.singleOrNull<io.github.nicolasfara.mktt.core.SharedSubscriptionAvailable>(),
        serverKeepAlive = properties.singleOrNull<io.github.nicolasfara.mktt.core.ServerKeepAlive>(),
        responseInformation = properties.singleOrNull<io.github.nicolasfara.mktt.core.ResponseInformation>(),
        serverReference = properties.singleOrNull<io.github.nicolasfara.mktt.core.ServerReference>(),
        authenticationMethod = properties.singleOrNull<io.github.nicolasfara.mktt.core.AuthenticationMethod>(),
        authenticationData = properties.singleOrNull<io.github.nicolasfara.mktt.core.AuthenticationData>(),
    )
}
