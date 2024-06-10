package it.nicolasfarabegoli.mktt.adapter.topic

import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import it.nicolasfarabegoli.mktt.utils.JavaKotlinUtils.toByteArray
import com.hivemq.client.mqtt.datatypes.MqttTopic as HiveMqttTopic

internal object HivemqTopicAdapter {
    fun HiveMqttTopic.toMqtt(): MqttTopic {
        val topicStringRepr = toByteBuffer().toByteArray().decodeToString()
        val topic = "topic=([^,]+)".toRegex().find(topicStringRepr)?.groupValues?.get(1)
            ?: error("Invalid topic string representation")
        val filterStringRepr = filter().toByteBuffer().toByteArray().decodeToString()
        val filter = "MqttTopicFilterImpl.+filter=([^,]+)".toRegex().find(filterStringRepr)?.groupValues?.get(1)
            ?: error("Invalid topic string representation")
        return MqttTopic.of(topic, MqttTopicFilter.of(filter))
    }
}
