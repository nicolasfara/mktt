package it.nicolasfarabegoli.mktt.adapter.publish.puback

import com.hivemq.client.mqtt.mqtt5.message.publish.puback.Mqtt5PubAck
import com.hivemq.client.mqtt.mqtt5.message.publish.puback.Mqtt5PubAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.puback.MqttPubAck
import it.nicolasfarabegoli.mktt.message.publish.puback.MqttPubAckReasonCode
import kotlin.jvm.optionals.getOrNull

/**
 * Adapter that converts HiveMQ's [Mqtt5PubAck] to MKTT's [MqttPubAck].
 */
object HivemqPubAckAdapter {
    /**
     * Converts a HiveMQ's [Mqtt5PubAck] to a MKTT's [MqttPubAck].
     */
    fun Mqtt5PubAck.toMqtt(): MqttPubAck =
        MqttPubAck(
            reasonCode = reasonCode.toMqtt(),
            reasonString = reasonString.getOrNull()?.toString(),
        )

    /**
     * Converts a MKTT's [MqttPubAck] to a HiveMQ's [Mqtt5PubAck].
     */
    fun Mqtt5PubAckReasonCode.toMqtt(): MqttPubAckReasonCode =
        when (this) {
            Mqtt5PubAckReasonCode.SUCCESS -> MqttPubAckReasonCode.Success
            Mqtt5PubAckReasonCode.NO_MATCHING_SUBSCRIBERS -> MqttPubAckReasonCode.NoMatchingSubscribers
            Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR -> MqttPubAckReasonCode.UnspecifiedError
            Mqtt5PubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR -> MqttPubAckReasonCode.ImplementationSpecificError
            Mqtt5PubAckReasonCode.NOT_AUTHORIZED -> MqttPubAckReasonCode.NotAuthorized
            Mqtt5PubAckReasonCode.TOPIC_NAME_INVALID -> MqttPubAckReasonCode.TopicNameInvalid
            Mqtt5PubAckReasonCode.PACKET_IDENTIFIER_IN_USE -> MqttPubAckReasonCode.PacketIdentifierInUse
            Mqtt5PubAckReasonCode.QUOTA_EXCEEDED -> MqttPubAckReasonCode.QuotaExceeded
            Mqtt5PubAckReasonCode.PAYLOAD_FORMAT_INVALID -> MqttPubAckReasonCode.PayloadFormatInvalid
        }
}
