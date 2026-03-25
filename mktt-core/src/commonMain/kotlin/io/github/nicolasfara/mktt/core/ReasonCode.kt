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

    /** Factory methods for converting wire reason codes. */
    companion object {
        /**
         * Converts a wire-encoded reason code to [ReasonCode].
         */
        fun from(code: Byte, defaultSuccessReason: ReasonCode = Success): ReasonCode {
            check(defaultSuccessReason.code == 0) {
                "The default success reason must be one of 'Success', NormalDisconnection' or 'GrantedQoS0'"
            }

            return when (code.toInt() and REASON_CODE_BYTE_MASK) {
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

    /** Compares this reason code numerically with [other]. */
    operator fun compareTo(other: ReasonCode): Int = this.code.compareTo(other.code)
}

private const val REASON_CODE_BYTE_MASK = 0xFF
private const val REASON_CODE_DISCONNECT_WITH_WILL_MESSAGE = 4
private const val REASON_CODE_NO_MATCHING_SUBSCRIBERS = 16
private const val REASON_CODE_NO_SUBSCRIPTION_EXISTED = 17
private const val REASON_CODE_CONTINUE_AUTHENTICATION = 24
private const val REASON_CODE_RE_AUTHENTICATE = 25
private const val REASON_CODE_UNSPECIFIED_ERROR = 128
private const val REASON_CODE_MALFORMED_PACKET = 129
private const val REASON_CODE_PROTOCOL_ERROR = 130
private const val REASON_CODE_IMPLEMENTATION_SPECIFIC_ERROR = 131
private const val REASON_CODE_UNSUPPORTED_PROTOCOL_VERSION = 132
private const val REASON_CODE_CLIENT_IDENTIFIER_NOT_VALID = 133
private const val REASON_CODE_BAD_USERNAME_OR_PASSWORD = 134
private const val REASON_CODE_NOT_AUTHORIZED = 135
private const val REASON_CODE_SERVER_UNAVAILABLE = 136
private const val REASON_CODE_SERVER_BUSY = 137
private const val REASON_CODE_BANNED = 138
private const val REASON_CODE_SERVER_SHUTTING_DOWN = 139
private const val REASON_CODE_BAD_AUTHENTICATION_METHOD = 140
private const val REASON_CODE_KEEP_ALIVE_TIMEOUT = 141
private const val REASON_CODE_SESSION_TAKEN_OVER = 142
private const val REASON_CODE_TOPIC_FILTER_INVALID = 143
private const val REASON_CODE_TOPIC_NAME_INVALID = 144
private const val REASON_CODE_PACKET_IDENTIFIER_IN_USE = 145
private const val REASON_CODE_PACKET_IDENTIFIER_NOT_FOUND = 146
private const val REASON_CODE_RECEIVE_MAXIMUM_EXCEEDED = 147
private const val REASON_CODE_TOPIC_ALIAS_INVALID = 148
private const val REASON_CODE_PACKET_TOO_LARGE = 149
private const val REASON_CODE_MESSAGE_RATE_TOO_HIGH = 150
private const val REASON_CODE_QUOTA_EXCEEDED = 151
private const val REASON_CODE_ADMINISTRATIVE_ACTION = 152
private const val REASON_CODE_PAYLOAD_FORMAT_INVALID = 153
private const val REASON_CODE_RETAIN_NOT_SUPPORTED = 154
private const val REASON_CODE_QOS_NOT_SUPPORTED = 155
private const val REASON_CODE_USE_ANOTHER_SERVER = 156

/**
 * The Success reason code.
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
    ReasonCode(REASON_CODE_DISCONNECT_WITH_WILL_MESSAGE, "Disconnect with Will Message")

/**
 * The NoMatchingSubscribers reason code.
 */
val NoMatchingSubscribers: ReasonCode =
    ReasonCode(REASON_CODE_NO_MATCHING_SUBSCRIBERS, "No matching subscribers")

/**
 * The NoSubscriptionExisted reason code.
 */
val NoSubscriptionExisted: ReasonCode =
    ReasonCode(REASON_CODE_NO_SUBSCRIPTION_EXISTED, "No subscription existed")

/**
 * The ContinueAuthentication reason code.
 */
val ContinueAuthentication: ReasonCode =
    ReasonCode(REASON_CODE_CONTINUE_AUTHENTICATION, "Continue authentication")

/**
 * The ReAuthenticate reason code.
 */
val ReAuthenticate: ReasonCode =
    ReasonCode(REASON_CODE_RE_AUTHENTICATE, "Re-authenticate")

/**
 * The UnspecifiedError reason code.
 */
val UnspecifiedError: ReasonCode =
    ReasonCode(REASON_CODE_UNSPECIFIED_ERROR, "Unspecified error")

/**
 * The MalformedPacket reason code.
 */
val MalformedPacket: ReasonCode =
    ReasonCode(REASON_CODE_MALFORMED_PACKET, "Malformed Packet")

/**
 * The ProtocolError reason code.
 */
val ProtocolError: ReasonCode =
    ReasonCode(REASON_CODE_PROTOCOL_ERROR, "Protocol Error")

/**
 * The ImplementationSpecificError reason code.
 */
val ImplementationSpecificError: ReasonCode =
    ReasonCode(REASON_CODE_IMPLEMENTATION_SPECIFIC_ERROR, "Implementation specific error")

/**
 * The UnsupportedProtocolVersion reason code.
 */
val UnsupportedProtocolVersion: ReasonCode =
    ReasonCode(REASON_CODE_UNSUPPORTED_PROTOCOL_VERSION, "Unsupported Protocol Version")

/**
 * The ClientIdentifierNotValid reason code.
 */
val ClientIdentifierNotValid: ReasonCode =
    ReasonCode(REASON_CODE_CLIENT_IDENTIFIER_NOT_VALID, "Client Identifier not valid")

/**
 * The BadUserNameOrPassword reason code.
 */
val BadUserNameOrPassword: ReasonCode =
    ReasonCode(REASON_CODE_BAD_USERNAME_OR_PASSWORD, "Bad User Name or Password")

/**
 * The NotAuthorized reason code.
 */
val NotAuthorized: ReasonCode =
    ReasonCode(REASON_CODE_NOT_AUTHORIZED, "Not authorized")

/**
 * The ServerUnavailable reason code.
 */
val ServerUnavailable: ReasonCode =
    ReasonCode(REASON_CODE_SERVER_UNAVAILABLE, "Server unavailable")

/**
 * The ServerBusy reason code.
 */
val ServerBusy: ReasonCode =
    ReasonCode(REASON_CODE_SERVER_BUSY, "Server busy")

/**
 * The Banned reason code.
 */
val Banned: ReasonCode =
    ReasonCode(REASON_CODE_BANNED, "Banned")

/**
 * The ServerShuttingDown reason code.
 */
val ServerShuttingDown: ReasonCode =
    ReasonCode(REASON_CODE_SERVER_SHUTTING_DOWN, "Server shutting down")

/**
 * The BadAuthenticationMethod reason code.
 */
val BadAuthenticationMethod: ReasonCode =
    ReasonCode(REASON_CODE_BAD_AUTHENTICATION_METHOD, "Bad authentication method")

/**
 * The KeepAliveTimeout reason code.
 */
val KeepAliveTimeout: ReasonCode =
    ReasonCode(REASON_CODE_KEEP_ALIVE_TIMEOUT, "Keep Alive timeout")

/**
 * The SessionTakenOver reason code.
 */
val SessionTakenOver: ReasonCode =
    ReasonCode(REASON_CODE_SESSION_TAKEN_OVER, "Session taken over")

/**
 * The TopicFilterInvalid reason code.
 */
val TopicFilterInvalid: ReasonCode =
    ReasonCode(REASON_CODE_TOPIC_FILTER_INVALID, "Topic Filter invalid")

/**
 * The TopicNameInvalid reason code.
 */
val TopicNameInvalid: ReasonCode =
    ReasonCode(REASON_CODE_TOPIC_NAME_INVALID, "Topic Name invalid")

/**
 * The PacketIdentifierInUse reason code.
 */
val PacketIdentifierInUse: ReasonCode =
    ReasonCode(REASON_CODE_PACKET_IDENTIFIER_IN_USE, "Packet Identifier in use")

/**
 * The PacketIdentifierNotFound reason code.
 */
val PacketIdentifierNotFound: ReasonCode =
    ReasonCode(REASON_CODE_PACKET_IDENTIFIER_NOT_FOUND, "Packet Identifier not found")

/**
 * The ReceiveMaximumExceeded reason code.
 */
val ReceiveMaximumExceeded: ReasonCode =
    ReasonCode(REASON_CODE_RECEIVE_MAXIMUM_EXCEEDED, "Receive Maximum exceeded")

/**
 * The TopicAliasInvalid reason code.
 */
val TopicAliasInvalid: ReasonCode =
    ReasonCode(REASON_CODE_TOPIC_ALIAS_INVALID, "Topic Alias invalid")

/**
 * The PacketTooLarge reason code.
 */
val PacketTooLarge: ReasonCode =
    ReasonCode(REASON_CODE_PACKET_TOO_LARGE, "Packet too large")

/**
 * The MessageRateTooHigh reason code.
 */
val MessageRateTooHigh: ReasonCode =
    ReasonCode(REASON_CODE_MESSAGE_RATE_TOO_HIGH, "Message rate too high")

/**
 * The QuotaExceeded reason code.
 */
val QuotaExceeded: ReasonCode =
    ReasonCode(REASON_CODE_QUOTA_EXCEEDED, "Quota exceeded")

/**
 * The AdministrativeAction reason code.
 */
val AdministrativeAction: ReasonCode =
    ReasonCode(REASON_CODE_ADMINISTRATIVE_ACTION, "Administrative action")

/**
 * The AdministrativeAction reason code.
 */
val PayloadFormatInvalid: ReasonCode =
    ReasonCode(REASON_CODE_PAYLOAD_FORMAT_INVALID, "Payload format invalid")

/**
 * The RetainNotSupported reason code.
 */
val RetainNotSupported: ReasonCode =
    ReasonCode(REASON_CODE_RETAIN_NOT_SUPPORTED, "Retain not supported")

/**
 * The QoSNotSupported reason code.
 */
val QoSNotSupported: ReasonCode =
    ReasonCode(REASON_CODE_QOS_NOT_SUPPORTED, "QoS not supported")

/**
 * The UseAnotherServer reason code.
 */
val UseAnotherServer: ReasonCode =
    ReasonCode(REASON_CODE_USE_ANOTHER_SERVER, "Use another server")

private const val REASON_CODE_SERVER_MOVED = 157
private const val REASON_CODE_SHARED_SUBSCRIPTIONS_NOT_SUPPORTED = 158
private const val REASON_CODE_CONNECTION_RATE_EXCEEDED = 159
private const val REASON_CODE_MAXIMUM_CONNECT_TIME = 160
private const val REASON_CODE_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED = 161
private const val REASON_CODE_WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED = 162

/**
 * The ServerMoved reason code.
 */
val ServerMoved: ReasonCode =
    ReasonCode(REASON_CODE_SERVER_MOVED, "Server moved")

/**
 * The SharedSubscriptionsNotSupported reason code.
 */
val SharedSubscriptionsNotSupported: ReasonCode =
    ReasonCode(REASON_CODE_SHARED_SUBSCRIPTIONS_NOT_SUPPORTED, "Shared Subscriptions not supported")

/**
 * The ConnectionRateExceeded reason code.
 */
val ConnectionRateExceeded: ReasonCode =
    ReasonCode(REASON_CODE_CONNECTION_RATE_EXCEEDED, "Connection rate exceeded")

/**
 * The MaximumConnectTime reason code.
 */
val MaximumConnectTime: ReasonCode =
    ReasonCode(REASON_CODE_MAXIMUM_CONNECT_TIME, "Maximum connect time")

/**
 * The SubscriptionIdentifiersNotSupported reason code.
 */
val SubscriptionIdentifiersNotSupported: ReasonCode =
    ReasonCode(REASON_CODE_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED, "Subscription Identifiers not supported")

/**
 * The WildcardSubscriptionsNotSupported reason code.
 */
val WildcardSubscriptionsNotSupported: ReasonCode =
    ReasonCode(REASON_CODE_WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED, "Wildcard Subscriptions not supported")
