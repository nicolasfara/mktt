package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.MqttDslMarker

/**
 * Topic filter and options used in SUBSCRIBE packets.
 *
 * @property filter topic filter expression.
 * @property subscriptionOptions MQTT subscription options for this filter.
 */
data class TopicFilter(val filter: Topic, val subscriptionOptions: SubscriptionOptions = SubscriptionOptions.DEFAULT) {
    init {
        require(filter.isNotBlank()) { "Empty topics are not allowed in topic filters" }
    }
}

/** Returns `true` if at least one filter is a shared subscription. */
fun List<TopicFilter>.hasSharedTopic(): Boolean = any { it.filter.isShared() }

/** Returns `true` if at least one filter contains MQTT wildcards. */
fun List<TopicFilter>.hasWildcard(): Boolean = any {
    it.filter.containsWildcard()
}

/** Builds a list of [TopicFilter] using [TopicFilterBuilder]. */
fun buildFilterList(init: TopicFilterBuilder.() -> Unit): List<TopicFilter> = TopicFilterBuilder().also(init).build()

/** DSL builder for creating lists of [TopicFilter]. */
@MqttDslMarker
class TopicFilterBuilder {

    private val filters = mutableListOf<TopicFilter>()

    /** Adds a topic filter with the provided subscription options. */
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

    /** Adds this string as a topic filter using default subscription options. */
    operator fun String.unaryPlus() {
        filters.add(
            TopicFilter(
                Topic(
                    this,
                ),
            ),
        )
    }

    /** Builds the list of configured topic filters. */
    fun build(): List<TopicFilter> = filters.toList()
}
