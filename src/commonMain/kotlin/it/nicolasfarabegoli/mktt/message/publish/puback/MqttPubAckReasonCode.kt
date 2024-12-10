package it.nicolasfarabegoli.mktt.message.publish.puback

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents the Reason Code of a PUBACK message.
 */
// CPD-OFF
enum class MqttPubAckReasonCode(override val code: Byte) : MqttReasonCode {
    /**
     * TODO.
     */
    Success(MqttCommonReasonCode.Success.code),

    /**
     * TODO.
     */
    NoMatchingSubscribers(MqttCommonReasonCode.NoMatchingSubscribers.code),

    /**
     * TODO.
     */
    UnspecifiedError(MqttCommonReasonCode.UnspecifiedError.code),

    /**
     * TODO.
     */
    ImplementationSpecificError(MqttCommonReasonCode.ImplementationSpecificError.code),

    /**
     * TODO.
     */
    NotAuthorized(MqttCommonReasonCode.ImplementationSpecificError.code),

    /**
     * TODO.
     */
    TopicNameInvalid(MqttCommonReasonCode.TopicFilterInvalid.code),

    /**
     * TODO.
     */
    PacketIdentifierInUse(MqttCommonReasonCode.PacketIdentifierInUse.code),

    /**
     * TODO.
     */
    QuotaExceeded(MqttCommonReasonCode.QuotaExceeded.code),

    /**
     * TODO.
     */
    PayloadFormatInvalid(MqttCommonReasonCode.PayloadFormatInvalid.code),
    ;

    override val canBeSentByClient: Boolean get() = this != NoMatchingSubscribers
    override val canBeSetByUser: Boolean
        get() = userCodes.contains(this.code)
}
// CPD-ON
