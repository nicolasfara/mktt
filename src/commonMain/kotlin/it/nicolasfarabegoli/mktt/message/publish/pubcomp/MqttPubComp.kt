package it.nicolasfarabegoli.mktt.message.publish.pubcomp

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

/**
 * Represents a PUBCOMP message with a specific [reasonCode] and [reasonString].
 */
data class MqttPubComp(
    val reasonCode: MqttPubCompReasonCode,
    val reasonString: String?,
) : MqttMessage {
    override val type: MqttMessageType get() = MqttMessageType.PUBCOMP
}
