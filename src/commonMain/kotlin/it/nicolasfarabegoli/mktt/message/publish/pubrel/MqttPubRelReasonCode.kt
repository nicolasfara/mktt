package it.nicolasfarabegoli.mktt.message.publish.pubrel

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents the Reason Code of a PUBREL message.
 */
sealed interface MqttPubRelReasonCode : MqttReasonCode {
    override val canBeSetByUser: Boolean get() = true

    /**
     * The success reason code.
     */
    companion object {
        /**
         * Returns the [MqttPubRelReasonCode] from the given [code].
         */
        fun from(code: Byte): MqttPubRelReasonCode = when (code) {
            MqttCommonReasonCode.Success.code -> Success
            MqttCommonReasonCode.PacketIdentifierNotFound.code -> PacketIdentifierNotFound
            else -> throw IllegalArgumentException("Invalid PubRel Reason Code: $code")
        }
    }

    /**
     * The success reason code.
     */
    data object Success : MqttPubRelReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    /**
     * The packet identifier not found reason code.
     */
    data object PacketIdentifierNotFound : MqttPubRelReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierNotFound.code
    }
}
