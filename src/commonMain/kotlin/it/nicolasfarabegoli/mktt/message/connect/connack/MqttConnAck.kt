package it.nicolasfarabegoli.mktt.message.connect.connack

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType

/**
 * Represents a CONNACK message with a specific [reasonCode] and [isSessionPresent].
 * Optionally, it can contain the [sessionExpiryInterval], [serverKeepAlive], and [assignedClientIdentifier].
 */
data class MqttConnAck(
    val reasonCode: MqttConnAckReasonCode,
    val isSessionPresent: Boolean,
    val sessionExpiryInterval: Long? = null,
    val serverKeepAlive: Int? = null,
    val assignedClientIdentifier: String? = null,
) : MqttMessage {
    override val type: MqttMessageType = MqttMessageType.CONNACK
}
