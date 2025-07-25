package io.github.nicolasfara.adapter

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import io.github.nicolasfara.MqttWill

internal object MqttWillAdapter {
    fun MqttWill.toHivemq(): Mqtt5Publish = Mqtt5Publish
        .builder()
        .topic(topic)
        .payload(message)
        .qos(MqttQos.fromCode(qos.code) ?: error("Invalid QoS"))
        .retain(retained)
        .build()
}
