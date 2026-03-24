package io.github.nicolasfara.mktt.core

/**
 * Represents a **Reason Code** as specified in
 * [chapter 2.4](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901031) of the MQTT specification.
 *
 * @property code the decimal reason code value as defined in MQTT
 * @property name the name of this reason code
 * @see io.github.nicolasfara.mktt.core.Success
 * @see io.github.nicolasfara.mktt.core.NormalDisconnection
 * @see io.github.nicolasfara.mktt.core.GrantedQoS0
 * @see io.github.nicolasfara.mktt.core.GrantedQoS1
 * @see io.github.nicolasfara.mktt.core.GrantedQoS2
 * @see io.github.nicolasfara.mktt.core.DisconnectWithWillMessage
 * @see io.github.nicolasfara.mktt.core.NoMatchingSubscribers
 * @see io.github.nicolasfara.mktt.core.NoSubscriptionExisted
 * @see io.github.nicolasfara.mktt.core.ContinueAuthentication
 * @see io.github.nicolasfara.mktt.core.ReAuthenticate
 * @see io.github.nicolasfara.mktt.core.UnspecifiedError
 * @see io.github.nicolasfara.mktt.core.MalformedPacket
 * @see io.github.nicolasfara.mktt.core.ProtocolError
 * @see io.github.nicolasfara.mktt.core.ImplementationSpecificError
 * @see io.github.nicolasfara.mktt.core.UnsupportedProtocolVersion
 * @see io.github.nicolasfara.mktt.core.ClientIdentifierNotValid
 * @see io.github.nicolasfara.mktt.core.BadUserNameOrPassword
 * @see io.github.nicolasfara.mktt.core.NotAuthorized
 * @see io.github.nicolasfara.mktt.core.ServerUnavailable
 * @see io.github.nicolasfara.mktt.core.ServerBusy
 * @see io.github.nicolasfara.mktt.core.Banned
 * @see io.github.nicolasfara.mktt.core.ServerShuttingDown
 * @see io.github.nicolasfara.mktt.core.BadAuthenticationMethod
 * @see io.github.nicolasfara.mktt.core.KeepAliveTimeout
 * @see io.github.nicolasfara.mktt.core.SessionTakenOver
 * @see io.github.nicolasfara.mktt.core.TopicFilterInvalid
 * @see io.github.nicolasfara.mktt.core.TopicNameInvalid
 * @see io.github.nicolasfara.mktt.core.PacketIdentifierInUse
 * @see io.github.nicolasfara.mktt.core.PacketIdentifierNotFound
 * @see io.github.nicolasfara.mktt.core.ReceiveMaximumExceeded
 * @see io.github.nicolasfara.mktt.core.TopicAliasInvalid
 * @see io.github.nicolasfara.mktt.core.PacketTooLarge
 * @see io.github.nicolasfara.mktt.core.MessageRateTooHigh
 * @see io.github.nicolasfara.mktt.core.QuotaExceeded
 * @see io.github.nicolasfara.mktt.core.AdministrativeAction
 * @see io.github.nicolasfara.mktt.core.PayloadFormatInvalid
 * @see io.github.nicolasfara.mktt.core.RetainNotSupported
 * @see io.github.nicolasfara.mktt.core.QoSNotSupported
 * @see io.github.nicolasfara.mktt.core.UseAnotherServer
 * @see io.github.nicolasfara.mktt.core.ServerMoved
 * @see io.github.nicolasfara.mktt.core.SharedSubscriptionsNotSupported
 * @see io.github.nicolasfara.mktt.core.ConnectionRateExceeded
 * @see io.github.nicolasfara.mktt.core.MaximumConnectTime
 * @see io.github.nicolasfara.mktt.core.SubscriptionIdentifiersNotSupported
 * @see io.github.nicolasfara.mktt.core.WildcardSubscriptionsNotSupported
 */
@ConsistentCopyVisibility
public data class ReasonCode internal constructor(val code: Int, val name: String) {

