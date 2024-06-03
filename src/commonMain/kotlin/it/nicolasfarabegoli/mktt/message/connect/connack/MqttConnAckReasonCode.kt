package it.nicolasfarabegoli.mktt.message.connect.connack

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode

sealed interface MqttConnAckReasonCode {
    val code: Byte

    companion object {
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

    data object Success : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    data object UnspecifiedError : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.UnspecifiedError.code
    }

    data object MalformedPacket : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.MalformedPacket.code
    }

    data object ProtocolError : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ProtocolError.code
    }

    data object ImplementationSpecificError : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ImplementationSpecificError.code
    }

    data object UnsupportedProtocolVersion : MqttConnAckReasonCode {
        override val code: Byte = 0x84.toByte()
    }

    data object ClientIdentifierNotValid : MqttConnAckReasonCode {
        override val code: Byte = 0x85.toByte()
    }

    data object BadUserNameOrPassword : MqttConnAckReasonCode {
        override val code: Byte = 0x86.toByte()
    }

    data object NotAuthorized : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.NotAuthorized.code
    }

    data object ServerUnavailable : MqttConnAckReasonCode {
        override val code: Byte = 0x88.toByte()
    }

    data object ServerBusy : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ServerBusy.code
    }

    data object Banned : MqttConnAckReasonCode {
        override val code: Byte = 0x8A.toByte()
    }

    data object BadAuthenticationMethod : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.BadAuthenticationMethod.code
    }

    data object TopicNameInvalid : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.TopicNameInvalid.code
    }

    data object PacketTooLarge : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketTooLarge.code
    }

    data object QuotaExceeded : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.QuotaExceeded.code
    }

    data object PayloadFormatInvalid : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PayloadFormatInvalid.code
    }

    data object RetainNotSupported : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.RetainNotSupported.code
    }

    data object QoSNotSupported : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.QoSNotSupported.code
    }

    data object UseAnotherServer : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.UseAnotherServer.code
    }

    data object ServerMoved : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ServerMoved.code
    }

    data object ConnectionRateExceeded : MqttConnAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ConnectionRateExceeded.code
    }
}
