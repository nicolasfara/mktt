package it.nicolasfarabegoli.mktt.message.publish.pubcomp

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents a PUBCOMP message.
 */
sealed interface MqttPubCompReasonCode : MqttReasonCode {
    override val canBeSentByClient: Boolean get() = true

    /**
     * The success reason code.
     */
    companion object {
        /**
         * Returns the [MqttPubCompReasonCode] from the given [code].
         */
        fun from(code: Byte): MqttPubCompReasonCode = when (code) {
            MqttCommonReasonCode.Success.code -> Success
            MqttCommonReasonCode.PacketIdentifierNotFound.code -> PacketIdentifierNotFound
            else -> throw IllegalArgumentException("Invalid PubComp Reason Code: $code")
        }
    }

    /**
     * The success reason code.
     */
    data object Success : MqttPubCompReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    /**
     * The packet identifier not found reason code.
     */
    data object PacketIdentifierNotFound : MqttPubCompReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierNotFound.code
    }
}
