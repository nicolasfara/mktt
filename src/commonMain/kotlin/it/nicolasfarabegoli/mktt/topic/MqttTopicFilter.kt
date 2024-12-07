package it.nicolasfarabegoli.mktt.topic

/**
 * Represents a topic filter.
 */
interface MqttTopicFilter {
    /**
     * The name of the filter.
     */
    val filterName: String

    /**
     * The levels of the filter.
     */
    val levels: List<String>

    /**
     * Whether the filter contains wildcards.
     */
    val containsWildcards: Boolean

    /**
     * Whether the filter contains a multilevel wildcard.
     */
    val containsMultilevelWildcard: Boolean

    /**
     * Whether the filter contains a single level wildcard.
     */
    val containsSingleLevelWildcard: Boolean

    /**
     * Returns `true` if the [topic] matches the filter.
     */
    fun matches(topic: MqttTopic): Boolean

    /**
     * Returns `true` if the [filter] matches this filter.
     */
    fun matches(filter: MqttTopicFilter): Boolean

    companion object {
        /**
         * Returns a [MqttTopicFilter] from the given [filter].
         */
        fun of(filter: String): MqttTopicFilter = MqttTopicFilterImpl(filter)

        /**
         * Returns a [MqttTopicFilter] from the given [filter].
         */
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
