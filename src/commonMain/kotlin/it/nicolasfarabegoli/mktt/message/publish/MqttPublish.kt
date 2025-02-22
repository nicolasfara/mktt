package it.nicolasfarabegoli.mktt.message.publish

import it.nicolasfarabegoli.mktt.message.MqttMessage
import it.nicolasfarabegoli.mktt.message.MqttMessageType
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.topic.MqttTopic

/**
 * Represents a PUBLISH message with a specific [qos], [topic], [payload], [isRetain], [expiryInterval], [contentType],
 * [responseTopic], and [correlationData].
 */
data class MqttPublish(
    val qos: MqttQoS = MqttQoS.AtMostOnce,
    val topic: MqttTopic,
    val payload: ByteArray? = null,
    val isRetain: Boolean = false,
    val expiryInterval: Long? = null,
    val contentType: String? = null,
    val responseTopic: MqttTopic? = null,
    val correlationData: ByteArray? = null,
) : MqttMessage {
    override val type: MqttMessageType = MqttMessageType.PUBLISH
}
