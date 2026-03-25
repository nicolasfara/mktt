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
data class ReasonCode internal constructor(val code: Int, val name: String) {

    override fun toString(): String = "$code $name"

    companion object {
        fun from(code: Byte, defaultSuccessReason: ReasonCode = Success): ReasonCode {
            check(defaultSuccessReason.code == 0) {
                "The default success reason must be one of 'Success', NormalDisconnection' or 'GrantedQoS0'"
            }

            return when (code.toInt() and 0xFF) {
                0 -> defaultSuccessReason

                GrantedQoS1.code -> GrantedQoS1

                GrantedQoS2.code -> GrantedQoS2

                DisconnectWithWillMessage.code -> DisconnectWithWillMessage

                NoMatchingSubscribers.code -> NoMatchingSubscribers

                NoSubscriptionExisted.code -> NoSubscriptionExisted

                ContinueAuthentication.code -> ContinueAuthentication

                ReAuthenticate.code -> ReAuthenticate

                UnspecifiedError.code -> UnspecifiedError

                MalformedPacket.code -> MalformedPacket

                ProtocolError.code -> ProtocolError

                ImplementationSpecificError.code -> ImplementationSpecificError

                UnsupportedProtocolVersion.code -> UnsupportedProtocolVersion

                ClientIdentifierNotValid.code -> ClientIdentifierNotValid

                BadUserNameOrPassword.code -> BadUserNameOrPassword

                NotAuthorized.code -> NotAuthorized

                ServerUnavailable.code -> ServerUnavailable

                ServerBusy.code -> ServerBusy

                Banned.code -> Banned

                ServerShuttingDown.code -> ServerShuttingDown

                BadAuthenticationMethod.code -> BadAuthenticationMethod

                KeepAliveTimeout.code -> KeepAliveTimeout

                SessionTakenOver.code -> SessionTakenOver

                TopicFilterInvalid.code -> TopicFilterInvalid

                TopicNameInvalid.code -> TopicNameInvalid

                PacketIdentifierInUse.code -> PacketIdentifierInUse

                PacketIdentifierNotFound.code -> PacketIdentifierNotFound

                ReceiveMaximumExceeded.code -> ReceiveMaximumExceeded

                TopicAliasInvalid.code -> TopicAliasInvalid

                PacketTooLarge.code -> PacketTooLarge

                MessageRateTooHigh.code -> MessageRateTooHigh

                QuotaExceeded.code -> QuotaExceeded

                AdministrativeAction.code -> AdministrativeAction

                PayloadFormatInvalid.code -> PayloadFormatInvalid

                RetainNotSupported.code -> RetainNotSupported

                QoSNotSupported.code -> QoSNotSupported

                UseAnotherServer.code -> UseAnotherServer

                ServerMoved.code -> ServerMoved

                SharedSubscriptionsNotSupported.code -> SharedSubscriptionsNotSupported

                ConnectionRateExceeded.code -> ConnectionRateExceeded

                MaximumConnectTime.code -> MaximumConnectTime

                SubscriptionIdentifiersNotSupported.code -> SubscriptionIdentifiersNotSupported

                WildcardSubscriptionsNotSupported.code -> WildcardSubscriptionsNotSupported

                else -> throw MalformedPacketException(
                    "Unknown reason code: $code",
                )
            }
        }
    }

    operator fun compareTo(other: ReasonCode): Int = this.code.compareTo(other.code)
}

/**
 * The Success reason code
 */
val Success: ReasonCode =
    ReasonCode(0, "Success")

/**
 * The NormalDisconnection reason code, only used in `DISCONNECT` packets.
 */
val NormalDisconnection: ReasonCode =
    ReasonCode(0, "Normal disconnection")

/**
 * The GrantedQoS0 reason code, only used in `SUBACK` packets.
 */
val GrantedQoS0: ReasonCode =
    ReasonCode(0, "Granted QoS 0")

/**
 * The GrantedQoS1 reason code.
 */
val GrantedQoS1: ReasonCode =
    ReasonCode(1, "Granted QoS 1")

/**
 * The GrantedQoS2 reason code.
 */
val GrantedQoS2: ReasonCode =
    ReasonCode(2, "Granted QoS 2")

/**
 * The DisconnectWithWillMessage reason code.
 */
val DisconnectWithWillMessage: ReasonCode =
    ReasonCode(4, "Disconnect with Will Message")

/**
 * The NoMatchingSubscribers reason code.
 */
val NoMatchingSubscribers: ReasonCode =
    ReasonCode(16, "No matching subscribers")

/**
 * The NoSubscriptionExisted reason code.
 */
val NoSubscriptionExisted: ReasonCode =
    ReasonCode(17, "No subscription existed")

/**
 * The ContinueAuthentication reason code.
 */
val ContinueAuthentication: ReasonCode =
    ReasonCode(24, "Continue authentication")

/**
 * The ReAuthenticate reason code.
 */
val ReAuthenticate: ReasonCode =
    ReasonCode(25, "Re-authenticate")

/**
 * The UnspecifiedError reason code.
 */
val UnspecifiedError: ReasonCode =
    ReasonCode(128, "Unspecified error")

/**
 * The MalformedPacket reason code.
 */
val MalformedPacket: ReasonCode =
    ReasonCode(129, "Malformed Packet")

