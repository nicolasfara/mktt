package it.nicolasfarabegoli.mktt.adapter.publish.pubrec

import com.hivemq.client.mqtt.mqtt5.message.publish.pubrec.Mqtt5PubRec
import it.nicolasfarabegoli.mktt.message.publish.pubrec.MqttPubRec
import it.nicolasfarabegoli.mktt.message.publish.pubrec.MqttPubRecReasonCode
import kotlin.jvm.optionals.getOrNull

/**
 * Adapter that converts HiveMQ's [Mqtt5PubRec] to MKTT's [MqttPubRec].
 */
object HivemqPubRecAdapter {
    /**
     * Converts a HiveMQ's [Mqtt5PubRec] to a MKTT's [MqttPubRec].
     */
    fun Mqtt5PubRec.toMqtt(): MqttPubRec =
        MqttPubRec(
            reasonCode = MqttPubRecReasonCode.from(reasonCode.code.toByte()),
            reasonString = reasonString.getOrNull()?.toString(),
        )
}
