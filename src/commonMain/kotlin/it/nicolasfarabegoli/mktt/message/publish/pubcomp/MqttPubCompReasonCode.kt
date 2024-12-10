package it.nicolasfarabegoli.mktt.message.publish.pubcomp

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents the Reason Code of a PUBCOMP message.
 */
enum class MqttPubCompReasonCode(override val code: Byte) : MqttReasonCode {
    /**
     * TODO.
     */
    Success(MqttCommonReasonCode.Success.code),

    /**
     * TODO.
     */
    PacketIdentifierNotFound(MqttCommonReasonCode.PacketIdentifierNotFound.code),
    ;

    override val canBeSentByClient: Boolean get() = true
}
