package it.nicolasfarabegoli.mktt.topic

/**
 * Represents an MQTT topic with a specific [topicName] and [filter] and [levels].
 */
interface MqttTopic {
    val topicName: String
    val levels: List<String>
    val filter: MqttTopicFilter

    companion object {
        /**
         * Returns an [MqttTopic] from the given [topic] and [filter].
         */
        fun of(topic: String, filter: MqttTopicFilter = MqttTopicFilter.of(topic)): MqttTopic =
            MqttTopicImpl(topic, filter)

        /**
         * Returns an [MqttTopic] from the given [topic].
         */
        fun String.asTopic(filter: MqttTopicFilter = MqttTopicFilter.of(this)): MqttTopic =
            MqttTopicImpl(this, filter)
    }

    private data class MqttTopicImpl(
        override val topicName: String,
        override val filter: MqttTopicFilter = MqttTopicFilter.of(topicName),
        override val levels: List<String> = topicName.split("/"),
    ) : MqttTopic
}
