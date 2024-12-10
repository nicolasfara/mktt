package it.nicolasfarabegoli.mktt.message

/**
 * Common reason [code]s for MQTT messages.
 */
enum class MqttCommonReasonCode(val code: Byte) {
    /**
     * TODO.
     */
    Success(0x00.toByte()),

    /**
     * TODO.
     */
    NoMatchingSubscribers(0x10.toByte()),

    /**
     * TODO.
     */
    UnspecifiedError(0x80.toByte()),

    /**
     * TODO.
     */
    MalformedPacket(0x81.toByte()),

    /**
     * TODO.
     */
    ProtocolError(0x82.toByte()),

    /**
     * TODO.
     */
    ImplementationSpecificError(0x83.toByte()),

    /**
     * TODO.
     */
    NotAuthorized(0x87.toByte()),

    /**
     * TODO.
     */
    ServerBusy(0x89.toByte()),

    /**
     * TODO.
     */
    BadAuthenticationMethod(0x8C.toByte()),

    /**
     * TODO.
     */
    TopicFilterInvalid(0x8F.toByte()),

    /**
     * TODO.
     */
    TopicNameInvalid(0x90.toByte()),

    /**
     * TODO.
     */
    PacketIdentifierInUse(0x91.toByte()),

    /**
     * TODO.
     */
    PacketIdentifierNotFound(0x92.toByte()),

    /**
     * TODO.
     */
    PacketTooLarge(0x95.toByte()),

    /**
     * TODO.
     */
    QuotaExceeded(0x97.toByte()),

    /**
     * TODO.
     */
    PayloadFormatInvalid(0x99.toByte()),

    /**
     * TODO.
     */
    RetainNotSupported(0x9A.toByte()),

    /**
     * TODO.
     */
    QoSNotSupported(0x9B.toByte()),

    /**
     * TODO.
     */
    UseAnotherServer(0x9C.toByte()),

    /**
     * TODO.
     */
    ServerMoved(0x9D.toByte()),

    /**
     * TODO.
     */
    SharedSubscriptionsNotSupported(0x9E.toByte()),

    /**
     * TODO.
     */
    ConnectionRateExceeded(0x9F.toByte()),

    /**
     * TODO.
     */
    SubscriptionIdentifiersNotSupported(0xA1.toByte()),

    /**
     * TODO.
     */
    WildcardSubscriptionsNotSupported(0xA2.toByte()),
    ;

    /**
     * Companion object for [MqttCommonReasonCode].
     */
    companion object {
        /**
         * Returns the [MqttCommonReasonCode] from the given [code].
         *
         * Throws an [IllegalArgumentException] if the [code] is unknown.
         */
        fun fromCode(code: Byte): MqttCommonReasonCode {
            return MqttCommonReasonCode.entries.first { it.code == code }
        }
    }
}
