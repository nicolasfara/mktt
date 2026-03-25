package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.MqttDslMarker

data class TopicFilter(val filter: Topic, val subscriptionOptions: SubscriptionOptions = SubscriptionOptions.DEFAULT) {
    init {
        require(filter.isNotBlank()) { "Empty topics are not allowed in topic filters" }
    }
}

fun List<TopicFilter>.hasSharedTopic(): Boolean = any { it.filter.isShared() }

fun List<TopicFilter>.hasWildcard(): Boolean = any {
    it.filter.containsWildcard()
}

fun buildFilterList(init: TopicFilterBuilder.() -> Unit): List<TopicFilter> = TopicFilterBuilder().also(init).build()

@MqttDslMarker
class TopicFilterBuilder {

    private val filters = mutableListOf<TopicFilter>()

    fun add(
        topic: String,
        qoS: QoS = QoS.AT_MOST_ONCE,
        isNoLocal: Boolean = false,
        retainAsPublished: Boolean = false,
        retainHandling: RetainHandling = RetainHandling.SEND_ON_SUBSCRIBE,
    ) {
        filters.add(
            TopicFilter(
                Topic(
                    topic,
                ),
                SubscriptionOptions(
                    qoS,
                    isNoLocal,
                    retainAsPublished,
                    retainHandling,
                ),
            ),
        )
    }

    operator fun String.unaryPlus() {
        filters.add(
            TopicFilter(
                Topic(
                    this,
                ),
            ),
        )
    }

    fun build(): List<TopicFilter> = filters.toList()
}