    public override fun toString(): String = "$code $name"

    public companion object {
        public fun from(
            code: Byte,
            defaultSuccessReason: io.github.nicolasfara.mktt.core.ReasonCode = _root_ide_package_.io.github.nicolasfara.mktt.core.Success,
        ): io.github.nicolasfara.mktt.core.ReasonCode {
            check(defaultSuccessReason.code == 0) {
                "The default success reason must be one of 'Success', NormalDisconnection' or 'GrantedQoS0'"
            }

            return when (code.toInt() and 0xFF) {
                0 -> defaultSuccessReason

                _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS1.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS1

                _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS2.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS2

                _root_ide_package_.io.github.nicolasfara.mktt.core.DisconnectWithWillMessage.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.DisconnectWithWillMessage

                _root_ide_package_.io.github.nicolasfara.mktt.core.NoMatchingSubscribers.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.NoMatchingSubscribers

                _root_ide_package_.io.github.nicolasfara.mktt.core.NoSubscriptionExisted.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.NoSubscriptionExisted

                _root_ide_package_.io.github.nicolasfara.mktt.core.ContinueAuthentication.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ContinueAuthentication

                _root_ide_package_.io.github.nicolasfara.mktt.core.ReAuthenticate.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ReAuthenticate

                _root_ide_package_.io.github.nicolasfara.mktt.core.UnspecifiedError.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.UnspecifiedError

                _root_ide_package_.io.github.nicolasfara.mktt.core.MalformedPacket.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.MalformedPacket

                _root_ide_package_.io.github.nicolasfara.mktt.core.ProtocolError.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ProtocolError

                _root_ide_package_.io.github.nicolasfara.mktt.core.ImplementationSpecificError.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ImplementationSpecificError

                _root_ide_package_.io.github.nicolasfara.mktt.core.UnsupportedProtocolVersion.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.UnsupportedProtocolVersion

                _root_ide_package_.io.github.nicolasfara.mktt.core.ClientIdentifierNotValid.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ClientIdentifierNotValid

                _root_ide_package_.io.github.nicolasfara.mktt.core.BadUserNameOrPassword.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.BadUserNameOrPassword

                _root_ide_package_.io.github.nicolasfara.mktt.core.NotAuthorized.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.NotAuthorized

                _root_ide_package_.io.github.nicolasfara.mktt.core.ServerUnavailable.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ServerUnavailable

                _root_ide_package_.io.github.nicolasfara.mktt.core.ServerBusy.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ServerBusy

                _root_ide_package_.io.github.nicolasfara.mktt.core.Banned.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.Banned

                _root_ide_package_.io.github.nicolasfara.mktt.core.ServerShuttingDown.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ServerShuttingDown

                _root_ide_package_.io.github.nicolasfara.mktt.core.BadAuthenticationMethod.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.BadAuthenticationMethod

                _root_ide_package_.io.github.nicolasfara.mktt.core.KeepAliveTimeout.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.KeepAliveTimeout

                _root_ide_package_.io.github.nicolasfara.mktt.core.SessionTakenOver.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.SessionTakenOver

                _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilterInvalid.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilterInvalid

                _root_ide_package_.io.github.nicolasfara.mktt.core.TopicNameInvalid.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.TopicNameInvalid

                _root_ide_package_.io.github.nicolasfara.mktt.core.PacketIdentifierInUse.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.PacketIdentifierInUse

                _root_ide_package_.io.github.nicolasfara.mktt.core.PacketIdentifierNotFound.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.PacketIdentifierNotFound

                _root_ide_package_.io.github.nicolasfara.mktt.core.ReceiveMaximumExceeded.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ReceiveMaximumExceeded

                _root_ide_package_.io.github.nicolasfara.mktt.core.TopicAliasInvalid.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.TopicAliasInvalid

                _root_ide_package_.io.github.nicolasfara.mktt.core.PacketTooLarge.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.PacketTooLarge

                _root_ide_package_.io.github.nicolasfara.mktt.core.MessageRateTooHigh.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.MessageRateTooHigh

                _root_ide_package_.io.github.nicolasfara.mktt.core.QuotaExceeded.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.QuotaExceeded

                _root_ide_package_.io.github.nicolasfara.mktt.core.AdministrativeAction.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.AdministrativeAction

                _root_ide_package_.io.github.nicolasfara.mktt.core.PayloadFormatInvalid.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.PayloadFormatInvalid

                _root_ide_package_.io.github.nicolasfara.mktt.core.RetainNotSupported.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.RetainNotSupported

                _root_ide_package_.io.github.nicolasfara.mktt.core.QoSNotSupported.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.QoSNotSupported

                _root_ide_package_.io.github.nicolasfara.mktt.core.UseAnotherServer.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.UseAnotherServer

                _root_ide_package_.io.github.nicolasfara.mktt.core.ServerMoved.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ServerMoved

                _root_ide_package_.io.github.nicolasfara.mktt.core.SharedSubscriptionsNotSupported.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.SharedSubscriptionsNotSupported

                _root_ide_package_.io.github.nicolasfara.mktt.core.ConnectionRateExceeded.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.ConnectionRateExceeded

                _root_ide_package_.io.github.nicolasfara.mktt.core.MaximumConnectTime.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.MaximumConnectTime

                _root_ide_package_.io.github.nicolasfara.mktt.core.SubscriptionIdentifiersNotSupported.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.SubscriptionIdentifiersNotSupported

                _root_ide_package_.io.github.nicolasfara.mktt.core.WildcardSubscriptionsNotSupported.code -> _root_ide_package_.io.github.nicolasfara.mktt.core.WildcardSubscriptionsNotSupported

                else -> throw _root_ide_package_.io.github.nicolasfara.mktt.core.MalformedPacketException(
                    "Unknown reason code: $code",
                )
            }
        }
    }