/**
 * The ProtocolError reason code.
 */
val ProtocolError: ReasonCode =
    ReasonCode(130, "Protocol Error")

/**
 * The ImplementationSpecificError reason code.
 */
val ImplementationSpecificError: ReasonCode =
    ReasonCode(131, "Implementation specific error")

/**
 * The UnsupportedProtocolVersion reason code.
 */
val UnsupportedProtocolVersion: ReasonCode =
    ReasonCode(132, "Unsupported Protocol Version")

/**
 * The ClientIdentifierNotValid reason code.
 */
val ClientIdentifierNotValid: ReasonCode =
    ReasonCode(133, "Client Identifier not valid")

/**
 * The BadUserNameOrPassword reason code.
 */
val BadUserNameOrPassword: ReasonCode =
    ReasonCode(134, "Bad User Name or Password")

/**
 * The NotAuthorized reason code.
 */
val NotAuthorized: ReasonCode =
    ReasonCode(135, "Not authorized")

/**
 * The ServerUnavailable reason code.
 */
val ServerUnavailable: ReasonCode =
    ReasonCode(136, "Server unavailable")

/**
 * The ServerBusy reason code.
 */
val ServerBusy: ReasonCode =
    ReasonCode(137, "Server busy")

/**
 * The Banned reason code.
 */
val Banned: ReasonCode =
    ReasonCode(138, "Banned")

/**
 * The ServerShuttingDown reason code.
 */
val ServerShuttingDown: ReasonCode =
    ReasonCode(139, "Server shutting down")

/**
 * The BadAuthenticationMethod reason code.
 */
val BadAuthenticationMethod: ReasonCode =
    ReasonCode(140, "Bad authentication method")

/**
 * The KeepAliveTimeout reason code.
 */
val KeepAliveTimeout: ReasonCode =
    ReasonCode(141, "Keep Alive timeout")

/**
 * The SessionTakenOver reason code.
 */
val SessionTakenOver: ReasonCode =
    ReasonCode(142, "Session taken over")

/**
 * The TopicFilterInvalid reason code.
 */
val TopicFilterInvalid: ReasonCode =
    ReasonCode(143, "Topic Filter invalid")

/**
 * The TopicNameInvalid reason code.
 */
val TopicNameInvalid: ReasonCode =
    ReasonCode(144, "Topic Name invalid")

/**
 * The PacketIdentifierInUse reason code.
 */
val PacketIdentifierInUse: ReasonCode =
    ReasonCode(145, "Packet Identifier in use")

/**
 * The PacketIdentifierNotFound reason code.
 */
val PacketIdentifierNotFound: ReasonCode =
    ReasonCode(146, "Packet Identifier not found")

/**
 * The ReceiveMaximumExceeded reason code.
 */
val ReceiveMaximumExceeded: ReasonCode =
    ReasonCode(147, "Receive Maximum exceeded")

/**
 * The TopicAliasInvalid reason code.
 */
val TopicAliasInvalid: ReasonCode =
    ReasonCode(148, "Topic Alias invalid")

/**
 * The PacketTooLarge reason code.
 */
val PacketTooLarge: ReasonCode =
    ReasonCode(149, "Packet too large")

/**
 * The MessageRateTooHigh reason code.
 */
val MessageRateTooHigh: ReasonCode =
    ReasonCode(150, "Message rate too high")

/**
 * The QuotaExceeded reason code.
 */
val QuotaExceeded: ReasonCode =
    ReasonCode(151, "Quota exceeded")

/**
 * The AdministrativeAction reason code.
 */
val AdministrativeAction: ReasonCode =
    ReasonCode(152, "Administrative action")

/**
 * The AdministrativeAction reason code.
 */
val PayloadFormatInvalid: ReasonCode =
    ReasonCode(153, "Payload format invalid")

/**
 * The RetainNotSupported reason code.
 */
val RetainNotSupported: ReasonCode =
    ReasonCode(154, "Retain not supported")

/**
 * The QoSNotSupported reason code.
 */
val QoSNotSupported: ReasonCode =
    ReasonCode(155, "QoS not supported")

/**
 * The UseAnotherServer reason code.
 */
val UseAnotherServer: ReasonCode =
    ReasonCode(156, "Use another server")

/**
 * The ServerMoved reason code.
 */
val ServerMoved: ReasonCode =
    ReasonCode(157, "Server moved")

/**
 * The SharedSubscriptionsNotSupported reason code.
 */
val SharedSubscriptionsNotSupported: ReasonCode =
    ReasonCode(158, "Shared Subscriptions not supported")

/**
 * The ConnectionRateExceeded reason code.
 */
val ConnectionRateExceeded: ReasonCode =
    ReasonCode(159, "Connection rate exceeded")

/**
 * The MaximumConnectTime reason code.
 */
val MaximumConnectTime: ReasonCode =
    ReasonCode(160, "Maximum connect time")

/**
 * The SubscriptionIdentifiersNotSupported reason code.
 */
val SubscriptionIdentifiersNotSupported: ReasonCode =
    ReasonCode(161, "Subscription Identifiers not supported")

/**
 * The WildcardSubscriptionsNotSupported reason code.
 */
val WildcardSubscriptionsNotSupported: ReasonCode =
    ReasonCode(162, "Wildcard Subscriptions not supported")
