package it.nicolasfarabegoli.mktt.message.publish.pubcomp

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

data class MqttPubComp(val reasonCode: MqttPubCompReasonCode, val reasonString: String?) : MqttMessage {
    override val type: MqttMessageType get() = MqttMessageType.PubComp
}
