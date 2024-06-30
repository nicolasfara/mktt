package it.nicolasfarabegoli.mktt.adapter.topic

import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import it.nicolasfarabegoli.mktt.utils.JavaKotlinUtils.toByteArray
import com.hivemq.client.mqtt.datatypes.MqttTopic as HiveMqttTopic

internal object HivemqTopicAdapter {
    fun HiveMqttTopic.toMqtt(): MqttTopic {
        val topic = toByteBuffer().toByteArray().decodeToString()
        val filter = filter().toByteBuffer().toByteArray().decodeToString()
        return MqttTopic.of(topic, MqttTopicFilter.of(filter))
    }
}
