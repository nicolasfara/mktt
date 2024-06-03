package it.nicolasfarabegoli.mktt.message.connect.connack

import it.nicolasfarabegoli.mktt.message.ConnAck
import it.nicolasfarabegoli.mktt.message.MqttMessageType

data class MqttConnAck(
    val reasonCode: MqttConnAckReasonCode
    // TODO: Add properties
) {
    val type: MqttMessageType = ConnAck
}