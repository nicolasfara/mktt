package it.nicolasfarabegoli.mktt.adapter.topic

import it.nicolasfarabegoli.mktt.topic.MqttTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import com.hivemq.client.mqtt.datatypes.MqttTopic as HiveMqttTopic
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter as HiveMqttTopicFilter

internal object HivemqTopicAdapter {
    fun HiveMqttTopic.toMqtt(): MqttTopic {
        val topic = this.toString()
        val levels = this.levels
        val filter = this.filter().toMqtt()
        return object : MqttTopic {
            override val topic: String = topic
            override val levels: List<String> = levels
            override val filter: MqttTopicFilter = filter

        }
    }
    fun MqttTopic.toHiveMqtt(): HiveMqttTopic = HiveMqttTopic.of(topic)

    fun HiveMqttTopicFilter.toMqtt(): MqttTopicFilter {
        val filter = this.toString()
        val levels = this.levels
        return object : MqttTopicFilter {
            override val filter: String = filter
            override val levels: List<String> = levels
            override val containsWildcards: Boolean = containsWildcards()
            override val containsMultilevelWildcard: Boolean = containsMultiLevelWildcard()
            override val containsSingleLevelWildcard: Boolean = containsSingleLevelWildcard()
            override fun matches(topic: MqttTopic): Boolean = matches(topic.toHiveMqtt())
            override fun matches(filter: MqttTopicFilter): Boolean = matches(filter.toHiveMqtt())
        }
    }
    fun MqttTopicFilter.toHiveMqtt(): HiveMqttTopicFilter = HiveMqttTopicFilter.of(filter)
}