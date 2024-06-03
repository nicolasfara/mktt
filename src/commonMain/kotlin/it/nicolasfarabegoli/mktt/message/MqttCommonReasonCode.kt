package it.nicolasfarabegoli.mktt.message

sealed interface MqttCommonReasonCode {
    val code: Byte

    companion object {
        fun fromCode(code: Byte): MqttCommonReasonCode {
            return when (code) {
                0x00.toByte() -> Success
                0x10.toByte() -> NoMatchingSubscribers
                0x80.toByte() -> UnspecifiedError
                0x81.toByte() -> MalformedPacket
                0x82.toByte() -> ProtocolError
                0x83.toByte() -> ImplementationSpecificError
                0x87.toByte() -> NotAuthorized
                0x89.toByte() -> ServerBusy
                0x8C.toByte() -> BadAuthenticationMethod
                0x8F.toByte() -> TopicFilterInvalid
                0x90.toByte() -> TopicNameInvalid
                0x91.toByte() -> PacketIdentifierInUse
                0x92.toByte() -> PacketIdentifierNotFound
                0x95.toByte() -> PacketTooLarge
                0x97.toByte() -> QuotaExceeded
                0x99.toByte() -> PayloadFormatInvalid
                0x9A.toByte() -> RetainNotSupported
                0x9B.toByte() -> QoSNotSupported
                0x9C.toByte() -> UseAnotherServer
                0x9D.toByte() -> ServerMoved
                0x9E.toByte() -> SharedSubscriptionsNotSupported
                0x9F.toByte() -> ConnectionRateExceeded
                0xA1.toByte() -> SubscriptionIdentifiersNotSupported
                0xA2.toByte() -> WildcardSubscriptionsNotSupported
                else -> error("Unknown Common Reason Code: $code")
            }
        }
    }

    data object Success : MqttCommonReasonCode {
        override val code: Byte = 0x00.toByte()
    }

    data object NoMatchingSubscribers : MqttCommonReasonCode {
        override val code: Byte = 0x10.toByte()
    }

    data object UnspecifiedError : MqttCommonReasonCode {
        override val code: Byte = 0x80.toByte()
    }

    data object MalformedPacket : MqttCommonReasonCode {
        override val code: Byte = 0x81.toByte()
    }

    data object ProtocolError : MqttCommonReasonCode {
        override val code: Byte = 0x82.toByte()
    }

    data object ImplementationSpecificError : MqttCommonReasonCode {
        override val code: Byte = 0x83.toByte()
    }

    data object NotAuthorized : MqttCommonReasonCode {
        override val code: Byte = 0x87.toByte()
    }

    data object ServerBusy : MqttCommonReasonCode {
        override val code: Byte = 0x89.toByte()
    }

    data object BadAuthenticationMethod : MqttCommonReasonCode {
        override val code: Byte = 0x8C.toByte()
    }

    data object TopicFilterInvalid : MqttCommonReasonCode {
        override val code: Byte = 0x8F.toByte()
    }

    data object TopicNameInvalid : MqttCommonReasonCode {
        override val code: Byte = 0x90.toByte()
    }

    data object PacketIdentifierInUse : MqttCommonReasonCode {
        override val code: Byte = 0x91.toByte()
    }

    data object PacketIdentifierNotFound : MqttCommonReasonCode {
        override val code: Byte = 0x92.toByte()
    }

    data object PacketTooLarge : MqttCommonReasonCode {
        override val code: Byte = 0x95.toByte()
    }

    data object QuotaExceeded : MqttCommonReasonCode {
        override val code: Byte = 0x97.toByte()
    }

    data object PayloadFormatInvalid : MqttCommonReasonCode {
        override val code: Byte = 0x99.toByte()
    }

    data object RetainNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0x9A.toByte()
    }

    data object QoSNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0x9B.toByte()
    }

    data object UseAnotherServer : MqttCommonReasonCode {
        override val code: Byte = 0x9C.toByte()
    }

    data object ServerMoved : MqttCommonReasonCode {
        override val code: Byte = 0x9D.toByte()
    }

    data object SharedSubscriptionsNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0x9E.toByte()
    }

    data object ConnectionRateExceeded : MqttCommonReasonCode {
        override val code: Byte = 0x9F.toByte()
    }

    data object SubscriptionIdentifiersNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0xA1.toByte()
    }

    data object WildcardSubscriptionsNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0xA2.toByte()
    }
}
