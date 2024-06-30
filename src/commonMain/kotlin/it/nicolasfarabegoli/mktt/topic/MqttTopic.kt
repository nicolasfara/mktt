package it.nicolasfarabegoli.mktt.topic

interface MqttTopic {
    val topicName: String
    val levels: List<String>
    val filter: MqttTopicFilter

    companion object {
        fun of(topic: String, filter: MqttTopicFilter = MqttTopicFilter.of(topic)): MqttTopic =
            MqttTopicImpl(topic, filter)
        fun String.asTopic(filter: MqttTopicFilter = MqttTopicFilter.of(this)): MqttTopic =
            MqttTopicImpl(this, filter)
    }

    private data class MqttTopicImpl(
        override val topicName: String,
        override val filter: MqttTopicFilter = MqttTopicFilter.of(topicName),
        override val levels: List<String> = topicName.split("/"),
    ) : MqttTopic
}
