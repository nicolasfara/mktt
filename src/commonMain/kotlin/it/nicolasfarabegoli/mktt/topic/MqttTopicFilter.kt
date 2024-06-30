package it.nicolasfarabegoli.mktt.topic

interface MqttTopicFilter {
    val filterName: String
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
        override val filterName: String,
        override val levels: List<String> = filterName.split("/"),
        override val containsWildcards: Boolean = levels.any { it == "#" || it == "+" },
        override val containsMultilevelWildcard: Boolean = levels.contains("#"),
        override val containsSingleLevelWildcard: Boolean = levels.contains("+"),
    ) : MqttTopicFilter {
        override fun matches(topic: MqttTopic): Boolean {
            if (topic.levels.size != levels.size) return false
            return levels.zip(topic.levels).all { (filter, topic) ->
                filter == topic || filter == "#" || filter == "+"
            }
        }
        override fun matches(filter: MqttTopicFilter): Boolean = filter == this
    }
}
