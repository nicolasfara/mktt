package it.nicolasfarabegoli.mktt.message.publish.puback

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

data class MqttPubAck(val reasonCode: MqttPubAckReasonCode, val reasonString: String? = null) : MqttMessage {
    override val type: MqttMessageType = MqttMessageType.PubAck
}