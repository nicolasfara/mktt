package it.nicolasfarabegoli.mktt.topic

interface MqttTopic {
    val topic: String
    val levels: List<String>
    val filter: MqttTopicFilter
}
