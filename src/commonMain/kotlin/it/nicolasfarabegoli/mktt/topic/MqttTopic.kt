package it.nicolasfarabegoli.mktt.topic

interface MqttTopic {
    val topic: String
    val levels: List<String>
    val filter: MqttTopicFilter

    companion object {
        fun of(topic: String, filter: MqttTopicFilter = MqttTopicFilter.of(topic)): MqttTopic =
            MqttTopicImpl(topic, filter)
        fun String.asTopic(filter: MqttTopicFilter = MqttTopicFilter.of(this)): MqttTopic =
            MqttTopicImpl(this, filter)
    }

    private data class MqttTopicImpl(
        override val topic: String,
        override val filter: MqttTopicFilter = MqttTopicFilter.of(topic),
        override val levels: List<String> = topic.split("/"),
    ) : MqttTopic
}