    public operator fun compareTo(other: io.github.nicolasfara.mktt.core.ReasonCode): Int =
        this.code.compareTo(other.code)
}

/**
 * The Success reason code
 */
public val Success: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(0, "Success")

/**
 * The NormalDisconnection reason code, only used in `DISCONNECT` packets.
 */
public val NormalDisconnection: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(0, "Normal disconnection")

/**
 * The GrantedQoS0 reason code, only used in `SUBACK` packets.
 */
public val GrantedQoS0: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(0, "Granted QoS 0")

/**
 * The GrantedQoS1 reason code.
 */
public val GrantedQoS1: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(1, "Granted QoS 1")

/**
 * The GrantedQoS2 reason code.
 */
public val GrantedQoS2: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(2, "Granted QoS 2")

/**
 * The DisconnectWithWillMessage reason code.
 */
public val DisconnectWithWillMessage: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(4, "Disconnect with Will Message")

/**
 * The NoMatchingSubscribers reason code.
 */
public val NoMatchingSubscribers: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(16, "No matching subscribers")

/**
 * The NoSubscriptionExisted reason code.
 */
public val NoSubscriptionExisted: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(17, "No subscription existed")

/**
 * The ContinueAuthentication reason code.
 */
public val ContinueAuthentication: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(24, "Continue authentication")

/**
 * The ReAuthenticate reason code.
 */
public val ReAuthenticate: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(25, "Re-authenticate")

/**
 * The UnspecifiedError reason code.
 */
public val UnspecifiedError: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(128, "Unspecified error")

/**
 * The MalformedPacket reason code.
 */
public val MalformedPacket: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(129, "Malformed Packet")

/**
 * The ProtocolError reason code.
 */
public val ProtocolError: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(130, "Protocol Error")

/**
 * The ImplementationSpecificError reason code.
 */
public val ImplementationSpecificError: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(131, "Implementation specific error")

/**
 * The UnsupportedProtocolVersion reason code.
 */
public val UnsupportedProtocolVersion: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(132, "Unsupported Protocol Version")

