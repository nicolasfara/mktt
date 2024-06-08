package it.nicolasfarabegoli.mktt.topic

interface MqttTopicFilter {
    val filter: String
    val levels: List<String>
    val containsWildcards: Boolean
    val containsMultilevelWildcard: Boolean
    val containsSingleLevelWildcard: Boolean
    fun matches(topic: MqttTopic): Boolean
    fun matches(filter: MqttTopicFilter): Boolean
}
