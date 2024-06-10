package it.nicolasfarabegoli.mktt.topic

interface MqttTopicFilter {
    val filter: String
    val levels: List<String>
    val containsWildcards: Boolean
    val containsMultilevelWildcard: Boolean
    val containsSingleLevelWildcard: Boolean
    fun matches(topic: MqttTopic): Boolean
    fun matches(filter: MqttTopicFilter): Boolean

    companion object {
        fun of(filter: String): MqttTopicFilter = MqttTopicFilterImpl(filter)
        fun String.asTopicFilter(): MqttTopicFilter = MqttTopicFilterImpl(this)
    }

    private data class MqttTopicFilterImpl(
        override val filter: String,
        override val levels: List<String> = filter.split("/"),
        override val containsWildcards: Boolean = levels.any { it == "#" || it == "+" },
        override val containsMultilevelWildcard: Boolean = levels.contains("#"),
        override val containsSingleLevelWildcard: Boolean = levels.contains("+"),
    ) : MqttTopicFilter {
        override fun matches(topic: MqttTopic): Boolean {
            val topicLevels = topic.levels
            if (topicLevels.size < levels.size) return false
            return levels.zip(topicLevels).all { (filterLevel, topicLevel) ->
                filterLevel == "#" || filterLevel == topicLevel || filterLevel == "+"
            }
        }
        override fun matches(filter: MqttTopicFilter): Boolean = filter == this
    }
}