/**
 * The ClientIdentifierNotValid reason code.
 */
public val ClientIdentifierNotValid: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(133, "Client Identifier not valid")

/**
 * The BadUserNameOrPassword reason code.
 */
public val BadUserNameOrPassword: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(134, "Bad User Name or Password")

/**
 * The NotAuthorized reason code.
 */
public val NotAuthorized: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(135, "Not authorized")

/**
 * The ServerUnavailable reason code.
 */
public val ServerUnavailable: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(136, "Server unavailable")

/**
 * The ServerBusy reason code.
 */
public val ServerBusy: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(137, "Server busy")

/**
 * The Banned reason code.
 */
public val Banned: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(138, "Banned")

/**
 * The ServerShuttingDown reason code.
 */
public val ServerShuttingDown: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(139, "Server shutting down")

/**
 * The BadAuthenticationMethod reason code.
 */
public val BadAuthenticationMethod: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(140, "Bad authentication method")

/**
 * The KeepAliveTimeout reason code.
 */
public val KeepAliveTimeout: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(141, "Keep Alive timeout")

/**
 * The SessionTakenOver reason code.
 */
public val SessionTakenOver: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(142, "Session taken over")

/**
 * The TopicFilterInvalid reason code.
 */
public val TopicFilterInvalid: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(143, "Topic Filter invalid")

/**
 * The TopicNameInvalid reason code.
 */
public val TopicNameInvalid: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(144, "Topic Name invalid")

/**
 * The PacketIdentifierInUse reason code.
 */
public val PacketIdentifierInUse: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(145, "Packet Identifier in use")

/**
 * The PacketIdentifierNotFound reason code.
 */
public val PacketIdentifierNotFound: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(146, "Packet Identifier not found")

/**
 * The ReceiveMaximumExceeded reason code.
 */
public val ReceiveMaximumExceeded: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(147, "Receive Maximum exceeded")

/**
 * The TopicAliasInvalid reason code.
 */
public val TopicAliasInvalid: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(148, "Topic Alias invalid")

/**
 * The PacketTooLarge reason code.
 */
public val PacketTooLarge: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(149, "Packet too large")

/**
 * The MessageRateTooHigh reason code.
 */
public val MessageRateTooHigh: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(150, "Message rate too high")

/**
 * The QuotaExceeded reason code.
 */
public val QuotaExceeded: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(151, "Quota exceeded")

/**
 * The AdministrativeAction reason code.
 */
public val AdministrativeAction: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(152, "Administrative action")

/**
 * The AdministrativeAction reason code.
 */
public val PayloadFormatInvalid: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(153, "Payload format invalid")

/**
 * The RetainNotSupported reason code.
 */
public val RetainNotSupported: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(154, "Retain not supported")

/**
 * The QoSNotSupported reason code.
 */
public val QoSNotSupported: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(155, "QoS not supported")

/**
 * The UseAnotherServer reason code.
 */
public val UseAnotherServer: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(156, "Use another server")

/**
 * The ServerMoved reason code.
 */
public val ServerMoved: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(157, "Server moved")

/**
 * The SharedSubscriptionsNotSupported reason code.
 */
public val SharedSubscriptionsNotSupported: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(158, "Shared Subscriptions not supported")

/**
 * The ConnectionRateExceeded reason code.
 */
public val ConnectionRateExceeded: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(159, "Connection rate exceeded")

/**
 * The MaximumConnectTime reason code.
 */
public val MaximumConnectTime: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(160, "Maximum connect time")

/**
 * The SubscriptionIdentifiersNotSupported reason code.
 */
public val SubscriptionIdentifiersNotSupported: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(161, "Subscription Identifiers not supported")

/**
 * The WildcardSubscriptionsNotSupported reason code.
 */
public val WildcardSubscriptionsNotSupported: io.github.nicolasfara.mktt.core.ReasonCode =
    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode(162, "Wildcard Subscriptions not supported")
