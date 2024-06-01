package it.nicolasfarabegoli.mktt.topic

data class MqttTopicFilter(val filter: String = WILDCARD_MULTI) {
    init {
        require(filter.isNotEmpty()) { "Filter cannot be empty" }
        require(filter.length <= 65535) { "Filter length cannot exceed 65535 characters" }
    }

    fun levels(): List<String> = filter.split(SEPARATOR)

    fun matches(topic: MqttTopic): Boolean {
        val topicLevels = topic.value.split("/")
        val filterLevels = filter.split("/")

        return filterLevels.withIndex().all { (index, filterLevel) ->
            when {
                filterLevel == "#" -> true
                filterLevel == "+" -> index < topicLevels.size
                index < topicLevels.size && filterLevel == topicLevels[index] -> true
                else -> false
            }
        } && (filterLevels.last() == "#" || filterLevels.size == topicLevels.size)
    }

    companion object {
        const val SEPARATOR = "/"
        const val WILDCARD_MULTI = "#"
        const val WILDCARD_SINGLE = "+"
        fun String.toMqttTopicFilter(): MqttTopicFilter = MqttTopicFilter(this)
    }
}