package it.nicolasfarabegoli.mktt.message.publish.puback

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

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
            PayloadFormatInvalid
        ).contains(this)
    }

    companion object {
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

    data object Success : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    data object NoMatchingSubscribers : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.NoMatchingSubscribers.code
    }

    data object UnspecifiedError : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.UnspecifiedError.code
    }

    data object ImplementationSpecificError : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.ImplementationSpecificError.code
    }

    data object NotAuthorized : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.NotAuthorized.code
    }

    data object TopicNameInvalid : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.TopicNameInvalid.code
    }

    data object PacketIdentifierInUse : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierInUse.code
    }

    data object QuotaExceeded : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.QuotaExceeded.code
    }

    data object PayloadFormatInvalid : MqttPubAckReasonCode {
        override val code: Byte = MqttCommonReasonCode.PayloadFormatInvalid.code
    }
}
