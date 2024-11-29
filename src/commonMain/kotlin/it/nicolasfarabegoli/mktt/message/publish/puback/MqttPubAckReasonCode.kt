package it.nicolasfarabegoli.mktt.message.publish.puback

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents a PUBACK Reason Code.
 */
sealed interface MqttPubAckReasonCode : MqttReasonCode {
    override val canBeSentByClient: Boolean get() = this != NoMatchingSubscribers

    override val canBeSetByUser: Boolean get() {
        return listOf(
            Success,
            UnspecifiedError,
            ImplementationSpecificError,
            NotAuthorized,
            TopicNameInvalid,
            QuotaExceeded,
            PayloadFormatInvalid,
        ).contains(this)
    }

    /**
     * The success reason code.
     */
    companion object {
        /**
         * Returns the [MqttPubAckReasonCode] from the given [code].
         */
        fun from(code: Byte): MqttPubAckReasonCode = when (code) {
            MqttCommonReasonCode.Success.code -> Success
            MqttCommonReasonCode.NoMatchingSubscribers.code -> NoMatchingSubscribers
            MqttCommonReasonCode.UnspecifiedError.code -> UnspecifiedError
            MqttCommonReasonCode.ImplementationSpecificError.code -> ImplementationSpecificError
            MqttCommonReasonCode.NotAuthorized.code -> NotAuthorized
            MqttCommonReasonCode.TopicNameInvalid.code -> TopicNameInvalid
            MqttCommonReasonCode.PacketIdentifierInUse.code -> PacketIdentifierInUse
            MqttCommonReasonCode.QuotaExceeded.code -> QuotaExceeded
            MqttCommonReasonCode.PayloadFormatInvalid.code -> PayloadFormatInvalid
            else -> throw IllegalArgumentException("Invalid PubAck Reason Code: $code")
        }
    }

    /**
     * The success reason code.
     */
    data object Success : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    /**
     * The no matching subscribers reason code.
     */
    data object NoMatchingSubscribers : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.NoMatchingSubscribers.code
    }

    /**
     * The unspecified error reason code.
     */
    data object UnspecifiedError : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.UnspecifiedError.code
    }

    /**
     * The implementation specific error reason code.
     */
    data object ImplementationSpecificError : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ImplementationSpecificError.code
    }

    /**
     * The not authorized reason code.
     */
    data object NotAuthorized : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.NotAuthorized.code
    }

    /**
     * The topic name invalid reason code.
     */
    data object TopicNameInvalid : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.TopicNameInvalid.code
    }

    /**
     * The packet identifier in use reason code.
     */
    data object PacketIdentifierInUse : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierInUse.code
    }

    /**
     * The quota exceeded reason code.
     */
    data object QuotaExceeded : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.QuotaExceeded.code
    }

    /**
     * The payload format invalid reason code.
     */
    data object PayloadFormatInvalid : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PayloadFormatInvalid.code
    }
}
