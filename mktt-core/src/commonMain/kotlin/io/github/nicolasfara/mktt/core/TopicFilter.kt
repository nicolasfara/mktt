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

/**
 * Returns `true` if at least one filter is a shared subscription.
 *
 * @return `true` when any filter uses the `$share/{name}/{filter}` form.
 */
fun List<TopicFilter>.hasSharedTopic(): Boolean = any { it.filter.isShared() }

/**
 * Returns `true` if at least one filter contains MQTT wildcards.
 *
 * @return `true` when any filter contains `+` or `#`.
 */
fun List<TopicFilter>.hasWildcard(): Boolean = any {
    it.filter.containsWildcard()
}

/**
 * Builds a list of [TopicFilter] using [TopicFilterBuilder].
 *
 * @param init builder block that adds filters.
 * @return an immutable list of configured topic filters.
 */
fun buildFilterList(init: TopicFilterBuilder.() -> Unit): List<TopicFilter> = TopicFilterBuilder().also(init).build()

/** DSL builder for creating lists of [TopicFilter]. */
@MqttDslMarker
class TopicFilterBuilder {

    private val filters = mutableListOf<TopicFilter>()

    /**
     * Adds a topic filter with the provided subscription options.
     *
     * @param topic MQTT topic filter expression.
     * @param qoS maximum QoS requested for matching messages.
     * @param isNoLocal whether the broker should avoid forwarding this client's own publications.
     * @param retainAsPublished whether retained messages should keep their retained flag.
     * @param retainHandling when retained messages should be sent after subscribing.
     */
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

    /**
     * Adds this string as a topic filter using default subscription options.
     */
    operator fun String.unaryPlus() {
        filters.add(
            TopicFilter(
                Topic(
                    this,
                ),
            ),
        )
    }

    /**
     * Builds the list of configured topic filters.
     *
     * @return an immutable list of configured topic filters.
     */
    fun build(): List<TopicFilter> = filters.toList()
}
