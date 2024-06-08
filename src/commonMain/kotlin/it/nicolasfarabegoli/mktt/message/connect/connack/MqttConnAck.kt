package it.nicolasfarabegoli.mktt.message.connect.connack

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

data class MqttConnAck(
    val reasonCode: MqttConnAckReasonCode,
    val isSessionPresent: Boolean,
    val sessionExpiryInterval: Long? = null,
    val serverKeepAlive: Int? = null,
    val assignedClientIdentifier: String? = null,
) : MqttMessage {
    override val type: MqttMessageType = MqttMessageType.ConnAck
}
