package it.nicolasfarabegoli.mktt.message.publish.pubrel

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

sealed interface MqttPubRelReasonCode : MqttReasonCode {
    override val canBeSetByUser: Boolean get() = true

    companion object {
        fun from(code: Byte): MqttPubRelReasonCode = when (code) {
            MqttCommonReasonCode.Success.code -> Success
            MqttCommonReasonCode.PacketIdentifierNotFound.code -> PacketIdentifierNotFound
            else -> throw IllegalArgumentException("Invalid PubRel Reason Code: $code")
        }
    }

    data object Success : MqttPubRelReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    data object PacketIdentifierNotFound : MqttPubRelReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierNotFound.code
    }
}
