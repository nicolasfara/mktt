package it.nicolasfarabegoli.mktt.message.publish.pubrel

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents the Reason Code of a PUBREL message.
 */
enum class MqttPubRelReasonCode(
    override val code: Byte,
) : MqttReasonCode {
    /**
     * TODO.
     */
    Success(MqttCommonReasonCode.Success.code),

    /**
     * TODO.
     */
    PacketIdentifierNotFound(MqttCommonReasonCode.PacketIdentifierNotFound.code),
    ;

    override val canBeSetByUser: Boolean get() = true
}
