package it.nicolasfarabegoli.mktt.message.connect.connack

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents a CONNACK message with a specific [reasonCode] and [reasonString].
 */
sealed interface MqttConnAckReasonCode : MqttReasonCode {
    override val code: Byte

    /**
     * The CONNACK Reason Code representing a successful connection.
     */
    companion object {
        /**
         * Returns the [MqttConnAckReasonCode] from the given [code].
         */
        @Suppress("CyclomaticComplexMethod")
        fun fromCode(code: Byte): MqttConnAckReasonCode {
            return when (code) {
                0x00.toByte() -> Success
                0x80.toByte() -> UnspecifiedError
                0x81.toByte() -> MalformedPacket
                0x82.toByte() -> ProtocolError
                0x83.toByte() -> ImplementationSpecificError
                0x84.toByte() -> UnsupportedProtocolVersion
                0x85.toByte() -> ClientIdentifierNotValid
                0x86.toByte() -> BadUserNameOrPassword
                0x87.toByte() -> NotAuthorized
                0x88.toByte() -> ServerUnavailable
                0x89.toByte() -> ServerBusy
                0x8A.toByte() -> Banned
                0x8C.toByte() -> BadAuthenticationMethod
                0x8F.toByte() -> TopicNameInvalid
                0x95.toByte() -> PacketTooLarge
                0x97.toByte() -> QuotaExceeded
                0x99.toByte() -> PayloadFormatInvalid
                0x9A.toByte() -> RetainNotSupported
                0x9B.toByte() -> QoSNotSupported
                0x9C.toByte() -> UseAnotherServer
                0x9D.toByte() -> ServerMoved
                0x9F.toByte() -> ConnectionRateExceeded
                else -> error("Unknown ConnAck Reason Code: $code")
            }
        }
    }

    /**
     * The success reason code.
     */
    data object Success : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    /**
     * The unspecified error reason code.
     */
    data object UnspecifiedError : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.UnspecifiedError.code
    }

    /**
     * The malformed packet reason code.
     */
    data object MalformedPacket : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.MalformedPacket.code
    }

    /**
     * The protocol error reason code.
     */
    data object ProtocolError : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ProtocolError.code
    }

    /**
     * The implementation specific error reason code.
     */
    data object ImplementationSpecificError : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ImplementationSpecificError.code
    }

    /**
     * The unsupported protocol version reason code.
     */
    data object UnsupportedProtocolVersion : MqttConnAckReasonCode {
        override val code: Byte = 0x84.toByte()
    }

    /**
     * The client identifier not valid reason code.
     */
    data object ClientIdentifierNotValid : MqttConnAckReasonCode {
        override val code: Byte = 0x85.toByte()
    }

    /**
     * The bad user name or password reason code.
     */
    data object BadUserNameOrPassword : MqttConnAckReasonCode {
        override val code: Byte = 0x86.toByte()
    }

    /**
     * The not authorized reason code.
     */
    data object NotAuthorized : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.NotAuthorized.code
    }

    /**
     * The server unavailable reason code.
     */
    data object ServerUnavailable : MqttConnAckReasonCode {
        override val code: Byte = 0x88.toByte()
    }

    /**
     * The server busy reason code.
     */
    data object ServerBusy : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ServerBusy.code
    }

    /**
     * The banned reason code.
     */
    data object Banned : MqttConnAckReasonCode {
        override val code: Byte = 0x8A.toByte()
    }

    /**
     * The bad authentication method reason code.
     */
    data object BadAuthenticationMethod : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.BadAuthenticationMethod.code
    }

    /**
     * The topic name invalid reason code.
     */
    data object TopicNameInvalid : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.TopicNameInvalid.code
    }

    /**
     * The packet too large reason code.
     */
    data object PacketTooLarge : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketTooLarge.code
    }

    /**
     * The quota exceeded reason code.
     */
    data object QuotaExceeded : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.QuotaExceeded.code
    }

    /**
     * The payload format invalid reason code.
     */
    data object PayloadFormatInvalid : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PayloadFormatInvalid.code
    }

    /**
     * The retain not supported reason code.
     */
    data object RetainNotSupported : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.RetainNotSupported.code
    }

    /**
     * The QoS not supported reason code.
     */
    data object QoSNotSupported : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.QoSNotSupported.code
    }

    /**
     * The use another server reason code.
     */
    data object UseAnotherServer : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.UseAnotherServer.code
    }

    /**
     * The server moved reason code.
     */
    data object ServerMoved : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ServerMoved.code
    }

    /**
     * The connection rate exceeded reason code.
     */
    data object ConnectionRateExceeded : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ConnectionRateExceeded.code
    }
}
