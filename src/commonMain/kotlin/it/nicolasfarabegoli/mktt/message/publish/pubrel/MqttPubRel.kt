package it.nicolasfarabegoli.mktt.message.publish.pubrel

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

data class MqttPubRel(val reasonCode: MqttPubRelReasonCode, val reasonString: String?) : MqttMessage {
    override val type: MqttMessageType get() = MqttMessageType.PubRel
}
