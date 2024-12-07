package it.nicolasfarabegoli.mktt.message

/**
 * Represents the common reason codes.
 */
sealed interface MqttCommonReasonCode {
    /**
     * The byte code of the [MqttCommonReasonCode].
     */
    val code: Byte

    companion object {
        /**
         * Returns the [MqttCommonReasonCode] from the given [code].
         */
        @Suppress("CyclomaticComplexMethod")
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

    /**
     * The success reason code.
     */
    data object Success : MqttCommonReasonCode {
        override val code: Byte = 0x00.toByte()
    }

    /**
     * The no matching subscribers reason code.
     */
    data object NoMatchingSubscribers : MqttCommonReasonCode {
        override val code: Byte = 0x10.toByte()
    }

    /**
     * The unspecified error reason code.
     */
    data object UnspecifiedError : MqttCommonReasonCode {
        override val code: Byte = 0x80.toByte()
    }

    /**
     * The malformed packet reason code.
     */
    data object MalformedPacket : MqttCommonReasonCode {
        override val code: Byte = 0x81.toByte()
    }

    /**
     * The protocol error reason code.
     */
    data object ProtocolError : MqttCommonReasonCode {
        override val code: Byte = 0x82.toByte()
    }

    /**
     * The implementation specific error reason code.
     */
    data object ImplementationSpecificError : MqttCommonReasonCode {
        override val code: Byte = 0x83.toByte()
    }

    /**
     * The not authorized reason code.
     */
    data object NotAuthorized : MqttCommonReasonCode {
        override val code: Byte = 0x87.toByte()
    }

    /**
     * The server busy reason code.
     */
    data object ServerBusy : MqttCommonReasonCode {
        override val code: Byte = 0x89.toByte()
    }

    /**
     * The bad authentication method reason code.
     */
    data object BadAuthenticationMethod : MqttCommonReasonCode {
        override val code: Byte = 0x8C.toByte()
    }

    /**
     * The topic filter invalid reason code.
     */
    data object TopicFilterInvalid : MqttCommonReasonCode {
        override val code: Byte = 0x8F.toByte()
    }

    /**
     * The topic name invalid reason code.
     */
    data object TopicNameInvalid : MqttCommonReasonCode {
        override val code: Byte = 0x90.toByte()
    }

    /**
     * The packet identifier in use reason code.
     */
    data object PacketIdentifierInUse : MqttCommonReasonCode {
        override val code: Byte = 0x91.toByte()
    }

    /**
     * The packet identifier not found reason code.
     */
    data object PacketIdentifierNotFound : MqttCommonReasonCode {
        override val code: Byte = 0x92.toByte()
    }

    /**
     * The packet too large reason code.
     */
    data object PacketTooLarge : MqttCommonReasonCode {
        override val code: Byte = 0x95.toByte()
    }

    /**
     * The quota exceeded reason code.
     */
    data object QuotaExceeded : MqttCommonReasonCode {
        override val code: Byte = 0x97.toByte()
    }

    /**
     * The payload format invalid reason code.
     */
    data object PayloadFormatInvalid : MqttCommonReasonCode {
        override val code: Byte = 0x99.toByte()
    }

    /**
     * The retain not supported reason code.
     */
    data object RetainNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0x9A.toByte()
    }

    /**
     * The QoS not supported reason code.
     */
    data object QoSNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0x9B.toByte()
    }

    /**
     * The use another server reason code.
     */
    data object UseAnotherServer : MqttCommonReasonCode {
        override val code: Byte = 0x9C.toByte()
    }

    /**
     * The server moved reason code.
     */
    data object ServerMoved : MqttCommonReasonCode {
        override val code: Byte = 0x9D.toByte()
    }

    /**
     * The shared subscriptions not supported reason code.
     */
    data object SharedSubscriptionsNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0x9E.toByte()
    }

    /**
     * The connection rate exceeded reason code.
     */
    data object ConnectionRateExceeded : MqttCommonReasonCode {
        override val code: Byte = 0x9F.toByte()
    }

    /**
     * The subscription identifiers not supported reason code.
     */
    data object SubscriptionIdentifiersNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0xA1.toByte()
    }

    /**
     * The wildcard subscriptions not supported reason code.
     */
    data object WildcardSubscriptionsNotSupported : MqttCommonReasonCode {
        override val code: Byte = 0xA2.toByte()
    }
}
