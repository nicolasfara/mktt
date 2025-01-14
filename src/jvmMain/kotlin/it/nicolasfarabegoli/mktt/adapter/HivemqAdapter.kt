package it.nicolasfarabegoli.mktt.adapter

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.subscribe.MqttRetainHandling
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import it.nicolasfarabegoli.mktt.utils.JavaKotlinUtils.toIntOrNull
import it.nicolasfarabegoli.mktt.utils.JavaKotlinUtils.toLongOrNull
import kotlin.jvm.optionals.getOrNull

internal object HivemqAdapter {
    internal fun fromHivemqMqttConnAck(connAck: Mqtt5ConnAck): MqttConnAck =
        MqttConnAck(
            reasonCode = MqttConnAckReasonCode.from(connAck.reasonCode.code.toByte()),
            isSessionPresent = connAck.isSessionPresent,
            sessionExpiryInterval = connAck.sessionExpiryInterval.toLongOrNull(),
            serverKeepAlive = connAck.serverKeepAlive.toIntOrNull(),
            assignedClientIdentifier = connAck.assignedClientIdentifier.getOrNull().toString(),
        )

    internal fun MqttQoS.toHiveMqttQos(): MqttQos =
        when (this) {
            MqttQoS.AtMostOnce -> MqttQos.AT_MOST_ONCE
            MqttQoS.AtLeastOnce -> MqttQos.AT_LEAST_ONCE
            MqttQoS.ExactlyOnce -> MqttQos.EXACTLY_ONCE
        }

    private fun MqttRetainHandling.toHiveMqtt(): Mqtt5RetainHandling =
        when (this) {
            MqttRetainHandling.SEND -> Mqtt5RetainHandling.SEND
            MqttRetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST ->
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST
            MqttRetainHandling.DO_NOT_SEND -> Mqtt5RetainHandling.DO_NOT_SEND
        }

    internal fun MqttSubscription.toHivemqMqtt(): Mqtt5Subscribe =
        Mqtt5Subscribe
            .builder()
            .topicFilter(topicFilter.filterName)
            .qos(qos.toHiveMqttQos())
            .noLocal(noLocal)
            .retainAsPublished(retainAsPublished)
            .retainHandling(retainHandling.toHiveMqtt())
            .retainAsPublished(retainAsPublished)
            .build()
}
