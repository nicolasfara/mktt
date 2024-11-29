package it.nicolasfarabegoli.mktt.message.publish.pubrec

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents the Reason Code of a PUBREC message.
 */
sealed interface MqttPubRecReasonCode : MqttReasonCode {
    override val canBeSentByClient: Boolean get() = this != NoMatchingSubscribers
    override val canBeSetByUser: Boolean get() = listOf(
        Success,
        UnspecifiedError,
        ImplementationSpecificError,
        NotAuthorized,
        TopicNameInvalid,
        QuotaExceeded,
        PayloadFormatInvalid,
    ).contains(this)

    companion object {
        /**
         * Returns the [MqttPubRecReasonCode] from the given [code].
         */
        fun from(code: Byte): MqttPubRecReasonCode = when (code) {
            MqttCommonReasonCode.Success.code -> Success
            MqttCommonReasonCode.NoMatchingSubscribers.code -> NoMatchingSubscribers
            MqttCommonReasonCode.UnspecifiedError.code -> UnspecifiedError
            MqttCommonReasonCode.ImplementationSpecificError.code -> ImplementationSpecificError
            MqttCommonReasonCode.NotAuthorized.code -> NotAuthorized
            MqttCommonReasonCode.TopicNameInvalid.code -> TopicNameInvalid
            MqttCommonReasonCode.PacketIdentifierInUse.code -> PacketIdentifierInUse
            MqttCommonReasonCode.QuotaExceeded.code -> QuotaExceeded
            MqttCommonReasonCode.PayloadFormatInvalid.code -> PayloadFormatInvalid
            else -> throw IllegalArgumentException("Invalid PubRec Reason Code: $code")
        }
    }

    /**
     * The success reason code.
     */
    data object Success : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    /**
     * The no matching subscribers reason code.
     */
    data object NoMatchingSubscribers : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.NoMatchingSubscribers.code
    }

    /**
     * The unspecified error reason code.
     */
    data object UnspecifiedError : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.UnspecifiedError.code
    }

    /**
     * The implementation specific error reason code.
     */
    data object ImplementationSpecificError : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.ImplementationSpecificError.code
    }

    /**
     * The not authorized reason code.
     */
    data object NotAuthorized : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.NotAuthorized.code
    }

    /**
     * The topic name invalid reason code.
     */
    data object TopicNameInvalid : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.TopicNameInvalid.code
    }

    /**
     * The packet identifier in use reason code.
     */
    data object PacketIdentifierInUse : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierInUse.code
    }

    /**
     * The quota exceeded reason code.
     */
    data object QuotaExceeded : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.QuotaExceeded.code
    }

    /**
     * The payload format invalid reason code.
     */
    data object PayloadFormatInvalid : MqttPubRecReasonCode {
        override val code: Byte = MqttCommonReasonCode.PayloadFormatInvalid.code
    }
}
