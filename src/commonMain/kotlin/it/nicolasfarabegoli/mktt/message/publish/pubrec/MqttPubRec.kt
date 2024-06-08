package it.nicolasfarabegoli.mktt.message.publish.pubrec

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

data class MqttPubRec(val reasonCode: MqttPubRecReasonCode, val reasonString: String?) : MqttMessage {
    override val type: MqttMessageType get() = MqttMessageType.PubRec
}