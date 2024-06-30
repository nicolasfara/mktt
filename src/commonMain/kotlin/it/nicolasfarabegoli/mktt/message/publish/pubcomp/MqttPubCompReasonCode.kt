package it.nicolasfarabegoli.mktt.message.publish.pubcomp

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

sealed interface MqttPubCompReasonCode : MqttReasonCode {
    override val canBeSentByClient: Boolean get() = true

    companion object {
        fun from(code: Byte): MqttPubCompReasonCode = when (code) {
            MqttCommonReasonCode.Success.code -> Success
            MqttCommonReasonCode.PacketIdentifierNotFound.code -> PacketIdentifierNotFound
            else -> throw IllegalArgumentException("Invalid PubComp Reason Code: $code")
        }
    }
    data object Success : MqttPubCompReasonCode {
        override val code: Byte = MqttCommonReasonCode.Success.code
    }

    data object PacketIdentifierNotFound : MqttPubCompReasonCode {
        override val code: Byte = MqttCommonReasonCode.PacketIdentifierNotFound.code
    }
}
